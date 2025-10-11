package Project_Noir.Athena.Controller;

import Project_Noir.Athena.DTO.*;
import Project_Noir.Athena.Model.AuthenticationResponse;
import Project_Noir.Athena.Model.Users;
import Project_Noir.Athena.Repo.ChannelRepository;
import Project_Noir.Athena.Repo.UserRepository;
import Project_Noir.Athena.Service.JwtService;
import Project_Noir.Athena.Service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/Users")
@AllArgsConstructor
@Slf4j
public class UserController {
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtService jwtService;
    private final ChannelRepository channelRepository;

//    // @dev Returns all users in the database
//    @GetMapping("/All")
//    public ResponseEntity<List<Users>> getAllUsers(){
//        return ResponseEntity.status(HttpStatus.OK).body(userRepository.findAll());
//    }

    // @dev Gets the user info from the JWT token
    @GetMapping("/User/{token}")
    public ResponseEntity<Users> getUser(@PathVariable("token") String JWT){
        return ResponseEntity.status(HttpStatus.OK).body(userRepository.findByUsername(jwtService.extractUsername(JWT)).orElseThrow());
    }

    @GetMapping("/Leaderboard/{userId}")
    public ResponseEntity<Users> getUserDetails(@PathVariable("userId") String userId){
        return ResponseEntity.status(HttpStatus.OK).body(userRepository.findById(userId).orElseThrow());
    }

    @GetMapping("/Leaderboard")
    public ResponseEntity<List<Users>> getLeaderBoard(@RequestParam(name = "totalHype", required = false) Double totalHype){
        Sort sort = Sort.by(Sort.Order.desc("totalHype"));
        List<Users> users;

        if (totalHype != null) {
            users = userRepository.findByTotalHypeLessThan(totalHype, PageRequest.of(0, 50, sort));
        } else {
            users = userRepository.findTop50ByOrderByTotalHypeDesc();
        }
        return ResponseEntity.status(HttpStatus.OK).body(users);
    }

    // @dev Sets the user's personal wallet address
    @PatchMapping("/PersonalWallet")
    public ResponseEntity<AuthenticationResponse> setUserPersonalWallet(@RequestBody PersonalWalletRequest personalWalletRequest, @RequestHeader("Authorization") String jwt) throws Exception {
        return new ResponseEntity<>(userService.setPersonalWallet(personalWalletRequest, jwtService.getJWTString(jwt)), HttpStatus.OK);
    }

    // @dev Sets the user's personal wallet address
    @GetMapping("/PersonalWallet/NextWalletChange/{JWT}")
    public ResponseEntity<String> getNextWalletChangeTime(@PathVariable() String JWT){
        return new ResponseEntity<String>(userService.getNextWalletChangeTime(JWT), HttpStatus.OK);
    }

    @PutMapping ("/PayLater/Add/{contentID}")
    public ResponseEntity<String> addToPayLater(@PathVariable() String contentID, @RequestHeader("Authorization") String jwt){
        userService.addToPayLaterList(jwtService.extractUserId(jwtService.getJWTString(jwt)), contentID);
        return new ResponseEntity<String>("Added to Pay Later" , HttpStatus.OK);
    }

    @PutMapping ("/Change/Username")
    public ResponseEntity<AuthenticationResponse> changeUsername(@RequestBody @Valid UsernameChangeRequest usernameChangeRequest, @RequestHeader("Authorization") String jwt) throws Exception {
        return new ResponseEntity<>(userService.changeUsername(usernameChangeRequest, jwtService.getJWTString(jwt)) , HttpStatus.OK);
    }

    @PutMapping ("/Change/Email")
    public ResponseEntity<AuthenticationResponse> changeEmail(@RequestBody @Valid ChangeEmailRequest changeEmailRequest, @RequestHeader("Authorization") String jwt) throws Exception {
        return new ResponseEntity<>(userService.changeEmail(changeEmailRequest, jwtService.getJWTString(jwt)) , HttpStatus.OK);
    }

    @PutMapping ("/Change/Email/Initiate/{newEmail}")
    public ResponseEntity<String> changeEmailInitiation(@PathVariable() String newEmail, @RequestHeader("Authorization") String jwt) throws Exception {
        userService.changeEmailInitiation(newEmail, jwtService.getJWTString(jwt));
        return new ResponseEntity<>("Email Change Initiated", HttpStatus.OK);
    }

    @PutMapping ("/Change/Password")
    public ResponseEntity<AuthenticationResponse> changePassword(@RequestBody @Valid PasswordChangeRequest passwordChangeRequest, @RequestHeader("Authorization") String jwt) throws Exception {
        return new ResponseEntity<>(userService.changePassword(passwordChangeRequest, jwtService.getJWTString(jwt)) , HttpStatus.OK);
    }

    @PutMapping ("/PayLater/Remove/{contentID}")
    public ResponseEntity<String> removeFromPayLater(@PathVariable() String contentID, @RequestHeader("Authorization") String jwt){
        userService.removeFromPayLaterList(jwtService.extractUserId(jwtService.getJWTString(jwt)), contentID);
        return new ResponseEntity<String>("Removed From Pay Later", HttpStatus.OK);
    }

    @PutMapping ("/Subscribe/{channelId}")
    public ResponseEntity<String> subscribeToChannel(@PathVariable() String channelId, @RequestHeader("Authorization") String jwt){
        userService.subscribeToChannel(jwtService.extractUserId(jwtService.getJWTString(jwt)), channelId);
        return new ResponseEntity<String>("Subscribed" , HttpStatus.OK);
    }

    @PutMapping ("/Unsubscribe/{channelId}")
    public ResponseEntity<String> unsubscribeFromChannel(@PathVariable() String channelId, @RequestHeader("Authorization") String jwt){
        userService.unsubscribeFromChannel(jwtService.extractUserId(jwtService.getJWTString(jwt)), channelId);
        return new ResponseEntity<String>("Unsubscribed", HttpStatus.OK);
    }

    @PutMapping ("/Toggle2FA")
    public ResponseEntity<AuthenticationResponse> toggle2FA(@RequestParam(required = false) String code, @RequestHeader("Authorization") String jwt) throws Exception {
        return new ResponseEntity<>(userService.toggle2FA(code, jwtService.getJWTString(jwt)), HttpStatus.OK);
    }

    @PutMapping ("/Delegate")
    public ResponseEntity<String> delegateNewEmployee(@PathVariable String CreatorID){
        return new ResponseEntity<String>(userRepository.findById(CreatorID).orElseThrow().getUsername(), HttpStatus.OK);
    }

    @PutMapping("/Delete")
    public ResponseEntity<AuthenticationResponse> deleteUser(@RequestParam(required = false) String code, @RequestHeader("Authorization") String jwt) throws Exception {
        return new ResponseEntity<>(userService.deleteUser(code, jwtService.getJWTString(jwt)) , HttpStatus.OK);
    }

    @PutMapping("/Ban")
    public String banChannel(@RequestBody Integer duration){
        return "";
    }
}
