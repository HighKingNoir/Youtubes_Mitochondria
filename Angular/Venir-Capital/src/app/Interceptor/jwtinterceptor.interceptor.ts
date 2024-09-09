import { Injectable } from '@angular/core';
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor, HTTP_INTERCEPTORS, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError  } from 'rxjs';
import { AuthenticationService } from '../services/Auth/authentication.service';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { UserService } from '../services/User/user-service';
import { GoogleAPIService } from '../services/GoogleAPI/google-api.service';
import { environment } from 'src/Environment/environment'
import { AuthenticationResponse } from '../models/AuthenticationResponse/AuthenticationResponse';
import { AlertService } from '../services/Alerts/alert.service';

@Injectable()
export class JWTinterceptorInterceptor implements HttpInterceptor {
  SivantisURL:string = environment.BackendURL!
  

  constructor(
    private authService: AuthenticationService, 
    private router: Router, 
    private alertService: AlertService
    ) {

  }

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = localStorage.getItem('token')!
    if(this.isLoggedIn() && this.isInternal(request.url) && !this.toAuthController(request.url)){
      //Inserts JWT token into Authorization header
      request = request.clone({
        headers: request.headers
        .set('Authorization', "Bearer " + token)
      })

    }
    return next.handle(request).pipe(
      
      catchError((error: HttpErrorResponse) => {
        //Expired Token
        if(error.status === 400){
          const jsonString = error.error
          try {
            const jsonObject = JSON.parse(jsonString);
            if (typeof jsonObject === 'object' && jsonObject !== null) {
              const message = jsonObject.message;
            return throwError(() => new Error(message))
            }
          } catch (err) {
            return throwError(() => new Error(error.error.message))
          }
          
        }
        if (error.status === 401) {
          this.authService.refreshToken(token).subscribe({
            next: (data:AuthenticationResponse) => {
              localStorage.setItem('token', data.jwtToken)
              request = request.clone({
                headers: request.headers.set('Authorization', "Bearer " + data.jwtToken),
              })
              window.location.reload()
            },
            error: (error) => {
              this.alertService.addAlert(error, "danger")
              this.router.navigateByUrl('/')   
            },
            complete: () => {
              return next.handle(request)
            }
          })
        }
         //Any other errors will be thrown
        if(error.status === 403 && this.isInternal(request.url)){
          this.router.navigateByUrl('/')
          this.alertService.addAlert(error.error.error.message, "danger")
        }
        if (error && error.error && error.error.message) {
           return throwError(() => new Error(error.error.message))
        } else {
          return throwError(() => new Error(error.message))
        }
    }));
}
  
  //Checks that the token in local storage is not null
  private isLoggedIn(): boolean{
    if(localStorage.getItem('token')){
      return true
    }
    return false
  }

  private isInternal(url:string): boolean{
    return url.startsWith(this.SivantisURL)
     
  }

  private toAuthController(url:string): boolean{
    return url.startsWith(this.SivantisURL + "Auth")
  }


  
}
export const JWTinterceptorProvider = {
  provide: HTTP_INTERCEPTORS,
  useClass: JWTinterceptorInterceptor,
  multi: true
}
