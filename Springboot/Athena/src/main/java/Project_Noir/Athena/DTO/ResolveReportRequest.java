package Project_Noir.Athena.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResolveReportRequest {
    private String contentID;
    private ArrayList<String> reporterIDs;
}
