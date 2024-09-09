package Project_Noir.Athena.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@NotNull
public class CompleteVideoRequest {
    private String contentID;
    private String privacyStatus;
    private String duration;
    private String youtubeMainVideoID;
}
