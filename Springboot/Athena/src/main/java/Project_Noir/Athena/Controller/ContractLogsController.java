package Project_Noir.Athena.Controller;

import Project_Noir.Athena.Model.ContractFunctionEnum;
import Project_Noir.Athena.Model.SivantisContractLogs;
import Project_Noir.Athena.Repo.SivantisContractLogsRepository;
import Project_Noir.Athena.Service.ContractServiceInterface;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.protocol.exceptions.TransactionException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/ContractLogs")
@AllArgsConstructor
@Slf4j
public class ContractLogsController {

    private final SivantisContractLogsRepository sivantisContractLogsRepository;
    private final ContractServiceInterface contractServiceInterface;

    @GetMapping("/View/Failed")
    public ResponseEntity<List<SivantisContractLogs>> getAllFailedLogs(@RequestParam(name = "creationDate", required = false) String creationDate){
        Sort sort = Sort.by("creationDate").descending();
        List<SivantisContractLogs> failedLogs;

        if (creationDate != null) {
            long timestampMillis = (long) (Double.parseDouble(creationDate) * 1000); // Convert to milliseconds
            failedLogs = sivantisContractLogsRepository.findByCreationDateBeforeAndContractTransactionReceiptStatus(Instant.ofEpochMilli(timestampMillis), PageRequest.of(0, 50, sort));
        } else {
            failedLogs = sivantisContractLogsRepository.findByContractTransactionReceiptStatus(PageRequest.of(0, 50, sort));
        }

        return  ResponseEntity.status(HttpStatus.OK).body(failedLogs);
    }


    @GetMapping("/View/Latest")
    public ResponseEntity<List<SivantisContractLogs>> getLatestLogs(@RequestParam(name = "creationDate", required = false) String creationDate) {
        Sort sort = Sort.by("creationDate").descending();
        List<SivantisContractLogs> logs;

        if (creationDate != null) {
            long timestampMillis = (long) (Double.parseDouble(creationDate) * 1000); // Convert to milliseconds
            logs = sivantisContractLogsRepository.findByCreationDateBefore(Instant.ofEpochMilli(timestampMillis), PageRequest.of(0, 50, sort));
        } else {
            logs = sivantisContractLogsRepository.findAll(PageRequest.of(0, 50, sort)).getContent();
        }

        return  ResponseEntity.status(HttpStatus.OK).body(logs);
    }

    @GetMapping("/View/Unresolved")
    public ResponseEntity<List<SivantisContractLogs>> getLogsNeedingUpdate(@RequestParam(name = "creationDate", required = false) String creationDate) {
        Sort sort = Sort.by("creationDate").descending();
        var functionEnums = List.of(ContractFunctionEnum.ReturnBid, ContractFunctionEnum.SetAuctionToInactive, ContractFunctionEnum.SendMana, ContractFunctionEnum.SendRefundPayment, ContractFunctionEnum.IncreaseCreatorRank, ContractFunctionEnum.SendWeeklyMana, ContractFunctionEnum.WatchNowPayLaterPayment);
        List<SivantisContractLogs> logs;

        if (creationDate != null) {
            long timestampMillis = (long) (Double.parseDouble(creationDate) * 1000); // Convert to milliseconds
            logs = sivantisContractLogsRepository.findByContractFunctionEnumInAndContractStatusEnumErrorAndCreationDateBefore(functionEnums, Instant.ofEpochMilli(timestampMillis), PageRequest.of(0, 50, sort));
        } else {
            logs = sivantisContractLogsRepository.findByContractFunctionEnumInAndContractStatusEnumError(functionEnums, PageRequest.of(0, 50, sort));
        }

        return  ResponseEntity.status(HttpStatus.OK).body(logs);
    }

    @PutMapping("/Resolve")
    public ResponseEntity<String> updateLog(@RequestParam String logID) throws TransactionException, IOException, ExecutionException, InterruptedException {
        contractServiceInterface.resolveLog(logID);
        return new ResponseEntity<>("Video Changes Successful", HttpStatus.OK);
    }

}
