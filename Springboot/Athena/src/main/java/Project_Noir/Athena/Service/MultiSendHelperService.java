package Project_Noir.Athena.Service;

import Project_Noir.Athena.Model.*;
import Project_Noir.Athena.SmartContracts.BidService.BidService;
import Project_Noir.Athena.SmartContracts.MultiSendCallOnly.MultiSendCallOnly;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.*;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MultiSendHelperService {

    @Value("${contract.multiSend.address}")
    private String multiSendAddress;

    @Value("${contract.interface.address}")
    private String interfaceModuleAddress;

    @Value("${contract.bid.address}")
    private String BidServiceAddress;

    @Value("${infura.api.secret}")
    private String infuraAPISecret;

    @Value("${infura.api.key}")
    private String infuraAPIKey;
    private Web3j web3j;
    private final CredentialsService credentialsService;
    private final GasLimitService gasLimitService;
    private final BigInteger GAS_LIMIT = BigInteger.valueOf(10000000L);
    private final BigInteger baseGas = BigInteger.valueOf(250000L);
    private final BigInteger highGasLimit = BigInteger.valueOf(750000L);
    private final BigInteger midGasLimit = BigInteger.valueOf(500000L);
    private final BigInteger lowGasLimit = BigInteger.valueOf(250000L);

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
                    .header("Authorization", okhttp3.Credentials.basic(infuraAPIKey, infuraAPISecret))
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(request);
        });

        return new HttpService(url, clientBuilder.build());
    }

    public byte[] buildCall(
            BigInteger value,
            Function callFunction
    ) {
        byte operation = 0x00; // CALL
        byte[] to = Numeric.hexStringToByteArray(interfaceModuleAddress);
        byte[] paddedTo = Arrays.copyOfRange(to, 0, 20);
        byte[] val = Numeric.toBytesPadded(value, 32);
        byte[] data = Numeric.hexStringToByteArray(FunctionEncoder.encode(callFunction));
        byte[] dataLen = Numeric.toBytesPadded(BigInteger.valueOf(data.length), 32);

        ByteBuffer buffer = ByteBuffer.allocate(1 + 20 + 32 + 32 + data.length);
        buffer.put(operation);
        buffer.put(paddedTo);
        buffer.put(val);
        buffer.put(dataLen);
        buffer.put(data);
        return buffer.array();
    }


    public ArrayList<MultiCallResponse> callServerSideMultiSend(
            List<byte[]> transactions,
            ContractFunctionEnum contractFunctionEnum
    ) {
        var multiCallResponses = new ArrayList<MultiCallResponse>();
        BigInteger gasPrice = getGasPrice(web3j);
        BigInteger GAS_PER_TRANSACTION = gasLimitService.getGasLimit(contractFunctionEnum, 0);
        BigInteger estimatedGas  = baseGas.add(GAS_PER_TRANSACTION.multiply(BigInteger.valueOf(transactions.size())));
        Credentials credentials = getCredentials();
        if (estimatedGas.compareTo(GAS_LIMIT) > 0) {
            int numBatches = estimatedGas.add(GAS_LIMIT.subtract(BigInteger.ONE)).divide(GAS_LIMIT).intValue();
            List<List<byte[]>> splitTransactionBatches = new ArrayList<>();

            int totalSize = transactions.size();
            int batchSize = (int) Math.ceil((double) totalSize / numBatches);

            for (int i = 0; i < totalSize; i += batchSize) {
                int end = Math.min(i + batchSize, totalSize);
                List<byte[]> batch = transactions.subList(i, end);
                splitTransactionBatches.add(new ArrayList<>(batch));
            }
            for(List<byte[]> batch: splitTransactionBatches){
                BigInteger gasLimit  = baseGas.add(GAS_PER_TRANSACTION.multiply(BigInteger.valueOf(batch.size())));
                var contractTransactionReceipt = executeMultiCall(credentials,gasPrice, gasLimit, batch);
                credentials = getCredentials();
                var multiCallResponse = MultiCallResponse.builder()
                        .contractTransactionReceipt(contractTransactionReceipt)
                        .transactionCount(batch.size())
                        .build();
                multiCallResponses.add(multiCallResponse);
            }
            return multiCallResponses;
        }
        var contractTransactionReceipt = executeMultiCall(credentials,gasPrice, estimatedGas, transactions);
        var multiCallResponse = MultiCallResponse.builder()
                .contractTransactionReceipt(contractTransactionReceipt)
                .transactionCount(transactions.size())
                .build();
        multiCallResponses.add(multiCallResponse);
        return multiCallResponses;
    }



    public ContractTransactionReceipt executeMultiCall(
            Credentials credentials,
            BigInteger gasPrice,
            BigInteger gasLimit,
            List<byte[]> transactions
    ){

        // Concatenate all calls
        int totalLength = transactions.stream().mapToInt(tx -> tx.length).sum();
        byte[] payload = new byte[totalLength];
        int offset = 0;
        for (byte[] tx : transactions) {
            System.arraycopy(tx, 0, payload, offset, tx.length);
            offset += tx.length;
        }

        var multiSendContract = MultiSendCallOnly.load(multiSendAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
        TransactionReceipt multiSendTransaction;
        try {
            multiSendTransaction = multiSendContract.multiSend(payload, BigInteger.ZERO).send();
        } catch (Exception e) {
            return ContractTransactionReceipt.builder()
                    .contractStatusEnum(ContractStatusEnum.Error)
                    .revertReason(e.getMessage())
                    .build();
        }
        if (multiSendTransaction == null || multiSendTransaction.getStatus().equals("0x0")) {
            return ContractTransactionReceipt.builder()
                    .transactionHash(multiSendTransaction != null ? multiSendTransaction.getTransactionHash() : null)
                    .contractStatusEnum(ContractStatusEnum.Error)
                    .gasUsed(multiSendTransaction != null ? multiSendTransaction.getGasUsed() : null)
                    .revertReason(multiSendTransaction != null ? multiSendTransaction.getRevertReason() : "Unknown failure")
                    .build();
        }

        return ContractTransactionReceipt.builder()
                .transactionHash(multiSendTransaction.getTransactionHash())
                .contractStatusEnum(ContractStatusEnum.Completed)
                .gasUsed(multiSendTransaction.getGasUsed())
                .build();
    }

    private org.web3j.crypto.Credentials getCredentials(){
        return credentialsService.getCredentials();
    }

    private ArrayList<String> getUsersThatCancelledBid(Content content, org.web3j.crypto.Credentials credentials) {
        DefaultGasProvider contractGasProvider = new DefaultGasProvider();
        var bidService = BidService.load(BidServiceAddress, web3j, credentials, contractGasProvider);
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

    private BigInteger getGasPrice(Web3j web3j){
        BigInteger polygonGasStation;
        try {
            polygonGasStation = web3j.ethGasPrice().send().getGasPrice();
        } catch (IOException e) {
            return BigInteger.valueOf(40000000000L);
        }
        return polygonGasStation.multiply(BigInteger.valueOf(12)).divide(BigInteger.valueOf(10));
    }



    @NotNull
    public ArrayList<String> getUsersThatCancelledBid(Content content) {
        DefaultGasProvider contractGasProvider = new DefaultGasProvider();
        var credentials = getCredentials();
        var bidService = BidService.load(BidServiceAddress, web3j, credentials, contractGasProvider);
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

}