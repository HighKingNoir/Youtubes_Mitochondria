package Project_Noir.Athena.SmartContracts.MultiSendCallOnly;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
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
public class MultiSendCallOnly extends Contract {
    public static final String BINARY = "60a060405234801561001057600080fd5b5060405161082138038061082183398101604081905261002f916100f9565b33608052805161004690600090602084019061004d565b50506101c8565b8280548282559060005260206000209081019282156100a2579160200282015b828111156100a257825182546001600160a01b0319166001600160a01b0390911617825560209092019160019091019061006d565b506100ae9291506100b2565b5090565b5b808211156100ae57600081556001016100b3565b634e487b7160e01b600052604160045260246000fd5b80516001600160a01b03811681146100f457600080fd5b919050565b60006020828403121561010b57600080fd5b81516001600160401b0381111561012157600080fd5b8201601f8101841361013257600080fd5b80516001600160401b0381111561014b5761014b6100c7565b604051600582901b90603f8201601f191681016001600160401b0381118282101715610179576101796100c7565b60405291825260208184018101929081018784111561019757600080fd5b6020850194505b838510156101bd576101af856100dd565b81526020948501940161019e565b509695505050505050565b6080516106386101e96000396000818160e0015261018001526106386000f3fe60806040526004361061003f5760003560e01c806342f1181e1461004457806370712939146100665780638d80ff0a146100865780639587463114610099575b600080fd5b34801561005057600080fd5b5061006461005f36600461044d565b6100d5565b005b34801561007257600080fd5b5061006461008136600461044d565b610175565b610064610094366004610493565b61030f565b3480156100a557600080fd5b506100b96100b436600461054c565b610423565b6040516001600160a01b03909116815260200160405180910390f35b336001600160a01b037f000000000000000000000000000000000000000000000000000000000000000016146101265760405162461bcd60e51b815260040161011d90610565565b60405180910390fd5b600080546001810182559080527f290decd9548b62a8d60345a988386fc84ba6bc95484008f6362f93160ef3e5630180546001600160a01b0319166001600160a01b0392909216919091179055565b336001600160a01b037f000000000000000000000000000000000000000000000000000000000000000016146101bd5760405162461bcd60e51b815260040161011d90610565565b60005b6000548110156102b157816001600160a01b0316600082815481106101e7576101e76105af565b6000918252602090912001546001600160a01b0316036102a95760008054610211906001906105c5565b81548110610221576102216105af565b600091825260208220015481546001600160a01b0390911691908390811061024b5761024b6105af565b6000918252602082200180546001600160a01b0319166001600160a01b039390931692909217909155805480610283576102836105ec565b600082815260209020810160001990810180546001600160a01b03191690550190555050565b6001016101c0565b5060405162461bcd60e51b815260206004820152602c60248201527f4164647265737320656e7465726564206973206e6f7420616e20417574686f7260448201526b697a6564204164647265737360a01b606482015260840161011d565b6000805b6000548110156103db57336001600160a01b03166000828154811061033a5761033a6105af565b6000918252602090912001546001600160a01b0316036103d357825160205b818110156103c8578085015160f81c6001820186015160601c60158301870151603584018801516055850189016000856000811461039e576001811461003f576103aa565b6000808585888a5af191505b50806103b557600080fd5b5050806055018501945050505050610359565b5050600191506103db565b600101610313565b508061041f5760405162461bcd60e51b8152602060048201526013602482015272556e617574686f72697a65642041636365737360681b604482015260640161011d565b5050565b6000818154811061043357600080fd5b6000918252602090912001546001600160a01b0316905081565b60006020828403121561045f57600080fd5b81356001600160a01b038116811461047657600080fd5b9392505050565b634e487b7160e01b600052604160045260246000fd5b6000602082840312156104a557600080fd5b813567ffffffffffffffff8111156104bc57600080fd5b8201601f810184136104cd57600080fd5b803567ffffffffffffffff8111156104e7576104e761047d565b604051601f8201601f19908116603f0116810167ffffffffffffffff811182821017156105165761051661047d565b60405281815282820160200186101561052e57600080fd5b81602084016020830137600091810160200191909152949350505050565b60006020828403121561055e57600080fd5b5035919050565b6020808252602a908201527f43616c6c6572206d7573742062652066726f6d20436f6d70616e792057616c6c6040820152696574206164647265737360b01b606082015260800190565b634e487b7160e01b600052603260045260246000fd5b818103818111156105e657634e487b7160e01b600052601160045260246000fd5b92915050565b634e487b7160e01b600052603160045260246000fdfea2646970667358221220a60f257e104cddaf4a790759934f5b629f4ca9f7f730ca3185178c5084dea6ef64736f6c634300081a0033";

    private static String librariesLinkedBinary;

    public static final String FUNC_ADDAUTHORIZEDADDRESS = "addAuthorizedAddress";

    public static final String FUNC_AUTHORIZEDADDRESSES = "authorizedAddresses";

    public static final String FUNC_MULTISEND = "multiSend";

    public static final String FUNC_REMOVEAUTHORIZEDADDRESS = "removeAuthorizedAddress";

    protected MultiSendCallOnly(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    protected MultiSendCallOnly(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<TransactionReceipt> addAuthorizedAddress(String newOwner) {
        final Function function = new Function(
                FUNC_ADDAUTHORIZEDADDRESS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, newOwner)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> authorizedAddresses(BigInteger param0) {
        final Function function = new Function(FUNC_AUTHORIZEDADDRESSES, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> multiSend(byte[] transactions,
            BigInteger weiValue) {
        final Function function = new Function(
                FUNC_MULTISEND, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicBytes(transactions)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteFunctionCall<TransactionReceipt> removeAuthorizedAddress(
            String _authorizedAddresses) {
        final Function function = new Function(
                FUNC_REMOVEAUTHORIZEDADDRESS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _authorizedAddresses)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public static MultiSendCallOnly load(String contractAddress, Web3j web3j,
            Credentials credentials, ContractGasProvider contractGasProvider) {
        return new MultiSendCallOnly(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static MultiSendCallOnly load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new MultiSendCallOnly(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<MultiSendCallOnly> deploy(Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider, List<String> _authorizedAddresses) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.datatypes.Address.class,
                        org.web3j.abi.Utils.typeMap(_authorizedAddresses, org.web3j.abi.datatypes.Address.class))));
        return deployRemoteCall(MultiSendCallOnly.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    public static RemoteCall<MultiSendCallOnly> deploy(Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider,
            List<String> _authorizedAddresses) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.datatypes.Address.class,
                        org.web3j.abi.Utils.typeMap(_authorizedAddresses, org.web3j.abi.datatypes.Address.class))));
        return deployRemoteCall(MultiSendCallOnly.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    private static String getDeploymentBinary() {
        if (librariesLinkedBinary != null) {
            return librariesLinkedBinary;
        } else {
            return BINARY;
        }
    }
}
