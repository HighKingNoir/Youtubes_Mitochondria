import { Component, HostListener } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { DisapproveChannelComponent } from 'src/app/components/Accessories/Popups/disapprove-channel/disapprove-channel.component';
import { isNumber } from 'src/app/components/Validators/isNumber';
import { Channels, StreamerInfo } from 'src/app/models/Channels/Channels';
import { ChannelService, ChannelStreamerInfoRequestPayload } from 'src/app/services/Channel/channel.service';

@Component({
  selector: 'app-change-channel',
  templateUrl: './change-channel.component.html',
  styleUrls: ['./change-channel.component.css']
})
export class ChangeChannelComponent {
  needsChange: Channels[] = []
  private start = 0
  private lastVideo = false
  private isScrollHandlerActive = false;
  AWVForm: FormGroup 
  loading = false

  constructor(
    private channelService: ChannelService,
    private modalService: NgbModal,
  ){
    this.AWVForm = new FormGroup({
      averageWeeklyViewers : new FormControl('',[Validators.required, isNumber]),
    })
  }



  
  ngOnInit(): void {
    // this.channelService.getChannelNeedingUpdate(this.start).subscribe({
    //   next: (data:any) => {
    //     this.needsChange.push(...data);
    //     if (data.length == 50) {
    //       this.start += 50
    //     }
    //     else{
    //       this.lastVideo = true
    //     }
    //     this.isScrollHandlerActive = true;
    //   },
    //   error: () => {
    //     this.isScrollHandlerActive = true;
    //   },
    //   complete: () => {}
    // });
  }

  approveChannel(channelID: string, streamerInfo: StreamerInfo[]){
    const approveChannel: ChannelStreamerInfoRequestPayload = {
      channelId: channelID,
      streamerInfo: streamerInfo
    }
    this.loading = true
    this.channelService.approveChannel(approveChannel).subscribe(() => {
      const indexToRemove = this.needsChange.findIndex(channel => channel.channelId === channelID);
    
      if (indexToRemove !== -1) {
        this.needsChange.splice(indexToRemove, 1); // Remove 1 item at the specified index
      }
      this.loading = false
    });
  }

  disapproveChannel(channelID: string, channelName: string,){
    const modalRef = this.modalService.open(DisapproveChannelComponent, {size: 'md', scrollable: true,centered: true , animation: false})
    modalRef.componentInstance.channelId = channelID
    modalRef.componentInstance.channelName = channelName
    modalRef.result.then((result) => {
      if(result === "success"){
        const indexToRemove = this.needsChange.findIndex(channel => channel.channelId === channelID);
    
        if (indexToRemove !== -1) {
          this.needsChange.splice(indexToRemove, 1); // Remove 1 item at the specified index
        }
      }
    })
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
          // this.channelService.getChannelNeedingUpdate(this.start).subscribe({
          //   next: (data:any) => {
          //     this.needsChange.push(...data);
          //     if (data.length == 50) {
          //       this.start += 50
          //     }
          //     else{
          //       this.lastVideo = true
          //     }
          //     this.isScrollHandlerActive = true;
          //   },
          //   error: () => {
          //     this.isScrollHandlerActive = true;
          //   },
          //   complete: () => {}
          // });
        }
      }
    }
  }
}
