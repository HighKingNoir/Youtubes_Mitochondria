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
public class Payment {

    @Id
    //The paymentID
    private String paymentId;

    //The ID of the content being purchased
    private String contentId;

    //The userID who purchased the content
    private String userId;

    private Instant paymentDate;

    //The amount of polygon mana used to purchase
    private String manaAmount;

    //The dollar equivalent to the amount of mana sent
    private Double dollarAmount;

    //The date at which the refund was submitted
    private Instant refundDate;

    private Double manaToCreator;

    //Distinguishes whether the content is a RefundedPurchase, PendingPurchase, Purchased, or Deleted
    private PaymentEnum status;

    //Transaction receipt
    private String transactionHash;


}
