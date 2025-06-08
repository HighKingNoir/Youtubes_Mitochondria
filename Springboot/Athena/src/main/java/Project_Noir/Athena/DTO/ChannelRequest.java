package Project_Noir.Athena.DTO;

import Project_Noir.Athena.Model.StreamerInfo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChannelRequest {
    @Size(min = 3, max = 20, message = "Input length must be between 3 and 15 characters.")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,20}$", message = "Input must consist only of lowercase letters, uppercase letters, digits, underscores, and hyphens.")
    private String channelName;
    @NotNull
    private String channelLogo;
    private ArrayList<StreamerInfo> steamerInfo;
    private String timezone;
}
