import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Subscription, interval, switchMap } from 'rxjs';
import { CreatedContentDetails } from 'src/app/models/Content/CreatedContentDetails';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ContentService } from 'src/app/services/Content/Content/content.service';
import { ChannelServiceContract } from 'src/app/services/Contracts/Channel/channel-service-contract.service';
import { ConnectWalletService } from 'src/app/services/Mana/ConnectPersonalWallet/connect-wallet.service';
import { PricingService } from 'src/app/services/Mana/PricingService/pricing.service';
import { PaymentRequestPayload, RefundRequestPayload, PaymentServiceService } from 'src/app/services/PaymentService/payment-service.service';
import { UserService } from 'src/app/services/User/user-service';
import { ChannelPaymentRequestPayload, ChannelService, WatchNowPayLaterRequestPayload } from 'src/app/services/Channel/channel.service';
import { Channels, StreamerInfo } from 'src/app/models/Channels/Channels';
import { FundChannelComponent } from '../../Accessories/Popups/fund-channel/fund-channel.component';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { PurchasedContentDetails } from 'src/app/models/Content/PurchasedContentDetails';
import { RefundContentComponent } from '../../Accessories/Popups/ChannelContent/refund-content/refund-content.component';
import { TransakService } from 'src/app/services/Mana/Transak/transak.service';
import { WatchNowPayLaterComponent } from '../../Accessories/Popups/ChannelContent/watch-now-pay-later/watch-now-pay-later.component';



@Component({
  selector: 'app-channel-buyer',
  templateUrl: './channel-buyer.component.html',
  styleUrls: ['./channel-buyer.component.css']
})
export class ChannelBuyerComponent implements OnInit, OnDestroy{

  decentralandLogo = 'assets/decentraland-mana-logo.png'
  priceSubscription!: Subscription;
  MANA_PRICE = 0;
  maxNumberOfBuyers: number;
  canExecute = true;

  private intervalSubscription?: Subscription;

  channelDollarbalance = 0;
  highestActiveWeeklyViewers = 0;
  connectedWallet?:string;
  channel?: Channels 
  videoInfo: CreatedContentDetails;
  sortedListOfBuyers!: [string, number][];
  isPayLater = false;
  loading = false;
  numberOfBuyers = 0
  countdown = 5;
  isLoggedIn: boolean;

  constructor(
    private contentService:ContentService, 
    private route: ActivatedRoute,
    private paymentService: PaymentServiceService, 
    private userService: UserService,
    private pricingService: PricingService, 
    private connectWalletService: ConnectWalletService,
    private authService: AuthenticationService, 
    private modalService: NgbModal,
    private channelService:ChannelService,
    public alertService:AlertService,
    private cdr: ChangeDetectorRef,
    private transakService:TransakService,
    private channelServiceContract: ChannelServiceContract,
    private router: Router,
  ){
      this.maxNumberOfBuyers = this.contentService.maxNumberOfBuyers
      this.connectedWallet = connectWalletService.getConnectedAccount()

      this.channelService.channel$.subscribe(channel => {
        this.channel = channel
      })

      this.videoInfo = {
        contentId: '',
        creatorID: '',
        contentEnum: '',
        contentName: '',
        youtubeMainVideoID: '',
        youtubeProfilePicture: '',
        youtubeTrailerVideoID: '',
        youtubeUsername: '',
        contentType: '',
        createdDate: 0,
        releaseDate: [],
        numbBidders: 0,
        startingCost: 0,
        hype: 0,
        description: '',
        thumbnail: '',
        isComplete: false,
        isViolator: false,
        duration: null,
        listOfBuyerIds: new Map<string, string>,
        sentEmails: false,
        reportRate: 0,
        contentReports: []
      }

      if(localStorage.getItem('token')){
        this.isLoggedIn = true
      }
      else{
        this.isLoggedIn = false
      }

      this.authService.isLoggedInProfile$.subscribe(isLoggedInStatus => {
        this.isLoggedIn = isLoggedInStatus
        if(isLoggedInStatus){
          this.channelService.getAllUserChannels().subscribe(userChannels =>{
            if(userChannels.length > 0){
              const channel = userChannels[0]
              this.channelService.setChannel(channel)
              this.getHighestAWV(channel.streamerInfo)
      
              this.channelServiceContract.callGetChannelBalance(channel.channelName).then(balance => {
                if(balance){
                  this.channelService.setChannelManaBalance(balance)
                  this.channelDollarbalance = balance;
                }
              })
              
              this.intervalSubscription = interval(5000) 
                .pipe(
                  switchMap(() => this.channelServiceContract.callGetChannelBalance(channel.channelName))
                )
                .subscribe(balance => {
                  if(balance){
                    this.channelService.setChannelManaBalance(balance)
                    this.channelDollarbalance = balance;
                  }
              });
            }
          })
        }
        else{
          if (this.intervalSubscription) {
            this.channel = undefined
            this.intervalSubscription.unsubscribe();
          }
        }
      })
  }

  
  openFundChannelModal(){
    if(this.channel){
      const modelRef = this.modalService.open(FundChannelComponent, {size: 'sm', scrollable: true,centered: true , animation: false})
      modelRef.componentInstance.channelName = this.channel.channelName
      modelRef.result.then(result => {
        if(result === "connecting Wallet"){
          this.connectWalletService.connectAccount().then(() => {
            if(this.channel){
              const modelRef = this.modalService.open(FundChannelComponent, { size: 'sm', scrollable: true, centered: true , animation: false})
              modelRef.componentInstance.channelName = this.channel.channelName
              modelRef.result.then(result => {
                if(result === "fund successful"){
                  this.alertService.addAlert("Channel Successfully Funded", 'success')
                }
                else if(result === "fund failed"){
                  this.alertService.addAlert("Failed to Fund Channel", "danger")
                }
              })
            }
          });
        } 
        else {
          if(result === "fund successful"){
            this.alertService.addAlert("Channel Successfully Funded", 'success')
          }
          else if(result === "fund failed"){
            this.alertService.addAlert("Failed to Fund Channel", "danger")
          }
        }
      })  
    }
    
  }

  createChannel(){
    if(this.isLoggedIn){
      this.router.navigateByUrl('/Create/Channel')
    }
    else{
      this.alertService.addAlert("Must login to create a channel.", "danger")
    }
  }

  formatDuration(totalSeconds: number): string {
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);

    if(hours > 0){
      if(hours == 1){
        if(minutes == 0){
          return `${hours}hr`;
        }
        else if(minutes == 1){
          return `${hours}hr ${minutes}min`;
        }   
        return `${hours}hr ${minutes}mins`;
      }
      return `${hours}hrs ${minutes}mins`;
    }
    return `${minutes}mins`;
    
  }


  startCountdown() {
    this.canExecute = false;
    this.countdown = 5
    this.updateCountdown();
  }

  updateCountdown() {
    if (this.countdown > 0) {
      setTimeout(() => {
        this.countdown--;
        this.updateCountdown();
      }, 1000);
    } else {
      this.canExecute = true;
    }
  }

  purchase(channelName: string) {
    if (!this.canExecute) {
      this.alertService.addAlert(`You can Purchase in ${this.countdown} seconds`, "danger")
      return; // Function can't be executed yet
    }
    if((this.channelDollarbalance * this.MANA_PRICE) < (this.getCost(this.videoInfo.contentType) * this.highestActiveWeeklyViewers)){
      this.alertService.addAlert(`Insufficent Funds`, "danger")
      return; // Function can't be executed yet
    }
    this.loading = true;
    const JWT = window.localStorage.getItem('token') || '{}'; 
    this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
      const channelPaymentRequest: ChannelPaymentRequestPayload = {
          channelName: channelName,
          contentID: this.route.snapshot.params['id'],
      };
      this.channelService.payForContent(channelPaymentRequest).subscribe({
        next: (manaAmount) => {
            const newEntity: [string, number] = [channelName, Number(manaAmount)];
            this.sortedListOfBuyers.push(newEntity);
            this.sortedListOfBuyers.sort((a, b) => b[1] - a[1]);
            this.loading = false;
            this.numberOfBuyers++;
            this.alertService.addAlert('Purchase Successful', 'success');
            const channelBalance = this.channelService.getChannelManaBalance()
            if(channelBalance){
              const cost = this.channelService.getChannelAWV() * this.getCost(this.videoInfo.contentType)
              this.channelService.setChannelManaBalance(channelBalance - cost * this.MANA_PRICE);
            }
        },
        error: (error) => {
            this.loading = false;
            this.alertService.addAlert(error, 'danger');
            this.channelService.setChannelManaBalance(undefined);
        },
        complete: () => {
            
        }
      })
    }).catch((error) => { // This is the error handler for the outer promise
        this.loading = false;
        this.alertService.addAlert(error, 'danger');
        this.channelService.setChannelManaBalance(undefined);
    });
}


  cancelPurchase(channelName: string){
    this.loading = true
    const JWT = window.localStorage.getItem('token') || '{}'
    this.authService.checkJWTExpiration(JWT).then(() => {
      const channelPaymentRequest: ChannelPaymentRequestPayload = {
        channelName: channelName,
        contentID:this.route.snapshot.params['id'],
      }
      this.channelService.refundChannelContent(channelPaymentRequest).subscribe({
          next: () => {
            this.loading = false
            this.sortedListOfBuyers = this.sortedListOfBuyers.filter(([ChannelName, _]) => ChannelName !== channelName);
            this.numberOfBuyers--;
            this.alertService.addAlert("Cancellation Successful", 'success')
          },
          error: (error) => {
            this.loading = false
            this.alertService.addAlert(error, 'danger')
          },
          complete: () => {
          }
        })
      }).catch((error) => { // This is the error handler for the outer promise
        this.loading = false;
        this.alertService.addAlert(error, 'danger');
        this.channelService.setChannelManaBalance(undefined);
    });

  }

  navigateToYouTubeTrailer() {
    window.open('https://www.youtube.com/watch?v=' + this.videoInfo.youtubeTrailerVideoID, '_blank'); 
  }

  openWatchNowPayLaterModal(channelName: string) {
    if (!this.canExecute) {
      this.alertService.addAlert(`You can Purchase in ${this.countdown} seconds`, "danger")
      return; // Function can't be executed yet
    }
    const watchNowPayLaterRequestPayload: WatchNowPayLaterRequestPayload = {
      channelName: channelName,
      contentID: this.route.snapshot.params['id'],
      paymentIncrements: 4
    };
    if((this.channelDollarbalance * this.MANA_PRICE) < (this.getCost(this.videoInfo.contentType) * this.highestActiveWeeklyViewers / watchNowPayLaterRequestPayload.paymentIncrements)){
      this.alertService.addAlert(`Insufficent Funds`, "danger")
      return; // Function can't be executed yet
    }


    this.loading = true;
    const JWT = window.localStorage.getItem('token') || '{}'; 
    this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
      this.channelService.watchNowPayLater(watchNowPayLaterRequestPayload).subscribe({
        next: (manaAmount) => {
            const newEntity: [string, number] = [channelName, Number(manaAmount)];
            this.sortedListOfBuyers.push(newEntity);
            this.sortedListOfBuyers.sort((a, b) => b[1] - a[1]);
            this.loading = false;
            this.numberOfBuyers++;
            this.alertService.addAlert('Purchase Successful', 'success');
            const channelBalance = this.channelService.getChannelManaBalance()
            if(channelBalance){
              const cost = this.channelService.getChannelAWV() * this.getCost(this.videoInfo.contentType)
              this.channelService.setChannelManaBalance(channelBalance - cost / watchNowPayLaterRequestPayload.paymentIncrements * this.MANA_PRICE);
            }
        },
        error: (error) => {
            this.loading = false;
            this.alertService.addAlert(error, 'danger');
            this.channelService.setChannelManaBalance(undefined);
        },
        complete: () => {
            
        }
      })
    })
  }

  openCancelModal(){
    if (!this.canExecute) {
      this.alertService.addAlert(`You can Cancel in ${this.countdown} seconds`, "danger")
      return; // Function can't be executed yet
    }
    if(this.hasPurchased()){
      const index = this.sortedListOfBuyers.findIndex(item => item[0].includes(this.channel!.channelName));
      if (index !== -1) {
        const refundManaAmount = this.sortedListOfBuyers[index][1]
        const modalRef = this.modalService.open(RefundContentComponent, {size: 'sm', scrollable: true,centered: true , animation: false})
        modalRef.componentInstance.refundManaAmount = refundManaAmount;
        modalRef.result.then(result => {
          if(result === "refund content"){
            if(this.channel){
              this.cancelPurchase(this.channel.channelName)
              this.startCountdown();
            }else{
              this.alertService.addAlert("Channel Not Connected", "danger")
            }
          }
        })
      } else {
        this.alertService.addAlert("Refund Amount Not Found", "danger")
      }
      
    }
  }

  getCost(type: string):number {
    switch (type) {
      case 'Short Film':
        return this.contentService.shortFilmPrice;
      case 'Sports':
        return this.contentService.sportsPrice;
      case 'Movies':
        return this.contentService.moviePrice;
      default:
        return 0;
    }
  }

  addSpaceBeforeUppercase(value: string): string {
    return value.replace(/([A-Z])/g, ' $1');
  }

  getStatusClass(status: string) {
    switch (status) {
      case 'Active':
        return 'status-green';
      case 'InProgress':
        return 'status-purple'
      default:
        return 'status-grey';
    }
  }

  isActive(): boolean{
    return this.videoInfo.contentEnum === 'Active'
  }
  
   ngOnInit(): void {
    const videoId: string = this.route.snapshot.params['id'];
    this.contentService.fetchSingleContent(videoId).subscribe(video => {
      this.videoInfo = video
    })
    if(this.isLoggedIn){
        this.channelService.getAllUserChannels().subscribe(userChannels =>{
        if(userChannels.length > 0){
          const channel = userChannels[0]
          this.channelService.setChannel(channel)
          this.getHighestAWV(channel.streamerInfo)

          this.channelServiceContract.callGetChannelBalance(channel.channelName).then(balance => {
            if(balance){
              this.channelService.setChannelManaBalance(balance)
              this.channelDollarbalance = balance;
            }
          })
          
          this.intervalSubscription = interval(5000) 
            .pipe(
              switchMap(() => this.channelServiceContract.callGetChannelBalance(channel.channelName))
            )
            .subscribe(balance => {
              if(balance){
                this.channelService.setChannelManaBalance(balance)
                this.channelDollarbalance = balance;
              }
          });
        }
      })
    }
    

    this.contentService.fetchChannelSortedBuyers(videoId).subscribe(sortedBuyers => {
      this.sortedListOfBuyers =  Object.entries(sortedBuyers);
      this.numberOfBuyers = this.sortedListOfBuyers.length
    });
    
    this.priceSubscription = this.pricingService.getManaPrices().subscribe({
      next: (price) => {
        this.MANA_PRICE = Number(price)
        this.cdr.detectChanges()
      },
      error: (err) => {
        this.priceSubscription.unsubscribe();
      },
      complete: () => {
          
      },
    });
  }

  hasPurchased(): boolean{
    const channel = this.channel
    if(channel){
      if (this.sortedListOfBuyers.some(item => item[0].includes(channel.channelName))) {
        return true;
      }
    }
    return false;
  }


  maxOwnersReached(): boolean{
    return this.videoInfo.listOfBuyerIds.size >= this.contentService.maxNumberOfBuyers
  }



  getHighestAWV(data: StreamerInfo[]): void {
    this.highestActiveWeeklyViewers = data.reduce((max, current) => {
      return current.averageWeeklyViewers > max ? current.averageWeeklyViewers : max;
    }, 0);
    this.channelService.setChannelAWV(this.highestActiveWeeklyViewers);
  }

  ngOnDestroy(): void {
    this.priceSubscription.unsubscribe();
    if (this.intervalSubscription) {
      this.intervalSubscription.unsubscribe();
    }
  }

  connectWallet(){
    this.connectWalletService.connectAccount().then(() => {
      this.connectedWallet = this.connectWalletService.getConnectedAccount();
    });
  }
  

  disconnectWallet(){
    this.connectedWallet = undefined;
  }

  getMoreMana(){
    this.transakService.openTransak()
  }


  hasChannel(){
    if(this.channel && this.channel.channelStatus === "Approved"){
      return true
    }   
    return false;
  }


  getbalance(){
    const channelName = this.channel?.channelName
    if(channelName){
      this.channelServiceContract.callGetChannelBalance(channelName).then((balance) => {
        if(balance){
          this.channelService.setChannelManaBalance(balance)
          this.channelDollarbalance = balance;
        }
      })
    }
  }

  payLater(){
    const JWT = window.localStorage.getItem('token') || '{}'
    const channelId = this.route.snapshot.params['id']
    this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
      if(this.isPayLater){
        this.userService.removeFromPayLater(channelId).subscribe({
          next: () => {},
          error: (error) => {
            this.alertService.addAlert(error, 'danger')
          },
          complete: () => {
            this.alertService.addAlert("Removed From Pay Later", 'success')
            this.isPayLater = false
          }
        })
    } else{
        this.userService.addToPayLater(channelId).subscribe({
          next: () => {},
          error: (error) => {
            this.alertService.addAlert(error, 'danger')
          },
          complete: () => {
            this.alertService.addAlert("Added To Pay Later", 'success')
            this.isPayLater = true
          }
        })
      }
    }) 
  }

  

  OnPayLaterList(): boolean{
    const list = this.userService.getUserPayLater();
    return list.includes(this.route.snapshot.params['id'])
  }

}
