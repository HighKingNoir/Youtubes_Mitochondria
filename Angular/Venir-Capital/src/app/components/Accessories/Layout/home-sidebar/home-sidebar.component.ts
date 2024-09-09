import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { User } from 'src/app/models/Users/User';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ConnectWalletService } from 'src/app/services/Mana/ConnectPersonalWallet/connect-wallet.service';
import { UserService } from 'src/app/services/User/user-service';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Channels, StreamerInfo } from 'src/app/models/Channels/Channels';
import { ChannelService } from 'src/app/services/Channel/channel.service';
import { IsActiveMatchOptions, Router } from '@angular/router';
import { TransakService } from 'src/app/services/Mana/Transak/transak.service';
import { ReportVideoComponent } from '../../Popups/report-video/report-video.component';
import { TransferManaComponent } from '../../Popups/transfer-mana/transfer-mana.component';
import { LimitedUsePolicyComponent } from '../../Popups/limited-use-policy/limited-use-policy.component';
import { AlertService } from 'src/app/services/Alerts/alert.service';

@Component({
  selector: 'app-home-sidebar',
  templateUrl: './home-sidebar.component.html',
  styleUrls: ['./home-sidebar.component.css'],
  // changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeSidebarComponent implements OnInit{

  userChannels: Channels[] = [] 
  channelsSubscribedTo: Channels[] = [] 
  temporarytoken: string = window.localStorage.getItem("token") || "{}"
  maxNumberOfChannels: number
  year:number = new Date().getFullYear();
  isLoggedIn: boolean;

  constructor(
    private modalService: NgbModal,
    private channelService:ChannelService,
    private transakService:TransakService,
    private alertService:AlertService,
    private router: Router,
    private authService:AuthenticationService,
  ){
      this.maxNumberOfChannels = this.channelService.getMaxNumberOfChannels()

      this.channelService.channel$.subscribe(channel => {
        this.userChannels[0] = channel;
      })
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
              const currentChannel = userChannels[0]
              this.channelService.setChannel(currentChannel)
              this.getHighestAWV(currentChannel.streamerInfo)
            }
          })
          this.channelService.getChannelSubscriptions().subscribe(channels => {
            this.channelsSubscribedTo = channels
          })
        }
        else{
          this.userChannels = [] 
          this.channelsSubscribedTo = [] 
        }
      })
    }

  
  ngOnInit(): void {
    if(this.isLoggedIn){
      this.channelService.getAllUserChannels().subscribe(userChannels =>{
        if(userChannels.length > 0){
          const currentChannel = userChannels[0]
          this.channelService.setChannel(currentChannel)
          this.getHighestAWV(currentChannel.streamerInfo)
        }
      })
      this.channelService.getChannelSubscriptions().subscribe(channels => {
        this.channelsSubscribedTo = channels
      })
    }
    
  }

  isActiveHomeRoute(): boolean {
    const exactMatchOptions: IsActiveMatchOptions = {
      paths: 'exact',
      queryParams: 'exact',
      matrixParams: 'exact',
      fragment: 'ignored'
    };

    const matchOptions: IsActiveMatchOptions = {
      paths: 'exact',
      queryParams: 'ignored',
      matrixParams: 'ignored',
      fragment: 'ignored'
    };

    return this.router.isActive('', exactMatchOptions) 
    || this.router.isActive('/Inventions', matchOptions)
    || this.router.isActive('/UpcomingReleases', matchOptions)
    || this.router.isActive('/MostHyped', matchOptions)
    || this.router.isActive('/Innovations', matchOptions)
    || this.router.isActive('/ShortFilms', matchOptions)
    || this.router.isActive('/Movies', matchOptions)
    || this.router.isActive('/Sports', matchOptions);
  }

  isActiveChannelRoute(): boolean {
    const exactMatchOptions: IsActiveMatchOptions = {
      paths: 'exact',
      queryParams: 'exact',
      matrixParams: 'exact',
      fragment: 'ignored'
    };

    return this.router.isActive('/Channels', exactMatchOptions) 
    || this.router.isActive('/Subscriptions/Channels', exactMatchOptions);
  }
  

  getHighestAWV(data: StreamerInfo[]): void {
    const highestViewers = data.reduce((max, current) => {
      return current.averageWeeklyViewers > max ? current.averageWeeklyViewers : max;
    }, 0);
    this.channelService.setChannelAWV(highestViewers);
  }

  navigateToCreateChannel(event: Event): void {
    if (!this.isLoggedIn) {
      this.alertService.addAlert('You must be logged in to Create a Channel.', "danger")
      event.preventDefault();
    } else {
      this.router.navigate(['/Create/Channel']);
    }
  }

  addChannel(channel: Channels){
    this.channelsSubscribedTo.push(channel)
  }
  
  removeChannel(channel: Channels){
    const index = this.channelsSubscribedTo.findIndex(
      (subscribedChannel) => subscribedChannel.channelId === channel.channelId
    );

    if (index !== -1) {
      this.channelsSubscribedTo.splice(index, 1);
    }  
  }

  openLimitedUsePolicy(){
    this.modalService.open(LimitedUsePolicyComponent, { size: 'sm', scrollable: true, centered: true , animation: false, })
  }

    getMoreMana(){
      this.transakService.openTransak()
    }
    
}
