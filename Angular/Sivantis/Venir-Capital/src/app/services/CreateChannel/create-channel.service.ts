import { Injectable } from '@angular/core';
import { StreamerInfo } from 'src/app/models/Channels/Channels';
import { AlertService } from '../Alerts/alert.service';
import { ChannelData, ChannelService } from '../Channel/channel.service';
import { AuthenticationService } from '../Auth/authentication.service';

@Injectable({
  providedIn: 'root'
})
export class CreateChannelService {
  private channelData: ChannelData = {
    channelName: '',
    channelLogo: undefined,
    streamerInfo: []
  };


  constructor(
    private alertService:AlertService,
    private channelService:ChannelService,
    private authService: AuthenticationService
  ) {
    // Load saved data from localStorage
    this.loadFromLocalStorage();
  }

  getChannelName() {
    return this.channelData.channelName;
  }

  getChannelLogo() {
    return this.channelData.channelLogo;
  }

  getStreamerInfo() {
    return this.channelData.streamerInfo;
  }

  setChannelName(channelName: string) {
    this.channelData.channelName = channelName;
    // Save to localStorage
    this.saveToLocalStorage();
  }

  setChannelLogo(channelLogo: string) {
    this.channelData.channelLogo = channelLogo;
    // Save to localStorage
    this.saveToLocalStorage();
  }

  setStreamerInfo(streamerInfo: StreamerInfo[]) {
    this.channelData.streamerInfo = streamerInfo
    // Save to localStorage
    this.saveToLocalStorage();
  }

  addStreamerInfo(streamerInfo: StreamerInfo) {
    const exists = this.channelData.streamerInfo.some(existingInfo => 
      existingInfo.platform === streamerInfo.platform &&
      existingInfo.streamerId === streamerInfo.streamerId
    );
    if(exists){
      this.alertService.addAlert("This Stream Info Was Already Added", "danger")
    }
    else{
      streamerInfo.username = this.extractText(streamerInfo.username)
      this.channelData.streamerInfo.push(streamerInfo);
      this.saveToLocalStorage();
      const JWT = window.localStorage.getItem('token') || ''
      this.authService.checkJWTExpiration(JWT).then(() => {
        this.channelService.channelExist(streamerInfo.platform, streamerInfo.streamerId).subscribe(exist => {
          if(exist === 'true'){
              this.alertService.addAlert("Another account already uses " + streamerInfo.username + " on " + streamerInfo.platform, "danger")
          }
          else{
            
          }
        })
      })
      
    }
  }

  removeStreamerInfo(indexToRemove: number) {
    if (indexToRemove >= 0 && indexToRemove < this.channelData.streamerInfo.length) {
      this.channelData.streamerInfo.splice(indexToRemove, 1); // Remove 1 item at the specified index
    }
    // Save to localStorage
    this.saveToLocalStorage();
  }

  // Save data to localStorage
  private saveToLocalStorage() {
    localStorage.setItem('createChannelData', JSON.stringify(this.channelData));
  }

  // Load data from localStorage
  private loadFromLocalStorage() {
    const data = localStorage.getItem('createChannelData');
    if (data) {
      const parsedData = JSON.parse(data);
      this.channelData.channelName = parsedData.channelName;
      this.channelData.channelLogo = parsedData.channelLogo;
      this.channelData.streamerInfo = parsedData.streamerInfo;
    }
  }

  private extractText(input: string): string {
    // Define a regular expression to match text inside parentheses
    const regex = /\(([^)]+)\)/;
  
    // Attempt to find text inside parentheses
    const match = input.match(regex);
  
    if (match) {
      // Return text inside parentheses if found
      return match[1];
    } else {
      // If no parentheses found, return first word
      const words = input.split(' ');
      return words[0];
    }
  }

  // Clear data from localStorage (if needed)
  clearLocalStorage() {
    localStorage.removeItem('createChannelData');
  }
}
