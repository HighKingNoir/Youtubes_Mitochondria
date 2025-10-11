
import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Subscription } from 'rxjs';
import { CreatedContentDetails } from 'src/app/models/Content/CreatedContentDetails';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ContentService } from 'src/app/services/Content/Content/content.service';
import { BidService } from 'src/app/services/Contracts/BidService/bid.service';
import { ConnectWalletService } from 'src/app/services/Mana/ConnectPersonalWallet/connect-wallet.service';
import { PricingService } from 'src/app/services/Mana/PricingService/pricing.service';
import {  PaymentServiceService } from 'src/app/services/PaymentService/payment-service.service';
import {  UserService } from 'src/app/services/User/user-service';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { isNumber } from '../../Validators/isNumber';
import { TransakService } from 'src/app/services/Mana/Transak/transak.service';
import { getAccount, watchAccount } from '@wagmi/core';
import { DecentralandManaService } from 'src/app/services/Contracts/DecentralandManaService/decentraland-mana.service';
import { TransferManaComponent } from '../../Accessories/Popups/transfer-mana/transfer-mana.component';
import { config } from 'src/app/services/Contracts/config';
import { GaslessFunctionCallPayload, GaslessFunctionCallService } from 'src/app/services/Contracts/GaslessFunctionCall/gasless-function-call.service';
import { parseUnits } from 'viem';


@Component({
    selector: 'app-single-buyer',
    templateUrl: './single-buyer.component.html',
    styleUrls: ['./single-buyer.component.css'],
    standalone: false
})
export class SingleBuyerComponent implements OnInit, OnDestroy{

  decentralandLogo = 'assets/decentraland-mana-logo.png'
  priceSubscription!: Subscription;
  MANA_PRICE = 0;
  videoInfo: CreatedContentDetails;
  connectedWallet?:string;
  sortedListOfBuyers!: [string, number][];
  hasPaid = false;
  isPayLater = false;
  loading = false;
  accountbalance = 0
  manaForm: FormGroup 
  isLoggedIn: boolean;
  userId: string = '';

  constructor(
    private contentService:ContentService, 
    private route: ActivatedRoute,
    private paymentService: PaymentServiceService, 
    private userService: UserService,
    private pricingService: PricingService, 
    private connectWalletService: ConnectWalletService,
    private bidService: BidService, 
    private authService: AuthenticationService, 
    private modalService: NgbModal,
    public alertService:AlertService,
    private cdr: ChangeDetectorRef,
    private transakService:TransakService,
    private decentralandManaService: DecentralandManaService,
    private router: Router,
    private gaslessFunctionCallService: GaslessFunctionCallService,
  ){
      this.manaForm = new FormGroup({
        dollarAmount : new FormControl('',[Validators.required, isNumber]),
      })
    
      if(this.userService.getUserID() === ''){
        this.userService.userInfo$.subscribe(() => {
          this.isPayLater = this.OnPayLaterList()
        })
      }
      else{
        this.isPayLater = this.OnPayLaterList()
      }
      
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
          if(this.userService.getUserID() === ''){
            this.userService.userInfo$.subscribe(() => {
              this.userId = this.userService.getUserID()
              this.getManaPricing(this.userId )
              this.hasPaid = this.sortedListOfBuyers.some(item => item[0].includes(this.userService.getUsername()))
            })
          }
          else{
            this.userId = this.userService.getUserID()
            this.getManaPricing(this.userId)
            this.hasPaid = this.sortedListOfBuyers.some(item => item[0].includes(this.userService.getUsername()))
          }
        }
        else{
          this.priceSubscription.unsubscribe()
        }
      })

      this.connectedWallet = this.connectWalletService.getConnectedAccount()
      if(this.connectedWallet){
        this.connectWalletService.accountManabalance(this.connectedWallet).then(balance =>{
          this.accountbalance = balance
        })
      }
      else{
        if(localStorage.getItem("@appkit/connection_status") == "connected"){
          const cache = localStorage.getItem("@appkit/ens_cache");
          if (cache) {
            const parsed = JSON.parse(cache);
            const addressKey = Object.keys(parsed)[0];
            const address = parsed[addressKey]?.address;
            this.connectedWallet = address
            if(this.connectedWallet){
              this.connectWalletService.accountManabalance(this.connectedWallet).then(balance =>{
                this.accountbalance = balance
              })
            }
          }     
        }
      }

      watchAccount(config, {
        onChange: (account) => { 
          const address = account.address;
          if (address) {
            this.connectedWallet = address
            this.connectWalletService.accountManabalance(this.connectedWallet).then(balance =>{
              this.accountbalance = balance
            })
          }else{
            this.connectedWallet = undefined
          }
        },
      });

  }

  




  bid(){
    if(this.manaForm.get('dollarAmount')?.value < this.videoInfo.startingCost){
      this.alertService.addAlert(`Must be atleast $${this.videoInfo.startingCost} to place bid`, 'danger')
      return
    }
    const connectedWallet = this.connectWalletService.getConnectedAccount();
    if(connectedWallet){
      this.decentralandManaService.getBidAllowance(connectedWallet).then(allowedMana => {
        const mana = (this.manaForm.get('dollarAmount')?.value / this.MANA_PRICE).toString();
        if(allowedMana == 0){
          const modelRef = this.modalService.open(TransferManaComponent, {size: 'lg', scrollable: true, centered: true , animation: false})
          modelRef.componentInstance.approveContract = true;
          modelRef.componentInstance.bidContract = true;
           modelRef.componentInstance.mana = Number(mana)
        }
        else if(allowedMana! < Number(mana)){
          const modelRef = this.modalService.open(TransferManaComponent, {size: 'lg', scrollable: true, centered: true , animation: false})
          modelRef.componentInstance.bidContract = true;
          modelRef.componentInstance.allowance = allowedMana;
          modelRef.componentInstance.mana = Number(mana)
        }
        else{
            this.bidService.callPlaceBid(this.videoInfo.contentId, this.userService.getUserID(), mana).then(transactionHash => {
            if(transactionHash){
              this.loading = true
              const JWT = window.localStorage.getItem('token') || '{}'
              this.authService.checkJWTExpiration(JWT).then((JWTResult) =>{
                const paymentRequestPayload = {
                  transactionHash: transactionHash,
                  contentID: this.route.snapshot.params['id'],
                  manaAmount: mana,
                  dollarAmount: this.manaForm.get('dollarAmount')?.value,
                }
                this.paymentService.newPayment(paymentRequestPayload).subscribe({
                  next: () => {},
                  error: (error) => {
                    this.loading = false
                    this.alertService.addAlert(error, 'danger')
                  },
                  complete: () => {
                    this.loading = false
                    this.hasPaid = true;
                    const newEntity: [string, number] = [this.userService.getUsername(), Number(mana)];
                    this.sortedListOfBuyers.push(newEntity);
                    this.sortedListOfBuyers.sort((a, b) => b[1] - a[1]);
                    this.alertService.addAlert('Bid Successful', 'success')
                    if(this.connectedWallet){
                      this.connectWalletService.accountManabalance(this.connectedWallet).then(balance =>{
                        this.accountbalance = balance
                      })
                    }
                  }
                })
              })
            }
            else{
              this.alertService.addAlert('Failed Transaction', 'danger')
            }
          });
        }
      })
    }
    else{
      this.connectedWallet = undefined
    }
    
  }

  raiseBid(){
    if(this.manaForm.get('dollarAmount')?.value < 5){
      this.alertService.addAlert("Must be atleast $5 to raise bid", 'danger')
      return
    }
    const connectedWallet = this.connectWalletService.getConnectedAccount();
    if(connectedWallet){
      const mana = (this.manaForm.get('dollarAmount')?.value / this.MANA_PRICE).toString();
      if(this.accountbalance * this.MANA_PRICE < Number(mana) * this.MANA_PRICE){
        this.alertService.addAlert("Amount Entered Exceeds Your Mana balance", "danger")
        return
      }
      this.decentralandManaService.getBidAllowance(connectedWallet).then(allowedMana => {
        if(allowedMana! < Number(mana)){
          const modelRef = this.modalService.open(TransferManaComponent, {size: 'lg', scrollable: true, centered: true , animation: false})
          modelRef.componentInstance.bidContract = true;
          modelRef.componentInstance.allowance = allowedMana;
          modelRef.componentInstance.mana = Number(mana)
        }
        else{
          this.bidService.callRaiseBid(this.videoInfo.contentId, this.userService.getUserID(), mana).then(transactionHash => {
            if(transactionHash){
              this.loading = true
              const JWT = window.localStorage.getItem('token') || '{}'
              this.authService.checkJWTExpiration(JWT).then((JWTResult) =>{
                const paymentRequestPayload = {
                  transactionHash: transactionHash,
                  contentID: this.route.snapshot.params['id'],
                  manaAmount: mana,
                  dollarAmount: this.manaForm.get('dollarAmount')?.value,
                }
                this.paymentService.updatePayment(paymentRequestPayload).subscribe({
                  next: () => {},
                  error: (error) => {
                    this.loading = false
                    this.alertService.addAlert(error, 'danger')
                  },
                  complete: () => {
                    this.loading = false
                    this.hasPaid = true;
                    const indexToUpdate = this.sortedListOfBuyers.findIndex(([username]) => username === this.userService.getUsername());
                    // Check if the entity exists in the array
                    if (indexToUpdate !== -1) {
                      this.sortedListOfBuyers[indexToUpdate][1] += Number(mana);
                      // Sort the array based on mana amount in descending order
                      this.sortedListOfBuyers.sort((a, b) => b[1] - a[1]);
                    }
                    this.alertService.addAlert('Bid Increase Successful', 'success')
                    if(this.connectedWallet){
                      this.connectWalletService.accountManabalance(this.connectedWallet).then(balance =>{
                        this.accountbalance = balance
                      })
                    }
                  }
                })
              })
            }
            else{
              this.alertService.addAlert('Failed Transaction', 'danger')
            }
          });
        }
      })
    }
    else{
      this.connectedWallet = undefined
    }
  }

  cancelBid(){
    if(this.connectWalletService.isConnected()){
      this.bidService.callCancelBid(this.videoInfo.contentId, this.userService.getUserID()).then(transactionHash => {
        if(transactionHash){
          this.loading = true
          const JWT = window.localStorage.getItem('token') || '{}'
          this.authService.checkJWTExpiration(JWT).then((JWTResult) =>{
            const refundRequestPayload = {
              
              contentID: this.videoInfo.contentId,
              transactionHash: transactionHash,
            }
            this.paymentService.refundPayment(refundRequestPayload).subscribe({
              next: () => {},
              error: (error) => {
                this.alertService.addAlert(error, 'danger')
              },
              complete: () => {
                this.loading = false
                this.hasPaid = false;
                this.sortedListOfBuyers = this.sortedListOfBuyers.filter(([username, _]) => username !== this.userService.getUsername());
                this.alertService.addAlert('Bid Cancellation Successful', 'success')
                if(this.connectedWallet){
                  this.connectWalletService.accountManabalance(this.connectedWallet).then(balance =>{
                    this.accountbalance = balance
                  })
                }
              }
            })
          })
        }
        else{
          this.alertService.addAlert('Failed Transaction', 'danger')
        }
      });
    }
    else{
      this.connectedWallet = undefined
    }
    
  }

   async gaslessPlaceBid(){
    if(!this.connectWalletService.isConnected()){
      return;
      
    }
    if(this.manaForm.get('dollarAmount')?.value < this.videoInfo.startingCost){
      this.alertService.addAlert(`Must be atleast $${this.videoInfo.startingCost} to place bid`, 'danger')
      return
    }
      const JWT = window.localStorage.getItem('token') || '{}'
      const userId = this.userService.getUserID()
      const contentId = this.videoInfo.contentId
      const deadline = BigInt(Math.floor(Date.now() / 1000) + 600);
      const sender = this.connectWalletService.getConnectedAccount() as `0x${string}`
      const dollarAmount = this.manaForm.get('dollarAmount')?.value;
      const manaTokens = dollarAmount / this.MANA_PRICE;
      const amount = parseUnits(manaTokens.toString(), 18);
      const fee = parseUnits((.03 / this.MANA_PRICE).toString(), 18);
      this.gaslessFunctionCallService.getPermitSignature({sender, amount, fee, deadline}).then(signature =>{
        const gaslessFunctionCallPayload:GaslessFunctionCallPayload = {
          signature: signature,
          sender: sender,
          contentID: contentId,
          channelName: null,
          userID: userId,
          amount: amount,
          fee: fee,
          deadline: deadline
        }
        this.loading = true
        this.authService.checkJWTExpiration(JWT).then((JWTResult) =>{
          this.gaslessFunctionCallService.placeBid(gaslessFunctionCallPayload).subscribe({
            next: () => {},
            error: (error) => {
              this.loading = false
              this.alertService.addAlert(error, 'danger')
            },
            complete: () => {
              this.loading = false
              this.hasPaid = true;
              const newEntity: [string, number] = [this.userService.getUsername(), Number(manaTokens)];
              this.sortedListOfBuyers.push(newEntity);
              this.sortedListOfBuyers.sort((a, b) => b[1] - a[1]);
              this.alertService.addAlert('Bid Successful', 'success')
              if(this.connectedWallet){
                this.connectWalletService.accountManabalance(this.connectedWallet).then(balance =>{
                  this.accountbalance = balance
                })
              }
            }
          })
        })
      })
     }
  
     async gaslessRaiseBid(){
      if(!this.connectWalletService.isConnected()){
        return;
      }
      if(this.manaForm.get('dollarAmount')?.value < 5){
        this.alertService.addAlert("Must be atleast $5 to raise bid", 'danger')
        return
      }
      const JWT = window.localStorage.getItem('token') || '{}'
      const deadline = BigInt(Math.floor(Date.now() / 1000) + 600);
      const sender = this.connectWalletService.getConnectedAccount() as `0x${string}`
      const userId = this.userService.getUserID()
      const contentId = this.videoInfo.contentId
      const dollarAmount = this.manaForm.get('dollarAmount')?.value;
      const manaTokens = dollarAmount / this.MANA_PRICE;
      const amount = parseUnits(manaTokens.toString(), 18);
      const fee = parseUnits((.03 / this.MANA_PRICE).toString(), 18);
      this.gaslessFunctionCallService.getPermitSignature({sender, amount, fee, deadline}).then(signature =>{
        const gaslessFunctionCallPayload:GaslessFunctionCallPayload = {
          signature: signature,
          sender: sender,
          contentID: contentId,
          channelName: null,
          userID: userId,
          amount: amount,
          fee: fee,
          deadline: deadline
        }
        this.loading = true
        this.authService.checkJWTExpiration(JWT).then((JWTResult) =>{
          this.gaslessFunctionCallService.raiseBid(gaslessFunctionCallPayload).subscribe({
            next: () => {},
            error: (error) => {
              this.loading = false
              this.alertService.addAlert(error, 'danger')
            },
            complete: () => {
              this.loading = false
              this.hasPaid = true;
              const indexToUpdate = this.sortedListOfBuyers.findIndex(([username]) => username === this.userService.getUsername());
              // Check if the entity exists in the array
              if (indexToUpdate !== -1) {
                this.sortedListOfBuyers[indexToUpdate][1] += Number(manaTokens);
                // Sort the array based on mana amount in descending order
                this.sortedListOfBuyers.sort((a, b) => b[1] - a[1]);
              }
              this.alertService.addAlert('Bid Increase Successful', 'success')
              if(this.connectedWallet){
                this.connectWalletService.accountManabalance(this.connectedWallet).then(balance =>{
                  this.accountbalance = balance
                })
              }
            }
          })
        })
      })
     }
  
     async gaslessCancelBid(){
      if(!this.connectWalletService.isConnected()){
        return;
      }
      const JWT = window.localStorage.getItem('token') || '{}'
      const deadline = BigInt(Math.floor(Date.now() / 1000) + 600);
      const amount = 0n;
      const sender = this.connectWalletService.getConnectedAccount() as `0x${string}`
      const userId = this.userService.getUserID()
      const contentId = this.videoInfo.contentId
      const fee = parseUnits((.03 / this.MANA_PRICE).toString(), 18);
      this.gaslessFunctionCallService.getPermitSignature({sender, amount, fee, deadline}).then(signature =>{
        const gaslessFunctionCallPayload:GaslessFunctionCallPayload = {
          signature: signature,
          sender: sender,
          contentID: contentId,
          channelName: null,
          userID: userId,
          amount: amount,
          fee: fee,
          deadline: deadline
        }
        this.loading = true
        this.authService.checkJWTExpiration(JWT).then((JWTResult) =>{
          this.gaslessFunctionCallService.cancelBid(gaslessFunctionCallPayload).subscribe({
              next: () => {},
              error: (error) => {
                this.alertService.addAlert(error, 'danger')
              },
              complete: () => {
                this.loading = false
                this.hasPaid = false;
                this.sortedListOfBuyers = this.sortedListOfBuyers.filter(([username, _]) => username !== this.userService.getUsername());
                this.alertService.addAlert('Bid Cancellation Successful', 'success')
                if(this.connectedWallet){
                  this.connectWalletService.accountManabalance(this.connectedWallet).then(balance =>{
                    this.accountbalance = balance
                  })
                }
              }
            })
        })
      })
     }

  navigateToYouTubeTrailer() {
    window.open('https://www.youtube.com/watch?v=' + this.videoInfo.youtubeTrailerVideoID, '_blank'); 
  }

  isActive(): boolean{
    return this.videoInfo.contentEnum === 'Active'
  }

  getManaPricing(userId: string){
    this.priceSubscription = this.pricingService.getManaPrices(userId).subscribe({
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




  ngOnInit(): void {
    const videoId: string = this.route.snapshot.params['id'];
    this.contentService.fetchSingleContent(videoId).subscribe(video => {
      this.videoInfo = video
      this.contentService.fetchSortedBuyers(this.videoInfo.contentId).subscribe(sortedBuyers => {
        this.sortedListOfBuyers =  Object.entries(sortedBuyers);
        if(this.userService.getUserID() === ''){
          this.userService.userInfo$.subscribe(() => {
            this.userId = this.userService.getUserID()
            this.getManaPricing(this.userId)
            this.hasPaid = this.sortedListOfBuyers.some(item => item[0].includes(this.userService.getUsername()))
          })
        }
        else{
          this.userId = this.userService.getUserID()
          this.getManaPricing(this.userId)
          this.hasPaid = this.sortedListOfBuyers.some(item => item[0].includes(this.userService.getUsername()))
        }
      });
    })



  }

  ngOnDestroy(): void {
    if(this.priceSubscription){
      this.priceSubscription.unsubscribe()
    }
  }

  connectWallet(){
    if(this.isLoggedIn){
      this.connectWalletService.connectAccount().then(() => {
        this.connectedWallet = this.connectWalletService.getConnectedAccount()
        if(this.connectedWallet){
          this.connectWalletService.accountManabalance(this.connectedWallet).then(balance =>{
            this.accountbalance = balance
          })
        }
      });
    }
    else{
      this.alertService.addAlert("Login before you connect your wallet.", "danger")
    }
  }
  

  disconnectWallet(){
    this.connectWalletService.disconnectAccount().then(() => {
      this.connectedWallet = undefined
    })
  }

  

  getMoreMana(){
    this.transakService.openTransak()
  }

  max(){
    this.manaForm.get('dollarAmount')?.setValue((this.accountbalance * this.MANA_PRICE).toFixed(2))
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
  payLater(){
    const JWT = window.localStorage.getItem('token') || '{}'
    this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
      const contentID = this.route.snapshot.params['id']
      if(this.isPayLater){
        this.userService.removeFromPayLater(contentID).subscribe({
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
        this.userService.addToPayLater(contentID).subscribe({
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

  OnPayLaterList(): boolean{
    const list = this.userService.getUserPayLater();
    return list.includes(this.route.snapshot.params['id'])
  }

 }