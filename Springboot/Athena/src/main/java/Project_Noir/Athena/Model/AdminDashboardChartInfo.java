package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminDashboardChartInfo {
    private String label;
    private Double manaValue;
    private BigDecimal expenses;
}
