package Project_Noir.Athena.Service;

import Project_Noir.Athena.Exception.SivantisException;
import Project_Noir.Athena.Model.Content;
import Project_Noir.Athena.Model.DashboardChartInfo;
import Project_Noir.Athena.Model.PaymentEnum;
import Project_Noir.Athena.Model.Users;
import Project_Noir.Athena.Repo.ContentRepository;
import Project_Noir.Athena.Repo.PaymentRepository;
import Project_Noir.Athena.Repo.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final ContentRepository contentRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<DashboardChartInfo> getYearChartData(String userId) {
        List<DashboardChartInfo> chartData = new ArrayList<>();
        Instant currentInstant = Instant.now();
        var user = userRepository.findById(userId).orElseThrow();
        List<String> contentIds = user.getCreatedContent();
        // Get payments for each content with status PendingPurchase or Purchased, grouped by month
        for (int i = 0; i < 12; i++) {
            Instant endDate = currentInstant.minus(i * 31, ChronoUnit.DAYS);
            Instant startDate = endDate.minus(31, ChronoUnit.DAYS);

            var listOfJsonString = paymentRepository.findManaToCreatorForContentAndStatusInDateRange(
                    contentIds,
                    Arrays.asList(PaymentEnum.PendingPurchase, PaymentEnum.Purchased),
                    startDate,
                    endDate
            );

            List<Double> manaAmounts = new ArrayList<>();

            for (String jsonString : listOfJsonString) {
                manaAmounts.add(getManaValue(jsonString));
            }

            var totalManaAmount = manaAmounts.stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();

            var month = endDate.atZone(ZoneOffset.UTC).getMonth().name();
            if(month.length() > 4){
                month = month.substring(0,3);
            }
            chartData.add(new DashboardChartInfo(
                    month, // Use month name as label
                    totalManaAmount
            ));
        }
        Collections.reverse(chartData);
        return chartData;
    }

    public List<DashboardChartInfo> getMonthChartData(String userId) {
        List<DashboardChartInfo> chartData = new ArrayList<>();
        Instant currentInstant = Instant.now();
        var user = userRepository.findById(userId).orElseThrow();
        List<String> contentIds = user.getCreatedContent();
        // Get payments for each content with status PendingPurchase or Purchased, grouped by month
        for (int i = 0; i < 5; i++) {
            Instant endDate = currentInstant.minus(i * 7, ChronoUnit.DAYS);
            Instant startDate = endDate.minus(7, ChronoUnit.DAYS);

            var listOfJsonString = paymentRepository.findManaToCreatorForContentAndStatusInDateRange(
                    contentIds,
                    Arrays.asList(PaymentEnum.PendingPurchase, PaymentEnum.Purchased),
                    startDate,
                    endDate
            );

            List<Double> manaAmounts = new ArrayList<>();

            for (String jsonString : listOfJsonString) {
                manaAmounts.add(getManaValue(jsonString));
            }

            var totalManaAmount = manaAmounts.stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();
            chartData.add(new DashboardChartInfo(
                    endDate.atZone(ZoneOffset.UTC).getMonthValue() + "/" + endDate.atZone(ZoneOffset.UTC).getDayOfMonth(), // Use month name as label
                    totalManaAmount
            ));
        }
        Collections.reverse(chartData);
        return chartData;
    }

    public List<DashboardChartInfo> getWeekChartData(String userId) {
        List<DashboardChartInfo> chartData = new ArrayList<>();
        Instant currentInstant = Instant.now();
        var user = userRepository.findById(userId).orElseThrow();
        List<String> contentIds = user.getCreatedContent();
        // Get payments for each content with status PendingPurchase or Purchased, grouped by month
        for (int i = 0; i < 8; i++) {
            Instant endDate = currentInstant.minus(i, ChronoUnit.DAYS);
            Instant startDate = endDate.minus(1, ChronoUnit.DAYS);

            var listOfJsonString = paymentRepository.findManaToCreatorForContentAndStatusInDateRange(
                    contentIds,
                    Arrays.asList(PaymentEnum.PendingPurchase, PaymentEnum.Purchased),
                    startDate,
                    endDate
            );

            List<Double> manaAmounts = new ArrayList<>();

            for (String jsonString : listOfJsonString) {
                manaAmounts.add(getManaValue(jsonString));
            }

            var totalManaAmount = manaAmounts.stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();

            chartData.add(new DashboardChartInfo(
                    endDate.atZone(ZoneOffset.UTC).getMonthValue() + "/" + endDate.atZone(ZoneOffset.UTC).getDayOfMonth(), // Use month name as label
                    totalManaAmount
            ));
        }
        Collections.reverse(chartData);
        return chartData;
    }

    public ArrayList<Object> getManaChange(String userId) {
        var array = new ArrayList<>();
        var user = userRepository.findById(userId).orElseThrow();
        List<String> contentIds = user.getCreatedContent();
        // Get payments for each content with status PendingPurchase or Purchased, grouped by month
        Instant currentDate = Instant.now();
        Instant monthAgo = currentDate.minus(31, ChronoUnit.DAYS);
        Instant twoMonthsAgo = currentDate.minus(62, ChronoUnit.DAYS);

        var listOfJsonStringThisMonth = paymentRepository.findManaToCreatorForContentAndStatusInDateRange(
                contentIds,
                Arrays.asList(PaymentEnum.PendingPurchase, PaymentEnum.Purchased),
                monthAgo,
                currentDate
        );

        var listOfJsonStringLastMonth = paymentRepository.findManaToCreatorForContentAndStatusInDateRange(
                contentIds,
                Arrays.asList(PaymentEnum.PendingPurchase, PaymentEnum.Purchased),
                twoMonthsAgo,
                monthAgo
        );

        List<Double> manaAmountsThisMonth = new ArrayList<>();
        List<Double> manaAmountsLastMonth = new ArrayList<>();

        for (String jsonString : listOfJsonStringThisMonth) {
            manaAmountsThisMonth.add(getManaValue(jsonString));
        }

        for (String jsonString : listOfJsonStringLastMonth) {
            manaAmountsLastMonth.add(getManaValue(jsonString));
        }


        var totalManaAmountThisMonth = manaAmountsThisMonth.stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        var totalManaAmountLastMonth = manaAmountsLastMonth.stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        var percentageChange = 0.0; // Default to zero if totalManaAmountLastMonth is zero
        if (totalManaAmountLastMonth != 0) {
            percentageChange = ((totalManaAmountThisMonth - totalManaAmountLastMonth) / totalManaAmountLastMonth) * 100;
        }
        array.add(totalManaAmountThisMonth);
        array.add(percentageChange);
        return array;
    }


    public ArrayList<Object> getHypeChange(String userId) {
        var array = new ArrayList<>();
        var currentDate = LocalDate.now();
        var monthAgo = currentDate.minusMonths(1);
        var twoMonthAgo = currentDate.minusMonths(2);
        Calendar latestPossibleDate = Calendar.getInstance();
        latestPossibleDate.setTime(new Date(Long.MAX_VALUE));
        var user = userRepository.findById(userId).orElseThrow();
        List<String> contentIds = user.getCreatedContent();
        var hypeValuesAfterToday = contentRepository.findByReleaseDateAfterAndContentIdIn(
                monthAgo,
                contentIds
        );

        var hypeValuesWithinMonth = contentRepository.findByReleaseDateBetweenAndContentIdIn(
                twoMonthAgo,
                monthAgo,
                contentIds
        );

        var totalHypeAfterToday = hypeValuesAfterToday.stream().map(Content::getHype).mapToDouble(Double::doubleValue).sum();
        var totalHypeWithinMonth = hypeValuesWithinMonth.stream().map(Content::getHype).mapToDouble(Double::doubleValue).sum();
        double percentageChange = 0.0;
        if (totalHypeWithinMonth != 0) {
            percentageChange = ((totalHypeAfterToday - totalHypeWithinMonth) / totalHypeWithinMonth) * 100;
        }

        array.add(totalHypeAfterToday);
        array.add(percentageChange);
        return array;
    }

    public ArrayList<Object> getVideoChange(String userId) {
        var array = new ArrayList<>();
        var currentDate = LocalDate.now();
        var monthAgo = currentDate.minusMonths(1);
        var twoMonthAgo = currentDate.minusMonths(2);
        var user = userRepository.findById(userId).orElseThrow();
        var contentIds = user.getCreatedContent();
        var countAfterToday = contentRepository.countByReleaseDateAfterAndContentIdIn(
                monthAgo,
                contentIds);
        var countWithinLastMonth = contentRepository.countByReleaseDateBetweenAndContentIdIn(
                twoMonthAgo,
                monthAgo,
                contentIds);
        if(countWithinLastMonth == null){
            countWithinLastMonth = 0L;
        }
        if(countAfterToday == null){
            countAfterToday = 0L;
        }
        double percentageChange = 0.0;
        if (countWithinLastMonth != 0L) {
            percentageChange = ((double) (countAfterToday - countWithinLastMonth) / countWithinLastMonth) * 100;
        }

        array.add(countAfterToday);
        array.add(percentageChange);
        return array;
    }

    public ArrayList<Object> getRankChange(String userId) {
        var array = new ArrayList<>();
        var user = userRepository.findById(userId).orElseThrow();
        array.add(user.getRank());
        array.add(user.getTotalHype());
        return array;
    }

    public List<Content> getTop5Videos(String userId) {
        Users user = userRepository.findById(userId).orElseThrow();
        return contentRepository.findAllById(user.getCreatedContent()).stream()
                .sorted((c1, c2) -> Double.compare(c2.getHype(), c1.getHype()))
                .limit(5)
                .collect(Collectors.toList());

    }

    public List<Content> getUpcomingVideos(String userId) {
        Users user = userRepository.findById(userId).orElseThrow();
        LocalDate today = LocalDate.now();
        List<Content> allContent = contentRepository.findByReleaseDateGreaterThanEqualOrderByReleaseDate(today);
        List<String> createdContentIds = user.getCreatedContent();
        List<Content> userContent = allContent.stream()
                .filter(content -> createdContentIds.contains(content.getContentId()))
                .sorted(Comparator.comparing(Content::getReleaseDate))
                .collect(Collectors.toList());
        if (userContent.size() > 5) {
            userContent = userContent.subList(0, 5);
        }

        return userContent;
    }

    private Double getManaValue(String jsonString){
        try {
            // Parse the JSON string into a JsonNode
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            // Extract the "manaAmount" value as a string
            String manaAmountStr = jsonNode.get("manaToCreator").asText();

            // Convert the string to a double and add it to the list
            return Double.parseDouble(manaAmountStr);

        } catch (Exception e) {
            throw new SivantisException(e.getMessage());
        }
    }
}
