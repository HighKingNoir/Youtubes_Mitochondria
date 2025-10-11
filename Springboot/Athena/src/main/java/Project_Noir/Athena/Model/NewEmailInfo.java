package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewEmailInfo {
    private String newEmail;
    private String currentEmailVerificationCode;
    private String newEmailVerificationCode;
    private Instant expiration;
}
