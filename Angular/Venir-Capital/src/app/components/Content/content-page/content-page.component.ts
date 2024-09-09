import { Component, HostListener, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { CreatedContentDetails } from 'src/app/models/Content/CreatedContentDetails';
import { ContentService } from 'src/app/services/Content/Content/content.service';
import { GoogleAPIService } from 'src/app/services/GoogleAPI/google-api.service';
import { UserService } from 'src/app/services/User/user-service';
import { ImportYoutubeVideoComponent } from '../../Accessories/Popups/CreateVideo/import-youtube-video/import-youtube-video.component';
import { ConnectPersonalWalletComponent } from '../../Accessories/Popups/PersonalWallet/connect-personal-wallet/connect-personal-wallet.component';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { ConnectWalletService } from 'src/app/services/Mana/ConnectPersonalWallet/connect-wallet.service';
import { EditVideoComponent } from '../../Accessories/Popups/edit-video/edit-video.component';
import { CompleteVideoRequestPayload } from 'src/app/models/Content/CompleteVideoRequestPayload';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { HttpErrorResponse } from '@angular/common/http';
import { UploadVideoInfoService } from 'src/app/services/Content/VideoInfo/upload-video-info.service';
import { YoutubeAPIService, YoutubeVideoResponse } from 'src/app/services/YouTubeAPI/youtube-api.service';
import { EditRequestPayload } from 'src/app/models/Content/EditRequestPayload';
import { ConfirmationComponent, VideoConfirmation } from '../../Accessories/Popups/confirmation/confirmation.component';
import { ReactivateContentRequestPayload } from 'src/app/models/Content/ReactivateContentRequestPayload';
import { SelectMainVideoComponent } from '../../Accessories/Popups/CreateVideo/SelectVideo/select-main-video/select-main-video.component';


@Component({
  selector: 'app-content-page',
  templateUrl: './content-page.component.html',
  styleUrls: ['./content-page.component.css']
})
export class ContentPageComponent implements OnInit{
  UserCreatedVideos: CreatedContentDetails[] = []
  showAlert = false;
  private lastVideo = false
  private activeDate?: string;
  private isScrollHandlerActive = false;

  YoutubeLogo = "assets/YoutubeLogo.png"
  
  constructor( 
    private readonly googleAPIService: GoogleAPIService,
    private userService: UserService, 
    private modalService: NgbModal, 
    private contentService: ContentService,
    private connectWalletService: ConnectWalletService,
    public alertService:AlertService,
    private authService: AuthenticationService,
    private videoInfo: UploadVideoInfoService,
    private youtubeAPI: YoutubeAPIService,
    ){
      this.contentService.createdContentInfo$.subscribe(content => {
        this.UserCreatedVideos = content;
      })
   
  }


  ngOnInit(): void {
    const JWT = localStorage.getItem('token') || ''
    this.contentService.fetchAllUserCreatedContent(JWT, this.activeDate).subscribe({
      next: (data:any) => {
        this.contentService.initualizeAllUserCreatedContent(data)
        if (data.length == 50) {
          this.activeDate = data[data.length - 1].activeDate;
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

  getNextUserCreatedContent(){
    const JWT = window.localStorage.getItem('token') || ''
    this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
      this.contentService.fetchAllUserCreatedContent(JWTResult, this.activeDate).subscribe({
        next: (data:any) => {
          this.contentService.updateAllUserCreatedContent(data)
          if (data.length == 50) {
            this.activeDate = data[data.length - 1].activeDate;
          }
          else{
            this.lastVideo = true
          }
        },
        error: () => {},
        complete: () => {}
      });
    })
  }


  isLoggedIn():boolean {
    return this.googleAPIService.isLoggedIn()
  }

  LinkYoutube(){
    this.googleAPIService.getYoutubeAccount();
  }

  editVideo(video: CreatedContentDetails) {
    const modalRef = this.modalService.open(EditVideoComponent, { size: 'lg', scrollable: true, centered: true , keyboard: false, backdrop: 'static', animation: false, });
    modalRef.componentInstance.video = video
    modalRef.result.then(result => {
      if(result === 'Content Successfully Edited'){
        const editedVideo:EditRequestPayload = this.videoInfo.getEditedVideo()
        const indexToEdit = this.UserCreatedVideos.findIndex(
          (v) => v.contentId === editedVideo.contentID
        );
        if (indexToEdit !== -1) {
          this.UserCreatedVideos[indexToEdit].contentName = editedVideo.contentName
          this.UserCreatedVideos[indexToEdit].description = editedVideo.description
          this.UserCreatedVideos[indexToEdit].youtubeTrailerVideoID = editedVideo.youtubeTrailerVideoID
        }
      }
    })
  }

  completeVideo(video: CreatedContentDetails){
    if(this.googleAPIService.isLoggedIn()){
      this.youtubeAPI.fetchLatestYouTubeVideos().subscribe((fetchedVideos: YoutubeVideoResponse)  => {
        this.videoInfo.setYoutubeAllVideoInfo(fetchedVideos.items)
        const modalRef = this.modalService.open(SelectMainVideoComponent, { size: 'lg', scrollable: true, centered: true , keyboard: false, backdrop: 'static', animation: false, });
        modalRef.componentInstance.nextPageToken = fetchedVideos.nextPageToken
        modalRef.componentInstance.toComplete = true
        modalRef.result.then(result => {
          if(result === 'continue'){
            const mainVideo = this.videoInfo.getSelectedMainYoutubeVideoInfo()
            if(mainVideo.status.privacyStatus !== "private"){
              this.alertService.addAlert("The Selected Video doesn't have it's privacy status set to 'private'.", 'danger')
            }
            else if(this.lessThan10Minutes(mainVideo.contentDetails.duration)){
              this.alertService.addAlert("The Selected Video is less than 10 minutes long.", 'danger')
            }
            else{
              const JWT = window.localStorage.getItem('token') || ''
              this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
                const completeVideoRequestPayload:CompleteVideoRequestPayload = {
                  
                  contentID: video.contentId,
                  privacyStatus: mainVideo.status.privacyStatus,
                  duration: mainVideo.contentDetails.duration,
                  youtubeMainVideoID: mainVideo.id
                }
                this.contentService.completeVideo(completeVideoRequestPayload).subscribe({
                  next: () => {
                    const indexToReactivate = this.UserCreatedVideos.findIndex(
                      (v) => v.contentId === video.contentId
                    );
                    if (indexToReactivate !== -1) {
                      this.UserCreatedVideos[indexToReactivate].isComplete = true
                    }
                  },
                  error: (error) => {
                    this.alertService.addAlert(error, 'danger')
                  },
                  complete: () => {
                    this.alertService.addAlert("Content Completed", 'success')
                  }
                })
              })
            }
          }
        })
      });
    }
    else {
      this.alertService.addAlert("Sign into your Youtube account to view your videos", "danger")
    }
  }

  reactivateVideo(video: CreatedContentDetails){
    if(video.contentEnum != "Inactive"){
      this.alertService.addAlert("To Reactive, this content must be Inactive", 'danger')
    }
    else if(video.contentType == "Sports"){
      this.alertService.addAlert("Cannot reactive a video set as Sports", 'danger')
    }
    else{
      const videoConfirmation:VideoConfirmation = {
        contentName: video.contentName,
        description: video.description,
        thumbnail: video.thumbnail,
      }
      const modalRef = this.modalService.open(ConfirmationComponent, {size: 'md', scrollable: true,centered: true , animation: false})
      modalRef.componentInstance.message = "Are you sure you want to Reactivate this video?"
      modalRef.componentInstance.videoConfirmation = videoConfirmation
      modalRef.componentInstance.reactivateContent = true
      modalRef.result.then(result => {
        if(result === 'continue'){
          const JWT = window.localStorage.getItem('token') || ''
          this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
            const newReleaseDate = this.videoInfo.getNewReleaseDate()
            const reactivateContentRequestPayload:ReactivateContentRequestPayload = {
              contentID: video.contentId,
              
              releaseDate: newReleaseDate,
            }
            this.contentService.reactivateVideo(reactivateContentRequestPayload).subscribe({
              next: () => {
                const indexToReactivate = this.UserCreatedVideos.findIndex(
                  (v) => v.contentId === video.contentId
                );
                if (indexToReactivate !== -1) {
                  this.UserCreatedVideos[indexToReactivate].contentEnum = "Active"
                  this.UserCreatedVideos[indexToReactivate].releaseDate = [newReleaseDate.year, newReleaseDate.month, newReleaseDate.day]
                  this.UserCreatedVideos[indexToReactivate].hype = 0
                }
              },
              error: (error) => {
                this.alertService.addAlert(error, 'danger')
              },
              complete: () => {
                this.alertService.addAlert("Content Reactivated", 'success')
              }
            })
          })
        }
      })
    } 
  }

  viewPage(contentID:string, contentType: string){
    if(this.isAuction(contentType)){
      window.open(`/Auction/${contentID}`, '_blank');
    }
    else{
      window.open(`/Buy/${contentID}`, '_blank');
    }
  }

  deleteVideo(video: CreatedContentDetails){
    if(video.contentEnum != "Inactive"){
      this.alertService.addAlert("Content must be Inactive to delete", 'danger')
    }
    else{
      const videoConfirmation:VideoConfirmation = {
        contentName: video.contentName,
        description: video.description,
        thumbnail: video.thumbnail,
      }
      const modalRef = this.modalService.open(ConfirmationComponent, {size: 'md', scrollable: true,centered: true , animation: false})
      modalRef.componentInstance.message = "Are you sure you want to Delete this video?"
      modalRef.componentInstance.videoConfirmation = videoConfirmation
      modalRef.result.then(result => {
        if(result === 'continue'){
          const JWT = window.localStorage.getItem('token') || ''
          this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
            this.contentService.deleteCreatedContent(JWTResult, video.contentId).subscribe({
              next: () => {
                const indexToDelete = this.UserCreatedVideos.findIndex(
                  (v) => v.contentId === video.contentId
                );
                if (indexToDelete !== -1) {
                  this.UserCreatedVideos.splice(indexToDelete, 1);
                }
              },
              error: (error) => {
                this.alertService.addAlert(error, 'danger')
              },
              complete: () => {
                this.alertService.addAlert("Content Deleted", 'success')
              }
            })
          })
        }
      })
    }
  }

  isAuction(contentType:string):boolean{
    return contentType === "Invention" || contentType === "Innovation";
  }

getStatusClass(status: string, isComplete: boolean) {
  switch (status) {
    case 'Active':
      if(isComplete){
        return 'status-green';
      }
      return 'status-yellow';
    case 'InProgress':
      return 'status-green'
    default:
      return 'status-grey';
  }
}

getStatus(status: string, isComplete: boolean) {
  switch (status) {
    case 'Active':
      if(isComplete){
        return 'Active';
      }
      return 'In Development';
    case 'InProgress':
      return 'Active'
    default:
      return 'Inactive';
  }
}

  importYoutubeVideo(){
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

  lessThan10Minutes(duration: string):boolean{
    const regex = /PT(\d+H)?(\d+M)?(\d+S)?/;
    const matches = duration.match(regex);

    if (!matches) {
      return true; // Invalid duration format
    }

    let hours = 0;
    let minutes = 0;
    let seconds = 0;

    if (matches[1]) {
      hours = parseInt(matches[1].replace('H', ''), 10);
    }

    if (matches[2]) {
      minutes = parseInt(matches[2].replace('M', ''), 10);
    }

    if (matches[3]) {
      seconds = parseInt(matches[3].replace('S', ''), 10);
    }

    return (hours * 60 + minutes + seconds / 60) < 10; 
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
          this.getNextUserCreatedContent()
        }
      }
    }
  }

}
