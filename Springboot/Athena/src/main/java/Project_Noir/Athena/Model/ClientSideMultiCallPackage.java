package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document()
public class ClientSideMultiCallPackage {
    @Id
    private String packageId;
    byte[] functionData;
    ContractFunctionDetails contractFunctionDetails;
}
