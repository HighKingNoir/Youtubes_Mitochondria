package Project_Noir.Athena.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PersonalWalletRequest {
    private String personalWallet;
    private String code;
}
