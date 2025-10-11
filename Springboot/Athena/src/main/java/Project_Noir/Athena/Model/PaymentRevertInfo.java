package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentRevertInfo {
    private String manaAmount;
    private BigDecimal dollarAmount;
    private Instant previousPaymentDate;
    private Double manaToCreator;
}
