package Project_Noir.Athena.Service;

import Project_Noir.Athena.Exception.SivantisException;
import Project_Noir.Athena.Model.NotificationEmail;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@AllArgsConstructor
@Slf4j
@Service
public class MailService {
    private JavaMailSender javaMailSender;
    private SpringTemplateEngine templateEngine;

    public void sendEmail(NotificationEmail notificationEmail) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

        // Set the dynamic link as a Thymeleaf context variable
        Context context = new Context();
        context.setVariable("buttonLink", notificationEmail.getBody());

        String emailContent = null;
        switch (notificationEmail.getNotificationEmailEnum()){
            case Activation -> emailContent = templateEngine.process("ActivationTemplate", context);
            case ForgotPassword -> emailContent = templateEngine.process("ForgotPasswordTemplate", context);
        }

        try {
            helper.setTo(notificationEmail.getRecipient());
            helper.setSubject(notificationEmail.getSubject());
            helper.setText(emailContent, true);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new SivantisException("Email Failed To Send");
        }
    }
}
