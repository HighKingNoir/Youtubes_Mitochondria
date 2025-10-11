package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VerifiedBidPaymentTransaction {
    private String contentID;
    private String manaAmount;
    private BigDecimal dollarAmount;
    private String transactionHash;
    private String userId;
}
