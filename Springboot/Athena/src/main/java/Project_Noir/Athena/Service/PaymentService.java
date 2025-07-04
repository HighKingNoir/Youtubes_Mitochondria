package Project_Noir.Athena.Service;

import Project_Noir.Athena.Controller.ServerSideEventController;
import Project_Noir.Athena.DTO.BidPaymentRequest;
import Project_Noir.Athena.DTO.RefundRequest;
import Project_Noir.Athena.Exception.SivantisException;
import Project_Noir.Athena.Model.*;
import Project_Noir.Athena.Repo.ChannelRepository;
import Project_Noir.Athena.Repo.ContentRepository;
import Project_Noir.Athena.Repo.PaymentRepository;
import Project_Noir.Athena.Repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final ServerSideEventController serverSideEventController;
    private final ChannelRepository channelRepository;
    private final ContentRepository contentRepository;
    private final MongoTemplate mongoTemplate;
    private final MessageService messageService;
    private final JwtService jwtService;
    @Value("${contract.bid.address}")
    private String BidAddress;
    private final Double auctionModifier = .02;
    private final Double buyModifier = .1;

    // @dev Creates a new purchase entity and maps the contentID with the newly created payment entity ID.
    // @dev Maps all the userIDs with the amount they sent in Mana to getListOfBuyerIds
    public void purchaseContent(BidPaymentRequest bidPaymentRequest, String JWT) {
        var content = contentRepository.findById(bidPaymentRequest.getContentID()).orElseThrow();
        if(!content.getContentEnum().equals(ContentEnum.Active)){
            throw new SivantisException("This video is no longer active");
        }
        var userID = jwtService.extractUserId(JWT);
        var Payment = findPayment(userID, bidPaymentRequest.getContentID());
        double totalManaAmount = Double.parseDouble(bidPaymentRequest.getManaAmount());
        if(Payment != null){
            if(!Payment.getStatus().equals(PaymentEnum.RefundedPurchase)){
                throw new SivantisException("Content was already purchased");
            }
            Payment.setManaAmount(bidPaymentRequest.getManaAmount());
            Payment.setDollarAmount(bidPaymentRequest.getDollarAmount());
            Payment.setStatus(PaymentEnum.PendingPurchase);
            Payment.setTransactionHash(bidPaymentRequest.getTransactionHash());
            Payment.setPaymentDate(Instant.now());
            Payment.setManaToCreator(totalManaAmount * .9);
            addToListOfBuyersAndHype(content.getContentId(), userID, Payment.getManaAmount(), Payment.getDollarAmount(), auctionModifier);
            paymentRepository.save(Payment);
            messageService.paymentMessage(userID, Payment,content);
        }
        else {
            var newPayment = Project_Noir.Athena.Model.Payment.builder()
                    .paymentId(ObjectId.get().toHexString())
                    .contentId(bidPaymentRequest.getContentID())
                    .userId(userID)
                    .manaAmount(bidPaymentRequest.getManaAmount())
                    .dollarAmount(bidPaymentRequest.getDollarAmount())
                    .refundDate(null)
                    .paymentDate(Instant.now())
                    .status(PaymentEnum.PendingPurchase)
                    .manaToCreator(totalManaAmount * .9)
                    .transactionHash(bidPaymentRequest.getTransactionHash())
                    .build();
            addPurchasedContent(userID, newPayment.getContentId(), newPayment.getPaymentId());
            addToListOfBuyersAndHype(content.getContentId(), userID, newPayment.getManaAmount(), newPayment.getDollarAmount(), auctionModifier);
            paymentRepository.save(newPayment);
            messageService.paymentMessage(userID, newPayment, content);
        }
    }

    public void purchaseChannelContent(Channels channel, Content content, int priceOfContent, Double averageWeeklyViewers) {
        var Payment = findChannelPayment(channel.getChannelName(), content.getContentId());
        BigDecimal dollarAmount = BigDecimal.valueOf((averageWeeklyViewers * priceOfContent) / 100);
        BigDecimal divisor = BigDecimal.valueOf(serverSideEventController.latestValue);
        BigDecimal totalManaAmount = dollarAmount.divide(divisor, 18, RoundingMode.HALF_UP);
        if(Payment != null){
            Payment.setManaAmount(String.valueOf(totalManaAmount));
            Payment.setDollarAmount(dollarAmount);
            Payment.setStatus(PaymentEnum.PendingPurchase);
            Payment.setTransactionHash(null);
            Payment.setPaymentDate(Instant.now());
            Payment.setManaToCreator(totalManaAmount.doubleValue() * .9);
            addToListOfBuyersAndHype(content.getContentId(), channel.getChannelName(), Payment.getManaAmount(), dollarAmount, buyModifier);
            paymentRepository.save(Payment);
            messageService.pendingPaymentChannelMessage(channel, Payment, content);
        }
        else {
            var newPayment = Project_Noir.Athena.Model.Payment.builder()
                    .paymentId(ObjectId.get().toHexString())
                    .contentId(content.getContentId())
                    .userId(channel.getOwnerID())
                    .manaAmount(String.valueOf(dollarAmount.doubleValue() / serverSideEventController.latestValue))
                    .dollarAmount(dollarAmount)
                    .refundDate(null)
                    .paymentDate(Instant.now())
                    .manaToCreator(totalManaAmount.doubleValue() * .9)
                    .status(PaymentEnum.PendingPurchase)
                    .transactionHash(null)
                    .build();
            addChannelPurchasedContent(channel.getChannelId(), newPayment.getContentId(), newPayment.getPaymentId());
            addToListOfBuyersAndHype(content.getContentId(), channel.getChannelName(), newPayment.getManaAmount(), dollarAmount, buyModifier);
            paymentRepository.save(newPayment);
            mongoTemplate.remove(new Query(Criteria.where("_id").is(channel.getChannelName() + "::" + content.getContentId())), PaymentLock.class);
            messageService.pendingPaymentChannelMessage(channel, newPayment, content);
        }
    }

    public void addToListOfBuyersAndHype(String contentId, String userIdOrChannelName, String manaAmount, BigDecimal dollarAmount, Double modifier){
        Query query = new Query(Criteria.where("_id").is(contentId));

        Update update = new Update()
                .set("listOfBuyerIds." + userIdOrChannelName, manaAmount) // Add/overwrite buyer
                .inc("hype", dollarAmount.multiply(BigDecimal.valueOf(modifier))); // Increment hype

        mongoTemplate.findAndModify(query, update, Content.class);
    }

    public void removeFromListOfBuyersAndHype(String contentId, String userIdOrChannelName, BigDecimal dollarAmount, Double modifier) {
        Query query = new Query(Criteria.where("_id").is(contentId));
        BigDecimal decrease = dollarAmount.multiply(BigDecimal.valueOf(modifier)).negate();

        Update update = new Update()
                .unset("listOfBuyerIds." + userIdOrChannelName)
                .inc("hype", decrease);

        mongoTemplate.findAndModify(query, update, Content.class);
    }

    public void addPurchasedContent(String userId, String contentId, String paymentId) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().set("purchasedContent." + contentId, paymentId);

        mongoTemplate.findAndModify(
                query,
                update,
                Users.class
        );
    }


    public void addChannelPurchasedContent(String channelId, String contentId, String paymentId) {
        Query query = new Query(Criteria.where("_id").is(channelId));
        Update update = new Update().set("purchasedContent." + contentId, paymentId);
        mongoTemplate.findAndModify(
                query,
                update,
                Channels.class
        );
    }


    // @dev Increases the amount of mana the user previously bid
    public void updatePurchasedContent(BidPaymentRequest bidPaymentRequest, String JWT) {
        var userID = jwtService.extractUserId(JWT);
        if(findPayment(userID, bidPaymentRequest.getContentID()) == null){
            throw new SivantisException("Bid Doesn't exist");
        }
        var Payment = findPayment(userID, bidPaymentRequest.getContentID());
        if(!Payment.getStatus().equals(PaymentEnum.PendingPurchase)){
            throw new SivantisException("Bid cannot be raised for this video");
        }
        var user = userRepository.findById(userID).orElseThrow();
        var content = contentRepository.findById(bidPaymentRequest.getContentID()).orElseThrow();
        if(!content.getContentEnum().equals(ContentEnum.Active)){
            throw new SivantisException("This video is no longer active");
        }
        var previousManaAmount = content.getListOfBuyerIds().get(userID);
        var newManaAmount = Double.parseDouble(previousManaAmount) + Double.parseDouble(bidPaymentRequest.getManaAmount());
        Payment.setManaAmount(String.valueOf(newManaAmount));
        Payment.setDollarAmount(Payment.getDollarAmount().add(bidPaymentRequest.getDollarAmount()));
        Payment.setPaymentDate(Instant.now());
        Payment.setManaToCreator(newManaAmount * .9);
        addToListOfBuyersAndHype(content.getContentId(), userID, Payment.getManaAmount(), Payment.getDollarAmount(), auctionModifier);
        paymentRepository.save(Payment);
        contentRepository.save(content);
        messageService.updatedPaymentMessage(
                user,
                Payment,
                content,
                bidPaymentRequest.getManaAmount(),
                String.valueOf(bidPaymentRequest.getDollarAmount())
        );
    }


    // @dev Sets the purchase entity to refunded and removes from ListOfBuyerIds
    public void refundPurchasedContent(RefundRequest refundRequest, String JWT) {
        var userID = jwtService.extractUserId(JWT);
        var user = userRepository.findById(userID).orElseThrow();
        if(findPayment(userID, refundRequest.getContentID()) == null){
            throw new SivantisException("Bid Doesn't exist");
        }
        var Payment = findPayment(userID, refundRequest.getContentID());
        if(Payment.getStatus().equals(PaymentEnum.RefundedPurchase)){
            throw new SivantisException("Content has already been refunded");
        }
        var content = contentRepository.findById(refundRequest.getContentID()).orElseThrow();
        if(!content.getContentEnum().equals(ContentEnum.Active)){
            throw new SivantisException("This video is no longer active");
        }
        removeFromListOfBuyersAndHype(content.getContentId(), userID, Payment.getDollarAmount(), auctionModifier);
        var manaAmountRefunded = Payment.getManaAmount();
        Payment.setDollarAmount(BigDecimal.ZERO);
        Payment.setStatus(PaymentEnum.RefundedPurchase);
        Payment.setRefundDate(Instant.now());
        Payment.setTransactionHash(refundRequest.getTransactionHash());
        Payment.setManaAmount("0");
        Payment.setManaToCreator(0.0);
        paymentRepository.save(Payment);
        messageService.refundMessage(user, refundRequest.getTransactionHash(), manaAmountRefunded, content);
    }



    // @dev Sets the purchase entity to refunded and removes from ListOfBuyerIds
    public void refundPurchasedContent(String userID, String contentID, String transactionHash) {
        if(findPayment(userID, contentID) != null){
            var Payment = findPayment(userID, contentID);
            if(!Payment.getStatus().equals(PaymentEnum.RefundedPurchase) && contentRepository.findById(contentID).isPresent()){
                removeFromListOfBuyersAndHype(contentID, userID, Payment.getDollarAmount(), auctionModifier);
                var manaAmountRefunded = Payment.getManaAmount();
                Payment.setDollarAmount(BigDecimal.ZERO);
                Payment.setStatus(PaymentEnum.RefundedPurchase);
                Payment.setRefundDate(Instant.now());
                Payment.setTransactionHash(transactionHash);
                Payment.setManaAmount("0");
                Payment.setManaToCreator(0.0);
                paymentRepository.save(Payment);
                if(userRepository.findById(userID).isPresent()){
                    var content = contentRepository.findById(contentID).get();
                    var user = userRepository.findById(userID).get();
                    messageService.refundMessage(user, transactionHash, manaAmountRefunded, content);
                }
            }
        }
    }

    public void failedRefundPurchasedContent(String userID, String contentID) {
        if(findPayment(userID, contentID) != null){
            var Payment = findPayment(userID, contentID);
            if(userRepository.findById(userID).isPresent()){
                var user = userRepository.findById(userID).get();
                messageService.failedRefundMessage(user, Payment.getManaAmount(), contentID);
            }
        }
    }

    public void refundChannelPurchasedContent(String channelName, String contentID , String transactionHash) {
        if(channelRepository.findByChannelName(channelName).isPresent()){
            var channel = channelRepository.findByChannelName(channelName).get();
            if(findChannelPayment(channel.getChannelName(), contentID) != null){
                var Payment = findChannelPayment(channel.getChannelName(), contentID);
                if(!Payment.getStatus().equals(PaymentEnum.RefundedPurchase) && contentRepository.findById(contentID).isPresent()){
                    removeFromListOfBuyersAndHype(contentID, channelName, Payment.getDollarAmount(), buyModifier);
                    var manaAmountRefunded = Payment.getManaAmount();
                    Payment.setDollarAmount(BigDecimal.ZERO);
                    Payment.setStatus(PaymentEnum.RefundedPurchase);
                    Payment.setRefundDate(Instant.now());
                    Payment.setTransactionHash(transactionHash);
                    Payment.setManaAmount("0");
                    Payment.setManaToCreator(0.0);
                    paymentRepository.save(Payment);
                    if(userRepository.findById(channel.getOwnerID()).isPresent()){
                        var user = userRepository.findById(channel.getOwnerID()).get();
                        var content = contentRepository.findById(contentID).get();
                        messageService.refundChannelMessage(user, transactionHash, channelName, manaAmountRefunded, content);
                    }
                }
            }
        }
    }

    public void pendingRefundChannelPurchasedContent(String channelName, String contentID) {
        if(channelRepository.findByChannelName(channelName).isPresent()){
            var channel = channelRepository.findByChannelName(channelName).get();
            if(findChannelPayment(channel.getChannelName(), contentID) != null){
                var Payment = findChannelPayment(channel.getChannelName(), contentID);
                if(!Payment.getStatus().equals(PaymentEnum.RefundedPurchase) && contentRepository.findById(contentID).isPresent()){
                    var content = contentRepository.findById(contentID).get();
                    removeFromListOfBuyersAndHype(contentID, channelName, Payment.getDollarAmount(), buyModifier);
                    var manaAmountRefunded = Payment.getManaAmount();
                    Payment.setDollarAmount(BigDecimal.ZERO);
                    Payment.setStatus(PaymentEnum.PendingRefund);
                    Payment.setRefundDate(Instant.now());
                    Payment.setTransactionHash(null);
                    Payment.setManaAmount("0");
                    Payment.setManaToCreator(0.0);
                    paymentRepository.save(Payment);
                    contentRepository.save(content);
                    if(userRepository.findById(channel.getOwnerID()).isPresent()){
                        var user = userRepository.findById(channel.getOwnerID()).get();
                        messageService.pendingRefundChannelMessage(user, channelName, manaAmountRefunded, content);
                    }
                }
            }
        }
    }

    public void failedRefundChannelPurchasedContent(String channelName, String contentID) {
        if(channelRepository.findByChannelName(channelName).isPresent()){
            var channel = channelRepository.findByChannelName(channelName).get();
            if(findChannelPayment(channel.getChannelName(), contentID) != null){
                var Payment = findChannelPayment(channel.getChannelName(), contentID);
                if(!Payment.getStatus().equals(PaymentEnum.RefundedPurchase) && contentRepository.findById(contentID).isPresent()){
                    if(userRepository.findById(channel.getOwnerID()).isPresent()){
                        var user = userRepository.findById(channel.getOwnerID()).get();
                        messageService.failedRefundChannelMessage(user, channelName, Payment.getManaAmount(), contentID);
                    }
                }
            }
        }
    }

    // @dev returns all the user payment entity IDs
    public List<Payment> getAllUserPayment(String userID) {
        return paymentRepository.findAllById(userRepository.findById(userID).orElseThrow().getPurchasedContent().keySet());
    }

    // @dev returns all the user payment entity IDs
    public List<Payment> getAllChannelPayment(String channelName) {
        return paymentRepository.findAllById(channelRepository.findByChannelName(channelName).orElseThrow().getPurchasedContent().keySet());
    }

    // @dev Returns the payment entity or null
    public Payment findPayment(String userID, String contentID){
        if(userRepository.findById(userID).orElseThrow().getPurchasedContent().get(contentID) == null){
            return null;
        }
        return paymentRepository.findById(userRepository.findById(userID).orElseThrow().getPurchasedContent().get(contentID)).orElseThrow();
    }

    // @dev Returns the payment entity or null
    public Payment findChannelPayment(String channelName, String contentID){
        if(channelRepository.findByChannelName(channelName).orElseThrow().getPurchasedContent().get(contentID) == null){
            return null;
        }
        return paymentRepository.findById(channelRepository.findByChannelName(channelName).orElseThrow().getPurchasedContent().get(contentID)).orElseThrow();
    }

    public Double getMaxNumber(Collection<Number> numbers){
        var maxNumber = 0.0;
        for (Number number : numbers) {
            double currentNumber = number.doubleValue();
            if (currentNumber > maxNumber) {
                maxNumber = currentNumber;
            }
        }
        return maxNumber;
    }

    public void resolvedAuctionPayment(String contentID) {
        messageService.resolvedAuctionPayment(contentID);
    }

    public void failedRefundChannelPurchasedContent(List<Channels> channels, List<Content> content) {
        List<Users> users = new ArrayList<>();
        List<String> manaAmounts = new ArrayList<>();
        List<String> channelNameList = new ArrayList<>();
        List<Content> contentList = new ArrayList<>();
        for (int i = 0; i < channels.size(); i++) {
            var channelName = channels.get(i).getChannelName();
            var contentID = content.get(i).getContentId();
            if(findChannelPayment(channelName, contentID) != null){
                var Payment = findChannelPayment(channelName, contentID);
                if(!Payment.getStatus().equals(PaymentEnum.RefundedPurchase) && contentRepository.findById(contentID).isPresent()){
                    if(userRepository.findById(channels.get(i).getOwnerID()).isPresent()){
                        users.add(userRepository.findById(channels.get(i).getOwnerID()).get());
                        manaAmounts.add(Payment.getManaAmount());
                        channelNameList.add(channelName);
                        contentList.add(content.get(i));
                    }
                }
            }
        }
        messageService.failedRefundChannelMessage(users, channelNameList, manaAmounts, contentList);
    }

    public void failedRefundPurchasedContent(List<Users> users, List<Content> content) {
        List<Users> usersList = new ArrayList<>();
        List<Content> contentList = new ArrayList<>();
        List<String> manaAmountList = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            if(findPayment(users.get(i).getUserId(), content.get(i).getContentId()) != null){
                var Payment = findPayment(users.get(i).getUserId(), content.get(i).getContentId());
                usersList.add(users.get(i));
                contentList.add(content.get(i));
                manaAmountList.add(Payment.getManaAmount());
            }
        }
        messageService.failedRefundMessage(usersList, manaAmountList, contentList);
    }
}
