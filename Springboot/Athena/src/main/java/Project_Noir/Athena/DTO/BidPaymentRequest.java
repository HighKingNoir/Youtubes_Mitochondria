package Project_Noir.Athena.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BidPaymentRequest {
    private String contentID;
    private String manaAmount;
    private Double dollarAmount;
    private String transactionHash;
}
