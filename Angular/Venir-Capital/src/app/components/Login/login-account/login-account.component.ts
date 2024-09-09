import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthenticationService, GoogleLoginPayload } from 'src/app/services/Auth/authentication.service';
import { UserService } from 'src/app/services/User/user-service';
import { GoogleAPIService } from 'src/app/services/GoogleAPI/google-api.service';
import { AuthenticationResponse } from 'src/app/models/AuthenticationResponse/AuthenticationResponse';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { TwoFactorAuthenticationComponent } from '../../Accessories/Popups/two-factor-authentication/two-factor-authentication.component';
import { EmailPopupComponent } from '../../CreateAccount/email-popup/email-popup.component';
import { ForgotPasswordPopupComponent } from '../forgot-password-popup/forgot-password-popup.component';
import { Subscription } from 'rxjs';
import { ReCaptchaV3Service } from 'ng-recaptcha';
import { AccountSignUpComponent } from '../../CreateAccount/account-sign-up/account-sign-up.component';

@Component({
  selector: 'app-login-account',
  templateUrl: './login-account.component.html',
  styleUrls: ['./login-account.component.css']
})
export class LoginAccountComponent implements OnDestroy{
  logo = "assets/Logo.png"
  GoogleLogo = "assets/GoogleLogo.png"
  showError = false
  loginForm: FormGroup = new FormGroup({});
  redirectURL: any;
  showPassword = false
  recaptchaSubscription?: Subscription;
  loading = false

  constructor(
    private router: Router, 
    private authService: AuthenticationService,
    private route: ActivatedRoute, 
    private userService: UserService, 
    private googleAuthService: GoogleAPIService,
    public alertService: AlertService,
    private modalService: NgbModal, 
    private recaptchaV3Service: ReCaptchaV3Service,
    private activeModal: NgbActiveModal,
    ){

    const params = this.route.snapshot.queryParams;
    if (params['redirectURL']) {
        this.redirectURL = params['redirectURL'];
    }
    localStorage.removeItem('token');
    this.loginForm = new FormGroup({
      Username: new FormControl('', Validators.required),
      Password: new FormControl('', Validators.required)
    })

  }


  ngOnDestroy() {
    if (this.recaptchaSubscription) {
      this.recaptchaSubscription.unsubscribe();
    }
  }

  createAccount(){
    this.activeModal.close('Sign Up')
    this.modalService.open(AccountSignUpComponent, { size: 'md', scrollable: true, centered: true , animation: false, })
  }



  onSumbit(){
    this.loading = true
    this.recaptchaSubscription = this.recaptchaV3Service.execute('registerCustomer').subscribe((recaptchaToken) => {
      if(recaptchaToken){
      const loginRequestPayload:LoginRequestPayload ={
          username: this.loginForm.get('Username')?.value,
          password: this.loginForm.get('Password')?.value,
          code: '',
          recaptchaToken: recaptchaToken,
        };
        this.authService.login(loginRequestPayload).subscribe({
          next: (data:AuthenticationResponse) => {
            this.loading = false
            if(data.mfaEnabled){
              const modalRef = this.modalService.open(TwoFactorAuthenticationComponent, { size: 'md', scrollable: true, centered: true , animation: false, })
              modalRef.componentInstance.loginRequestPayload = loginRequestPayload;
              modalRef.result.then(result => {
                if(result === "success"){
                  this.userService.getUserFromServer().subscribe(user => {
                    this.userService.UpdateUser(user)
                    this.activeModal.close("Logged In")
                  });
                }
              })
            }
            else if(data.invalidEmail){
              this.activeModal.close("Invalid Email")
              const modalRef = this.modalService.open(EmailPopupComponent, { size: 'md', scrollable: true, animation: false, centered: true });
              modalRef.componentInstance.email = data.email
            }
            else{
              window.localStorage.setItem('token', data.jwtToken)
              this.activeModal.close("Logged In")
              this.authService.logIn()
              this.userService.getUserFromServer().subscribe(user => {
                this.userService.UpdateUser(user)
              });
            }
          },
          error: () => {
            this.loading = false
            this.showError = true
          },
          complete: () => {
          }
        });
      }
      else{
        this.loading = false
        this.alertService.addAlert("Recaptcha failed", "danged")
      }
    })
    
  }



  /*
  Sends login information to server from Google. 
    On success, returns a JWT token and redircts user to their Previous url or '/Home'
    On failure, 
  */
  GoogleSignIn(){
    this.googleAuthService.initiateLoginFlow()
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  forgotPassword(){
    this.modalService.open(ForgotPasswordPopupComponent, { size: 'md', scrollable: true, animation: false, centered: true });
  }



}
export interface LoginRequestPayload{
  username: string;
  password: string;
  code: string;
  recaptchaToken: string
}