import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormGroup, FormControl, Validators, FormBuilder } from '@angular/forms';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { watchAccount } from '@wagmi/core';
import { Subscription } from 'rxjs';
import { isNumber } from 'src/app/components/Validators/isNumber';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ChannelService } from 'src/app/services/Channel/channel.service';
import { ChannelServiceContract } from 'src/app/services/Contracts/Channel/channel-service-contract.service';
import { config } from 'src/app/services/Contracts/config';
import { WarchestService } from 'src/app/services/Contracts/WarChestSerivce/warchest.service';
import { ConnectWalletService } from 'src/app/services/Mana/ConnectPersonalWallet/connect-wallet.service';
import { PricingService } from 'src/app/services/Mana/PricingService/pricing.service';
import { MessagesService, UserWithdrawRequestPayload } from 'src/app/services/MessageService/messages.service';
import { UserService } from 'src/app/services/User/user-service';

@Component({
  selector: 'app-warchest-withdraw',
  templateUrl: './warchest-withdraw.component.html',
  styleUrls: ['./warchest-withdraw.component.css']
})
export class WarchestWithdrawComponent implements OnInit, OnDestroy{

  connectedWallet?:string;
  userbalance?:string
  priceSubscription!: Subscription;
  MANA_PRICE!: number;

  dollarForm: FormGroup 

  constructor(
    private pricingService: PricingService, 
    private connectWalletService: ConnectWalletService,
    public activeModal: NgbActiveModal,
    private warchestService: WarchestService,
    private userService:UserService,
    private alertService: AlertService,
    private authService:AuthenticationService,
    private messageService: MessagesService,
    private fb: FormBuilder
  ){

    this.dollarForm = this.fb.group({
      dollarAmount: ['', [Validators.required, Validators.pattern(/^\d+(\.\d{1,2})?$/)]]
    });
    this.connectedWallet = connectWalletService.getConnectedAccount()
    watchAccount(config, {
      onChange: (account) => { 
        const address = account.address;
        if (address) {
          this.connectedWallet = address
        }else{
          this.connectedWallet = undefined
        }
      },
    });
  }


  ngOnInit(): void {
    if(this.connectedWallet){
      this.priceSubscription = this.pricingService.getManaPrices().subscribe({
        next: (price) => {
          this.MANA_PRICE = Number(price)
        },
        error: (err) => {
          this.priceSubscription.unsubscribe();
        },
        complete: () => {
            
        },
      });
    }
  }


  ngOnDestroy(): void {
    if(this.priceSubscription){
      this.priceSubscription.unsubscribe();
    }
  }



  getbalance(){
    this.warchestService.callGetUserBalance(this.userService.getUserID()).then(balance => {
      if(balance !== null && balance !== undefined){
        this.userbalance = Number(balance * this.MANA_PRICE).toFixed(2);
      }
    })
  }

  get totalWithdrawn(): number {
    const dollarAmount = this.dollarForm.get('dollarAmount')?.value;
    return dollarAmount ? dollarAmount / 0.9 : 0;
  }

  withdrawFunds(){
    if(this.connectedWallet){
      if(Number(this.userbalance) < this.totalWithdrawn){
        this.alertService.addAlert("Amount entered exceeds your balance.", 'danger')
        return
      }
      if(this.userService.getUserRank() == 1){
        this.alertService.addAlert("Must Be Reach Rank 2", 'danger')
        return
      }
      if(this.connectedWallet !== this.userService.getPersonalWallet()){
        this.alertService.addAlert("Connect to the Crypto Address " + this.userService.getPersonalWallet() + " To Withdraw", 'danger')
        return
      }
      if(this.dollarForm.valid){
        this.warchestService.callUserWithdraw(this.userService.getUserID(), this.totalWithdrawn).then(transactionHash => {
          if(transactionHash){
            const JWT = window.localStorage.getItem('token') || '{}'
            this.authService.checkJWTExpiration(JWT).then((JWTResult) =>{
              const userWithdrawRequestPayload: UserWithdrawRequestPayload = {
                dollarAmount: this.dollarForm.get("dollarAmount")?.value,
                transactionHash: transactionHash,
              }
              this.messageService.userWithdrawMessage(userWithdrawRequestPayload).subscribe({
                next: () => {},
                error: (error) => {
                  this.alertService.addAlert(error, 'danger')
                },
                complete: () => {
                  this.alertService.addAlert('Withdraw Successful', 'success')
                  this.activeModal.close('Withdraw')
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
        this.alertService.addAlert('Please enter a valid dollar amount (e.g., 10.00)', 'danger')
      }
    }
    else{
      this.connectedWallet = undefined
    }
    
  }



}

