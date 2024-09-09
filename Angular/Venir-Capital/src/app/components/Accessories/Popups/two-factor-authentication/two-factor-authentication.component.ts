import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { LoginRequestPayload } from 'src/app/components/Login/login-account/login-account.component';
import { AuthenticationResponse } from 'src/app/models/AuthenticationResponse/AuthenticationResponse';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService, GoogleLoginPayload } from 'src/app/services/Auth/authentication.service';
import { PasswordChangePayload, PersonalWalletPayload, UserService, UsernameChangePayload } from 'src/app/services/User/user-service';

@Component({
  selector: 'app-two-factor-authentication',
  templateUrl: './two-factor-authentication.component.html',
  styleUrls: ['./two-factor-authentication.component.css']
})
export class TwoFactorAuthenticationComponent {
  private otp = '';
  inputDigitLeft = "Enter code";
  btnStatus = "btn-gray";

  public configOptions = {
    length: 6,
    inputClass: 'digit-otp'
  }

  @Input() deleteUserBoolean = false;
  @Input() secretImageUrl?: string;
  @Input() code?: string;
  @Input() googleLoginPayload?: GoogleLoginPayload;
  @Input() usernameChangePayload?: UsernameChangePayload
  @Input() passwordChangePayload?: PasswordChangePayload
  @Input() personalWalletPayload?: PersonalWalletPayload
  @Input() loginRequestPayload?: LoginRequestPayload


  constructor(
    private userService: UserService,
    public alertService: AlertService,
    public activeModal: NgbActiveModal,
    private authService: AuthenticationService,
    private router: Router,
  ){

  }

  onOtpChange(event: any){
    this.otp = event;
    if(this.otp.length < this.configOptions.length) {
      this.inputDigitLeft = this.configOptions.length - this.otp.length + " digits left";
      this.btnStatus = "btn-gray"
    }
    if(this.otp.length == this.configOptions.length) {
      this.inputDigitLeft = "Send Code";
      this.btnStatus = "btn-green"
      this.verifyCode()
    }
  }

  verifyCode(){
    if(this.inputDigitLeft === "Send Code"){
      const JWT = localStorage.getItem('token') || ''
      this.authService.checkJWTExpiration(JWT).then(() => {
        if(this.loginRequestPayload){
          this.loginRequestPayload.code = this.otp;
          this.loginUser(this.loginRequestPayload)
        }
        else if(this.code !== undefined){
          if(this.deleteUserBoolean){
            this.deleteUser(this.otp)
          }
          else{
            this.toggle2FA(this.otp)
          }
        }
        else if(this.googleLoginPayload){
          this.googleLoginPayload.code = this.otp;
          this.googleLogin(this.googleLoginPayload)
        }
        else if(this.usernameChangePayload){
          this.usernameChangePayload.code = this.otp;
          this.changeUsername(this.usernameChangePayload)
        }
        else if(this.passwordChangePayload){
          this.passwordChangePayload.code = this.otp
          this.changePassword(this.passwordChangePayload)
        }
        else if(this.personalWalletPayload){
          this.personalWalletPayload.code = this.otp
          this.changePersonalWallet(this.personalWalletPayload)
        }


      })
    }
 
  }

  loginUser(loginRequestPayload:LoginRequestPayload){
    this.authService.login(loginRequestPayload).subscribe({
      next: (data:AuthenticationResponse) => {
        window.localStorage.setItem('token', data.jwtToken)
        this.authService.logIn()
        this.activeModal.close('success')
      },
      error: () => {
        this.inputDigitLeft = "Wrong Code";
        this.btnStatus = "btn-red"
      },
      complete: () => {}
    })
  }

  deleteUser(code: string){
    this.userService.deleteUser(code).subscribe({
      next: () => {
        this.activeModal.close('success')
      },
      error: (err) => {
        this.inputDigitLeft = "Wrong Code";
        this.btnStatus = "btn-red"
        this.alertService.addAlert(err, "danger")
      },
      complete: () => {

      }
    })
  }

  toggle2FA(code: string){
    this.userService.toggle2FA(code).subscribe({
      next: (data) => {
        const authResponse:AuthenticationResponse = data;
        if(authResponse.mfaEnabled){
          this.alertService.addAlert("Two Factor Authentication Enabled", "success")
          this.activeModal.close("Enabled")
        }
        else{
          this.alertService.addAlert("Two Factor Authentication Disabled", "success")
          this.activeModal.close("Disabled")
        }
      },
      error: () => {
        this.inputDigitLeft = "Wrong Code";
        this.btnStatus = "btn-red"
      },
      complete: () => {

      }
    })
  }

  googleLogin(googleLoginPayload:GoogleLoginPayload){
    const redirectURL = localStorage.getItem("redirectURL")
    this.authService.googleLogin(googleLoginPayload).subscribe({
      next: (data) => {
        localStorage.setItem('token', data.jwtToken);
        this.authService.logIn()
        this.activeModal.close('success')
        if(redirectURL){
          this.router.navigateByUrl(redirectURL)
        }
        else{
          this.router.navigateByUrl("")
        }
      },
      error: () => {
        this.inputDigitLeft = "Wrong Code";
        this.btnStatus = "btn-red"
      },
      complete: () => {

      }
    })
  }

  changeUsername(usernameChangePayload:UsernameChangePayload){
    this.userService.changeUsername(usernameChangePayload).subscribe({
      next: (data) => {
        localStorage.setItem('token', data.jwtToken);
        this.authService.logIn()
        this.activeModal.close('success')
      },
      error: () => {
        this.inputDigitLeft = "Wrong Code";
        this.btnStatus = "btn-red"
      },
      complete: () => {

      }
    })
  }

  changePassword(passwordChangePayload:PasswordChangePayload){
    this.userService.changePassword(passwordChangePayload).subscribe({
      next: () => {
        this.activeModal.close('success')
      },
      error: () => {
        this.inputDigitLeft = "Wrong Code";
        this.btnStatus = "btn-red"
      },
      complete: () => {

      }
    })
  }

  changePersonalWallet(personalWalletPayload:PersonalWalletPayload){
    this.userService.setUserPersonalWallet(personalWalletPayload).subscribe({
      next: () => {
        this.activeModal.close('success')
      },
      error: () => {
        this.inputDigitLeft = "Wrong Code";
        this.btnStatus = "btn-red"
      },
      complete: () => {

      }
    })
  }


}
