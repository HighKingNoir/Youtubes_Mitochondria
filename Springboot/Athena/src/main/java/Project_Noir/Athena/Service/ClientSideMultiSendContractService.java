package Project_Noir.Athena.Service;

import Project_Noir.Athena.Model.*;
import Project_Noir.Athena.Repo.*;
import Project_Noir.Athena.SmartContracts.BidService.BidService;
import Project_Noir.Athena.SmartContracts.ChannelService.ChannelService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import okhttp3.OkHttpClient;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Int;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
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
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientSideMultiSendContractService {

    private final MultiSendHelperService multiSendHelperService;
    private final SivantisContractLogsRepository sivantisContractLogsRepository;
    private final GasLimitService gasLimitService;
    private final PaymentRepository paymentRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final MessageService messageService;
    private final MongoTemplate mongoTemplate;
    private final CredentialsService credentialsService;
    private final Ec2InstanceTagService ec2InstanceTagService;
    private final ClientSideMultiCallPackageRepository clientSideMultiCallPackageRepository;
    private final Web3JService web3JService;

    @Value("${contract.channel.address}")
    private String ChannelServiceAddress;




    private final BigInteger GAS_LIMIT = BigInteger.valueOf(10000000L);
    private final BigInteger baseGas = BigInteger.valueOf(250000L);



    public void addToQueue(ClientSideMultiCallPackage clientSideMultiCallPackage){
        clientSideMultiCallPackage.setStatus(BlockchainInteractionStatusEnum.Awaiting);
        clientSideMultiCallPackageRepository.save(clientSideMultiCallPackage);
    }

    @Scheduled(fixedDelayString = "${multiCall.scheduler.delay}")
    @SchedulerLock(name = "executeClientMultiCallLock", lockAtLeastFor = "PT10S", lockAtMostFor = "PT55S")
    public void executeMultiCall() {
        var allClientSideMultiCallPackages = clientSideMultiCallPackageRepository.findAllByStatus(BlockchainInteractionStatusEnum.Awaiting);
        if(allClientSideMultiCallPackages.isEmpty()){
            return;
        }
        ec2InstanceTagService.markTransactionInProgress();
        for (ClientSideMultiCallPackage clientSideMultiCallPackage: allClientSideMultiCallPackages){
            clientSideMultiCallPackage.setStatus(BlockchainInteractionStatusEnum.InProgress);
        }
        clientSideMultiCallPackageRepository.saveAll(allClientSideMultiCallPackages);
        try {
            Credentials credentials = getCredentials();
            ArrayList<MultiCallResponse> multiCallResponses = new ArrayList<>();
            var estimatedGasLimit = baseGas;
            List<byte[]> transactions = new ArrayList<>();
            var transactionCount = 0;
            for (int index = 0; index < allClientSideMultiCallPackages.size(); index++) {
                var clientSideMultiCallPackage = allClientSideMultiCallPackages.get(index);
                if(estimatedGasLimit.add(getGasLimit(clientSideMultiCallPackage.getContractFunctionDetails())).compareTo(GAS_LIMIT) > 0){
                    var contractTransactionReceipt = multiSendHelperService.executeMultiCall(
                            credentials,
                            getGasPrice(web3JService.web3j),
                            estimatedGasLimit,
                            transactions
                    );
                    credentials = getCredentials();
                    var multiCallResponse = MultiCallResponse.builder()
                            .contractTransactionReceipt(contractTransactionReceipt)
                            .transactionCount(transactionCount)
                            .build();
                    multiCallResponses.add(multiCallResponse);
                    estimatedGasLimit = baseGas;
                    transactions = new ArrayList<>();
                    transactionCount = 0;
                }
                estimatedGasLimit = estimatedGasLimit.add(getGasLimit(clientSideMultiCallPackage.getContractFunctionDetails()));
                transactions.add(clientSideMultiCallPackage.getFunctionData());
                transactionCount++;
                if (index == allClientSideMultiCallPackages.size() - 1){
                    var contractTransactionReceipt = multiSendHelperService.executeMultiCall(
                            credentials,
                            getGasPrice(web3JService.web3j),
                            estimatedGasLimit,
                            transactions
                    );
                    var multiCallResponse = MultiCallResponse.builder()
                            .contractTransactionReceipt(contractTransactionReceipt)
                            .transactionCount(transactionCount)
                            .build();
                    multiCallResponses.add(multiCallResponse);
                }
            }
            int index = 0;
            for (MultiCallResponse multiCallResponse: multiCallResponses){
                List<ClientSideMultiCallPackage> subList = allClientSideMultiCallPackages.subList(
                        index,
                        index + multiCallResponse.getTransactionCount()
                );
                var clientMultiCallLog = createMultiCallLog(subList, multiCallResponse.getTransactionCount());
                clientMultiCallLog.setContractTransactionReceipt(multiCallResponse.getContractTransactionReceipt());
                if(multiCallResponse.getContractTransactionReceipt().getContractStatusEnum().equals(ContractStatusEnum.Completed)){
                    functionChecks(subList, multiCallResponse.getContractTransactionReceipt().getTransactionHash(), clientMultiCallLog);
                }
                sivantisContractLogsRepository.save(clientMultiCallLog);
                index += multiCallResponse.getTransactionCount();
            }
            clientSideMultiCallPackageRepository.deleteAll(allClientSideMultiCallPackages);
        } finally {
            ec2InstanceTagService.clearTransactionTag();
        }
    }

    private void functionChecks(List<ClientSideMultiCallPackage> clientSideMultiCallPackages, String transactionHash, SivantisContractLogs clientMultiCallLog){
        double totalManaAmount = 0.0;
        double manaToCompany = 0.0;
        var approvedChannelNames = new ArrayList<String>();
        var allPayments = new ArrayList<Payment>();
        var activeContent = new ArrayList<Content>();
        var allPendingContent = new ArrayList<Content>();
        var purchasedPaymentsList = new ArrayList<Payment>();
        var purchasedContentList = new ArrayList<Content>();
        var channelNamePurchases = new ArrayList<String>();
        var canceledUserList = new ArrayList<Users>();
        var canceledContentList = new ArrayList<Content>();
        var channelNameCancellation = new ArrayList<String>();
        var canceledManaAmounts = new ArrayList<String>();
        for (ClientSideMultiCallPackage multiCallPackage: clientSideMultiCallPackages){
            var contractFunctionDetails = multiCallPackage.getContractFunctionDetails();
            switch (multiCallPackage.getContractFunctionDetails().getContractFunctionEnum()){
                case AddChannel -> approvedChannelNames.add(contractFunctionDetails.getChannelName());
                case ReactivateContent, SetAuctionToActive, CreateNewAuction -> {
                    if(contentRepository.findById(contractFunctionDetails.getContentID()).isPresent()){
                        var content = contentRepository.findById(contractFunctionDetails.getContentID()).get();
                        content.setContentEnum(ContentEnum.Active);
                        content.setActiveDate(Instant.now());
                        activeContent.add(content);
                    }
                }
                case AddContentCreator -> {
                    if(userRepository.findById(contractFunctionDetails.getUserID()).isPresent()){
                        approveContentCreatorRequest(contractFunctionDetails.getUserID());
                        var user = userRepository.findById(contractFunctionDetails.getUserID()).get();
                        var pendingContent = contentRepository.findByContentIdInAndPendingCreatorApprovalTrue(user.getCreatedContent());
                        for (Content content: pendingContent){
                            content.setActiveDate(Instant.now());
                            content.setPendingCreatorApproval(false);
                            content.setContentEnum(ContentEnum.Active);
                        }
                        allPendingContent.addAll(pendingContent);
                    }
                }
                case PayForContent, WatchNowPayLater -> {
                    var purchasedContent = paymentService.findChannelPayment(contractFunctionDetails.getChannelName(), contractFunctionDetails.getContentID());
                    purchasedContent.setTransactionHash(transactionHash);
                    purchasedContent.setStatus(PaymentEnum.Purchased);
                    totalManaAmount = totalManaAmount + contractFunctionDetails.getManaAmount();
                    manaToCompany = manaToCompany + contractFunctionDetails.getManaAmount() / 10;
                    var content = contentRepository.findById(contractFunctionDetails.getContentID()).orElseThrow();
                    purchasedPaymentsList.add(purchasedContent);
                    purchasedContentList.add(content);
                    channelNamePurchases.add(contractFunctionDetails.getChannelName());
                    allPayments.add(purchasedContent);
                }
                case CancelWatchNowPayLater, CancelPayment -> {
                    var Payment = paymentService.findChannelPayment(contractFunctionDetails.getChannelName(), contractFunctionDetails.getContentID());
                    Payment.setTransactionHash(transactionHash);
                    Payment.setStatus(PaymentEnum.RefundedPurchase);
                    totalManaAmount = totalManaAmount + contractFunctionDetails.getManaAmount();
                    var content = contentRepository.findById(contractFunctionDetails.getContentID()).orElseThrow();
                    var user = userRepository.findById(Payment.getUserId()).orElseThrow();
                    canceledUserList.add(user);
                    channelNameCancellation.add(contractFunctionDetails.getChannelName());
                    canceledContentList.add(content);
                    canceledManaAmounts.add(contractFunctionDetails.getManaAmount().toString());
                    allPayments.add(Payment);
                }
                case ReturnBid -> {
                    var Payment = paymentService.findPayment(contractFunctionDetails.getUserID(), contractFunctionDetails.getContentID());
                    Payment.setTransactionHash(transactionHash);
                    Payment.setStatus(PaymentEnum.RefundedPurchase);
                    totalManaAmount = totalManaAmount + contractFunctionDetails.getManaAmount();
                    allPayments.add(Payment);
                }
            }
        }
        if(totalManaAmount > 0.0){
            clientMultiCallLog.setTotalManaAmount(totalManaAmount);
        }
        if(manaToCompany > 0.0){
            clientMultiCallLog.setManaToCompany(manaToCompany);
        }
        if(!approvedChannelNames.isEmpty()){
            messageService.approvedChannelMessage(approvedChannelNames);
        }
        if(!allPayments.isEmpty()){
            paymentRepository.saveAll(allPayments);
        }
        if(!activeContent.isEmpty()){
            contentRepository.saveAll(activeContent);
        }
        if(!allPendingContent.isEmpty()){
            contentRepository.saveAll(allPendingContent);
        }
        if(!channelNamePurchases.isEmpty()){
            messageService.purchasedPaymentChannelMessage(channelNamePurchases, purchasedPaymentsList, purchasedContentList);
        }
        if(!canceledUserList.isEmpty()){
            messageService.refundChannelMessage(canceledUserList, transactionHash, channelNameCancellation, canceledManaAmounts, canceledContentList);
        }
    }

    private void approveContentCreatorRequest(String userId) {
        Query query = new Query(Criteria.where("_id").is(userId));

        Update update = new Update()
                .set("isContentCreator", true)
                .set("contentCreatorPending", false);

        mongoTemplate.updateFirst(
                query,
                update,
                Users.class
        );
    }

    public boolean hasSufficientChannelBalance(
            String channelName,
            Double manaPrice,
            Double averageWeeklyViewers,
            String contentType,
            Integer numberOfPayments,
            Double pendingChannelPurchasesManaAmount
    ) {
        DefaultGasProvider contractGasProvider = new DefaultGasProvider();
        var readonly = new org.web3j.tx.ReadonlyTransactionManager(web3JService.web3j, ChannelServiceAddress);
        var channelService = ChannelService.load(ChannelServiceAddress, web3JService.web3j, readonly, contractGasProvider);
        var result = channelService.getChannelBalance(channelName);
        BigInteger weiBalance;
        try {
            weiBalance = result.send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        var manaNeeded = (averageWeeklyViewers * priceOfContent(contentType)) / (100 * manaPrice);
        var channelManaBalance = Convert.fromWei(weiBalance.toString(), Convert.Unit.ETHER).doubleValue();
        return channelManaBalance >= (manaNeeded / numberOfPayments) + pendingChannelPurchasesManaAmount;
    }

    private int priceOfContent(String contentType){
        return switch (contentType){
            case "Short Film" -> 5;
            case "Sports", "Concerts" -> 10;
            case "Movies" -> 20;
            default -> throw new IllegalStateException("Unexpected value: " + contentType);
        };
    }

    private org.web3j.crypto.Credentials getCredentials(){
        return credentialsService.getCredentials();
    }

    private BigInteger getGasLimit( ContractFunctionDetails contractFunctionDetails){
        if(contractFunctionDetails.getContractFunctionEnum().equals(ContractFunctionEnum.SetAuctionToActive) || contractFunctionDetails.getContractFunctionEnum().equals(ContractFunctionEnum.ReactivateContent)){
            var content = contentRepository.findById(contractFunctionDetails.getContentID()).orElseThrow();
            return gasLimitService.getGasLimit(contractFunctionDetails.getContractFunctionEnum(), content.getListOfBuyerIds().size());
        }
        else {
            return gasLimitService.getGasLimit(contractFunctionDetails.getContractFunctionEnum(), 0);
        }
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



    private SivantisContractLogs createMultiCallLog(List<ClientSideMultiCallPackage> clientSideMultiCallPackages, Integer transactionCount){
        var contractFunctionDetails = clientSideMultiCallPackages.stream().map(ClientSideMultiCallPackage::getContractFunctionDetails).toList();
        return SivantisContractLogs.builder()
                .logId(ObjectId.get().toHexString())
                .creationDate(Instant.now())
                .contractEnum(ContractEnum.ClientMultiCall)
                .contractFunctionDetails(contractFunctionDetails)
                .transactionCount(transactionCount)
                .build();
    }
}
