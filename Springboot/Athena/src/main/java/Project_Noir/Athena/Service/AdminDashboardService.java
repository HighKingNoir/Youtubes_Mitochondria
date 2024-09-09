package Project_Noir.Athena.Service;

import Project_Noir.Athena.Model.AdminDashboardChartInfo;
import Project_Noir.Athena.Model.SivantisContractLogs;
import Project_Noir.Athena.Repo.ChannelRepository;
import Project_Noir.Athena.Repo.ContentRepository;
import Project_Noir.Athena.Repo.SivantisContractLogsRepository;
import Project_Noir.Athena.Repo.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class AdminDashboardService {
    private final UserRepository userRepository;
    private final SivantisContractLogsRepository sivantisContractLogsRepository;
    private final ContentRepository contentRepository;
    private final ChannelRepository channelRepository;


    public List<AdminDashboardChartInfo> getYearChartData() {
        List<AdminDashboardChartInfo> chartData = new ArrayList<>();
        Instant currentInstant = Instant.now();
        for (int i = 0; i < 12; i++) {
            Instant endDate = currentInstant.minus(i * 31, ChronoUnit.DAYS);
            Instant startDate = endDate.minus(31, ChronoUnit.DAYS);

            var logs = sivantisContractLogsRepository.findByManaToCompanyGreaterThanAndCreationDateBetween(
                    0.0,
                    startDate,
                    endDate
            );
            var allLogs = sivantisContractLogsRepository.findByCreationDateBetween(
                    startDate,
                    endDate
            );
            var totalManaAmount = logs.stream().map(SivantisContractLogs::getManaToCompany).mapToDouble(Double::doubleValue).sum();
            BigDecimal totalGasUsedEther = allLogs.stream()
                    .filter(log -> log.getContractTransactionReceipt() != null && log.getContractTransactionReceipt().getGasUsed() != null)
                    .map(log -> {
                        BigInteger gasUsed = log.getContractTransactionReceipt().getGasUsed();
                        return new BigDecimal(gasUsed).divide(new BigDecimal("1000000000000000000"),9, RoundingMode.UP);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            var month = endDate.atZone(ZoneOffset.UTC).getMonth().name();
            if(month.length() > 4){
                month = month.substring(0,3);
            }
            chartData.add(new AdminDashboardChartInfo(
                    month, // Use month name as label
                    totalManaAmount,
                    totalGasUsedEther
            ));
        }
        Collections.reverse(chartData);
        return chartData;
    }

    public List<AdminDashboardChartInfo> getMonthChartData() {
        List<AdminDashboardChartInfo> chartData = new ArrayList<>();
        Instant currentInstant = Instant.now();
        // Get payments for each content with status PendingPurchase or Purchased, grouped by month
        for (int i = 0; i < 5; i++) {
            Instant endDate = currentInstant.minus(i * 7, ChronoUnit.DAYS);
            Instant startDate = endDate.minus(7, ChronoUnit.DAYS);

            var logs = sivantisContractLogsRepository.findByManaToCompanyGreaterThanAndCreationDateBetween(
                    0.0,
                    startDate,
                    endDate
            );
            var allLogs = sivantisContractLogsRepository.findByCreationDateBetween(
                    startDate,
                    endDate
            );

            var totalManaAmount = logs.stream().map(SivantisContractLogs::getManaToCompany).mapToDouble(Double::doubleValue).sum();
            BigDecimal totalGasUsedEther = allLogs.stream()
                    .filter(log -> log.getContractTransactionReceipt() != null && log.getContractTransactionReceipt().getGasUsed() != null)
                    .map(log -> {
                        BigInteger gasUsed = log.getContractTransactionReceipt().getGasUsed();
                        return new BigDecimal(gasUsed).divide(new BigDecimal("1000000000000000000"),9, RoundingMode.UP);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            chartData.add(new AdminDashboardChartInfo(
                    endDate.atZone(ZoneOffset.UTC).getMonthValue() + "/" + endDate.atZone(ZoneOffset.UTC).getDayOfMonth(), // Use month name as label
                    totalManaAmount,
                    totalGasUsedEther
            ));
        }
        Collections.reverse(chartData);
        return chartData;
    }

    public List<AdminDashboardChartInfo> getWeekChartData() {
        List<AdminDashboardChartInfo> chartData = new ArrayList<>();
        Instant currentInstant = Instant.now();
        // Get payments for each content with status PendingPurchase or Purchased, grouped by month
        for (int i = 0; i < 8; i++) {
            Instant endDate = currentInstant.minus(i, ChronoUnit.DAYS);
            Instant startDate = endDate.minus(1, ChronoUnit.DAYS);
            var manaLogs = sivantisContractLogsRepository.findByManaToCompanyGreaterThanAndCreationDateBetween(
                    0.0,
                    startDate,
                    endDate
            );
            var allLogs = sivantisContractLogsRepository.findByCreationDateBetween(
                    startDate,
                    endDate
            );

            var totalManaAmount = manaLogs.stream().map(SivantisContractLogs::getManaToCompany).mapToDouble(Double::doubleValue).sum();
            BigDecimal totalGasUsedEther = allLogs.stream()
                    .filter(log -> log.getContractTransactionReceipt() != null && log.getContractTransactionReceipt().getGasUsed() != null)
                    .map(log -> {
                        BigInteger gasUsed = log.getContractTransactionReceipt().getGasUsed();
                        return new BigDecimal(gasUsed).divide(new BigDecimal("1000000000000000000"),9, RoundingMode.UP);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            chartData.add(new AdminDashboardChartInfo(
                    endDate.atZone(ZoneOffset.UTC).getMonthValue() + "/" + endDate.atZone(ZoneOffset.UTC).getDayOfMonth(), // Use month name as label
                    totalManaAmount,
                    totalGasUsedEther
            ));
        }
        Collections.reverse(chartData);
        return chartData;
    }

    public ArrayList<Object> getManaChange() {
        var array = new ArrayList<>();
        // Get payments for each content with status PendingPurchase or Purchased, grouped by month
        Instant currentDate = Instant.now();
        Instant monthAgo = currentDate.minus(31, ChronoUnit.DAYS);
        Instant twoMonthsAgo = currentDate.minus(62, ChronoUnit.DAYS);

        var logsThisMonth = sivantisContractLogsRepository.findByManaToCompanyGreaterThanAndCreationDateBetween(
                0.0,
                monthAgo,
                currentDate
        );

        var logsLastMonth = sivantisContractLogsRepository.findByManaToCompanyGreaterThanAndCreationDateBetween(
                0.0,
                twoMonthsAgo,
                monthAgo
        );


        double totalManaAmountThisMonth = logsThisMonth.stream().map(SivantisContractLogs::getManaToCompany).mapToDouble(Double::doubleValue).sum();
        double totalManaAmountLastMonth = logsLastMonth.stream().map(SivantisContractLogs::getManaToCompany).mapToDouble(Double::doubleValue).sum();
        double percentageChange = 0.0; // Default to zero if totalManaAmountLastMonth is zero
        if (totalManaAmountLastMonth != 0) {
            percentageChange = ((totalManaAmountThisMonth - totalManaAmountLastMonth) / totalManaAmountLastMonth) * 100;
        }
        array.add(totalManaAmountThisMonth);
        array.add(percentageChange);
        return array;
    }


    public ArrayList<Object> getVideoChange() {
        var array = new ArrayList<>();
        Instant currentDate = Instant.now();
        Instant monthAgo = currentDate.minus(31, ChronoUnit.DAYS);
        Instant twoMonthsAgo = currentDate.minus(62, ChronoUnit.DAYS);
        var countThisMonth = contentRepository.countByCreatedDateBetween(
                monthAgo,
                currentDate);
        var countLastMonth = contentRepository.countByCreatedDateBetween(
                twoMonthsAgo,
                monthAgo);
        if(countLastMonth == null){
            countLastMonth = 0L;
        }
        if(countThisMonth == null){
            countThisMonth = 0L;
        }
        double percentageChange = 0.0;
        if (countLastMonth != 0L) {
            percentageChange = ((double) (countThisMonth - countLastMonth) / countLastMonth) * 100;
        }

        array.add(countThisMonth);
        array.add(percentageChange);
        array.add(contentRepository.count());
        return array;
    }


    public ArrayList<Object> getUserChange() {
        var array = new ArrayList<>();
        Instant currentDate = Instant.now();
        Instant monthAgo = currentDate.minus(31, ChronoUnit.DAYS);
        Instant twoMonthsAgo = currentDate.minus(62, ChronoUnit.DAYS);
        var countThisMonth = userRepository.countByCreationDateBetween(
                monthAgo,
                currentDate);
        var countLastMonth = userRepository.countByCreationDateBetween(
                twoMonthsAgo,
                monthAgo);
        if(countLastMonth == null){
            countLastMonth = 0L;
        }
        if(countThisMonth == null){
            countThisMonth = 0L;
        }
        double percentageChange = 0.0;
        if (countLastMonth != 0L) {
            percentageChange = ((double) (countThisMonth - countLastMonth) / countLastMonth) * 100;
        }

        array.add(countThisMonth);
        array.add(percentageChange);
        array.add(userRepository.count());
        return array;
    }

    public ArrayList<Object> getChannelChange() {
        var array = new ArrayList<>();
        Instant currentDate = Instant.now();
        Instant monthAgo = currentDate.minus(31, ChronoUnit.DAYS);
        Instant twoMonthsAgo = currentDate.minus(62, ChronoUnit.DAYS);
        var countThisMonth = channelRepository.countByApprovedDateBetween(
                monthAgo,
                currentDate);
        var countLastMonth = channelRepository.countByApprovedDateBetween(
                twoMonthsAgo,
                monthAgo);
        if(countLastMonth == null){
            countLastMonth = 0L;
        }
        if(countThisMonth == null){
            countThisMonth = 0L;
        }
        double percentageChange = 0.0;
        if (countLastMonth != 0L) {
            percentageChange = ((double) (countThisMonth - countLastMonth) / countLastMonth) * 100;
        }

        array.add(countThisMonth);
        array.add(percentageChange);
        array.add(channelRepository.count());
        return array;
    }


}
