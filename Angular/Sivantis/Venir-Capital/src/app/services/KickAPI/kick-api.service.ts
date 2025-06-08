import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { CreateChannelService } from '../CreateChannel/create-channel.service';
import { environment } from 'src/Environment/environment';
import { EditChannelService } from '../EditChannel/edit-channel.service';
import { StreamerInfo } from 'src/app/models/Channels/Channels';
import { AlertService } from '../Alerts/alert.service';

@Injectable({
  providedIn: 'root'
})
export class KickApiService {
  redirectUri = ''
  constructor(
    private http: HttpClient,
    private router: Router,
    private createChannelService: CreateChannelService,
    private editChannelService: EditChannelService,
    private alertService: AlertService
  ) { }

  getKickChannel(accessToken: string, redirectURL: string) {
    const headers = new HttpHeaders({
      Authorization: `Bearer ${accessToken}`, // Replace with your access token if required
    });
    const apiUrl = "https://api.kick.com/public/v1/users"


    this.http.get(apiUrl, { headers }).subscribe({
      next: (response: any) => {
        const streamerInfo: StreamerInfo = {
          platform: "Kick",
          streamerId: response.data[0].user_id,
          username: response.data[0].name,
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

      },
      error: (error) =>{
        if(redirectURL.startsWith("/Change")){
          this.alertService.addAlert(error, "danger")
          this.router.navigateByUrl(redirectURL)
        }
        else{
          this.alertService.addAlert(error, "danger")
          this.router.navigateByUrl('/Create/Channel')
        }
      },
      complete() {
          return false
      },
    }
    );
  }

  async redirectToKickAuthorization(){
    if(this.router.url.startsWith("/Change")){
      this.redirectUri = environment.FrontEndURL + "Edit/Channel/Kick"
    }
    else {
      this.redirectUri = environment.FrontEndURL + "Create/Channel/Kick"
    }


    const state = this.generateRandomState();
    localStorage.setItem('oauth_state', state);

    const { codeVerifier, codeChallenge } = await this.generateCodeVerifierAndChallenge()
    localStorage.setItem('pkce_code_verifier', codeVerifier);

    const params = new HttpParams()
    .set('client_id', environment.Kick_ClientID)
    .set('redirect_uri', this.redirectUri)
    .set('response_type', 'code')
    .set('scope', 'user:read')
    .set('state', state)
    .set('code_challenge', codeChallenge)
    .set('code_challenge_method', 'S256');



      const kickAuthorizationUrl = `https://id.kick.com/oauth/authorize?${params.toString()}`;

      this.router.navigate(['/external-redirect', { externalUrl: kickAuthorizationUrl }]);
  }

  generateCodeVerifierAndChallenge(): Promise<{ codeVerifier: string; codeChallenge: string }> {
    const codeVerifier = [...crypto.getRandomValues(new Uint8Array(32))]
      .map(b => b.toString(16).padStart(2, '0'))
      .join('');
  
    const encoder = new TextEncoder();
    const data = encoder.encode(codeVerifier);
  
    return crypto.subtle.digest('SHA-256', data).then((hashBuffer) => {
      const base64url = btoa(String.fromCharCode(...new Uint8Array(hashBuffer)))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/, '');
  
      return {
        codeVerifier,
        codeChallenge: base64url,
      };
    });
  }

  generateRandomState(): string {
    return [...crypto.getRandomValues(new Uint8Array(16))]
      .map(b => b.toString(16).padStart(2, '0'))
      .join('');
  }

}
