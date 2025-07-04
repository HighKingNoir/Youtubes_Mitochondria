package Project_Noir.Athena.Service;

import Project_Noir.Athena.DTO.PasswordChangeRequest;
import Project_Noir.Athena.DTO.PersonalWalletRequest;
import Project_Noir.Athena.DTO.UsernameChangeRequest;
import Project_Noir.Athena.Exception.SivantisException;
import Project_Noir.Athena.Model.AuthenticationResponse;
import Project_Noir.Athena.Model.ContentEnum;
import Project_Noir.Athena.Model.Users;
import Project_Noir.Athena.Repo.ContentRepository;
import Project_Noir.Athena.Repo.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ContractServiceInterface contractServiceInterface;
    private final TwoFactorAuthenticationService TFAService;
    private final EncryptionService encryptionService;
    private final Ec2InstanceTagService ec2InstanceTagService;
    private PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MongoTemplate mongoTemplate;
    private final ContentRepository contentRepository;
    private final ServerSideMultiSendContractService serverSideMultiSendContractService;

    //Adds a personal wallet to a user account
    public AuthenticationResponse setPersonalWallet(PersonalWalletRequest personalWalletRequest, String JWT) throws Exception {
        var user = userRepository.findById(jwtService.extractUserId(JWT)).orElseThrow();
        if(user.getPersonalWallet() != null && user.getPersonalWallet().equals(personalWalletRequest.getPersonalWallet())){
            throw new SivantisException("This wallet is already set as your personal wallet");
        }
        if(user.getMfaEnabled() && !personalWalletRequest.getCode().isEmpty()){
            if (!TFAService.isOtpValid(encryptionService.decrypt(user.getMfaSecret()), personalWalletRequest.getCode())){
                throw new SivantisException("Incorrect Code");
            }
            Users updatedUser = tryUpdateWallet(user.getUserId(), personalWalletRequest.getPersonalWallet());
            if (updatedUser == null) {
                throw new SivantisException("Wallet cannot be updated until " + user.getNextWalletChangeTime());
            }
            if(user.isContentCreator()){
                contractServiceInterface.updatePersonalWallet(user.getUserId(), personalWalletRequest.getPersonalWallet());
            }
            return AuthenticationResponse.builder()
                    .mfaEnabled(true)
                    .build();
        }
        if(user.getMfaEnabled()){
            return AuthenticationResponse.builder()
                    .mfaEnabled(true)
                    .build();
        }
        Users updatedUser = tryUpdateWallet(user.getUserId(), personalWalletRequest.getPersonalWallet());
        if (updatedUser == null) {
            throw new SivantisException("Wallet cannot be updated until " + user.getNextWalletChangeTime());
        }
        if(updatedUser.isContentCreator()){
            contractServiceInterface.updatePersonalWallet(user.getUserId(), personalWalletRequest.getPersonalWallet());
        }
        return AuthenticationResponse.builder()
                .mfaEnabled(false)
                .build();
    }

    public Users tryUpdateWallet(String userId, String newWallet) {
        Instant now = Instant.now();

        Query query = new Query(Criteria
                .where("_id").is(userId)
                .and("nextWalletChangeTime").lt(now) // only allow update if changeTime is in the past
        );

        Update update = new Update()
                .set("personalWallet", newWallet)
                .set("nextWalletChangeTime", now.plus(1, ChronoUnit.DAYS));

        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Users.class
        );
    }

    public String getNextWalletChangeTime(String JWT){
        var user = userRepository.findById(jwtService.extractUserId(JWT)).orElseThrow();
        if(user.getNextWalletChangeTime().isAfter(Instant.now())){
            long timeDifferenceSeconds = Duration.between(Instant.now(), user.getNextWalletChangeTime()).getSeconds();
            long hoursRemaining = timeDifferenceSeconds / 3600;
            long minutesRemaining = (timeDifferenceSeconds % 3600) / 60;
            if(hoursRemaining > 1){
                return "Next Wallet Change in " + hoursRemaining + "hrs " + minutesRemaining + "min(s)";
            }
            if(hoursRemaining == 1){
                return "Next Wallet Change in " + hoursRemaining + "hr " + minutesRemaining + "min(s)";
            }
            return "Next Wallet Change in " + minutesRemaining + "min(s)" ;
        }
        return null;
    }


    public AuthenticationResponse toggle2FA(String code, String JWT) throws Exception {
        var user = userRepository.findById(jwtService.extractUserId(JWT)).orElseThrow();
        //2FA is enabled
        if(user.getMfaEnabled()){
            if(!code.isEmpty()){
                if (!TFAService.isOtpValid(encryptionService.decrypt(user.getMfaSecret()), code)){
                    throw new SivantisException("Incorrect Code");
                }
                //disable 2FA
                user.setMfaEnabled(false);
                userRepository.save(user);
                return AuthenticationResponse.builder()
                        .mfaEnabled(false)
                        .build();
            }
            return AuthenticationResponse.builder()
                    .mfaEnabled(true)
                    .build();
        }
        //2FA is not enabled yet
        if(!code.isEmpty()){
            if (!TFAService.isOtpValid(encryptionService.decrypt(user.getMfaSecret()), code)){
                throw new SivantisException("Incorrect Code");
            }
            //Enable 2FA
            user.setMfaEnabled(true);
            userRepository.save(user);
            return AuthenticationResponse.builder()
                    .mfaEnabled(true)
                    .build();
        }
        user.setMfaSecret(encryptionService.encrypt(TFAService.generateNewSecret()));
        userRepository.save(user);
        return AuthenticationResponse.builder()
                .mfaEnabled(false)
                .secretImageUri(TFAService.generateQRCodeImageUrl(encryptionService.decrypt(user.getMfaSecret())))
                .build();

    }

    public AuthenticationResponse changeUsername(UsernameChangeRequest usernameChangeRequest, String JWT) throws Exception {
        if(userRepository.findByUsername(usernameChangeRequest.getUsername()).isPresent()){
            throw new SivantisException("Username already exist");
        }
        var user = userRepository.findById(jwtService.extractUserId(JWT)).orElseThrow();
        if(user.getUsername().equals(usernameChangeRequest.getUsername())){
            throw new SivantisException("No change to Username");
        }
        if(user.getMfaEnabled() && !usernameChangeRequest.getCode().isEmpty()){
            if (!TFAService.isOtpValid(encryptionService.decrypt(user.getMfaSecret()), usernameChangeRequest.getCode())){
                throw new SivantisException("Incorrect Code");
            }
            user.setUsername(usernameChangeRequest.getUsername());
            userRepository.save(user);
            return AuthenticationResponse.builder()
                    .mfaEnabled(true)
                    .jwtToken(jwtService.generateToken(user))
                    .build();
        }
        if(user.getMfaEnabled()){
            return AuthenticationResponse.builder()
                    .mfaEnabled(true)
                    .build();
        }
        user.setUsername(usernameChangeRequest.getUsername());
        userRepository.save(user);
        return AuthenticationResponse.builder()
                .mfaEnabled(false)
                .jwtToken(jwtService.generateToken(user))
                .build();
    }

    public AuthenticationResponse changePassword(PasswordChangeRequest passwordChangeRequest, String JWT) throws Exception {
        var user = userRepository.findById(jwtService.extractUserId(JWT)).orElseThrow();
        if (!passwordEncoder.matches(passwordChangeRequest.getCurrentPassword(), user.getPassword())) {
            throw new SivantisException("Current password entered is incorrect");
        }
        if(user.getMfaEnabled() && !passwordChangeRequest.getCode().isEmpty()){
            if (!TFAService.isOtpValid(encryptionService.decrypt(user.getMfaSecret()), passwordChangeRequest.getCode())){
                throw new SivantisException("Incorrect Code");
            }
            user.setPassword(passwordEncoder.encode(passwordChangeRequest.getNewPassword()));
            userRepository.save(user);
            return AuthenticationResponse.builder()
                    .mfaEnabled(true)
                    .build();
        }
        if(user.getMfaEnabled()){
            return AuthenticationResponse.builder()
                    .mfaEnabled(true)
                    .build();
        }
        user.setPassword(passwordEncoder.encode(passwordChangeRequest.getNewPassword()));
        userRepository.save(user);
        return AuthenticationResponse.builder()
                .mfaEnabled(false)
                .build();
    }

    public AuthenticationResponse deleteUser(String code, String JWT) throws Exception {
        var user = userRepository.findById(jwtService.extractUserId(JWT)).orElseThrow();
        var userActiveContent = contentRepository.findAllById(user.getCreatedContent()).stream()
                .filter(content -> !content.getContentEnum().equals(ContentEnum.Inactive))
                .toList();
        if(!userActiveContent.isEmpty()){
            throw new SivantisException("All Content Must Be Inactive To Delete Account");
        }
        if(user.getMfaEnabled() && !code.isEmpty()){
            if (!TFAService.isOtpValid(encryptionService.decrypt(user.getMfaSecret()), code)){
                throw new SivantisException("Incorrect Code");
            }
            user.setDeleteRequest(true);
            user.setDeleteAccountDate(Instant.now().plus(30, ChronoUnit.DAYS));
            userRepository.save(user);
            return AuthenticationResponse.builder()
                    .mfaEnabled(true)
                    .build();
        }
        if(user.getMfaEnabled()){
            return AuthenticationResponse.builder()
                    .mfaEnabled(true)
                    .build();
        }
        user.setDeleteRequest(true);
        user.setDeleteAccountDate(Instant.now().plus(30, ChronoUnit.DAYS));
        userRepository.save(user);
        return AuthenticationResponse.builder()
                .mfaEnabled(false)
                .build();
    }

    @Scheduled(cron = "0 0 22 ? * THU", zone = "America/New_York")
    @SchedulerLock(name = "sendWeeklyMana", lockAtMostFor = "PT1M", lockAtLeastFor = "PT30S")
    public void sendWeeklyMana(){
        ec2InstanceTagService.markTransactionInProgress();
        try {
            var contentCreatorIds = userRepository.findAllByIsContentCreator(true).stream().map(Users::getUserId).toList();
            serverSideMultiSendContractService.sendWeeklyManaMultiCall(contentCreatorIds);
        } finally {
            ec2InstanceTagService.clearTransactionTag();
        }
    }



    @Scheduled(cron = "0 0 0 * * ?", zone = "America/New_York")
    @SchedulerLock(name = "deleteAccounts", lockAtMostFor = "PT1M", lockAtLeastFor = "PT30S")
    public void deleteAccounts() {
        var users = userRepository.findUsersByDeleteRequestIsTrueOrEnabledIsFalse();
        Instant now = Instant.now();
        for (Users user: users ){
            if(user.getDeleteRequest()){
                if(user.getDeleteAccountDate().isBefore(now)){
                    userRepository.delete(user);
                }
            }
            else {
                if (Duration.between(user.getCreationDate(), now).toDays() >= 30) {
                    userRepository.delete(user);
                }
            }
        }
    }

    public void banUser(String userID, LocalDate unbanDate){
        var user = userRepository.findById(userID).orElseThrow();
        user.setIsBanned(true);
        user.setUnbanDate(unbanDate);
        userRepository.save(user);
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "America/New_York")
    @SchedulerLock(name = "checkBannedChannels", lockAtMostFor = "PT1M", lockAtLeastFor = "PT30S")
    public void checkBannedChannels(){
        LocalDate currentDate = LocalDate.now();
        var bannedUsers = getAllBannedUsers();
        for(Users user: bannedUsers){
            if(currentDate.isEqual(user.getUnbanDate())){
                user.setIsBanned(false);
                userRepository.save(user);
            }
        }
    }

    private List<Users> getAllBannedUsers(){
        return userRepository.findAllByIsBanned(true);
    }
}
