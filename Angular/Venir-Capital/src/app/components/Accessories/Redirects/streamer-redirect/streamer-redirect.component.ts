import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { CreateChannelService } from 'src/app/services/CreateChannel/create-channel.service';
import { GoogleAPIService } from 'src/app/services/GoogleAPI/google-api.service';
import { KickApiService } from 'src/app/services/KickAPI/kick-api.service';
import { TwitchApiService } from 'src/app/services/TwitchAPI/twitch-api.service';

@Component({
  selector: 'app-streamer-redirect',
  templateUrl: './streamer-redirect.component.html',
  styleUrls: ['./streamer-redirect.component.css']
})
export class StreamerRedirectComponent {
  constructor(
    private googleAPIService:GoogleAPIService,
    private twitchAPIService:TwitchApiService,
    private kickAPIService:KickApiService,
    private alertService:AlertService,
    private route: ActivatedRoute,
    private router: Router,
    private createChannelService:CreateChannelService,
  ){
    this.process()
  }

  process(){
    const platform: string = this.route.snapshot.params['platform'];
    const redirectURL = "/Create/Channel"
    switch (platform) {
      case 'Youtube':
          this.googleAPIService.loadYoutubeChannel(redirectURL)
        break;

      case 'Twitch':
        const twitchFragment = this.route.snapshot.fragment!;

        // Parse the fragment to extract parameters
        const twitchParams = new URLSearchParams(twitchFragment);
        const twitchAccessToken = twitchParams.get('access_token');
        if(twitchAccessToken){
          // Access and parse the id token (assuming it's a JWT)
          const idToken = twitchParams.get('id_token');
          if (idToken) {
            const idTokenPayload = this.parseJwt(idToken);
            // Access the sub claim from the ID Token
            const subClaim = idTokenPayload.sub;
            const hasError = this.twitchAPIService.getTwitchChannel(twitchAccessToken, subClaim, redirectURL)
          }
          else{
            this.router.navigateByUrl(redirectURL)
            this.alertService.addAlert("Failed to add Twitch Channel", "danger")
          }
        }
        else{
          this.router.navigateByUrl(redirectURL)
          this.alertService.addAlert("Failed to add Twitch Channel", "danger")
        }
        
        break;

      case 'Kick':
        const kickFragment = this.route.snapshot.fragment!;

        // Parse the fragment to extract parameters
        const kickParams = new URLSearchParams(kickFragment);
        const kickAccessToken = kickParams.get('access_token');
        if(kickAccessToken){
          // Access and parse the id token (assuming it's a JWT)
          const idToken = kickParams.get('id_token');
          if (idToken) {
            const idTokenPayload = this.parseJwt(idToken);
            // Access the sub claim from the ID Token
            const subClaim = idTokenPayload.sub;
            this.kickAPIService.getKickChannel(kickAccessToken, subClaim, redirectURL)
          }
          else{
            this.router.navigateByUrl(redirectURL)
            this.alertService.addAlert("Failed to add Kick Channel", "danger")
          }
        }
        else{
          this.router.navigateByUrl(redirectURL)
          this.alertService.addAlert("Failed to add Kick Channel", "danger")
        }
        break;

      default:
        this.alertService.addAlert("Error Getting Streamer Info", "danger")
        this.router.navigateByUrl(redirectURL)
        break;
    }
  }

  parseJwt(token: string): any {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64));
  }
}
