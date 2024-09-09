import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { CreateChannelService } from '../CreateChannel/create-channel.service';
import { environment } from 'src/Environment/environment';
import { EditChannelService } from '../EditChannel/edit-channel.service';

@Injectable({
  providedIn: 'root'
})
export class KickApiService {
  redirectUri = ''
  constructor(
    private http: HttpClient,
    private router: Router,
    private createChannelService: CreateChannelService,
    private editChannelService: EditChannelService
  ) { }

  getKickChannel(accessToken: string,broadcaster_id: string, redirectURL: string) {
    // const headers = new HttpHeaders({
    //   'Client-ID': environment.Kick_ClientID, 
    //   Authorization: `Bearer ${accessToken}`, // Replace with your access token if required
    // });



    // this.http.get(apiUrl, { headers }).subscribe({
    //   next: (response: any) => {
    //     const streamerInfo: StreamerInfo = {
    //       platform: "Kick",
    //       streamerId: response.data[0].broadcaster_id,
    //       username: response.data[0].broadcaster_name,
    //       averageWeeklyViewers: 0,
              // youtubeChannelId: null
    // //     }
    //         if(redirectURL.startsWith("/Change")){
              //this.editChannelService.addStreamerInfo(streamerInfo)
              //this.router.navigateByUrl(redirectURL)
            // }
            // else{
              //this.createChannelService.addStreamerInfo(streamerInfo)
              //this.router.navigateByUrl('/Create/Channel')
            // }

    //   },
    //   error: (error) =>{
      //   if(redirectURL.startsWith("/Change")){
      //     this.alertService.addAlert(error, "danger")
      //     this.router.navigateByUrl(redirectURL)
      //   }
      //   else{
      //     this.alertService.addAlert(error, "danger")
      //     this.router.navigateByUrl('/Create/Channel')
      //   }
      // },
    //   complete() {
          // return false
    //   },
    // }
    // );
  }

  redirectToKickAuthorization(){
    if(this.router.url.startsWith("/Change")){
      this.redirectUri = environment.FrontEndURL + "Edit/Channel/Kick"
    }
    else {
      this.redirectUri = environment.FrontEndURL + "Create/Channel/Kick"
    }
    const params = new HttpParams()
      .set('client_id', environment.Kick_ClientID)
      .set('redirect_uri', environment.FrontEndURL + this.redirectUri)
      .set('response_type', "token id_token")
      .set('scope', 'openid');



    // this.router.navigate(['/external-redirect', { externalUrl: twitchAuthorizationUrl }]);
  }
}
