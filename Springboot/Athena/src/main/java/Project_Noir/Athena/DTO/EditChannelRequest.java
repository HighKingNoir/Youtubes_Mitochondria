package Project_Noir.Athena.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditChannelRequest {
    @NotNull
    private String channelLogo;
    private String channelBanner;
    private String channelDescription;
    private String channelID;
    private ArrayList<StreamerLinks> streamerLinks;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StreamerLinks {
        private String platform;
        private String username;
        private String youtubeChannelId;
    }
}


