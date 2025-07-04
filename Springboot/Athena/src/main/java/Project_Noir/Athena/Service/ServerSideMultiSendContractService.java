package Project_Noir.Athena.Service;

import Project_Noir.Athena.Model.*;
import Project_Noir.Athena.Repo.*;
import com.google.errorprone.annotations.Var;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.checkerframework.checker.units.qual.C;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;

import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServerSideMultiSendContractService {
    private final MultiSendHelperService multiSendHelperService;
    private final SivantisContractLogsRepository sivantisContractLogsRepository;
    private final WatchNowPayLaterRepository watchNowPayLaterRepository;
    private final ContentRepository contentRepository;
    private final ChannelRepository channelRepository;
    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final MessageService messageService;



    public void sendWeeklyManaMultiCall(List<String> contentCreatorIds){
        if(!contentCreatorIds.isEmpty()){
            var contractFunctionDetailsList = new ArrayList<ContractFunctionDetails>();
            List<byte[]> calls = new java.util.ArrayList<>(List.of());
            for(String creatorId: contentCreatorIds){
                calls.add(buildCall("sendWeeklyMana", List.of(new Utf8String(creatorId))));
                var contractFunctionDetails = ContractFunctionDetails.builder()
                        .contentID(creatorId)
                        .contractFunctionEnum(ContractFunctionEnum.SendWeeklyMana)
                        .build();
                contractFunctionDetailsList.add(contractFunctionDetails);
            }
            var SendWeeklyManaMultiCallTransactionResponse = sendMultiCallTransaction(calls, ContractFunctionEnum.SendWeeklyMana);
            int index = 0;
            for(MultiCallResponse multiCallResponse: SendWeeklyManaMultiCallTransactionResponse){
                List<ContractFunctionDetails> subList = contractFunctionDetailsList.subList(
                        index,
                        index + multiCallResponse.getTransactionCount()
                );
                var SendWeeklyManaMultiCallLog = createMultiCallLog(subList, multiCallResponse.getTransactionCount());
                SendWeeklyManaMultiCallLog.setContractTransactionReceipt(multiCallResponse.getContractTransactionReceipt());
                sivantisContractLogsRepository.save(SendWeeklyManaMultiCallLog);
                index += multiCallResponse.getTransactionCount();
            }
        }
    }

    public void watchNowPayLaterPaymentsMultiCall(List<WatchNowPayLater> watchNowPayLaterPayments){
        List<WatchNowPayLater> watchNowPayLaterList = new ArrayList<>();
        if(!watchNowPayLaterPayments.isEmpty()){
            var contractFunctionDetailsList = new ArrayList<ContractFunctionDetails>();
            List<byte[]> calls = new java.util.ArrayList<>(List.of());
            for(WatchNowPayLater watchNowPayLater : watchNowPayLaterPayments){
                calls.add(buildCall("watchNowPayLaterPayment", Arrays.asList(new Utf8String(watchNowPayLater.getChannelName()), new Utf8String(watchNowPayLater.getContentID()))));
                var contractFunctionDetails = ContractFunctionDetails.builder()
                        .contentID(watchNowPayLater.getContentID())
                        .channelName(watchNowPayLater.getChannelName())
                        .contractFunctionEnum(ContractFunctionEnum.WatchNowPayLaterPayment)
                        .build();
                contractFunctionDetailsList.add(contractFunctionDetails);
            }
            int index = 0;
            var watchNowPayLaterPaymentMultiCallTransactionResponse = sendMultiCallTransaction(calls, ContractFunctionEnum.WatchNowPayLaterPayment);
            for(MultiCallResponse multiCallResponse: watchNowPayLaterPaymentMultiCallTransactionResponse){
                List<ContractFunctionDetails> subList = contractFunctionDetailsList.subList(
                        index,
                        index + multiCallResponse.getTransactionCount()
                );
                var contractTransactionReceipt = multiCallResponse.getContractTransactionReceipt();
                var watchNowPayLaterPaymentMultiCallLog = createMultiCallLog(subList, multiCallResponse.getTransactionCount());
                watchNowPayLaterPaymentMultiCallLog.setContractTransactionReceipt(contractTransactionReceipt);
                sivantisContractLogsRepository.save(watchNowPayLaterPaymentMultiCallLog);
                if(contractTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Completed)){
                    var channelNames = subList.stream().map(ContractFunctionDetails::getChannelName).toList();
                    var contentId = subList.stream().map(ContractFunctionDetails::getContentID).toList();
                    List<WatchNowPayLater> subWatchNowPayLaterList = new ArrayList<>();
                    for (int i = 0; i < channelNames.size(); i++) {
                        watchNowPayLaterRepository.findByChannelNameAndContentID(channelNames.get(i), contentId.get(i))
                                .ifPresent(subWatchNowPayLaterList::add);
                    }
                    for(WatchNowPayLater watchNowPayLater : subWatchNowPayLaterList){
                        var paymentsLeft = watchNowPayLater.getPaymentsLeft() - 1;
                        watchNowPayLater.setPaymentsLeft(paymentsLeft);
                        if(paymentsLeft == 0){
                            watchNowPayLater.setWatchNowPayLaterEnum(WatchNowPayLaterEnum.Paid);
                        }
                        else{
                            watchNowPayLater.setNextPaymentDate(Instant.now().plus(7, ChronoUnit.DAYS));
                        }
                        watchNowPayLaterList.add(watchNowPayLater);
                    }
                }
                index += multiCallResponse.getTransactionCount();
            }
            watchNowPayLaterRepository.saveAll(watchNowPayLaterList);
        }
    }

    public void successfulVideoMultiCall(List<Content> successfulVideoContent){
        if(!successfulVideoContent.isEmpty()){
            var sendManaFunctionDetailsList = new ArrayList<ContractFunctionDetails>();
            var increaseCreatorRankFunctionDetailsList = new ArrayList<ContractFunctionDetails>();
            List<byte[]> sendManaCalls = new java.util.ArrayList<>(List.of());
            List<byte[]> increaseCreatorRankCalls = new java.util.ArrayList<>(List.of());
            double totalEarnings = 0;
            messageService.successfulVideoMessage(successfulVideoContent);
            for(Content content : successfulVideoContent){
                if(isAuction(content.getContentType()) && !content.getListOfBuyerIds().isEmpty()){
                    var buyers = convertToDoubleMapAndSort(content.getListOfBuyerIds()).entrySet().stream().toList();
                    var maxWinners = content.getNumbBidders();
                    if(buyers.size() < maxWinners){
                        for (Map.Entry<String, Double> buyer : buyers) {
                            totalEarnings += buyer.getValue();
                        }
                    }
                    else {
                        for(int index = 0; index < maxWinners; index++){
                            totalEarnings += buyers.get(index).getValue();
                        }
                    }
                    sendManaCalls.add(buildCall("sendMana", List.of(new Utf8String(content.getContentId()))));
                    var sendManaFunctionDetails = ContractFunctionDetails.builder()
                            .contentID(content.getContentId())
                            .contractFunctionEnum(ContractFunctionEnum.SendMana)
                            .manaAmount(totalEarnings)
                            .build();
                    sendManaFunctionDetailsList.add(sendManaFunctionDetails);
                }
            }
            var users = userRepository.findAllById(successfulVideoContent.stream().map(Content::getCreatorID).toList());
            for (Users user: users){
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
            }

            if(!sendManaCalls.isEmpty()){
                var sendManaMultiCallTransactionResponse = sendMultiCallTransaction(sendManaCalls, ContractFunctionEnum.SendMana);
                int sendManaMultiCallIndex = 0;
                for (MultiCallResponse multiCallResponse: sendManaMultiCallTransactionResponse){
                    List<ContractFunctionDetails> sendManaSubList = sendManaFunctionDetailsList.subList(
                            sendManaMultiCallIndex,
                            sendManaMultiCallIndex + multiCallResponse.getTransactionCount()
                    );
                    var sendManaTransactionReceipt = multiCallResponse.getContractTransactionReceipt();
                    var sendManaMultiCallLog = createMultiCallLog(sendManaSubList, multiCallResponse.getTransactionCount());
                    sendManaMultiCallLog.setContractTransactionReceipt(sendManaTransactionReceipt);
                    var manaAmount = getManaAmount(sendManaSubList);
                    if(sendManaTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Completed)){
                        sendManaMultiCallLog.setTotalManaAmount(manaAmount);
                        sendManaMultiCallLog.setManaToCompany(manaAmount / 15);
                    }
                    sivantisContractLogsRepository.save(sendManaMultiCallLog);
                    sendManaMultiCallIndex += multiCallResponse.getTransactionCount();
                }
            }
            if(!increaseCreatorRankCalls.isEmpty()){
                var increaseCreatorRankMultiCallTransactionResponse = sendMultiCallTransaction(increaseCreatorRankCalls, ContractFunctionEnum.IncreaseCreatorRank);
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
    }

    public void returnAllManaMultiCall(List<Content> returnAllManaContent){
        if(returnAllManaContent.isEmpty()){
            return;
        }
        List<byte[]> returnBidCalls = new java.util.ArrayList<>(List.of());
        List<byte[]> setAuctionToInactiveCalls = new java.util.ArrayList<>(List.of());
        List<byte[]> sendRefundPaymentCalls = new java.util.ArrayList<>(List.of());
        List<byte[]> sendWatchNowPayLaterRefundPaymentCalls = new java.util.ArrayList<>(List.of());
        var sendRefundPaymentFunctionDetailsList = new ArrayList<ContractFunctionDetails>();
        var sendWatchNowPayLaterRefundPaymentFunctionDetailsList = new ArrayList<ContractFunctionDetails>();
        var returnBidFunctionDetailsList = new ArrayList<ContractFunctionDetails>();
        var setAuctionToInactiveFunctionDetailsList = new ArrayList<ContractFunctionDetails>();
        for(Content content : returnAllManaContent){
            var contentId = content.getContentId();
            var buyers = content.getListOfBuyerIds();
            if(!isAuction(content.getContentType())){
                for (Map.Entry<String, String> buyer : buyers.entrySet()) {
                    var channelName = buyer.getKey();
                    var optionalWatchNowPayLater = watchNowPayLaterRepository.findByChannelNameAndContentID(channelName, contentId);
                    if(optionalWatchNowPayLater.isPresent() && optionalWatchNowPayLater.get().getWatchNowPayLaterEnum().equals(WatchNowPayLaterEnum.Unpaid)){
                        var parameters = Arrays.asList(new Utf8String(content.getCreatorID()), new Utf8String(contentId), new Utf8String(channelName));
                        sendWatchNowPayLaterRefundPaymentCalls.add(buildCall("sendWatchNowPayLaterRefundPayment", parameters));
                        var sendWatchNowPayLaterRefundPaymentFunctionDetails = ContractFunctionDetails.builder()
                                .contentID(content.getContentId())
                                .channelName(channelName)
                                .userID(content.getCreatorID())
                                .manaAmount(Double.valueOf(buyer.getValue()))
                                .contractFunctionEnum(ContractFunctionEnum.SendWatchNowPayLaterRefundPayment)
                                .build();
                        sendWatchNowPayLaterRefundPaymentFunctionDetailsList.add(sendWatchNowPayLaterRefundPaymentFunctionDetails);
                    }
                    else{
                        var parameters = Arrays.asList(new Utf8String(content.getCreatorID()), new Utf8String(contentId), new Utf8String(channelName));
                        sendRefundPaymentCalls.add(buildCall("sendRefundPayment", parameters));
                        var sendRefundPaymentFunctionDetails = ContractFunctionDetails.builder()
                                .contentID(content.getContentId())
                                .userID(content.getCreatorID())
                                .channelName(channelName)
                                .manaAmount(Double.valueOf(buyer.getValue()))
                                .contractFunctionEnum(ContractFunctionEnum.SendRefundPayment)
                                .build();
                        sendRefundPaymentFunctionDetailsList.add(sendRefundPaymentFunctionDetails);
                    }
                }
            }
            else{
                if(!buyers.isEmpty()){
                    var userIdsThatCancelled = multiSendHelperService.getUsersThatCancelledBid(content);
                    for (String userID: userIdsThatCancelled){
                        content.getListOfBuyerIds().remove(userID);
                    }
                    if(!userIdsThatCancelled.isEmpty()){
                        contentRepository.save(content);
                    }
                    for (Map.Entry<String, String> buyer : content.getListOfBuyerIds().entrySet()) {
                        var userID = buyer.getKey();
                        returnBidCalls.add(buildCall("returnBid", Arrays.asList(new Utf8String(contentId), new Utf8String(userID))));
                        var returnBidFunctionDetails = ContractFunctionDetails.builder()
                                .contentID(contentId)
                                .userID(userID)
                                .manaAmount(Double.valueOf(buyer.getValue()))
                                .contractFunctionEnum(ContractFunctionEnum.ReturnBid)
                                .build();
                        returnBidFunctionDetailsList.add(returnBidFunctionDetails);
                    }
                }
                setAuctionToInactiveCalls.add(buildCall("setAuctionToInactive", List.of(new Utf8String(contentId))));
                var setAuctionToInactiveFunctionDetails = ContractFunctionDetails.builder()
                        .contentID(contentId)
                        .contractFunctionEnum(ContractFunctionEnum.SetAuctionToInactive)
                        .build();
                setAuctionToInactiveFunctionDetailsList.add(setAuctionToInactiveFunctionDetails);
            }
        }
        returnBidsAndInactiveAuction(returnBidCalls, setAuctionToInactiveCalls, returnBidFunctionDetailsList, setAuctionToInactiveFunctionDetailsList);
        if(!sendRefundPaymentCalls.isEmpty()){
            var sendRefundPaymentMultiCallTransactionResponse = sendMultiCallTransaction(sendRefundPaymentCalls, ContractFunctionEnum.SendRefundPayment);
            int sendRefundPaymentMultiCallIndex = 0;
            for(MultiCallResponse multiCallResponse: sendRefundPaymentMultiCallTransactionResponse){
                List<ContractFunctionDetails> sendRefundPaymentSubList = sendRefundPaymentFunctionDetailsList.subList(
                        sendRefundPaymentMultiCallIndex,
                        sendRefundPaymentMultiCallIndex + multiCallResponse.getTransactionCount()
                );
                var sendRefundPaymentTransactionReceipt = multiCallResponse.getContractTransactionReceipt();
                var sendRefundPaymentMultiCallLog = createMultiCallLog(sendRefundPaymentSubList, multiCallResponse.getTransactionCount());
                var manaAmount = getManaAmount(sendRefundPaymentSubList);
                sendRefundPaymentMultiCallLog.setContractTransactionReceipt(sendRefundPaymentTransactionReceipt);
                sendRefundPaymentMultiCallLog.setTotalManaAmount(manaAmount);
                sivantisContractLogsRepository.save(sendRefundPaymentMultiCallLog);
                var channelNames = sendRefundPaymentSubList.stream().map(ContractFunctionDetails::getChannelName).toList();
                var contentIds = sendRefundPaymentSubList.stream().map(ContractFunctionDetails::getContentID).toList();
                if (sendRefundPaymentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)) {
                    if(hasDuplicates(channelNames) || hasDuplicates(contentIds)){
                        for (int i = 0; i < channelNames.size(); i++) {
                            paymentService.failedRefundChannelPurchasedContent(channelNames.get(i), contentIds.get(i));
                        }
                    }
                    else {
                        var Channels = channelRepository.findByChannelNameIn(channelNames);
                        var Content = contentRepository.findByContentIdIn(contentIds);
                        paymentService.failedRefundChannelPurchasedContent(Channels, Content);
                    }
                } else {
                    for (int i = 0; i < channelNames.size(); i++) {
                        paymentService.refundChannelPurchasedContent(channelNames.get(i), contentIds.get(i), sendRefundPaymentTransactionReceipt.getTransactionHash());
                    }
                }
                sendRefundPaymentMultiCallIndex += multiCallResponse.getTransactionCount();
            }
        }
        if(!sendWatchNowPayLaterRefundPaymentCalls.isEmpty()){
            var watchNowPayLaterRefundPaymentMultiCallTransactionResponse = sendMultiCallTransaction(sendWatchNowPayLaterRefundPaymentCalls, ContractFunctionEnum.SendWatchNowPayLaterRefundPayment);
            int watchNowPayLaterRefundPaymentMultiCallIndex = 0;
            for(MultiCallResponse multiCallResponse: watchNowPayLaterRefundPaymentMultiCallTransactionResponse){
                List<ContractFunctionDetails> watchNowPayLaterRefundPaymentSubList = sendWatchNowPayLaterRefundPaymentFunctionDetailsList.subList(
                        watchNowPayLaterRefundPaymentMultiCallIndex,
                        watchNowPayLaterRefundPaymentMultiCallIndex + multiCallResponse.getTransactionCount()
                );
                var watchNowPayLaterRefundPaymentTransactionReceipt = multiCallResponse.getContractTransactionReceipt();
                var watchNowPayLaterRefundPaymentMultiCallLog = createMultiCallLog(watchNowPayLaterRefundPaymentSubList, multiCallResponse.getTransactionCount());
                var manaAmount = getManaAmount(watchNowPayLaterRefundPaymentSubList);
                watchNowPayLaterRefundPaymentMultiCallLog.setContractTransactionReceipt(watchNowPayLaterRefundPaymentTransactionReceipt);
                watchNowPayLaterRefundPaymentMultiCallLog.setTotalManaAmount(manaAmount);
                sivantisContractLogsRepository.save(watchNowPayLaterRefundPaymentMultiCallLog);
                var channelNames = watchNowPayLaterRefundPaymentSubList.stream().map(ContractFunctionDetails::getChannelName).toList();
                var contentIds = watchNowPayLaterRefundPaymentSubList.stream().map(ContractFunctionDetails::getContentID).toList();
                if(watchNowPayLaterRefundPaymentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
                    if(hasDuplicates(channelNames) || hasDuplicates(contentIds)){
                        for (int i = 0; i < channelNames.size(); i++) {
                            paymentService.failedRefundChannelPurchasedContent(channelNames.get(i), contentIds.get(i));
                        }
                    }
                    else {
                        var Channels = channelRepository.findByChannelNameIn(channelNames);
                        var Content = contentRepository.findByContentIdIn(contentIds);
                        paymentService.failedRefundChannelPurchasedContent(Channels, Content);
                    }
                }
                else{
                    List<WatchNowPayLater> watchNowPayLaterList = new ArrayList<>();
                    for (int i = 0; i < channelNames.size(); i++) {
                        var channelName = channelNames.get(i);
                        var contentID = contentIds.get(i);
                        if(channelRepository.findByChannelName(channelName).isPresent()){
                            var channel = channelRepository.findByChannelName(channelName).get();
                            paymentService.refundChannelPurchasedContent(channelNames.get(i), contentIds.get(i), watchNowPayLaterRefundPaymentTransactionReceipt.getTransactionHash());
                            if (watchNowPayLaterRepository.findByChannelNameAndContentID(channelName, contentID).isPresent()) {
                                var watchNowPayLater = watchNowPayLaterRepository.findByChannelNameAndContentID(channelName, contentID).get();
                                removeWatchNowPlayLaterIdToChannel(channelName, watchNowPayLater.getWatchNowPlayLaterId());
                                watchNowPayLaterList.add(watchNowPayLater);
                            }
                        }
                    }
                    watchNowPayLaterRepository.deleteAll(watchNowPayLaterList);
                }
                watchNowPayLaterRefundPaymentMultiCallIndex += multiCallResponse.getTransactionCount();
            }
        }
    }

    private void removeWatchNowPlayLaterIdToChannel(String channelName, String watchNowPlayLaterId) {
        Query query = new Query(Criteria.where("channelName").is(channelName));
        Update update = new Update().pull("watchNowPayLaterIDs", watchNowPlayLaterId);
        mongoTemplate.findAndModify(
                query,
                update,
                Channels.class
        );
    }

    public void releaseEmailsMultiCall(List<Content> releaseEmailsContent){
        List<byte[]> returnBidCalls = new java.util.ArrayList<>(List.of());
        List<byte[]> setAuctionToInactiveCalls = new java.util.ArrayList<>(List.of());
        var returnBidFunctionDetailsList = new ArrayList<ContractFunctionDetails>();
        var setAuctionToInactiveFunctionDetailsList = new ArrayList<ContractFunctionDetails>();
        for(Content content : releaseEmailsContent){
            var contentId = content.getContentId();
            if(isAuction(content.getContentType())) {
                if (!content.getListOfBuyerIds().isEmpty()) {
                    var userIdsThatCancelled = multiSendHelperService.getUsersThatCancelledBid(content);
                    for (String userID : userIdsThatCancelled) {
                        content.getListOfBuyerIds().remove(userID);
                    }
                    if (!userIdsThatCancelled.isEmpty()) {
                        contentRepository.save(content);
                    }
                    if (content.getListOfBuyerIds().size() > content.getNumbBidders()) {
                        var buyers = convertToDoubleMapAndSort(content.getListOfBuyerIds()).entrySet().stream().toList();
                        for (int index = content.getNumbBidders(); index < content.getListOfBuyerIds().size(); index++) {
                            var userID = buyers.get(index).getKey();
                            returnBidCalls.add(buildCall("returnBid", Arrays.asList(new Utf8String(contentId), new Utf8String(userID))));
                            var returnBidFunctionDetails = ContractFunctionDetails.builder()
                                    .contentID(contentId)
                                    .userID(userID)
                                    .manaAmount(buyers.get(index).getValue())
                                    .contractFunctionEnum(ContractFunctionEnum.ReturnBid)
                                    .build();
                            returnBidFunctionDetailsList.add(returnBidFunctionDetails);
                        }
                    }
                    setAuctionToInactiveCalls.add(buildCall("setAuctionToInactive", List.of(new Utf8String(contentId))));
                    var setAuctionToInactiveFunctionDetails = ContractFunctionDetails.builder()
                            .contentID(contentId)
                            .contractFunctionEnum(ContractFunctionEnum.SetAuctionToInactive)
                            .build();
                    setAuctionToInactiveFunctionDetailsList.add(setAuctionToInactiveFunctionDetails);
                }
            }
        }
        var users = userRepository.findAllById(releaseEmailsContent.stream().map(Content::getCreatorID).toList());
        messageService.youtubeEmailsMessage(users, releaseEmailsContent);
        returnBidsAndInactiveAuction(returnBidCalls, setAuctionToInactiveCalls, returnBidFunctionDetailsList, setAuctionToInactiveFunctionDetailsList);

    }


    private void returnBidsAndInactiveAuction(
            List<byte[]> returnBidCalls,
            List<byte[]> setAuctionToInactiveCalls,
            ArrayList<ContractFunctionDetails> returnBidFunctionDetailsList,
            ArrayList<ContractFunctionDetails> setAuctionToInactiveFunctionDetailsList
    ){
        if(!returnBidCalls.isEmpty()){
            var returnBidMultiCallTransactionResponse = sendMultiCallTransaction(returnBidCalls, ContractFunctionEnum.ReturnBid);
            int returnBidMultiCallIndex = 0;
            for(MultiCallResponse multiCallResponse: returnBidMultiCallTransactionResponse){
                List<ContractFunctionDetails> returnBidSubList = returnBidFunctionDetailsList.subList(
                        returnBidMultiCallIndex,
                        returnBidMultiCallIndex + multiCallResponse.getTransactionCount()
                );
                var returnBidTransactionReceipt = multiCallResponse.getContractTransactionReceipt();
                var returnBidMultiCallLog = createMultiCallLog(returnBidSubList, multiCallResponse.getTransactionCount());
                var manaAmount = getManaAmount(returnBidSubList);
                returnBidMultiCallLog.setContractTransactionReceipt(returnBidTransactionReceipt);
                returnBidMultiCallLog.setTotalManaAmount(manaAmount);
                sivantisContractLogsRepository.save(returnBidMultiCallLog);
                var userIDs = returnBidSubList.stream().map(ContractFunctionDetails::getUserID).toList();
                var contentIds = returnBidSubList.stream().map(ContractFunctionDetails::getContentID).toList();
                if (returnBidTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)) {
                    if (hasDuplicates(userIDs) || hasDuplicates(contentIds)){
                        for (int i = 0; i < userIDs.size(); i++) {
                            paymentService.failedRefundPurchasedContent(userIDs.get(i), contentIds.get(i));
                        }
                    }
                    else {
                        var Users = userRepository.findAllById(userIDs);
                        var Content = contentRepository.findByContentIdIn(contentIds);
                        paymentService.failedRefundPurchasedContent(Users, Content);
                    }
                } else {
                    for (int i = 0; i < userIDs.size(); i++) {
                        paymentService.refundPurchasedContent(userIDs.get(i), contentIds.get(i), returnBidTransactionReceipt.getTransactionHash());
                    }
                }
                returnBidMultiCallIndex += multiCallResponse.getTransactionCount();
            }
        }
        if(!setAuctionToInactiveCalls.isEmpty()){
            var setAuctionToInactiveMultiCallTransactionResponse = sendMultiCallTransaction(setAuctionToInactiveCalls, ContractFunctionEnum.SetAuctionToInactive);
            int setAuctionToInactiveMultiCallIndex = 0;
            for(MultiCallResponse multiCallResponse: setAuctionToInactiveMultiCallTransactionResponse){
                List<ContractFunctionDetails> setAuctionToInactiveSubList = setAuctionToInactiveFunctionDetailsList.subList(
                        setAuctionToInactiveMultiCallIndex,
                        setAuctionToInactiveMultiCallIndex + multiCallResponse.getTransactionCount()
                );
                var setAuctionToInactiveMultiCallLog = createMultiCallLog(setAuctionToInactiveSubList, multiCallResponse.getTransactionCount());
                setAuctionToInactiveMultiCallLog.setContractTransactionReceipt(multiCallResponse.getContractTransactionReceipt());
                sivantisContractLogsRepository.save(setAuctionToInactiveMultiCallLog);
                setAuctionToInactiveMultiCallIndex += multiCallResponse.getTransactionCount();
            }
        }
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

    private Double getManaAmount(List<ContractFunctionDetails> functionDetails){
        return functionDetails.stream().map(ContractFunctionDetails::getManaAmount).mapToDouble(Double::doubleValue).sum();
    }

    private ArrayList<MultiCallResponse> sendMultiCallTransaction(List<byte[]> calls, ContractFunctionEnum contractFunctionEnum){
        return multiSendHelperService.callServerSideMultiSend(calls, contractFunctionEnum);
    }

    private byte[] buildCall(String functionName, List parameters){
        Function function = new Function(
                functionName,
                parameters,
                Collections.emptyList()
        );
        return multiSendHelperService.buildCall(BigInteger.ZERO, function);
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

    private boolean isAuction(String contentType){
        return contentType.equals("Invention") || contentType.equals("Innovation");
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

    public boolean hasDuplicates(List<String> list) {
        Set<String> seen = new HashSet<>();
        for (String item : list) {
            if (!seen.add(item)) {
                return true; // Duplicate found
            }
        }
        return false; // No duplicates
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
        mongoTemplate.findAndModify(
                query,
                update,
                Users.class
        );
    }
}