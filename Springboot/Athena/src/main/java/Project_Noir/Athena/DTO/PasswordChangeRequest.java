package Project_Noir.Athena.DTO;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PasswordChangeRequest {
    private String code;
    private String currentPassword;
    @Size(min = 8, max = 20, message = "Password length must be between 8 and 20 characters.")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!*_])[A-Za-z0-9@#$%^&+=!*_]{8,20}$", message = "Password must meet the specified criteria.")
    private String newPassword;

}
