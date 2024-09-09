import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ContentService } from 'src/app/services/Content/Content/content.service';
import { UploadVideoInfoService } from 'src/app/services/Content/VideoInfo/upload-video-info.service';
import { UserService } from 'src/app/services/User/user-service';

import { YoutubeVideoResponse, YoutubeAPIService } from 'src/app/services/YouTubeAPI/youtube-api.service';
import { SelectMainVideoComponent } from '../SelectVideo/select-main-video/select-main-video.component';
import { InDevelopmentVideoComponent } from '../in-development-video/in-development-video.component';
import { GoogleAPIService } from 'src/app/services/GoogleAPI/google-api.service';

@Component({
  selector: 'app-import-youtube-video',
  templateUrl: './import-youtube-video.component.html',
  styleUrls: ['./import-youtube-video.component.css']
})
export class ImportYoutubeVideoComponent {
  uploadedVideos?: any[];
  tokens: number
  SivantisToken = "assets/SivantisToken.png"

  @Input() mistake?: string;
  
  constructor(public activeModal: NgbActiveModal,
    private modalService: NgbModal, 
    private userService: UserService, 
    private videoInfo: UploadVideoInfoService,
    private youtubeAPI: YoutubeAPIService,
    private googleAPIService: GoogleAPIService,
  ){
      this.tokens = userService.getAllowedDevelopingVideos();
  }

  isLoggedIn():boolean {
    return this.googleAPIService.isLoggedIn()
  }

    videoInDevelopment(){
      if(this.isLoggedIn()){
        this.youtubeAPI.fetchLatestYouTubeVideos().subscribe(fetchedVideos => {
          this.videoInfo.setYoutubeAllVideoInfo(fetchedVideos)
          this.activeModal.close('Close click');
          this.modalService.open(InDevelopmentVideoComponent, { size: 'lg', scrollable: true, centered: true , keyboard: false, backdrop: 'static', animation: false, });
        });
      }
      else{
        this.mistake = "You must link your Youtube channel before you can proceed. If linked, Refresh Page."
      }
      
    }

    importYoutubeVideo(){
      if(this.isLoggedIn()){
        this.youtubeAPI.fetchLatestYouTubeVideos().subscribe((fetchedVideos: YoutubeVideoResponse) => {
          this.videoInfo.setYoutubeAllVideoInfo(fetchedVideos.items)
          this.activeModal.close('Close click');
          const modalRef = this.modalService.open(SelectMainVideoComponent, { size: 'lg', scrollable: true, centered: true , keyboard: false, backdrop: 'static', animation: false, });
          modalRef.componentInstance.nextPageToken = fetchedVideos.nextPageToken
        });
      }
      else{
        this.mistake = "You must link your Youtube channel before you can proceed. If linked, Refresh Page."
      }
    }

    
}
