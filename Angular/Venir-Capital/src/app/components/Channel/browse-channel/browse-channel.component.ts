import { Component, HostListener } from '@angular/core';
import { Channels } from 'src/app/models/Channels/Channels';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { ChannelService } from 'src/app/services/Channel/channel.service';

@Component({
  selector: 'app-browse-channel',
  templateUrl: './browse-channel.component.html',
  styleUrls: ['./browse-channel.component.css']
})
export class BrowseChannelComponent {
  Channels: Channels[] = []
  private excludedChannelIds?: string[]
  private isScrollHandlerActive = false;
  private lastVideo = false
  
  constructor(
    private channelService:ChannelService,
    public alertService:AlertService,
  ){



  }

  ngOnInit(): void {
    this.channelService.getRandomActiveChannels(this.excludedChannelIds).subscribe({
      next: (channels:Channels[]) => {
        this.Channels.push(...channels);
        if (channels.length == 50) {
          const newExcludedIds = channels.map(channel => channel.channelId);
          this.excludedChannelIds = newExcludedIds;
          this.isScrollHandlerActive = true
        }
        else{
          this.lastVideo = true
        }
      },
      error: () => {},
      complete: () => {}
    });
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
          this.channelService.getRandomActiveChannels(this.excludedChannelIds).subscribe({
            next: (channels:Channels[]) => {
              this.Channels.push(...channels);
              if (channels.length == 50) {
                const newExcludedIds = channels.map(channel => channel.channelId);
                if(this.excludedChannelIds){
                  if(this.excludedChannelIds.length >= 1000){
                    this.lastVideo = true
                  }
                  else{
                    this.excludedChannelIds.push(...newExcludedIds);
                  }
                }
              }
              else{
                this.lastVideo = true
              }
            },
            error: () => {},
            complete: () => {}
          });
        }
      }
    }
  }
}
