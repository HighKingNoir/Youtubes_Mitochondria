package Project_Noir.Athena.DTO;

import Project_Noir.Athena.Model.StreamerInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResubmitChannelRequest {
    private String channelName;
    private ArrayList<StreamerInfo> steamerInfo;
}
