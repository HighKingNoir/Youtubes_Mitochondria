package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardChangesResponse {
    private ArrayList<Object> manaChange;
    private ArrayList<Object> rankChange;
    private ArrayList<Object> videoChange;
    private ArrayList<Object> hypeChange;
}
