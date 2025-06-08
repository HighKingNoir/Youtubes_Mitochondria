package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document()
public class SivantisContractLogs {

    @Id
    //The message ID
    private String logId;

    private Instant creationDate;

    private ContractEnum contractEnum;

    private List<ContractFunctionDetails> contractFunctionDetails;

    private ContractTransactionReceipt contractTransactionReceipt;

    private Double totalManaAmount;

    private Double manaToCompany;

    private Integer transactionCount;

}
