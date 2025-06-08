package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractTransactionReceipt {
    private String transactionHash;
    private BigInteger gasUsed;
    private String revertReason;
    private ContractStatusEnum contractStatusEnum;
}
