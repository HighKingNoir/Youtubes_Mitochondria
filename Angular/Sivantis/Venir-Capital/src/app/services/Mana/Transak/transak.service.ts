import { Injectable } from '@angular/core';
import transakSDK, { Settings } from '@transak/transak-sdk';
import { environment } from 'src/Environment/environment';
import { ConnectWalletService } from '../ConnectPersonalWallet/connect-wallet.service';

import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { TransakPopupComponent } from 'src/app/components/Accessories/Popups/transak-popup/transak-popup.component';

@Injectable({
  providedIn: 'root'
})
export class TransakService {

  private environmentStage: "STAGING" | "PRODUCTION";

  constructor(
    private connectWalletService:ConnectWalletService,
    private modalService: NgbModal,
  ) {
    if(environment.production){
      this.environmentStage = 'PRODUCTION'
    } else{
      this.environmentStage = 'STAGING'
    }
   }

  openTransak(){
    this.modalService.open(TransakPopupComponent, { size: 'md', scrollable: true, centered: true , animation: false, })
    // window.open(`https://global.transak.com/?defaultNetwork=polygon&defaultCryptoCurrency=MANA&apiKey=${environment.Transak_API}`, '_blank');
    // const settings:Settings = {
    //   apiKey: environment.Transak_API,  // Your API Key
    //   environment: this.environmentStage, // STAGING/PRODUCTION
    //   defaultCryptoCurrency: 'MANA',
    //   hostURL: window.location.origin,
    //   widgetHeight: "600px",
    //   widgetWidth: "500px",
    //   network: 'polygon',
    //   walletAddress: this.connectWalletService.getConnectedAccount(),
    // }

    // const transak = new transakSDK(settings);

    // transak.init();

    // // To get all the events
    // transak.on(transak.ALL_EVENTS, (data: any) => {
    //     if(data.eventName === "TRANSAK_WIDGET_CLOSE"){
    //       document.body.style.overflow = 'auto';
    //     }
    // });


    // // This will trigger when the user marks payment is made.
    // transak.on('TRANSAK_ORDER_SUCCESSFUL', (orderData: any) => {
    //     transak.close();
    // });
  }
}
