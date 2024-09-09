import { Component, ElementRef, HostListener, OnInit } from '@angular/core';
import { Messages } from 'src/app/models/Messages/messages';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { MessagesService } from 'src/app/services/MessageService/messages.service';
import { UserService } from 'src/app/services/User/user-service';
import { RankComponent } from '../../Accessories/Popups/rank/rank.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { SentVideoRequestPayload } from 'src/app/models/Content/SentVideoRequestPayload';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ContentService } from 'src/app/services/Content/Content/content.service';
import { ReportVideoComponent } from '../../Accessories/Popups/report-video/report-video.component';
import { ChannelService } from 'src/app/services/Channel/channel.service';

enum MessageEnum {
  NewUser,
  Payment,
  Refund,
  YoutubeEmails,
  RankUp,
  UpdatedPayment,
  FailedVideo,
  SuccessfulVideo,
  WarningMessage,
  ApprovedChannel,
  DisapprovedChannel,
  ChannelPayment,
  ChannelRefund,
  FundChannel,
  FailedRefund,
  FailedChannelRefund,
  UserWithdraw,
  FailedToSendEmails,
  SentVideo,
  FailedAuctionPayment,
}

@Component({
  selector: 'app-message-main-page',
  templateUrl: './message-main-page.component.html',
  styleUrls: ['./message-main-page.component.css']
})
export class MessageMainPageComponent implements OnInit{
  MessageEnum = MessageEnum;
  selectedEnum?: MessageEnum
  messageContent: Messages
  manaAmount = 0
  dollarAmount = 0
  totalManaAmount = 0
  romanNumerals = ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X', 'XI'];
  clickedCheckboxIndices: number[] = [];
  clipboardError = false
  sendVideoError = false
  sentVideoEmails = false
  failedCalls = 0
  minAWV = 0

  private isScrollHandlerActive = false;
  private start = 0
  allMessages: Messages[] = []
  private lastVideo = false

  constructor(
    public alertService:AlertService,
    private messageService:MessagesService,
    private userService:UserService,
    private modalService: NgbModal,
    private contentService:ContentService,
    private authService:AuthenticationService,
    private channelService: ChannelService,
    private elementRef: ElementRef,
  ){
    this.minAWV = this.channelService.minAWV
    this.messageContent = {
      messageId: '',
      transactionHash: '',
      extraInfo: [],
      hasRead: false,
      messageEnum: '',
      creationDate: 0
    }
    const storedMessage = localStorage.getItem('selectedMessage');
    if (storedMessage) {
      this.messageContent = JSON.parse(storedMessage);
      localStorage.removeItem('selectedMessage');
      this.selectedEnum = this.getEnum(this.messageContent.messageEnum)
      this.processEnums()
    }


    this.messageService.userProfile$.subscribe(message => {
      this.messageContent = message
      this.selectedEnum = this.getEnum(this.messageContent.messageEnum)
      this.processEnums()
      this.clipboardError = false
      this.sendVideoError = false
      this.sentVideoEmails = false
      this.failedCalls = 0
    })

  }

  ngOnInit(): void {
    const JWT = localStorage.getItem('token') || ''
    this.messageService.getAllMessages(JWT, this.start).subscribe({
      next: (data) => {
        this.allMessages.push(...data);
        if (data.length == 50) {
          this.start += data.length;
          this.isScrollHandlerActive = true
        }
        else{
          this.lastVideo = true
        }
      },
      error: () => {
      }
    });
  }

  private processEnums(){
    if(this.selectedEnum != MessageEnum.YoutubeEmails){
      this.markAsRead()
    }
    else if(this.messageContent.extraInfo.length == 4){
      this.markAsRead()
    }
    if(this.selectedEnum == MessageEnum.Payment){
      this.manaAmount = Number(this.messageContent.extraInfo[2])
      this.dollarAmount = Number(this.messageContent.extraInfo[3])
    }
    else if(this.selectedEnum == MessageEnum.ChannelPayment){
      this.manaAmount = Number(this.messageContent.extraInfo[3])
      this.dollarAmount = Number(this.messageContent.extraInfo[4])
    }
    else if(this.selectedEnum == MessageEnum.Refund){
      this.manaAmount = Number(this.messageContent.extraInfo[2])
    }
    else if(this.selectedEnum == MessageEnum.ChannelRefund){
      this.manaAmount = Number(this.messageContent.extraInfo[3])
    }
    else if(this.selectedEnum == MessageEnum.FundChannel){
      this.manaAmount = Number(this.messageContent.extraInfo[1])
      this.dollarAmount = Number(this.messageContent.extraInfo[2])
    }
    else if(this.selectedEnum == MessageEnum.UpdatedPayment){
      this.manaAmount = Number(this.messageContent.extraInfo[2])
      this.dollarAmount = Number(this.messageContent.extraInfo[3])
      this.totalManaAmount = Number(this.messageContent.extraInfo[4])
    }
    else if(this.selectedEnum == MessageEnum.UserWithdraw){
      this.manaAmount = Number(this.messageContent.extraInfo[1])
      this.dollarAmount = Number(this.messageContent.extraInfo[0])
    }
    else if(this.selectedEnum == MessageEnum.FailedRefund){
      this.manaAmount = Number(this.messageContent.extraInfo[2])
    }
    else if(this.selectedEnum == MessageEnum.FailedChannelRefund){
      this.manaAmount = Number(this.messageContent.extraInfo[3])
    }
  }

  openRankModal(){
    this.modalService.open(RankComponent, {size: 'lg', scrollable: true,centered: true , animation: false})
  }

  getRank(rank:string): number{
    return Number(rank) - 1
  }

  getEnum(enumString: string): MessageEnum | undefined {
    return MessageEnum[enumString as keyof typeof MessageEnum];
  }

  markAsRead(){
    if(!this.messageContent.hasRead){
      this.messageService.markAsRead(this.messageContent.messageId).subscribe(() => {
         // Find the message with the same message ID
      const messageToUpdate = this.allMessages.find(message => message.messageId === this.messageContent.messageId);

      // Check if the message was found
      if (messageToUpdate) {
        // Update the isRead property
        messageToUpdate.hasRead = true;
      }
      });
    }
  } 

  reportVideo(contentID: string){
    const modalRef = this.modalService.open(ReportVideoComponent, { size: 'md', scrollable: true, animation: false, centered: true });
    modalRef.componentInstance.contentID = contentID
  }

  videoSent(){
    if(!this.messageContent.hasRead){
      const JWT: string = window.localStorage.getItem('token') || '{}'
      this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
        const sentVideoRequestPayload:SentVideoRequestPayload = {
          contentID: this.messageContent.extraInfo[3],
          messageID: this.messageContent.messageId
        }
        this.contentService.sentVideo(sentVideoRequestPayload).subscribe({
          next: () => {},
          error: (err) => {
            this.alertService.addAlert(err, "danger")
            this.sentVideoEmails = true
            this.sendVideoError = true
            this.failedCalls++
            if(this.failedCalls < 4){
              setTimeout(() => {
                this.videoSent()
              }, 5000); 
            }
          },
          complete: () => {
            this.sentVideoEmails = true
            this.sendVideoError = false
            const messageToUpdate = this.allMessages.find(message => message.messageId === this.messageContent.messageId);
            // Check if the message was found
            if (messageToUpdate) {
              // Update the isRead property
              messageToUpdate.hasRead = true;
            }
          }
        })
      })
    }
    
  }


  async copyLabel(checkbox: HTMLInputElement, index: number) {
    const label = checkbox.nextElementSibling as HTMLLabelElement;
    const labelText = label.innerText;
    if (checkbox.checked) {
      // Check if the checkbox's ID is already in the array
      this.clickedCheckboxIndices.push(index);
      try {
        await navigator.clipboard.writeText(labelText);
        this.clipboardError = false
        this.alertService.addAlert(labelText + " Copied", "success")

        // Check if all checkboxes are clicked
        if (this.clickedCheckboxIndices.length === this.messageContent.extraInfo.length - 4) {
          this.videoSent()
        }
      } catch (err) {
        this.clipboardError = true
      }
    }else{
      const indexToRemove = this.clickedCheckboxIndices.indexOf(index);
      if (indexToRemove !== -1) {
        this.clickedCheckboxIndices.splice(indexToRemove, 1);
      }
    }
  }

  getNextMessages(){
    const JWT = localStorage.getItem('token') || ''
    this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
      this.messageService.getAllMessages(JWTResult, this.start).subscribe({
        next: (data) => {
          this.allMessages.push(...data);
          if (data.length == 50) {
            this.start += data.length;
          }
          else{
            this.lastVideo = true
          }
          this.isScrollHandlerActive = true; // Re-enable the event handler once loading is complete
        },
        error: () => {
          this.isScrollHandlerActive = true; // Ensure the event handler is re-enabled in case of an error
        }
      });
    })
  }

  selectMessage(selectedMessage: Messages) {
    this.messageService.setMessage(selectedMessage)
  }

  

  addSpaceBeforeUppercase(value: string): string {
    return value.replace(/([A-Z])/g, ' $1');
  }


@HostListener('scroll', ['$event'])
onScroll(event: any) {
  if (this.isScrollHandlerActive) {
    const scrollableContent = this.elementRef.nativeElement.querySelector('.sidebar');
    const windowHeight = 'innerHeight' in window ? window.innerHeight : document.documentElement.offsetHeight;
    const docHeight = scrollableContent.scrollHeight;
    const windowBottom = scrollableContent.scrollTop + windowHeight;

    if (windowBottom >= docHeight - 50) {
      this.isScrollHandlerActive = false; // Disable the event handler while loading data

      // Call your loadLatestMessages function
      if(this.lastVideo == false){
        this.getNextMessages()
      }
    }
  }
}

}
