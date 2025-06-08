package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.web3j.abi.datatypes.Function;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractFunctionDetails {

    private String contentID;
    private String userID;
    private String channelName;
    private ContractFunctionEnum contractFunctionEnum;
    private Double manaAmount;

}
