package Project_Noir.Athena.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
public class NotificationEmail {
    private String subject;
    private String recipient;
    private String body;
    private NotificationEmailEnum notificationEmailEnum;
}
