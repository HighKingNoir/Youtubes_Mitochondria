package Project_Noir.Athena.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@NotNull
public class EditVideoRequest {
    private String contentID;
    private String contentName;
    private String description;
    private String youtubeTrailerVideoID;
}
