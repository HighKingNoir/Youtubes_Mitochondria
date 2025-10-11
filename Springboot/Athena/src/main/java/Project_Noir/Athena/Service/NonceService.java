package Project_Noir.Athena.Service;

import Project_Noir.Athena.Model.NonceRecord;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class NonceService {

    private static final int MAX_DRIFT = 5; // number of transactions
    private final MongoTemplate mongoTemplate;
    private final Web3JService web3JService;
    private final Environment environment;
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



    /**
     * Atomically gets and increments the nonce for a given address.
     */
    public BigInteger getNextNonce(String address) {
        String activeProfile = environment.getProperty("spring.profiles.active");
        address = address.toLowerCase();

        Query query = new Query(Criteria.where("_id").is(address));
        Update update = new Update()
                .inc("nonce", 1)
                .set("updatedAt", Instant.now());

        FindAndModifyOptions options = new FindAndModifyOptions()
                .upsert(false)
                .returnNew(true);

        NonceRecord updated = mongoTemplate.findAndModify(query, update, options, NonceRecord.class);

        if (updated == null || updated.getNonce() == null) {
            if ("prod".equalsIgnoreCase(activeProfile)) {
                log.warn("Nonce record missing for {}, falling back to on-chain nonce", address);
            }
            try {
                resetNonceFromChain(address);
            }catch (RuntimeException e){
                return null;
            }
            // Try again after reset
            updated = mongoTemplate.findAndModify(query, update, options, NonceRecord.class);
            if (updated == null || updated.getNonce() == null) {
                return null; // or throw exception, or return BigInteger.ZERO as a last resort
            }
        }

        // Return the nonce *before* the increment
        return  BigInteger.valueOf(updated.getNonce() - 1);
    }

    /**
     * Syncs Mongo nonce with on-chain nonce if reset is needed.
     */
    public void resetNonceFromChain(String address) {
        String activeProfile = environment.getProperty("spring.profiles.active");
        address = address.toLowerCase();
        try {
            EthGetTransactionCount response = web3JService.web3j.ethGetTransactionCount(
                    address, DefaultBlockParameterName.PENDING).send();

            BigInteger pendingChainNonce = response.getTransactionCount();

            Query query = new Query(Criteria.where("_id").is(address));
            NonceRecord current = mongoTemplate.findOne(query, NonceRecord.class);

            if (current != null) {
                BigInteger localNonce = BigInteger.valueOf(current.getNonce());
                if (localNonce.subtract(pendingChainNonce).compareTo(BigInteger.valueOf(MAX_DRIFT)) <= 0) {
                    // Safe to update: chain nonce is not too far behind local
                    Update update = new Update()
                            .set("nonce", pendingChainNonce.longValue())
                            .set("updatedAt", Instant.now());

                    mongoTemplate.upsert(query, update, NonceRecord.class);
                    log.info("Reset nonce for {} to {} (within drift)", address, pendingChainNonce);
                } else {
                    log.warn("Skipping nonce reset for {}: chain nonce {} is behind local {} beyond max drift {}",
                            address, pendingChainNonce, localNonce, MAX_DRIFT);
                }
            } else {
                // No record exists yet — safe to initialize
                Update update = new Update()
                        .set("nonce", pendingChainNonce.longValue())
                        .set("updatedAt", Instant.now());

                mongoTemplate.upsert(query, update, NonceRecord.class);
                if ("prod".equalsIgnoreCase(activeProfile)) {
                    log.info("Initialized nonce for {} to {}", address, pendingChainNonce);
                }
            }

        } catch (IOException e) {
            log.error("Failed to reset nonce from chain for address {}", address, e);
            throw new RuntimeException("Failed to reset nonce from chain", e);
        }
    }

}
