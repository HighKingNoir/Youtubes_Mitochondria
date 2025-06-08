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
public class UsernameChangeRequest {
    @Size(min = 3, max = 15, message = "Input length must be between 3 and 15 characters.")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,15}$", message = "Input must consist only of lowercase letters, uppercase letters, digits, underscores, and hyphens.")
    private String username;
    private String code;
}
