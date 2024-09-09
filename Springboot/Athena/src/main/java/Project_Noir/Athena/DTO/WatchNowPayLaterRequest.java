package Project_Noir.Athena.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WatchNowPayLaterRequest {
    private String channelName;
    private String contentID;
    private Integer paymentIncrements;
}
