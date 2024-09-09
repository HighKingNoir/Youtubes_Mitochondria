import { Component, Input } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { DecentralandManaService } from 'src/app/services/Contracts/DecentralandManaService/decentraland-mana.service';
import { ConnectWalletService } from 'src/app/services/Mana/ConnectPersonalWallet/connect-wallet.service';

@Component({
  selector: 'app-transfer-mana',
  templateUrl: './transfer-mana.component.html',
  styleUrls: ['./transfer-mana.component.css']
})
export class TransferManaComponent {

  @Input() approveContract = false;
  @Input() bidContract = false;
  @Input() allowance = 0;
  @Input() mana = 0;

  constructor(
    private decentralandManaService: DecentralandManaService,
    private activeModal: NgbActiveModal, 
    private alertService: AlertService
  ){
    
  }

  approveBidService(){
    let approvalAmount;
    if(this.mana > 10000){
      approvalAmount = (this.mana * 10).toString()
    }
    else{
      approvalAmount = '10000'
    }
    this.decentralandManaService.approveBidService(approvalAmount).then((transactionHash) => {
      if(transactionHash){
        this.activeModal.close("Approved")
      }
      else{
        this.alertService.addAlert("Approval Failed", "danger")
      }
    }).catch(() => {
    })
  }

  approveChannelService(){
    let approvalAmount;
    if(this.mana > 10000){
      approvalAmount = (this.mana * 10).toString()
    }
    else{
      approvalAmount = '10000'
    }
    this.decentralandManaService.approveChannelService(approvalAmount).then((transactionHash) => {
      if(transactionHash){
        this.activeModal.close("Approved")
      }
      else{
        this.alertService.addAlert("Approval Failed", "danger")
      }
    }).catch(() => {
    })
  }

  increaseBidAllowance(){
    let allowanceIncrease;
    if(this.allowance != 0 && this.mana != 0){
      const newAllowance = this.mana * 5
      if(newAllowance < 10000){
        allowanceIncrease = '10000'
      }
      else{
        allowanceIncrease = newAllowance.toString()
      }
    }
    else{
      allowanceIncrease = "10000"
    }
    this.decentralandManaService.increaseAllowanceBidService(allowanceIncrease).then((transactionHash) => {
      if(transactionHash){
        this.activeModal.close("Increased")
      }
      else{
        this.alertService.addAlert("Increase Failed", "danger")

      }
    })
  }

  increaseChannelFundAllowance(){
    let allowanceIncrease;
    if(this.allowance != 0 && this.mana != 0){
      const newAllowance = this.mana * 5
      if(newAllowance < 10000){
        allowanceIncrease = '10000'
      }
      else{
        allowanceIncrease = newAllowance.toString()
      }
      
    }
    else{
      allowanceIncrease = "10000"
    }
    this.decentralandManaService.increaseAllowanceChannelService(allowanceIncrease).then((transactionHash) => {
      if(transactionHash){
        this.activeModal.close("Increased")
      }
      else{
        this.alertService.addAlert("Increase Failed", "danger")
      }
    })
  }

}
