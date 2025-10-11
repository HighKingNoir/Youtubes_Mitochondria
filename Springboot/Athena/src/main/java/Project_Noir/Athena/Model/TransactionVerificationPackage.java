package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigInteger;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document()
public class TransactionVerificationPackage {
    @Id
    private String transactionVerificationId;
    private TransactionVerificationFunctionEnum transactionVerificationFunctionEnum;
    private String contentID;
    private String userId;
    private String transactionHash;
    private BigInteger expectedManaAmount;
    private BlockchainInteractionStatusEnum status;
}
