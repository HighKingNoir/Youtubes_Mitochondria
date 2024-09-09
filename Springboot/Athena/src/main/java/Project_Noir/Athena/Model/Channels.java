package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@Document()
@AllArgsConstructor
@NoArgsConstructor
public class Channels {

    @Id
    //The ID of the content
    private String channelId;

    private String ownerID;

    //Map between content ID and purchase ID
    private Map<String, String> purchasedContent;

    //The name the user decides to call their channel
    private String channelName;

    private String channelLogo;

    private String channelBanner;

    private String channelDescription;

    private ArrayList<String> watchNowPayLaterIDs;

    //IDs of users you are subscribed to
    private ArrayList<String> channelSubscribers;

    private String timezone;

    private boolean streamerChangeRequest;

    private ArrayList<StreamerInfo> streamerInfo;

    private ChannelStatus channelStatus;

    private Boolean awvIsUpdated;

    private Instant approvedDate;

    private Boolean isBanned;

    private LocalDate unbanDate;

    private List<ChannelEvents> channelEvents;

    private Instant nextAvailableResubmitDate;

    private ArrayList<StreamerInfo> streamerChangeInfo;
}
