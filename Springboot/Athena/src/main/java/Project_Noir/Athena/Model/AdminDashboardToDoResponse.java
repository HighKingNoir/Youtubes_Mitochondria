package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminDashboardToDoResponse {
    private Long failedLogCount;
    private Long channelRequestCount;
    private Long channelUpdatesCount;
    private Long reportedContentCount;
}
