import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AsyncValidatorFn, AbstractControl, ValidationErrors } from '@angular/forms';
import { map, Observable, of, Subject } from 'rxjs';
import { SignUpRequestPayload } from 'src/app/components/CreateAccount/account-sign-up/account-sign-up.component';
import { LoginRequestPayload } from 'src/app/components/Login/login-account/login-account.component';
import { environment } from 'src/Environment/environment';
import { catchError, debounceTime, switchMap } from 'rxjs/operators';
import { JwtHelperService } from '@auth0/angular-jwt';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {

  SivantisURL:string = environment.BackendURL + "Auth"
  private isLoggedInSubject = new Subject<boolean>()
  isLoggedInProfile$ = this.isLoggedInSubject.asObservable();
  private redirectList: string[] = ['/', '/Innovations', '/Inventions', '/ShortFilms', '/UpcomingReleases', '/MostHyped', '/Movies', '/Sports', '/Search', '/Channels', '/LeaderBoard', '/Concerts', '/Subscriptions/Channels']
  constructor(
    private httpClient: HttpClient,
    private jwtHelper: JwtHelperService,
    private router: Router
  ) {
    
  }

  logIn(){
    this.isLoggedInSubject.next(true)
  }

  logOut(){
    window.localStorage.clear();
    this.isLoggedInSubject.next(false)
    const currentUrl = this.router.url;
    const trimmedUrl = this.trimUrl(currentUrl);
    if(!this.redirectList.includes(trimmedUrl) && !trimmedUrl.startsWith("/Buy") && !trimmedUrl.startsWith("/Auction") && !trimmedUrl.startsWith("/Channel")){
      this.router.navigate(['']);
    }
    else {
        const urlTree = this.router.parseUrl(currentUrl);
        this.router.navigate([urlTree.root.children['primary'].segments.map(segment => segment.path).join('/')], {
          queryParams: urlTree.queryParams
        });
      
    }
  }




  private trimUrl(url: string): string {
    const urlTree = this.router.parseUrl(url);
    const primarySegmentGroup = urlTree.root.children['primary'];
    if (primarySegmentGroup) {
      const primarySegmentPath = primarySegmentGroup.segments.map(segment => segment.path).join('/');
      return `/${primarySegmentPath}`;
    }
    return url;
  }

  //Creates a new user
  signup(SignUpRequestPayload: SignUpRequestPayload): Observable<any>{
    return this.httpClient.post(this.SivantisURL + '/Register', SignUpRequestPayload, {responseType: 'text'});
  }

  validateUsernameAvailability(): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> => {
      // Debounce user input for 300 milliseconds to reduce API requests
      return control.valueChanges.pipe(
        debounceTime(500),
        switchMap((username: string) => {
          if (!username || username === '') {
            // Username is empty; no need to make a request
            return of(null);
          }
          // Make an HTTP request to check username availability
          return this.httpClient.get(`${this.SivantisURL}/Exist/Username/${username}`).pipe(
            map((notAvailable) => {
              if (notAvailable) {
                control.setErrors({ usernameTaken: true })
                return of({ usernameTaken: true }); // Username is already taken
              } else {
                return of(null); // Username is availab
              }
            }),
            catchError(() => of(null)) // Handle errors gracefully
          );
        })
      );
    };
  }

  validateEmailAvailability(): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> => {
      // Debounce user input for 300 milliseconds to reduce API requests
      return control.valueChanges.pipe(
        debounceTime(300),
        switchMap((email: string) => {
          if (!email || email === '') {
            // Username is empty; no need to make a request
            return of(null);
          }
          // Make an HTTP request to check username availability
          return this.httpClient.get(`${this.SivantisURL}/Exist/Email/${email}`).pipe(
            map((notAvailable) => {
              if (notAvailable) {
                control.setErrors({ emailTaken: true })
                return of({ emailTaken: true }); // Username is already taken
              } else {
                return of(null); // Username is availab
              }
            }),
            catchError(() => of(null)) // Handle errors gracefully
          );
        })
      );
    };
  }

  
  resendActivationEmail(email:string) : Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/ResendActivationEmail/${email}`, {responseType: "text"});
  }

  resendForgotPasswordEmail(email:string) : Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/ResendActivationEmail/${email}`, {responseType: "text"});
  }

  forgotPassword(forgotPasswordPayload:ForgotPasswordPayload) : Observable<any>{
    return this.httpClient.put(`${this.SivantisURL}/ForgotPassword`,forgotPasswordPayload, {responseType: "json"});
  }

  //Logins in a user
  login(LoginRequestPayload: LoginRequestPayload) : Observable<any>{
    return this.httpClient.post(this.SivantisURL + '/Login', LoginRequestPayload, {responseType: "json"});
  }

  //Logins in a user using Google single sign up
  googleLogin(googleLoginPayload: GoogleLoginPayload) : Observable<any>{
    return this.httpClient.post(`${this.SivantisURL}/GoogleLogin` , googleLoginPayload, {responseType: "json"});
  }

  //Returns a new JWT token
  refreshToken(Token: string): Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/Refresh/${Token}`, {responseType: 'json'});
  }


  async checkJWTExpiration(JWT: string) {
    return new Promise<string>((resolve) => {
      if(JWT === ''){
        resolve(JWT);
      }
      else{
        if (this.isTokenExpired(JWT)) {
          this.refreshToken(JWT).subscribe((newJWT) => {
            localStorage.setItem('token', newJWT);
            resolve(newJWT);
          })
        } else {
          resolve(JWT);
        }
      }
    });
  }
  

  isTokenExpired(token: string): boolean {
    return this.jwtHelper.isTokenExpired(token);
  }
  
  //Sets the user's 'enabled' status in the Database to 'true'
  verifyUser(ID:string): Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/AccountVerified/${ID}`, {responseType: 'json'});
  }

}

export interface ForgotPasswordPayload{
  username: string;
  newPassword: string | null
  secret: string
}

export interface GoogleLoginPayload{
  googleJWT: string;
  code: string;
  language: string
}


