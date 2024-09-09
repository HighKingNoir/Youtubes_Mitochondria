import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthConfig, OAuthService } from 'angular-oauth2-oidc';

import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { environment } from 'src/Environment/environment';
import { AuthenticationResponse } from 'src/app/models/AuthenticationResponse/AuthenticationResponse';
import { AuthenticationService, GoogleLoginPayload } from '../Auth/authentication.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { TwoFactorAuthenticationComponent } from 'src/app/components/Accessories/Popups/two-factor-authentication/two-factor-authentication.component';
import { EmailPopupComponent } from 'src/app/components/CreateAccount/email-popup/email-popup.component';
import { AlertService } from '../Alerts/alert.service';
import { StreamerInfo } from 'src/app/models/Channels/Channels';
import { CreateChannelService } from '../CreateChannel/create-channel.service';
import { EditChannelService } from '../EditChannel/edit-channel.service';



const oAuthConfig: AuthConfig = {
  issuer: 'https://accounts.google.com',
  strictDiscoveryDocumentValidation: false,
  redirectUri: window.location.href,
  clientId: environment.Google_ClientID,
  scope: 'https://www.googleapis.com/auth/youtube.readonly https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email'
}

@Injectable({
  providedIn: 'root'
})
export class GoogleAPIService {
  UserInfo!: UserInfo
  userProfileSubject = new BehaviorSubject<UserInfo>(this.UserInfo)

  constructor(
    private readonly oAuthService: OAuthService, 
    private router: Router,
    public alertService: AlertService,
    private modalService: NgbModal, 
    private authService:AuthenticationService,
    private createChannelService: CreateChannelService,
    private editChannelService:EditChannelService
  ) {
    oAuthService.configure(oAuthConfig)
    this.oAuthService.setupAutomaticSilentRefresh()
  }

  getYoutubeAccount(){
    this.oAuthService.scope = "https://www.googleapis.com/auth/youtube.readonly https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email"
    this.oAuthService.redirectUri = window.location.href
    this.oAuthService.loadDiscoveryDocument().then(() => {
      this.oAuthService.tryLoginImplicitFlow().then(() => {
          this.oAuthService.initLoginFlow()
      })
    })
  }

  loadYoutubeAccount(){
    this.oAuthService.scope = "https://www.googleapis.com/auth/youtube.readonly https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email"
    this.oAuthService.loadDiscoveryDocument().then(() => {
      this.oAuthService.tryLoginImplicitFlow().then(() => {
        if (this.oAuthService.hasValidAccessToken()) {
          this.oAuthService.loadUserProfile().then((userProfile) => {
            this.userProfileSubject.next(userProfile as UserInfo)
          }).catch(() => {
            this.logOut()
          });
        }
      })
    })
  }

  

  googleSignIn(){
    const oAuthLoginService = this.oAuthService;
    oAuthLoginService.scope = "https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email"
    oAuthLoginService.loadDiscoveryDocument().then(() => {
      oAuthLoginService.tryLoginImplicitFlow().then(() => {
        if(oAuthLoginService.hasValidAccessToken()){
          oAuthLoginService.loadUserProfile().then(() => {
            const googleLoginPayload: GoogleLoginPayload = {
              googleJWT:this.getIDToken(),
              code: '',
              language: navigator.language
            }
            const redirectURL = localStorage.getItem("redirectURL")
            this.authService.googleLogin(googleLoginPayload).subscribe({
              next: (data:AuthenticationResponse) => {
                if(data.newUser){
                  if(data.invalidEmail){
                    const modalRef = this.modalService.open(EmailPopupComponent, { size: 'md', scrollable: true, animation: false, centered: true });
                    modalRef.componentInstance.email = data.email
                  }
                  else{
                    localStorage.setItem('token', data.jwtToken);
                    this.oAuthService.logOut()
                    if(redirectURL){
                      this.router.navigateByUrl(redirectURL)
                    }
                    else{
                      this.router.navigateByUrl("")
                    }
                  }
                }
                else if(data.invalidEmail){
                  const modalRef = this.modalService.open(EmailPopupComponent, { size: 'md', scrollable: true, animation: false, centered: true });
                  modalRef.componentInstance.email = data.email
                }
                else if(data.mfaEnabled){
                  const modalRef = this.modalService.open(TwoFactorAuthenticationComponent, { size: 'md', scrollable: true, centered: true , animation: false, })
                  modalRef.componentInstance.googleLoginPayload = googleLoginPayload;
                }
                else{
                  localStorage.setItem('token', data.jwtToken);
                  this.oAuthService.logOut()
                  if(redirectURL){
                    this.router.navigateByUrl(redirectURL)
                  }
                  else{
                    this.router.navigateByUrl("")
                  }
                }  
              },
              error: (error) => {
                this.alertService.addAlert(error, 'danger')
                this.router.navigateByUrl("")
              },
              complete: () => {
                
              }
            });
          })
        }
      })
    })
  }

  initiateLoginFlow(){
    localStorage.setItem("redirectURL", this.router.url)
    const oAuthLoginService = this.oAuthService;
    oAuthLoginService.scope = "https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email"
    oAuthLoginService.redirectUri = environment.FrontEndURL + "Login/Google"
    oAuthLoginService.loadDiscoveryDocument().then(() => {
      oAuthLoginService.tryLoginImplicitFlow().then(() => {
        oAuthLoginService.initLoginFlow()
      })
    })
  }

  addYoutubeChannel() {
    this.oAuthService.scope = "https://www.googleapis.com/auth/youtube.readonly https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email"
    if(this.router.url.startsWith("/Change")){
      this.oAuthService.redirectUri = environment.FrontEndURL + "Edit/Channel/Youtube"
    }
    else {
      this.oAuthService.redirectUri = environment.FrontEndURL + "Create/Channel/Youtube"
    }
    this.oAuthService.loadDiscoveryDocument().then(() => {
      this.oAuthService.tryLoginImplicitFlow().then(() => {
        this.oAuthService.initLoginFlow()
      })
    })
  }

  loadYoutubeChannel(redirectURL: string){
    this.oAuthService.scope = "https://www.googleapis.com/auth/youtube.readonly https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email"
    this.oAuthService.loadDiscoveryDocument().then(() => {
      this.oAuthService.tryLoginImplicitFlow().then(() => {
        if (this.oAuthService.hasValidAccessToken()) {
          this.oAuthService.loadUserProfile().then((userProfile) => {
            const youtubeChannel = userProfile as UserInfo
            const streamerInfo:StreamerInfo = {
              streamerId: youtubeChannel.info.sub,
              platform: 'Youtube',
              username: youtubeChannel.info.name,
              averageWeeklyViewers: 0,
              youtubeChannelId: null
            }
            if(redirectURL.startsWith("/Change")){
              this.editChannelService.addStreamerInfo(streamerInfo)
              this.router.navigateByUrl(redirectURL)
            }
            else{
              this.createChannelService.addStreamerInfo(streamerInfo)
              this.router.navigateByUrl('/Create/Channel')
            }
          }).catch((error) => {
            this.errorLoadingYoutubeChannel(error, redirectURL)
          });
        }
      }).catch((error) => {
        this.errorLoadingYoutubeChannel(error, redirectURL)
      });
    }).catch((error) => {
      this.errorLoadingYoutubeChannel(error, redirectURL)
    });
  }
  

  errorLoadingYoutubeChannel(errorMessage: string, redirectURL: string){
    if(redirectURL.startsWith("/Change")){
      this.alertService.addAlert(errorMessage, "danger")
      this.router.navigateByUrl(redirectURL)
    }
    else{
      this.alertService.addAlert(errorMessage, "danger")
      this.router.navigateByUrl('/Create/Channel')
    }
  }

  
  isLoggedIn(): boolean {
    return this.oAuthService.hasValidAccessToken();   
  }

  getIDToken():string{
    return this.oAuthService.getIdToken();
  }

  getAccessToken():string{
    return this.oAuthService.getAccessToken();
  }

  logOut(){
    this.oAuthService.logOut()
  }

}

export interface UserInfo{
  info: {
    sub: string,
    email: string,
    name: string,
    picture: string
  }
}