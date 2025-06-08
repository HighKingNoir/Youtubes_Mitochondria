package Project_Noir.Athena.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CredentialsService {
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

    public org.web3j.crypto.Credentials getCredentials(){
        switch (selectedPrivateKey){
            case 1 -> {
                selectedPrivateKey++;
                return org.web3j.crypto.Credentials.create(privateKeyOne);
            }
            case 2 -> {
                selectedPrivateKey++;
                return org.web3j.crypto.Credentials.create(privateKeyTwo);
            }
            case 3 -> {
                selectedPrivateKey++;
                return org.web3j.crypto.Credentials.create(privateKeyThree);
            }
            case 4 -> {
                selectedPrivateKey++;
                return org.web3j.crypto.Credentials.create(privateKeyFour);

            }
            case 5 -> {
                selectedPrivateKey++;
                return org.web3j.crypto.Credentials.create(privateKeyFive);
            }
            default -> {
                selectedPrivateKey = 1;
                return org.web3j.crypto.Credentials.create(privateKeySix);
            }
        }
    }
}
