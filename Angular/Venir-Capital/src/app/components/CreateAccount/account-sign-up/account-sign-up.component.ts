import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { EmailPopupComponent } from '../email-popup/email-popup.component';
import { AuthenticationService, GoogleLoginPayload } from 'src/app/services/Auth/authentication.service';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { UserService } from 'src/app/services/User/user-service';
import { GoogleAPIService } from 'src/app/services/GoogleAPI/google-api.service';
import { AuthenticationResponse } from 'src/app/models/AuthenticationResponse/AuthenticationResponse';
import { Router } from '@angular/router';
import { TwoFactorAuthenticationComponent } from '../../Accessories/Popups/two-factor-authentication/two-factor-authentication.component';
import { matchPassword } from '../../Validators/passwordMatch.validator';
import { ReCaptchaV3Service } from 'ng-recaptcha';
import { Subscription } from 'rxjs';
import { LoginAccountComponent } from '../../Login/login-account/login-account.component';

@Component({
  selector: 'app-account-sign-up',
  templateUrl: './account-sign-up.component.html',
  styleUrls: ['./account-sign-up.component.css']
})
export class AccountSignUpComponent implements OnInit, OnDestroy{
  logo = "assets/Logo.png"
  GoogleLogo = "assets/GoogleLogo.png"
  isChecked = false;
  showPassword = false;
  showConfirmPassword = false;
  signinForm: FormGroup = new FormGroup({});
  recaptchaSubscription?: Subscription;

  constructor(
    private modalService: NgbModal, 
    private authService: AuthenticationService,
    public alertService: AlertService,
    private router: Router, 
    private googleAuthService:GoogleAPIService,
    private recaptchaV3Service: ReCaptchaV3Service,
    private activeModal: NgbActiveModal,
  ){
    localStorage.removeItem('token');
    this.signinForm = new FormGroup({
      Username: new FormControl('', [Validators.required, Validators.pattern('^[a-zA-Z0-9_-]{3,15}$')], [this.authService.validateUsernameAvailability()]),
      Email: new FormControl('', [Validators.required, Validators.email], [this.authService.validateEmailAvailability()]),
      Password: new FormControl('', [Validators.required, Validators.pattern('^(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!*_])[A-Za-z0-9@#$%^&+=!*_]{8,20}$')]),
      ConfirmPassword: new FormControl('', [Validators.required]),
    },{validators:matchPassword});
    
  }

  ngOnInit(): void {
    this.googleAuthService.logOut()
  }

  ngOnDestroy() {
    if (this.recaptchaSubscription) {
      this.recaptchaSubscription.unsubscribe();
    }
  }

  logIn(){
    this.activeModal.close('Login')
    this.modalService.open(LoginAccountComponent, { size: 'md', scrollable: true, centered: true , animation: false, })
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPasswordVisibility() {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  onChange(event: Event): void {
    this.isChecked = (event.target as HTMLInputElement).checked;
  }


  GoogleSignIn(){
    this.googleAuthService.initiateLoginFlow()
    
  }

  /*
  Registers user information
    On success, creates new user
    On failure, N/A
  */
  CreateAccount(){
    this.recaptchaSubscription = this.recaptchaV3Service.execute('registerCustomer').subscribe((recaptchaToken) => {
      if(recaptchaToken){
        const signUpRequestPayload: SignUpRequestPayload = {
          email: this.signinForm.get('Email')?.value,
          password: this.signinForm.get('Password')?.value,
          username: this.signinForm.get('Username')?.value,
          language: navigator.language,
          recaptchaToken: recaptchaToken,
        }
        this.authService.signup(signUpRequestPayload).subscribe({
          next: () => {},
          error: (err) => {
            this.alertService.addAlert(err, 'danger')
          },
          complete: () => {
            this.signinForm.reset()
            localStorage.setItem("redirectURL", this.router.url)
            const modalRef = this.modalService.open(EmailPopupComponent, { size: 'md', scrollable: true, animation: false, centered: true });
            modalRef.componentInstance.email = signUpRequestPayload.email
          }
        })
      }
      
      else{
        this.alertService.addAlert("Recaptcha failed", "danged")
      }


    })
    
      
  }

}


export interface SignUpRequestPayload {
  email: string;
  username: string;
  password: string;
  language: string,
  recaptchaToken: string
}