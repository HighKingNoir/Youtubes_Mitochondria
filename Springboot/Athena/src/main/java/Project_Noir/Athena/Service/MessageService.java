package Project_Noir.Athena.Service;

import Project_Noir.Athena.Controller.ServerSideEventController;
import Project_Noir.Athena.DTO.FundChannelRequest;
import Project_Noir.Athena.DTO.UserWithdrawRequest;
import Project_Noir.Athena.Model.*;
import Project_Noir.Athena.Repo.*;
import com.auth0.jwt.JWT;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class MessageService {
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ContentRepository contentRepository;
    private final ChannelRepository channelRepository;
    private final ServerSideEventController serverSideEventController;
    private final PaymentRepository paymentRepository;
    private final MongoTemplate mongoTemplate;
    private final JwtService jwtService;


    public void newUserMessage(Users user){
        var Message = buildMessages(MessageEnum.NewUser);
        Message.getExtraInfo().add(user.getUsername());
        addMessageIdToUser(user.getUserId(), Message.getMessageId());
        messageRepository.save(Message);
    }

    public void fundChannelMessage(FundChannelRequest fundChannelRequest, String JWT){
        var userId = jwtService.extractUserId(JWT);
        var Message = buildMessages(MessageEnum.FundChannel, fundChannelRequest.getTransactionHash());
        Message.getExtraInfo().add(fundChannelRequest.getChannelName());
        Message.getExtraInfo().add(fundChannelRequest.getManaAmount());
        var dollarAmount = serverSideEventController.latestValue * Double.parseDouble(fundChannelRequest.getManaAmount());
        Message.getExtraInfo().add(String.valueOf(dollarAmount));
        addMessageIdToUser(userId, Message.getMessageId());
        messageRepository.save(Message);
    }

    public void rankUpMessage(Users user){
        var Message = buildMessages(MessageEnum.RankUp);
        Message.getExtraInfo().add(String.valueOf(user.getRank()));
        Message.getExtraInfo().add(rankNames(user.getRank()));
        addMessageIdToUser(user.getUserId(), Message.getMessageId());
        messageRepository.save(Message);
    }


    public void paymentMessage(String userId, Payment payment, Content content){
        var Message = buildMessages(MessageEnum.Payment, payment.getTransactionHash());
        Message.getExtraInfo().add(content.getThumbnail());
        Message.getExtraInfo().add(content.getContentName());
        Message.getExtraInfo().add(payment.getManaAmount());
        Message.getExtraInfo().add(String.valueOf(payment.getDollarAmount()));
        addMessageIdToUser(userId, Message.getMessageId());
        messageRepository.save(Message);
    }


    public void pendingPaymentChannelMessage(Channels channel, Payment payment, Content content) {
        var Message = buildMessages(MessageEnum.ChannelPendingPayment);
        Message.getExtraInfo().add(channel.getChannelName());
        Message.getExtraInfo().add(content.getThumbnail());
        Message.getExtraInfo().add(content.getContentName());
        Message.getExtraInfo().add(payment.getManaAmount());
        Message.getExtraInfo().add(String.valueOf(payment.getDollarAmount()));
        addMessageIdToUser(channel.getOwnerID(), Message.getMessageId());
        messageRepository.save(Message);
    }

    public void purchasedPaymentChannelMessage(String channelName, Payment payment, Content content) {
        var Message = buildMessages(MessageEnum.ChannelPayment, payment.getTransactionHash());
        Message.getExtraInfo().add(channelName);
        Message.getExtraInfo().add(content.getThumbnail());
        Message.getExtraInfo().add(content.getContentName());
        Message.getExtraInfo().add(payment.getManaAmount());
        Message.getExtraInfo().add(String.valueOf(payment.getDollarAmount()));
        addMessageIdToUser(payment.getUserId(), Message.getMessageId());
        messageRepository.save(Message);
    }

    public void purchasedPaymentChannelMessage(ArrayList<String> channelNamePurchases, ArrayList<Payment> purchasedPaymentsList, ArrayList<Content> purchasedContentList) {
        var Messages = new ArrayList<Messages>();
        for (int i = 0; i < channelNamePurchases.size(); i++) {
            var Message = buildMessages(MessageEnum.ChannelPayment, purchasedPaymentsList.get(i).getTransactionHash());
            Message.getExtraInfo().add(channelNamePurchases.get(i));
            Message.getExtraInfo().add(purchasedContentList.get(i).getThumbnail());
            Message.getExtraInfo().add(purchasedContentList.get(i).getContentName());
            Message.getExtraInfo().add(purchasedPaymentsList.get(i).getManaAmount());
            Message.getExtraInfo().add(String.valueOf(purchasedPaymentsList.get(i).getDollarAmount()));
            Messages.add(Message);
            addMessageIdToUser(purchasedPaymentsList.get(i).getUserId(), Message.getMessageId());
        }
        messageRepository.saveAll(Messages);
    }

    public void updatedPaymentMessage(Users user, Payment payment, Content content, String manaSpent, String dollarSpent){
        var Message = buildMessages(MessageEnum.UpdatedPayment, payment.getTransactionHash());
        Message.getExtraInfo().add(content.getThumbnail());
        Message.getExtraInfo().add(content.getContentName());
        Message.getExtraInfo().add(manaSpent);
        Message.getExtraInfo().add(dollarSpent);
        Message.getExtraInfo().add(payment.getManaAmount());
        addMessageIdToUser(user.getUserId(), Message.getMessageId());
        messageRepository.save(Message);
    }


    public void failedVideoMessage(Content content){
        content.setContentEnum(ContentEnum.Inactive);
        content.setContentReports(new ArrayList<>());
        contentRepository.save(content);
        if(userRepository.findById(content.getCreatorID()).isPresent()){
            var Message = buildMessages(MessageEnum.FailedVideo);
            var user = userRepository.findById(content.getCreatorID()).get();
            if(!content.getIsComplete()){
                setAllowedDevelopingVideos(user.getUserId(), user.getAllowedDevelopingVideos() + 1);
                content.setIsComplete(true);
                contentRepository.save(content);
            }
            Message.getExtraInfo().add(content.getThumbnail());
            Message.getExtraInfo().add(content.getContentName());
            Message.getExtraInfo().add(String.valueOf(content.getReleaseDate()));
            setAllUserContentToIsViolator(user);
            setViolatorStatus(user.getUserId(), true);
            addMessageIdToUser(user.getUserId(), Message.getMessageId());
            messageRepository.save(Message);
        }

    }

    public void warningMessage(Content content){
        var Message = buildMessages(MessageEnum.WarningMessage);
        Message.getExtraInfo().add(content.getThumbnail());
        Message.getExtraInfo().add(content.getContentName());
        addMessageIdToUser(content.getCreatorID(), Message.getMessageId());
        messageRepository.save(Message);
    }



    public void successfulVideoMessage(Content content){
        if(userRepository.findByCreatedContent(content.getContentId()).isPresent()){
            var Message = buildMessages(MessageEnum.SuccessfulVideo);
            var user = userRepository.findByCreatedContent(content.getContentId()).get();
            if(user.getIsViolator()){
                setViolatorStatus(user.getUserId(), false);
                setAllUserContentToIsNotViolator(user);
            }
            Message.getExtraInfo().add(content.getThumbnail());
            Message.getExtraInfo().add(content.getContentName());
            content.setContentEnum(ContentEnum.Inactive);
            content.setContentReports(new ArrayList<>());
            increaseUserHype(user.getUserId(), content.getHype().doubleValue());
            addMessageIdToUser(user.getUserId(), Message.getMessageId());
            contentRepository.save(content);
            messageRepository.save(Message);
        }
    }

    public void refundMessage(Users user, String transactionHash, String manaAmount, Content content){
        var Message = buildMessages(MessageEnum.Refund, transactionHash);
        Message.getExtraInfo().add(content.getThumbnail());
        Message.getExtraInfo().add(content.getContentName());
        Message.getExtraInfo().add(manaAmount);
        addMessageIdToUser(user.getUserId(), Message.getMessageId());
        messageRepository.save(Message);
    }

    public void refundMessage(List<Users> usersList, String transactionHash, List<String> manaAmountList, List<Content> contentList){
        List<Messages> messagesList = new ArrayList<>();
        for (int i = 0; i < usersList.size(); i++) {
            String userId = usersList.get(i).getUserId();
            var Message = buildMessages(MessageEnum.Refund, transactionHash);
            Message.getExtraInfo().add(contentList.get(i).getThumbnail());
            Message.getExtraInfo().add(contentList.get(i).getContentName());
            Message.getExtraInfo().add(manaAmountList.get(i));
            addMessageIdToUser(userId, Message.getMessageId());
            messagesList.add(Message);
        }
        messageRepository.saveAll(messagesList);
    }

    public void failedRefundMessage(Users user, String manaAmount, String contentID) {
        var Message = buildMessages(MessageEnum.FailedRefund);
        var content = contentRepository.findById(contentID).orElseThrow();
        Message.getExtraInfo().add(content.getThumbnail());
        Message.getExtraInfo().add(content.getContentName());
        Message.getExtraInfo().add(manaAmount);
        addMessageIdToUser(user.getUserId(), Message.getMessageId());
        messageRepository.save(Message);
    }

    public void refundChannelMessage(Users user, String transactionHash, String channelName, String manaAmount, Content content){
        var Message = buildMessages(MessageEnum.ChannelRefund, transactionHash);
        Message.getExtraInfo().add(channelName);
        Message.getExtraInfo().add(content.getThumbnail());
        Message.getExtraInfo().add(content.getContentName());
        Message.getExtraInfo().add(manaAmount);
        addMessageIdToUser(user.getUserId(), Message.getMessageId());
        messageRepository.save(Message);
    }

    public void refundChannelMessage(ArrayList<Users> canceledUserList, String transactionHash, ArrayList<String> channelNameCancellation, ArrayList<String> canceledManaAmounts, ArrayList<Content> canceledContentList) {
        List<Messages> messagesList = new ArrayList<>();
        for (int i = 0; i < canceledUserList.size(); i++) {
            String userId = canceledUserList.get(i).getUserId();
            var Message = buildMessages(MessageEnum.ChannelRefund, transactionHash);
            Message.getExtraInfo().add(channelNameCancellation.get(i));
            Message.getExtraInfo().add(canceledContentList.get(i).getThumbnail());
            Message.getExtraInfo().add(canceledContentList.get(i).getContentName());
            Message.getExtraInfo().add(canceledManaAmounts.get(i));
            addMessageIdToUser(userId, Message.getMessageId());
        }
        messageRepository.saveAll(messagesList);
    }

    public void pendingRefundChannelMessage(Users user, String channelName, String manaAmount, Content content){
        var Message = buildMessages(MessageEnum.ChannelPendingRefund);
        Message.getExtraInfo().add(channelName);
        Message.getExtraInfo().add(content.getThumbnail());
        Message.getExtraInfo().add(content.getContentName());
        Message.getExtraInfo().add(manaAmount);
        addMessageIdToUser(user.getUserId(), Message.getMessageId());
        messageRepository.save(Message);
    }

    public void failedRefundChannelMessage(Users user, String channelName, String manaAmount, String contentID) {
        if(contentRepository.findById(contentID).isPresent()){
            var Message = buildMessages(MessageEnum.FailedChannelRefund);
            var content = contentRepository.findById(contentID).get();
            Message.getExtraInfo().add(channelName);
            Message.getExtraInfo().add(content.getThumbnail());
            Message.getExtraInfo().add(content.getContentName());
            Message.getExtraInfo().add(manaAmount);
            addMessageIdToUser(user.getUserId(), Message.getMessageId());
            messageRepository.save(Message);
        }

    }

    public void failedToSendEmailsMessage(Content content) {
        content.setContentEnum(ContentEnum.Inactive);
        content.setContentReports(new ArrayList<>());
        contentRepository.save(content);
        var Message = buildMessages(MessageEnum.FailedToSendEmails);
        if(userRepository.findById(content.getCreatorID()).isPresent()){
            var user = userRepository.findById(content.getCreatorID()).orElseThrow();
            Message.getExtraInfo().add(content.getThumbnail());
            Message.getExtraInfo().add(content.getContentName());
            Message.getExtraInfo().add(String.valueOf(content.getReleaseDate()));
            if(!user.getIsViolator()){
                setAllUserContentToIsViolator(user);
                setViolatorStatus(user.getUserId(), true);
            }
            addMessageIdToUser(user.getUserId(), Message.getMessageId());
            messageRepository.save(Message);
        }

    }

    public void sentVideo(Content content) {
        if(isAuction(content.getContentType())){
            var sortedMapping  = convertToDoubleMapAndSort(content.getListOfBuyerIds());
            var userIDs = sortedMapping.keySet().stream().toList();
            if(userIDs.size() < content.getNumbBidders()){
                for (String userID: userIDs){
                    if(userRepository.findById(userID).isPresent()){
                        var user = userRepository.findById(userID).get();
                        var payment = paymentRepository.findById(user.getPurchasedContent().get(content.getContentId())).orElseThrow();
                        payment.setStatus(PaymentEnum.Purchased);
                        var Message = buildMessages(MessageEnum.SentVideo);
                        Message.getExtraInfo().add(content.getThumbnail());
                        Message.getExtraInfo().add(content.getContentName());
                        Message.getExtraInfo().add(content.getYoutubeMainVideoID());
                        Message.getExtraInfo().add(content.getContentId());
                        addMessageIdToUser(userID, Message.getMessageId());
                        paymentRepository.save(payment);
                        messageRepository.save(Message);
                    }
                }
            }
            else {
                for(int index = 0; index < content.getNumbBidders(); index++){
                    if(userRepository.findById(userIDs.get(index)).isPresent()){
                        var user = userRepository.findById(userIDs.get(index)).get();
                        var payment = paymentRepository.findById(user.getPurchasedContent().get(content.getContentId())).orElseThrow();
                        payment.setStatus(PaymentEnum.Purchased);
                        var Message = buildMessages(MessageEnum.SentVideo);
                        Message.getExtraInfo().add(content.getThumbnail());
                        Message.getExtraInfo().add(content.getContentName());
                        Message.getExtraInfo().add(content.getYoutubeMainVideoID());
                        addMessageIdToUser(user.getUserId(), Message.getMessageId());
                        paymentRepository.save(payment);
                        messageRepository.save(Message);
                    }
                }
            }
        }
        else {
            var channelNames = content.getListOfBuyerIds().keySet().stream().toList();
            for(String channelName: channelNames){
                if(channelRepository.findByChannelName(channelName).isPresent()){
                    var channel = channelRepository.findByChannelName(channelName).get();
                    if(userRepository.findById(channel.getOwnerID()).isPresent()){
                        var Message = buildMessages(MessageEnum.SentVideo);
                        Message.getExtraInfo().add(content.getThumbnail());
                        Message.getExtraInfo().add(content.getContentName());
                        Message.getExtraInfo().add(content.getYoutubeMainVideoID());
                        addMessageIdToUser(channel.getOwnerID(), Message.getMessageId());
                        messageRepository.save(Message);
                    }
                }
            }
        }
    }

    public void youtubeEmailsMessage(Users user, Content content){
        var Message = buildMessages(MessageEnum.YoutubeEmails);
        Message.getExtraInfo().add(content.getThumbnail());
        Message.getExtraInfo().add(content.getContentName());
        Message.getExtraInfo().add(content.getYoutubeMainVideoID());
        Message.getExtraInfo().add(content.getContentId());
        if(!content.getListOfBuyerIds().isEmpty()){
            if(isAuction(content.getContentType())){
                var sortedMapping  = convertToDoubleMapAndSort(content.getListOfBuyerIds());
                var userIDs = sortedMapping.keySet().stream().toList();
                var users = userRepository.findAllById(userIDs);
                if(userIDs.size() < content.getNumbBidders()){
                    for (Users winningUsers: users){
                        Message.getExtraInfo().add(winningUsers.getEmail());
                    }
                }
                else {
                    for(int index = 0; index < content.getNumbBidders(); index++){
                        Message.getExtraInfo().add(users.get(index).getEmail());
                    }
                }
            }
            else {
                var channelNames = content.getListOfBuyerIds().keySet().stream().toList();
                var Channels = channelRepository.findByChannelNameIn(channelNames);
                var userIds = Channels.stream().map(Project_Noir.Athena.Model.Channels::getOwnerID).toList();
                var users = userRepository.findAllById(userIds);
                for(Users channelOwner: users){
                    Message.getExtraInfo().add(channelOwner.getEmail());
                }
            }
        }
        if(Message.getExtraInfo().size() == 4){
            content.setSentEmails(true);
            content.setContentEnum(ContentEnum.Inactive);
        }
        else {
            content.setContentEnum(ContentEnum.InProgress);
        }
        addMessageIdToUser(user.getUserId(), Message.getMessageId());
        contentRepository.save(content);
        messageRepository.save(Message);
    }

    public void youtubeEmailsMessage(List<Users> users, List<Content> contents) {
        List<Content> contentToSave = new ArrayList<>();
        List<Messages> messagesToSave = new ArrayList<>();
        List<Payment> paymentsToSave = new ArrayList<>();
        // Preload users into map


        for (Content content : contents) {

            var message = buildMessages(MessageEnum.YoutubeEmails);
            message.getExtraInfo().add(content.getThumbnail());
            message.getExtraInfo().add(content.getContentName());
            message.getExtraInfo().add(content.getYoutubeMainVideoID());
            message.getExtraInfo().add(content.getContentId());

            if (!content.getListOfBuyerIds().isEmpty()) {
                if (isAuction(content.getContentType())) {
                    var sortedMapping = convertToDoubleMapAndSort(content.getListOfBuyerIds());
                    var userIDs = sortedMapping.keySet().stream().toList();
                    var buyers = userRepository.findAllById(userIDs);

                    if (userIDs.size() < content.getNumbBidders()) {
                        for (Users buyer : buyers) {
                            message.getExtraInfo().add(buyer.getEmail());
                        }
                    } else {
                        for (int i = 0; i < content.getNumbBidders(); i++) {
                            message.getExtraInfo().add(buyers.get(i).getEmail());
                        }
                    }

                } else {
                    var channelNames = content.getListOfBuyerIds().keySet().stream().toList();
                    var channels = channelRepository.findByChannelNameIn(channelNames);
                    var ownerIds = channels.stream().map(Project_Noir.Athena.Model.Channels::getOwnerID).toList();
                    var channelOwners = userRepository.findAllById(ownerIds);
                    for (Users channelOwner : channelOwners) {
                        message.getExtraInfo().add(channelOwner.getEmail());
                    }
                }
            }

            if (message.getExtraInfo().size() == 4) {
                content.setSentEmails(true);
                content.setContentEnum(ContentEnum.Inactive);
            } else {
                content.setContentEnum(ContentEnum.InProgress);
            }
            addMessageIdToUser(content.getCreatorID(), message.getMessageId());
            contentToSave.add(content);
            messagesToSave.add(message);
        }

        // Save all updated entities
        contentRepository.saveAll(contentToSave);
        messageRepository.saveAll(messagesToSave);
        paymentRepository.saveAll(paymentsToSave);
    }

    public void approvedChannelMessage(String channelName){
        if(channelRepository.findByChannelName(channelName).isPresent()){
            var channel = channelRepository.findByChannelName(channelName).get();
            channel.setChannelStatus(ChannelStatus.Approved);
            channel.setAwvIsUpdated(true);
            channel.setApprovedDate(Instant.now());
            channelRepository.save(channel);
            var Message = buildMessages(MessageEnum.ApprovedChannel);
            Message.getExtraInfo().add(channel.getChannelName());
            addMessageIdToUser(channel.getOwnerID(), Message.getMessageId());
            messageRepository.save(Message);
        }
    }

    public void approvedChannelMessage(ArrayList<String> approvedChannelNames) {
        var Messages = new ArrayList<Messages>();
        var allChannels = channelRepository.findByChannelNameIn(approvedChannelNames);
        for (Channels channel: allChannels){
            channel.setChannelStatus(ChannelStatus.Approved);
            channel.setAwvIsUpdated(true);
            channel.setApprovedDate(Instant.now());
            var Message = buildMessages(MessageEnum.ApprovedChannel);
            Message.getExtraInfo().add(channel.getChannelName());
            addMessageIdToUser(channel.getOwnerID(), Message.getMessageId());
            Messages.add(Message);
        }
        channelRepository.saveAll(allChannels);
        messageRepository.saveAll(Messages);
    }
    public void disapprovedChannelMessage(Channels channel, String reason){
        var Message = buildMessages(MessageEnum.DisapprovedChannel);
        Message.getExtraInfo().add(channel.getChannelName());
        Message.getExtraInfo().add(reason);
        addMessageIdToUser(channel.getOwnerID(), Message.getMessageId());
        messageRepository.save(Message);
    }


    public void userWithdraw(UserWithdrawRequest userWithdrawRequest, String JWT) {
        var userId = jwtService.extractUserId(JWT);
        var Message = buildMessages(MessageEnum.UserWithdraw, userWithdrawRequest.getTransactionHash());
        var manaPrice = serverSideEventController.latestValue;
        Message.getExtraInfo().add(String.valueOf(userWithdrawRequest.getDollarAmount()));
        Message.getExtraInfo().add(String.valueOf(userWithdrawRequest.getDollarAmount() / manaPrice));
        addMessageIdToUser(userId, Message.getMessageId());
        messageRepository.save(Message);
    }

    public void resolvedAuctionPayment(String contentID) {
        var content = contentRepository.findById(contentID).orElseThrow();
        var Message = buildMessages(MessageEnum.ResolvedAuctionPayment);
        Message.getExtraInfo().add(content.getThumbnail());
        Message.getExtraInfo().add(content.getContentName());
        addMessageIdToUser(contentID, Message.getMessageId());
        messageRepository.save(Message);
    }

    public void failedAuctionPayment(Content content) {
        if(userRepository.findById(content.getCreatorID()).isPresent()){
            var Message = buildMessages(MessageEnum.FailedAuctionPayment);
            Message.getExtraInfo().add(content.getThumbnail());
            Message.getExtraInfo().add(content.getContentName());
            addMessageIdToUser(content.getCreatorID(), Message.getMessageId());
            messageRepository.save(Message);
        }
    }

    private Messages buildMessages(MessageEnum messageEnum, String transactionHash){
        return Messages.builder()
                .messageId(ObjectId.get().toHexString())
                .messageEnum(messageEnum)
                .transactionHash(transactionHash)
                .hasRead(false)
                .creationDate(Instant.now())
                .extraInfo(new ArrayList<>())
                .build();
    }

    private Messages buildMessages(MessageEnum messageEnum){
        return Messages.builder()
                .messageId(ObjectId.get().toHexString())
                .messageEnum(messageEnum)
                .hasRead(false)
                .creationDate(Instant.now())
                .extraInfo(new ArrayList<>())
                .build();
    }

    public List<Messages> getAllUnreadMessages(String userID){
        return messageRepository.findAllById(userRepository.findById(userID).orElseThrow().getMessages()).stream().filter(messages -> messages.getHasRead().equals(false)).collect(Collectors.toList());
    }

    private boolean isAuction(String contentType){
        return contentType.equals("Invention") || contentType.equals("Innovation");
    }

    private void setAllUserContentToIsViolator(Users user){
        var allActiveContent = contentRepository.findAllById(user.getCreatedContent()).stream()
                .filter(content -> content.getContentEnum().equals(ContentEnum.Active))
                .toList();
        for(Content content: allActiveContent){
            content.setIsViolator(true);
            contentRepository.save(content);
        }
    }

    private void setAllUserContentToIsNotViolator(Users user){
        var allFlaggedContent = contentRepository.findAllById(user.getCreatedContent()).stream()
                .filter(Content::getIsViolator)
                .toList();
        for(Content content: allFlaggedContent){
            content.setIsViolator(false);
            contentRepository.save(content);
        }
    }


    private Map<String, Double> convertToDoubleMapAndSort(Map<String, String> listOfBuyers) {
        Map<String, Double> doubleMap = new HashMap<>();
        for (Map.Entry<String, String> entry : listOfBuyers.entrySet()) {
            String key = entry.getKey();
            String valueAsString = entry.getValue();
            var valueAsDouble = Double.parseDouble(valueAsString);
            doubleMap.put(key, valueAsDouble);
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

    private String rankNames(int userRank){
        switch (userRank) {
            case 2 -> {
                return  "Sentinel";
            }
            case 3 -> {
                return "Crusader";
            }
            case 4 -> {
                return "Enforcer";
            }
            case 5 -> {
                return "Templar";
            }
            case 6 -> {
                return "Paladin";
            }
            case 7 -> {
                return "Prodigy";
            }
            case 8 -> {
                return  "Titan";
            }
            case 9-> {
                return "Champion";
            }
            case 10 -> {
                return "High King";
            }
        }
        return "Emperor";
    }


    public void failedRefundChannelMessage(
            List<Users> users,
            List<String> channelNameList,
            List<String> manaAmounts,
            List<Content> contentList
    ) {
        List<Messages> messages = new ArrayList<>();
        for (int i = 0; i < contentList.size(); i++) {
            String userId = users.get(i).getUserId();
            var message = buildMessages(MessageEnum.FailedChannelRefund);
            message.getExtraInfo().add(channelNameList.get(i));
            message.getExtraInfo().add(contentList.get(i).getThumbnail());
            message.getExtraInfo().add(contentList.get(i).getContentName());
            message.getExtraInfo().add(manaAmounts.get(i));
            addMessageIdToUser(userId, message.getMessageId());
            messages.add(message);
        }
        messageRepository.saveAll(messages);
    }

    public void successfulVideoMessage(List<Content> successfulVideoContent) {
        List<Content> contentList = new ArrayList<>();
        List<Messages> messagesList = new ArrayList<>();
        for (Content content : successfulVideoContent) {
            var userOptional = userRepository.findByCreatedContent(content.getContentId());
            if (userOptional.isEmpty()) continue;
            var user = userOptional.get();
            var message = buildMessages(MessageEnum.SuccessfulVideo);
            message.getExtraInfo().add(content.getThumbnail());
            message.getExtraInfo().add(content.getContentName());
            if (user.getIsViolator()) {
                setViolatorStatus(user.getUserId(), false);
                setAllUserContentToIsNotViolator(user);
            }
            increaseUserHype(user.getUserId(), content.getHype().doubleValue());
            addMessageIdToUser(user.getUserId(), message.getMessageId());
            content.setContentEnum(ContentEnum.Inactive);
            content.setContentReports(new ArrayList<>());
            contentList.add(content);
            messagesList.add(message);
        }

        contentRepository.saveAll(contentList);
        messageRepository.saveAll(messagesList);
    }


    public void failedRefundMessage(List<Users> usersList, List<String> manaAmountList, List<Content> contentList) {
        List<Messages> messagesList = new ArrayList<>();
        for (int i = 0; i < usersList.size(); i++) {
            String userId = usersList.get(i).getUserId();
            var message = buildMessages(MessageEnum.FailedRefund);
            message.getExtraInfo().add(contentList.get(i).getThumbnail());
            message.getExtraInfo().add(contentList.get(i).getContentName());
            message.getExtraInfo().add(manaAmountList.get(i));
            addMessageIdToUser(userId, message.getMessageId());
            messagesList.add(message);
        }
        messageRepository.saveAll(messagesList);
    }

    public void failedToSendEmailsMessage(List<Content> failedToSendEmailsContent) {
        List<Messages> messages = new ArrayList<>();
        var userIDs = failedToSendEmailsContent.stream().map(Content::getCreatorID).toList();
        for (int i = 0; i < failedToSendEmailsContent.size(); i++) {
            if(userRepository.findById(userIDs.get(i)).isPresent()){
                var content = failedToSendEmailsContent.get(i);
                var user = userRepository.findById(userIDs.get(i)).get();
                var Message = buildMessages(MessageEnum.FailedToSendEmails);
                content.setContentEnum(ContentEnum.Inactive);
                content.setContentReports(new ArrayList<>());
                Message.getExtraInfo().add(content.getThumbnail());
                Message.getExtraInfo().add(content.getContentName());
                Message.getExtraInfo().add(String.valueOf(content.getReleaseDate()));
                addMessageIdToUser(user.getUserId(), Message.getMessageId());
                if(!user.getIsViolator()){
                    setAllUserContentToIsViolator(user);
                    setViolatorStatus(user.getUserId(), true);
                }
                messages.add(Message);
            }

        }
        contentRepository.saveAll(failedToSendEmailsContent);
        messageRepository.saveAll(messages);
    }


    public void addMessageIdToUser(String userId, String messageId) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().push("messages", messageId);
        mongoTemplate.findAndModify(
                query,
                update,
                Users.class
        );
    }

    public void increaseUserHype(String userId, Double addedHype) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().inc("totalHype", addedHype);

        mongoTemplate.findAndModify(
                query,
                update,
                Users.class
        );
    }

    public void setAllowedDevelopingVideos(String userId, Integer allowedVideos) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().set("allowedDevelopingVideos", allowedVideos);

        mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Users.class
        );
    }



    public void setViolatorStatus(String userId, boolean isViolator) {
        Query query = new Query(Criteria.where("_id").is(userId));

        Update update = new Update().set("isViolator", isViolator);

        mongoTemplate.findAndModify(
                query,
                update,
                Users.class
        );
    }
}
