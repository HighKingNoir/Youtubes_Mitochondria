import { Component, HostListener, OnInit } from '@angular/core';
import { Channels } from 'src/app/models/Channels/Channels';
import { CreatedContentDetails } from 'src/app/models/Content/CreatedContentDetails';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ChannelService } from 'src/app/services/Channel/channel.service';
import { ContentService } from 'src/app/services/Content/Content/content.service';
import { UserService } from 'src/app/services/User/user-service';

@Component({
  selector: 'app-pay-later',
  templateUrl: './pay-later.component.html',
  styleUrls: ['./pay-later.component.css']
})
export class PayLaterComponent implements OnInit{
  private isScrollHandlerActive = false;
  Content: CreatedContentDetails[] = []
  private lastVideo = false
  private start = 0;
  private channel?: Channels

  ngOnInit(): void {
    const JWT = localStorage.getItem('token') || ''
    this.contentService.getAllPayLaterContent(JWT, this.start).subscribe({
      next: (data:any) => {
        this.Content.push(...data);
        if (data.length == 50) {
          this.start += data.length;
          this.isScrollHandlerActive = true
        }
        else{
          this.lastVideo = true
        }
      },
      error: () => {},
      complete: () => {}
    });
  }

  

  constructor(
    private contentService: ContentService,
    private channelService:ChannelService,
    public alertService:AlertService,
    private userService:UserService,
    private authService: AuthenticationService
  ){
    
    this.channelService.channel$.subscribe(channel => {
      this.channel = channel
    })

  }



  hasChannel(){
    if(this.channel && this.channel.channelStatus === "Approved"){
      return true
    }   
    return false;
  }
  

  getCost(type: string):string {
    const channelAWV = this.channelService.getChannelAWV();
    if(channelAWV == 0){
      return "Channel Needed";
    }
    switch (type) {
      case 'Short Film':
        return (this.contentService.shortFilmPrice * channelAWV).toFixed(2);
      case 'Sports':
        return (this.contentService.sportsPrice * channelAWV).toFixed(2);
      case 'Movies':
        return (this.contentService.moviePrice * channelAWV).toFixed(2);
      default:
        return "Channel Needed";
    }
  }

  watchTrailer(trailerId: string){
    window.open(`https://www.youtube.com/watch?v=${trailerId}`, '_blank');
  }

  removePayLater(contentId: string){
    const JWT = localStorage.getItem('token') || '{}'
    this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
      this.userService.removeFromPayLater(contentId).subscribe({
        next: () => {},
        error: (error) => {
          this.alertService.addAlert(error, 'danger')
        },
        complete: () => {
          const indexToDelete = this.Content.findIndex(
            (v) => v.contentId === contentId
          );
          if (indexToDelete !== -1) {
            this.Content.splice(indexToDelete, 1);
            const indexToRemove = this.userService.getUserPayLater().indexOf(contentId);
            if (indexToRemove !== -1) {
              this.userService.getUserPayLater().splice(indexToRemove, 1);
            }  
          }  
          this.alertService.addAlert("Removed From Pay Later", 'success')
        }
      })
    }) 
  }



  
  isSingleBuyer(contentType:string): boolean{
    if(contentType == "Invention" || contentType == "Innovation"){
      return true;
    }
    return false
  }

  navigateToContent(contentType:string): string{
    if(contentType == "Invention" || contentType == "Innovation"){
      return "Auction";
    }
    return "Buy"
  }

  getNextVideo(){
    
  }

  @HostListener('window:scroll', ['$event'])
  onScroll(event: any) {
    if(this.isScrollHandlerActive){
      const windowHeight = 'innerHeight' in window ? window.innerHeight : document.documentElement.offsetHeight;
      const body = document.body, html = document.documentElement;
      const docHeight = Math.max(body.scrollHeight, body.offsetHeight, html.clientHeight, html.scrollHeight, html.offsetHeight);
      const windowBottom = window.scrollY + windowHeight; // Calculate window bottom correctly

      if (windowBottom >= docHeight - 200) {
        this.isScrollHandlerActive = false;
        if(this.lastVideo == false){
          const JWT = localStorage.getItem('token') || ''
          this.contentService.getAllPayLaterContent(JWT, this.start).subscribe({
            next: (data:any) => {
              this.Content.push(...data);
              if (data.length == 50) {
                this.start += data.length;
              }
              else{
                this.lastVideo = true
              }
            },
            error: () => {},
            complete: () => {}
          });
        }
      }
    }
    
  }

}
