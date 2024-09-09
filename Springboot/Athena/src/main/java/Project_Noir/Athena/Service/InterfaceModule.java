package Project_Noir.Athena.Service;

import Project_Noir.Athena.Model.ContractStatusEnum;
import Project_Noir.Athena.Model.ContractTransactionReceipt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class InterfaceModule {

    @Value("${interface.module.address}")
    private String interfaceModuleAddress;

    @Value("${private.key.one}")
    private String privateKeyOne;

    @Value("${private.key.two}")
    private String privateKeyTwo;

    @Value("${private.key.three}")
    private String privateKeyThree;

    @Value("${private.key.four}")
    private String privateKeyFour;

    @Value("${private.key.five}")
    private String privateKeyFive;

    @Value("${private.key.six}")
    private String privateKeySix;

    @Value("${spring.profiles.active}")
    private String environment;

    private Integer selectedPrivateKey = 1;

    // @dev Calls the Community Service Contract
    public ContractTransactionReceipt callInterfaceModule(
            Web3j web3j,
            String functionName,
            BigInteger gasLimit,
            List parameters,
            List output) {


        Credentials credentials;
        switch (selectedPrivateKey){
            case 1 -> {
                credentials = Credentials.create(privateKeyOne);
                selectedPrivateKey++;
            }
            case 2 -> {
                credentials = Credentials.create(privateKeyTwo);
                selectedPrivateKey++;
            }
            case 3 -> {
                credentials = Credentials.create(privateKeyThree);
                selectedPrivateKey++;
            }
            case 4 -> {
                credentials = Credentials.create(privateKeyFour);
                selectedPrivateKey++;
            }
            case 5 -> {
                credentials = Credentials.create(privateKeyFive);
                selectedPrivateKey++;
            }
            default -> {
                credentials = Credentials.create(privateKeySix);
                selectedPrivateKey = 1;
            }
        }



        Function function = new Function(functionName, parameters, output);
        String encodedFunction = FunctionEncoder.encode(function);

        BigInteger gasPrice = getGasPrice(web3j);

        long chainID;
        if (environment.equals("dev")) {
            chainID = 1337L;
            gasPrice = BigInteger.valueOf(20000000000L);
            gasLimit = BigInteger.valueOf(6721975L);
        } else {
            chainID = 137L;
        }

        org.web3j.protocol.core.methods.response.EthSendTransaction transactionResponse = getTransactionResponse(
                web3j,
                credentials,
                gasPrice,
                gasLimit,
                encodedFunction,
                chainID
        );
        if(transactionResponse == null){
            return ContractTransactionReceipt.builder()
                    .contractStatusEnum(ContractStatusEnum.Error)
                    .build();
        }

        TransactionReceiptProcessor receiptProcessor = new PollingTransactionReceiptProcessor(web3j, 1000, 20);
        String transactionHash = transactionResponse.getTransactionHash();
        TransactionReceipt receipt = null;
        if(transactionHash != null){
            try {
                receipt = receiptProcessor.waitForTransactionReceipt(transactionHash);
            } catch (IOException | TransactionException ignored) {

            }
        }
        if (receipt == null && transactionHash == null) {
            return ContractTransactionReceipt.builder()
                    .contractStatusEnum(ContractStatusEnum.Error)
                    .build();
        }
        if (receipt != null && receipt.getStatus().equals("0x0")) {
            if (receipt.getRevertReason() != null && !receipt.getRevertReason().isEmpty()) {
                // The transaction failed, and there is a custom revert reason
                return ContractTransactionReceipt.builder()
                        .transactionHash(receipt.getTransactionHash())
                        .contractStatusEnum(ContractStatusEnum.Error)
                        .gasUsed(receipt.getGasUsed())
                        .revertReason(receipt.getRevertReason())
                        .build();
            } else {
                // The transaction failed, but no custom reason provided
                return ContractTransactionReceipt.builder()
                        .transactionHash(receipt.getTransactionHash())
                        .contractStatusEnum(ContractStatusEnum.Error)
                        .gasUsed(receipt.getGasUsed())
                        .revertReason(null)
                        .build();
            }
        }
        assert receipt != null;
        return ContractTransactionReceipt.builder()
                .transactionHash(transactionHash)
                .contractStatusEnum(ContractStatusEnum.Completed)
                .gasUsed(receipt.getGasUsed())
                .build();
    }

    private EthSendTransaction getTransactionResponse(
            Web3j web3j,
            Credentials credentials,
            BigInteger gasPrice,
            BigInteger gasLimit,
            String encodedFunction,
            long chainID
    ){
        RawTransaction rawTransaction;
        try {
            rawTransaction = RawTransaction.createTransaction(
                    web3j.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST).send().getTransactionCount(),
                    gasPrice,
                    gasLimit,
                    interfaceModuleAddress,
                    encodedFunction);
        } catch (IOException e) {
            return null;
        }

        RawTransactionManager rawTransactionManager = new RawTransactionManager(web3j, credentials, chainID);

        try {
            return rawTransactionManager.signAndSend(rawTransaction);
        } catch (IOException e) {
            return null;
        }
    }

    private BigInteger getGasPrice(Web3j web3j){
        BigInteger polygonGasStation;
        try {
            polygonGasStation = web3j.ethGasPrice().send().getGasPrice();
        } catch (IOException e) {
            return BigInteger.valueOf(60000000000L);
        }
        BigInteger levelOne = BigInteger.valueOf(40000000000L);
        BigInteger levelTwo = BigInteger.valueOf(60000000000L);
        BigInteger levelThree = BigInteger.valueOf(90000000000L);
        BigInteger levelFour = BigInteger.valueOf(120000000000L);

        if(polygonGasStation.compareTo(levelOne) < 0){
            return polygonGasStation.add(BigInteger.valueOf(2000000000L));
        }
        else if(polygonGasStation.compareTo(levelTwo) < 0){
            return polygonGasStation.add(BigInteger.valueOf(4000000000L));
        }

        else if(polygonGasStation.compareTo(levelThree) < 0){
            return polygonGasStation.add(BigInteger.valueOf(6000000000L));
        }
        else if(polygonGasStation.compareTo(levelFour) < 0){
            return polygonGasStation.add(BigInteger.valueOf(8000000000L));
        }
        else{
            return polygonGasStation.add(BigInteger.valueOf(10000000000L));
        }
    }
}
