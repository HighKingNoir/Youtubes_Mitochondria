package Project_Noir.Athena.Controller;

import Project_Noir.Athena.Model.ContractFunctionEnum;
import Project_Noir.Athena.Model.SivantisContractLogs;
import Project_Noir.Athena.Repo.SivantisContractLogsRepository;
import Project_Noir.Athena.Service.ContractServiceInterface;
import Project_Noir.Athena.Service.ResolveSmartContractFunctionCallService;
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
    private final ResolveSmartContractFunctionCallService resolveSmartContractFunctionCallService;

    @GetMapping("/View/Failed")
    public ResponseEntity<List<SivantisContractLogs>> getAllFailedLogs(@RequestParam(name = "creationDate", required = false) String creationDate){
        Sort sort = Sort.by("creationDate").ascending();
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

    @PutMapping("/Resolve/Single")
    public ResponseEntity<String> resolveSingleFunctionContactLog(@RequestParam String logID) throws TransactionException, IOException, ExecutionException, InterruptedException {
        resolveSmartContractFunctionCallService.resolveSingleFunctionContactLog(logID);
        return new ResponseEntity<>("Resolve Submitted", HttpStatus.OK);
    }

    @PutMapping("/Resolve/Multi")
    public ResponseEntity<String> resolveMultiFunctionContactLog(@RequestParam String logID) throws TransactionException, IOException, ExecutionException, InterruptedException {
        resolveSmartContractFunctionCallService.resolveMultiCall(logID);
        return new ResponseEntity<>("Resolve Submitted", HttpStatus.OK);
    }

    @PutMapping("/Resolve/Split")
    public ResponseEntity<String> SplitMultiFunctionContactLog(@RequestParam String logID) throws TransactionException, IOException, ExecutionException, InterruptedException {
        resolveSmartContractFunctionCallService.splitMultiCall(logID);
        return new ResponseEntity<>("Resolve Submitted", HttpStatus.OK);
    }

    @PutMapping("/Resolve/OnChain")
    public ResponseEntity<String> resolveContactLogOnChain(@RequestParam String logID) throws TransactionException, IOException, ExecutionException, InterruptedException {
        resolveSmartContractFunctionCallService.resolveOnChain(logID);
        return new ResponseEntity<>("Resolve Submitted", HttpStatus.OK);
    }

}
