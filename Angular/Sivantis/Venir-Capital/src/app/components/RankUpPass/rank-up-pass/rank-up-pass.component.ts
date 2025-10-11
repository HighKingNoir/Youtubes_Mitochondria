import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { DecentralandManaService } from 'src/app/services/Contracts/DecentralandManaService/decentraland-mana.service';
import { ConnectWalletService } from 'src/app/services/Mana/ConnectPersonalWallet/connect-wallet.service';
import { UserService } from 'src/app/services/User/user-service';
import { Subscription } from 'rxjs';
import { PricingService } from 'src/app/services/Mana/PricingService/pricing.service';
import { watchAccount } from '@wagmi/core';
import { config } from 'src/app/services/Contracts/config';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { TransferManaComponent } from '../../Accessories/Popups/transfer-mana/transfer-mana.component';
import { PayToRankUpServiceService } from 'src/app/services/Contracts/PayToRankUp/pay-to-rank-up-service.service';
import { PaymentServiceService } from 'src/app/services/PaymentService/payment-service.service';

@Component({
  selector: 'app-rank-up-pass',
  standalone: false,
  templateUrl: './rank-up-pass.component.html',
  styleUrl: './rank-up-pass.component.css'
})
export class RankUpPassComponent implements OnInit{
  
  priceSubscription!: Subscription;
  decentralandLogo = 'assets/decentraland-mana-logo.png'
  MANA_PRICE = 0;
  isLoggedIn: boolean;
  userId: string = '';
  connectedWallet?:string;
  accountbalance = 0
  loading = false;
  hasPaidArchonPassList: boolean = false
  hasPaidMasterPassList: boolean = false

  constructor(
    private decentralandManaService: DecentralandManaService,
    private connectWalletService: ConnectWalletService,
    private modalService: NgbModal,
    private authService: AuthenticationService, 
    private userService: UserService,
    private cdr: ChangeDetectorRef,
    private pricingService: PricingService, 
    public alertService:AlertService,
    private payToRankUpService: PayToRankUpServiceService,
    private paymentService: PaymentServiceService
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
          if(this.userService.getUserID() === ''){
            this.userService.userInfo$.subscribe(() => {
              this.userId = this.userService.getUserID()
              this.payToRankUpService.callHasPaidArchonPassList(this.userId).then(result => {
                this.hasPaidArchonPassList = result || false
              })
              this.payToRankUpService.callHasPaidMasterPassList(this.userId).then(result => {
                this.hasPaidMasterPassList = result || false
              })
              this.getManaPricing(this.userId)
            })
          }
          else{
            this.userId = this.userService.getUserID()
            this.payToRankUpService.callHasPaidArchonPassList(this.userId).then(result => {
              this.hasPaidArchonPassList = result || false
            })
            this.payToRankUpService.callHasPaidMasterPassList(this.userId).then(result => {
              this.hasPaidMasterPassList = result || false
            })
            this.getManaPricing(this.userId)
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

  ngOnInit(): void {
    if(this.userService.getUserID() === ''){
      this.userService.userInfo$.subscribe(() => {
        this.userId = this.userService.getUserID()
        this.payToRankUpService.callHasPaidArchonPassList(this.userId).then(result => {
          this.hasPaidArchonPassList = result || false
        })
        this.payToRankUpService.callHasPaidMasterPassList(this.userId).then(result => {
          this.hasPaidMasterPassList = result || false
        })
        this.getManaPricing(this.userId)
      })
    }
    else{
      this.userId = this.userService.getUserID()
      this.payToRankUpService.callHasPaidArchonPassList(this.userId).then(result => {
        this.hasPaidArchonPassList = result || false
      })
      this.payToRankUpService.callHasPaidMasterPassList(this.userId).then(result => {
        this.hasPaidMasterPassList = result || false
      })
      this.getManaPricing(this.userId)
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


  archonPass(){
    if(!this.isLoggedIn){
      return
    }
    const connectedWallet = this.connectWalletService.getConnectedAccount();
    if(connectedWallet){
      const manaNeeded = 250000 / this.MANA_PRICE;
      this.connectWalletService.accountManabalance(connectedWallet).then(accountBalance => {
        if(manaNeeded > accountBalance){
          this.alertService.addAlert("You do not have enough MANA","danger")
          return;
        }
        this.decentralandManaService.getPayToRankUpAllowance(connectedWallet).then(allowedMana => {
          if(allowedMana == 0){
            const modelRef = this.modalService.open(TransferManaComponent, {size: 'lg', scrollable: true, centered: true , animation: false})
            modelRef.componentInstance.approveContract = true;
            modelRef.componentInstance.payToRankUpContract = true;
            modelRef.componentInstance.mana = manaNeeded
          }
          else if(allowedMana! < manaNeeded){
            const modelRef = this.modalService.open(TransferManaComponent, {size: 'lg', scrollable: true, centered: true , animation: false})
            modelRef.componentInstance.payToRankUpContract = true;
            modelRef.componentInstance.allowance = allowedMana;
            modelRef.componentInstance.mana = manaNeeded
          }
          else{
              this.payToRankUpService.callArchonPass(this.userId).then(transactionHash => {
              if(transactionHash){
                this.loading = true
                const JWT = window.localStorage.getItem('token') || '{}'
                this.authService.checkJWTExpiration(JWT).then((JWTResult) =>{
                  this.paymentService.purchaseArchonPass(transactionHash).subscribe({
                    next: () => {},
                    error: (error) => {
                      this.loading = false
                      this.alertService.addAlert(error, 'danger')
                    },
                    complete: () => {
                      this.loading = false
                      this.hasPaidArchonPassList = true;
                      this.alertService.addAlert('Archon Pass Transaction Successful', 'success')
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
      })
      }
      else{
        this.connectedWallet = undefined
      }
    }

    masterPass(){
    if(!this.isLoggedIn){
      return
    }
      const connectedWallet = this.connectWalletService.getConnectedAccount();
      if(connectedWallet){
        const manaNeeded = 75000 / this.MANA_PRICE;
        this.connectWalletService.accountManabalance(connectedWallet).then(accountBalance =>{
          if(manaNeeded > accountBalance){
          this.alertService.addAlert("You do not have enough MANA","danger")
          return;
          }
          this.decentralandManaService.getPayToRankUpAllowance(connectedWallet).then(allowedMana => {
          if(allowedMana == 0){
            const modelRef = this.modalService.open(TransferManaComponent, {size: 'lg', scrollable: true, centered: true , animation: false})
            modelRef.componentInstance.approveContract = true;
            modelRef.componentInstance.payToRankUpContract = true;
            modelRef.componentInstance.mana = manaNeeded
          }
          else if(allowedMana! < manaNeeded){
            const modelRef = this.modalService.open(TransferManaComponent, {size: 'lg', scrollable: true, centered: true , animation: false})
            modelRef.componentInstance.payToRankUpContract = true;
            modelRef.componentInstance.allowance = allowedMana;
            modelRef.componentInstance.mana = manaNeeded
          }
          else{
              this.payToRankUpService.callMasterPass(this.userId).then(transactionHash => {
              if(transactionHash){
                this.loading = true
                const JWT = window.localStorage.getItem('token') || '{}'
                this.authService.checkJWTExpiration(JWT).then((JWTResult) =>{
                  this.paymentService.purchaseMasterPass(transactionHash).subscribe({
                    next: () => {},
                    error: (error) => {
                      this.loading = false
                      this.alertService.addAlert(error, 'danger')
                    },
                    complete: () => {
                      this.loading = false
                      this.hasPaidMasterPassList = true;
                      this.alertService.addAlert('Master Pass Transaction Successful', 'success')
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
        })
      }
      else{
        this.connectedWallet = undefined
      }
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
}
