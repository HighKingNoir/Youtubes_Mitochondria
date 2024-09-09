import { Component, Input, OnInit } from '@angular/core';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';

@Component({
  selector: 'app-email-popup',
  templateUrl: './email-popup.component.html',
  styleUrls: ['./email-popup.component.css']
})
export class EmailPopupComponent implements OnInit {
  isResendDisabled = false;
  remainingTime = 60;
  
  @Input() email = '';


  constructor (private authService:AuthenticationService){

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

  resendEmail() {
    this.isResendDisabled = true;
    this.startResendTimer(this.remainingTime);
    this.authService.resendActivationEmail(this.email).subscribe(() => {
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
