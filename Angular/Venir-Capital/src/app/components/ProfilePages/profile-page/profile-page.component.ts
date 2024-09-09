import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AuthenticationResponse } from 'src/app/models/AuthenticationResponse/AuthenticationResponse';
import { User } from 'src/app/models/Users/User';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { UserService, UsernameChangePayload } from 'src/app/services/User/user-service';
import { ConfirmationComponent } from '../../Accessories/Popups/confirmation/confirmation.component';
import { TwoFactorAuthenticationComponent } from '../../Accessories/Popups/two-factor-authentication/two-factor-authentication.component';

@Component({
  selector: 'app-profile-page',
  templateUrl: './profile-page.component.html',
  styleUrls: ['./profile-page.component.css']
})
export class ProfilePageComponent {

  usernameForm: FormGroup;
  username = ''
  userEmail = ''

  constructor(
    public alertService:AlertService,
    private userService: UserService,
    private authService: AuthenticationService,
    private router: Router,
    private modalService: NgbModal,
  ){
    this.usernameForm = new FormGroup({
      username: new FormControl('', [Validators.required, Validators.pattern('^[a-zA-Z0-9_-]{3,15}$')], [this.authService.validateUsernameAvailability()]),
    });
    if(this.userService.getUserID() === ''){
      this.userService.userInfo$.subscribe(() => {
        this.username = this.userService.getUsername()
        this.userEmail = this.userService.getUserEmail()
        this.usernameForm.get("username")?.setValue(this.username)
      })
    }
    else{
      this.username = this.userService.getUsername()
      this.userEmail = this.userService.getUserEmail()
      this.usernameForm.get("username")?.setValue(this.username)
    }
  }


  changeUsername(){
    const JWT = window.localStorage.getItem('token') || ''
    this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
      const usernameChangePayload: UsernameChangePayload = {
        
        username: this.usernameForm.get("username")?.value,
        code: ''
      }
      this.userService.changeUsername(usernameChangePayload).subscribe({
        next: (data:AuthenticationResponse) => {
          if(data.mfaEnabled){
            const modalRef = this.modalService.open(TwoFactorAuthenticationComponent, { size: 'md', scrollable: true, centered: true , animation: false, })
            modalRef.componentInstance.usernameChangePayload = usernameChangePayload;
            modalRef.result.then(result => {
              if(result === "success"){
                this.username = this.usernameForm.get("username")?.value
                this.alertService.addAlert("Username Successfully Changed","success")
              }
            })
          }
          else{
            this.username = this.usernameForm.get("username")?.value
            window.localStorage.setItem('token', data.jwtToken)
            this.alertService.addAlert("Username Successfully Changed","success")
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



  deleteUser(){
    const modalRef = this.modalService.open(ConfirmationComponent, {size: 'sm', scrollable: true,centered: true , animation: false})
    modalRef.componentInstance.message = "Are you sure you want to delete your account?"
    modalRef.result.then(result =>{
      if(result === 'continue'){
        const JWT = window.localStorage.getItem('token') || ''
        this.authService.checkJWTExpiration(JWT).then((JWTResult) =>{
          const code = ''
          this.userService.deleteUser(code).subscribe({
            next: (data:AuthenticationResponse) => {
              if(data.mfaEnabled){
                const modalRef = this.modalService.open(TwoFactorAuthenticationComponent, { size: 'md', scrollable: true, centered: true , animation: false, })
                modalRef.componentInstance.code = code;
                modalRef.componentInstance.deleteUserBoolean = true;
                modalRef.result.then(result => {
                  if(result === "success"){
                    this.router.navigateByUrl('')
                    this.alertService.addAlert("Deletion Request Successful. Login within 30 days to undo Deletion Request","success")
                  }
                })
              }
              else{
                this.router.navigateByUrl('')
                this.alertService.addAlert("Account Successfully Deleted","success")
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
    })
  }
}

