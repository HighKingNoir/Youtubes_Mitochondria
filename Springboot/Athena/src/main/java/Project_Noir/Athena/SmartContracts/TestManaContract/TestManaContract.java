package Project_Noir.Athena.SmartContracts.TestManaContract;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/LFDT-web3j/web3j/tree/main/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.7.0.
 */
@SuppressWarnings("rawtypes")
public class TestManaContract extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b506040518060400160405280600c81526020016b506f6c79676f6e204d616e6160a01b815250604051806040016040528060048152602001634d616e6160e01b815250816003908161006291906102b8565b50600461006f82826102b8565b5050506100a6336100846100ab60201b60201c565b61008f90600a610475565b6100a19067016345785d8a000061048b565b6100b0565b6104b5565b601290565b6001600160a01b0382166100df5760405163ec442f0560e01b8152600060048201526024015b60405180910390fd5b6100eb600083836100ef565b5050565b6001600160a01b03831661011a57806002600082825461010f91906104a2565b9091555061018c9050565b6001600160a01b0383166000908152602081905260409020548181101561016d5760405163391434e360e21b81526001600160a01b038516600482015260248101829052604481018390526064016100d6565b6001600160a01b03841660009081526020819052604090209082900390555b6001600160a01b0382166101a8576002805482900390556101c7565b6001600160a01b03821660009081526020819052604090208054820190555b816001600160a01b0316836001600160a01b03167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef8360405161020c91815260200190565b60405180910390a3505050565b634e487b7160e01b600052604160045260246000fd5b600181811c9082168061024357607f821691505b60208210810361026357634e487b7160e01b600052602260045260246000fd5b50919050565b601f8211156102b357806000526020600020601f840160051c810160208510156102905750805b601f840160051c820191505b818110156102b0576000815560010161029c565b50505b505050565b81516001600160401b038111156102d1576102d1610219565b6102e5816102df845461022f565b84610269565b6020601f82116001811461031957600083156103015750848201515b600019600385901b1c1916600184901b1784556102b0565b600084815260208120601f198516915b828110156103495787850151825560209485019460019092019101610329565b50848210156103675786840151600019600387901b60f8161c191681555b50505050600190811b01905550565b634e487b7160e01b600052601160045260246000fd5b6001815b60018411156103c7578085048111156103ab576103ab610376565b60018416156103b957908102905b60019390931c928002610390565b935093915050565b6000826103de5750600161046f565b816103eb5750600061046f565b8160018114610401576002811461040b57610427565b600191505061046f565b60ff84111561041c5761041c610376565b50506001821b61046f565b5060208310610133831016604e8410600b841016171561044a575081810a61046f565b610457600019848461038c565b806000190482111561046b5761046b610376565b0290505b92915050565b600061048460ff8416836103cf565b9392505050565b808202811582820484141761046f5761046f610376565b8082018082111561046f5761046f610376565b610990806104c46000396000f3fe608060405234801561001057600080fd5b50600436106100b45760003560e01c80633950935111610071578063395093511461013857806370a082311461014b57806395d89b4114610174578063a457c2d71461017c578063a9059cbb1461018f578063dd62ed3e146101a257600080fd5b806306fdde03146100b9578063095ea7b3146100d75780631249c58b146100fa57806318160ddd1461010457806323b872dd14610116578063313ce56714610129575b600080fd5b6100c16101db565b6040516100ce91906106b1565b60405180910390f35b6100ea6100e536600461071b565b61026d565b60405190151581526020016100ce565b610102610287565b005b6002545b6040519081526020016100ce565b6100ea610124366004610745565b6102b0565b604051601281526020016100ce565b6100ea61014636600461071b565b6102d4565b610108610159366004610782565b6001600160a01b031660009081526020819052604090205490565b6100c1610327565b6100ea61018a36600461071b565b610336565b6100ea61019d36600461071b565b610375565b6101086101b03660046107a4565b6001600160a01b03918216600090815260016020908152604080832093909416825291909152205490565b6060600380546101ea906107d7565b80601f0160208091040260200160405190810160405280929190818152602001828054610216906107d7565b80156102635780601f1061023857610100808354040283529160200191610263565b820191906000526020600020905b81548152906001019060200180831161024657829003601f168201915b5050505050905090565b60003361027b818585610383565b60019150505b92915050565b6102ae336102976012600a61090e565b6102a99067016345785d8a000061091d565b610395565b565b6000336102be8582856103d4565b6102c9858585610453565b506001949350505050565b3360008181526006602090815260408083206001600160a01b038716845290915281208054919261031e92909186918691908690610313908490610934565b925050819055610383565b50600192915050565b6060600480546101ea906107d7565b3360008181526006602090815260408083206001600160a01b038716845290915281208054919261031e92909186918691908690610313908490610947565b60003361027b818585610453565b61039083838360016104b2565b505050565b6001600160a01b0382166103c45760405163ec442f0560e01b8152600060048201526024015b60405180910390fd5b6103d060008383610587565b5050565b6001600160a01b0383811660009081526001602090815260408083209386168352929052205460001981101561044d578181101561043e57604051637dc7a0d960e11b81526001600160a01b038416600482015260248101829052604481018390526064016103bb565b61044d848484840360006104b2565b50505050565b6001600160a01b03831661047d57604051634b637e8f60e11b8152600060048201526024016103bb565b6001600160a01b0382166104a75760405163ec442f0560e01b8152600060048201526024016103bb565b610390838383610587565b6001600160a01b0384166104dc5760405163e602df0560e01b8152600060048201526024016103bb565b6001600160a01b03831661050657604051634a1406b160e11b8152600060048201526024016103bb565b6001600160a01b038085166000908152600160209081526040808320938716835292905220829055801561044d57826001600160a01b0316846001600160a01b03167f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b9258460405161057991815260200190565b60405180910390a350505050565b6001600160a01b0383166105b25780600260008282546105a79190610934565b909155506106249050565b6001600160a01b038316600090815260208190526040902054818110156106055760405163391434e360e21b81526001600160a01b038516600482015260248101829052604481018390526064016103bb565b6001600160a01b03841660009081526020819052604090209082900390555b6001600160a01b0382166106405760028054829003905561065f565b6001600160a01b03821660009081526020819052604090208054820190555b816001600160a01b0316836001600160a01b03167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef836040516106a491815260200190565b60405180910390a3505050565b602081526000825180602084015260005b818110156106df57602081860181015160408684010152016106c2565b506000604082850101526040601f19601f83011684010191505092915050565b80356001600160a01b038116811461071657600080fd5b919050565b6000806040838503121561072e57600080fd5b610737836106ff565b946020939093013593505050565b60008060006060848603121561075a57600080fd5b610763846106ff565b9250610771602085016106ff565b929592945050506040919091013590565b60006020828403121561079457600080fd5b61079d826106ff565b9392505050565b600080604083850312156107b757600080fd5b6107c0836106ff565b91506107ce602084016106ff565b90509250929050565b600181811c908216806107eb57607f821691505b60208210810361080b57634e487b7160e01b600052602260045260246000fd5b50919050565b634e487b7160e01b600052601160045260246000fd5b6001815b60018411156108625780850481111561084657610846610811565b600184161561085457908102905b60019390931c92800261082b565b935093915050565b60008261087957506001610281565b8161088657506000610281565b816001811461089c57600281146108a6576108c2565b6001915050610281565b60ff8411156108b7576108b7610811565b50506001821b610281565b5060208310610133831016604e8410600b84101617156108e5575081810a610281565b6108f26000198484610827565b806000190482111561090657610906610811565b029392505050565b600061079d60ff84168361086a565b808202811582820484141761028157610281610811565b8082018082111561028157610281610811565b818103818111156102815761028161081156fea2646970667358221220cf319e13afc387a78f2cf5871c79850b0060210b9e7a81342f12faf208e521a564736f6c634300081a0033";

    private static String librariesLinkedBinary;

    public static final String FUNC_ALLOWANCE = "allowance";

    public static final String FUNC_APPROVE = "approve";

    public static final String FUNC_BALANCEOF = "balanceOf";

    public static final String FUNC_DECIMALS = "decimals";

    public static final String FUNC_DECREASEALLOWANCE = "decreaseAllowance";

    public static final String FUNC_INCREASEALLOWANCE = "increaseAllowance";

    public static final String FUNC_MINT = "mint";

    public static final String FUNC_NAME = "name";

    public static final String FUNC_SYMBOL = "symbol";

    public static final String FUNC_TOTALSUPPLY = "totalSupply";

    public static final String FUNC_TRANSFER = "transfer";

    public static final String FUNC_TRANSFERFROM = "transferFrom";

    public static final Event APPROVAL_EVENT = new Event("Approval", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event TRANSFER_EVENT = new Event("Transfer", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    protected TestManaContract(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    protected TestManaContract(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static ApprovalEventResponse getApprovalEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(APPROVAL_EVENT, log);
        ApprovalEventResponse typedResponse = new ApprovalEventResponse();
        typedResponse.log = log;
        typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.spender = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<ApprovalEventResponse> approvalEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getApprovalEventFromLog(log));
    }

    public Flowable<ApprovalEventResponse> approvalEventFlowable(DefaultBlockParameter startBlock,
            DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(APPROVAL_EVENT));
        return approvalEventFlowable(filter);
    }

    public static TransferEventResponse getTransferEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(TRANSFER_EVENT, log);
        TransferEventResponse typedResponse = new TransferEventResponse();
        typedResponse.log = log;
        typedResponse.from = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.to = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.value = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<TransferEventResponse> transferEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getTransferEventFromLog(log));
    }

    public Flowable<TransferEventResponse> transferEventFlowable(DefaultBlockParameter startBlock,
            DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(TRANSFER_EVENT));
        return transferEventFlowable(filter);
    }

    public RemoteFunctionCall<BigInteger> allowance(String owner, String spender) {
        final Function function = new Function(FUNC_ALLOWANCE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, owner), 
                new org.web3j.abi.datatypes.Address(160, spender)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> approve(String spender, BigInteger value) {
        final Function function = new Function(
                FUNC_APPROVE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, spender), 
                new org.web3j.abi.datatypes.generated.Uint256(value)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> balanceOf(String account) {
        final Function function = new Function(FUNC_BALANCEOF, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, account)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> decimals() {
        final Function function = new Function(FUNC_DECIMALS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> decreaseAllowance(String spender,
            BigInteger subtractedValue) {
        final Function function = new Function(
                FUNC_DECREASEALLOWANCE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, spender), 
                new org.web3j.abi.datatypes.generated.Uint256(subtractedValue)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> increaseAllowance(String spender,
            BigInteger addedValue) {
        final Function function = new Function(
                FUNC_INCREASEALLOWANCE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, spender), 
                new org.web3j.abi.datatypes.generated.Uint256(addedValue)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> mint() {
        final Function function = new Function(
                FUNC_MINT, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> name() {
        final Function function = new Function(FUNC_NAME, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> symbol() {
        final Function function = new Function(FUNC_SYMBOL, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> totalSupply() {
        final Function function = new Function(FUNC_TOTALSUPPLY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> transfer(String to, BigInteger value) {
        final Function function = new Function(
                FUNC_TRANSFER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.generated.Uint256(value)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> transferFrom(String from, String to,
            BigInteger value) {
        final Function function = new Function(
                FUNC_TRANSFERFROM, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, from), 
                new org.web3j.abi.datatypes.Address(160, to), 
                new org.web3j.abi.datatypes.generated.Uint256(value)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public static TestManaContract load(String contractAddress, Web3j web3j,
            Credentials credentials, ContractGasProvider contractGasProvider) {
        return new TestManaContract(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static TestManaContract load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new TestManaContract(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<TestManaContract> deploy(Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return deployRemoteCall(TestManaContract.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), "");
    }

    public static RemoteCall<TestManaContract> deploy(Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(TestManaContract.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), "");
    }

    private static String getDeploymentBinary() {
        if (librariesLinkedBinary != null) {
            return librariesLinkedBinary;
        } else {
            return BINARY;
        }
    }

    public static class ApprovalEventResponse extends BaseEventResponse {
        public String owner;

        public String spender;

        public BigInteger value;
    }

    public static class TransferEventResponse extends BaseEventResponse {
        public String from;

        public String to;

        public BigInteger value;
    }
}
