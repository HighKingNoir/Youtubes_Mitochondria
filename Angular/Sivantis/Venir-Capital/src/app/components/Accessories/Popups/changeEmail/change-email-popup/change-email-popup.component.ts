import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AuthenticationResponse } from 'src/app/models/AuthenticationResponse/AuthenticationResponse';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ChangeEmailRequestPayload, UserService } from 'src/app/services/User/user-service';
import { TwoFactorAuthenticationComponent } from '../../two-factor-authentication/two-factor-authentication.component';

@Component({
  selector: 'app-change-email-popup',
  standalone: false,
  templateUrl: './change-email-popup.component.html',
  styleUrl: './change-email-popup.component.css'
})
export class ChangeEmailPopupComponent {
  isPhase1: boolean = true;
  currentEmail: String = ''
  newEmailForm: FormGroup = new FormGroup({});
  

  constructor(
    public activeModal: NgbActiveModal,
    private alertService: AlertService,
    private authService: AuthenticationService,
    private userService: UserService,
    private modalService: NgbModal, 
  ){
    this.newEmailForm = new FormGroup({
      newEmail: new FormControl(
        '',
        {
          validators: [Validators.required, Validators.email],
          asyncValidators: [this.authService.validateEmailAvailability()],
          updateOn: 'blur'   
        }
      ),
      currentEmailVerificationCode: new FormControl(
        '',
        {
          validators: [Validators.required, Validators.pattern('^[a-zA-Z0-9]{6}$')],
          updateOn: 'blur'   
        }
      ),
      newEmailVerificationCode: new FormControl(
        '',
        {
          validators: [Validators.required, Validators.pattern('^[a-zA-Z0-9]{6}$')],
          updateOn: 'blur'   
        }
      ),
    });
    this.currentEmail = this.userService.getUserEmail()
  }

  back(){
    this.isPhase1 = true;
  }

  next(){
    const newEmail = this.newEmailForm.get('newEmail')
    if(!newEmail?.invalid){
      this.isPhase1 = false;
      const JWT = localStorage.getItem('token') || ''
      this.authService.checkJWTExpiration(JWT).then(() => {
        this.userService.changeEmailInitiation(newEmail?.value).subscribe({
          next: () => {
          },
          error: (err:HttpErrorResponse) => {
            this.alertService.addAlert(err.message, "danger")
          },
          complete: () => {}
        });
      })
    }
  }

  submitNewEmail(){
    const currentEmailVerificationCode = this.newEmailForm.get('currentEmailVerificationCode')
    const newEmailVerificationCode = this.newEmailForm.get('newEmailVerificationCode')
    if(!currentEmailVerificationCode?.invalid && !newEmailVerificationCode?.invalid){
    const JWT = localStorage.getItem('token') || ''
      this.authService.checkJWTExpiration(JWT).then(() => {
        const changeEmailRequestPayload: ChangeEmailRequestPayload = {
          currentEmailVerificationCode: currentEmailVerificationCode?.value,
          newEmailVerificationCode: newEmailVerificationCode?.value,
          code: ''
        }
        this.userService.changeEmail(changeEmailRequestPayload).subscribe({
          next: (data: AuthenticationResponse) => {
            if(data.mfaEnabled){
              const modalRef = this.modalService.open(TwoFactorAuthenticationComponent, { size: 'md', scrollable: true, centered: true , animation: false, })
              modalRef.componentInstance.changeEmailRequestPayload = changeEmailRequestPayload;
              modalRef.result.then(result => {
                if(result === "success"){
                  this.alertService.addAlert("Email Successfully Changed", "success")
                  this.userService.setEmail(this.newEmailForm.get('newEmail')?.value)
                  this.activeModal.close("Email Changed")
                }
              })
            }
            else {
              this.alertService.addAlert("Email Successfully Changed", "success")
              this.userService.setEmail(this.newEmailForm.get('newEmail')?.value)
              this.activeModal.close("Email Changed")
            }
          },
          error: (err:HttpErrorResponse) => {
            this.alertService.addAlert(err.message, "danger")
          },
          complete: () => {}
        });
      })
    }
  }

}
