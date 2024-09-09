package Project_Noir.Athena.Service;

import Project_Noir.Athena.Exception.SivantisException;
import Project_Noir.Athena.Model.*;
import Project_Noir.Athena.Repo.ChannelRepository;
import Project_Noir.Athena.Repo.ContentRepository;
import Project_Noir.Athena.Repo.SivantisContractLogsRepository;
import Project_Noir.Athena.Repo.WatchNowPayLaterRepository;
import Project_Noir.Athena.SmartContracts.BidService.BidService;
import Project_Noir.Athena.SmartContracts.WarChestService.WarChestService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractServiceInterface {

    private final InterfaceModule interfaceModule;
    private final SivantisContractLogsRepository sivantisContractLogsRepository;
    private final WatchNowPayLaterRepository watchNowPayLaterRepository;
    private final ContentRepository contentRepository;
    private final ChannelRepository channelRepository;
    private final PaymentService paymentService;
    private final BigInteger GAS_LIMIT = BigInteger.valueOf(10000000L);

    @Value("${contract.bid.address}")
    private String BidServiceAddress;

    @Value("${contract.bid.key}")
    private String privateBidKey;

    @Value("${contract.channel.address}")
    private String ChannelServiceAddress;

    @Value("${contract.channel.key}")
    private String privateChannelKey;

    @Value("${contract.warchest.address}")
    private String WarChestServiceAddress;

    @Value("${contract.warchest.key}")
    private String privateWarChestKey;

    @Value("${infura.api.secret}")
    private String infuraAPISecret;

    @Value("${infura.api.key}")
    private String infuraAPIKey;
    private Web3j web3j;

    @PostConstruct
    public void init() {
        if (infuraAPIKey != null && !infuraAPIKey.isEmpty()) {
            web3j = Web3j.build(createCustomHttpService("https://polygon-mainnet.infura.io/v3/" + infuraAPIKey));
        } else {
            web3j = Web3j.build(new HttpService());
        }
    }

    private HttpService createCustomHttpService(String url) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        // Add an interceptor to add the Bearer token to each request
        clientBuilder.addInterceptor(chain -> {
            okhttp3.Request original = chain.request();
            okhttp3.Request request = original.newBuilder()
                    .header("Authorization", Credentials.basic(infuraAPIKey, infuraAPISecret))
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(request);
        });

        return new HttpService(url, clientBuilder.build());
    }

    //BidService
    // @dev Creates a new auction struct in the bid Service contract
    public ContractStatusEnum createNewAuction(String contentID, int maxWinners, int minEntryCost, String creatorAddress)  {
        var parameters = Arrays.asList(
                new Utf8String(contentID),
                new Uint256(maxWinners),
                new Uint256(minEntryCost),
                new Address(creatorAddress)
        );
        var createNewAuctionLog = createBidServiceLogs(ContractFunctionEnum.CreateNewAuction, contentID, null);
        var createNewAuctionTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "createNewAuction", GAS_LIMIT,
                parameters, Collections.EMPTY_LIST);
        createNewAuctionLog.setContractTransactionReceipt(createNewAuctionTransactionReceipt);
        sivantisContractLogsRepository.save(createNewAuctionLog);
        return createNewAuctionTransactionReceipt.getContractStatusEnum();
    }

    public boolean returnBidReport(String contentID, String reporterID){
        var returnBidLog = createBidServiceLogs(ContractFunctionEnum.ReturnBid, contentID, reporterID);
        ContractTransactionReceipt returnBidTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "returnBid", GAS_LIMIT,
                    Arrays.asList(new Utf8String(contentID), new Utf8String(reporterID)), Collections.EMPTY_LIST);
            if(returnBidTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
                return false;
            }else{
                returnBidLog.setContractTransactionReceipt(returnBidTransactionReceipt);
                sivantisContractLogsRepository.save(returnBidLog);
                paymentService.refundPurchasedContent(reporterID, contentID, returnBidTransactionReceipt.getTransactionHash());
                return true;
            }
    }

    public void failAuction(String contentID, Map<String, String> listOfBuyers)  {
        var userIDs = listOfBuyers.keySet().stream().toList();
        for(int index = 0; index < listOfBuyers.size(); index++){
            var returnBidLog = createBidServiceLogs(ContractFunctionEnum.ReturnBid, contentID, userIDs.get(index));
            var returnBidTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "returnBid", GAS_LIMIT,
                    Arrays.asList(new Utf8String(contentID), new Utf8String(userIDs.get(index))), Collections.EMPTY_LIST);

            returnBidLog.setContractTransactionReceipt(returnBidTransactionReceipt);
            sivantisContractLogsRepository.save(returnBidLog);
            if(returnBidTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
                paymentService.failedRefundPurchasedContent(userIDs.get(index), contentID);
            }else {
                paymentService.refundPurchasedContent(userIDs.get(index), contentID, returnBidTransactionReceipt.getTransactionHash());
            }
        }

        var setAuctionToInactiveLog = createBidServiceLogs(ContractFunctionEnum.SetAuctionToInactive, contentID, null);
        var setAuctionToInactiveTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "setAuctionToInactive", GAS_LIMIT,
                List.of(new Utf8String(contentID)), Collections.EMPTY_LIST);
        setAuctionToInactiveLog.setContractTransactionReceipt(setAuctionToInactiveTransactionReceipt);
        sivantisContractLogsRepository.save(setAuctionToInactiveLog);
    }

    public void auctionReleaseDate(Content content)  {
        var setAuctionToInactiveLog = createBidServiceLogs(ContractFunctionEnum.SetAuctionToInactive, content.getContentId(), null);
        var setAuctionToInactiveTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "setAuctionToInactive", GAS_LIMIT,
                List.of(new Utf8String(content.getContentId())), Collections.EMPTY_LIST);
        setAuctionToInactiveLog.setContractTransactionReceipt(setAuctionToInactiveTransactionReceipt);
        sivantisContractLogsRepository.save(setAuctionToInactiveLog);

        org.web3j.crypto.Credentials credentials = org.web3j.crypto.Credentials.create(privateBidKey);
        var userIdsThatCancelled = getUsersThatCancelledBid(content, credentials);
        for (String userID: userIdsThatCancelled){
            content.getListOfBuyerIds().remove(userID);
        }
        if(!userIdsThatCancelled.isEmpty()){
            contentRepository.save(content);
        }

        if(content.getListOfBuyerIds().size() > content.getNumbBidders()) {
            var buyers = convertToDoubleMapAndSort(content.getListOfBuyerIds()).entrySet().stream().toList();
            for (int index = content.getNumbBidders(); index < content.getListOfBuyerIds().size(); index++) {
                var returnBidLog = createBidServiceLogs(ContractFunctionEnum.ReturnBid, content.getContentId(), buyers.get(index).getKey());
                var returnBidTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "returnBid", GAS_LIMIT,
                        Arrays.asList(new Utf8String(content.getContentId()), new Utf8String(buyers.get(index).getKey())), Collections.EMPTY_LIST);
                returnBidLog.setContractTransactionReceipt(returnBidTransactionReceipt);
                returnBidLog.setTotalManaAmount(buyers.get(index).getValue());
                sivantisContractLogsRepository.save(returnBidLog);
                if (returnBidTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)) {
                    paymentService.failedRefundPurchasedContent(buyers.get(index).getKey(), content.getContentId());
                } else {
                    paymentService.refundPurchasedContent(buyers.get(index).getKey(), content.getContentId(), returnBidTransactionReceipt.getTransactionHash());
                }
            }
        }
    }

    @NotNull
    private ArrayList<String> getUsersThatCancelledBid(Content content, org.web3j.crypto.Credentials credentials) {
        DefaultGasProvider contractGasProvider = new DefaultGasProvider();
        var bidService = new BidService(BidServiceAddress, web3j, credentials, contractGasProvider);
        var userIdsThatCancelled = new ArrayList<String>();
        for(String userID: content.getListOfBuyerIds().keySet()){
            var result = bidService.findBidByContentIDAndUserID(content.getContentId(), userID);
            BigInteger amount;
            try {
                amount = result.send().component3();
                if(Objects.equals(amount, BigInteger.ZERO)){
                    userIdsThatCancelled.add(userID);
                }
            } catch (Exception ignored) {

            }
        }
        return userIdsThatCancelled;
    }

    public void reactivateAuction(Content content)  {
        if(content.getListOfBuyerIds().isEmpty()){
            return;
        }
        var buyers = convertToDoubleMapAndSort(content.getListOfBuyerIds());
        var userIDs = buyers.keySet().stream().toList();
        List<String> previousWinners = userIDs.subList(0, Math.min(content.getNumbBidders(), userIDs.size()));
        List<Utf8String> utf8StringList = previousWinners.stream()
                .map(Utf8String::new)
                .toList();

        var parameters = Arrays.asList(
                new Utf8String(content.getContentId()),
                new DynamicArray<>(Utf8String.class, utf8StringList)
        );
        var setAuctionToActiveLog = createBidServiceLogs(ContractFunctionEnum.SetAuctionToActive, content.getContentId(), null);
        var setAuctionToActiveTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "setAuctionToActive", GAS_LIMIT,
                parameters, Collections.EMPTY_LIST);
        setAuctionToActiveLog.setContractTransactionReceipt(setAuctionToActiveTransactionReceipt);
        sivantisContractLogsRepository.save(setAuctionToActiveLog);
        if(setAuctionToActiveLog.getContractTransactionReceipt().getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Setting Auction back to Active failed. Try again Later");
        }
    }

    public Boolean successfulAuction(String contentID, Map<String, String> listOfBuyers, int maxWinners)  {
        var buyers = convertToDoubleMapAndSort(listOfBuyers).entrySet().stream().toList();
        double totalEarnings = 0;
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
        var sendManaLog = createBidServiceLogs(ContractFunctionEnum.SendMana, contentID, null);
        var sendManaTransactionReceipt = interfaceModule.callInterfaceModule(web3j,  "sendMana", GAS_LIMIT,
                List.of(new Utf8String(contentID)), Collections.EMPTY_LIST);
        sendManaLog.setContractTransactionReceipt(sendManaTransactionReceipt);
        boolean successfulTransaction = false;
        if(sendManaTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Completed)){
            sendManaLog.setTotalManaAmount(totalEarnings);
            sendManaLog.setManaToCompany(totalEarnings / 15);
            successfulTransaction = true;
        }
        sivantisContractLogsRepository.save(sendManaLog);
        return successfulTransaction;
    }

    //ChannelService
    public void createChannel(String channelName, Double averageWeeklyViewers)  {
        var parameters = Arrays.asList(
                new Utf8String(channelName),
                new Uint256((int) Math.round(averageWeeklyViewers))
        );
        var createChannelLog = createChannelServiceLogs(ContractFunctionEnum.CreateChannel, channelName, null);
        var createChannelTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "addChannel", GAS_LIMIT,
                parameters, Collections.EMPTY_LIST);
        createChannelLog.setContractTransactionReceipt(createChannelTransactionReceipt);
        sivantisContractLogsRepository.save(createChannelLog);
        if(createChannelTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Creating Channel On Smart Contract Failed.");
        }
    }

    public void payForContent(Channels channel,
                              String contentCreatorID,
                              String contentID,
                              String contentType,
                              Double priceOfMana,
                              Double averageWeeklyViewers)  {
        var contentPricePerHundred = priceOfContent(contentType);
        var parameters = Arrays.asList(
                new Utf8String(channel.getChannelName()),
                new Utf8String(contentCreatorID),
                new Utf8String(contentID),
                new Uint256(contentPricePerHundred)
        );
        var payForContentLog = createChannelServiceLogs(ContractFunctionEnum.PayForContent, channel.getChannelName(), contentID);
        var payForContentTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "payForContent", GAS_LIMIT,
                parameters, Collections.EMPTY_LIST);
        if(payForContentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Transaction for Pay Later failed. Try again later");
        }
        var totalManaAmount = contentPricePerHundred * averageWeeklyViewers / (priceOfMana * 100);
        payForContentLog.setContractTransactionReceipt(payForContentTransactionReceipt);
        payForContentLog.setTotalManaAmount(totalManaAmount);
        payForContentLog.setManaToCompany(totalManaAmount / 10);
        sivantisContractLogsRepository.save(payForContentLog);
        paymentService.purchaseChannelContent(
                channel,
                contentID,
                contentPricePerHundred,
                payForContentTransactionReceipt.getTransactionHash(),
                averageWeeklyViewers);
    }

    public void watchNowPayLater(Channels channel,
                                 String contentCreatorID,
                                 String contentID,
                                 String contentType,
                                 int paymentIncrements,
                                 Double priceOfMana,
                                 Double averageWeeklyViewers)  {
        var contentPricePerHundred = priceOfContent(contentType);
        var parameters = Arrays.asList(
                new Utf8String(channel.getChannelName()),
                new Utf8String(contentCreatorID),
                new Utf8String(contentID),
                new Uint256(contentPricePerHundred),
                new Uint8(paymentIncrements)
        );
        var watchNowPayLaterLog = createChannelServiceLogs(ContractFunctionEnum.WatchNowPayLater, channel.getChannelName(), contentID);
        var watchNowPayLaterTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "watchNowPayLater", GAS_LIMIT,
                parameters, Collections.EMPTY_LIST);
        var totalManaAmount = contentPricePerHundred * averageWeeklyViewers / (priceOfMana * 100);
        watchNowPayLaterLog.setContractTransactionReceipt(watchNowPayLaterTransactionReceipt);
        watchNowPayLaterLog.setTotalManaAmount(totalManaAmount);
        watchNowPayLaterLog.setManaToCompany(totalManaAmount / 10);
        sivantisContractLogsRepository.save(watchNowPayLaterLog);
        if(watchNowPayLaterTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Transaction for Watch Now Pay Later failed. Try again later");
        }
        var watchNowPlayLater = WatchNowPayLater.builder()
                .watchNowPlayLaterId(ObjectId.get().toHexString())
                .channelName(channel.getChannelName())
                .contentID(contentID)
                .watchNowPayLaterEnum(WatchNowPayLaterEnum.Unpaid)
                .paymentAmountInUSD(totalManaAmount * priceOfMana / paymentIncrements)
                .paymentsLeft(paymentIncrements - 1)
                .nextPaymentDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .build() ;
        watchNowPayLaterRepository.save(watchNowPlayLater);
        channel.getWatchNowPayLaterIDs().add(watchNowPlayLater.getWatchNowPlayLaterId());
        channelRepository.save(channel);
        paymentService.purchaseChannelContent(
                channel,
                contentID,
                contentPricePerHundred,
                watchNowPayLaterTransactionReceipt.getTransactionHash(),
                averageWeeklyViewers);
    }


    public void watchNowPayLaterPayment(String watchNowPayLaterID, Double priceOfMana)  {
        if(watchNowPayLaterRepository.findById(watchNowPayLaterID).isPresent()){
            var watchNowPayLater = watchNowPayLaterRepository.findById(watchNowPayLaterID).get();
            var parameters = Arrays.asList(
                    new Utf8String(watchNowPayLater.getChannelName()),
                    new Utf8String(watchNowPayLater.getContentID())
            );
            var watchNowPayLaterPaymentLog = createChannelServiceLogs(ContractFunctionEnum.WatchNowPayLaterPayment, watchNowPayLater.getChannelName(), watchNowPayLater.getContentID());
            var watchNowPayLaterPaymentTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "watchNowPayLaterPayment", GAS_LIMIT,
                    parameters, Collections.EMPTY_LIST);
            watchNowPayLaterPaymentLog.setContractTransactionReceipt(watchNowPayLaterPaymentTransactionReceipt);
            watchNowPayLaterPaymentLog.setTotalManaAmount(watchNowPayLater.getPaymentAmountInUSD() / priceOfMana);
            sivantisContractLogsRepository.save(watchNowPayLaterPaymentLog);
            if(watchNowPayLaterPaymentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Completed)){
                var paymentsLeft = watchNowPayLater.getPaymentsLeft() - 1;
                watchNowPayLater.setPaymentsLeft(paymentsLeft);
                if(paymentsLeft == 0){
                    watchNowPayLater.setWatchNowPayLaterEnum(WatchNowPayLaterEnum.Paid);
                }
                watchNowPayLater.setNextPaymentDate(Instant.now().plus(7, ChronoUnit.DAYS));
                watchNowPayLaterRepository.save(watchNowPayLater);
            }
        }
    }

    public void updateAverageWeeklyViewers(String channelName, Double newAverageWeeklyViewers)  {
        var parameters = Arrays.asList(
                new Utf8String(channelName),
                new Uint256((int) Math.round(newAverageWeeklyViewers))
        );
        var updateAverageWeeklyViewersLog = createChannelServiceLogs(ContractFunctionEnum.UpdateAverageWeeklyViewers, channelName, null);
        var updateAverageWeeklyViewersTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "updateAverageWeeklyViewers", GAS_LIMIT,
                parameters, Collections.EMPTY_LIST);
        updateAverageWeeklyViewersLog.setContractTransactionReceipt(updateAverageWeeklyViewersTransactionReceipt);
        sivantisContractLogsRepository.save(updateAverageWeeklyViewersLog);
        if(updateAverageWeeklyViewersTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Failed To update Average Weekly Viewers");
        }
    }

    public boolean hasSufficientChannelBalance(String channelName, Double manaPrice, Double averageWeeklyViewers, String contentType, Integer numberOfPayments) {
        Function function = new Function(
                "getChannelBalance",
                List.of(new Utf8String(channelName)),
                List.of(new TypeReference<Int>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        org.web3j.crypto.Credentials credentials = org.web3j.crypto.Credentials.create(privateChannelKey);

        EthCall response;
        try {
            response = web3j.ethCall(
                            Transaction.createEthCallTransaction(
                                    credentials.getAddress(),
                                    ChannelServiceAddress,
                                    encodedFunction),
                            DefaultBlockParameterName.LATEST)
                    .send();
        } catch (IOException e) {
            return true;
        }
        List<Type> result = FunctionReturnDecoder.decode(response.getValue(), function.getOutputParameters());

        var manaNeeded = (averageWeeklyViewers * priceOfContent(contentType)) / (100 * manaPrice);

        BigInteger weiBalance = ((org.web3j.abi.datatypes.Int) result.get(0)).getValue();

        var channelManaBalance = Convert.fromWei(weiBalance.toString(), Convert.Unit.ETHER).doubleValue();
        // Assuming the function returns a single int value
        return channelManaBalance >= (manaNeeded / numberOfPayments);
    }

    //WarChestService
    public ContractStatusEnum addContentCreator(String creatorID, String creatorPersonalWallet, int rank)  {
        var parameters = Arrays.asList(
                new Utf8String(creatorID),
                new Address(creatorPersonalWallet),
                new Uint8(rank)
        );
        var addContentCreatorLog = createWarChestServiceLogs(ContractFunctionEnum.AddContentCreator,null, creatorID);
        var addContentCreatorTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "addContentCreator", GAS_LIMIT,
                parameters, Collections.EMPTY_LIST);
        addContentCreatorLog.setContractTransactionReceipt(addContentCreatorTransactionReceipt);
        sivantisContractLogsRepository.save(addContentCreatorLog);
        return addContentCreatorTransactionReceipt.getContractStatusEnum();
    }

    public void CancelPayment(String creatorID, String contentID, String channelName, String manaAmount)  {
        var parameters = Arrays.asList(
                new Utf8String(creatorID),
                new Utf8String(contentID),
                new Utf8String(channelName)
        );
        var sendRefundPaymentLog = createWarChestServiceLogs(ContractFunctionEnum.CancelPayment,contentID, creatorID, channelName);
        var sendRefundPaymentTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "sendRefundPayment", GAS_LIMIT,
                parameters, Collections.EMPTY_LIST);
        sendRefundPaymentLog.setContractTransactionReceipt(sendRefundPaymentTransactionReceipt);
        sendRefundPaymentLog.setTotalManaAmount(Double.parseDouble(manaAmount));
        sivantisContractLogsRepository.save(sendRefundPaymentLog);
        if(sendRefundPaymentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Failed To Cancel Purchase. Try again later.");
        }
        paymentService.refundChannelPurchasedContent(channelName, contentID, sendRefundPaymentTransactionReceipt.getTransactionHash());
    }

    public void sendRefundPayment(String creatorID, String contentID, String channelName, String manaAmount)  {
        var parameters = Arrays.asList(
                new Utf8String(creatorID),
                new Utf8String(contentID),
                new Utf8String(channelName)
        );
        var sendRefundPaymentLog = createWarChestServiceLogs(ContractFunctionEnum.SendRefundPayment,contentID, creatorID, channelName);
        var sendRefundPaymentTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "sendRefundPayment", GAS_LIMIT,
                parameters, Collections.EMPTY_LIST);
        sendRefundPaymentLog.setContractTransactionReceipt(sendRefundPaymentTransactionReceipt);
        sendRefundPaymentLog.setTotalManaAmount(Double.parseDouble(manaAmount));
        sivantisContractLogsRepository.save(sendRefundPaymentLog);
        if(sendRefundPaymentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            paymentService.failedRefundChannelPurchasedContent(channelName, contentID);
        } else {
            paymentService.refundChannelPurchasedContent(channelName, contentID, sendRefundPaymentTransactionReceipt.getTransactionHash());
        }
    }

    public boolean CancelPaymentReport(String creatorID, String contentID, String channelName, String manaAmount)  {
        var parameters = Arrays.asList(
                new Utf8String(creatorID),
                new Utf8String(contentID),
                new Utf8String(channelName)
        );
        var sendRefundPaymentLog = createWarChestServiceLogs(ContractFunctionEnum.SendRefundPayment,contentID, creatorID, channelName);
        ContractTransactionReceipt sendRefundPaymentTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "sendRefundPayment", GAS_LIMIT,
                    parameters, Collections.EMPTY_LIST);
            if(sendRefundPaymentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
                return false;
            } else {
                sendRefundPaymentLog.setContractTransactionReceipt(sendRefundPaymentTransactionReceipt);
                sendRefundPaymentLog.setTotalManaAmount(Double.parseDouble(manaAmount));
                sivantisContractLogsRepository.save(sendRefundPaymentLog);
                paymentService.refundChannelPurchasedContent(channelName, contentID, sendRefundPaymentTransactionReceipt.getTransactionHash());
                return true;
            }
    }

    public void CancelWatchNowPayLaterPayment(WatchNowPayLater watchNowPayLater, String creatorID, String contentID, String channelName, String manaAmount)  {
        var parameters = Arrays.asList(
                new Utf8String(creatorID),
                new Utf8String(contentID),
                new Utf8String(channelName)
        );
        var sendWatchNowPayLaterRefundPaymentLog = createWarChestServiceLogs(ContractFunctionEnum.CancelWatchNowPayLater, contentID, creatorID);
        var sendWatchNowPayLaterRefundPaymentTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "sendWatchNowPayLaterRefundPayment", GAS_LIMIT,
                parameters, Collections.EMPTY_LIST);
        sendWatchNowPayLaterRefundPaymentLog.setContractTransactionReceipt(sendWatchNowPayLaterRefundPaymentTransactionReceipt);
        sendWatchNowPayLaterRefundPaymentLog.setTotalManaAmount(Double.parseDouble(manaAmount));
        sivantisContractLogsRepository.save(sendWatchNowPayLaterRefundPaymentLog);
        if(sendWatchNowPayLaterRefundPaymentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Failed To Cancel Purchase. Try again later.");
        }
        watchNowPayLater.setWatchNowPayLaterEnum(WatchNowPayLaterEnum.Paid);
        watchNowPayLater.setPaymentsLeft(0);
        watchNowPayLaterRepository.save(watchNowPayLater);
        paymentService.refundChannelPurchasedContent(channelName, contentID, sendWatchNowPayLaterRefundPaymentTransactionReceipt.getTransactionHash());
    }

    public boolean CancelWatchNowPayLaterPaymentReport(WatchNowPayLater watchNowPayLater, String creatorID, String contentID, String channelName, String manaAmount)  {
        var parameters = Arrays.asList(
                new Utf8String(creatorID),
                new Utf8String(contentID),
                new Utf8String(channelName)
        );
        var sendWatchNowPayLaterRefundPaymentLog = createWarChestServiceLogs(ContractFunctionEnum.CancelWatchNowPayLater, contentID, creatorID);
        ContractTransactionReceipt sendWatchNowPayLaterRefundPaymentTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "sendWatchNowPayLaterRefundPayment", GAS_LIMIT,
                    parameters, Collections.EMPTY_LIST);
        sendWatchNowPayLaterRefundPaymentLog.setContractTransactionReceipt(sendWatchNowPayLaterRefundPaymentTransactionReceipt);
        sendWatchNowPayLaterRefundPaymentLog.setTotalManaAmount(Double.parseDouble(manaAmount));
        if(sendWatchNowPayLaterRefundPaymentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            return false;
        }
        watchNowPayLaterRepository.delete(watchNowPayLater);
        sivantisContractLogsRepository.save(sendWatchNowPayLaterRefundPaymentLog);
        paymentService.refundChannelPurchasedContent(channelName, contentID, sendWatchNowPayLaterRefundPaymentTransactionReceipt.getTransactionHash());
        return true;
    }

    public void sendWatchNowPayLaterRefundPayment(WatchNowPayLater watchNowPayLater,String creatorID, String contentID, String channelName, String manaAmount)  {
        var parameters = Arrays.asList(
                new Utf8String(creatorID),
                new Utf8String(contentID),
                new Utf8String(channelName)
        );
        var sendWatchNowPayLaterRefundPaymentLog = createWarChestServiceLogs(ContractFunctionEnum.SendWatchNowPayLaterRefundPayment, contentID, creatorID);
        var sendWatchNowPayLaterRefundPaymentTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "sendWatchNowPayLaterRefundPayment", GAS_LIMIT,
                parameters, Collections.EMPTY_LIST);
        sendWatchNowPayLaterRefundPaymentLog.setContractTransactionReceipt(sendWatchNowPayLaterRefundPaymentTransactionReceipt);
        sendWatchNowPayLaterRefundPaymentLog.setTotalManaAmount(Double.parseDouble(manaAmount));
        sivantisContractLogsRepository.save(sendWatchNowPayLaterRefundPaymentLog);
        if(sendWatchNowPayLaterRefundPaymentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            paymentService.failedRefundChannelPurchasedContent(channelName, contentID);
        } else {
            watchNowPayLater.setWatchNowPayLaterEnum(WatchNowPayLaterEnum.Paid);
            watchNowPayLater.setPaymentsLeft(0);
            watchNowPayLaterRepository.save(watchNowPayLater);
            paymentService.refundChannelPurchasedContent(channelName, contentID, sendWatchNowPayLaterRefundPaymentTransactionReceipt.getTransactionHash());
        }
    }

    public void reactivateContent(Content content) {
        if(content.getListOfBuyerIds().isEmpty()){
            return;
        }
        var buyers = convertToDoubleMapAndSort(content.getListOfBuyerIds());
        var channelNames = buyers.keySet().stream().toList();
        List<String> previousWinners = channelNames.subList(0, Math.min(content.getNumbBidders(), channelNames.size()));
        List<Utf8String> utf8StringList = previousWinners.stream()
                .map(Utf8String::new)
                .toList();

        var parameters = Arrays.asList(
                new Utf8String(content.getContentId()),
                new DynamicArray<>(Utf8String.class, utf8StringList)
        );
        var setAuctionToActiveLog = createBidServiceLogs(ContractFunctionEnum.ReactivateContent, content.getContentId(), content.getCreatorID());
        var setAuctionToActiveTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "reactivateContent", GAS_LIMIT,
                parameters, Collections.EMPTY_LIST);
        setAuctionToActiveLog.setContractTransactionReceipt(setAuctionToActiveTransactionReceipt);
        sivantisContractLogsRepository.save(setAuctionToActiveLog);
        if(setAuctionToActiveLog.getContractTransactionReceipt().getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Error setting Auction back to active. Try again Later");
        }
    }

    public void updatePersonalWallet(String creatorID, String _newPersonalWallet) {
        var parameters = Arrays.asList(
                new Utf8String(creatorID),
                new Address(_newPersonalWallet)
        );
        var updatePersonalWalletLog = createWarChestServiceLogs(ContractFunctionEnum.UpdatePersonalWallet, null, creatorID);
        var updatePersonalWalletTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "updatePersonalWallet", GAS_LIMIT,
                parameters, Collections.EMPTY_LIST);
        updatePersonalWalletLog.setContractTransactionReceipt(updatePersonalWalletTransactionReceipt);
        sivantisContractLogsRepository.save(updatePersonalWalletLog);
        if(updatePersonalWalletTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Failed To change wallet address on smart contract. Try again later.");
        }
    }

    public void increaseCreatorRank(String creatorID) {
        var increaseCreatorRankLog = createWarChestServiceLogs(ContractFunctionEnum.IncreaseCreatorRank, null , creatorID);
        var increaseCreatorRankTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "increaseCreatorRank", GAS_LIMIT,
                List.of(new Utf8String(creatorID)), Collections.EMPTY_LIST);
        increaseCreatorRankLog.setContractTransactionReceipt(increaseCreatorRankTransactionReceipt);
        sivantisContractLogsRepository.save(increaseCreatorRankLog);
    }



    public void sendWeeklyMana(String creatorID) {
        DefaultGasProvider contractGasProvider = new DefaultGasProvider();
        org.web3j.crypto.Credentials credentials = org.web3j.crypto.Credentials.create(privateWarChestKey);
        var warChestService = new WarChestService(WarChestServiceAddress, web3j, credentials, contractGasProvider);
        var result = warChestService.findContentCreatorByID(creatorID);
        BigInteger balance;
        try {
            balance = result.send().component3();
        } catch (Exception ignored) {
            balance = BigInteger.valueOf(1);
        }
        if(balance.compareTo(BigInteger.valueOf(0)) > 0){
            var SendWeeklyManaLog = createWarChestServiceLogs(ContractFunctionEnum.SendWeeklyMana, null, creatorID);
            var SendWeeklyManaTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "sendWeeklyMana", GAS_LIMIT,
                    List.of(new Utf8String(creatorID)), Collections.EMPTY_LIST);
            SendWeeklyManaLog.setContractTransactionReceipt(SendWeeklyManaTransactionReceipt);
            sivantisContractLogsRepository.save(SendWeeklyManaLog);
        }
    }



    //Resolve Logs
    public void resolveLog(String logId)  {
        var log = sivantisContractLogsRepository.findById(logId).orElseThrow();
        switch (log.getContractFunctionEnum()){
            case SendMana -> resolveSendMana(log);
            case ReturnBid -> resolveReturnBid(log);
            case SendWeeklyMana -> resolveSendWeeklyMana(log);
            case SetAuctionToInactive -> resolveSetAuctionToInactive(log);
            case SendRefundPayment -> resolveSendRefundPayment(log);
            case WatchNowPayLaterPayment -> resolveWatchNowPayLaterPayment(log);
            case IncreaseCreatorRank -> resolveIncreaseCreatorRank(log);
            default -> throw new SivantisException("Invalid Contract Function Enum");
        }
    }

    private void resolveIncreaseCreatorRank(SivantisContractLogs unresolvedLog)  {
        var increaseCreatorRankTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "increaseCreatorRank", GAS_LIMIT,
                List.of(new Utf8String(unresolvedLog.getUserID())), Collections.EMPTY_LIST);
        if(increaseCreatorRankTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setContractTransactionReceipt(increaseCreatorRankTransactionReceipt);
        sivantisContractLogsRepository.save(unresolvedLog);
    }

    private void resolveWatchNowPayLaterPayment(SivantisContractLogs unresolvedLog) {
        var watchNowPayLater = watchNowPayLaterRepository.findByChannelNameAndContentID(unresolvedLog.getChannelName(), unresolvedLog.getContentID()).orElseThrow();
        var parameters = Arrays.asList(
                new Utf8String(unresolvedLog.getChannelName()),
                new Utf8String(unresolvedLog.getContentID())
        );
        var watchNowPayLaterPaymentTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "watchNowPayLaterPayment", GAS_LIMIT,
                parameters, Collections.EMPTY_LIST);
        if(watchNowPayLaterPaymentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setContractTransactionReceipt(watchNowPayLaterPaymentTransactionReceipt);
        var paymentsLeft = watchNowPayLater.getPaymentsLeft() - 1;
        watchNowPayLater.setPaymentsLeft(paymentsLeft);
        if(paymentsLeft == 0){
            watchNowPayLater.setWatchNowPayLaterEnum(WatchNowPayLaterEnum.Paid);
        }
        watchNowPayLater.setNextPaymentDate(Instant.now().plus(7, ChronoUnit.DAYS));
        sivantisContractLogsRepository.save(unresolvedLog);
        watchNowPayLaterRepository.save(watchNowPayLater);
    }

    private void resolveSendRefundPayment(SivantisContractLogs unresolvedLog) {
        var parameters = Arrays.asList(
                new Utf8String(unresolvedLog.getUserID()),
                new Utf8String(unresolvedLog.getContentID()),
                new Utf8String(unresolvedLog.getChannelName())
        );
        var sendRefundPaymentTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "sendRefundPayment", GAS_LIMIT,
                parameters, Collections.EMPTY_LIST);
        if(sendRefundPaymentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setContractTransactionReceipt(sendRefundPaymentTransactionReceipt);
        sivantisContractLogsRepository.save(unresolvedLog);
        paymentService.refundChannelPurchasedContent(unresolvedLog.getChannelName(), unresolvedLog.getContentID(), sendRefundPaymentTransactionReceipt.getTransactionHash());
    }

    private void resolveSetAuctionToInactive(SivantisContractLogs unresolvedLog)  {
        var setAuctionToInactiveTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "setAuctionToInactive", GAS_LIMIT,
                List.of(new Utf8String(unresolvedLog.getContentID())), Collections.EMPTY_LIST);
        if(setAuctionToInactiveTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setContractTransactionReceipt(setAuctionToInactiveTransactionReceipt);
        sivantisContractLogsRepository.save(unresolvedLog);
    }

    private void resolveSendWeeklyMana(SivantisContractLogs unresolvedLog)  {
        var SendWeeklyManaTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "sendWeeklyMana", GAS_LIMIT,
                List.of(new Utf8String(unresolvedLog.getUserID())), Collections.EMPTY_LIST);
        if(SendWeeklyManaTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setContractTransactionReceipt(SendWeeklyManaTransactionReceipt);
        sivantisContractLogsRepository.save(unresolvedLog);
    }

    private void resolveReturnBid(SivantisContractLogs unresolvedLog)  {
        var returnBidTransactionReceipt = interfaceModule.callInterfaceModule(web3j, "returnBid", GAS_LIMIT,
                Arrays.asList(new Utf8String(unresolvedLog.getContentID()), new Utf8String(unresolvedLog.getUserID())), Collections.EMPTY_LIST);

        if(returnBidTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setContractTransactionReceipt(returnBidTransactionReceipt);
        sivantisContractLogsRepository.save(unresolvedLog);
        paymentService.refundPurchasedContent(unresolvedLog.getUserID(), unresolvedLog.getContentID(), returnBidTransactionReceipt.getTransactionHash());
    }

    private void resolveSendMana(SivantisContractLogs unresolvedLog)  {
        var content = contentRepository.findById(unresolvedLog.getContentID()).orElseThrow();
        var buyers = convertToDoubleMapAndSort(content.getListOfBuyerIds()).entrySet().stream().toList();
        double totalEarnings = 0;
        if(buyers.size() < content.getNumbBidders()){
            for (Map.Entry<String, Double> buyer : buyers) {
                totalEarnings += buyer.getValue();
            }
        }
        else {
            for(int index = 0; index < content.getNumbBidders(); index++){
                totalEarnings += buyers.get(index).getValue();
            }
        }
        var sendManaTransactionReceipt = interfaceModule.callInterfaceModule(web3j,  "sendMana", GAS_LIMIT,
                List.of(new Utf8String(content.getContentId())), Collections.EMPTY_LIST);
        unresolvedLog.setContractTransactionReceipt(sendManaTransactionReceipt);
        if(sendManaTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setTotalManaAmount(totalEarnings);
        unresolvedLog.setManaToCompany(totalEarnings / 15);
        sivantisContractLogsRepository.save(unresolvedLog);
        paymentService.resolvedAuctionPayment(unresolvedLog.getContentID(), unresolvedLog.getUserID());
    }


    public int priceOfContent(String contentType){
        return switch (contentType){
            case "Short Film" -> 5;
            case "Sports", "Concerts" -> 10;
            case "Movies" -> 20;
            default -> throw new IllegalStateException("Unexpected value: " + contentType);
        };

    }

    private SivantisContractLogs createBidServiceLogs(ContractFunctionEnum contractFunctionEnum, String contentID, String userID){
        return SivantisContractLogs.builder()
                .logId(ObjectId.get().toHexString())
                .contentID(contentID)
                .userID(userID)
                .creationDate(Instant.now())
                .contractEnum(ContractEnum.BidService)
                .contractFunctionEnum(contractFunctionEnum)
                .build();
    }

    private SivantisContractLogs createChannelServiceLogs(ContractFunctionEnum contractFunctionEnum, String channelName, String contentID){
        return SivantisContractLogs.builder()
                .logId(ObjectId.get().toHexString())
                .channelName(channelName)
                .contentID(contentID)
                .creationDate(Instant.now())
                .contractEnum(ContractEnum.ChannelService)
                .contractFunctionEnum(contractFunctionEnum)
                .build();
    }

    private SivantisContractLogs createWarChestServiceLogs(ContractFunctionEnum contractFunctionEnum, String contentID, String userID){
        return SivantisContractLogs.builder()
                .logId(ObjectId.get().toHexString())
                .contentID(contentID)
                .userID(userID)
                .creationDate(Instant.now())
                .contractEnum(ContractEnum.WarChestService)
                .contractFunctionEnum(contractFunctionEnum)
                .build();
    }

    private SivantisContractLogs createWarChestServiceLogs(ContractFunctionEnum contractFunctionEnum, String contentID, String userID, String channelName){
        return SivantisContractLogs.builder()
                .logId(ObjectId.get().toHexString())
                .contentID(contentID)
                .channelName(channelName)
                .userID(userID)
                .creationDate(Instant.now())
                .contractEnum(ContractEnum.WarChestService)
                .contractFunctionEnum(contractFunctionEnum)
                .build();
    }

    private ContractTransactionReceipt failedContractTransactionReceipt() {
        return ContractTransactionReceipt.builder()
                .contractStatusEnum(ContractStatusEnum.Error)
                .build();
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

