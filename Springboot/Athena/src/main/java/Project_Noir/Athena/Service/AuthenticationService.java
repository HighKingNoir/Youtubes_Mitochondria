package Project_Noir.Athena.Service;

import Project_Noir.Athena.DTO.ForgotPasswordRequest;
import Project_Noir.Athena.DTO.GoogleLoginRequest;
import Project_Noir.Athena.DTO.LoginRequest;
import Project_Noir.Athena.DTO.RegisterRequest;
import Project_Noir.Athena.Exception.SivantisException;
import Project_Noir.Athena.Model.*;
import Project_Noir.Athena.Repo.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TwoFactorAuthenticationService TFAService;
    private final AuthenticationManager authManager;
    private final EncryptionService encryptionService;
    private final MailService mailService;
    private final MessageService messageService;
    Random random = new Random();

    @Value("${google.clientId}")
    private String googleClientId;
    @Value("${url}")
    private String url;
    @Value("${recaptcha.secret.key}")
    private String recaptchaSecretKey;


    @Async
    // @dev Generates a new user and Sends verification email
    public void registerUser(RegisterRequest request) {
        if(userRepo.findByEmailIgnoreCase(request.getEmail()).isPresent()){
            throw  new SivantisException("User with this Email already exist");
        }
        if(userRepo.findByUsernameIgnoreCase(request.getUsername()).isPresent()){
            throw  new SivantisException("User with this Username already exist");
        }
        verifyRecaptcha(request.getRecaptchaToken());
        var user = Users.builder()
                .userId(ObjectId.get().toHexString())
                .email(request.getEmail())
                .username(request.getUsername())
                .messages(new ArrayList<>())
                //UserChannelDetails
                        .personalWallet(null)
                        .isViolator(false)
                        .channelSubscribedTo(new ArrayList<>())
                        .channels(new ArrayList<>())
                //UserContentDetails
                        .payLater(new ArrayList<>())
                        .videosPosted(0)
                        .totalHype(0.0)
                        .rank(1)
                //UserDetails not saved to the client
                        .createdContent(new ArrayList<>())
                        .purchasedContent(new HashMap<>())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .allowedDevelopingVideos(0)
                        .role(Role.USER)
                        .enabled(false)
                        .creationDate(Instant.now())
                        .isContentCreator(false)
                        .nextWalletChangeTime(Instant.now())
                        .mfaEnabled(false)
                        .secret(TFAService.generateNewSecret())
                        .secretExpiration(Instant.now().plus(2, ChronoUnit.HOURS))
                        .deleteRequest(false)
                        .language(request.getLanguage())
                        .isBanned(false)
                .build();
        messageService.newUserMessage(user);
        sendActivationEmail(user);
    }

    private void verifyRecaptcha(String response) {
        String url = "https://www.google.com/recaptcha/api/siteverify";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("secret", recaptchaSecretKey);
        body.add("response", response);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<RecaptchaResponse> responseEntity = new RestTemplate().postForEntity(url, requestEntity, RecaptchaResponse.class);
        RecaptchaResponse recaptchaResponse = responseEntity.getBody();
        if(recaptchaResponse == null){
            throw new SivantisException("Recaptcha Verification Failed. Try Again.");
        }
        if(!recaptchaResponse.isSuccess()){
            throw new SivantisException("Recaptcha Verification Failed. Try Again.");
        }
        if(recaptchaResponse.getScore() < 0.5){
            throw new SivantisException("Recaptcha Verification Failed. Try Again.");
        }
    }

    @Async
    public void resendActivationEmail(String email) {
        var user = userRepo.findByEmail(email).orElseThrow();
        user.setSecret(TFAService.generateNewSecret());
        user.setSecretExpiration(Instant.now().plus(2, ChronoUnit.HOURS));
        userRepo.save(user);
        sendActivationEmail(user);
    }

    @Async
    public void resendForgotPasswordEmail(String email) {
        var user = userRepo.findByEmail(email).orElseThrow();
        user.setSecret(TFAService.generateNewSecret());
        user.setSecretExpiration(Instant.now().plus(2, ChronoUnit.HOURS));
        userRepo.save(user);
        sendForgotPasswordEmail(user);
    }

    private void sendActivationEmail(Users users){
        mailService.sendEmail(new NotificationEmail("Please Activate your Account",
                users.getEmail(),
                url + "/Enable/" + users.getSecret(),
                NotificationEmailEnum.Activation
                ));
    }

    private void sendForgotPasswordEmail(Users users){
        mailService.sendEmail(new NotificationEmail("Password Reset",
                users.getEmail(),
                url + "/ForgotPassword/" + users.getSecret(),
                NotificationEmailEnum.ForgotPassword));
    }

    public AuthenticationResponse forgotPassword(ForgotPasswordRequest forgotPasswordRequest){
        var potentialUser = userRepo.findBySecret(forgotPasswordRequest.getSecret());
        if (potentialUser.isEmpty()) {
            potentialUser = userRepo.findByUsername(forgotPasswordRequest.getUsername());
            if(potentialUser.isEmpty()){
                throw new SivantisException("User not found");
            }
        }
        var user = potentialUser.get();
        if(forgotPasswordRequest.getNewPassword() != null){
            if(secretHasExpired(user.getSecretExpiration())){
                throw new SivantisException("Secret Has Expired");
            }
            user.setPassword(passwordEncoder.encode(forgotPasswordRequest.getNewPassword()));
            user.setSecret(null);
            userRepo.save(user);
            return AuthenticationResponse.builder()
                    .mfaEnabled(false)
                    .build();
        }
        user.setSecret(TFAService.generateNewSecret());
        user.setSecretExpiration(Instant.now().plus(2, ChronoUnit.HOURS));
        userRepo.save(user);
        sendForgotPasswordEmail(user);
        return AuthenticationResponse.builder()
                .email(user.getEmail())
                .build();
    }


    // @dev Logs in a user with their Sivantis credentials
    public AuthenticationResponse login(LoginRequest request) throws Exception {
        var user = userRepo.findByUsername(request.getUsername()).orElseThrow(()-> new SivantisException("User not Found"));
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (DisabledException e) {
            user.setSecret(TFAService.generateNewSecret());
            user.setSecretExpiration(Instant.now().plus(2, ChronoUnit.HOURS));
            userRepo.save(user);
            sendActivationEmail(user);
            return AuthenticationResponse.builder()
                    .invalidEmail(true)
                    .email(user.getEmail())
                    .build();
        }
        catch (LockedException e){
            throw new SivantisException("Your account is banned until " + user.getUnbanDate());
        }
        if(user.getDeleteRequest()) {
            user.setDeleteRequest(false);
            userRepo.save(user);
        }
        if(user.getMfaEnabled() && !request.getCode().isEmpty()){
            if (!TFAService.isOtpValid(encryptionService.decrypt(user.getMfaSecret()), request.getCode())){
                throw new SivantisException("Incorrect Code");
            }
            verifyRecaptcha(request.getRecaptchaToken());
            return AuthenticationResponse.builder()
                    .mfaEnabled(true)
                    .invalidEmail(false)
                    .jwtToken(jwtService.generateToken(user))
                    .build();
        }
        if(user.getMfaEnabled()){
            return AuthenticationResponse.builder()
                    .mfaEnabled(true)
                    .invalidEmail(false)
                    .build();
        }
        verifyRecaptcha(request.getRecaptchaToken());
        return AuthenticationResponse.builder()
                .mfaEnabled(false)
                .invalidEmail(false)
                .jwtToken(jwtService.generateToken(user))
                .build();
    }

    //done
    // @dev Logs in a user with their Google credentials
    public AuthenticationResponse googleLogin(GoogleLoginRequest googleLoginRequest) throws Exception {
        var Payload = validateGoogleJWT(googleLoginRequest.getGoogleJWT());
        if(userRepo.findById(Payload.getSubject()).isEmpty()) {
            if(userRepo.findByEmailIgnoreCase(Payload.getEmail()).isPresent()){
                var user = userRepo.findByEmailIgnoreCase(Payload.getEmail()).orElseThrow();
                if(user.getIsBanned()){
                    throw new SivantisException("Your account is banned until " + user.getUnbanDate());
                }
                if(user.getDeleteRequest()){
                    user.setDeleteRequest(false);
                    userRepo.save(user);
                }
                if(!user.isEnabled()){
                    user.setSecret(TFAService.generateNewSecret());
                    user.setSecretExpiration(Instant.now().plus(2, ChronoUnit.HOURS));
                    userRepo.save(user);
                    sendActivationEmail(user);
                    return AuthenticationResponse.builder()
                            .newUser(false)
                            .invalidEmail(true)
                            .email(user.getEmail())
                            .build();
                }
                if(user.getMfaEnabled() && !googleLoginRequest.getCode().isEmpty()){
                    if (!TFAService.isOtpValid(encryptionService.decrypt(user.getMfaSecret()), googleLoginRequest.getCode())){
                        throw new SivantisException("Incorrect Code");
                    }
                    return AuthenticationResponse.builder()
                            .newUser(false)
                            .invalidEmail(false)
                            .mfaEnabled(true)
                            .jwtToken(jwtService.generateToken(user))
                            .build();
                }
                if(user.getMfaEnabled()){
                    return AuthenticationResponse.builder()
                            .newUser(false)
                            .invalidEmail(false)
                            .mfaEnabled(true)
                            .build();
                }
                return AuthenticationResponse.builder()
                        .jwtToken(jwtService.generateToken(user))
                        .newUser(false)
                        .invalidEmail(false)
                        .mfaEnabled(false)
                        .build();
            }
            //New user
            var user = generateNewGoogleUser(Payload, googleLoginRequest.getLanguage());
            checkGoogleUserName(user);
            if (user.isEnabled()) {
                return AuthenticationResponse.builder()
                        .newUser(true)
                        .invalidEmail(false)
                        .jwtToken(jwtService.generateToken(user))
                        .build();
            }
            user.setSecret(TFAService.generateNewSecret());
            user.setSecretExpiration(Instant.now().plus(2, ChronoUnit.HOURS));
            userRepo.save(user);
            sendActivationEmail(user);
            return AuthenticationResponse.builder()
                    .newUser(true)
                    .invalidEmail(true)
                    .email(user.getEmail())
                    .build();
        }
        //Returning user
        var user = userRepo.findById(Payload.getSubject()).orElseThrow();
        if(user.getIsBanned()){
            throw new SivantisException("Your account is banned until " + user.getUnbanDate());
        }
        if(user.getDeleteRequest()){
            user.setDeleteRequest(false);
            userRepo.save(user);
        }
        //Invalid Email
        if(!user.isEnabled()){
            user.setSecret(TFAService.generateNewSecret());
            user.setSecretExpiration(Instant.now().plus(2, ChronoUnit.HOURS));
            userRepo.save(user);
            sendActivationEmail(user);
            return AuthenticationResponse.builder()
                    .newUser(false)
                    .invalidEmail(true)
                    .email(user.getEmail())
                    .build();
        }
        if(user.getMfaEnabled() && !googleLoginRequest.getCode().isEmpty()){
            if (!TFAService.isOtpValid(encryptionService.decrypt(user.getMfaSecret()), googleLoginRequest.getCode())){
                throw new SivantisException("Incorrect Code");
            }
            return AuthenticationResponse.builder()
                    .newUser(false)
                    .invalidEmail(false)
                    .mfaEnabled(true)
                    .jwtToken(jwtService.generateToken(user))
                    .build();
        }
        if(user.getMfaEnabled()){
            return AuthenticationResponse.builder()
                    .newUser(false)
                    .invalidEmail(false)
                    .mfaEnabled(true)
                    .build();
        }
        //Valid user without 2FA enabled
        return AuthenticationResponse.builder()
                .jwtToken(jwtService.generateToken(user))
                .newUser(false)
                .invalidEmail(false)
                .mfaEnabled(false)
                .build();
    }

    private void checkGoogleUserName(Users user) {
        var originalUsername = user.getUsername();
        int attempts = 0;
        while(userRepo.findByUsernameIgnoreCase(user.getUsername()).isPresent() && attempts != 5){
            user.setUsername(originalUsername + "#" + randomThreeDigitNumber());
            attempts++;
        }
        if(attempts == 5){
            throw new SivantisException("Couldn't generate username");
        }
        userRepo.save(user);
    }

    private Integer randomThreeDigitNumber(){
        int min = 100;
        int max = 999;
        return random.nextInt(max - min + 1) + min;
    }

    // @dev Checks if the received Google JWT is valid
    private GoogleIdToken.Payload validateGoogleJWT(String GoogleJWT) throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
        GoogleIdToken idToken = verifier.verify(GoogleJWT);
        if (idToken == null) {
            throw new SivantisException("Invalid GoogleId token");
        }
        return idToken.getPayload();
    }

    // @dev Generates a new user account using google credentials
    private Users generateNewGoogleUser(GoogleIdToken.Payload payload, String userLanguage) {
        var User = Users.builder()
                .userId(payload.getSubject())
                .email(payload.getEmail())
                .username(extractText((String) payload.get("name")))
                .messages(new ArrayList<>())
                //UserChannelDetails
                .personalWallet(null)
                .isViolator(false)
                .channelSubscribedTo(new ArrayList<>())
                .channels(new ArrayList<>())
                //UserContentDetails
                .payLater(new ArrayList<>())
                .videosPosted(0)
                .totalHype(0.0)
                .rank(1)
                //UserDetails not sent to the client
                .createdContent(new ArrayList<>())
                .purchasedContent(new HashMap<>())
                .password(null)
                .allowedDevelopingVideos(0)
                .role(Role.USER)
                .enabled(payload.getEmailVerified())
                .creationDate(Instant.now())
                .isContentCreator(false)
                .nextWalletChangeTime(Instant.now())
                .mfaEnabled(false)
                .deleteRequest(false)
                .language(userLanguage)
                .isBanned(false)
                .build();
        messageService.newUserMessage(User);
        return User;
    }

    // @dev Enables the user account
    public AuthenticationResponse fetchUserAndEnable(String secret) {
            if(userRepo.findBySecret(secret).isEmpty()){
                throw new SivantisException("Invalid link");
            }
            var User = userRepo.findBySecret(secret).orElseThrow();
            if(secretHasExpired(User.getSecretExpiration())){
                throw new SivantisException("Secret Has Expired");
            }
            User.setEnabled(true);
            User.setSecret(null);
            userRepo.save(User);
            return AuthenticationResponse.builder()
                    .jwtToken(jwtService.generateToken(User))
                    .build();
    }

    private Boolean secretHasExpired(Instant secretExpiration){
        return Instant.now().isAfter(secretExpiration);
    }

    private String extractText(String input) {
        // Define a regular expression to match text inside parentheses
        String regex = "\\(([^)]+)\\)";

        // Attempt to find text inside parentheses
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            // Return text inside parentheses if found
            return matcher.group(1);
        } else {
            // If no parentheses found, return the first word
            return input.split("\\s")[0];
        }
    }
}











