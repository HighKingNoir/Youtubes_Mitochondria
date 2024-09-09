import { Component } from '@angular/core';
import { Channels } from 'src/app/models/Channels/Channels';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ChannelService } from 'src/app/services/Channel/channel.service';

@Component({
  selector: 'app-channel-subscriptions',
  templateUrl: './channel-subscriptions.component.html',
  styleUrls: ['./channel-subscriptions.component.css']
})
export class ChannelSubscriptionsComponent {
  Channels: Channels[] = []
  isLoggedIn: boolean;

  constructor(
    private channelService:ChannelService,
    public alertService:AlertService,
    private authService:AuthenticationService,
  ){
    if(localStorage.getItem('token')){
      this.isLoggedIn = true
    }
    else{
      this.isLoggedIn = false
    }
    this.authService.isLoggedInProfile$.subscribe(isLoggedInStatus => {
      this.isLoggedIn = isLoggedInStatus
      if(isLoggedInStatus){
        this.channelService.getChannelSubscriptions().subscribe(channels =>{
          this.Channels = channels;
        })
      }
      else{
        this.Channels = []
      }
    })
  }

  ngOnInit(): void {
    if(this.isLoggedIn){
      this.channelService.getChannelSubscriptions().subscribe(channels =>{
        this.Channels = channels;
      })
    }
    
  }
}
