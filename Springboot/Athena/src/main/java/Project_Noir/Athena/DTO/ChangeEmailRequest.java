package Project_Noir.Athena.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeEmailRequest {
    @NotNull
    private String currentEmailVerificationCode;
    @NotNull
    private String newEmailVerificationCode;
    private String code;
}
