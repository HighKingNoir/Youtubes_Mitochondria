package Project_Noir.Athena.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BidPaymentRequest {
    private String contentID;
    private String manaAmount;
    private BigDecimal dollarAmount;
    private String transactionHash;
}
