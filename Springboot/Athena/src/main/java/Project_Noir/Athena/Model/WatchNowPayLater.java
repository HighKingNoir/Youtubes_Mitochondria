package Project_Noir.Athena.Model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document()
public class WatchNowPayLater {
    @Id
    private String watchNowPlayLaterId;

    private WatchNowPayLaterEnum watchNowPayLaterEnum;

    private String channelName;

    private String contentID;

    private Double paymentAmountInUSD;

    private int paymentsLeft;

    private Instant nextPaymentDate;


}
