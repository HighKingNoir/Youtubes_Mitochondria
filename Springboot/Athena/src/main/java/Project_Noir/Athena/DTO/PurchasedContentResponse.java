package Project_Noir.Athena.DTO;

import Project_Noir.Athena.Model.Content;
import Project_Noir.Athena.Model.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PurchasedContentResponse {
        ArrayList<Content> content;
        ArrayList<Payment> payment;
}
