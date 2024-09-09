import { Component } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthenticationResponse } from 'src/app/models/AuthenticationResponse/AuthenticationResponse';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService, ForgotPasswordPayload } from 'src/app/services/Auth/authentication.service';
import { matchPassword } from '../../Validators/passwordMatch.validator';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent {

  logo = "assets/Logo.png"
  showPassword = false;
  showConfirmPassword = false;
  forgotPasswordForm: FormGroup = new FormGroup({});
  errorMessage?: string;

  constructor(
    private route: ActivatedRoute,
    public alertService: AlertService,
    private authService: AuthenticationService,
    private router: Router, 
  ){
    this.forgotPasswordForm = new FormGroup({
      Password: new FormControl('', [Validators.required, Validators.pattern('^(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!*_])[A-Za-z0-9@#$%^&+=!*_]{8,20}$')]),
      ConfirmPassword: new FormControl('', [Validators.required]),
    },{validators:matchPassword});
  }


  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPasswordVisibility() {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  changePassword(){
    const forgotPasswordPayload: ForgotPasswordPayload = {
      newPassword: this.forgotPasswordForm.get('Password')?.value,
      username: '',
      secret: this.route.snapshot.params['secret']
    }
    const redirectURL = localStorage.getItem("redirectURL")
    this.authService.forgotPassword(forgotPasswordPayload).subscribe({
      next: () => {
        this.alertService.addAlert("Password Reset Successful", 'success')
        if(redirectURL){
          this.router.navigateByUrl(redirectURL)
        }
        else{
          this.router.navigateByUrl("")
        }
      },
      error: () => {
        this.errorMessage = "Invalid link. Check your email and click the latest one. If this doesn't work send another email request."
      },
      complete: () => {

      }
    })
      
  }

}
