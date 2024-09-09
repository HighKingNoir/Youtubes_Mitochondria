import { Component, HostListener, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { isNumber } from 'src/app/components/Validators/isNumber';
import { Channels, StreamerInfo } from 'src/app/models/Channels/Channels';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { ChannelService, ChannelStreamerInfoRequestPayload } from 'src/app/services/Channel/channel.service';
import { UserService } from 'src/app/services/User/user-service';

@Component({
  selector: 'app-awv-updates',
  templateUrl: './awv-updates.component.html',
  styleUrls: ['./awv-updates.component.css']
})
export class AwvUpdatesComponent implements OnInit{
  needsUpdates: Channels[] = []
  private start = 0
  private lastVideo = false
  private isScrollHandlerActive = false;
  AWVForm: FormGroup 
  loading = false

  constructor(
    private channelService: ChannelService,
    private userService:UserService,
    public alertService:AlertService,
  ){
    this.AWVForm = new FormGroup({
      averageWeeklyViewers : new FormControl('',[Validators.required, isNumber]),
    })
  }



  
  ngOnInit(): void {
    this.channelService.getChannelNeedingUpdate(this.start).subscribe({
      next: (data:any) => {
        this.needsUpdates.push(...data);
        if (data.length == 50) {
          this.start += 50
        }
        else{
          this.lastVideo = true
        }
        this.isScrollHandlerActive = true;
      },
      error: () => {
        this.isScrollHandlerActive = true;
      },
      complete: () => {}
    });
  }

  updateChannel(channelID: string, streamerInfo: StreamerInfo[]){
    const updateChannel: ChannelStreamerInfoRequestPayload = {
      channelId: channelID,
      streamerInfo: streamerInfo
    }
    this.loading = true
    this.channelService.updateAverageWeeklyViewers(updateChannel).subscribe({
      next: () => {
        const indexToRemove = this.needsUpdates.findIndex(channel => channel.channelId === channelID);
    
      if (indexToRemove !== -1) {
        this.needsUpdates.splice(indexToRemove, 1); // Remove 1 item at the specified index
      }
      this.loading = false
      this.alertService.addAlert("Channel Successfully Updated", 'success')
      },
      error: () => {
        this.loading = false
        this.alertService.addAlert("Channel Update Failed", "danger")
      },
      complete: () => {}
      
    });
  }

  updateAWV(streamerInfo: StreamerInfo[], index: number) {
    const streamer = streamerInfo.at(index);
    const averageWeeklyViewers: number = this.AWVForm.get('averageWeeklyViewers')?.value
    if (streamer) {
        streamer.averageWeeklyViewers = averageWeeklyViewers;
    }
  }

  @HostListener('window:scroll', ['$event'])
  onScroll(event: any) {
    if(this.isScrollHandlerActive){
      const windowHeight = 'innerHeight' in window ? window.innerHeight : document.documentElement.offsetHeight;
      const body = document.body, html = document.documentElement;
      const docHeight = Math.max(body.scrollHeight, body.offsetHeight, html.clientHeight, html.scrollHeight, html.offsetHeight);
      const windowBottom = window.scrollY + windowHeight; // Calculate window bottom correctly

      if (windowBottom >= docHeight - 200) {
        this.isScrollHandlerActive = false;
        if(this.lastVideo == false){
          this.channelService.getChannelNeedingUpdate(this.start).subscribe({
            next: (data:any) => {
              this.needsUpdates.push(...data);
              if (data.length == 50) {
                this.start += 50
              }
              else{
                this.lastVideo = true
              }
              this.isScrollHandlerActive = true;
            },
            error: () => {
              this.isScrollHandlerActive = true;
            },
            complete: () => {}
          });
        }
      }
    }
  }
}
