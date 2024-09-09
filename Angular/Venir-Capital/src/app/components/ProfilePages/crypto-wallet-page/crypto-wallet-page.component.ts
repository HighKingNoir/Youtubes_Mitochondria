import { Component, OnInit } from '@angular/core';

import { User } from 'src/app/models/Users/User';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { ConnectWalletService } from 'src/app/services/Mana/ConnectPersonalWallet/connect-wallet.service';
import { UserService } from 'src/app/services/User/user-service';
import { ConnectPersonalWalletComponent } from '../../Accessories/Popups/PersonalWallet/connect-personal-wallet/connect-personal-wallet.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { DecentralandManaService } from 'src/app/services/Contracts/DecentralandManaService/decentraland-mana.service';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { isNumber } from '../../Validators/isNumber';

@Component({
  selector: 'app-crypto-wallet-page',
  templateUrl: './crypto-wallet-page.component.html',
  styleUrls: ['./crypto-wallet-page.component.css']
})
export class CryptoWalletPageComponent {

  personalWallet?:string
  connectedWallet?:string
  bidAllowance = 0
  fundingAllowance = 0

  increaseBidAllowanceForm: FormGroup 
  decreaseBidAllowanceForm: FormGroup 
  increaseFundingAllowanceForm: FormGroup 
  decreaseFundingAllowanceForm: FormGroup 


  constructor(
    public alertService:AlertService,
    private userService: UserService,
    private connectWalletService: ConnectWalletService,
    private modalService: NgbModal,
    private decentralandManaService:DecentralandManaService
  ){
    this.increaseBidAllowanceForm = new FormGroup({
      manaAmount : new FormControl('',[Validators.required, isNumber]),
    })
    this.decreaseBidAllowanceForm = new FormGroup({
      manaAmount : new FormControl('',[Validators.required, isNumber]),
    })
    this.increaseFundingAllowanceForm = new FormGroup({
      manaAmount : new FormControl('',[Validators.required, isNumber]),
    })
    this.decreaseFundingAllowanceForm = new FormGroup({
      manaAmount : new FormControl('',[Validators.required, isNumber]),
    })

    if(this.userService.getUserID() === ''){
      this.userService.userInfo$.subscribe(() => {
          this.personalWallet = this.userService.getPersonalWallet()
          const connectedWallet = this.connectWalletService.getConnectedAccount()
          if(connectedWallet){
            this.connectedWallet = connectedWallet
            this.decentralandManaService.getBidAllowance(this.connectedWallet).then(bidAllowance => {
              this.bidAllowance = bidAllowance
            })
            this.decentralandManaService.getChannelAllowance(this.connectedWallet).then(fundingAllowance => {
              this.fundingAllowance = fundingAllowance
            })
          }
          else if(this.personalWallet){
            this.connectedWallet = this.personalWallet
            this.decentralandManaService.getBidAllowance(this.connectedWallet).then(bidAllowance => {
              this.bidAllowance = bidAllowance
            })
            this.decentralandManaService.getChannelAllowance(this.connectedWallet).then(fundingAllowance => {
              this.fundingAllowance = fundingAllowance
            })
          }
      })
    }
    else{
      this.personalWallet = this.userService.getPersonalWallet()
          const connectedWallet = this.connectWalletService.getConnectedAccount()
          if(connectedWallet){
            this.connectedWallet = connectedWallet
            this.decentralandManaService.getBidAllowance(this.connectedWallet).then(bidAllowance => {
              this.bidAllowance = bidAllowance
            })
            this.decentralandManaService.getChannelAllowance(this.connectedWallet).then(fundingAllowance => {
              this.fundingAllowance = fundingAllowance
            })
          }
          else if(this.personalWallet){
            this.connectedWallet = this.personalWallet
            this.decentralandManaService.getBidAllowance(this.connectedWallet).then(bidAllowance => {
              this.bidAllowance = bidAllowance
            })
            this.decentralandManaService.getChannelAllowance(this.connectedWallet).then(fundingAllowance => {
              this.fundingAllowance = fundingAllowance
            })
          }
    }
  }

  connectWallet(){
    this.modalService.open(ConnectPersonalWalletComponent, { size: 'md', scrollable: true, centered: true , animation: false, }).result.then((result) => {
      if(result === "connecting Wallet"){
        this.connectWalletService.connectAccount().then(() => {
          this.modalService.open(ConnectPersonalWalletComponent, { size: 'md', scrollable: true, centered: true , animation: false}).result.then(result => {
            if(result === "personal wallet connected"){
              this.personalWallet = this.userService.getPersonalWallet()
              this.alertService.addAlert("Wallet Successfully Set", 'success')
            }
          })
        })
      }
    })
  }

  viewAllowance(){
    this.connectWalletService.connectAccount().then(() => {
      const wallet = this.connectWalletService.getConnectedAccount()
      if(wallet){
        this.connectedWallet = wallet
        this.decentralandManaService.getBidAllowance(wallet).then(allowance => {
          this.bidAllowance = allowance
        })
        this.decentralandManaService.getChannelAllowance(wallet).then(allowance => {
          this.fundingAllowance = allowance
        }) 
      }
    })
  }

  increaseBidAllowance(){
    const manaAmount: number = this.increaseBidAllowanceForm.get('manaAmount')?.value
    if(!this.connectWalletService.isConnected()){
      this.connectWalletService.connectAccount().then(() => {
        this.connectedWallet = this.connectWalletService.getConnectedAccount()
        if(this.connectedWallet === undefined){
          this.alertService.addAlert("Connect cryto wallet", "danger")
          return
        }
        if(manaAmount){
          this.decentralandManaService.increaseAllowanceBidService(manaAmount.toString() + '').then(transactionHash =>{
            if(transactionHash){
              this.increaseBidAllowanceForm.reset()
              this.decentralandManaService.getBidAllowance(this.connectedWallet!).then(bidAllowance => {
                this.bidAllowance = bidAllowance
              })
            }
          })
        }
        else{
          this.alertService.addAlert("Enter Mana Amount", "danger")
        }
      })
    }else{
      if(manaAmount){
        this.decentralandManaService.increaseAllowanceBidService(manaAmount.toString() + '').then(transactionHash =>{
          if(transactionHash){
            this.increaseBidAllowanceForm.reset()
            this.decentralandManaService.getBidAllowance(this.connectedWallet!).then(bidAllowance => {
              this.bidAllowance = bidAllowance
            })
          }
        })
      }
      else{
        this.alertService.addAlert("Enter Mana Amount", "danger")
      }
    }
  }

  decreaseBidAllowance(){
    const manaAmount: number = this.decreaseBidAllowanceForm.get('manaAmount')?.value
    if(manaAmount){

    }
    if(!this.connectWalletService.isConnected()){
      this.connectWalletService.connectAccount().then(() => {
        this.connectedWallet = this.connectWalletService.getConnectedAccount()
        if(this.connectedWallet === undefined){
          this.alertService.addAlert("Connect cryto wallet", "danger")
          return
        }
        if(manaAmount){
          this.decentralandManaService.decreaseAllowanceBidService(manaAmount.toString() + '').then(transactionHash =>{
            if(transactionHash){
              this.decreaseBidAllowanceForm.reset()
              this.decentralandManaService.getBidAllowance(this.connectedWallet!).then(bidAllowance => {
                this.bidAllowance = bidAllowance
              })
            }
          })
        }
        else{
          this.alertService.addAlert("Enter Mana Amount", "danger")
        }
      })
    }else{
      if(manaAmount){
        this.decentralandManaService.decreaseAllowanceBidService(manaAmount.toString() + '').then(transactionHash =>{
          if(transactionHash){
            this.decreaseBidAllowanceForm.reset()
            this.decentralandManaService.getBidAllowance(this.connectedWallet!).then(bidAllowance => {
              this.bidAllowance = bidAllowance
            })
          }
        })
      }
      else{
        this.alertService.addAlert("Enter Mana Amount", "danger")
      }
    }
  }

  increaseFundingAllowance(){
    const manaAmount: number = this.increaseFundingAllowanceForm.get('manaAmount')?.value
    if(!this.connectWalletService.isConnected()){
      this.connectWalletService.connectAccount().then(() => {
        this.connectedWallet = this.connectWalletService.getConnectedAccount()
        if(this.connectedWallet === undefined){
          this.alertService.addAlert("Connect cryto wallet", "danger")
          return
        }
        if(manaAmount){
          this.decentralandManaService.increaseAllowanceChannelService(manaAmount.toString() + '').then(transactionHash =>{
            if(transactionHash){
              this.increaseFundingAllowanceForm.reset()
              this.decentralandManaService.getChannelAllowance(this.connectedWallet!).then(fundingAllowance => {
                this.fundingAllowance = fundingAllowance
              })
            }
          })
        }
        else{
          this.alertService.addAlert("Enter Mana Amount", "danger")
        }
      })
    }else{
      if(manaAmount){
        this.decentralandManaService.increaseAllowanceChannelService(manaAmount.toString() + '').then(transactionHash =>{
          if(transactionHash){
            this.increaseFundingAllowanceForm.reset()
            this.decentralandManaService.getChannelAllowance(this.connectedWallet!).then(fundingAllowance => {
              this.fundingAllowance = fundingAllowance
            })
          }
        })
      }
      else{
        this.alertService.addAlert("Enter Mana Amount", "danger")
      }
    }
  }

  decreaseFundingAllowance(){
    const manaAmount: number = this.decreaseFundingAllowanceForm.get('manaAmount')?.value
    if(!this.connectWalletService.isConnected()){
      this.connectWalletService.connectAccount().then(() => {
        this.connectedWallet = this.connectWalletService.getConnectedAccount()
        if(this.connectedWallet === undefined){
          this.alertService.addAlert("Connect cryto wallet", "danger")
          return
        }
        if(manaAmount){
          this.decentralandManaService.decreaseAllowanceChannelService(manaAmount.toString() + '').then(transactionHash =>{
            if(transactionHash){
              this.decreaseFundingAllowanceForm.reset()
              this.decentralandManaService.getChannelAllowance(this.connectedWallet!).then(fundingAllowance => {
                this.fundingAllowance = fundingAllowance
              })
            }
          })
        }
        else{
          this.alertService.addAlert("Enter Mana Amount", "danger")
        }
      })
    }else{
      if(manaAmount){
        this.decentralandManaService.decreaseAllowanceChannelService(manaAmount.toString() + '').then(transactionHash =>{
          if(transactionHash){
            this.decreaseFundingAllowanceForm.reset()
            this.decentralandManaService.getChannelAllowance(this.connectedWallet!).then(fundingAllowance => {
              this.fundingAllowance = fundingAllowance
            })
          }
        })
      }
      else{
        this.alertService.addAlert("Enter Mana Amount", "danger")
      }
    }
  }

}
