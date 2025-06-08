package Project_Noir.Athena.Service;

import Project_Noir.Athena.Exception.SivantisException;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
@Slf4j
public class TwoFactorAuthenticationService {

    public String generateNewSecret(){
        return new DefaultSecretGenerator().generate();
    }

    public String generateQRCodeImageUrl(String secret){
        QrData data = new QrData.Builder()
                .label("Sivantis")
                .secret(secret)
                .issuer("Sivantis.com")
                .algorithm(HashingAlgorithm.SHA512)
                .digits(6)
                .period(45)
                .build();
        QrGenerator generator = new ZxingPngQrGenerator();
        byte[] imageData;
        try {
            imageData = generator.generate(data);
        } catch (QrGenerationException e) {
            throw new SivantisException(e.getMessage());
        }
        return getDataUriForImage(imageData, generator.getImageMimeType());
    }

    public boolean isOtpValid(String secret, String code){
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return  codeVerifier.isValidCode(secret,code);
    }
}
