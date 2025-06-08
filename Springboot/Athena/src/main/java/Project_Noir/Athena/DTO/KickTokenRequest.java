package Project_Noir.Athena.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KickTokenRequest {
    private String code;
    private String codeVerifier;
    private String redirectUri;
}
