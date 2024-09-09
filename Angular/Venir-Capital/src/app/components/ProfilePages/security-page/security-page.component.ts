import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { PasswordChangePayload, UserService } from 'src/app/services/User/user-service';
import { TwoFactorAuthenticationComponent } from '../../Accessories/Popups/two-factor-authentication/two-factor-authentication.component';
import { AuthenticationResponse } from 'src/app/models/AuthenticationResponse/AuthenticationResponse';
import { matchNewPassword } from '../../Validators/passwordMatch.validator';


@Component({
  selector: 'app-security-page',
  templateUrl: './security-page.component.html',
  styleUrls: ['./security-page.component.css']
})
export class SecurityPageComponent {
  passwordForm: FormGroup;
  tfaStatus = false;

  showCurrentPassword = false;
  showNewPassword = false;
  showRetryNewPassword = false;

  constructor(
    public alertService:AlertService,
    private fb: FormBuilder,
    private userService: UserService,
    private authService: AuthenticationService,
    private modalService: NgbModal,
  ){
    this.passwordForm = this.fb.group({
      currentPassword: ['', [Validators.required, Validators.pattern('^(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!*_])[A-Za-z0-9@#$%^&+=!*_]{8,20}$')]],
      newPassword:['', [Validators.required, Validators.pattern('^(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!*_])[A-Za-z0-9@#$%^&+=!*_]{8,20}$')]],
      confirmNewPassword:['', [Validators.required]],
  },{validators:matchNewPassword});

    if(this.userService.getUserID() === ''){
      this.userService.userInfo$.subscribe(() => {
        this.tfaStatus = this.userService.getMfaEnabled();
      })
    }
    else{
      this.tfaStatus = this.userService.getMfaEnabled();
    }
  }

  toggleCurrentPasswordVisibility() {
    this.showCurrentPassword = !this.showCurrentPassword;
  }

  toggleNewPasswordVisibility() {
    this.showNewPassword = !this.showNewPassword;
  }

  toggleRetryNewPasswordVisibility() {
    this.showRetryNewPassword = !this.showRetryNewPassword;
  }

  
  changePassword(){
    const JWT = window.localStorage.getItem('token') || ''
    this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
      const passwordChangePayload:PasswordChangePayload = {
        
        code: '',
        newPassword: this.passwordForm.get('newPassword')?.value,
        currentPassword: this.passwordForm.get('currentPassword')?.value
      }
      this.userService.changePassword(passwordChangePayload).subscribe({
        next: (data:AuthenticationResponse) => {
          if(data.mfaEnabled){
            const modalRef = this.modalService.open(TwoFactorAuthenticationComponent, { size: 'md', scrollable: true, centered: true , animation: false, })
            modalRef.componentInstance.passwordChangePayload = passwordChangePayload;
            modalRef.result.then(result => {
              if(result === "success"){
                this.alertService.addAlert("Password Successfully Changed","success")
                this.passwordForm.get('newPassword')?.reset()
                this.passwordForm.get('currentPassword')?.reset()
                this.passwordForm.get('confirmNewPassword')?.reset()
              }
            })
          }
          else{
            this.alertService.addAlert("Password Successfully Changed","success")
            this.passwordForm.get('newPassword')?.reset()
            this.passwordForm.get('currentPassword')?.reset()
            this.passwordForm.get('confirmNewPassword')?.reset()
          }
        },
        error: (err) => {
            this.alertService.addAlert(err, "danger")
        },
        complete: () => {
        },
      })
    })


  }


  toggle2FA(){
    const JWT = window.localStorage.getItem('token') || ''
    this.authService.checkJWTExpiration(JWT).then((JWTResult) =>{
      const code = ''
      this.userService.toggle2FA(code).subscribe({
        next: (data:AuthenticationResponse) => {
          if(data.mfaEnabled){
            const modalRef = this.modalService.open(TwoFactorAuthenticationComponent, { size: 'md', scrollable: true, centered: true , animation: false, })
            modalRef.componentInstance.code = code;
            modalRef.result.then(result => {
              if(result === "Disabled"){
                this.tfaStatus = false
              }
            })
          }
          else{
            const modalRef = this.modalService.open(TwoFactorAuthenticationComponent, { size: 'md', scrollable: true, centered: true , animation: false, })
            modalRef.componentInstance.secretImageUrl = data.secretImageUri
            modalRef.componentInstance.code = code;
            modalRef.result.then(result => {
              if(result === "Enabled"){
                this.tfaStatus = true
              }
            })
          }
        },
        error: (err) => {
            this.alertService.addAlert(err, "danger")
        },
        complete: () => {
            
        },
      })
    })
  }


  }

