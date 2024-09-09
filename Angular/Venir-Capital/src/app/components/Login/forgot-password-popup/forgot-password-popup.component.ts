import { Component, Input, OnInit } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthenticationResponse } from 'src/app/models/AuthenticationResponse/AuthenticationResponse';
import { AuthenticationService, ForgotPasswordPayload } from 'src/app/services/Auth/authentication.service';

@Component({
  selector: 'app-forgot-password-popup',
  templateUrl: './forgot-password-popup.component.html',
  styleUrls: ['./forgot-password-popup.component.css']
})
export class ForgotPasswordPopupComponent implements OnInit {
  isResendDisabled = false;
  remainingTime = 60;
  usernameForm: FormGroup;
  invalidUsername = false

  @Input() email = '';


  constructor(
    private authService:AuthenticationService,
    private router: Router,
  ){
    localStorage.setItem("redirectURL", this.router.url)
    this.usernameForm = new FormGroup({
      username: new FormControl('', [Validators.required, Validators.pattern('^[A-Za-z0-9#]{3,20}$')]),
    });
  }
  
  ngOnInit(): void {
    // Check if the user should still wait (e.g., after a page reload)
    const lastSentTimestamp = localStorage.getItem('lastSentTimestamp');
    if (lastSentTimestamp) {
      const currentTime = Math.floor(Date.now() / 1000); // Current time in seconds
      const elapsedTime = currentTime - parseInt(lastSentTimestamp, 10);
      if (elapsedTime < this.remainingTime) {
        this.startResendTimer(this.remainingTime - elapsedTime);
      }
    }
  }

  submit(){
    const forgotPasswordPayload:ForgotPasswordPayload = {
      username: this.usernameForm.get('username')?.value,
      newPassword: null,
      secret: '',
    }
    this.authService.forgotPassword(forgotPasswordPayload).subscribe({
      next: (data:AuthenticationResponse) => {
        console.log(data)
        this.email = data.email
      },
      error: () => {
        this.invalidUsername = true
      },
      complete: () => {
      }
    })
  }

  resendEmail() {
    this.isResendDisabled = true;
    this.startResendTimer(this.remainingTime);
    this.authService.resendForgotPasswordEmail(this.email).subscribe(() => {
    })
    setTimeout(() => {
      this.isResendDisabled = false;
      localStorage.setItem('lastSentTimestamp', Math.floor(Date.now() / 1000).toString());
    }, 60000); 
  }

  private startResendTimer(seconds: number) {
    const interval = setInterval(() => {
      if (seconds > 0) {
        seconds--;
        this.remainingTime = seconds;
      } else {
        clearInterval(interval);
        this.isResendDisabled = false;
        this.remainingTime = 60;
      }
    }, 1000);
  }
}