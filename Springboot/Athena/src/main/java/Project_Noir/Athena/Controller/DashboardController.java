package Project_Noir.Athena.Controller;

import Project_Noir.Athena.Model.Content;
import Project_Noir.Athena.Model.DashboardChangesResponse;
import Project_Noir.Athena.Model.DashboardChartInfo;
import Project_Noir.Athena.Service.DashboardService;
import Project_Noir.Athena.Service.JwtService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/Dashboard")
@AllArgsConstructor
@Slf4j
public class DashboardController {
    private final DashboardService dashboardService;
    private final ServerSideEventController serverSideEventController;
    private final JwtService jwtService;

    @GetMapping("/Chart/Year")
    public ResponseEntity<List<DashboardChartInfo>> getYearChartData(@RequestHeader("Authorization") String jwt) {
        return ResponseEntity.ok(dashboardService.getYearChartData(jwtService.extractUserId(jwtService.getJWTString(jwt))));
    }

    @GetMapping("/Chart/Month")
    public ResponseEntity<List<DashboardChartInfo>> getMonthChartData(@RequestHeader("Authorization") String jwt) {
        return ResponseEntity.ok(dashboardService.getMonthChartData(jwtService.extractUserId(jwtService.getJWTString(jwt))));
    }

    @GetMapping("/Chart/Week")
    public ResponseEntity<List<DashboardChartInfo>> getWeekChartData(@RequestHeader("Authorization") String jwt) {
        return ResponseEntity.ok(dashboardService.getWeekChartData(jwtService.extractUserId(jwtService.getJWTString(jwt))));
    }

    @GetMapping("/Changes")
    public ResponseEntity<DashboardChangesResponse> getChanges(@RequestHeader("Authorization") String jwt) {
        var userID = jwtService.extractUserId(jwtService.getJWTString(jwt));
        return ResponseEntity.ok(DashboardChangesResponse.builder()
                        .rankChange(dashboardService.getRankChange(userID))
                        .hypeChange(dashboardService.getHypeChange(userID))
                        .manaChange(dashboardService.getManaChange(userID))
                        .videoChange(dashboardService.getVideoChange(userID))
                .build());
    }

    @GetMapping("/Video/Top")
    public ResponseEntity<List<Content>> getTopVideos(@RequestHeader("Authorization") String jwt) {
        return ResponseEntity.ok(dashboardService.getTop5Videos(jwtService.extractUserId(jwtService.getJWTString(jwt))));
    }

    @GetMapping("/Video/Upcoming")
    public ResponseEntity<List<Content>> getUpcomingVideos(@RequestHeader("Authorization") String jwt) {
        return ResponseEntity.ok(dashboardService.getUpcomingVideos(jwtService.extractUserId(jwtService.getJWTString(jwt))));
    }

    @GetMapping("/Latest/ManaPrice")
    public ResponseEntity<Double> getLatestManaPrice() {
        return ResponseEntity.ok(serverSideEventController.latestValue);
    }


}
