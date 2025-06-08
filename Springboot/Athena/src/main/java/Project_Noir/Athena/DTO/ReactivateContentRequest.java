package Project_Noir.Athena.DTO;

import Project_Noir.Athena.Model.DateInfo;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@NotNull
public class ReactivateContentRequest {
    private String contentID;
    private DateInfo releaseDate;
}
