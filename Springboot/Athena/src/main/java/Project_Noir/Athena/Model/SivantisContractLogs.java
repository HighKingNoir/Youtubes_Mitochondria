package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document()
public class SivantisContractLogs {

    @Id
    //The message ID
    private String logId;

    private String contentID;

    private String userID;

    private String channelName;

    private Instant creationDate;

    private ContractEnum contractEnum;

    private ContractFunctionEnum contractFunctionEnum;

    private ContractTransactionReceipt contractTransactionReceipt;

    private Double totalManaAmount;

    private Double manaToCompany;

}
