package Project_Noir.Athena.Model;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreamerInfo {
    private String platform;
    private String streamerId;
    private String username;
    private Number averageWeeklyViewers;
    @Nullable
    private String youtubeChannelId;
}
