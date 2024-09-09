import { Component, OnDestroy, OnInit } from '@angular/core';
import { NgbModal} from '@ng-bootstrap/ng-bootstrap';
import { UserService } from 'src/app/services/User/user-service';
import { GoogleAPIService, UserInfo } from 'src/app/services/GoogleAPI/google-api.service';
import { ImportYoutubeVideoComponent } from '../../Popups/CreateVideo/import-youtube-video/import-youtube-video.component';
import { ConnectPersonalWalletComponent } from '../../Popups/PersonalWallet/connect-personal-wallet/connect-personal-wallet.component';
import { ConnectWalletService } from 'src/app/services/Mana/ConnectPersonalWallet/connect-wallet.service';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { WarchestWithdrawComponent } from '../../Popups/warchest-withdraw/warchest-withdraw.component';
import { RankComponent } from '../../Popups/rank/rank.component';
import { ActivatedRoute } from '@angular/router';
import { LimitedUsePolicyComponent } from '../../Popups/limited-use-policy/limited-use-policy.component';


@Component({
  selector: 'app-channel-sidebar',
  templateUrl: './channel-sidebar.component.html',
  styleUrls: ['./channel-sidebar.component.css']
})
export class ChannelSidebarComponent {
  YoutubeLogo = "assets/YoutubeLogo.png"
year:number = new Date().getFullYear();
UserInfo?: UserInfo

  
  constructor(
    private modalService: NgbModal, 
    private userService: UserService,
    private googleAPIService: GoogleAPIService, 
    private connectWalletService: ConnectWalletService,
    public alertService:AlertService,
    private route: ActivatedRoute,
  ){
    const fragment = this.route.snapshot.fragment;
    if(fragment){
      this.googleAPIService.loadYoutubeAccount()
    }
    else if(this.isLoggedIn()){
      this.googleAPIService.loadYoutubeAccount()
    }
    this.googleAPIService.userProfileSubject.subscribe(user => {
      if(user){
        this.UserInfo = user;
      }
    }) 
  }

  isLoggedIn():boolean {
    return this.googleAPIService.isLoggedIn()
  }

  /*
  Opens Stage 1 of posting new content
    On success, Popup1component will open
    On failure, N/A
  */
  openCreate() {
    if(this.userService.getPersonalWallet() === null){
      this.modalService.open(ConnectPersonalWalletComponent, { size: 'md', scrollable: true, centered: true , animation: false, }).result.then((result) => {
        if(result === "connecting Wallet"){
          this.connectWalletService.connectAccount().then(() => {
            this.modalService.open(ConnectPersonalWalletComponent, { size: 'md', scrollable: true, centered: true , animation: false}).result.then(result => {
              if(result === "personal wallet connected"){
                this.alertService.addAlert("Wallet Successfully Set", 'success')
                this.modalService.open(ImportYoutubeVideoComponent, { size: 'lg', scrollable: true, centered: true , keyboard: false, backdrop: 'static', animation: false, });
              }
              else {
                this.alertService.addAlert(result, "danger")
              }
            })
          });
        } 
        else{
          if(result === "personal wallet connected"){
            this.alertService.addAlert("Wallet Successfully Set", 'success')
            this.modalService.open(ImportYoutubeVideoComponent, { size: 'lg', scrollable: true, centered: true , keyboard: false, backdrop: 'static', animation: false, });
          }
          else {
            this.alertService.addAlert(result, "danger")
          }
        }
      })
    }else{
      this.modalService.open(ImportYoutubeVideoComponent, { size: 'lg', scrollable: true, centered: true , keyboard: false, backdrop: 'static', animation: false, });
    }
	}



  getYoutubeAccount(){
    this.googleAPIService.getYoutubeAccount()
  }

  openContentTeir(){
    this.modalService.open(RankComponent, {size: 'lg', scrollable: true,centered: true , animation: false})
  }

  openWarChestModal(){
    this.modalService.open(WarchestWithdrawComponent, {size: 'sm', scrollable: true,centered: true , animation: false}).result.then(result => {
      if(result === "connecting Wallet"){
        this.connectWalletService.connectAccount().then(() => {
          this.modalService.open(WarchestWithdrawComponent, { size: 'sm', scrollable: true, centered: true , animation: false, })
        });
      } 
    })
  }

  openLimitedUsePolicy(){
    this.modalService.open(LimitedUsePolicyComponent, { size: 'sm', scrollable: true, centered: true , animation: false, })
  }


}


