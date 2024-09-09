package Project_Noir.Athena.Service;

import Project_Noir.Athena.Controller.ServerSideEventController;
import Project_Noir.Athena.DTO.BidPaymentRequest;
import Project_Noir.Athena.DTO.RefundRequest;
import Project_Noir.Athena.Exception.SivantisException;
import Project_Noir.Athena.Model.Channels;
import Project_Noir.Athena.Model.ContentEnum;
import Project_Noir.Athena.Model.Payment;
import Project_Noir.Athena.Model.PaymentEnum;
import Project_Noir.Athena.Repo.ChannelRepository;
import Project_Noir.Athena.Repo.ContentRepository;
import Project_Noir.Athena.Repo.PaymentRepository;
import Project_Noir.Athena.Repo.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class PaymentService {
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final ServerSideEventController serverSideEventController;
    private final ChannelRepository channelRepository;
    private final ContentRepository contentRepository;
    private final MessageService messageService;
    private final JwtService jwtService;
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
        var user = userRepository.findById(userID).orElseThrow();
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
            Payment.setManaToCreator(totalManaAmount * .85);
            paymentRepository.save(Payment);
            messageService.paymentMessage(user, Payment,content);
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
                    .manaToCreator(totalManaAmount * .85)
                    .transactionHash(bidPaymentRequest.getTransactionHash())
                    .build();
            user.getPurchasedContent().put(newPayment.getContentId(), newPayment.getPaymentId());
            paymentRepository.save(newPayment);
            messageService.paymentMessage(user, newPayment, content);
        }
        content.getListOfBuyerIds().put(userID, bidPaymentRequest.getManaAmount());
        content.setHype(content.getHype() + (bidPaymentRequest.getDollarAmount() * auctionModifier));
        contentRepository.save(content);
    }

    public void purchaseChannelContent(Channels channel, String contentID, int priceOfContent, String transactionHash, Double averageWeeklyViewers) {
        var user = userRepository.findById(channel.getOwnerID()).orElseThrow();
        var Payment = findChannelPayment(channel.getChannelName(), contentID);
        var content = contentRepository.findById(contentID).orElseThrow();
        var dollarAmount = (averageWeeklyViewers * priceOfContent) / 100;
        var totalManaAmount = dollarAmount / serverSideEventController.latestValue;
        if(Payment != null){
            Payment.setManaAmount(String.valueOf(totalManaAmount));
            Payment.setDollarAmount(dollarAmount);
            Payment.setStatus(PaymentEnum.Purchased);
            Payment.setTransactionHash(transactionHash);
            Payment.setPaymentDate(Instant.now());
            Payment.setManaToCreator(totalManaAmount * .9);
            content.setHype(content.getHype() + (Payment.getDollarAmount() * buyModifier));
            content.getListOfBuyerIds().put(channel.getChannelName(), Payment.getManaAmount());
            contentRepository.save(content);
            paymentRepository.save(Payment);
            messageService.paymentChannelMessage(channel, Payment, user, content);
        }
        else {
            var newPayment = Project_Noir.Athena.Model.Payment.builder()
                    .paymentId(ObjectId.get().toHexString())
                    .contentId(contentID)
                    .userId(user.getUserId())
                    .manaAmount(String.valueOf(dollarAmount / serverSideEventController.latestValue))
                    .dollarAmount(dollarAmount)
                    .refundDate(null)
                    .paymentDate(Instant.now())
                    .manaToCreator(totalManaAmount * .9)
                    .status(PaymentEnum.Purchased)
                    .transactionHash(transactionHash)
                    .build();
            channel.getPurchasedContent().put(newPayment.getContentId(), newPayment.getPaymentId());
            content.setHype(content.getHype() + (newPayment.getDollarAmount() * buyModifier));
            content.getListOfBuyerIds().put(channel.getChannelName(), newPayment.getManaAmount());
            contentRepository.save(content);
            paymentRepository.save(newPayment);
            messageService.paymentChannelMessage(channel, newPayment, user, content);
        }
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
        Payment.setDollarAmount(Payment.getDollarAmount() + bidPaymentRequest.getDollarAmount());
        Payment.setPaymentDate(Instant.now());
        Payment.setManaToCreator(newManaAmount * .85);
        content.getListOfBuyerIds().put(userID, String.valueOf(newManaAmount));
        content.setHype(content.getHype() + (bidPaymentRequest.getDollarAmount() * auctionModifier));
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
        content.getListOfBuyerIds().remove(userID);
        content.setHype(content.getHype() - (Payment.getDollarAmount() * auctionModifier));
        var manaAmountRefunded = Payment.getManaAmount();
        Payment.setDollarAmount(0.0);
        Payment.setStatus(PaymentEnum.RefundedPurchase);
        Payment.setRefundDate(Instant.now());
        Payment.setTransactionHash(refundRequest.getTransactionHash());
        Payment.setManaAmount("0");
        Payment.setManaToCreator(0.0);
        paymentRepository.save(Payment);
        var user = userRepository.findById(userID).orElseThrow();
        contentRepository.save(content);
        messageService.refundMessage(user, refundRequest.getTransactionHash(), manaAmountRefunded, content);
    }



    // @dev Sets the purchase entity to refunded and removes from ListOfBuyerIds
    public void refundPurchasedContent(String userID, String contentID, String transactionHash) {
        if(findPayment(userID, contentID) != null){
            var Payment = findPayment(userID, contentID);
            if(!Payment.getStatus().equals(PaymentEnum.RefundedPurchase) && contentRepository.findById(contentID).isPresent()){
                var content = contentRepository.findById(contentID).get();
                content.setHype(content.getHype() - (Payment.getDollarAmount() * auctionModifier));
                content.getListOfBuyerIds().remove(userID);
                var manaAmountRefunded = Payment.getManaAmount();
                Payment.setDollarAmount(0.0);
                Payment.setStatus(PaymentEnum.RefundedPurchase);
                Payment.setRefundDate(Instant.now());
                Payment.setTransactionHash(transactionHash);
                Payment.setManaAmount("0");
                Payment.setManaToCreator(0.0);
                paymentRepository.save(Payment);
                contentRepository.save(content);
                if(userRepository.findById(userID).isPresent()){
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

    public void refundChannelPurchasedContent(String channelName, String contentID, String transactionHash) {
        if(channelRepository.findByChannelName(channelName).isPresent()){
            var channel = channelRepository.findByChannelName(channelName).get();
            if(findChannelPayment(channel.getChannelName(), contentID) != null){
                var Payment = findChannelPayment(channel.getChannelName(), contentID);
                if(!Payment.getStatus().equals(PaymentEnum.RefundedPurchase) && contentRepository.findById(contentID).isPresent()){
                    var content = contentRepository.findById(contentID).get();
                    content.setHype(content.getHype() - (Payment.getDollarAmount() * buyModifier));
                    content.getListOfBuyerIds().remove(channelName);
                    var manaAmountRefunded = Payment.getManaAmount();
                    Payment.setDollarAmount(0.0);
                    Payment.setStatus(PaymentEnum.RefundedPurchase);
                    Payment.setRefundDate(Instant.now());
                    Payment.setTransactionHash(transactionHash);
                    Payment.setManaAmount("0");
                    Payment.setManaToCreator(0.0);
                    paymentRepository.save(Payment);
                    contentRepository.save(content);
                    if(userRepository.findById(channel.getOwnerID()).isPresent()){
                        var user = userRepository.findById(channel.getOwnerID()).get();
                        messageService.refundChannelMessage(user, transactionHash, channelName, manaAmountRefunded, content);
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

    public void resolvedAuctionPayment(String contentID, String userID) {
        messageService.resolvedAuctionPayment(contentID, userID);
    }
}
