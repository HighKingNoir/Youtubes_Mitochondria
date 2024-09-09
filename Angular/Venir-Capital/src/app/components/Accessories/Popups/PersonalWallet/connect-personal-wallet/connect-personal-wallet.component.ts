import { KeyValue } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ConnectWalletService } from 'src/app/services/Mana/ConnectPersonalWallet/connect-wallet.service';
import { PersonalWalletPayload, UserService } from 'src/app/services/User/user-service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { HttpErrorResponse } from '@angular/common/http';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationResponse } from 'src/app/models/AuthenticationResponse/AuthenticationResponse';
import { TwoFactorAuthenticationComponent } from '../../two-factor-authentication/two-factor-authentication.component';
import { watchAccount } from '@wagmi/core';
import { config } from 'src/app/services/Contracts/config';

@Component({
  selector: 'app-connect-personal-wallet',
  templateUrl: './connect-personal-wallet.component.html',
  styleUrls: ['./connect-personal-wallet.component.css']
})
export class ConnectPersonalWalletComponent implements OnInit{

  JWT: string = window.localStorage.getItem('token') || '{}'
  connectedWallet?:string;
  nextWalletChange?: string

  constructor(
    public activeModal: NgbActiveModal,
    private connectWalletService:ConnectWalletService,
    private userService: UserService,
    private authSerice: AuthenticationService,
    public alertService: AlertService,
    private modalService: NgbModal,
    
  ){
    this.connectedWallet = this.connectWalletService.getConnectedAccount()
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
    const JWT = localStorage.getItem('token') || ''
    this.userService.getNextWalletChangeTime(JWT).subscribe(data =>{
      if(data != null){
        this.nextWalletChange = data
      }
    })
  }


  back(){
    this.activeModal.close('Close click');
  }

  connect(){
    const JWT = localStorage.getItem('token') || ''
    this.authSerice.checkJWTExpiration(JWT).then((JWTResult) => {
      if(this.connectedWallet){
        const personalWalletPayload: PersonalWalletPayload = {
          personalWallet: this.connectedWallet,
          code: ''
        }
        this.userService.setUserPersonalWallet(personalWalletPayload).subscribe({
        next: (data:AuthenticationResponse) => {
          if(data.mfaEnabled){
            const modalRef = this.modalService.open(TwoFactorAuthenticationComponent, { size: 'md', scrollable: true, centered: true , animation: false, })
            modalRef.componentInstance.personalWalletPayload = personalWalletPayload;
            modalRef.result.then(result => {
              if(result === "success"){
                this.userService.setPersonalWallet(personalWalletPayload.personalWallet)
                this.activeModal.close('personal wallet connected')
              }
            })
          }else {
            this.userService.setPersonalWallet(personalWalletPayload.personalWallet)
            this.activeModal.close('personal wallet connected')
          }
        },
        error: (error: HttpErrorResponse) => {
          this.alertService.addAlert(error.message, "danger")
          this.activeModal.close(error.message)
        },
        complete: () => {
        }
        })
      }
    })
  }




  
}
