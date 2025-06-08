package Project_Noir.Athena.DTO;

import Project_Noir.Athena.Model.StreamerInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChannelStreamerInfoRequest {
    private String channelId;
    private ArrayList<StreamerInfo> streamerInfo;
}
