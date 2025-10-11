package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@CompoundIndexes({
        @CompoundIndex(
                name = "uniq_user_txType",
                def = "{'userId': 1, 'transactionVerificationFunctionEnum': 1}",
                unique = true
        )
})
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document()
public class RankUpPaymentLock {
    @Id
    private String rankUpPaymentId;
    private String userId;
    private TransactionVerificationFunctionEnum transactionVerificationFunctionEnum;
    private String transactionHash;
    private PaymentEnum paymentEnum;
    private Instant createdAt;
    private Instant verifiedAt;
}
