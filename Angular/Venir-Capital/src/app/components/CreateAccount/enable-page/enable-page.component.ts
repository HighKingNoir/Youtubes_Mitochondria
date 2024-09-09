import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthenticationResponse } from 'src/app/models/AuthenticationResponse/AuthenticationResponse';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';

@Component({
  selector: 'app-enable-page',
  templateUrl: './enable-page.component.html',
  styleUrls: ['./enable-page.component.css']
})
export class EnablePageComponent implements OnInit{
/*
  Verifies the user's email address
    On success, sets the user's 'enabled' status to true and return a JWT token
    On failure, N/A
  */
    ngOnInit(): void {
      //gets the userID from the url
      const redirectURL = localStorage.getItem("redirectURL")
      localStorage.removeItem("redirectURL")
      const secret = this.route.snapshot.params['secret'];
      this.authService.verifyUser(secret).subscribe({
        next: (data:AuthenticationResponse) => {
          window.localStorage.setItem('token', data.jwtToken)
          if(redirectURL){
            this.router.navigateByUrl(redirectURL)
          }
          else{
            this.router.navigateByUrl("")
          }
        },
        error: (error) =>{
          this.alertService.addAlert(error, "danger")
          if(redirectURL){
            this.router.navigateByUrl(redirectURL)
          }
          else{
            this.router.navigateByUrl("")
          }
        },
        complete: () =>{

        },
      })

      

  }
  
  constructor(
    private authService: AuthenticationService, 
    private route: ActivatedRoute, 
    private router: Router,
    public alertService:AlertService
  ){

  }
}
