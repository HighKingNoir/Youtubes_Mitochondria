import { Component } from '@angular/core';
import { Route, Router } from '@angular/router';
import { GoogleAPIService } from 'src/app/services/GoogleAPI/google-api.service';
import { KickApiService } from 'src/app/services/KickAPI/kick-api.service';
import { TwitchApiService } from 'src/app/services/TwitchAPI/twitch-api.service';

@Component({
  selector: 'app-select-platform',
  templateUrl: './select-platform.component.html',
  styleUrls: ['./select-platform.component.css']
})
export class SelectPlatformComponent {


  constructor(
    private googleAPIService:GoogleAPIService,
    private twitchAPIService:TwitchApiService,
    private kickAPIService: KickApiService,
  ){
    
  }

  addYoutube(){
    this.googleAPIService.addYoutubeChannel()
  }

  addTwitch(){
    this.twitchAPIService.redirectToTwitchAuthorization()
  }

  addKick(){
    this.kickAPIService.redirectToKickAuthorization()
  }

}
