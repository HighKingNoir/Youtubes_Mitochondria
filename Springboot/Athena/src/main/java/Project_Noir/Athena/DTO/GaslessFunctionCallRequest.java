package Project_Noir.Athena.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GaslessFunctionCallRequest {
    private String signature;
    private String sender;
    private String contentID;
    private String channelName;
    private String userID;
    private BigInteger amount;
    private BigInteger fee;
    private BigInteger deadline;
}
