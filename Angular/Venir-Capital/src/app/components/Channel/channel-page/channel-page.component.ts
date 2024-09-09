import { Component, HostListener, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute} from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Channels } from 'src/app/models/Channels/Channels';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ChannelPaymentRequestPayload, ChannelService } from 'src/app/services/Channel/channel.service';
import { FundChannelPayload, MessagesService } from 'src/app/services/MessageService/messages.service';
import { UserService } from 'src/app/services/User/user-service';
import { FundChannelComponent } from '../../Accessories/Popups/fund-channel/fund-channel.component';
import { ConnectWalletService } from 'src/app/services/Mana/ConnectPersonalWallet/connect-wallet.service';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { EditChannelComponent } from '../../Accessories/Popups/edit-channel/edit-channel.component';
import { ContentService } from 'src/app/services/Content/Content/content.service';
import { PurchasedContentResponse } from 'src/app/models/Content/PurchasedContentResponse';
import { PurchasedContentDetails } from 'src/app/models/Content/PurchasedContentDetails';
import { RefundContentComponent } from '../../Accessories/Popups/ChannelContent/refund-content/refund-content.component';
import { ConfirmationComponent, VideoConfirmation } from '../../Accessories/Popups/confirmation/confirmation.component';
import { CreatedContentDetails } from 'src/app/models/Content/CreatedContentDetails';
import { ReportVideoComponent } from '../../Accessories/Popups/report-video/report-video.component';
import { HomeSidebarComponent } from '../../Accessories/Layout/home-sidebar/home-sidebar.component';


@Component({
  selector: 'app-channel-page',
  templateUrl: './channel-page.component.html',
  styleUrls: ['./channel-page.component.css']
})
export class ChannelPageComponent implements OnInit{
  channel: Channels
  isSubscribed = false;
  isInactive = true;
  isOwner = false;
  ChannelPurchasedVideos: PurchasedContentResponse = {
    content: [],
    payment: [],
  };
  private paymentDate?: string
  private lastVideo = false
  private isScrollHandlerActive = false;
  videoIndices: number[] = [];
  isLoggedIn: boolean;
  @ViewChild(HomeSidebarComponent) homeSideBarComponent!: HomeSidebarComponent;
  
  constructor(
    private modalService: NgbModal, 
    private messsageService: MessagesService,
    private userService: UserService, 
    private channelService:ChannelService, 
    private route: ActivatedRoute,
    private connectWalletService: ConnectWalletService,
    public alertService:AlertService,
    private authService:AuthenticationService,
    private contentService: ContentService, 
  ){
    this.channel = {
      channelBanner: '',
      channelDescription: '',
      channelEvents: [],
      channelId: '',
      channelLogo: '',
      channelName: '',
      channelStatus: '',
      ownerID: '',
      isBanned: false,
      timezone: '',
      streamerInfo: [],
      streamerChangeInfo: []
    }
    this.route.params.subscribe((params) => {
      const newChannelName = params['name'];
      // Perform actions based on the new channel name, e.g., load data
      this.loadDataForChannel(newChannelName);
    });
    this.channelService.channel$.subscribe(channel => {
      if(this.isOwner){
        this.channel = channel
      }
    })

    if(localStorage.getItem('token')){
      this.isLoggedIn = true
    }
    else{
      this.isLoggedIn = false
    }
    this.authService.isLoggedInProfile$.subscribe(isLoggedInStatus => {
      this.isLoggedIn = isLoggedInStatus
      if(isLoggedInStatus){
        if(this.isLoggedIn){
          this.isSubscribed = this.isSubscribedTo()
          if(this.userService.getUserID() === ''){
            this.userService.userInfo$.subscribe(() => {
              this.isOwner = this.channel.ownerID == this.userService.getUserID()
            })
          }
          else{
            this.isOwner = this.channel.ownerID == this.userService.getUserID()
          }
        }
      }
      else{
        this.isSubscribed = false;
        this.isOwner = false;
      }
    })
  }

  private loadDataForChannel(channelName: string): void {
    this.channelService.getChannel(channelName).subscribe(channel => {
      this.channel = channel
      if(channel.channelStatus === "Approved"){
        this.isInactive = false
      }
      this.isSubscribed = this.isSubscribedTo()
      this.isOwner = this.channel.ownerID == this.userService.getUserID()
    })
  }

  ngOnInit(): void {
    this.channelService.getChannel(this.route.snapshot.params['name']).subscribe(channel => {
      this.channel = channel
      if(channel.channelStatus === "Approved"){
        this.isInactive = false
      }
      if(this.isLoggedIn){
        this.isSubscribed = this.isSubscribedTo()
        if(this.userService.getUserID() === ''){
          this.userService.userInfo$.subscribe(() => {
            this.isOwner = this.channel.ownerID == this.userService.getUserID()
          })
        }
        else{
          this.isOwner = this.channel.ownerID == this.userService.getUserID()
        }
      }
    })
    this.contentService.fetchAllChannelPurchasedContent(this.route.snapshot.params['name'], this.paymentDate).subscribe((purchasedContent: PurchasedContentResponse) => {
      if (purchasedContent.payment.length != 50) {
        this.lastVideo = true;
      }
      else{
        this.isScrollHandlerActive = true
      }
      this.ChannelPurchasedVideos.content.push(...purchasedContent.content)
      this.ChannelPurchasedVideos.payment.push(...purchasedContent.payment)
      this.videoIndices = Array.from({ length: this.ChannelPurchasedVideos.payment.length }, (_, i) => i);
    })
  }

  getNextPurchases(){
    const JWT = window.localStorage.getItem('token') || ''
    this.authService.checkJWTExpiration(JWT).then(() => {
      this.contentService.fetchAllChannelPurchasedContent(this.channel.channelName, this.paymentDate).subscribe({
        next: (data:PurchasedContentResponse) => {
          this.ChannelPurchasedVideos.content.push(...data.content)
          this.ChannelPurchasedVideos.payment.push(...data.payment)
          this.videoIndices = Array.from({ length: this.ChannelPurchasedVideos.payment.length }, (_, i) => i);
          if (data.payment.length == 50) {
            this.paymentDate = data.payment[data.payment.length - 1].paymentDate.toString();
            this.isScrollHandlerActive = true
          }
          else{
            this.lastVideo = true
          }
        },
        error: () => {},
        complete: () => {}
      });
    })
  }
  
  openFundChannel(){
    const modelRef = this.modalService.open(FundChannelComponent, {size: 'sm', scrollable: true,centered: true , animation: false})
    modelRef.componentInstance.channelName = this.channel.channelName
    modelRef.result.then(result => {
      if(result === "connecting Wallet"){
        this.connectWalletService.connectAccount().then(() => {
          const modelRef = this.modalService.open(FundChannelComponent, { size: 'sm', scrollable: true, centered: true , animation: false})
          modelRef.componentInstance.channelName = this.channel.channelName
          modelRef.result.then(result => {
            if(result === "Funding successful"){
              this.alertService.addAlert("Channel Successfully Funded", 'success')
            }
            else if(result === "Funding failed"){
              this.alertService.addAlert("Failed to Fund Channel", "danger")
            }
          })
        });
      } 
      else {
        if(result === "Funding successful"){
          this.alertService.addAlert("Channel Successfully Funded", 'success')
        }
        else if(result === "Funding failed"){
          this.alertService.addAlert("Failed to Fund Channel", "danger")
        }
      }
    })  
  }

  redirectToStream(platform: string, to: string){
    let url = ''
    switch(platform){
      case 'Youtube':
        url = "https://www.youtube.com/channel/"
        break
      case 'Twitch':
        url = "https://www.twitch.tv/"
        break
      case "Kick":
        url = "https://kick.com/"
        break
    }
    window.open(`${url}${to}`, '_blank');
  }

  editChannel(){
    const modelRef = this.modalService.open(EditChannelComponent, {size: 'lg', scrollable: true,centered: true , animation: false})
    modelRef.componentInstance.channel = this.channel
    modelRef.result.then((result) => {
      if(result === 'success'){
        
      }
    })
  }

  hasBanner(): boolean {
    return !!this.channel.channelBanner;
  }

  watchTrailer(trailerId: string){
    window.open(`https://www.youtube.com/watch?v=${trailerId}`, '_blank');
  }

  watchVideo(videoId: string){
    window.open(`https://www.youtube.com/watch?v=${videoId}`, '_blank');
  }


  openCancelModal(payment: PurchasedContentDetails, index: number){
    const refundManaAmount = Number(payment.manaAmount)
    const modalRef = this.modalService.open(RefundContentComponent, {size: 'sm', scrollable: true,centered: true , animation: false})
    modalRef.componentInstance.refundManaAmount = refundManaAmount;
    modalRef.result.then(result => {
      if(result === "refund content"){
        this.cancelPurchase(payment, index)
      }
    })
  }

  cancelPurchase(payment: PurchasedContentDetails, index: number){
    const JWT = window.localStorage.getItem('token') || '{}'
    this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
    const channelPaymentRequest: ChannelPaymentRequestPayload = {
      channelName: this.channel.channelName,
      contentID: payment.contentId,
      
    }
      this.channelService.refundChannelContent(channelPaymentRequest).subscribe({
        next: () => {},
        error: (error) => {
          this.alertService.addAlert(error, 'danger')
        },
        complete: () => {
          this.ChannelPurchasedVideos.payment[index].status = "RefundedPurchase"
          this.alertService.addAlert("Cancellation Successful", 'success')
        }
      })
    })
  }

  reportVideo(contentID: string){
    const modalRef = this.modalService.open(ReportVideoComponent, { size: 'md', scrollable: true, animation: false, centered: true });
    modalRef.componentInstance.contentID = contentID
  }

  deleteVideo(video: CreatedContentDetails){
    const videoConfirmation:VideoConfirmation = {
      contentName: video.contentName,
      description: video.description,
      thumbnail: video.thumbnail,
    }
    const modalRef = this.modalService.open(ConfirmationComponent, {size: 'md', scrollable: true,centered: true , animation: false})
    modalRef.componentInstance.message = "Are you sure you want to Delete this video?"
    modalRef.componentInstance.videoConfirmation = videoConfirmation
    modalRef.result.then(result => {
      if(result === 'continue'){
        const JWT = window.localStorage.getItem('token') || ''
        this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
          this.contentService.deleteChannelPurchasedContent(JWTResult, video.contentId, this.channel.channelId).subscribe({
            next: () => {
              const indexToDelete = this.ChannelPurchasedVideos.content.findIndex(
                (v) => v.contentId === video.contentId
              );
              if (indexToDelete !== -1) {
                this.videoIndices.splice(indexToDelete, 1);
              }
            },
            error: (error) => {
              this.alertService.addAlert(error, 'danger')
            },
            complete: () => {
              this.alertService.addAlert("Content Deleted", 'success')
            }
          })
        })
      }
    })
  }

  subscribe(){
    if(this.isLoggedIn){
      const JWT = window.localStorage.getItem('token') || ''
      this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
        if(this.isSubscribed){
          this.userService.unsubscribeToChannel(this.channel.channelId).subscribe({
            next: () => {
              this.isSubscribed = false;
              this.homeSideBarComponent?.removeChannel(this.channel)
            },
            error: (err) => {
              this.alertService.addAlert("Error Subscribing", 'danger')
            },
            complete: () => {
    
            }
          })
      } else{
          this.userService.subscribeToChannel(this.channel.channelId).subscribe({
            next: () => {
              this.isSubscribed = true;
              this.homeSideBarComponent?.addChannel(this.channel)
            },
            error: (err) => {
              this.alertService.addAlert("Error Unsubscribing", 'danger')
            },
            complete: () => {

            }
          })
        }
      }) 
    }
    else{
      this.alertService.addAlert("Must be logged in to Subscribe.", "danger")
    }
  }


  isSubscribedTo(): boolean{
    const list = this.userService.getChannelsSubscribedTo();
    return list.includes(this.channel.channelId)
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
          this.getNextPurchases()
        }
      }
    }
  }

}
