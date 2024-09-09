package Project_Noir.Athena.Controller;

import Project_Noir.Athena.DTO.ForgotPasswordRequest;
import Project_Noir.Athena.DTO.GoogleLoginRequest;
import Project_Noir.Athena.DTO.LoginRequest;
import Project_Noir.Athena.DTO.RegisterRequest;
import Project_Noir.Athena.Model.AuthenticationResponse;
import Project_Noir.Athena.Repo.UserRepository;
import Project_Noir.Athena.Service.AuthenticationService;
import Project_Noir.Athena.Service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/Auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @GetMapping("/Exist/Username/{username}")
    // @dev Generates a new user entity
    public ResponseEntity<Boolean> usernameExist(@PathVariable String username){
        return new ResponseEntity<>(userRepository.findByUsernameIgnoreCase(username).isPresent(),HttpStatus.OK);
    }

    @GetMapping("/Exist/Email/{email}")
    // @dev Generates a new user entity
    public ResponseEntity<Boolean> emailExist(@PathVariable String email){
        return new ResponseEntity<>(userRepository.findByEmailIgnoreCase(email).isPresent(),HttpStatus.OK);
    }

    @PostMapping("/Register")
    // @dev Generates a new user entity
    public ResponseEntity<String> register(@RequestBody @Valid RegisterRequest request){
        authenticationService.registerUser(request);
        return new ResponseEntity<String>("Account Registered",HttpStatus.CREATED);
    }

    @GetMapping("/AccountVerified/{secret}")
    // @dev Sets user to enabled
    public ResponseEntity<AuthenticationResponse> enableUser(@PathVariable String secret){
        return new ResponseEntity<>(authenticationService.fetchUserAndEnable(secret), HttpStatus.OK);
    }

    @GetMapping("/ResendActivationEmail/{email}")
    // @dev Sets user to enabled
    public ResponseEntity<String> resendActivationEmail(@PathVariable String email){
        authenticationService.resendActivationEmail(email);
        return new ResponseEntity<>("Activation Email resent", HttpStatus.OK);
    }

    @GetMapping("/ResendForgotPasswordEmail/{email}")
    // @dev Sets user to enabled
    public ResponseEntity<String> resendForgotPasswordEmail(@PathVariable String email){
        authenticationService.resendForgotPasswordEmail(email);
        return new ResponseEntity<>("Forgot Password Email resent", HttpStatus.OK);
    }

    @PutMapping("/ForgotPassword")
    // @dev Sets user to enabled
    public ResponseEntity<AuthenticationResponse> forgotPassword(@RequestBody @Valid ForgotPasswordRequest forgotPasswordRequest){
        return new ResponseEntity<>(authenticationService.forgotPassword(forgotPasswordRequest), HttpStatus.OK);
    }

    @PostMapping("/Login")
    // @dev Generates a JWT using Sivantis credentials
    public ResponseEntity<AuthenticationResponse> login(@RequestBody LoginRequest loginRequest) throws Exception {
        return new ResponseEntity<>(authenticationService.login(loginRequest), HttpStatus.OK);
    }


    @PostMapping("/GoogleLogin")
    // @dev Generates JWT using Google credentials
    public ResponseEntity<AuthenticationResponse> googleLogin(@RequestBody GoogleLoginRequest googleLoginRequest) throws Exception {
        return new ResponseEntity<>(authenticationService.googleLogin(googleLoginRequest), HttpStatus.OK);
    }

    @GetMapping("/Refresh/{ExpiredJWT}")
    // @dev Generates refresh JWT
    public ResponseEntity<AuthenticationResponse> RefreshToken(@PathVariable String ExpiredJWT){
        return new ResponseEntity<>(
            AuthenticationResponse.builder()
                .jwtToken(jwtService.generateRefreshToken(ExpiredJWT))
                .build(),
            HttpStatus.OK);
    }



}
