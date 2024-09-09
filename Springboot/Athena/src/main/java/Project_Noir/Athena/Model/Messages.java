package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document()
public class Messages {

    @Id
    //The message ID
    private String messageId;

    //the contents of the message
    private String transactionHash;

    //the contents of the message
    private ArrayList<String> extraInfo;

    //If the user has viewed the message
    private Boolean hasRead;

    //Weather the content is a Welcome, Announcement, Payment, Refund, YoutubeEmails message, etc...
    private MessageEnum messageEnum;

    private Instant creationDate;
}
