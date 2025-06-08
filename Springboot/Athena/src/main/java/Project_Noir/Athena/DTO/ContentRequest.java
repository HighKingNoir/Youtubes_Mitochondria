package Project_Noir.Athena.DTO;

import Project_Noir.Athena.Model.DateInfo;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContentRequest {
    @NotNull
    private String contentType;
    @NotNull
    private String contentName;
    private String description;
    private int startingCost;
    private int numbBidders;
    @NotNull
    private String youtubeTrailerVideoID;
    private String privacyStatus;
    @NotNull
    private DateInfo releaseDate;
    @NotNull
    private String thumbnail;
    private String duration;
    private String youtubeMainVideoID;
    @NotNull
    private String youtubeUsername;
    @NotNull
    private String youtubeProfilePicture;
    @NotNull
    private String googleSubject;
    private String liveBroadcastContent;
}
