import { Component, HostListener, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { DisapproveChannelComponent } from 'src/app/components/Accessories/Popups/disapprove-channel/disapprove-channel.component';
import { isNumber } from 'src/app/components/Validators/isNumber';
import { Channels, StreamerInfo } from 'src/app/models/Channels/Channels';
import { ChannelStreamerInfoRequestPayload, ChannelService } from 'src/app/services/Channel/channel.service';

@Component({
  selector: 'app-channel-request',
  templateUrl: './channel-request.component.html',
  styleUrls: ['./channel-request.component.css']
})
export class ChannelRequestComponent implements OnInit{
  ChannelRequest: Channels[] = []
  AWVForm: FormGroup 
  loading = false
  private lastVideo = false
  private resubmissionDate?: string;
  private isScrollHandlerActive = false;
  
  constructor(
    private channelService: ChannelService,
    private modalService: NgbModal,
  ){
    this.AWVForm = new FormGroup({
      averageWeeklyViewers : new FormControl('',[Validators.required, isNumber]),
    })
  }

  
  ngOnInit(): void {
    this.channelService.getChannelRequest().subscribe({
      next: (data:any) => {
        this.ChannelRequest.push(...data);
        if (data.length == 50) {
          this.resubmissionDate = data[data.length - 1].nextAvailableResubmitDate;
          this.isScrollHandlerActive = true
        }
        else{
          this.lastVideo = true
        }
      },
      error: () => {
      },
      complete: () => {}
    });
  }

  approveChannel(channelID: string, streamerInfo: StreamerInfo[]){
    const approveChannel: ChannelStreamerInfoRequestPayload = {
      channelId: channelID,
      streamerInfo: streamerInfo
    }
    this.loading = true
    this.channelService.approveChannel(approveChannel).subscribe(() => {
      const indexToRemove = this.ChannelRequest.findIndex(channel => channel.channelId === channelID);
    
      if (indexToRemove !== -1) {
        this.ChannelRequest.splice(indexToRemove, 1); // Remove 1 item at the specified index
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
        const indexToRemove = this.ChannelRequest.findIndex(channel => channel.channelId === channelID);
    
        if (indexToRemove !== -1) {
          this.ChannelRequest.splice(indexToRemove, 1); // Remove 1 item at the specified index
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
          this.channelService.getChannelRequest(this.resubmissionDate).subscribe({
            next: (data:any) => {
              this.ChannelRequest.push(...data);
              if (data.length == 50) {
                this.resubmissionDate = data[data.length - 1].nextAvailableResubmitDate;
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
