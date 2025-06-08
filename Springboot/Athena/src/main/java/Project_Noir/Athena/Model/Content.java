package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

@Data
@Document()
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Content {

    @Id
    //The ID of the content
    private String contentId;

    //The content's name
    private String contentName;

    //The trailer video ID
    private String youtubeTrailerVideoID;

    //The maximum number of bidders
    private int numbBidders;

    //The description of the YouTube video
    private String description;

    //The starting cost of the Content
    private int startingCost;

    //The creator's ID
    private String creatorID;

    //List of buyer's userID and the amount they spent in Mana
    private Map<String, String> listOfBuyerIds;

    //The Created date
    private Instant createdDate;

    //The Release date for the YouTube Video
    private LocalDate releaseDate;

    //The Thumbnail of the video
    private String thumbnail;

    //The amount of hype this content has generated
    private Double hype;

    //Distinguishes whether the content is an Innovation, Invention, Short Film, Sports, or Movie
    private String contentType;

    //Distinguishes whether the content is a Draft, Active, or Inactive
    private ContentEnum contentEnum;

    //Is the video already produced (I.e. Did the user create the main video already or is it still under production)
    private Boolean isComplete;

    //How long the video last
    private Duration duration;

    //The main video ID
    private String youtubeMainVideoID;

    //The username of the user's YouTube channel
    private String youtubeUsername;

    //The profile picture of the user's YouTube channel
    private String youtubeProfilePicture;

    //The state of weather the user violated any content policy
    private Boolean isViolator;

    private String googleSubject;

    private Boolean sentEmails;

    private Instant activeDate;

    private ArrayList<ContentReports> contentReports;

    private Double reportRate;

}
