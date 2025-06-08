package Project_Noir.Athena.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FundChannelRequest {
    private String channelName;
    private String manaAmount;
    private String transactionHash;
}
