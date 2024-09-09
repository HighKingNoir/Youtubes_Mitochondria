package Project_Noir.Athena.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserWithdrawRequest {
    private Double dollarAmount;
    private String transactionHash;
}
