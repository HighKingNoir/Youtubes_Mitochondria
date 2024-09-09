package Project_Noir.Athena.Controller;

import Project_Noir.Athena.Model.*;
import Project_Noir.Athena.Repo.ChannelRepository;
import Project_Noir.Athena.Repo.ContentRepository;
import Project_Noir.Athena.Repo.SivantisContractLogsRepository;
import Project_Noir.Athena.Repo.UserRepository;
import Project_Noir.Athena.Service.AdminDashboardService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/AdminDashboard")
@AllArgsConstructor
@Slf4j
public class AdminDashboardController {
    private final ServerSideEventController serverSideEventController;
    private final SivantisContractLogsRepository sivantisContractLogsRepository;
    private final ContentRepository contentRepository;
    private final ChannelRepository channelRepository;
    private final AdminDashboardService adminDashboardService;
    private final UserRepository userRepository;

    @GetMapping("/Chart/Year")
    public ResponseEntity<List<AdminDashboardChartInfo>> getYearChartData() {
        return ResponseEntity.ok(adminDashboardService.getYearChartData());
    }

    @GetMapping("/Chart/Month")
    public ResponseEntity<List<AdminDashboardChartInfo>> getMonthChartData() {
        return ResponseEntity.ok(adminDashboardService.getMonthChartData());
    }

    @GetMapping("/Chart/Week")
    public ResponseEntity<List<AdminDashboardChartInfo>> getWeekChartData() {
        return ResponseEntity.ok(adminDashboardService.getWeekChartData());
    }

    @GetMapping("/Changes")
    public ResponseEntity<AdminDashboardChangesResponse> getChanges() {
        return ResponseEntity.ok(AdminDashboardChangesResponse.builder()
                        .channelChange(adminDashboardService.getChannelChange())
                        .userChange(adminDashboardService.getUserChange())
                        .manaChange(adminDashboardService.getManaChange())
                        .videoChange(adminDashboardService.getVideoChange())
                .build());
    }


    @GetMapping("/Todo")
    public ResponseEntity<AdminDashboardToDoResponse> getTodoFailedLogs() {
        return ResponseEntity.ok(
                AdminDashboardToDoResponse.builder()
                    .failedLogCount(sivantisContractLogsRepository.countByContractTransactionReceipt_ContractStatusEnum(ContractStatusEnum.Error))
                    .channelRequestCount(channelRepository.countByChannelStatus(ChannelStatus.Pending))
                    .channelUpdatesCount(channelRepository.countByChannelStatusAndAwvIsUpdated(
                            ChannelStatus.Approved,
                            false
                    ))
                    .reportedContentCount((long) contentRepository.findAllWithNonEmptyContentReports().size())
                    .build()
        );
    }

    @GetMapping("/Search/Channel")
    public ResponseEntity<List<Channels>> searchChannels(@RequestParam String query) {
        // You can use channelRepository to perform a search query
        return ResponseEntity.ok(
                channelRepository.findTop10ByChannelNameContainingOrChannelIdContaining(query, query)
        );
    }

    @GetMapping("/Search/User")
    public ResponseEntity<List<Users>> searchUser(@RequestParam String query) {
        // You can use channelRepository to perform a search query
        return ResponseEntity.ok(
                userRepository.findTop10ByUsernameContainingOrUserIdContaining(query, query)
        );
    }

    @GetMapping("/Latest/ManaPrice")
    public ResponseEntity<Double> getLatestManaPrice() {
        return ResponseEntity.ok(serverSideEventController.latestValue);
    }


}
