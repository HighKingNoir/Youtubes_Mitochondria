package Project_Noir.Athena.Service;

import Project_Noir.Athena.Model.*;
import Project_Noir.Athena.Repo.ChannelRepository;
import Project_Noir.Athena.Repo.ContentRepository;
import Project_Noir.Athena.Repo.WatchNowPayLaterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;

import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractServiceInterface {


    private final WatchNowPayLaterRepository watchNowPayLaterRepository;
    private final ClientSideMultiSendContractService clientSideMultiSendContractService;
    private final MultiSendHelperService multiSendHelperService;
    private final ContentRepository contentRepository;
    private final ChannelRepository channelRepository;
    private final PaymentService paymentService;
    private final MongoTemplate mongoTemplate;

    //BidService
    // @dev Creates a new auction struct in the bid Service contract
    public void createNewAuction(String contentID, int maxWinners, int minEntryCost, String creatorAddress) {
        Function function = new Function(
                "createNewAuction",
                List.of(
                        new Utf8String(contentID),
                        new Uint256(maxWinners),
                        new Uint256(minEntryCost),
                        new Address(creatorAddress)
                ),
                Collections.emptyList()
        );
        var contractFunctionDetails = ContractFunctionDetails.builder()
                .contractFunctionEnum(ContractFunctionEnum.CreateNewAuction)
                .contentID(contentID)
                .build();
        var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                .packageId(ObjectId.get().toHexString())
                .contractFunctionDetails(contractFunctionDetails)
                .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                .build();
        clientSideMultiSendContractService.addToQueue(clientSideMultiCallPackage);
    }

    public void returnBidReport(String contentID, String reporterID, String manaAmount){
        Function function = new Function(
                "returnBid",
                List.of(
                        new Utf8String(contentID),
                        new Utf8String(reporterID)
                ),
                Collections.emptyList()
        );
        var contractFunctionDetails = ContractFunctionDetails.builder()
                .contractFunctionEnum(ContractFunctionEnum.ReturnBid)
                .userID(reporterID)
                .contentID(contentID)
                .manaAmount(Double.parseDouble(manaAmount))
                .build();
        var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                .packageId(ObjectId.get().toHexString())
                .contractFunctionDetails(contractFunctionDetails)
                .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                .build();
        clientSideMultiSendContractService.addToQueue(clientSideMultiCallPackage);
        paymentService.refundPurchasedContent(reporterID, contentID, null);
    }

    public void reactivateAuction(Content content)  {
        if(content.getListOfBuyerIds().isEmpty()){
            return;
        }
        var userIDs = content.getListOfBuyerIds().keySet().stream().toList();
        List<String> previousWinners = userIDs.subList(0, Math.min(content.getNumbBidders(), userIDs.size()));
        Function function = new Function(
                "setAuctionToActive",
                List.of(
                        new Utf8String(content.getContentId()),
                        new DynamicArray<>(
                                Utf8String.class,
                                previousWinners.stream().map(Utf8String::new).toList()
                        )
                ),
                Collections.emptyList()
        );
        var contractFunctionDetails = ContractFunctionDetails.builder()
                .contractFunctionEnum(ContractFunctionEnum.SetAuctionToActive)
                .contentID(content.getContentId())
                .build();
        var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                .packageId(ObjectId.get().toHexString())
                .contractFunctionDetails(contractFunctionDetails)
                .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                .build();
        clientSideMultiSendContractService.addToQueue(clientSideMultiCallPackage);
    }

    //ChannelService
    public void createChannel(String channelName, Double averageWeeklyViewers)  {
        Function function = new Function(
                "addChannel",
                List.of(
                        new Utf8String(channelName),
                        new Uint256(Math.round(averageWeeklyViewers))
                ),
                Collections.emptyList()
        );
        var contractFunctionDetails = ContractFunctionDetails.builder()
                .contractFunctionEnum(ContractFunctionEnum.AddChannel)
                .channelName(channelName)
                .build();
        var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                .packageId(ObjectId.get().toHexString())
                .contractFunctionDetails(contractFunctionDetails)
                .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                .build();
        clientSideMultiSendContractService.addToQueue(clientSideMultiCallPackage);
    }

    public void payForContent(Channels channel,
                              Content content,
                              Double priceOfMana,
                              Double averageWeeklyViewers)  {
        String contentCreatorID = content.getCreatorID();
        String contentID = content.getContentId();
        String contentType = content.getContentType();
        var contentPricePerHundred = priceOfContent(contentType);
        var totalManaAmount = contentPricePerHundred * averageWeeklyViewers / (priceOfMana * 100);
        Function function = new Function(
                "payForContent",
                List.of(
                        new Utf8String(channel.getChannelName()),
                        new Utf8String(contentCreatorID),
                        new Utf8String(contentID),
                        new Uint256(contentPricePerHundred)
                ),
                Collections.emptyList()
        );
        var contractFunctionDetails = ContractFunctionDetails.builder()
                .contractFunctionEnum(ContractFunctionEnum.PayForContent)
                .channelName(channel.getChannelName())
                .contentID(contentID)
                .userID(contentCreatorID)
                .manaAmount(totalManaAmount)
                .build();
        var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                .packageId(ObjectId.get().toHexString())
                .contractFunctionDetails(contractFunctionDetails)
                .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                .build();
        paymentService.purchaseChannelContent(
                channel,
                content,
                contentPricePerHundred,
                averageWeeklyViewers);
        clientSideMultiSendContractService.addToQueue(clientSideMultiCallPackage);
    }

    public void watchNowPayLater(Channels channel,
                                 Content content,
                                 int paymentIncrements,
                                 Double priceOfMana,
                                 Double averageWeeklyViewers)  {
        String contentCreatorID = content.getCreatorID();
        String contentID = content.getContentId();
        String contentType = content.getContentType();
        var contentPricePerHundred = priceOfContent(contentType);
        var totalManaAmount = contentPricePerHundred * averageWeeklyViewers / (priceOfMana * 100);
        Function function = new Function(
                "watchNowPayLater",
                List.of(
                        new Utf8String(channel.getChannelName()),
                        new Utf8String(contentCreatorID),
                        new Utf8String(contentID),
                        new Uint256(contentPricePerHundred),
                        new Uint8(paymentIncrements)
                ),
                Collections.emptyList()
        );
        var contractFunctionDetails = ContractFunctionDetails.builder()
                .contractFunctionEnum(ContractFunctionEnum.WatchNowPayLater)
                .contentID(contentID)
                .channelName(channel.getChannelName())
                .userID(contentCreatorID)
                .manaAmount(totalManaAmount)
                .build();
        var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                .packageId(ObjectId.get().toHexString())
                .contractFunctionDetails(contractFunctionDetails)
                .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                .build();
        paymentService.purchaseChannelContent(
                channel,
                content,
                contentPricePerHundred,
                averageWeeklyViewers);
        clientSideMultiSendContractService.addToQueue(clientSideMultiCallPackage);
        var watchNowPlayLater = WatchNowPayLater.builder()
                .watchNowPlayLaterId(ObjectId.get().toHexString())
                .channelName(channel.getChannelName())
                .contentID(contentID)
                .watchNowPayLaterEnum(WatchNowPayLaterEnum.Unpaid)
                .manaIncrements(totalManaAmount / paymentIncrements)
                .paymentsLeft(paymentIncrements - 1)
                .nextPaymentDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .build() ;
        watchNowPayLaterRepository.save(watchNowPlayLater);
        addWatchNowPlayLaterIdToChannel(channel.getChannelId(), watchNowPlayLater.getWatchNowPlayLaterId());
    }

    private void addWatchNowPlayLaterIdToChannel(String channelId, String watchNowPlayLaterId) {
        Query query = new Query(Criteria.where("_id").is(channelId));
        Update update = new Update().push("watchNowPayLaterIDs", watchNowPlayLaterId);
        mongoTemplate.updateFirst(
                query,
                update,
                Channels.class
        );
    }

    public void updateAverageWeeklyViewers(String channelName, Double newAverageWeeklyViewers)  {
        Function function = new Function(
                "updateAverageWeeklyViewers",
                List.of(
                        new Utf8String(channelName),
                        new Uint256(Math.round(newAverageWeeklyViewers))
                ),
                Collections.emptyList()
        );
        var contractFunctionDetails = ContractFunctionDetails.builder()
                .contractFunctionEnum(ContractFunctionEnum.UpdateAverageWeeklyViewers)
                .channelName(channelName)
                .build();
        var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                .packageId(ObjectId.get().toHexString())
                .contractFunctionDetails(contractFunctionDetails)
                .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                .build();
        clientSideMultiSendContractService.addToQueue(clientSideMultiCallPackage);
    }



    //WarChestService
    public void addContentCreator(String creatorID, String creatorPersonalWallet, int rank)  {
        Function function = new Function(
                "addContentCreator",
                List.of(
                        new Utf8String(creatorID),
                        new Address(creatorPersonalWallet),
                        new Uint8(rank)
                ),
                Collections.emptyList()
        );
        var contractFunctionDetails = ContractFunctionDetails.builder()
                .contractFunctionEnum(ContractFunctionEnum.AddContentCreator)
                .userID(creatorID)
                .build();
        var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                .packageId(ObjectId.get().toHexString())
                .contractFunctionDetails(contractFunctionDetails)
                .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                .build();
        clientSideMultiSendContractService.addToQueue(clientSideMultiCallPackage);
    }

    public void CancelPayment(String creatorID, String contentID, String channelName, String manaAmount)  {
        Function function = new Function(
                "sendRefundPayment",
                List.of(
                        new Utf8String(creatorID),
                        new Utf8String(contentID),
                        new Utf8String(channelName)
                ),
                Collections.emptyList()
        );
        var contractFunctionDetails = ContractFunctionDetails.builder()
                .contractFunctionEnum(ContractFunctionEnum.CancelPayment)
                .manaAmount(Double.parseDouble(manaAmount))
                .userID(creatorID)
                .contentID(contentID)
                .channelName(channelName)
                .build();
        var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                .packageId(ObjectId.get().toHexString())
                .contractFunctionDetails(contractFunctionDetails)
                .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                .build();
        clientSideMultiSendContractService.addToQueue(clientSideMultiCallPackage);
        paymentService.pendingRefundChannelPurchasedContent(channelName, contentID);
    }


    public void CancelWatchNowPayLaterPayment(WatchNowPayLater watchNowPayLater, String creatorID, String contentID, String channelName, String manaAmount)  {
        Function function = new Function(
                "sendWatchNowPayLaterRefundPayment",
                List.of(
                        new Utf8String(creatorID),
                        new Utf8String(contentID),
                        new Utf8String(channelName)
                ),
                Collections.emptyList()
        );
        var contractFunctionDetails = ContractFunctionDetails.builder()
                .contractFunctionEnum(ContractFunctionEnum.CancelWatchNowPayLater)
                .manaAmount(Double.parseDouble(manaAmount))
                .userID(creatorID)
                .contentID(contentID)
                .channelName(channelName)
                .build();
        var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                .packageId(ObjectId.get().toHexString())
                .contractFunctionDetails(contractFunctionDetails)
                .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                .build();
        clientSideMultiSendContractService.addToQueue(clientSideMultiCallPackage);
        paymentService.pendingRefundChannelPurchasedContent(channelName, contentID);
        removeWatchNowPlayLaterIdToChannel(channelName, watchNowPayLater.getWatchNowPlayLaterId());
        watchNowPayLaterRepository.delete(watchNowPayLater);
    }

    private void removeWatchNowPlayLaterIdToChannel(String channelName, String watchNowPlayLaterId) {
        Query query = new Query(Criteria.where("channelName").is(channelName));
        Update update = new Update().pull("watchNowPayLaterIDs", watchNowPlayLaterId);
        mongoTemplate.updateFirst(
                query,
                update,
                Channels.class
        );
    }

    public void reactivateContent(Content content) {
        if(content.getListOfBuyerIds().isEmpty()){
            return;
        }
        var channelNames = content.getListOfBuyerIds().keySet().stream().toList();
        Function function = new Function(
                "reactivateContent",
                List.of(
                        new Utf8String(content.getContentId()),
                        new DynamicArray<>(
                                Utf8String.class,
                                channelNames.stream().map(Utf8String::new).toList()
                        )
                ),
                Collections.emptyList()
        );
        var contractFunctionDetails = ContractFunctionDetails.builder()
                .contractFunctionEnum(ContractFunctionEnum.ReactivateContent)
                .contentID(content.getContentId())
                .build();
        var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                .packageId(ObjectId.get().toHexString())
                .contractFunctionDetails(contractFunctionDetails)
                .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                .build();
        clientSideMultiSendContractService.addToQueue(clientSideMultiCallPackage);
    }

    public void updatePersonalWallet(String creatorID, String _newPersonalWallet) {
        Function function = new Function(
                "updatePersonalWallet",
                List.of(
                        new Utf8String(creatorID),
                        new Address(_newPersonalWallet)
                ),
                Collections.emptyList()
        );
        var contractFunctionDetails = ContractFunctionDetails.builder()
                .contractFunctionEnum(ContractFunctionEnum.UpdatePersonalWallet)
                .userID(creatorID)
                .build();
        var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                .packageId(ObjectId.get().toHexString())
                .contractFunctionDetails(contractFunctionDetails)
                .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                .build();
        clientSideMultiSendContractService.addToQueue(clientSideMultiCallPackage);
    }


    public int priceOfContent(String contentType){
        return switch (contentType){
            case "Short Film" -> 5;
            case "Sports", "Concerts" -> 10;
            case "Movies" -> 20;
            default -> throw new IllegalStateException("Unexpected value: " + contentType);
        };
    }

    private Double getManaAmount(List<ContractFunctionDetails> functionDetails){
        return functionDetails.stream().map(ContractFunctionDetails::getManaAmount).mapToDouble(Double::doubleValue).sum();
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

}

