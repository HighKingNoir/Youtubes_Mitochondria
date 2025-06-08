package Project_Noir.Athena.Controller;

import Project_Noir.Athena.DTO.ContentRequest;
import Project_Noir.Athena.DTO.GaslessFunctionCallRequest;
import Project_Noir.Athena.Model.Content;
import Project_Noir.Athena.Model.Payment;
import Project_Noir.Athena.Service.GaslessFunctionCallService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.protocol.exceptions.TransactionException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/Gasless")
@AllArgsConstructor
@Slf4j
public class GaslessFunctionCallController {
    private final GaslessFunctionCallService gaslessFunctionCallService;

    @PostMapping("/Fund")
    // @dev Generates a new content entity that is finished
    public ResponseEntity<String> fundChannel(@RequestBody GaslessFunctionCallRequest gaslessFunctionCallRequest, @RequestHeader("Authorization") String jwt) throws IOException, URISyntaxException, TransactionException, ExecutionException, InterruptedException {
        gaslessFunctionCallService.gaslessFundChannel(gaslessFunctionCallRequest, jwt);
        return ResponseEntity.status(HttpStatus.OK).body("Channel successfully funded");
    }

    @PostMapping("/Place")
    // @dev Generates a new content entity that is finished
    public ResponseEntity<String> placeBid(@RequestBody GaslessFunctionCallRequest gaslessFunctionCallRequest, @RequestHeader("Authorization") String jwt) throws IOException, URISyntaxException, TransactionException, ExecutionException, InterruptedException {
        gaslessFunctionCallService.gaslessPlaceBid(gaslessFunctionCallRequest, jwt);
        return new ResponseEntity<String>("Payment Successful", HttpStatus.OK);
    }

    @PostMapping("/Raise")
    // @dev Generates a new content entity that is finished
    public ResponseEntity<String> raiseBid(@RequestBody GaslessFunctionCallRequest gaslessFunctionCallRequest, @RequestHeader("Authorization") String jwt) throws IOException, URISyntaxException, TransactionException, ExecutionException, InterruptedException {
        gaslessFunctionCallService.gaslessRaiseBid(gaslessFunctionCallRequest, jwt);
        return new ResponseEntity<String>("Updated Payment Successful", HttpStatus.OK);
    }

    @PostMapping("/Cancel")
    // @dev Generates a new content entity that is finished
    public ResponseEntity<String> cancelBid(@RequestBody GaslessFunctionCallRequest gaslessFunctionCallRequest, @RequestHeader("Authorization") String jwt) throws IOException, URISyntaxException, TransactionException, ExecutionException, InterruptedException {
        gaslessFunctionCallService.gaslessCancelBid(gaslessFunctionCallRequest, jwt);
        return new ResponseEntity<String>("Updated Payment Successful", HttpStatus.OK);
    }
}
