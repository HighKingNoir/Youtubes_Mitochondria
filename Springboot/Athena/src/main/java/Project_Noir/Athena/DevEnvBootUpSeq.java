package Project_Noir.Athena;

import Project_Noir.Athena.SmartContracts.BidService.BidService;
import Project_Noir.Athena.SmartContracts.ChannelService.ChannelService;
import Project_Noir.Athena.SmartContracts.GaslessFunctionCallModule.GaslessFunctionCallModule;
import Project_Noir.Athena.SmartContracts.InterfaceService.InterfaceService;
import Project_Noir.Athena.SmartContracts.MultiSendCallOnly.MultiSendCallOnly;
import Project_Noir.Athena.SmartContracts.TestManaContract.TestManaContract;
import Project_Noir.Athena.SmartContracts.WarChestService.WarChestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.StaticGasProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
@Profile("dev")
public class DevEnvBootUpSeq implements CommandLineRunner {

    @Value("${private.key.company}")
    private String privateKeyCompany;

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

    @Value("${contract.bid.address}")
    private String bidAddress;

    @Value("${contract.channel.address}")
    private String channelAddress;

    @Value("${contract.warchest.address}")
    private String warchestAddress;

    @Value("${contract.multiSend.address}")
    private String multiSendAddress;

    @Value("${contract.interface.address}")
    private String interfaceAddress;

    @Value("${contract.gaslessFunctionCall.address}")
    private String gaslessFunctionCallAddress;

    @Value("${contract.testMana.address}")
    private String testManaAddress;

    private final Web3j web3j = Web3j.build(new HttpService());
    private final TutorialService tutorialService;

    @Override
    public void run(String... args) throws IOException {
        System.out.println("\nSpringBoot Application Boot Up Successful");
        if(areAllContractsValid()){
            System.out.println("Choose an option:");
            System.out.println("1) Deploy smart contracts");
            System.out.println("2) Start tutorial");
            System.out.print("Enter choice: ");
            Scanner scanner = new Scanner(System.in);
            int choice = scanner.nextInt();
            if(choice == 1){
                deployContracts();
            }
            else if (choice == 2) {
                System.out.println("\nStarting tutorial\n");
                tutorialService.tutorial();
            }
            else{
                System.out.println("Not an option");
            }
        }
        else{
            Scanner scanner = new Scanner(System.in);

            System.out.print("Would you like to deploy the smart contracts (Y/n): ");
            String input = scanner.nextLine().trim();
            if (!input.equalsIgnoreCase("y")) {
                System.out.println("Skipping contract deployment.");
                return;
            }
            deployContracts();
        }
    }

    private void deployContracts() {
        if (!isCompanyKeysValid()) {
            System.out.println("Error: Company private key in 'application-dev.properties' is missing or empty!");
            System.out.println("Assign a private key to 'private.key.company' and reload application to try again.");
            return;
        }
        if (!areAllPrivateKeysValid()) {
            System.out.println("Error: One or more private keys in 'application-dev.properties' are missing or empty!");
            System.out.println("Assign missing private key(s) and reload application to try again.");
            return;
        }
        System.out.println("\nDeploying smart contracts...");
        var companyCredentials = Credentials.create(privateKeyCompany);
        BigInteger gasPrice;
        try {
            gasPrice = web3j.ethGasPrice().send().getGasPrice().multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100));;
        } catch (IOException e) {
            gasPrice = BigInteger.valueOf(40000000000L);
        }
        BigInteger gasLimit = BigInteger.valueOf(5000000L);
        List<String> _authorizedAddresses = new ArrayList<>();
        _authorizedAddresses.add(Credentials.create(privateKeyOne).getAddress());
        _authorizedAddresses.add(Credentials.create(privateKeyTwo).getAddress());
        _authorizedAddresses.add(Credentials.create(privateKeyThree).getAddress());
        _authorizedAddresses.add(Credentials.create(privateKeyFour).getAddress());
        _authorizedAddresses.add(Credentials.create(privateKeyFive).getAddress());
        _authorizedAddresses.add(Credentials.create(privateKeySix).getAddress());
        TestManaContract testManaContract;
        try {
            testManaContract = TestManaContract.deploy(web3j, companyCredentials, new StaticGasProvider(gasPrice, gasLimit)).send();
        } catch (Exception e) {
            System.out.println("Error: Failed to load Test Mana Contract");
            return;
        }
        System.out.println("Test Mana Contract deployed.");
        System.out.println("Deploying Bid Service Contract...");
        BidService bidServiceContract;
        try {
            bidServiceContract = BidService.deploy(web3j, companyCredentials, new StaticGasProvider(gasPrice, gasLimit), testManaContract.getContractAddress()).send();
        } catch (Exception e) {
            System.out.println("Error: Failed to deploy Bid Service Contract");
            return;
        }
        System.out.println("Bid Service Contract deployed.");
        System.out.println("Deploying Channel Service Contract...");
        ChannelService channelServiceContract;
        try {
            channelServiceContract = Project_Noir.Athena.SmartContracts.ChannelService.ChannelService.deploy(web3j, companyCredentials, new StaticGasProvider(gasPrice, gasLimit), testManaContract.getContractAddress()).send();
        } catch (Exception e) {
            System.out.println("Error: Failed to deploy Channel Service Contract");
            return;
        }
        System.out.println("Channel Service Contract deployed.");
        System.out.println("Deploying WarChest Service Contract...");
        WarChestService warChestServiceContract;
        try {
            warChestServiceContract = WarChestService.deploy(web3j, companyCredentials, new StaticGasProvider(gasPrice, gasLimit), channelServiceContract.getContractAddress(), testManaContract.getContractAddress()).send();
        } catch (Exception e) {
            System.out.println("Error: Failed to deploy WarChest Service Contract");
            return;
        }
        System.out.println("WarChest Service Contract deployed.");
        System.out.println("Connecting WarChest and Channel Service Contracts...");
        try {
            channelServiceContract.setWarChestServiceAddress(warChestServiceContract.getContractAddress()).send();
        } catch (Exception e) {
            System.out.println("Error: Failed to connect WarChest and Channel Service Contracts");
            return;
        }
        System.out.println("Connection Successful.");
        System.out.println("Deploying Interface Module Contract...");
        InterfaceService interfaceService;
        try {
            interfaceService = InterfaceService.deploy(
                    web3j,
                    companyCredentials,
                    new StaticGasProvider(gasPrice, gasLimit),
                    bidServiceContract.getContractAddress(),
                    channelServiceContract.getContractAddress(),
                    warChestServiceContract.getContractAddress(),
                    _authorizedAddresses
            ).send();
        } catch (Exception e) {
            System.out.println("Error: Failed to deploy Interface Contract");
            return;
        }
        System.out.println("Interface Contract deployed.");
        System.out.println("Deploying MultiCall Contract...");
        MultiSendCallOnly multiSendCallOnly;
        try {
            multiSendCallOnly = MultiSendCallOnly.deploy(
                    web3j,
                    companyCredentials,
                    new StaticGasProvider(gasPrice, gasLimit),
                    _authorizedAddresses
            ).send();
        } catch (Exception e) {
            System.out.println("Error: Failed to deploy MultiCall Contract");
            return;
        }
        System.out.println("MultiCall Contract deployed.");
        System.out.println("Deploying GaslessFunctionCall Contract...");
        GaslessFunctionCallModule gaslessFunctionCallModule;
        try {
            gaslessFunctionCallModule = GaslessFunctionCallModule.deploy(
                    web3j,
                    companyCredentials,
                    new StaticGasProvider(gasPrice, gasLimit),
                    bidServiceContract.getContractAddress(),
                    channelServiceContract.getContractAddress(),
                    testManaContract.getContractAddress(),
                    _authorizedAddresses
            ).send();
        } catch (Exception e) {
            System.out.println("Error: Failed to deploy GaslessFunctionCall Contract");
            return;
        }
        System.out.println("GaslessFunctionCall Contract deployed.");
        System.out.println("Completing Final Step...");
        try {
            bidServiceContract.addOverseer(interfaceService.getContractAddress()).send();
            channelServiceContract.addOverseer(interfaceService.getContractAddress()).send();
            warChestServiceContract.addOverseer(interfaceService.getContractAddress()).send();
            interfaceService.addAuthorizedAddress(multiSendCallOnly.getContractAddress()).send();
        } catch (Exception e){
            System.out.println("Error: Final Step Failed");
            return;
        }
        System.out.println("All Contracts Successfully Deployed.");
        System.out.println(dashedLine());
        System.out.println("Copy Into 'application-dev.properties'");
        System.out.println("contract.bid.address=" + bidServiceContract.getContractAddress());
        System.out.println("contract.channel.address=" + channelServiceContract.getContractAddress());
        System.out.println("contract.warchest.address=" + warChestServiceContract.getContractAddress());
        System.out.println("contract.multiSend.address=" + multiSendCallOnly.getContractAddress());
        System.out.println("contract.interface.address=" + interfaceService.getContractAddress());
        System.out.println("contract.gaslessFunctionCall.address=" + gaslessFunctionCallModule.getContractAddress());
        System.out.println("contract.testMana.address=" + testManaContract.getContractAddress() + "\n");
        System.out.println(dashedLine());
        System.out.println("Copy Into Angular 'environment.ts' file");
        System.out.println("    Contract_Bid_Address: " + "'" + bidServiceContract.getContractAddress() + "',");
        System.out.println("    Contract_Channel_Address: " + "'" + channelServiceContract.getContractAddress() + "',");
        System.out.println("    Contract_Warchest_Address: " + "'" + warChestServiceContract.getContractAddress() + "',");
        System.out.println("    Contract_Decentraland_Mana_Address: " + "'" + testManaContract.getContractAddress() + "',");
        System.out.println("    Contract_Gasless_Function_Call_Address: " + "'" + gaslessFunctionCallModule.getContractAddress() + "',\n");
        System.out.println(dashedLine());
        System.out.println("Rerun Springboot Application with 'application-dev.properties' changes");
    }

    private boolean isCompanyKeysValid() {
        return privateKeyCompany != null && !privateKeyCompany.isEmpty();
    }

    private boolean areAllPrivateKeysValid() {
        return privateKeyOne != null && !privateKeyOne.isEmpty() &&
                privateKeyTwo != null && !privateKeyTwo.isEmpty() &&
                privateKeyThree != null && !privateKeyThree.isEmpty() &&
                privateKeyFour != null && !privateKeyFour.isEmpty() &&
                privateKeyFive != null && !privateKeyFive.isEmpty() &&
                privateKeySix != null && !privateKeySix.isEmpty();
    }

    private boolean areAllContractsValid() {
        return bidAddress != null && !bidAddress.isEmpty() &&
                channelAddress != null && !channelAddress.isEmpty() &&
                warchestAddress != null && !warchestAddress.isEmpty() &&
                multiSendAddress != null && !multiSendAddress.isEmpty() &&
                interfaceAddress != null && !interfaceAddress.isEmpty() &&
                testManaAddress != null && !testManaAddress.isEmpty() &&
                gaslessFunctionCallAddress != null && !gaslessFunctionCallAddress.isEmpty();
    }



    private String dashedLine()
    {
        StringBuilder sb = new StringBuilder(20);
        for(int n = 0; n < 40; ++n)
            sb.append('-');
        sb.append(System.lineSeparator());
        return sb.toString();
    }


}
