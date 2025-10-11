package Project_Noir.Athena.Service;

import Project_Noir.Athena.Controller.ServerSideEventController;
import Project_Noir.Athena.DTO.BidPaymentRequest;
import Project_Noir.Athena.DTO.RefundRequest;
import Project_Noir.Athena.Exception.SivantisException;
import Project_Noir.Athena.Model.*;
import Project_Noir.Athena.Repo.*;
import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
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
    private final TransactionVerificationPackageRepository transactionVerificationPackageRepository;
    private final SivantisContractLogsRepository sivantisContractLogsRepository;
    private final MultiSendHelperService multiSendHelperService;
    private final ChannelRepository channelRepository;
    private final RankUpPaymentLockRepository rankUpPaymentLockRepository;
    private final ContentRepository contentRepository;
    private final MongoTemplate mongoTemplate;
    private final MessageService messageService;
    private final JwtService jwtService;
    @Value("${contract.bid.address}")
    private String BidAddress;
    public final Double auctionModifier = .02;
    private final Double buyModifier = .1;


    public void verifiedTransactions(ArrayList<TransactionVerificationPackage> allTransactionVerificationPackages){
        var increaseCreatorRankFunctionDetailsList = new ArrayList<ContractFunctionDetails>();
        var rankUpPaymentLockList = new ArrayList<RankUpPaymentLock>();
        List<byte[]> increaseCreatorRankCalls = new java.util.ArrayList<>(List.of());
        ArrayList<Payment> paymentArrayList = new ArrayList<>();
        for (TransactionVerificationPackage transactionVerificationPackage: allTransactionVerificationPackages){
            var userId = transactionVerificationPackage.getUserId();
            switch (transactionVerificationPackage.getTransactionVerificationFunctionEnum()) {
                case PlaceBid -> {
                    var content = contentRepository.findById(transactionVerificationPackage.getContentID()).orElseThrow();
                    var Payment = findPayment(userId, content.getContentId());
                    if(Payment != null){
                        Payment.setStatus(PaymentEnum.PendingPurchase);
                        paymentArrayList.add(Payment);
                        messageService.paymentMessage(userId, Payment,content);
                    }
                }
                case RaiseBid -> {
                    var content = contentRepository.findById(transactionVerificationPackage.getContentID()).orElseThrow();
                    var Payment = findPayment(userId, content.getContentId());
                    if(Payment != null){
                        Payment.setStatus(PaymentEnum.PendingPurchase);
                        var manaSpent = Payment.getPaymentRevertInfo().get(transactionVerificationPackage.getTransactionVerificationId()).getManaAmount();
                        Payment.getPaymentRevertInfo().remove(transactionVerificationPackage.getTransactionVerificationId());
                        paymentArrayList.add(Payment);
                        messageService.updatedPaymentMessage(
                                userId,
                                Payment,
                                content,
                                manaSpent,
                                String.valueOf(Payment.getDollarAmount())
                        );
                    }
                }
                case ArchonPass -> {
                    var user = increaseUserHype(userId, 1000000.0);
                    while (user.getRank() < 11 && user.getTotalHype() >= rankValues(user)) {
                        if (user.isContentCreator()) {
                            increaseCreatorRankCalls.add(buildCall("increaseCreatorRank", List.of(new Utf8String(user.getUserId()))));

                            var increaseCreatorRankFunctionDetails = ContractFunctionDetails.builder()
                                    .contentID(user.getUserId())
                                    .contractFunctionEnum(ContractFunctionEnum.IncreaseCreatorRank)
                                    .build();

                            increaseCreatorRankFunctionDetailsList.add(increaseCreatorRankFunctionDetails);
                        }
                        user = increaseUserRank(user.getUserId());
                        addSivantisTokens(user);
                        messageService.rankUpMessage(user);
                    }
                    var optionalRankUpPaymentLock = rankUpPaymentLockRepository.findByUserIdAndTransactionVerificationFunctionEnum(userId, TransactionVerificationFunctionEnum.ArchonPass);
                    if(optionalRankUpPaymentLock.isPresent()) {
                        var rankUpPaymentLock = optionalRankUpPaymentLock.get();
                        rankUpPaymentLock.setVerifiedAt(Instant.now());
                        rankUpPaymentLock.setPaymentEnum(PaymentEnum.Purchased);
                        rankUpPaymentLockList.add(rankUpPaymentLock);
                    }
                    messageService.archonPass(userId);
                }
                case MasterPass -> {
                    var user = increaseUserHype(userId, 25000.0);
                    while (user.getRank() < 11 && user.getTotalHype() >= rankValues(user)) {
                        if (user.isContentCreator()) {
                            increaseCreatorRankCalls.add(buildCall("increaseCreatorRank", List.of(new Utf8String(user.getUserId()))));

                            var increaseCreatorRankFunctionDetails = ContractFunctionDetails.builder()
                                    .contentID(user.getUserId())
                                    .contractFunctionEnum(ContractFunctionEnum.IncreaseCreatorRank)
                                    .build();

                            increaseCreatorRankFunctionDetailsList.add(increaseCreatorRankFunctionDetails);
                        }
                        user = increaseUserRank(user.getUserId());
                        addSivantisTokens(user);
                        messageService.rankUpMessage(user);
                    }
                    var optionalRankUpPaymentLock = rankUpPaymentLockRepository.findByUserIdAndTransactionVerificationFunctionEnum(userId, TransactionVerificationFunctionEnum.MasterPass);
                    if(optionalRankUpPaymentLock.isPresent()){
                        var rankUpPaymentLock = optionalRankUpPaymentLock.get();
                        rankUpPaymentLock.setVerifiedAt(Instant.now());
                        rankUpPaymentLock.setPaymentEnum(PaymentEnum.Purchased);
                        rankUpPaymentLockList.add(rankUpPaymentLock);
                    }
                    messageService.masterPassMessage(userId);
                }
            }
        }
        if(!paymentArrayList.isEmpty()){
            paymentRepository.saveAll(paymentArrayList);
        }
        if(!rankUpPaymentLockList.isEmpty()){
            rankUpPaymentLockRepository.saveAll(rankUpPaymentLockList);
        }
        if(!increaseCreatorRankCalls.isEmpty()){
            var increaseCreatorRankMultiCallTransactionResponse = sendMultiCallTransaction(increaseCreatorRankCalls);
            int increaseCreatorRankMultiCallIndex = 0;
            for (MultiCallResponse multiCallResponse: increaseCreatorRankMultiCallTransactionResponse){
                List<ContractFunctionDetails> increaseCreatorRankSubList = increaseCreatorRankFunctionDetailsList.subList(
                        increaseCreatorRankMultiCallIndex,
                        increaseCreatorRankMultiCallIndex + multiCallResponse.getTransactionCount()
                );
                var increaseCreatorRankMultiCallLog = createMultiCallLog(increaseCreatorRankSubList, multiCallResponse.getTransactionCount());
                increaseCreatorRankMultiCallLog.setContractTransactionReceipt(multiCallResponse.getContractTransactionReceipt());
                sivantisContractLogsRepository.save(increaseCreatorRankMultiCallLog);
                increaseCreatorRankMultiCallIndex += multiCallResponse.getTransactionCount();
            }
        }
    }

    private void addSivantisTokens(Users user){
        switch (user.getRank()) {
            case 2, 3, 4 -> increaseDevelopingVideosAllowance(user.getUserId(), 1);
            case 5, 6, 7 -> increaseDevelopingVideosAllowance(user.getUserId(), 2);
            case 8, 9, 10 -> increaseDevelopingVideosAllowance(user.getUserId(), 3);
        }
    }

    private void increaseDevelopingVideosAllowance(String userId, int Amount){
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().inc("allowedDevelopingVideos", Amount);
        mongoTemplate.updateFirst(
                query,
                update,
                Users.class
        );
    }

    private Users increaseUserRank(String userId){
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().inc("rank", 1);
        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Users.class
        );
    }

    private Users increaseUserHype(String userId, Double addedHype) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().inc("totalHype", addedHype);
        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), Users.class);
    }

    public void invalidTransactionHash(String reason, TransactionVerificationPackage transactionVerificationPackage){
        var userID = transactionVerificationPackage.getUserId();
        switch (transactionVerificationPackage.getTransactionVerificationFunctionEnum()){
            case PlaceBid -> {
                revertPlaceBid(transactionVerificationPackage);
                messageService.invalidBidTransaction(userID, TransactionVerificationFunctionEnum.PlaceBid, reason);
            }
            case RaiseBid -> {
                revertRaiseBid(transactionVerificationPackage);
                messageService.invalidBidTransaction(userID, TransactionVerificationFunctionEnum.RaiseBid, reason);
            }
            case ArchonPass -> {
                var optionalRankUpPaymentLock = rankUpPaymentLockRepository.findByUserIdAndTransactionVerificationFunctionEnum(userID, TransactionVerificationFunctionEnum.ArchonPass);
                optionalRankUpPaymentLock.ifPresent(rankUpPaymentLockRepository::delete);
                messageService.invalidRankUpTransaction(userID, TransactionVerificationFunctionEnum.ArchonPass, reason);
            }
            case MasterPass ->{
                var optionalRankUpPaymentLock = rankUpPaymentLockRepository.findByUserIdAndTransactionVerificationFunctionEnum(userID, TransactionVerificationFunctionEnum.MasterPass);
                if(optionalRankUpPaymentLock.isPresent()){
                    optionalRankUpPaymentLock.ifPresent(rankUpPaymentLockRepository::delete);
                }
                messageService.invalidRankUpTransaction(userID, TransactionVerificationFunctionEnum.MasterPass, reason);
            }
        }
    }

    private void revertRaiseBid(TransactionVerificationPackage transactionVerificationPackage) {
        var userID = transactionVerificationPackage.getUserId();
        var contentId = transactionVerificationPackage.getContentID();
        var Payment = findPayment(userID, contentId);

        if(Payment == null){
            return;
        }
        var paymentRevertInfo = Payment.getPaymentRevertInfo().get(transactionVerificationPackage.getTransactionVerificationId());

        BigInteger previousManaWei = Convert.toWei(new BigDecimal(Payment.getManaAmount()), Convert.Unit.ETHER)
                .toBigIntegerExact();
        BigInteger newManaWei = previousManaWei.subtract(
                Convert.toWei(new BigDecimal(paymentRevertInfo.getManaAmount()), Convert.Unit.ETHER)
                        .toBigIntegerExact()
        );

        Payment.setStatus(PaymentEnum.PendingPurchase);
        Payment.setDollarAmount(Payment.getDollarAmount().subtract(paymentRevertInfo.getDollarAmount()));
        Payment.setPaymentDate(paymentRevertInfo.getPreviousPaymentDate());
        Payment.setManaAmount(Convert.fromWei(new BigDecimal(newManaWei), Convert.Unit.ETHER).toPlainString());
        Payment.setManaToCreator(paymentRevertInfo.getManaToCreator());

        Query query = new Query(Criteria.where("_id").is(contentId));
        BigDecimal decrease = paymentRevertInfo.getDollarAmount().multiply(BigDecimal.valueOf(auctionModifier)).negate();

        Update update = new Update().inc("hype", decrease);

        mongoTemplate.updateFirst(query, update, Content.class);

        paymentRepository.save(Payment);
    }


    private void revertPlaceBid(TransactionVerificationPackage transactionVerificationPackage) {
        var userID = transactionVerificationPackage.getUserId();
        var contentId = transactionVerificationPackage.getContentID();
        var Payment = findPayment(userID, contentId);
        if(Payment == null){
            return;
        }
        removeFromListOfBuyersAndHype(contentId, userID, Payment.getDollarAmount(), auctionModifier);
        Query query = new Query(Criteria.where("_id").is(userID));
        Update update = new Update().unset("purchasedContent." + contentId);

        mongoTemplate.updateFirst(
                query,
                update,
                Users.class
        );
        paymentRepository.delete(Payment);
    }

    public void purchaseArchonPass(String transactionHash, String JWT){
        var userId = jwtService.extractUserId(jwtService.getJWTString(JWT));
        var rankUpPaymentLock = mongoTemplate.insert(RankUpPaymentLock.builder()
                .rankUpPaymentId(ObjectId.get().toHexString())
                .userId(userId)
                .transactionVerificationFunctionEnum(TransactionVerificationFunctionEnum.ArchonPass)
                .transactionHash(transactionHash)
                .paymentEnum(PaymentEnum.VerifyingTransaction)
                .createdAt(Instant.now())
                .build());
        try {
            rankUpPaymentLockRepository.save(rankUpPaymentLock);
        } catch (DuplicateKeyException e) {
            throw new SivantisException(
                    "Payment already exists for user " + userId + " and type Archon Pass" );
        }

        messageService.verifyingTransactionMessage(userId, TransactionVerificationFunctionEnum.ArchonPass, transactionHash, null);
        addTransactionVerificationToQueue(
                null,
                userId,
                transactionHash,
                TransactionVerificationFunctionEnum.ArchonPass,
                BigInteger.ZERO
        );
    }

    public void purchaseMasterPass(String transactionHash, String JWT){
        var userId = jwtService.extractUserId(jwtService.getJWTString(JWT));
        var rankUpPaymentLock = mongoTemplate.insert(RankUpPaymentLock.builder()
                .rankUpPaymentId(ObjectId.get().toHexString())
                .userId(userId)
                .transactionVerificationFunctionEnum(TransactionVerificationFunctionEnum.MasterPass)
                .transactionHash(transactionHash)
                .paymentEnum(PaymentEnum.VerifyingTransaction)
                .createdAt(Instant.now())
                .build());
        try {
            rankUpPaymentLockRepository.save(rankUpPaymentLock);
        } catch (DuplicateKeyException e) {
            throw new SivantisException(
                    "Payment already exists for user " + userId + " and type Master Pass" );
        }

        messageService.verifyingTransactionMessage(userId, TransactionVerificationFunctionEnum.MasterPass, transactionHash, null);
        addTransactionVerificationToQueue(
                null,
                userId,
                transactionHash,
                TransactionVerificationFunctionEnum.MasterPass,
                BigInteger.ZERO
        );
    }

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
            Payment.setStatus(PaymentEnum.VerifyingTransaction);
            Payment.setTransactionHash(bidPaymentRequest.getTransactionHash());
            Payment.setPaymentDate(Instant.now());
            Payment.setManaToCreator(totalManaAmount * .9);
            addToListOfBuyersAndHype(content.getContentId(), userID, Payment.getManaAmount(), Payment.getDollarAmount(), auctionModifier);
            paymentRepository.save(Payment);
            messageService.verifyingTransactionMessage(userID, TransactionVerificationFunctionEnum.PlaceBid, bidPaymentRequest.getTransactionHash(), content);
            addTransactionVerificationToQueue(
                    content.getContentId(),
                    userID,
                    bidPaymentRequest.getTransactionHash(),
                    TransactionVerificationFunctionEnum.PlaceBid,
                    Convert.toWei(new BigDecimal(bidPaymentRequest.getManaAmount()), Convert.Unit.ETHER)
                            .toBigIntegerExact()
            );
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
                    .status(PaymentEnum.VerifyingTransaction)
                    .manaToCreator(totalManaAmount * .9)
                    .paymentRevertInfo(new HashMap<>())
                    .transactionHash(bidPaymentRequest.getTransactionHash())
                    .build();
            addPurchasedContent(userID, newPayment.getContentId(), newPayment.getPaymentId());
            addToListOfBuyersAndHype(content.getContentId(), userID, newPayment.getManaAmount(), newPayment.getDollarAmount(), auctionModifier);
            paymentRepository.save(newPayment);
            messageService.verifyingTransactionMessage(userID, TransactionVerificationFunctionEnum.PlaceBid, bidPaymentRequest.getTransactionHash(), content);
            addTransactionVerificationToQueue(
                    content.getContentId(),
                    userID,
                    bidPaymentRequest.getTransactionHash(),
                    TransactionVerificationFunctionEnum.PlaceBid,
                    Convert.toWei(new BigDecimal(bidPaymentRequest.getManaAmount()), Convert.Unit.ETHER)
                            .toBigIntegerExact()
            );
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

        mongoTemplate.updateFirst(query, update, Content.class);
    }

    public void removeFromListOfBuyersAndHype(String contentId, String userIdOrChannelName, BigDecimal dollarAmount, Double modifier) {
        Query query = new Query(Criteria.where("_id").is(contentId));
        BigDecimal decrease = dollarAmount.multiply(BigDecimal.valueOf(modifier)).negate();

        Update update = new Update()
                .unset("listOfBuyerIds." + userIdOrChannelName)
                .inc("hype", decrease);

        mongoTemplate.updateFirst(query, update, Content.class);
    }

    public void addPurchasedContent(String userId, String contentId, String paymentId) {
        Query query = new Query(Criteria.where("_id").is(userId));
        Update update = new Update().set("purchasedContent." + contentId, paymentId);

        mongoTemplate.updateFirst(
                query,
                update,
                Users.class
        );
    }


    public void addChannelPurchasedContent(String channelId, String contentId, String paymentId) {
        Query query = new Query(Criteria.where("_id").is(channelId));
        Update update = new Update().set("purchasedContent." + contentId, paymentId);
        mongoTemplate.updateFirst(
                query,
                update,
                Channels.class
        );
    }


    // @dev Increases the amount of mana the user previously bid
    public void updatePurchasedContent(BidPaymentRequest bidPaymentRequest, String JWT) {
        var userID = jwtService.extractUserId(JWT);
        var Payment = findPayment(userID, bidPaymentRequest.getContentID());
        if(Payment == null){
            throw new SivantisException("Bid Doesn't exist");
        }
        if(!Payment.getStatus().equals(PaymentEnum.PendingPurchase)){
            throw new SivantisException("Bid cannot be raised for this video");
        }
        var user = userRepository.findById(userID).orElseThrow();
        var content = contentRepository.findById(bidPaymentRequest.getContentID()).orElseThrow();
        if(!content.getContentEnum().equals(ContentEnum.Active)){
            throw new SivantisException("This video is no longer active");
        }

        var paymentRevertInfo = PaymentRevertInfo.builder()
                .dollarAmount(bidPaymentRequest.getDollarAmount())
                .previousPaymentDate(Payment.getPaymentDate())
                .manaAmount(bidPaymentRequest.getManaAmount())
                .manaToCreator(Payment.getManaToCreator())
                .build();

        var previousManaAmount = content.getListOfBuyerIds().get(userID);
        BigInteger previousManaWei = Convert.toWei(new BigDecimal(previousManaAmount), Convert.Unit.ETHER)
                .toBigIntegerExact();
        BigInteger newManaWei = previousManaWei.add(
                Convert.toWei(new BigDecimal(bidPaymentRequest.getManaAmount()), Convert.Unit.ETHER)
                        .toBigIntegerExact()
        );
        var newManaAmountInEther = Convert.fromWei(new BigDecimal(newManaWei), Convert.Unit.ETHER);
        Payment.setManaAmount(newManaAmountInEther.toPlainString());
        Payment.setDollarAmount(Payment.getDollarAmount().add(bidPaymentRequest.getDollarAmount()));
        Payment.setPaymentDate(Instant.now());
        Payment.setManaToCreator(newManaAmountInEther.doubleValue() * .9);
        addToListOfBuyersAndHype(content.getContentId(), userID, Payment.getManaAmount(), bidPaymentRequest.getDollarAmount(), auctionModifier);
        messageService.verifyingTransactionMessage(userID, TransactionVerificationFunctionEnum.RaiseBid, bidPaymentRequest.getTransactionHash(), content);
        var transactionVerificationId = addTransactionVerificationToQueue(
                content.getContentId(),
                userID,
                bidPaymentRequest.getTransactionHash(),
                TransactionVerificationFunctionEnum.RaiseBid,
                newManaWei
        );
        Payment.getPaymentRevertInfo().put(transactionVerificationId, paymentRevertInfo);
        paymentRepository.save(Payment);
    }


    // @dev Sets the purchase entity to refunded and removes from ListOfBuyerIds
    public void refundPurchasedContent(RefundRequest refundRequest, String JWT) {
        var userID = jwtService.extractUserId(JWT);
        var user = userRepository.findById(userID).orElseThrow();
        var Payment = findPayment(userID, refundRequest.getContentID());
        if(Payment == null){
            throw new SivantisException("Bid Doesn't exist");
        }
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
        return paymentRepository.findById(userRepository.findById(userID).orElseThrow().getPurchasedContent().get(contentID)).orElse(null);
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

    private ArrayList<MultiCallResponse> sendMultiCallTransaction(List<byte[]> calls){
        return multiSendHelperService.callServerSideMultiSend(calls, ContractFunctionEnum.IncreaseCreatorRank);
    }

    private byte[] buildCall(String functionName, List parameters){
        Function function = new Function(
                functionName,
                parameters,
                Collections.emptyList()
        );
        return multiSendHelperService.buildCall(BigInteger.ZERO, function);
    }

    public String addTransactionVerificationToQueue(
            String contentId,
            String userId,
            String transactionHash,
            TransactionVerificationFunctionEnum transactionVerificationFunctionEnum,
            BigInteger expectedManaAmount
    ){
        TransactionVerificationPackage transactionVerificationPackage = TransactionVerificationPackage.builder()
                .transactionVerificationId(ObjectId.get().toHexString())
                .transactionVerificationFunctionEnum(transactionVerificationFunctionEnum)
                .contentID(contentId)
                .transactionHash(transactionHash)
                .userId(userId)
                .status(BlockchainInteractionStatusEnum.Awaiting)
                .expectedManaAmount(expectedManaAmount)
                .build();
        transactionVerificationPackageRepository.save(transactionVerificationPackage);
        return transactionVerificationPackage.getTransactionVerificationId();
    }

    private SivantisContractLogs createMultiCallLog(List<ContractFunctionDetails> contractFunctionDetails, Integer transactionCount){
        return SivantisContractLogs.builder()
                .logId(ObjectId.get().toHexString())
                .creationDate(Instant.now())
                .contractEnum(ContractEnum.MultiCall)
                .contractFunctionDetails(contractFunctionDetails)
                .transactionCount(transactionCount)
                .build();
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

    public void failedRefundPurchasedContent(List<Users> users, List<Content> content) {
        List<Users> usersList = new ArrayList<>();
        List<Content> contentList = new ArrayList<>();
        List<String> manaAmountList = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            var Payment = findPayment(users.get(i).getUserId(), content.get(i).getContentId());
            if(Payment != null){

                usersList.add(users.get(i));
                contentList.add(content.get(i));
                manaAmountList.add(Payment.getManaAmount());
            }
        }
        messageService.failedRefundMessage(usersList, manaAmountList, contentList);
    }
}
