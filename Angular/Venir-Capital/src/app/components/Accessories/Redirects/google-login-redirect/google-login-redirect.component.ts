import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { GoogleAPIService } from 'src/app/services/GoogleAPI/google-api.service';

@Component({
  selector: 'app-google-login-redirect',
  templateUrl: './google-login-redirect.component.html',
  styleUrls: ['./google-login-redirect.component.css']
})
export class GoogleLoginRedirectComponent implements OnInit{
  showButton = false;

  constructor(
    private googleAuthService: GoogleAPIService,
    private router: Router,
  ){

  }

  ngOnInit(): void {
    this.googleAuthService.googleSignIn()
    setTimeout(() => {
      this.showButton = true;
    }, 3000);
  }

  back(){
    const redirectURL = localStorage.getItem("redirectURL")
    localStorage.removeItem("redirectURL")
    if(redirectURL){
      this.router.navigateByUrl(redirectURL)
    }
    else{
      this.router.navigateByUrl("")
    }
  }
}
