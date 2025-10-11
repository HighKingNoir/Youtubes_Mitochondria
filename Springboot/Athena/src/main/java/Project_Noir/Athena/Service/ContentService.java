package Project_Noir.Athena.Service;

import Project_Noir.Athena.Controller.ServerSideEventController;
import Project_Noir.Athena.DTO.*;
import Project_Noir.Athena.Exception.SivantisException;
import Project_Noir.Athena.Model.*;
import Project_Noir.Athena.Repo.*;
import com.auth0.jwt.JWT;
import com.mongodb.DuplicateKeyException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.protocol.exceptions.TransactionException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@AllArgsConstructor
public class ContentService {
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final PaymentRepository paymentRepository;
    private final ServerSideEventController serverSideEventController;
    private final WatchNowPayLaterRepository watchNowPayLaterRepository;
    private final ContractServiceInterface contractServiceInterface;
    private final JwtService jwtService;
    private final MongoTemplate mongoTemplate;
    private final Ec2InstanceTagService ec2InstanceTagService;
    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;
    private final MessageService messageService;
    private final ServerSideMultiSendContractService serverSideMultiSendContractService;
    private final Integer maxNumberOfBuyers = 50;


    // @dev Generates content entity that is finished
    public Content saveCompleteContent(ContentRequest contentRequest, String JWT) throws IOException, URISyntaxException, TransactionException, ExecutionException, InterruptedException {
        contentRequestChecks(contentRequest);
        if(!isAtLeast3DaysFromToday(contentRequest.getReleaseDate())){
            throw new SivantisException("Release date must be at least 3 days from now");
        }
        if(!contentRequest.getPrivacyStatus().equals("private")){
            throw new SivantisException("Main Video must have a privacy status of 'private'");
        }
        var User = userRepository.findById(jwtService.extractUserId(JWT)).orElseThrow();
        if(User.getPersonalWallet() == null){
            throw new SivantisException("Must have a personal wallet on file");
        }
        var Content = mapContentRequest(contentRequest, User.getUserId(),User.getIsViolator());
        try {
            contentRepository.save(Content); // save will fail if the ID already exists
        } catch (DuplicateKeyException e) {
            throw new SivantisException("A user published a video with this YouTube ID already");
        }
        if(Content.getContentType().equals("Short Film")){
            Content.setDuration(isGreaterThanTenMinutes(contentRequest.getDuration()));
        }
        else if (Content.getContentType().equals("Movies")) {
            Content.setDuration(isGreaterThanOneHourAndFifteenMinutes(contentRequest.getDuration()));
        }
        else if(Content.getContentType().equals("Sports") || Content.getContentType().equals("Concerts")){
            if(!contentRequest.getLiveBroadcastContent().equals("upcoming")) {
                throw new SivantisException("Selected video must be a upcoming livestream");
            }
        }
        Content.setContentEnum(ContentEnum.PendingConfirmation);
        if(isAuction(Content.getContentType())){
            contractServiceInterface.createNewAuction(Content.getContentId(), Content.getNumbBidders(), Content.getStartingCost(), User.getPersonalWallet());
            if(contentRequest.getLiveBroadcastContent().equals("none")){
                Content.setDuration(isGreaterThanTenMinutes(contentRequest.getDuration()));
            }
        }
        else {
            if (isNotYetCreatorOrPending(User)) {
                // Mark content as pending until on-chain addContentCreator finishes
                Content.setPendingCreatorApproval(true);

                // Only queue blockchain tx once
                if (isNotContentCreator(User.getUserId())) {
                    contractServiceInterface.addContentCreator(User.getUserId(), User.getPersonalWallet(), User.getRank());
                }
            } else if (!User.isContentCreator()) {
                // User is pending — still mark content as PendingConfirmation
                Content.setPendingCreatorApproval(true);
            } else {
                // User is already a content creator
                Content.setContentEnum(ContentEnum.Active);
                Content.setActiveDate(Instant.now());
            }
        }
        Content.setIsComplete(true);
        contentRepository.save(Content);
        increaseVideoPostedAndAddContentId(User, Content.getContentId());
        return Content;
    }

    private void increaseVideoPostedAndAddContentId(Users user, String contentID){
        Query query = new Query(Criteria.where("_id").is(user.getUserId()));
        Update update = new Update()
                .inc("videosPosted", 1)
                .push("createdContent", contentID);
        mongoTemplate.updateFirst(query, update, Users.class);
    }

    public void completeVideo(CompleteVideoRequest completeVideoRequest, String JWT){
        if(!completeVideoRequest.getPrivacyStatus().equals("private")){
            throw new SivantisException("Main Video must have a privacy status of 'private'");
        }
        var content = contentRepository.findById(completeVideoRequest.getContentID()).orElseThrow();
        var userID = jwtService.extractUserId(JWT);
        if(!content.getCreatorID().equals(userID)){
            throw new SivantisException("You are not the owner of this video");
        }
        if(content.getIsComplete()){
            throw new SivantisException("Video is already complete");
        }
        var user = userRepository.findById(userID).orElseThrow();
        Duration validatedDuration = null;
        if(content.getContentType().equals("Short Film")){
            validatedDuration = isGreaterThanTenMinutes(completeVideoRequest.getDuration());
        }
        else if (content.getContentType().equals("Movies")) {
            validatedDuration = isGreaterThanOneHourAndFifteenMinutes(completeVideoRequest.getDuration());
        }
        incrementAllowedDevelopingVideos(user);
        Query query = new Query(Criteria.where("_id").is(content.getContentId()));

        Update update = new Update()
                .set("youtubeMainVideoID", completeVideoRequest.getYoutubeMainVideoID())
                .set("isComplete", true)
                .set("duration", validatedDuration);

        try {
            mongoTemplate.updateFirst(query, update, Content.class);
        } catch (DuplicateKeyException e) {
            throw new SivantisException("A user published a video with this YouTube ID already");
        }
    }

    private void incrementAllowedDevelopingVideos(Users user){
        Query query = new Query(Criteria.where("_id").is(user.getUserId()));
        Update update = new Update().inc("allowedDevelopingVideos", 1);
        mongoTemplate.updateFirst(query, update, Users.class);
    }

    // @dev Generates content entity that is under development
    public Content saveInDevelopmentContent(ContentRequest contentRequest, String JWT) throws IOException, URISyntaxException, TransactionException, ExecutionException, InterruptedException {
        contentRequestChecks(contentRequest);
        if(!isAtLeastOneWeekFromToday(contentRequest.getReleaseDate())){
            throw new SivantisException("Release date must be at least a week from now");
        }
        var User = userRepository.findById(jwtService.extractUserId(JWT)).orElseThrow();
        if(User.getIsViolator()){
            throw new SivantisException("You're ability to create new In Development Builds has temporarily been revoked");
        }
        if(User.getAllowedDevelopingVideos() < 1){
            throw new SivantisException("You don't have any available Development Tokens left");
        }
        if(User.getPersonalWallet().isEmpty()){
            throw new SivantisException("Must have a personal wallet on file");
        }
        if(!contentRequest.getLiveBroadcastContent().equals("none")){
            throw new SivantisException("Selected video cannot be a livestream");
        }
        var Content = mapContentRequest(contentRequest,User.getUserId(), false);
        Content.setContentEnum(ContentEnum.PendingConfirmation);
        if(isAuction(Content.getContentType())){
            contractServiceInterface.createNewAuction(Content.getContentId(), Content.getNumbBidders(), Content.getStartingCost(), User.getPersonalWallet());
        }
        else {
            if (isNotYetCreatorOrPending(User)) {
                // Mark content as pending until on-chain addContentCreator finishes
                Content.setPendingCreatorApproval(true);

                // Only queue blockchain tx once
                if (isNotContentCreator(User.getUserId())) {
                    contractServiceInterface.addContentCreator(User.getUserId(), User.getPersonalWallet(), User.getRank());
                }
            } else if (!User.isContentCreator()) {
                // User is pending — still mark content as PendingConfirmation
                Content.setPendingCreatorApproval(true);
            } else {
                // User is already a content creator
                Content.setContentEnum(ContentEnum.Active);
                Content.setActiveDate(Instant.now());
            }
        }
        Content.setIsComplete(false);
        contentRepository.save(Content);
        increaseVideoPostedAndAddContentIdAndDecrementAllowedDevelopingVideos(User, Content.getContentId());
        return Content;
    }

    private void increaseVideoPostedAndAddContentIdAndDecrementAllowedDevelopingVideos(Users user, String contentID){
        Query query = new Query(Criteria.where("_id").is(user.getUserId()));
        Update update = new Update()
                .inc("videosPosted", 1)
                .push("createdContent", contentID)
                .inc("allowedDevelopingVideos", -1);
        mongoTemplate.updateFirst(query, update, Users.class);
    }

    public void editVideo(EditVideoRequest editVideoRequest, String JWT){
        var content = contentRepository.findById(editVideoRequest.getContentID()).orElseThrow();
        if(!content.getCreatorID().equals(jwtService.extractUserId(JWT))){
            throw new SivantisException("You are not the owner of this video");
        }
        if(editVideoRequest.getDescription().length() > 5000){
            throw new SivantisException("The description exceeds the character limit of " + 5000);
        }
        if(editVideoRequest.getContentName().length() > 100){
            throw new SivantisException("The title exceeds the character limit of " + 100);
        }
        updateContent(content.getContentId(), editVideoRequest);
    }

    private void updateContent(String contentId, EditVideoRequest editVideoRequest){
        Query query = new Query(Criteria.where("_id").is(contentId));
        Update update = new Update()
                .set("contentName", editVideoRequest.getContentName())
                .set("description", editVideoRequest.getDescription())
                .set("youtubeTrailerVideoID", editVideoRequest.getYoutubeTrailerVideoID());
        mongoTemplate.updateFirst(query, update, Content.class);
    }

    public void deleteCreatedContent(String userID, String contentID){
        var content = contentRepository.findById(contentID).orElseThrow();
        if(!content.getCreatorID().equals(userID)){
            throw new SivantisException("You are not the owner of this video");
        }
        if(!content.getContentEnum().equals(ContentEnum.Inactive)){
            throw new SivantisException("Content must be Inactive to delete");
        }
        content.setYoutubeMainVideoID(null);
        removeUserCreatedContent(userID, contentID);
        contentRepository.save(content);
    }

    private void removeUserCreatedContent(String userId, String contentID){
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().pull("createdContent", contentID);
        mongoTemplate.updateFirst(query, update, Users.class);
    }


    // @dev Generates content entity that is under development
    public void reactivateContent(ReactivateContentRequest reactivateContentRequest, String JWT) {
        var content = contentRepository.findById(reactivateContentRequest.getContentID()).orElseThrow();
        var userID = jwtService.extractUserId(JWT);
        if(!content.getCreatorID().equals(userID)){
            throw new SivantisException("You are not the owner of this video");
        }
        var updatedContent = tryTransitionToPendingConfirmation(content.getContentId());
        if (updatedContent == null) {
            throw new SivantisException("Content must be Inactive to reactivate");
        }
        if(!isAtLeast3DaysFromToday(reactivateContentRequest.getReleaseDate())){
            throw new SivantisException("Release date must be at least 3 days from now");
        }
        if(content.getContentType().equals("Sports") || content.getContentType().equals("Concerts")){
            throw new SivantisException("Cannot reactivate video type of Sport OR Concert");
        }
        content.setContentEnum(ContentEnum.PendingConfirmation);
        if(content.getListOfBuyerIds().isEmpty()){
            content.setContentEnum(ContentEnum.Active);
            content.setActiveDate(Instant.now());
        }
        else if(isAuction(content.getContentType())){
            contractServiceInterface.reactivateAuction(content);
        }
        else {
            contractServiceInterface.reactivateContent(content);
        }
        var user = userRepository.findById(userID).orElseThrow();
        content.setListOfBuyerIds(new HashMap<>());
        content.setHype(BigDecimal.ZERO);
        content.setSentEmails(false);
        content.setContentReports(new ArrayList<>());
        if(user.getIsViolator()){
            content.setIsViolator(true);
        }
        content.setReleaseDate(convertToLocalDate(reactivateContentRequest.getReleaseDate()));
        contentRepository.save(content);
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "America/New_York")
    @SchedulerLock(name = "dailyCheckup", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void dailyCheckup() {
        var ActiveContent = getAllActiveAndInProgressContent();
        List<Content> returnAllManaContent = new ArrayList<>();
        List<Content> successfulVideoContent = new ArrayList<>();
        List<Content> releaseEmailsContent = new ArrayList<>();
        List<Content> failedToSendEmailsContent = new ArrayList<>();
        LocalDate currentDate = LocalDate.now();
        ec2InstanceTagService.markTransactionInProgress();
        try {
            for(Content content: ActiveContent) {
                var releaseDate = content.getReleaseDate();
                if (content.getIsComplete()) {
                    if (currentDate.isEqual(releaseDate.minusDays(1))) {
                        releaseEmailsContent.add(content);
                    }
                    else if (currentDate.isEqual(releaseDate.plusDays(1)) &&  !content.getSentEmails()) {
                        failedToSendEmailsContent.add(content);
                    }
                    else if (currentDate.isEqual(releaseDate.plusDays(2))) {
                        successfulVideoContent.add(content);
                    }
                }
                else {
                    if (currentDate.isEqual(releaseDate.minusDays(4))) {
                        messageService.warningMessage(content);
                    }
                    if (currentDate.isEqual(releaseDate.minusDays(2))) {
                        returnAllManaContent.add(content);
                    }
                }
            }
            if(!returnAllManaContent.isEmpty()){
                List<Content> allContentNeedingReturn = new ArrayList<>(returnAllManaContent);
                if(!failedToSendEmailsContent.isEmpty()){
                    allContentNeedingReturn.addAll(failedToSendEmailsContent);
                }
                serverSideMultiSendContractService.returnAllManaMultiCall(allContentNeedingReturn);
                for(Content content: returnAllManaContent){
                    messageService.failedVideoMessage(content);
                }
            }
            if(!successfulVideoContent.isEmpty()){
                serverSideMultiSendContractService.successfulVideoMultiCall(successfulVideoContent);
            }
            if(!releaseEmailsContent.isEmpty()){
                serverSideMultiSendContractService.releaseEmailsMultiCall(releaseEmailsContent);
            }
            if(!failedToSendEmailsContent.isEmpty()){
                messageService.failedToSendEmailsMessage(failedToSendEmailsContent);
            }
        } finally {
            ec2InstanceTagService.clearTransactionTag();
        }
    }

    @Scheduled(cron = "0 0 5,11,17,23 * * *", zone = "America/New_York")
    @SchedulerLock(name = "watchNowPayLaterPayments", lockAtMostFor = "PT2M", lockAtLeastFor = "PT30S")
    public void watchNowPayLaterPayments() {
        ec2InstanceTagService.markTransactionInProgress();
        try {
            var watchNowPayLaterPayments = watchNowPayLaterRepository.findAllByWatchNowPayLaterEnumAndNextPaymentDateBefore(WatchNowPayLaterEnum.Unpaid, Instant.now());
            serverSideMultiSendContractService.watchNowPayLaterPaymentsMultiCall(watchNowPayLaterPayments);
        } finally {
            ec2InstanceTagService.clearTransactionTag();
        }

    }

    public void sentVideos(SentVideoRequest sentVideoRequest, String JWT){
        var content = contentRepository.findById(sentVideoRequest.getContentID()).orElseThrow();
        if(!content.getCreatorID().equals(jwtService.extractUserId(JWT))){
            throw new SivantisException("You are not the owner of this content");
        }
        if(hasNotSentEmails(content.getContentId())){
            var message = messageRepository.findById(sentVideoRequest.getMessageID()).orElseThrow();
            message.setHasRead(true);
            messageRepository.save(message);
            messageService.sentVideo(content);
        }
    }

    private boolean hasNotSentEmails(String contentId) {
        Query query = new Query(Criteria
                .where("_id").is(contentId)
                .and("sentEmails").is(false));

        Update update = new Update()
                .set("sentEmails", true);

        Content updatedContent = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Content.class
        );

        return updatedContent != null;
    }
    public void reportVideo(ReportVideoRequest reportVideoRequest, String userID) {
        if(reportVideoRequest.getReport().isEmpty() || reportVideoRequest.getReport().length() > 300){
            throw new SivantisException("Invalid Report Description");
        }
        var content = contentRepository.findById(reportVideoRequest.getContentID()).orElseThrow();
        if(!content.getContentEnum().equals(ContentEnum.InProgress)){
            throw new SivantisException("This video is no longer In Progress");
        }
        double allBuyers;
        if(isAuction(content.getContentType())){
            if(!content.getListOfBuyerIds().containsKey(userID)){
                throw new SivantisException("Not a Buyer");
            }
            allBuyers = Math.min(content.getListOfBuyerIds().size(), content.getNumbBidders());
        }
        else {
            if(!content.getListOfBuyerIds().containsKey(channelRepository.findById(userRepository.findById(userID).orElseThrow().getChannels().get(0)).orElseThrow().getChannelName())){
                throw new SivantisException("Not a Buyer");
            }
            allBuyers = content.getListOfBuyerIds().size();
        }

        var report = buildReport(reportVideoRequest.getReport() ,userID);
        var index = findIndexByReporterID(content.getContentReports(), userID);
        if(index != -1){
            throw new SivantisException("Already Submitted A Report For This Video");
        }
        double reportRate = content.getContentReports().size() / allBuyers * 100;
        addReport(content.getContentId(), report, reportRate);
    }

    private void addReport(String contentId, ContentReports report,  Double reportRate){
        Query query = new Query(Criteria.where("_id").is(contentId));
        Update update = new Update()
                .addToSet("contentReports", report)
                .set("reportRate", reportRate);
        mongoTemplate.updateFirst(query, update, Content.class);
    }

    private ContentReports buildReport(String report ,String userID) {
        return ContentReports.builder()
                .report(report)
                .reporterID(userID)
                .isResolved(false)
                .timeStamp(Instant.now())
                .build();
    }

    private int findIndexByReporterID(List<ContentReports> contentReportsList, String reporterID) {
        for (int i = 0; i < contentReportsList.size(); i++) {
            if (contentReportsList.get(i).getReporterID().equals(reporterID)) {
                return i; // Return the index if reporterID matches
            }
        }
        return -1; // Return -1 to indicate that the reporterID was not found
    }

    public void deleteChannelPurchasedVideo(String userID, String contentID, String channelID) {
        var channel = channelRepository.findById(channelID).orElseThrow();
        if(!channel.getOwnerID().equals(userID)){
            throw new SivantisException("You are not the owner of this channel");
        }
        var content = contentRepository.findById(contentID).orElseThrow();
        var payment = paymentRepository.findById(channel.getPurchasedContent().get(contentID)).orElseThrow();
        if(!content.getContentEnum().equals(ContentEnum.Inactive) && !payment.getStatus().equals(PaymentEnum.RefundedPurchase)){
            throw new SivantisException("Content must be Inactive OR must be refunded to delete");
        }
        removeChannelPurchasedContent(channel.getChannelId(), contentID);
    }

    private void removeChannelPurchasedContent(String channelId, String contentID) {
        Query query = new Query(Criteria.where("_id").is(channelId));
        Update update = new Update().unset("purchasedContent." + contentID);
        mongoTemplate.updateFirst(query, update, Channels.class);
    }

    public void deletePurchasedVideo(String userID, String contentID) {
        var content = contentRepository.findById(contentID).orElseThrow();
        var user = userRepository.findById(userID).orElseThrow();
        var payment = paymentRepository.findById(user.getPurchasedContent().get(contentID)).orElseThrow();
        if(!content.getContentEnum().equals(ContentEnum.Inactive) && !payment.getStatus().equals(PaymentEnum.RefundedPurchase)){
            throw new SivantisException("Content must be Inactive OR must be refunded to delete");
        }
        removeUserPurchasedContent(userID, contentID);
    }

    private void removeUserPurchasedContent(String userId, String contentID){
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().pull("purchasedContent", contentID);
        mongoTemplate.updateFirst(query, update, Users.class);
    }


    // @dev Generates a new Content entity
    private Content mapContentRequest(ContentRequest contentRequest, String userID, Boolean isViolator) throws IOException, URISyntaxException {
        return Content.builder()
                .contentId(ObjectId.get().toHexString())
                .contentName(contentRequest.getContentName())
                .description(contentRequest.getDescription())
                .numbBidders(contentRequest.getNumbBidders())
                .startingCost(contentRequest.getStartingCost())
                .youtubeTrailerVideoID(contentRequest.getYoutubeTrailerVideoID())
                .youtubeMainVideoID(contentRequest.getYoutubeMainVideoID())
                .thumbnail(downloadImageAsDataUri(contentRequest.getThumbnail()))
                .createdDate(Instant.now())
                .contentType(contentRequest.getContentType())
                .releaseDate(convertToLocalDate(contentRequest.getReleaseDate()))
                .creatorID(userID)
                .listOfBuyerIds(new HashMap<>())
                .hype(BigDecimal.ZERO)
                .googleSubject(contentRequest.getGoogleSubject())
                .pendingCreatorApproval(false)
                .youtubeUsername(contentRequest.getYoutubeUsername())
                .youtubeProfilePicture(downloadImageAsDataUri(contentRequest.getYoutubeProfilePicture()))
                .isViolator(isViolator)
                .sentEmails(false)
                .activeDate(null)
                .contentReports(new ArrayList<>())
                .build();
    }


    // @dev Returns all the Content the user purchased
    public PurchasedContentResponse getUserPurchasedContent(String userId, String paymentDate) {
        var userPurchasedContentIds = userRepository.findById(userId).orElseThrow().getPurchasedContent().values();
        Sort sort = Sort.by("paymentDate").descending();
        var purchasedContentResponse = PurchasedContentResponse.builder()
                .content(new ArrayList<>())
                .payment(new ArrayList<>())
                .build();
        List<Payment> payments;

        if (paymentDate != null) {
            long timestampMillis = (long) (Double.parseDouble(paymentDate) * 1000); // Convert to milliseconds
            payments = paymentRepository.findByPaymentDateBeforeAndPaymentIdIn(Instant.ofEpochMilli(timestampMillis), userPurchasedContentIds, PageRequest.of(0, 50, sort));
        } else {
            payments = paymentRepository.findByPaymentIdIn(userPurchasedContentIds, PageRequest.of(0, 50, sort));
        }

        payments.forEach(payment -> {
            purchasedContentResponse.getContent().add(contentRepository.findById(payment.getContentId()).orElseThrow());
            purchasedContentResponse.getPayment().add(payment);
        });

        // Sort the mapping entries by paymentDate in descending order (most recent first)
        return purchasedContentResponse;
    }

    public PurchasedContentResponse getChannelPurchasedContent(String channelName, String paymentDate) {
        var channelPurchasedContentIds = channelRepository.findByChannelName(channelName).orElseThrow().getPurchasedContent().values();
        Sort sort = Sort.by("paymentDate").descending();
        var purchasedContentResponse = PurchasedContentResponse.builder()
                .content(new ArrayList<>())
                .payment(new ArrayList<>())
                .build();
        List<Payment> payments;

        if (paymentDate != null) {
            long timestampMillis = (long) (Double.parseDouble(paymentDate) * 1000); // Convert to milliseconds
            payments = paymentRepository.findByPaymentDateBeforeAndPaymentIdIn(Instant.ofEpochMilli(timestampMillis), channelPurchasedContentIds, PageRequest.of(0, 50, sort));
        } else {
            payments = paymentRepository.findByPaymentIdIn(channelPurchasedContentIds, PageRequest.of(0, 50, sort));
        }

        payments.forEach(payment -> {
            purchasedContentResponse.getContent().add(contentRepository.findById(payment.getContentId()).orElseThrow());
            purchasedContentResponse.getPayment().add(payment);
        });

        // Sort the mapping entries by paymentDate in descending order (most recent first)
        return purchasedContentResponse;
    }

    // @dev Returns all Content labeled 'Active' and 'InProgress'
    public List<Content> getAllActiveAndInProgressContent() {
        return contentRepository.findContentsByContentEnumIn(Arrays.asList(ContentEnum.Active, ContentEnum.InProgress));

    }

    public Map<String, Double> sortedBuyersByUsername(String contentID) {
        var listOfBuyers = contentRepository.findById(contentID).orElseThrow().getListOfBuyerIds();
        Map<String, Double> doubleMap = new HashMap<>();
        for (Map.Entry<String, String> entry : listOfBuyers.entrySet()) {
            if(userRepository.findById(entry.getKey()).isPresent()){
                String keyUsername = userRepository.findById(entry.getKey()).get().getUsername();
                String valueAsString = entry.getValue();
                var valueAsDouble = Double.parseDouble(valueAsString);
                doubleMap.put(keyUsername, valueAsDouble);
            }
        }
        List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(doubleMap.entrySet());
        sortedEntries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // Create a LinkedHashMap to preserve the insertion order
        Map<String, Double> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : sortedEntries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    public Map<String, Double> sortedBuyersChannelName(String contentID) {
        var listOfBuyers = contentRepository.findById(contentID).orElseThrow().getListOfBuyerIds();
        Map<String, Double> doubleMap = new HashMap<>();
        for (Map.Entry<String, String> entry : listOfBuyers.entrySet()) {
                String valueAsString = entry.getValue();
                var valueAsDouble = Double.parseDouble(valueAsString);
                doubleMap.put(entry.getKey(), valueAsDouble);

        }
        List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(doubleMap.entrySet());
        sortedEntries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // Create a LinkedHashMap to preserve the insertion order
        Map<String, Double> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : sortedEntries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }


    public Double getAllUserActiveHype(String userID){
        BigDecimal contentHype = BigDecimal.ZERO;
        var user = userRepository.findById(userID).orElseThrow();
        if(!user.getCreatedContent().isEmpty()){
            var activeContentHype =  contentRepository.findAllById(user.getCreatedContent()).stream()
                    .filter(content -> !content.getContentEnum().equals(ContentEnum.Inactive))
                    .map(Content::getHype)
                    .toList();
            for (BigDecimal hype : activeContentHype) {
                contentHype = contentHype.add(hype);
            }
        }
        return contentHype.doubleValue();
    }



    private void contentRequestChecks(ContentRequest contentRequest){
        if(!isCorrectContentType(contentRequest.getContentType())){
            throw new SivantisException("Invalid Content Type");
        }
        if(isAuction(contentRequest.getContentType())){
            if(contentRequest.getNumbBidders() < 1 || contentRequest.getNumbBidders() > maxNumberOfBuyers){
                throw new SivantisException("The number of bidders should be between: 1 and " + maxNumberOfBuyers);
            } else if (contentRequest.getStartingCost() < 5) {
                throw new SivantisException("The cost of this Auction should be greater than 5");
            }
        }
        if(contentRequest.getDescription().length() > 5000){
            throw new SivantisException("The description exceeds the character limit of " + 5000);
        }
        if(contentRequest.getContentName().length() > 100){
            throw new SivantisException("The title exceeds the character limit of " + 100);
        }
    }


    private Duration isGreaterThanTenMinutes(String isoDuration) {
        Duration duration = Duration.parse(isoDuration);
        Duration tenMinutes = Duration.ofMinutes(10);

        if(duration.compareTo(tenMinutes) < 0){
            throw new SivantisException("Content Must be at least 10 minutes");
        }
        return duration;
    }

    private Duration isGreaterThanOneHourAndFifteenMinutes(String isoDuration) {
        Duration duration = Duration.parse(isoDuration);
        Duration oneHourFifteenMinutes = Duration.ofHours(1).plusMinutes(15);

        if(duration.compareTo(oneHourFifteenMinutes) < 0){
            throw new SivantisException("Content Must be at least an Hour and 15 minutes");
        }
        return duration;
    }


    private boolean isCorrectContentType(String contentType){
        switch(contentType){
            case "Innovation", "Invention", "Short Film", "Sports", "Movies", "Concerts" -> {
                return true;
            }
        }
        return false;
    }

    private boolean isAuction(String contentType){
        return contentType.equals("Invention") || contentType.equals("Innovation");
    }

    public String downloadImageAsDataUri(String imageUrl) throws IOException {
        if (!isImageUrlValid(imageUrl)) {
            throw new IllegalArgumentException("Invalid image URL");
        }

        URI uri = URI.create(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();

        try (InputStream inputStream = connection.getInputStream()) {
            byte[] imageBytes = inputStream.readAllBytes();
            String base64EncodedImage = encodeBytesToBase64(imageBytes);
            return constructDataUri(base64EncodedImage);
        }
    }

    private boolean isImageUrlValid(String imageUrl) throws IOException {
        URI uri = URI.create(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("HEAD");

        try {
            connection.connect();
            String contentType = connection.getContentType();
            return contentType != null && contentType.startsWith("image/");
        } finally {
            connection.disconnect();
        }
    }

    private boolean isNotYetCreatorOrPending(Users user) {
        return !user.isContentCreator() && !user.isContentCreatorPending();
    }

    private boolean isNotContentCreator(String userId) {
        Query query = new Query(Criteria
                .where("_id").is(userId)
                .and("isContentCreator").is(false)
                .and("contentCreatorPending").is(false));

        Update update = new Update()
                .set("contentCreatorPending", true);

        Users updatedUser = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Users.class
        );

        return updatedUser != null;
    }

    private Content tryTransitionToPendingConfirmation(String contentId) {
        Query query = new Query(Criteria
                .where("_id").is(contentId)
                .and("contentEnum").is(ContentEnum.Inactive.name())); // stored as string

        Update update = new Update()
                .set("contentEnum", ContentEnum.PendingConfirmation.name());

        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Content.class
        );
    }

    private String encodeBytesToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String constructDataUri(String base64EncodedImage) {
        return "data:image/jpeg;base64," + base64EncodedImage;
    }

    private boolean notSuccessfulTransaction(ContractStatusEnum contractStatusEnum){
        return contractStatusEnum.equals(ContractStatusEnum.Error);
    }

    private boolean isAtLeast3DaysFromToday(DateInfo dateInfo) {
        if(isNotValidDate(dateInfo)){
            throw new SivantisException("Invalid release date");
        }
        LocalDate currentDate = LocalDate.now();
        LocalDate dateInfoDate = LocalDate.of(dateInfo.getYear(), dateInfo.getMonth(), dateInfo.getDay());

        LocalDate threeDaysFromNow = currentDate.plusDays(3);

        return dateInfoDate.isAfter(threeDaysFromNow) || dateInfoDate.isEqual(threeDaysFromNow);
    }

    private boolean isAtLeastOneWeekFromToday(DateInfo dateInfo) {
        if(isNotValidDate(dateInfo)){
            throw new SivantisException("Invalid release date");
        }
        LocalDate currentDate = LocalDate.now();
        LocalDate dateInfoDate = LocalDate.of(dateInfo.getYear(), dateInfo.getMonth(), dateInfo.getDay());

        LocalDate weekFromNow = currentDate.plusWeeks(1);

        return dateInfoDate.isAfter(weekFromNow) || dateInfoDate.isEqual(weekFromNow);
    }

    private boolean isNotValidDate(DateInfo dateInfo) {
        try {
            String dateString = String.format("%04d-%02d-%02d", dateInfo.getYear(), dateInfo.getMonth(), dateInfo.getDay());
            LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return false;
        } catch (DateTimeParseException e) {
            return true;
        }
    }

    public LocalDate convertToLocalDate(DateInfo dateInfo) {
        String dateString = String.format("%04d-%02d-%02d", dateInfo.getYear(), dateInfo.getMonth(), dateInfo.getDay());
        return LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private void addSivantisTokens(Users user){
        switch (user.getRank()) {
            case 2, 3, 4 -> user.setAllowedDevelopingVideos(user.getAllowedDevelopingVideos() + 1);
            case 5, 6, 7 -> user.setAllowedDevelopingVideos(user.getAllowedDevelopingVideos() + 2);
            case 8, 9, 10 -> user.setAllowedDevelopingVideos(user.getAllowedDevelopingVideos() + 3);
        }
    }



    private int rankValues(Users user){
        switch (user.getRank() + 1) {
            case 2 -> {
                return  1000;
            }
            case 3 -> {
                return 5000;
            }
            case 4 -> {
                return 10000;
            }
            case 5 -> {
                return 25000;
            }
            case 6 -> {
                return 50000;
            }
            case 7 -> {
                return 100000;
            }
            case 8 -> {
                return  250000;
            }
            case 9-> {
                return 500000;
            }
            case 10 -> {
                return 1000000;
            }
        }
        return 999999999;
    }


}
