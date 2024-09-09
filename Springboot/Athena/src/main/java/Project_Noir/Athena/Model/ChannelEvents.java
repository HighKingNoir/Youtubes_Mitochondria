package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChannelEvents {

    private ChannelEventStatus channelEventStatus;

    private String contentName;

    private Instant showTime;

    private String timeZone;

    private String watcher;
}
