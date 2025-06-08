package Project_Noir.Athena.Controller;

import Project_Noir.Athena.DTO.BidPaymentRequest;
import Project_Noir.Athena.DTO.RefundRequest;
import Project_Noir.Athena.Model.Payment;
import Project_Noir.Athena.Service.JwtService;
import Project_Noir.Athena.Service.PaymentService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/Payment")
@AllArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtService jwtService;

    @PostMapping()
    // @dev Generates a new payment entity
    public ResponseEntity<String> purchaseContent(@RequestBody BidPaymentRequest bidPaymentRequest, @RequestHeader("Authorization") String jwt){
        paymentService.purchaseContent(bidPaymentRequest, jwtService.getJWTString(jwt));
        return new ResponseEntity<String>("Payment Successful", HttpStatus.OK);
    }

    @PutMapping("/update")
    // @dev updates the payment entity to the new mana amount
    public ResponseEntity<String> updatePurchasedContent(@RequestBody BidPaymentRequest bidPaymentRequest, @RequestHeader("Authorization") String jwt){
        paymentService.updatePurchasedContent(bidPaymentRequest, jwtService.getJWTString(jwt));
        return new ResponseEntity<String>("Updated Payment Successful", HttpStatus.OK);
    }

    @PatchMapping("/refund")
    // @dev Sets payment entity to refunded
    public ResponseEntity<String> refundPurchasedContent(@RequestBody RefundRequest refundRequest, @RequestHeader("Authorization") String jwt){
        paymentService.refundPurchasedContent(refundRequest, jwtService.getJWTString(jwt));
        return new ResponseEntity<String>("Refund Successful", HttpStatus.OK);
    }

    @GetMapping("/{JWT}")
    // @dev returns all user payment entities
    public ResponseEntity<List<Payment>> getAllUserPayment(@PathVariable String JWT){
        return ResponseEntity.status(HttpStatus.OK).body(paymentService.getAllUserPayment(jwtService.extractUserId(JWT)));
    }

    @GetMapping("/Channel/{channelName}")
    // @dev returns all user payment entities
    public ResponseEntity<List<Payment>> getAllChannelPayment(@PathVariable String channelName){
        return ResponseEntity.status(HttpStatus.OK).body(paymentService.getAllChannelPayment(channelName));
    }

    // @dev returns the payment entity or null
    @GetMapping("/{JWT}/{contentID}")
    public ResponseEntity<Payment> getUserPayment(@PathVariable String JWT, @PathVariable String contentID){
        return ResponseEntity.status(HttpStatus.OK).body(paymentService.findPayment(jwtService.extractUserId(JWT), contentID));
    }

    // @dev returns the payment entity or null
    @GetMapping("/Channel/{channelName}/{contentID}")
    public ResponseEntity<Payment> getChannelPayment(@PathVariable String channelName, @PathVariable String contentID){
        return ResponseEntity.status(HttpStatus.OK).body(paymentService.findChannelPayment(channelName, contentID));
    }



}
