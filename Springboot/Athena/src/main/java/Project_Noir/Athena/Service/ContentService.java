package Project_Noir.Athena.Service;

import Project_Noir.Athena.Controller.ServerSideEventController;
import Project_Noir.Athena.DTO.*;
import Project_Noir.Athena.Exception.SivantisException;
import Project_Noir.Athena.Model.*;
import Project_Noir.Athena.Repo.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.protocol.exceptions.TransactionException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ExecutionException;


//the user should be able to bid lower than the lowest bid but display a pop up saying "youre unlickly to get the url"

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
    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;
    private final MessageService messageService;
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
        if(contentRepository.findByYoutubeMainVideoID(contentRequest.getYoutubeMainVideoID()).isPresent()){
            throw new SivantisException("A user published a video with this youtube ID already");
        }
        var Content = mapContentRequest(contentRequest, User.getUserId(),User.getIsViolator());
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
        if(isAuction(Content.getContentType())){
            var status = contractServiceInterface.createNewAuction(Content.getContentId(), Content.getNumbBidders(), Content.getStartingCost(), User.getPersonalWallet());
            if(notSuccessfulTransaction(status)){
                throw new SivantisException("Transaction creating Auction failed. Try again later");
            }
            if(contentRequest.getLiveBroadcastContent().equals("none")){
                Content.setDuration(isGreaterThanTenMinutes(contentRequest.getDuration()));
            }
        }
        else if(!User.isContentCreator()) {
            var status = contractServiceInterface.addContentCreator(User.getUserId(), User.getPersonalWallet(), User.getRank());
            if(notSuccessfulTransaction(status)){
                throw new SivantisException("Transaction creating Content Creator failed. Try again later");
            }
            User.setContentCreator(true);
        }
        Content.setContentEnum(ContentEnum.Active);
        Content.setIsComplete(true);
        User.setVideosPosted(User.getVideosPosted() + 1);
        User.getCreatedContent().add(Content.getContentId());
        contentRepository.save(Content);
        userRepository.save(User);
        return Content;
    }

    public void completeVideo(CompleteVideoRequest completeVideoRequest, String JWT){
        if(contentRepository.findByYoutubeMainVideoID(completeVideoRequest.getYoutubeMainVideoID()).isPresent()){
            throw new SivantisException("A user published a video with this youtube ID already");
        }
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
        if(content.getContentType().equals("Short Film")){
            content.setDuration(isGreaterThanTenMinutes(completeVideoRequest.getDuration()));
        }
        else if (content.getContentType().equals("Movies")) {
            content.setDuration(isGreaterThanOneHourAndFifteenMinutes(completeVideoRequest.getDuration()));
        }
        user.setAllowedDevelopingVideos(user.getAllowedDevelopingVideos() + 1);
        content.setYoutubeMainVideoID((completeVideoRequest.getYoutubeMainVideoID()));
        content.setIsComplete(true);
        userRepository.save(user);
        contentRepository.save(content);
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
        if(isAuction(Content.getContentType())){
            var status = contractServiceInterface.createNewAuction(Content.getContentId(), Content.getNumbBidders(), Content.getStartingCost(), User.getPersonalWallet());
            if(notSuccessfulTransaction(status)){
                throw new SivantisException("Error: Transaction creating Auction failed");
            }
        }
        else if(!User.isContentCreator()) {
            var status = contractServiceInterface.addContentCreator(User.getUserId(), User.getPersonalWallet(), User.getRank());
            if(notSuccessfulTransaction(status)){
                throw new SivantisException("Error: Transaction creating Content Creator failed");
            }
            User.setContentCreator(true);
        }
        Content.setContentEnum(ContentEnum.Active);
        Content.setIsComplete(false);
        User.setVideosPosted(User.getVideosPosted() + 1);
        User.getCreatedContent().add(Content.getContentId());
        User.setAllowedDevelopingVideos(User.getAllowedDevelopingVideos() - 1);
        userRepository.save(User);
        contentRepository.save(Content);
        return Content;
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
        content.setContentName(editVideoRequest.getContentName());
        content.setDescription(editVideoRequest.getDescription());
        content.setYoutubeTrailerVideoID(editVideoRequest.getYoutubeTrailerVideoID());
        contentRepository.save(content);
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
        var user = userRepository.findById(userID).orElseThrow();
        user.getCreatedContent().remove(contentID);
        userRepository.save(user);
        contentRepository.save(content);
    }


    // @dev Generates content entity that is under development
    public void reactivateContent(ReactivateContentRequest reactivateContentRequest, String JWT) {
        var content = contentRepository.findById(reactivateContentRequest.getContentID()).orElseThrow();
        var userID = jwtService.extractUserId(JWT);
        if(!content.getCreatorID().equals(userID)){
            throw new SivantisException("You are not the owner of this video");
        }
        if(!content.getContentEnum().equals(ContentEnum.Inactive)){
            throw new SivantisException("Content must be Inactive to reactivate");
        }
        if(!isAtLeast3DaysFromToday(reactivateContentRequest.getReleaseDate())){
            throw new SivantisException("Release date must be at least 3 days from now");
        }
        if(content.getContentType().equals("Sports") || content.getContentType().equals("Concerts")){
            throw new SivantisException("Cannot reactivate video type of Sport OR Concert");
        }
        if(isAuction(content.getContentType())){
            contractServiceInterface.reactivateAuction(content);
        }
        else {
            contractServiceInterface.reactivateContent(content);
        }
        var user = userRepository.findById(userID).orElseThrow();
        content.setContentEnum(ContentEnum.Active);
        content.setListOfBuyerIds(new HashMap<>());
        content.setHype(0.0);
        content.setActiveDate(Instant.now());
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

        LocalDate currentDate = LocalDate.now();

        for(Content content: ActiveContent) {
            var releaseDate = content.getReleaseDate();
            if (content.getIsComplete()) {
                if (currentDate.isEqual(releaseDate.minusDays(1))) {
                    releaseEmails(content);
                } else if (currentDate.isEqual(releaseDate.plusDays(1)) &&  !content.getSentEmails()) {
                    failedToSendEmails(content);
                } else if (currentDate.isEqual(releaseDate.plusDays(2))) {
                    successfulVideo(content);
                }
            } else {
                if (currentDate.isEqual(releaseDate.minusDays(4))) {
                    messageService.warningMessage(content);
                }
                if (currentDate.isEqual(releaseDate.minusDays(2))) {
                   returnAllMana(content);
                   messageService.failedVideoMessage(content);

                }
            }
        }
    }

    @Scheduled(cron = "0 0 5,11,17,23 * * *", zone = "America/New_York")
    @SchedulerLock(name = "watchNowPayLaterPayments", lockAtMostFor = "PT2M", lockAtLeastFor = "PT30S")
    public void watchNowPayLaterPayments() {
        var watchNowPayLaterPayments = watchNowPayLaterRepository.findAllByWatchNowPayLaterEnumAndNextPaymentDateBefore(WatchNowPayLaterEnum.Unpaid, Instant.now());
        for(WatchNowPayLater watchNowPayLater : watchNowPayLaterPayments){
            contractServiceInterface.watchNowPayLaterPayment(watchNowPayLater.getWatchNowPlayLaterId(),serverSideEventController.latestValue);
        }
    }

    public void releaseEmails(Content content) {
        if(userRepository.findById(content.getCreatorID()).isPresent()){
            if(isAuction(content.getContentType())){
                contractServiceInterface.auctionReleaseDate(content);
            }
            var user = userRepository.findById(content.getCreatorID()).get();
            messageService.youtubeEmailsMessage(user, content);
        }
    }

    public void failedToSendEmails(Content content)  {
        if(!content.getSentEmails()) {
            returnAllMana(content);
            messageService.failedToSendEmailsMessage(content);
        }
    }

    public void sentVideos(SentVideoRequest sentVideoRequest, String JWT){
        var content = contentRepository.findById(sentVideoRequest.getContentID()).orElseThrow();
        if(!content.getCreatorID().equals(jwtService.extractUserId(JWT))){
            throw new SivantisException("You are not the owner of this message");
        }
        if(!content.getSentEmails()){
            var message = messageRepository.findById(sentVideoRequest.getMessageID()).orElseThrow();
            message.setHasRead(true);
            content.setSentEmails(true);
            messageRepository.save(message);
            contentRepository.save(content);
            messageService.sentVideo(content);
        }
    }

    public void successfulVideo(Content content) {
        if(isAuction(content.getContentType())){
            var successfulTransaction = contractServiceInterface.successfulAuction(content.getContentId(), content.getListOfBuyerIds(), content.getNumbBidders());
            if(!successfulTransaction){
                messageService.failedAuctionPayment(content);
            }
        }
        messageService.successfulVideoMessage(content);
        if(userRepository.findById(content.getCreatorID()).isPresent()){
            var user = userRepository.findById(content.getCreatorID()).get();
            if(user.getRank() < 11 && user.getTotalHype() >= rankValues(user)){
                if(user.isContentCreator()){
                    contractServiceInterface.increaseCreatorRank(user.getUserId());
                }
                user.setRank(user.getRank() + 1);
                addSivantisTokens(user);
                messageService.rankUpMessage(user);
            }
        }

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
        content.getContentReports().add(report);
        double totalReports = content.getContentReports().size();
        content.setReportRate(totalReports / allBuyers * 100);
        contentRepository.save(content);
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

    public void returnAllMana(Content content) {
        if(isAuction(content.getContentType())){
            contractServiceInterface.failAuction(content.getContentId(), content.getListOfBuyerIds());
        }
        else{
            for (Map.Entry<String, String> buyer : content.getListOfBuyerIds().entrySet()) {
                var optionalWatchNowPayLater = watchNowPayLaterRepository.findByChannelNameAndContentID(buyer.getKey(), content.getContentId());
                if(optionalWatchNowPayLater.isPresent() && optionalWatchNowPayLater.get().getWatchNowPayLaterEnum().equals(WatchNowPayLaterEnum.Unpaid)){
                    var watchNowPayLater = optionalWatchNowPayLater.get();
                    contractServiceInterface.sendWatchNowPayLaterRefundPayment(watchNowPayLater, content.getCreatorID(), content.getContentId(), buyer.getKey(), buyer.getValue());
                }
                else{
                    contractServiceInterface.sendRefundPayment(content.getCreatorID(), content.getContentId(), buyer.getKey(), buyer.getValue());
                }
            }

        }
    }

    public void deleteChannelPurchasedVideo(String userID, String contentID, String channelID) {
        var channel = channelRepository.findById(channelID).orElseThrow();
        if(!channel.getOwnerID().equals(userID)){
            throw new SivantisException("You are not the owner of this message");
        }
        var content = contentRepository.findById(contentID).orElseThrow();
        var payment = paymentRepository.findById(channel.getPurchasedContent().get(contentID)).orElseThrow();
        if(!content.getContentEnum().equals(ContentEnum.Inactive) && !payment.getStatus().equals(PaymentEnum.RefundedPurchase)){
            throw new SivantisException("Content must be Inactive OR must be refunded to delete");
        }
        channel.getPurchasedContent().remove(contentID);
        channelRepository.save(channel);
    }

    public void deletePurchasedVideo(String userID, String contentID) {
        var user = userRepository.findById(userID).orElseThrow();
        var content = contentRepository.findById(contentID).orElseThrow();
        var payment = paymentRepository.findById(user.getPurchasedContent().get(contentID)).orElseThrow();
        if(!content.getContentEnum().equals(ContentEnum.Inactive) && !payment.getStatus().equals(PaymentEnum.RefundedPurchase)){
            throw new SivantisException("Content must be Inactive OR must be refunded to delete");
        }
        user.getPurchasedContent().remove(contentID);
        userRepository.save(user);
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
                .hype(0.0)
                .googleSubject(contentRequest.getGoogleSubject())
                .youtubeUsername(contentRequest.getYoutubeUsername())
                .youtubeProfilePicture(downloadImageAsDataUri(contentRequest.getYoutubeProfilePicture()))
                .isViolator(isViolator)
                .sentEmails(false)
                .activeDate(Instant.now())
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
        var contentHype = 0.0;
        var user = userRepository.findById(userID).orElseThrow();
        if(!user.getCreatedContent().isEmpty()){
            var activeContentHype =  contentRepository.findAllById(user.getCreatedContent()).stream()
                    .filter(content -> !content.getContentEnum().equals(ContentEnum.Inactive))
                    .map(Content::getHype)
                    .toList();
            for (Double hype : activeContentHype) {
                contentHype += hype;
            }
        }
        return contentHype;
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
