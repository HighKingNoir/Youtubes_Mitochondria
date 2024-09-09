package Project_Noir.Athena.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @Email
    private String email;

    @Size(min = 3, max = 15, message = "Input length must be between 3 and 15 characters.")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,15}$", message = "Input must consist only of lowercase letters, uppercase letters, digits, underscores, and hyphens.")
    private String username;

    @Size(min = 8, max = 20, message = "Password length must be between 8 and 20 characters.")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!*_])[A-Za-z0-9@#$%^&+=!*_]{8,20}$", message = "Password must meet the specified criteria.")
    private String password;

    private String language;
    private String recaptchaToken;
}
