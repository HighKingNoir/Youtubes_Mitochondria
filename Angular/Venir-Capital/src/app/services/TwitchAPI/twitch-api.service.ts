import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { environment } from 'src/Environment/environment';
import { StreamerInfo } from 'src/app/models/Channels/Channels';
import { CreateChannelService } from '../CreateChannel/create-channel.service';
import { EditChannelService } from '../EditChannel/edit-channel.service';
import { AlertService } from '../Alerts/alert.service';




@Injectable({
  providedIn: 'root'
})
export class TwitchApiService {
  redirectUri = ''

  constructor(
    private http: HttpClient,
    private router: Router,
    private createChannelService: CreateChannelService,
    private editChannelService: EditChannelService,
    private alertService: AlertService
  ) {}

  getTwitchChannel(accessToken: string,broadcaster_id: string, redirectURL:string) {
    const headers = new HttpHeaders({
      'Client-ID': environment.Twitch_ClientID, 
      Authorization: `Bearer ${accessToken}`, // Replace with your access token if required
    });

    const apiUrl = `https://api.twitch.tv/helix/channels?broadcaster_id=${broadcaster_id}`;

    this.http.get(apiUrl, { headers }).subscribe({
      next: (response: any) => {
        const streamerInfo: StreamerInfo = {
          platform: "Twitch",
          streamerId: response.data[0].broadcaster_id,
          username: response.data[0].broadcaster_name,
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

      },
    }
    );
  }



  redirectToTwitchAuthorization() {
    if(this.router.url.startsWith("/Change")){
      this.redirectUri = "Edit/Channel/Twitch"
    }
    else {
      this.redirectUri = "Create/Channel/Twitch"
    }
    const params = new HttpParams()
      .set('client_id', environment.Twitch_ClientID)
      .set('redirect_uri', environment.FrontEndURL +  this.redirectUri)
      .set('response_type', "token id_token")
      .set('scope', 'openid');
  
    const twitchAuthorizationUrl = `https://id.twitch.tv/oauth2/authorize?${params.toString()}`;

    // Use the Angular Router to navigate to the Twitch authorization URL
    this.router.navigate(['/external-redirect', { externalUrl: twitchAuthorizationUrl }]);
  }
  

}

