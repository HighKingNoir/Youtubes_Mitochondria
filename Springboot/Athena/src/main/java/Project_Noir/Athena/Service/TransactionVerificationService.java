package Project_Noir.Athena.Service;

import Project_Noir.Athena.Controller.ServerSideEventController;
import Project_Noir.Athena.Model.BlockchainInteractionStatusEnum;
import Project_Noir.Athena.Model.TransactionVerificationPackage;
import Project_Noir.Athena.Repo.PaymentRepository;
import Project_Noir.Athena.Repo.TransactionVerificationPackageRepository;
import Project_Noir.Athena.SmartContracts.BidService.BidService;
import Project_Noir.Athena.SmartContracts.PayToRankUpService.PayToRankUpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionVerificationService {

    private final Web3JService web3JService;
    private final TransactionVerificationPackageRepository transactionVerificationPackageRepository;
    private final ServerSideEventController serverSideEventController;
    private final Ec2InstanceTagService ec2InstanceTagService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @Value("${contract.bid.address}")
    private String BidServiceAddress;

    @Value("${contract.payToRankUp.address}")
    private String PayToRankUpServiceAddress;

    @Scheduled(fixedDelayString = "${txVerification.scheduler.delay}")
    @SchedulerLock(name = "verifyTransactionsLock", lockAtLeastFor = "PT10S", lockAtMostFor = "PT40S")
    public void verifyTransactions(){
        var allTransactionVerificationPackages = transactionVerificationPackageRepository.findAllByStatus(BlockchainInteractionStatusEnum.Awaiting);
        if(allTransactionVerificationPackages.isEmpty()){
            return;
        }
        ec2InstanceTagService.markTransactionInProgress();

        var bidServiceOutput = new org.web3j.abi.datatypes.Function("",
                List.of(),
                List.of(
                        new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.Address>() {},
                        new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.Utf8String>() {},
                        new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {}
                )
        );

        var payToRankUpOutput = new org.web3j.abi.datatypes.Function("",
                List.of(),
                List.of(new TypeReference<Bool>() {})
        );

        var web3j = web3JService.web3j;
        DefaultGasProvider contractGasProvider = new DefaultGasProvider();
        var bidService = BidService.load(BidServiceAddress, web3j, new ReadonlyTransactionManager(web3j, BidServiceAddress), contractGasProvider);
        var payToRankUpService = PayToRankUpService.load(PayToRankUpServiceAddress, web3j, new ReadonlyTransactionManager(web3j, PayToRankUpServiceAddress), contractGasProvider);
        for (TransactionVerificationPackage transactionVerificationPackage: allTransactionVerificationPackages){
            transactionVerificationPackage.setStatus(BlockchainInteractionStatusEnum.InProgress);
        }
        transactionVerificationPackageRepository.saveAll(allTransactionVerificationPackages);
        ArrayList<TransactionVerificationPackage> verifiedTransactions = new ArrayList<>();
        try {
            for (TransactionVerificationPackage transactionVerificationPackage: allTransactionVerificationPackages){
                // 1) Wait for the tx
                var processor = new PollingTransactionReceiptProcessor(web3j, 250, 20);
                TransactionReceipt receipt;
                try {
                    receipt = processor.waitForTransactionReceipt(transactionVerificationPackage.getTransactionHash());
                } catch (IOException e) {
                    invalidTransaction("Network error: " + e, transactionVerificationPackage);
                    continue;
                } catch (TransactionException e) {
                    invalidTransaction("Transaction reverted: " + e, transactionVerificationPackage);
                    continue;
                }

                if (!receipt.isStatusOK()) {
                    invalidTransaction("Transaction reverted", transactionVerificationPackage);
                    continue;
                }

                // 2) Check the transaction destination
                Optional<Transaction> optionalTransaction;
                try {
                    optionalTransaction = web3j.ethGetTransactionByHash(transactionVerificationPackage.getTransactionHash()).send().getTransaction();
                } catch (IOException e) {
                    invalidTransaction("Network error: " + e, transactionVerificationPackage);
                    continue;
                }
                if (optionalTransaction.isEmpty()) {
                    invalidTransaction("Transaction Not Found", transactionVerificationPackage);
                    continue;
                };
                var transaction = optionalTransaction.get();
                String contractAddress;
                String encodedFunction;

                java.util.List<org.web3j.abi.TypeReference<org.web3j.abi.datatypes.Type>> outputParameters;
                switch (transactionVerificationPackage.getTransactionVerificationFunctionEnum()){
                    case ArchonPass -> {
                        contractAddress = PayToRankUpServiceAddress;
                        encodedFunction = payToRankUpService.hasPaidArchonPassList(transactionVerificationPackage.getUserId()).encodeFunctionCall();
                        outputParameters = payToRankUpOutput.getOutputParameters();
                    }
                    case MasterPass -> {
                        contractAddress = PayToRankUpServiceAddress;
                        encodedFunction = payToRankUpService.hasPaidMasterPassList(transactionVerificationPackage.getUserId()).encodeFunctionCall();
                        outputParameters = payToRankUpOutput.getOutputParameters();
                    }
                    default -> {
                        contractAddress = BidServiceAddress;
                        encodedFunction = bidService.findBidByContentIDAndUserID(transactionVerificationPackage.getContentID(), transactionVerificationPackage.getUserId()).encodeFunctionCall();
                        outputParameters = bidServiceOutput.getOutputParameters();
                    }
                }
                if (!contractAddress.equalsIgnoreCase(transaction.getTo())) {
                    invalidTransaction("Transaction 'to' != " + contractAddress, transactionVerificationPackage);
                    continue;
                }

                // 3) Read mapping AT THE RECEIPT BLOCK
                var blockParam = new DefaultBlockParameterNumber(receipt.getBlockNumber());
                EthCall call;
                try {
                     call = web3j.ethCall(
                            org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                                    null, contractAddress, encodedFunction),
                            blockParam
                    ).send();
                } catch (IOException e) {
                    invalidTransaction("Network error: " + e, transactionVerificationPackage);
                    continue;
                }
                var outputs = FunctionReturnDecoder.decode(call.getValue(), outputParameters);
                if(!validOutput(outputs, transactionVerificationPackage)){
                    continue;
                };
                verifiedTransactions.add(transactionVerificationPackage);
            }
            paymentService.verifiedTransactions(verifiedTransactions);
            transactionVerificationPackageRepository.deleteAll(allTransactionVerificationPackages);
        } finally {
            ec2InstanceTagService.clearTransactionTag();
        }
    }

    private Boolean validOutput(List<Type> outputs, TransactionVerificationPackage transactionVerificationPackage) {
        switch (transactionVerificationPackage.getTransactionVerificationFunctionEnum()){
            case RaiseBid, PlaceBid -> {
                String userIdOnChain = ((Utf8String) outputs.get(1)).getValue();
                BigInteger manaAmount = ((Uint256) outputs.get(2)).getValue();
                if(!transactionVerificationPackage.getUserId().equals(userIdOnChain)){
                    invalidTransaction("UserId Mismatch", transactionVerificationPackage);
                    return false;
                }
                if(transactionVerificationPackage.getExpectedManaAmount().equals(BigInteger.ZERO)){
                    invalidTransaction("Mana Amount Mismatch.", transactionVerificationPackage);
                    return false;
                }
                if(!transactionVerificationPackage.getExpectedManaAmount().equals(manaAmount)){
                    BigInteger diff = transactionVerificationPackage.getExpectedManaAmount().subtract(manaAmount).abs();
                    if (diff.multiply(BigInteger.valueOf(10_000)).compareTo(transactionVerificationPackage.getExpectedManaAmount()) > 0) {
                        var contentId = transactionVerificationPackage.getContentID();
                        var userID = transactionVerificationPackage.getUserId();
                        var Payment = paymentService.findPayment(userID, contentId);
                        if(Payment != null){
                            var newManaAmountInEther = Convert.fromWei(new BigDecimal(manaAmount), Convert.Unit.ETHER);
                            var previousDollarAmount = Payment.getDollarAmount();
                            var newDollarAmount = newManaAmountInEther.multiply(new BigDecimal(serverSideEventController.latestValue));
                            var dollarAmountDifference = newDollarAmount.subtract(previousDollarAmount);
                            Payment.setManaAmount(newManaAmountInEther.toPlainString());
                            Payment.setDollarAmount(newDollarAmount);
                            Payment.setManaToCreator(newManaAmountInEther.doubleValue() * .9);
                            paymentService.addToListOfBuyersAndHype(contentId, userID, Payment.getManaAmount(), dollarAmountDifference, paymentService.auctionModifier);
                            paymentRepository.save(Payment);
                        }
                    }
                }
            }
            case MasterPass, ArchonPass -> {
                boolean hasPaid = ((Bool) outputs.get(0)).getValue();
                if(!hasPaid){
                    invalidTransaction("Has not paid.", transactionVerificationPackage);
                    return false;
                }
            }
        }
        return true;
    }


    private void invalidTransaction(String reason, TransactionVerificationPackage transactionVerificationPackage){
        paymentService.invalidTransactionHash(reason, transactionVerificationPackage);
    }
}
