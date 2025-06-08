package Project_Noir.Athena.Model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AuthenticationResponse {

    private String jwtToken;
    private Boolean mfaEnabled;
    private String secretImageUri;
    private String secret;
    private Boolean newUser;
    private Boolean invalidEmail;
    private String email;
}
