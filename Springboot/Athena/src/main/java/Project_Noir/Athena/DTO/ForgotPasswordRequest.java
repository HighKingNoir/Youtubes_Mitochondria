package Project_Noir.Athena.DTO;

import com.mongodb.lang.Nullable;
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
public class ForgotPasswordRequest {
    @Size(min = 3, max = 20, message = "Username length must be between 3 and 20 characters.")
    private String username;
    private String secret;
    @Size(min = 8, max = 20, message = "Password length must be between 8 and 20 characters.")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!*_])[A-Za-z0-9@#$%^&+=!*_]{8,20}$", message = "Password must meet the specified criteria.")
    @Nullable
    private String newPassword;
}
