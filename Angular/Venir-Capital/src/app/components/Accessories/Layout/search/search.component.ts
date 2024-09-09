import { Component, HostListener, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Channels } from 'src/app/models/Channels/Channels';
import { CreatedContentDetails } from 'src/app/models/Content/CreatedContentDetails';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ChannelService } from 'src/app/services/Channel/channel.service';
import { ContentService } from 'src/app/services/Content/Content/content.service';
import { UserService } from 'src/app/services/User/user-service';

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.css']
})
export class SearchComponent implements OnInit{
  private channel?: Channels
  private isScrollHandlerActive = false;
  private lastVideo = false
  videos: CreatedContentDetails[] = [];
  channels: Channels[] = []
  searchQuery = '';
  isVideoSearch = false;
  private payLaterList: string[] = []
  private start = 0
  private isLoggedIn: boolean;

  constructor(
    private route: ActivatedRoute,
    private contentService: ContentService,
    private channelService: ChannelService,
    public alertService: AlertService,
    private userService: UserService,
    private authService: AuthenticationService,
  ){
    if(localStorage.getItem('token')){
      this.isLoggedIn = true
    }
    else{
      this.isLoggedIn = false
    }
    this.authService.isLoggedInProfile$.subscribe(isLoggedInStatus => {
      this.isLoggedIn = isLoggedInStatus
      if(!isLoggedInStatus){
        this.payLaterList = []
        this.channel = undefined
      }
    })
  }
  
  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.searchQuery = params['searchQuery'];
      this.isVideoSearch = params['isVideoSearch'] === 'true'; // Convert string to boolean
      if(this.isVideoSearch){
        this.contentService.searchVideos(this.searchQuery, this.start).subscribe((videos:any) => {
          this.videos = []
          this.videos.push(...videos)
          if (videos.length == 50) {
            this.start = 50;
            this.isScrollHandlerActive = true
          }
          else{
            this.lastVideo = true
          }
        })
      }
      else{
        this.channelService.searchChannels(this.searchQuery, this.start).subscribe((channels:any) => {
          this.channels = []
          this.channels.push(...channels)
          if (channels.length == 50) {
            this.start = 50
          }
          else{
            this.lastVideo = true
          }
        })
      }
    });

    this.channelService.channel$.subscribe(channel => {
      this.channel = channel
    })

    if(this.userService.getUserID() === ''){
      this.userService.userInfo$.subscribe(() => {
        this.payLaterList = this.userService.getUserPayLater()
      })
    }
    else{
      this.payLaterList = this.userService.getUserPayLater()
    }
  }


  hasChannel(){
    if(this.channel && this.channel.channelStatus === "Approved"){
      return true
    }   
    return false;
  }

  watchTrailer(trailerId: string){
    window.open(`https://www.youtube.com/watch?v=${trailerId}`, '_blank');
  }

  togglePayLater(contentId: string){
    if(!this.isLoggedIn){
      this.alertService.addAlert('You must be logged in to Add to Pay Later.', "danger")
    }
    else{
      const JWT = localStorage.getItem('token') || '{}';
      this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
        if(this.isOnPayLaterList(contentId)){
          this.userService.removeFromPayLater(contentId).subscribe({
            next: () => {},
            error: (error) => {
              this.alertService.addAlert(error, 'danger')
            },
            complete: () => {
              const indexToDelete = this.payLaterList.indexOf(contentId);
              if (indexToDelete !== -1) {
                this.payLaterList.splice(indexToDelete, 1);
              }  
              this.alertService.addAlert("Removed From Pay Later", 'success')
            }
          })
      } else{
          this.userService.addToPayLater(contentId).subscribe({
            next: () => {},
            error: (error) => {
              this.alertService.addAlert(error, 'danger')
            },
            complete: () => {
              this.alertService.addAlert("Added To Pay Later", 'success')
              this.payLaterList.push(contentId)
            }
          })
        }
      }) 
    }
    
  }

  payLaterText(contentID:string): string{
    if(this.isOnPayLaterList(contentID)){
      return "Remove From "
    }
    return "Add To "
  }

  isOnPayLaterList(contentID:string): boolean{
    return this.payLaterList.includes(contentID)
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

  getNextVideos(){
    const JWT = localStorage.getItem('token') || ''
    this.authService.checkJWTExpiration(JWT).then(() => {
      this.contentService.searchVideos(this.searchQuery, this.start).subscribe({
        next: (data:any) => {
          this.videos.push(...data)
          if (data.length == 50) {
            this.start += 50
          }
          else{
            this.lastVideo = true
          }
          this.isScrollHandlerActive = true;
        },
        error: () => {
          this.isScrollHandlerActive = true;
        },
        complete: () => {}
      });
    })
  }

  getNextChannels(){
    const JWT = localStorage.getItem('token') || ''
    this.authService.checkJWTExpiration(JWT).then(() => {
      this.contentService.searchVideos(this.searchQuery, this.start).subscribe({
        next: (data:any) => {
          this.channels.push(...data)
          if (data.length == 50) {
            this.start += 50
          }
          else{
            this.lastVideo = true
          }
          this.isScrollHandlerActive = true;
        },
        error: () => {
          this.isScrollHandlerActive = true;
        },
        complete: () => {}
      });
    })
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
          if(this.isVideoSearch){
            this.getNextVideos()
          }
          else{
            this.getNextChannels()
          }
          
        }
      }
    }
    
  }
}
