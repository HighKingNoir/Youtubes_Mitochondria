import { KeyValue } from '@angular/common';
import { ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ChannelService } from 'src/app/services/Channel/channel.service';
import { ChannelServiceContract } from 'src/app/services/Contracts/Channel/channel-service-contract.service';
import { DecentralandManaService } from 'src/app/services/Contracts/DecentralandManaService/decentraland-mana.service';
import { ConnectWalletService } from 'src/app/services/Mana/ConnectPersonalWallet/connect-wallet.service';
import { TransakService } from 'src/app/services/Mana/Transak/transak.service';
import { FundChannelPayload, MessagesService } from 'src/app/services/MessageService/messages.service';
import { UserService } from 'src/app/services/User/user-service';
import { TransferManaComponent } from '../transfer-mana/transfer-mana.component';
import { watchAccount } from '@wagmi/core';
import { config } from 'src/app/services/Contracts/config';

@Component({
  selector: 'app-fund-channel',
  templateUrl: './fund-channel.component.html',
  styleUrls: ['./fund-channel.component.css']
})
export class FundChannelComponent implements OnInit{
  loading = false;
  connectedWallet?:string;
  accountbalance = 0
  isClicked = false;
  @Input() channelName = ''
  

  manaForm: FormGroup 
  amountOfMana:FormControl = new FormControl('',Validators.required)

  constructor(private channelServiceContract:ChannelServiceContract, 
    private messsageService: MessagesService,
    private userService: UserService, 
    public activeModal:NgbActiveModal, 
    private route: ActivatedRoute,
    private authService: AuthenticationService, 
    private connectWalletService: ConnectWalletService,
    private transakService:TransakService,
    private decentralandManaService: DecentralandManaService,
    private modalService: NgbModal,
  ){
    this.manaForm = new FormGroup({
      amountOfMana : this.amountOfMana,
    })
      this.connectedWallet = connectWalletService.getConnectedAccount()
      if(this.connectedWallet){
        this.getAccountbalance()
      }
      watchAccount(config, {
        onChange: (account) => { 
          const address = account.address;
          if (address) {
            this.connectedWallet = address
            this.getAccountbalance()
          }else{
            this.connectedWallet = undefined
          }
        },
      });
  }


  

  toggleClick() {
    if(!this.connectWalletService.isConnected()){
      this.connectedWallet = undefined
      return
    }
    this.decentralandManaService.getChannelAllowance(this.connectedWallet!).then(userFundingAllowance => {
      if(userFundingAllowance == 0){
        const modelRef = this.modalService.open(TransferManaComponent, {size: 'lg', scrollable: true, centered: true , animation: false})
        modelRef.componentInstance.approveContract = true;
      }
    })
    this.isClicked = true;
  }





  ngOnInit(): void {
    
  }

  fundChannel(){
    if(!this.connectWalletService.isConnected()){
      this.connectedWallet = undefined
      return
    }
    this.decentralandManaService.getChannelAllowance(this.connectedWallet!).then(userFundingAllowance => {
      const manaAmount =  Number(this.manaForm.get('amountOfMana')?.value)
      if(userFundingAllowance < manaAmount){
        const modelRef = this.modalService.open(TransferManaComponent, {size: 'lg', scrollable: true, centered: true , animation: false})
        modelRef.componentInstance.allowance = userFundingAllowance;
        modelRef.componentInstance.mana = Number(this.manaForm.get('amountOfMana')?.value)
      }
      else{
          this.channelServiceContract.callfundChannel(this.channelName, this.manaForm.get('amountOfMana')?.value).then((transactionHash) => {
          if(transactionHash){
            this.loading = true
            const JWT = window.localStorage.getItem('token')
            if(JWT){
              this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
                const fundChannelPayload: FundChannelPayload = {
                  channelName: this.channelName,
                  manaAmount: this.amountOfMana.value,
                  transactionHash: transactionHash,
                }
                this.messsageService.fundChannelMessage(fundChannelPayload).subscribe({
                  next: () => {},
                  error: () => {
                    this.loading = false
                    this.activeModal.close('Funding failed')
                  },
                  complete: () => {
                    this.loading = false
                    this.activeModal.close('Funding successful')
                  }
                })
              })
            }
            else{
              this.loading = false
              this.activeModal.close('Funding successful')
            }
          }
        });
      }
    }) 
  }

  getAccountbalance(){
    if(this.connectedWallet){
      this.connectWalletService.accountManabalance(this.connectedWallet).then(balance => {
        this.accountbalance = balance
      })
    } 
  }


  getMoreMana(){
    this.activeModal.close("more mana")
    this.transakService.openTransak()
  }



}
