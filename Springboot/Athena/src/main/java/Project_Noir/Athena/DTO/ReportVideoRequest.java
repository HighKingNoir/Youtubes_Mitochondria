package Project_Noir.Athena.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportVideoRequest {
    private String contentID;
    private String report;
}
