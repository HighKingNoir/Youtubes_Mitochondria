package Project_Noir.Athena.Service;

import Project_Noir.Athena.Controller.ServerSideEventController;
import Project_Noir.Athena.DTO.BidPaymentRequest;
import Project_Noir.Athena.DTO.FundChannelRequest;
import Project_Noir.Athena.DTO.GaslessFunctionCallRequest;
import Project_Noir.Athena.Exception.SivantisException;
import Project_Noir.Athena.SmartContracts.GaslessFunctionCallModule.GaslessFunctionCallModule;
import Project_Noir.Athena.SmartContracts.InterfaceService.InterfaceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class GaslessFunctionCallService {

    private final PaymentService paymentService;
    private final MessageService messageService;
    private final ServerSideEventController serverSideEventController;
    private final CredentialsService credentialsService;

    @Value("${contract.gaslessFunctionCall.address}")
    private String gaslessFunctionCallAddress;

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

    public void gaslessFundChannel(GaslessFunctionCallRequest gaslessFunctionCallRequest, String Jwt) {
        validateDeadline(gaslessFunctionCallRequest.getDeadline());
        var signatureParts = parseSignature(gaslessFunctionCallRequest.getSignature());
        TransactionReceipt transactionReceipt;
        try {
            transactionReceipt = loadGaslessFunctionCallModule().gaslessFundChannel(
                    gaslessFunctionCallRequest.getChannelName(),
                    gaslessFunctionCallRequest.getSender(),
                    gaslessFunctionCallRequest.getAmount(),
                    gaslessFunctionCallRequest.getFee(),
                    gaslessFunctionCallRequest.getDeadline(),
                    signatureParts.v,
                    signatureParts.r,
                    signatureParts.s
            ).send();
        } catch (Exception e) {
            throw new SivantisException("Transaction Failed");
        }
        var manaWeiAmount = gaslessFunctionCallRequest.getAmount();
        BigDecimal manaEtherAmount = Convert.fromWei(new BigDecimal(manaWeiAmount), Convert.Unit.ETHER);
        var fundChannelRequest = FundChannelRequest.builder()
                .channelName(gaslessFunctionCallRequest.getChannelName())
                .manaAmount(manaEtherAmount.toPlainString())
                .transactionHash(transactionReceipt.getTransactionHash())
                .build();
        messageService.fundChannelMessage(fundChannelRequest, Jwt);
    }

    public void gaslessPlaceBid(GaslessFunctionCallRequest gaslessFunctionCallRequest, String Jwt){
        validateDeadline(gaslessFunctionCallRequest.getDeadline());
        var signatureParts = parseSignature(gaslessFunctionCallRequest.getSignature());
        TransactionReceipt transactionReceipt;
        try {
            transactionReceipt = loadGaslessFunctionCallModule().gaslessPlaceBid(
                    gaslessFunctionCallRequest.getContentID(),
                    gaslessFunctionCallRequest.getUserID(),
                    gaslessFunctionCallRequest.getSender(),
                    gaslessFunctionCallRequest.getAmount(),
                    gaslessFunctionCallRequest.getFee(),
                    gaslessFunctionCallRequest.getDeadline(),
                    signatureParts.v,
                    signatureParts.r,
                    signatureParts.s
            ).send();
        } catch (Exception e) {
            throw new SivantisException("Transaction Failed");
        }
        var manaPrice = serverSideEventController.latestValue;
        var manaWeiAmount = gaslessFunctionCallRequest.getAmount();
        BigDecimal manaPriceDecimal = BigDecimal.valueOf(manaPrice);
        BigDecimal manaEtherAmount = Convert.fromWei(new BigDecimal(manaWeiAmount), Convert.Unit.ETHER);
        BigDecimal dollarAmount = manaEtherAmount.multiply(manaPriceDecimal);
        var bidPaymentRequest = BidPaymentRequest.builder()
                .transactionHash(transactionReceipt.getTransactionHash())
                .contentID(gaslessFunctionCallRequest.getContentID())
                .dollarAmount(dollarAmount)
                .manaAmount(manaEtherAmount.toPlainString())
                .build();
        paymentService.purchaseContent(bidPaymentRequest, Jwt);
    }

    public void gaslessRaiseBid(GaslessFunctionCallRequest gaslessFunctionCallRequest, String Jwt){
        validateDeadline(gaslessFunctionCallRequest.getDeadline());
        var signatureParts = parseSignature(gaslessFunctionCallRequest.getSignature());
        TransactionReceipt transactionReceipt;
        try {
            transactionReceipt = loadGaslessFunctionCallModule().gaslessRaiseBid(
                    gaslessFunctionCallRequest.getContentID(),
                    gaslessFunctionCallRequest.getUserID(),
                    gaslessFunctionCallRequest.getSender(),
                    gaslessFunctionCallRequest.getAmount(),
                    gaslessFunctionCallRequest.getFee(),
                    gaslessFunctionCallRequest.getDeadline(),
                    signatureParts.v,
                    signatureParts.r,
                    signatureParts.s
            ).send();
        } catch (Exception e) {
            throw new SivantisException("Transaction Failed");
        }
        var manaPrice = serverSideEventController.latestValue;
        var manaWeiAmount = gaslessFunctionCallRequest.getAmount();
        BigDecimal manaPriceDecimal = BigDecimal.valueOf(manaPrice);
        BigDecimal manaEtherAmount = Convert.fromWei(new BigDecimal(manaWeiAmount), Convert.Unit.ETHER);
        BigDecimal dollarAmount = manaEtherAmount.multiply(manaPriceDecimal);
        var bidPaymentRequest = BidPaymentRequest.builder()
                .transactionHash(transactionReceipt.getTransactionHash())
                .contentID(gaslessFunctionCallRequest.getContentID())
                .dollarAmount(dollarAmount)
                .manaAmount(manaEtherAmount.toPlainString())
                .build();
        paymentService.updatePurchasedContent(bidPaymentRequest, Jwt);
    }

    public void gaslessCancelBid(GaslessFunctionCallRequest gaslessFunctionCallRequest, String Jwt){
        validateDeadline(gaslessFunctionCallRequest.getDeadline());
        var signatureParts = parseSignature(gaslessFunctionCallRequest.getSignature());
        TransactionReceipt transactionReceipt;
        try {
            transactionReceipt = loadGaslessFunctionCallModule().gaslessCancelBid(
                    gaslessFunctionCallRequest.getContentID(),
                    gaslessFunctionCallRequest.getUserID(),
                    gaslessFunctionCallRequest.getSender(),
                    gaslessFunctionCallRequest.getFee(),
                    gaslessFunctionCallRequest.getDeadline(),
                    signatureParts.v,
                    signatureParts.r,
                    signatureParts.s
            ).send();
        } catch (Exception e) {
            throw new SivantisException("Transaction Failed");
        }
        var manaPrice = serverSideEventController.latestValue;
        var manaWeiAmount = gaslessFunctionCallRequest.getAmount();
        BigDecimal manaPriceDecimal = BigDecimal.valueOf(manaPrice);
        BigDecimal manaEtherAmount = Convert.fromWei(new BigDecimal(manaWeiAmount), Convert.Unit.ETHER);
        double dollarAmount = manaEtherAmount.multiply(manaPriceDecimal).doubleValue();
        paymentService.refundPurchasedContent(gaslessFunctionCallRequest.getUserID(), gaslessFunctionCallRequest.getContentID(), transactionReceipt.getTransactionHash());
    }

    private GaslessFunctionCallModule loadGaslessFunctionCallModule(){
        var credentials = getCredentials();
        return GaslessFunctionCallModule.load(gaslessFunctionCallAddress, web3j, credentials, new StaticGasProvider(getGasPrice(web3j), BigInteger.valueOf(500000L)));
    }

    private void validateDeadline(BigInteger deadline) {
        long currentTime = Instant.now().getEpochSecond(); // current time in seconds

        if (deadline.longValue() < currentTime) {
            throw new SivantisException("Deadline has already passed");
        }
    }

    private org.web3j.crypto.Credentials getCredentials(){
        return credentialsService.getCredentials();
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

    private static SignatureParts parseSignature(String signatureHex) {
        byte[] signatureBytes = Numeric.hexStringToByteArray(signatureHex);

        if (signatureBytes.length != 65) {
            throw new IllegalArgumentException("Invalid signature length: expected 65 bytes");
        }

        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(signatureBytes, 0, r, 0, 32);
        System.arraycopy(signatureBytes, 32, s, 0, 32);
        byte vByte = signatureBytes[64];
        BigInteger v = BigInteger.valueOf((vByte < 27) ? vByte + 27 : vByte);

        return new SignatureParts(v, r, s);
    }

    private record SignatureParts(BigInteger v, byte[] r, byte[] s) { }
}
