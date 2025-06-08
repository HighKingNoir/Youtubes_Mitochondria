package Project_Noir.Athena.Service;

import Project_Noir.Athena.Exception.SivantisException;
import Project_Noir.Athena.Model.*;
import Project_Noir.Athena.Repo.*;
import Project_Noir.Athena.SmartContracts.InterfaceService.InterfaceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.StaticGasProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResolveSmartContractFunctionCallService {

    private final SivantisContractLogsRepository sivantisContractLogsRepository;
    private final MultiSendHelperService multiSendHelperService;
    private final WatchNowPayLaterRepository watchNowPayLaterRepository;
    private final GasLimitService gasLimitService;
    private final MessageService messageService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final ChannelRepository channelRepository;
    private final PaymentService paymentService;
    private final CredentialsService credentialsService;
    private final BigInteger ultraHighGasLimit = BigInteger.valueOf(1500000L);
    private final BigInteger highGasLimit = BigInteger.valueOf(750000L);
    private final BigInteger midGasLimit = BigInteger.valueOf(500000L);
    private final BigInteger lowGasLimit = BigInteger.valueOf(250000L);



    @Value("${contract.bid.address}")
    private String BidServiceAddress;

    @Value("${contract.channel.address}")
    private String ChannelServiceAddress;

    @Value("${contract.warchest.address}")
    private String WarChestServiceAddress;

    @Value("${contract.interface.address}")
    private String interfaceModuleAddress;

    @Value("${infura.api.secret}")
    private String infuraAPISecret;

    @Value("${infura.api.key}")
    private String infuraAPIKey;
    private Web3j web3j;

    @Value("${spring.profiles.active}")
    private String environment;

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

    public void resolveMultiCall(String logId){
        var log = sivantisContractLogsRepository.findById(logId).orElseThrow();
        if(!log.getContractEnum().equals(ContractEnum.MultiCall) && !log.getContractEnum().equals(ContractEnum.ClientMultiCall)){
            throw new SivantisException("Invalid Contract Enum");
        }
        if(log.getContractEnum().equals(ContractEnum.ClientMultiCall)){
            resolveClientMultiCall(log);
            return;
        }
        switch (log.getContractFunctionDetails().get(0).getContractFunctionEnum()){
            case SendMana -> resolveSendManaMultiCall(log);
            case ReturnBid -> resolveReturnBidMultiCall(log);
            case SendWeeklyMana -> resolveSendWeeklyManaMultiCall(log);
            case SetAuctionToInactive -> resolveSetAuctionToInactiveMultiCall(log);
            case SendRefundPayment -> resolveSendRefundPaymentMultiCall(log);
            case SendWatchNowPayLaterRefundPayment -> resolveSendWatchNowPayLaterRefundPaymentMultiCall(log);
            case WatchNowPayLaterPayment -> resolveWatchNowPayLaterPaymentMultiCall(log);
            case IncreaseCreatorRank -> resolveIncreaseCreatorRankMultiCall(log);
            default -> throw new SivantisException("Invalid Contract Function Enum");
        }
    }

    public void splitMultiCall(String logId){
        var multiCallLog = sivantisContractLogsRepository.findById(logId).orElseThrow();
        if(!multiCallLog.getContractEnum().equals(ContractEnum.MultiCall) && !multiCallLog.getContractEnum().equals(ContractEnum.ClientMultiCall)){
            throw new SivantisException("Invalid Contract Enum");
        }
        for (ContractFunctionDetails contractFunctionDetails: multiCallLog.getContractFunctionDetails()){
            switch (contractFunctionDetails.getContractFunctionEnum()){
                case AddContentCreator, CancelPayment, SendRefundPayment, UpdatePersonalWallet, IncreaseCreatorRank, SendWeeklyMana, CancelWatchNowPayLater, ReactivateContent, SendWatchNowPayLaterRefundPayment -> {
                    var warChestLog = createWarChestServiceLogs(contractFunctionDetails);
                    try {
                        resolveFunction(warChestLog);
                    }catch (SivantisException ignored){
                        var contractTransactionReceipt = ContractTransactionReceipt.builder()
                                .contractStatusEnum(ContractStatusEnum.Error)
                                .build();
                        warChestLog.setContractTransactionReceipt(contractTransactionReceipt);
                        sivantisContractLogsRepository.save(warChestLog);
                    }
                }
                case CreateNewAuction, ReturnBid, SetAuctionToInactive, SendMana, SetAuctionToActive -> {
                    var bidLog = createBidServiceLogs(contractFunctionDetails);
                    try {
                        resolveFunction(bidLog);
                    }catch (SivantisException ignored){
                        var contractTransactionReceipt = ContractTransactionReceipt.builder()
                                .contractStatusEnum(ContractStatusEnum.Error)
                                .build();
                        bidLog.setContractTransactionReceipt(contractTransactionReceipt);
                        sivantisContractLogsRepository.save(bidLog);
                    }
                }
                case AddChannel, PayForContent, UpdateAverageWeeklyViewers, WatchNowPayLater, WatchNowPayLaterPayment -> {
                    var channelLog = createChannelServiceLogs(contractFunctionDetails);
                    try {
                        resolveFunction(channelLog);
                    }catch (SivantisException ignored){
                        var contractTransactionReceipt = ContractTransactionReceipt.builder()
                                .contractStatusEnum(ContractStatusEnum.Error)
                                .build();
                        channelLog.setContractTransactionReceipt(contractTransactionReceipt);
                        sivantisContractLogsRepository.save(channelLog);
                    }
                }
            }
        }
        sivantisContractLogsRepository.delete(multiCallLog);
    }

    //Resolve Logs
    public void resolveSingleFunctionContactLog(String logId)  {
        var log = sivantisContractLogsRepository.findById(logId).orElseThrow();
        if(log.getContractEnum().equals(ContractEnum.MultiCall) || log.getContractEnum().equals(ContractEnum.ClientMultiCall)){
            throw new SivantisException("Invalid Contract Enum");
        }
        resolveFunction(log);
    }

    private void resolveFunction(SivantisContractLogs log){
        switch (log.getContractFunctionDetails().get(0).getContractFunctionEnum()){
            case SendMana -> resolveSendMana(log);
            case ReturnBid -> resolveReturnBid(log);
            case SendWeeklyMana -> resolveSendWeeklyMana(log);
            case SetAuctionToInactive -> resolveSetAuctionToInactive(log);
            case SendRefundPayment -> resolveSendRefundPayment(log);
            case SendWatchNowPayLaterRefundPayment -> resolveSendWatchNowPayLaterRefundPayment(log);
            case WatchNowPayLaterPayment -> resolveWatchNowPayLaterPayment(log);
            case IncreaseCreatorRank -> resolveIncreaseCreatorRank(log);
            case CreateNewAuction -> resolveCreateNewAuction(log);
            case SetAuctionToActive -> resolveSetAuctionToActive(log);
            case AddChannel -> resolveAddChannel(log);
            case PayForContent -> resolvePayForContent(log);
            case CancelPayment -> resolveCancelPayment(log);
            case UpdateAverageWeeklyViewers -> resolveUpdateAverageWeeklyViewers(log);
            case AddContentCreator -> resolveAddContentCreator(log);
            case UpdatePersonalWallet -> resolveUpdatePersonalWallet(log);
            case WatchNowPayLater -> resolveWatchNowPayLater(log);
            case CancelWatchNowPayLater -> resolveCancelWatchNowPayLater(log);
            case ReactivateContent -> resolveReactivateContent(log);
            default -> throw new SivantisException("Invalid Contract Function Enum");
        }
    }

    private void resolveCancelWatchNowPayLater(SivantisContractLogs log) {
        var functionDetails = log.getContractFunctionDetails().get(0);
        var creatorID = functionDetails.getUserID();
        var contentID = functionDetails.getContentID();
        var channelName = functionDetails.getChannelName();
        var functionCall = loadInterfaceService(ContractFunctionEnum.CancelWatchNowPayLater, 0).sendWatchNowPayLaterRefundPayment(creatorID, contentID, channelName);
        var sendWatchNowPayLaterRefundPaymentTransactionReceipt = executeFunctionCall(functionCall);
        if(sendWatchNowPayLaterRefundPaymentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        var Payment = paymentService.findChannelPayment(channelName, contentID);
        Payment.setTransactionHash(sendWatchNowPayLaterRefundPaymentTransactionReceipt.getTransactionHash());
        Payment.setStatus(PaymentEnum.RefundedPurchase);
        var totalManaAmount = functionDetails.getManaAmount();
        var content = contentRepository.findById(contentID).orElseThrow();
        var user = userRepository.findById(Payment.getUserId()).orElseThrow();
        messageService.refundChannelMessage(user, Payment.getTransactionHash(), channelName, functionDetails.getManaAmount().toString(), content);
        log.setTotalManaAmount(totalManaAmount);
        log.setContractTransactionReceipt(sendWatchNowPayLaterRefundPaymentTransactionReceipt);
        sivantisContractLogsRepository.save(log);
        paymentRepository.save(Payment);
    }

    private void resolveReactivateContent(SivantisContractLogs log) {
        var content = contentRepository.findById(log.getContractFunctionDetails().get(0).getContentID()).orElseThrow();
        var channelNames = content.getListOfBuyerIds().keySet().stream().toList();
        var functionCall = loadInterfaceService(ContractFunctionEnum.ReactivateContent, channelNames.size()).reactivateContent(content.getContentId(), channelNames);
        var reactivateContentTransactionReceipt = executeFunctionCall(functionCall);
        if(reactivateContentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        log.setContractTransactionReceipt(reactivateContentTransactionReceipt);
        sivantisContractLogsRepository.save(log);
    }

    private void resolveWatchNowPayLater(SivantisContractLogs log) {
        var functionDetails = log.getContractFunctionDetails().get(0);
        var creatorID = functionDetails.getUserID();
        var contentID = functionDetails.getContentID();
        var channelName = functionDetails.getChannelName();
        var contentType = contentRepository.findById(contentID).orElseThrow().getContentType();
        var paymentIncrements = watchNowPayLaterRepository.findByChannelNameAndContentID(channelName, contentID).orElseThrow().getPaymentsLeft() + 1;
        var contentPricePerHundred = priceOfContent(contentType);
        var functionCall = loadInterfaceService(ContractFunctionEnum.WatchNowPayLater, 0).watchNowPayLater(channelName, creatorID, contentID, BigInteger.valueOf(contentPricePerHundred), BigInteger.valueOf(paymentIncrements));
        var watchNowPayLaterTransactionReceipt = executeFunctionCall(functionCall);
        if(watchNowPayLaterTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        var purchasedContent = paymentService.findChannelPayment(channelName, contentID);
        purchasedContent.setTransactionHash(watchNowPayLaterTransactionReceipt.getTransactionHash());
        purchasedContent.setStatus(PaymentEnum.Purchased);
        var totalManaAmount = functionDetails.getManaAmount();
        var manaToCompany = totalManaAmount / 10;
        var content = contentRepository.findById(contentID).orElseThrow();
        messageService.purchasedPaymentChannelMessage(channelName, purchasedContent, content);
        log.setContractTransactionReceipt(watchNowPayLaterTransactionReceipt);
        log.setTotalManaAmount(totalManaAmount);
        log.setManaToCompany(manaToCompany);
        sivantisContractLogsRepository.save(log);
        paymentRepository.save(purchasedContent);
    }

    private void resolveUpdatePersonalWallet(SivantisContractLogs log) {
        var userId = log.getContractFunctionDetails().get(0).getUserID();
        var _newPersonalWallet = userRepository.findById(userId).orElseThrow().getPersonalWallet();
        var functionCall = loadInterfaceService(ContractFunctionEnum.UpdatePersonalWallet, 0).updatePersonalWallet(userId, _newPersonalWallet);
        var updatePersonalWalletTransactionReceipt = executeFunctionCall(functionCall);
        if(updatePersonalWalletTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        log.setContractTransactionReceipt(updatePersonalWalletTransactionReceipt);
        sivantisContractLogsRepository.save(log);
    }

    private void resolveAddContentCreator(SivantisContractLogs log) {
        var user = userRepository.findById(log.getContractFunctionDetails().get(0).getUserID()).orElseThrow();
        var creatorID = user.getUserId();
        var creatorPersonalWallet = user.getPersonalWallet();
        var rank = user.getRank();
        var functionCall = loadInterfaceService(ContractFunctionEnum.AddContentCreator, 0).addContentCreator(creatorID, creatorPersonalWallet, BigInteger.valueOf(rank));
        var addContentCreatorTransactionReceipt = executeFunctionCall(functionCall);
        if(addContentCreatorTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        log.setContractTransactionReceipt(addContentCreatorTransactionReceipt);
        sivantisContractLogsRepository.save(log);
    }

    private void resolveUpdateAverageWeeklyViewers(SivantisContractLogs log) {
        var channel = channelRepository.findByChannelName(log.getContractFunctionDetails().get(0).getChannelName()).orElseThrow();
        var highestAverageWeeklyViewers = paymentService.getMaxNumber(channel.getStreamerInfo().stream().map(StreamerInfo::getAverageWeeklyViewers).collect(Collectors.toList()));
        var functionCall = loadInterfaceService(ContractFunctionEnum.UpdateAverageWeeklyViewers, 0).updateAverageWeeklyViewers(channel.getChannelName(), BigInteger.valueOf(Math.round(highestAverageWeeklyViewers)));
        var updateAverageWeeklyViewersTransactionReceipt = executeFunctionCall(functionCall);
        if(updateAverageWeeklyViewersTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        log.setContractTransactionReceipt(updateAverageWeeklyViewersTransactionReceipt);
        sivantisContractLogsRepository.save(log);
    }

    private void resolveCancelPayment(SivantisContractLogs log) {
        var functionDetails = log.getContractFunctionDetails().get(0);
        var creatorID = functionDetails.getUserID();
        var contentID = functionDetails.getContentID();
        var channelName = functionDetails.getChannelName();
        var functionCall = loadInterfaceService(ContractFunctionEnum.CancelPayment,0).sendRefundPayment(creatorID,contentID,channelName);
        var sendRefundPaymentTransactionReceipt = executeFunctionCall(functionCall);
        if(sendRefundPaymentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        var Payment = paymentService.findChannelPayment(channelName, contentID);
        Payment.setTransactionHash(sendRefundPaymentTransactionReceipt.getTransactionHash());
        Payment.setStatus(PaymentEnum.RefundedPurchase);
        var content = contentRepository.findById(functionDetails.getContentID()).orElseThrow();
        var user = userRepository.findById(Payment.getUserId()).orElseThrow();
        messageService.refundChannelMessage(user, Payment.getTransactionHash(), channelName, functionDetails.getManaAmount().toString(), content);
        log.setTotalManaAmount(functionDetails.getManaAmount());
        log.setContractTransactionReceipt(sendRefundPaymentTransactionReceipt);
        sivantisContractLogsRepository.save(log);
        paymentRepository.save(Payment);
    }

    private void resolvePayForContent(SivantisContractLogs log) {
        var functionDetails = log.getContractFunctionDetails().get(0);
        var creatorID = functionDetails.getUserID();
        var contentID = functionDetails.getContentID();
        var channelName = functionDetails.getChannelName();
        var contentType = contentRepository.findById(contentID).orElseThrow().getContentType();
        var contentPricePerHundred = priceOfContent(contentType);
        var functionCall = loadInterfaceService(ContractFunctionEnum.PayForContent, 0).payForContent(channelName,creatorID,contentID,BigInteger.valueOf(contentPricePerHundred));
        var payForContentTransactionReceipt = executeFunctionCall(functionCall);
        if(payForContentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        var purchasedContent = paymentService.findChannelPayment(channelName, contentID);
        purchasedContent.setTransactionHash(payForContentTransactionReceipt.getTransactionHash());
        purchasedContent.setStatus(PaymentEnum.Purchased);
        var totalManaAmount = functionDetails.getManaAmount();
        var manaToCompany = totalManaAmount / 10;
        var content = contentRepository.findById(contentID).orElseThrow();
        messageService.purchasedPaymentChannelMessage(channelName, purchasedContent, content);
        log.setContractTransactionReceipt(payForContentTransactionReceipt);
        log.setTotalManaAmount(totalManaAmount);
        log.setManaToCompany(manaToCompany);
        sivantisContractLogsRepository.save(log);
        paymentRepository.save(purchasedContent);
    }

    private void resolveAddChannel(SivantisContractLogs log) {
        var channel = channelRepository.findByChannelName(log.getContractFunctionDetails().get(0).getChannelName()).orElseThrow();
        var highestAverageWeeklyViewers = paymentService.getMaxNumber(channel.getStreamerInfo().stream().map(StreamerInfo::getAverageWeeklyViewers).collect(Collectors.toList()));
        var functionCall = loadInterfaceService(ContractFunctionEnum.AddChannel, 0).addChannel(channel.getChannelName(), BigInteger.valueOf(Math.round(highestAverageWeeklyViewers)));
        var addChannelTransactionReceipt = executeFunctionCall(functionCall);
        if(addChannelTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        log.setContractTransactionReceipt(addChannelTransactionReceipt);
        sivantisContractLogsRepository.save(log);
    }

    private void resolveSetAuctionToActive(SivantisContractLogs log) {
        var content = contentRepository.findById(log.getContractFunctionDetails().get(0).getContentID()).orElseThrow();
        var userIDs = content.getListOfBuyerIds().keySet().stream().toList();
        List<String> previousWinners = userIDs.subList(0, Math.min(content.getNumbBidders(), userIDs.size()));
        var functionCall = loadInterfaceService(ContractFunctionEnum.SetAuctionToActive, previousWinners.size()).setAuctionToActive(content.getContentId(), previousWinners);
        var setAuctionToActiveTransactionReceipt = executeFunctionCall(functionCall);
        if(setAuctionToActiveTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        log.setContractTransactionReceipt(setAuctionToActiveTransactionReceipt);
        sivantisContractLogsRepository.save(log);
    }

    private void resolveCreateNewAuction(SivantisContractLogs log) {
        var content = contentRepository.findById(log.getContractFunctionDetails().get(0).getContentID()).orElseThrow();
        var creatorAddress = userRepository.findById(content.getCreatorID()).orElseThrow().getPersonalWallet();
        var functionCall = loadInterfaceService(ContractFunctionEnum.CreateNewAuction, 0).createNewAuction(content.getContentId(), BigInteger.valueOf(content.getNumbBidders()), BigInteger.valueOf(content.getStartingCost()), creatorAddress);
        var createNewAuctionTransactionReceipt = executeFunctionCall(functionCall);
        if(createNewAuctionTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        log.setContractTransactionReceipt(createNewAuctionTransactionReceipt);
        sivantisContractLogsRepository.save(log);
    }

    private void resolveSendWatchNowPayLaterRefundPayment(SivantisContractLogs unresolvedLog) {
        var userID = unresolvedLog.getContractFunctionDetails().get(0).getUserID();
        var contentID = unresolvedLog.getContractFunctionDetails().get(0).getContentID();
        var channelName = unresolvedLog.getContractFunctionDetails().get(0).getChannelName();
        var functionCall = loadInterfaceService(ContractFunctionEnum.SendWatchNowPayLaterRefundPayment, 0).sendWatchNowPayLaterRefundPayment(userID, contentID, channelName);
        var WatchNowPayLaterRefundPaymentTransactionReceipt = executeFunctionCall(functionCall);
        if(WatchNowPayLaterRefundPaymentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        paymentService.refundChannelPurchasedContent(channelName, contentID, WatchNowPayLaterRefundPaymentTransactionReceipt.getTransactionHash());
        if(watchNowPayLaterRepository.findByChannelNameAndContentID(channelName, contentID).isPresent()){
            var watchNowPayLater = watchNowPayLaterRepository.findByChannelNameAndContentID(channelName, contentID).get();
            var channel = channelRepository.findByChannelName(channelName).orElseThrow();
            channel.getWatchNowPayLaterIDs().remove(watchNowPayLater.getWatchNowPlayLaterId());
            channelRepository.save(channel);
            watchNowPayLaterRepository.delete(watchNowPayLater);
        }
        unresolvedLog.setContractTransactionReceipt(WatchNowPayLaterRefundPaymentTransactionReceipt);
        sivantisContractLogsRepository.save(unresolvedLog);
    }

    private void resolveSendWatchNowPayLaterRefundPaymentMultiCall(SivantisContractLogs unresolvedLog) {
        List<byte[]> transactions = new java.util.ArrayList<>(List.of());
        var sendWatchNowPayLaterRefundPaymentFunctionDetailsList = unresolvedLog.getContractFunctionDetails();
        for(ContractFunctionDetails contractFunctionDetails: sendWatchNowPayLaterRefundPaymentFunctionDetailsList){
            var userID = contractFunctionDetails.getUserID();
            var contentID = contractFunctionDetails.getContentID();
            var channelName = contractFunctionDetails.getChannelName();
            var function = new Function(
                    "sendWatchNowPayLaterRefundPayment",
                    List.of(new Utf8String(userID), new Utf8String(contentID), new Utf8String(channelName)),
                    Collections.emptyList()
            );
            transactions.add(multiSendHelperService.buildCall(BigInteger.ZERO, function));
        }
        var watchNowPayLaterRefundPaymentMultiCallTransactionResponse = sendServerSideMultiCallTransaction(transactions, ContractFunctionEnum.SendWatchNowPayLaterRefundPayment);
        var sendWatchNowPayLaterRefundPaymentTransactionReceipt = watchNowPayLaterRefundPaymentMultiCallTransactionResponse.get(0).getContractTransactionReceipt();
        if (sendWatchNowPayLaterRefundPaymentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)) {
            throw new SivantisException("Unable to Resolve Log");
        } else {
            for(ContractFunctionDetails FunctionDetails : sendWatchNowPayLaterRefundPaymentFunctionDetailsList){
                paymentService.refundChannelPurchasedContent(FunctionDetails.getChannelName(), FunctionDetails.getContentID(), sendWatchNowPayLaterRefundPaymentTransactionReceipt.getTransactionHash());
                if(watchNowPayLaterRepository.findByChannelNameAndContentID(FunctionDetails.getChannelName(), FunctionDetails.getContentID()).isPresent()){
                    var watchNowPayLater = watchNowPayLaterRepository.findByChannelNameAndContentID(FunctionDetails.getChannelName(), FunctionDetails.getContentID()).get();
                    var channel = channelRepository.findByChannelName(FunctionDetails.getChannelName()).orElseThrow();
                    channel.getWatchNowPayLaterIDs().remove(watchNowPayLater.getWatchNowPlayLaterId());
                    channelRepository.save(channel);
                    watchNowPayLaterRepository.delete(watchNowPayLater);
                }
            }
            unresolvedLog.setContractTransactionReceipt(sendWatchNowPayLaterRefundPaymentTransactionReceipt);
            sivantisContractLogsRepository.save(unresolvedLog);
        }
    }

    private void resolveClientMultiCall(SivantisContractLogs unresolvedLog) {
        List<ClientSideMultiCallPackage> clientSideMultiCallPackage = repackageClientMultiCall(unresolvedLog);
        org.web3j.crypto.Credentials credentials = getCredentials();
        ArrayList<MultiCallResponse> multiCallResponses = new ArrayList<>();
        List<byte[]> transactions = new ArrayList<>();
        BigInteger gasPrice = getGasPrice(web3j);
        for(ClientSideMultiCallPackage multiCallPackage: clientSideMultiCallPackage){
            transactions.add(multiCallPackage.getFunctionData());
        }
        var contractTransactionReceipt = multiSendHelperService.executeMultiCall(
                credentials,
                gasPrice,
                BigInteger.valueOf(10000000L),
                transactions
        );
        if(contractTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setContractTransactionReceipt(contractTransactionReceipt);
        paymentCheck(clientSideMultiCallPackage, contractTransactionReceipt.getTransactionHash(), unresolvedLog);
        sivantisContractLogsRepository.save(unresolvedLog);
    }

    private void paymentCheck(List<ClientSideMultiCallPackage> clientSideMultiCallPackages, String transactionHash, SivantisContractLogs clientMultiCallLog){
        double totalManaAmount = 0.0;
        double manaToCompany = 0.0;
        for (ClientSideMultiCallPackage multiCallPackage: clientSideMultiCallPackages){
            var contractFunctionDetails = multiCallPackage.getContractFunctionDetails();
            switch (multiCallPackage.getContractFunctionDetails().getContractFunctionEnum()){
                case PayForContent, WatchNowPayLater -> {
                    var purchasedContent = paymentService.findChannelPayment(contractFunctionDetails.getChannelName(), contractFunctionDetails.getContentID());
                    purchasedContent.setTransactionHash(transactionHash);
                    purchasedContent.setStatus(PaymentEnum.Purchased);
                    totalManaAmount = totalManaAmount + contractFunctionDetails.getManaAmount();
                    manaToCompany = manaToCompany + contractFunctionDetails.getManaAmount() / 10;
                    var content = contentRepository.findById(contractFunctionDetails.getContentID()).orElseThrow();
                    messageService.purchasedPaymentChannelMessage(contractFunctionDetails.getChannelName(), purchasedContent, content);
                    paymentRepository.save(purchasedContent);
                }
                case CancelWatchNowPayLater, CancelPayment -> {
                    var Payment = paymentService.findChannelPayment(contractFunctionDetails.getChannelName(), contractFunctionDetails.getContentID());
                    Payment.setTransactionHash(transactionHash);
                    Payment.setStatus(PaymentEnum.RefundedPurchase);
                    totalManaAmount = totalManaAmount + contractFunctionDetails.getManaAmount();
                    var content = contentRepository.findById(contractFunctionDetails.getContentID()).orElseThrow();
                    var user = userRepository.findById(Payment.getUserId()).orElseThrow();
                    messageService.refundChannelMessage(user, Payment.getTransactionHash(), contractFunctionDetails.getChannelName(), contractFunctionDetails.getManaAmount().toString(), content);
                    paymentRepository.save(Payment);
                }
                case ReturnBid -> {
                    var Payment = paymentService.findPayment(contractFunctionDetails.getUserID(), contractFunctionDetails.getContentID());
                    Payment.setTransactionHash(transactionHash);
                    totalManaAmount = totalManaAmount + contractFunctionDetails.getManaAmount();
                    paymentRepository.save(Payment);
                }
            }
        }
        if(totalManaAmount > 0.0){
            clientMultiCallLog.setTotalManaAmount(totalManaAmount);
        }
        if(manaToCompany > 0.0){
            clientMultiCallLog.setManaToCompany(manaToCompany);
        }
    }


    private void resolveIncreaseCreatorRank(SivantisContractLogs unresolvedLog)  {
        var userID = unresolvedLog.getContractFunctionDetails().get(0).getUserID();
        var functionCall = loadInterfaceService(ContractFunctionEnum.IncreaseCreatorRank, 0).increaseCreatorRank(userID);
        var increaseCreatorRankTransactionReceipt = executeFunctionCall(functionCall);
        if(increaseCreatorRankTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setContractTransactionReceipt(increaseCreatorRankTransactionReceipt);
        sivantisContractLogsRepository.save(unresolvedLog);
    }

    private void resolveIncreaseCreatorRankMultiCall(SivantisContractLogs unresolvedLog){
        List<byte[]> transactions = new java.util.ArrayList<>(List.of());
        var increaseCreatorRankFunctionDetailsList = unresolvedLog.getContractFunctionDetails();
        for(ContractFunctionDetails contractFunctionDetails: increaseCreatorRankFunctionDetailsList){
            var userID = contractFunctionDetails.getUserID();
            var function = new Function(
                    "increaseCreatorRank",
                    List.of(new Utf8String(userID)),
                    Collections.emptyList()
            );
            transactions.add(multiSendHelperService.buildCall(BigInteger.ZERO, function));
        }
        var increaseCreatorRankMultiCallTransactionResponse = sendServerSideMultiCallTransaction(transactions, ContractFunctionEnum.IncreaseCreatorRank);
        var increaseCreatorRankTransactionReceipt = increaseCreatorRankMultiCallTransactionResponse.get(0).getContractTransactionReceipt();
        if(increaseCreatorRankTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setContractTransactionReceipt(increaseCreatorRankTransactionReceipt);
        sivantisContractLogsRepository.save(unresolvedLog);
    }

    private void resolveWatchNowPayLaterPayment(SivantisContractLogs unresolvedLog) {
        var channelName = unresolvedLog.getContractFunctionDetails().get(0).getChannelName();
        var contentID = unresolvedLog.getContractFunctionDetails().get(0).getContentID();
        var watchNowPayLater = watchNowPayLaterRepository.findByChannelNameAndContentID(channelName, contentID).orElseThrow();
        var functionCall = loadInterfaceService(ContractFunctionEnum.WatchNowPayLaterPayment, 0).watchNowPayLaterPayment(channelName, contentID);
        var watchNowPayLaterPaymentTransactionReceipt = executeFunctionCall(functionCall);
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
        watchNowPayLaterRepository.save(watchNowPayLater);
        sivantisContractLogsRepository.save(unresolvedLog);
    }

    private void resolveWatchNowPayLaterPaymentMultiCall(SivantisContractLogs unresolvedLog){
        List<byte[]> transactions = new java.util.ArrayList<>(List.of());
        var watchNowPayLaterPaymentFunctionDetailsList = unresolvedLog.getContractFunctionDetails();
        for(ContractFunctionDetails contractFunctionDetails: watchNowPayLaterPaymentFunctionDetailsList){
            var contentId = contractFunctionDetails.getContentID();
            var channelName = contractFunctionDetails.getChannelName();
            var function = new Function(
                    "watchNowPayLaterPayment",
                    List.of(new Utf8String(channelName),  new Utf8String(contentId)),
                    Collections.emptyList()
            );
            transactions.add(multiSendHelperService.buildCall(BigInteger.ZERO, function));
        }
        var watchNowPayLaterPaymentMultiCallTransactionResponse = sendServerSideMultiCallTransaction(transactions, ContractFunctionEnum.WatchNowPayLaterPayment);
        var watchNowPayLaterPaymentTransactionReceipt = watchNowPayLaterPaymentMultiCallTransactionResponse.get(0).getContractTransactionReceipt();
        if(watchNowPayLaterPaymentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setContractTransactionReceipt(watchNowPayLaterPaymentTransactionReceipt);
        sivantisContractLogsRepository.save(unresolvedLog);
        for(ContractFunctionDetails contractFunctionDetails: watchNowPayLaterPaymentFunctionDetailsList){
            var contentID = contractFunctionDetails.getContentID();
            var channelName = contractFunctionDetails.getChannelName();
            if(watchNowPayLaterRepository.findByChannelNameAndContentID(channelName, contentID).isPresent()){
                var watchNowPayLater = watchNowPayLaterRepository.findByChannelNameAndContentID(channelName, contentID).orElseThrow();
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

    private void resolveSendRefundPayment(SivantisContractLogs unresolvedLog) {
        var channelName = unresolvedLog.getContractFunctionDetails().get(0).getChannelName();
        var contentID = unresolvedLog.getContractFunctionDetails().get(0).getContentID();
        var userID = unresolvedLog.getContractFunctionDetails().get(0).getUserID();
        var functionCall = loadInterfaceService(ContractFunctionEnum.SendRefundPayment, 0).sendRefundPayment(userID, contentID, channelName);
        var sendRefundPaymentTransactionReceipt = executeFunctionCall(functionCall);
        if(sendRefundPaymentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setContractTransactionReceipt(sendRefundPaymentTransactionReceipt);
        sivantisContractLogsRepository.save(unresolvedLog);
        paymentService.refundChannelPurchasedContent(channelName, contentID, sendRefundPaymentTransactionReceipt.getTransactionHash());
    }

    private void resolveSendRefundPaymentMultiCall(SivantisContractLogs unresolvedLog){
        List<byte[]> transactions = new java.util.ArrayList<>(List.of());
        var sendRefundPaymentFunctionDetailsList = unresolvedLog.getContractFunctionDetails();
        for(ContractFunctionDetails contractFunctionDetails: sendRefundPaymentFunctionDetailsList){
            var contentId = contractFunctionDetails.getContentID();
            var channelName = contractFunctionDetails.getChannelName();
            var userID = contractFunctionDetails.getUserID();
            var function = new Function(
                    "sendRefundPayment",
                    List.of(new Utf8String(userID),  new Utf8String(contentId), new Utf8String(channelName)),
                    Collections.emptyList()
            );
            transactions.add(multiSendHelperService.buildCall(BigInteger.ZERO, function));
        }
        var sendRefundPaymentMultiCallTransactionResponse = sendServerSideMultiCallTransaction(transactions, ContractFunctionEnum.SendRefundPayment);
        var sendRefundPaymentTransactionReceipt = sendRefundPaymentMultiCallTransactionResponse.get(0).getContractTransactionReceipt();
        if(sendRefundPaymentTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setContractTransactionReceipt(sendRefundPaymentTransactionReceipt);
        sivantisContractLogsRepository.save(unresolvedLog);
        for(ContractFunctionDetails contractFunctionDetails: sendRefundPaymentFunctionDetailsList){
            var contentID = contractFunctionDetails.getContentID();
            var channelName = contractFunctionDetails.getChannelName();
            paymentService.refundChannelPurchasedContent(channelName, contentID, sendRefundPaymentTransactionReceipt.getTransactionHash());
        }
    }

    private void resolveSetAuctionToInactive(SivantisContractLogs unresolvedLog)  {
        var contentID = unresolvedLog.getContractFunctionDetails().get(0).getContentID();
        var functionCall = loadInterfaceService(ContractFunctionEnum.SetAuctionToInactive, 0).setAuctionToInactive(contentID);
        var setAuctionToInactiveTransactionReceipt = executeFunctionCall(functionCall);
        if(setAuctionToInactiveTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setContractTransactionReceipt(setAuctionToInactiveTransactionReceipt);
        sivantisContractLogsRepository.save(unresolvedLog);
    }

    private void resolveSetAuctionToInactiveMultiCall(SivantisContractLogs unresolvedLog){
        List<byte[]> transactions = new java.util.ArrayList<>(List.of());
        var setAuctionToInactiveFunctionDetailsList = unresolvedLog.getContractFunctionDetails();
        for(ContractFunctionDetails contractFunctionDetails: setAuctionToInactiveFunctionDetailsList){
            var contentId = contractFunctionDetails.getContentID();
            var function = new Function(
                    "setAuctionToInactive",
                    List.of(new Utf8String(contentId)),
                    Collections.emptyList()
            );
            transactions.add(multiSendHelperService.buildCall(BigInteger.ZERO, function));
        }
        var setAuctionToInactiveMultiCallTransactionResponse = sendServerSideMultiCallTransaction(transactions, ContractFunctionEnum.SetAuctionToInactive);
        var setAuctionToInactiveTransactionReceipt = setAuctionToInactiveMultiCallTransactionResponse.get(0).getContractTransactionReceipt();
        if(setAuctionToInactiveTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setContractTransactionReceipt(setAuctionToInactiveTransactionReceipt);
        sivantisContractLogsRepository.save(unresolvedLog);
    }

    private void resolveSendWeeklyMana(SivantisContractLogs unresolvedLog)  {
        var userID = unresolvedLog.getContractFunctionDetails().get(0).getUserID();
        var functionCall = loadInterfaceService(ContractFunctionEnum.SendWeeklyMana, 0).sendWeeklyMana(userID);
        var SendWeeklyManaTransactionReceipt = executeFunctionCall(functionCall);
        if(SendWeeklyManaTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setContractTransactionReceipt(SendWeeklyManaTransactionReceipt);
        sivantisContractLogsRepository.save(unresolvedLog);
    }

    private void resolveSendWeeklyManaMultiCall(SivantisContractLogs unresolvedLog){
        List<byte[]> transactions = new java.util.ArrayList<>(List.of());
        var sendWeeklyManaFunctionDetailsList = unresolvedLog.getContractFunctionDetails();
        for(ContractFunctionDetails contractFunctionDetails: sendWeeklyManaFunctionDetailsList){
            var userID = contractFunctionDetails.getUserID();
            var function = new Function(
                    "sendWeeklyMana",
                    List.of(new Utf8String(userID)),
                    Collections.emptyList()
            );
            transactions.add(multiSendHelperService.buildCall(BigInteger.ZERO, function));
        }
        var sendWeeklyManaMultiCallTransactionResponse = sendServerSideMultiCallTransaction(transactions, ContractFunctionEnum.SendWeeklyMana);
        var sendWeeklyManaTransactionReceipt = sendWeeklyManaMultiCallTransactionResponse.get(0).getContractTransactionReceipt();
        if(sendWeeklyManaTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setContractTransactionReceipt(sendWeeklyManaTransactionReceipt);
        sivantisContractLogsRepository.save(unresolvedLog);
    }

    private void resolveReturnBid(SivantisContractLogs unresolvedLog)  {
        var contentID = unresolvedLog.getContractFunctionDetails().get(0).getContentID();
        var userID = unresolvedLog.getContractFunctionDetails().get(0).getUserID();
        var functionCall = loadInterfaceService(ContractFunctionEnum.ReturnBid,0).returnBid(contentID, userID);
        var returnBidTransactionReceipt = executeFunctionCall(functionCall);
        if(returnBidTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setContractTransactionReceipt(returnBidTransactionReceipt);
        sivantisContractLogsRepository.save(unresolvedLog);
        paymentService.refundPurchasedContent(userID, contentID, returnBidTransactionReceipt.getTransactionHash());
    }

    private void resolveReturnBidMultiCall(SivantisContractLogs unresolvedLog){
        List<byte[]> transactions = new java.util.ArrayList<>(List.of());
        var returnBidFunctionDetailsList = unresolvedLog.getContractFunctionDetails();
        for(ContractFunctionDetails contractFunctionDetails: returnBidFunctionDetailsList){
            var contentID = contractFunctionDetails.getContentID();
            var userID = contractFunctionDetails.getUserID();
            var function = new Function(
                    "returnBid",
                    List.of(new Utf8String(contentID), new Utf8String(userID)),
                    Collections.emptyList()
            );
            transactions.add(multiSendHelperService.buildCall(BigInteger.ZERO, function));
        }
        var returnBidMultiCallTransactionResponse = sendServerSideMultiCallTransaction(transactions, ContractFunctionEnum.ReturnBid);
        var returnBidTransactionReceipt = returnBidMultiCallTransactionResponse.get(0).getContractTransactionReceipt();
        if(returnBidTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setContractTransactionReceipt(returnBidTransactionReceipt);
        sivantisContractLogsRepository.save(unresolvedLog);
        for(ContractFunctionDetails contractFunctionDetails: returnBidFunctionDetailsList){
            var contentID = contractFunctionDetails.getContentID();
            var userID = contractFunctionDetails.getUserID();
            paymentService.refundPurchasedContent(userID, contentID, returnBidTransactionReceipt.getTransactionHash());
        }
    }

    private void resolveSendMana(SivantisContractLogs unresolvedLog)  {
        var contentID = unresolvedLog.getContractFunctionDetails().get(0).getContentID();
        var userID = unresolvedLog.getContractFunctionDetails().get(0).getUserID();
        var content = contentRepository.findById(contentID).orElseThrow();
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
        var functionCall = loadInterfaceService(ContractFunctionEnum.SendMana, 0).sendMana(contentID);
        var sendManaTransactionReceipt = executeFunctionCall(functionCall);
        unresolvedLog.setContractTransactionReceipt(sendManaTransactionReceipt);
        if(sendManaTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        unresolvedLog.setTotalManaAmount(totalEarnings);
        unresolvedLog.setManaToCompany(totalEarnings / 15);
        sivantisContractLogsRepository.save(unresolvedLog);
        paymentService.resolvedAuctionPayment(contentID, userID);
    }

    private void resolveSendManaMultiCall(SivantisContractLogs unresolvedLog){
        List<byte[]> transactions = new java.util.ArrayList<>(List.of());
        var sendManaFunctionDetailsList = unresolvedLog.getContractFunctionDetails();
        for(ContractFunctionDetails contractFunctionDetails: sendManaFunctionDetailsList){
            var contentID = contractFunctionDetails.getContentID();
            var userID = contractFunctionDetails.getUserID();
            var function = new Function(
                    "sendMana",
                    List.of(new Utf8String(contentID)),
                    Collections.emptyList()
            );
            transactions.add(multiSendHelperService.buildCall(BigInteger.ZERO, function));
        }
        var sendManaMultiCallTransactionResponse = sendServerSideMultiCallTransaction(transactions, ContractFunctionEnum.SendMana);
        var sendManaTransactionReceipt = sendManaMultiCallTransactionResponse.get(0).getContractTransactionReceipt();
        if(sendManaTransactionReceipt.getContractStatusEnum().equals(ContractStatusEnum.Error)){
            throw new SivantisException("Unable to Resolve Log");
        }
        var manaAmount = getManaAmount(sendManaFunctionDetailsList);
        unresolvedLog.setContractTransactionReceipt(sendManaTransactionReceipt);
        unresolvedLog.setTotalManaAmount(manaAmount);
        unresolvedLog.setManaToCompany(manaAmount / 15);
        sivantisContractLogsRepository.save(unresolvedLog);
        for(ContractFunctionDetails contractFunctionDetails: sendManaFunctionDetailsList){
            var contentID = contractFunctionDetails.getContentID();
            var userID = contractFunctionDetails.getUserID();
            paymentService.resolvedAuctionPayment(contentID, userID);
        }
    }


    private InterfaceService loadInterfaceService(ContractFunctionEnum contractFunctionEnum, int listOfBuyersSize){
        return InterfaceService.load(interfaceModuleAddress, web3j, getCredentials(), new StaticGasProvider(getGasPrice(web3j), getGasLimit(contractFunctionEnum, listOfBuyersSize)));
    }

    private org.web3j.crypto.Credentials getCredentials(){
        return credentialsService.getCredentials();
    }

    private BigInteger getGasLimit(ContractFunctionEnum contractFunctionEnum, int listOfBuyersSize){
        return gasLimitService.getGasLimit(contractFunctionEnum, listOfBuyersSize).multiply(BigInteger.valueOf(2));
    }

    private SivantisContractLogs createBidServiceLogs(ContractFunctionDetails contractFunctionDetails){
        ArrayList<ContractFunctionDetails> contractFunctionDetailsList = new ArrayList<>();
        contractFunctionDetailsList.add(contractFunctionDetails);
        return SivantisContractLogs.builder()
                .logId(ObjectId.get().toHexString())
                .contractFunctionDetails(contractFunctionDetailsList)
                .creationDate(Instant.now())
                .contractEnum(ContractEnum.BidService)
                .transactionCount(1)
                .build();
    }

    private SivantisContractLogs createChannelServiceLogs(ContractFunctionDetails contractFunctionDetails){
        ArrayList<ContractFunctionDetails> contractFunctionDetailsList = new ArrayList<>();
        contractFunctionDetailsList.add(contractFunctionDetails);
        return SivantisContractLogs.builder()
                .logId(ObjectId.get().toHexString())
                .creationDate(Instant.now())
                .contractEnum(ContractEnum.ChannelService)
                .contractFunctionDetails(contractFunctionDetailsList)
                .transactionCount(1)
                .build();
    }


    private ContractTransactionReceipt executeFunctionCall(
            RemoteFunctionCall<TransactionReceipt> functionCall
    ){
        TransactionReceipt transactionReceipt;
        try {
            transactionReceipt = functionCall.send();
        } catch (Exception e) {
            return ContractTransactionReceipt.builder()
                    .contractStatusEnum(ContractStatusEnum.Error)
                    .revertReason(e.getMessage())
                    .build();
        }
        if (transactionReceipt == null || transactionReceipt.getStatus().equals("0x0")) {
            return ContractTransactionReceipt.builder()
                    .transactionHash(transactionReceipt != null ? transactionReceipt.getTransactionHash() : null)
                    .contractStatusEnum(ContractStatusEnum.Error)
                    .gasUsed(transactionReceipt != null ? transactionReceipt.getGasUsed() : null)
                    .revertReason(transactionReceipt != null ? transactionReceipt.getRevertReason() : "Unknown failure")
                    .build();
        }

        return ContractTransactionReceipt.builder()
                .transactionHash(transactionReceipt.getTransactionHash())
                .contractStatusEnum(ContractStatusEnum.Completed)
                .gasUsed(transactionReceipt.getGasUsed())
                .build();
    }

    private BigInteger getGasPrice(Web3j web3j){
        BigInteger polygonGasStation;
        try {
            polygonGasStation = web3j.ethGasPrice().send().getGasPrice();
        } catch (IOException e) {
            return BigInteger.valueOf(40000000000L);
        }
        return polygonGasStation.multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100));
    }

    private SivantisContractLogs createWarChestServiceLogs(ContractFunctionDetails contractFunctionDetails){
        ArrayList<ContractFunctionDetails> contractFunctionDetailsList = new ArrayList<>();
        contractFunctionDetailsList.add(contractFunctionDetails);
        return SivantisContractLogs.builder()
                .logId(ObjectId.get().toHexString())
                .creationDate(Instant.now())
                .contractEnum(ContractEnum.WarChestService)
                .contractFunctionDetails(contractFunctionDetailsList)
                .transactionCount(1)
                .build();
    }

    private int priceOfContent(String contentType){
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

    private ArrayList<MultiCallResponse> sendServerSideMultiCallTransaction(List<byte[]> calls, ContractFunctionEnum contractFunctionEnum){
        return multiSendHelperService.callServerSideMultiSend(calls, contractFunctionEnum);
    }

    private List<ClientSideMultiCallPackage> repackageClientMultiCall(SivantisContractLogs unresolvedLog) {
        List<ClientSideMultiCallPackage> clientSideMultiCallPackages = new ArrayList<>();
        for (ContractFunctionDetails functionDetails: unresolvedLog.getContractFunctionDetails()){
            switch (functionDetails.getContractFunctionEnum()){
                case UpdatePersonalWallet -> {
                    var _newPersonalWallet = userRepository.findById(functionDetails.getUserID()).orElseThrow().getPersonalWallet();
                    Function function = new Function(
                            "updatePersonalWallet",
                            List.of(
                                    new Utf8String(functionDetails.getUserID()),
                                    new Address(_newPersonalWallet)
                            ),
                            Collections.emptyList()
                    );
                    var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                            .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                            .build();
                    clientSideMultiCallPackages.add(clientSideMultiCallPackage);
                }
                case ReactivateContent -> {
                    var content = contentRepository.findById(functionDetails.getContentID()).orElseThrow();
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
                    var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                            .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                            .build();
                    clientSideMultiCallPackages.add(clientSideMultiCallPackage);
                }
                case CancelWatchNowPayLater -> {
                    var creatorID = functionDetails.getUserID();
                    var contentID = functionDetails.getContentID();
                    var channelName = functionDetails.getChannelName();
                    Function function = new Function(
                            "sendWatchNowPayLaterRefundPayment",
                            List.of(
                                    new Utf8String(creatorID),
                                    new Utf8String(contentID),
                                    new Utf8String(channelName)
                            ),
                            Collections.emptyList()
                    );
                    var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                            .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                            .build();
                    clientSideMultiCallPackages.add(clientSideMultiCallPackage);
                }
                case CancelPayment -> {
                    var creatorID = functionDetails.getUserID();
                    var contentID = functionDetails.getContentID();
                    var channelName = functionDetails.getChannelName();
                    Function function = new Function(
                            "sendRefundPayment",
                            List.of(
                                    new Utf8String(creatorID),
                                    new Utf8String(contentID),
                                    new Utf8String(channelName)
                            ),
                            Collections.emptyList()
                    );
                    var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                            .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                            .build();
                    clientSideMultiCallPackages.add(clientSideMultiCallPackage);
                }
                case AddContentCreator -> {
                    var user = userRepository.findById(functionDetails.getUserID()).orElseThrow();
                    var creatorID = user.getUserId();
                    var creatorPersonalWallet = user.getPersonalWallet();
                    var rank = user.getRank();
                    Function function = new Function(
                            "addContentCreator",
                            List.of(
                                    new Utf8String(creatorID),
                                    new Address(creatorPersonalWallet),
                                    new Uint8(rank)
                            ),
                            Collections.emptyList()
                    );
                    var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                            .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                            .build();
                    clientSideMultiCallPackages.add(clientSideMultiCallPackage);
                }
                case UpdateAverageWeeklyViewers -> {
                    var channel = channelRepository.findByChannelName(functionDetails.getChannelName()).orElseThrow();
                    var highestAverageWeeklyViewers = paymentService.getMaxNumber(channel.getStreamerInfo().stream().map(StreamerInfo::getAverageWeeklyViewers).collect(Collectors.toList()));
                    Function function = new Function(
                            "updateAverageWeeklyViewers",
                            List.of(
                                    new Utf8String(channel.getChannelName()),
                                    new Uint256(Math.round(highestAverageWeeklyViewers))
                            ),
                            Collections.emptyList()
                    );
                    var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                            .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                            .build();
                    clientSideMultiCallPackages.add(clientSideMultiCallPackage);
                }
                case WatchNowPayLater -> {
                    var creatorID = functionDetails.getUserID();
                    var contentID = functionDetails.getContentID();
                    var channelName = functionDetails.getChannelName();
                    var contentType = contentRepository.findById(contentID).orElseThrow().getContentType();
                    var paymentIncrements = watchNowPayLaterRepository.findByChannelNameAndContentID(channelName, contentID).orElseThrow().getPaymentsLeft() + 1;
                    var contentPricePerHundred = priceOfContent(contentType);
                    Function function = new Function(
                            "watchNowPayLater",
                            List.of(
                                    new Utf8String(channelName),
                                    new Utf8String(creatorID),
                                    new Utf8String(contentID),
                                    new Uint256(contentPricePerHundred),
                                    new Uint8(paymentIncrements)
                            ),
                            Collections.emptyList()
                    );
                    var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                            .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                            .build();
                    clientSideMultiCallPackages.add(clientSideMultiCallPackage);
                }
                case PayForContent -> {
                    var creatorID = functionDetails.getUserID();
                    var contentID = functionDetails.getContentID();
                    var channelName = functionDetails.getChannelName();
                    var contentType = contentRepository.findById(contentID).orElseThrow().getContentType();
                    var contentPricePerHundred = priceOfContent(contentType);
                    Function function = new Function(
                            "payForContent",
                            List.of(
                                    new Utf8String(channelName),
                                    new Utf8String(creatorID),
                                    new Utf8String(contentID),
                                    new Uint256(contentPricePerHundred)
                            ),
                            Collections.emptyList()
                    );
                    var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                            .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                            .build();
                    clientSideMultiCallPackages.add(clientSideMultiCallPackage);
                }
                case AddChannel -> {
                    var channel = channelRepository.findByChannelName(functionDetails.getChannelName()).orElseThrow();
                    var highestAverageWeeklyViewers = paymentService.getMaxNumber(channel.getStreamerInfo().stream().map(StreamerInfo::getAverageWeeklyViewers).collect(Collectors.toList()));
                    Function function = new Function(
                            "addChannel",
                            List.of(
                                    new Utf8String(channel.getChannelName()),
                                    new Uint256(Math.round(highestAverageWeeklyViewers))
                            ),
                            Collections.emptyList()
                    );
                    var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                            .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                            .build();
                    clientSideMultiCallPackages.add(clientSideMultiCallPackage);
                }
                case SetAuctionToActive -> {
                    var content = contentRepository.findById(functionDetails.getContentID()).orElseThrow();
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
                    var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                            .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                            .build();
                    clientSideMultiCallPackages.add(clientSideMultiCallPackage);
                }
                case ReturnBid -> {
                    Function function = new Function(
                            "returnBid",
                            List.of(
                                    new Utf8String(functionDetails.getContentID()),
                                    new Utf8String(functionDetails.getUserID())
                            ),
                            Collections.emptyList()
                    );
                    var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                            .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                            .build();
                    clientSideMultiCallPackages.add(clientSideMultiCallPackage);
                }
                case CreateNewAuction -> {
                    var content = contentRepository.findById(functionDetails.getContentID()).orElseThrow();
                    var creatorAddress = userRepository.findById(content.getCreatorID()).orElseThrow().getPersonalWallet();
                    Function function = new Function(
                            "createNewAuction",
                            List.of(
                                    new Utf8String(content.getContentId()),
                                    new Uint256(content.getNumbBidders()),
                                    new Uint256(content.getStartingCost()),
                                    new Address(creatorAddress)
                            ),
                            Collections.emptyList()
                    );
                    var clientSideMultiCallPackage = ClientSideMultiCallPackage.builder()
                            .functionData(multiSendHelperService.buildCall(BigInteger.ZERO, function))
                            .build();
                    clientSideMultiCallPackages.add(clientSideMultiCallPackage);
                }
            }
        }
        return clientSideMultiCallPackages;
    }
}
