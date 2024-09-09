package Project_Noir.Athena.Service;

import Project_Noir.Athena.Controller.ServerSideEventController;
import Project_Noir.Athena.DTO.FundChannelRequest;
import Project_Noir.Athena.DTO.UserWithdrawRequest;
import Project_Noir.Athena.Model.*;
import Project_Noir.Athena.Repo.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
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
    private final JwtService jwtService;


    public void newUserMessage(Users user){
        var Message = buildMessages(MessageEnum.NewUser);
        Message.getExtraInfo().add(user.getUsername());
        user.getMessages().add(Message.getMessageId());
        userRepository.save(user);
        messageRepository.save(Message);
    }

    public void fundChannelMessage(FundChannelRequest fundChannelRequest, String JWT){
        var user = userRepository.findById(jwtService.extractUserId(JWT)).orElseThrow();
        var Message = buildMessages(MessageEnum.FundChannel, fundChannelRequest.getTransactionHash());
        Message.getExtraInfo().add(fundChannelRequest.getChannelName());
        Message.getExtraInfo().add(fundChannelRequest.getManaAmount());
        var dollarAmount = serverSideEventController.latestValue * Double.parseDouble(fundChannelRequest.getManaAmount());
        Message.getExtraInfo().add(String.valueOf(dollarAmount));
        user.getMessages().add(Message.getMessageId());
        messageRepository.save(Message);
        userRepository.save(user);
    }

    public void rankUpMessage(Users user){
        var Message = buildMessages(MessageEnum.RankUp);
        Message.getExtraInfo().add(String.valueOf(user.getRank()));
        Message.getExtraInfo().add(rankNames(user.getRank()));
        user.getMessages().add(Message.getMessageId());
        messageRepository.save(Message);
        userRepository.save(user);
    }


    public void paymentMessage(Users user, Payment payment, Content content){
        var Message = buildMessages(MessageEnum.Payment, payment.getTransactionHash());
        Message.getExtraInfo().add(content.getThumbnail());
        Message.getExtraInfo().add(content.getContentName());
        Message.getExtraInfo().add(payment.getManaAmount());
        Message.getExtraInfo().add(String.valueOf(payment.getDollarAmount()));
        user.getMessages().add(Message.getMessageId());
        userRepository.save(user);
        messageRepository.save(Message);
    }

    public void paymentChannelMessage(Channels channel, Payment payment, Users user, Content content) {
        var Message = buildMessages(MessageEnum.ChannelPayment, payment.getTransactionHash());
        Message.getExtraInfo().add(channel.getChannelName());
        Message.getExtraInfo().add(content.getThumbnail());
        Message.getExtraInfo().add(content.getContentName());
        Message.getExtraInfo().add(payment.getManaAmount());
        Message.getExtraInfo().add(String.valueOf(payment.getDollarAmount()));
        user.getMessages().add(Message.getMessageId());
        userRepository.save(user);
        channelRepository.save(channel);
        messageRepository.save(Message);
    }

    public void updatedPaymentMessage(Users user, Payment payment, Content content, String manaSpent, String dollarSpent){
        var Message = buildMessages(MessageEnum.UpdatedPayment, payment.getTransactionHash());
        Message.getExtraInfo().add(content.getThumbnail());
        Message.getExtraInfo().add(content.getContentName());
        Message.getExtraInfo().add(manaSpent);
        Message.getExtraInfo().add(dollarSpent);
        Message.getExtraInfo().add(payment.getManaAmount());
        user.getMessages().add(Message.getMessageId());
        userRepository.save(user);
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
                user.setAllowedDevelopingVideos(user.getAllowedDevelopingVideos() + 1);
                content.setIsComplete(true);
                contentRepository.save(content);
            }
            Message.getExtraInfo().add(content.getThumbnail());
            Message.getExtraInfo().add(content.getContentName());
            Message.getExtraInfo().add(String.valueOf(content.getReleaseDate()));
            setAllUserContentToIsViolator(user);
            user.setIsViolator(true);
            user.getMessages().add(Message.getMessageId());
            userRepository.save(user);
            messageRepository.save(Message);
        }

    }

    public void warningMessage(Content content){
        if(userRepository.findById(content.getCreatorID()).isPresent()){
            var user = userRepository.findById(content.getCreatorID()).get();
            var Message = buildMessages(MessageEnum.WarningMessage);
            Message.getExtraInfo().add(content.getThumbnail());
            Message.getExtraInfo().add(content.getContentName());
            user.getMessages().add(Message.getMessageId());
            userRepository.save(user);
            messageRepository.save(Message);
        }
    }



    public void successfulVideoMessage(Content content){
        if(userRepository.findByCreatedContent(content.getContentId()).isPresent()){
            var Message = buildMessages(MessageEnum.SuccessfulVideo);
            var user = userRepository.findByCreatedContent(content.getContentId()).get();
            if(user.getIsViolator()){
                user.setIsViolator(false);
                setAllUserContentToIsNotViolator(user);
            }
            Message.getExtraInfo().add(content.getThumbnail());
            Message.getExtraInfo().add(content.getContentName());
            content.setContentEnum(ContentEnum.Inactive);
            content.setContentReports(new ArrayList<>());
            user.setTotalHype(user.getTotalHype() + content.getHype());
            user.getMessages().add(Message.getMessageId());
            contentRepository.save(content);
            userRepository.save(user);
            messageRepository.save(Message);
        }
    }

    public void refundMessage(Users user, String transactionHash, String manaAmount, Content content){
        var Message = buildMessages(MessageEnum.Refund, transactionHash);
        user.getMessages().add(Message.getMessageId());
        Message.getExtraInfo().add(content.getThumbnail());
        Message.getExtraInfo().add(content.getContentName());
        Message.getExtraInfo().add(manaAmount);
        userRepository.save(user);
        messageRepository.save(Message);
    }

    public void failedRefundMessage(Users user, String manaAmount, String contentID) {
        var Message = buildMessages(MessageEnum.FailedRefund);
        user.getMessages().add(Message.getMessageId());
        var content = contentRepository.findById(contentID).orElseThrow();
        Message.getExtraInfo().add(content.getThumbnail());
        Message.getExtraInfo().add(content.getContentName());
        Message.getExtraInfo().add(manaAmount);
        userRepository.save(user);
        messageRepository.save(Message);
    }

    public void refundChannelMessage(Users user, String transactionHash, String channelName, String manaAmount, Content content){
        var Message = buildMessages(MessageEnum.ChannelRefund, transactionHash);
        user.getMessages().add(Message.getMessageId());
        Message.getExtraInfo().add(channelName);
        Message.getExtraInfo().add(content.getThumbnail());
        Message.getExtraInfo().add(content.getContentName());
        Message.getExtraInfo().add(manaAmount);
        userRepository.save(user);
        messageRepository.save(Message);
    }

    public void failedRefundChannelMessage(Users user, String channelName, String manaAmount, String contentID) {
        if(contentRepository.findById(contentID).isPresent()){
            var Message = buildMessages(MessageEnum.FailedChannelRefund);
            user.getMessages().add(Message.getMessageId());
            var content = contentRepository.findById(contentID).get();
            Message.getExtraInfo().add(channelName);
            Message.getExtraInfo().add(content.getThumbnail());
            Message.getExtraInfo().add(content.getContentName());
            Message.getExtraInfo().add(manaAmount);
            userRepository.save(user);
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
            user.getMessages().add(Message.getMessageId());
            Message.getExtraInfo().add(content.getThumbnail());
            Message.getExtraInfo().add(content.getContentName());
            Message.getExtraInfo().add(String.valueOf(content.getReleaseDate()));
            setAllUserContentToIsViolator(user);
            user.setIsViolator(true);
            userRepository.save(user);
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
                        user.getMessages().add(Message.getMessageId());
                        paymentRepository.save(payment);
                        userRepository.save(user);
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
                        user.getMessages().add(Message.getMessageId());
                        paymentRepository.save(payment);
                        userRepository.save(user);
                        messageRepository.save(Message);
                    }
                }
            }

        }
        else {
            var userIDs = content.getListOfBuyerIds().keySet().stream().toList();
            for(int i = 0; i < content.getListOfBuyerIds().size(); i++){
                if(userRepository.findById(userIDs.get(i)).isPresent()){
                    var user = userRepository.findById(userIDs.get(i)).get();
                    var Message = buildMessages(MessageEnum.SentVideo);
                    var payment = paymentRepository.findById(user.getPurchasedContent().get(content.getContentId())).orElseThrow();
                    payment.setStatus(PaymentEnum.Purchased);
                    Message.getExtraInfo().add(content.getThumbnail());
                    Message.getExtraInfo().add(content.getContentName());
                    Message.getExtraInfo().add(content.getYoutubeMainVideoID());
                    user.getMessages().add(Message.getMessageId());
                    paymentRepository.save(payment);
                    userRepository.save(user);
                    messageRepository.save(Message);
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
                if(userIDs.size() < content.getNumbBidders()){
                    for(String userID: userIDs){
                        if(userRepository.findById(userID).isPresent()){
                            Message.getExtraInfo().add(userRepository.findById(userID).get().getEmail());
                        }
                    }
                }
                else {
                    for(int index = 0; index < content.getNumbBidders(); index++){
                        if(userRepository.findById(userIDs.get(index)).isPresent()){
                            Message.getExtraInfo().add(userRepository.findById(userIDs.get(index)).get().getEmail());
                        }
                    }
                }

            }
            else {
                var channelNames = content.getListOfBuyerIds().keySet().stream().toList();
                for(int index = 0; index < content.getListOfBuyerIds().size(); index++){
                    if(channelRepository.findByChannelName(channelNames.get(index)).isPresent()){
                        var channelOwner = channelRepository.findByChannelName(channelNames.get(index)).get().getOwnerID();
                        Message.getExtraInfo().add(userRepository.findById(channelOwner).orElseThrow().getEmail());
                    }
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
        user.getMessages().add(Message.getMessageId());
        contentRepository.save(content);
        userRepository.save(user);
        messageRepository.save(Message);
    }

    public void approvedChannelMessage(Channels channel){
        var user = userRepository.findById(channel.getOwnerID()).orElseThrow();
        var Message = buildMessages(MessageEnum.ApprovedChannel);
        Message.getExtraInfo().add(channel.getChannelName());
        user.getMessages().add(Message.getMessageId());
        userRepository.save(user);
        messageRepository.save(Message);
    }

    public void disapprovedChannelMessage(Channels channel, String reason){
        var user = userRepository.findById(channel.getOwnerID()).orElseThrow();
        var Message = buildMessages(MessageEnum.DisapprovedChannel);
        Message.getExtraInfo().add(channel.getChannelName());
        Message.getExtraInfo().add(reason);
        user.getMessages().add(Message.getMessageId());
        userRepository.save(user);
        messageRepository.save(Message);
    }


    public void userWithdraw(UserWithdrawRequest userWithdrawRequest, String JWT) {
        var user = userRepository.findById(jwtService.extractUserId(JWT)).orElseThrow();
        var Message = buildMessages(MessageEnum.UserWithdraw, userWithdrawRequest.getTransactionHash());
        var manaPrice = serverSideEventController.latestValue;
        Message.getExtraInfo().add(String.valueOf(userWithdrawRequest.getDollarAmount() * .85));
        Message.getExtraInfo().add(String.valueOf(userWithdrawRequest.getDollarAmount() / manaPrice * .85));
        user.getMessages().add(Message.getMessageId());
        userRepository.save(user);
        messageRepository.save(Message);
    }

    public void resolvedAuctionPayment(String contentID, String userID) {
        var user = userRepository.findById(userID).orElseThrow();
        var content = contentRepository.findById(contentID).orElseThrow();
        var Message = buildMessages(MessageEnum.ResolvedAuctionPayment);
        Message.getExtraInfo().add(content.getThumbnail());
        Message.getExtraInfo().add(content.getContentName());
        user.getMessages().add(Message.getMessageId());
        userRepository.save(user);
        messageRepository.save(Message);
    }

    public void failedAuctionPayment(Content content) {
        if(userRepository.findById(content.getCreatorID()).isPresent()){
            var user = userRepository.findById(content.getCreatorID()).get();
            var Message = buildMessages(MessageEnum.FailedAuctionPayment);
            Message.getExtraInfo().add(content.getThumbnail());
            Message.getExtraInfo().add(content.getContentName());
            user.getMessages().add(Message.getMessageId());
            userRepository.save(user);
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


}
