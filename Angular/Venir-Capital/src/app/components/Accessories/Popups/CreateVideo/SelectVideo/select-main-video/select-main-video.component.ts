import { Component, ElementRef, HostListener, Input, OnInit } from '@angular/core';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { UploadVideoInfoService } from 'src/app/services/Content/VideoInfo/upload-video-info.service';
import { YoutubeVideoResponse, YoutubeAPIService } from 'src/app/services/YouTubeAPI/youtube-api.service';
import { ImportYoutubeVideoComponent } from '../../import-youtube-video/import-youtube-video.component';
import { CompleteVideoComponent } from '../../complete-video/complete-video.component';
import { ConfirmationComponent } from '../../../confirmation/confirmation.component';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-select-main-video',
  templateUrl: './select-main-video.component.html',
  styleUrls: ['./select-main-video.component.css']
})
export class SelectMainVideoComponent implements OnInit{
  private isScrollHandlerActive = false;
  hasSelectedVideo = false;
  selectedIndex = -1;
  uploadedVideos: any[] = []
  isChecked = false;

  @Input() toComplete = false
  @Input() nextPageToken?: string

  constructor(
    public activeModal: NgbActiveModal,
    private modalService: NgbModal, 
    private videoInfo: UploadVideoInfoService,
    private youtubeAPI: YoutubeAPIService,
    private elementRef: ElementRef,
    private alertService:AlertService
  ){
    this.uploadedVideos = this.videoInfo.getYoutubeAllVideoInfo().map(item => {
      return {
        videoId: item.id.videoId,
        description: item.snippet.description,
        publishTime: item.snippet.publishTime,
        thumbnails: item.snippet.thumbnails,
        title: item.snippet.title
      };
    });
  }
  ngOnInit(): void {
    this.isScrollHandlerActive = true
  }
    

  selectVideo(index: number) {
      this.selectedIndex = index;
      this.hasSelectedVideo = true;
  }
    


  isEmpty(): boolean{
    return this.uploadedVideos.length == 0;
  }

  back(){
    this.activeModal.close('back')
    if(!this.toComplete){
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

  handleCheckboxClick() {
    this.uploadedVideos = []
    this.nextPageToken = undefined
    if(this.isChecked){
      this.getNextVideos()
      this.isChecked = false
    }
    else{
      this.getNextBroadcast()
      this.isChecked = true
    }
  }

  submitVideo(){
    this.youtubeAPI.fetchYoutTubeVideoByID(this.uploadedVideos[this.selectedIndex].videoId).subscribe(video =>{
      if(this.toComplete){
        this.videoInfo.setSelectedMainYoutubeVideoInfo(video.items[0]);
        const modalRef = this.modalService.open(ConfirmationComponent, {size: 'md', scrollable: true,centered: true , animation: false})
        modalRef.componentInstance.message = "Are you sure you want to use this video?"
        modalRef.componentInstance.youtubeVideoInfo = video.items[0]
        modalRef.result.then(result => {
          if(result === 'continue'){
            this.activeModal.close(result)
          }
        })
      }
      else{
        if(video.items[0].status.privacyStatus !== "private"){
          this.modalService.open(ImportYoutubeVideoComponent, { size: 'lg', scrollable: true, centered: true , keyboard: false, backdrop: 'static', animation: false, })
            .componentInstance.mistake = "The Selected Video doesn't have it's privacy status set to 'private'."
          this.activeModal.close();
        }
        else if(video.items[0].snippet.liveBroadcastContent === 'none' && this.lessThan10Minutes(video.items[0].contentDetails.duration)){
          this.modalService.open(ImportYoutubeVideoComponent, { size: 'lg', scrollable: true, centered: true , keyboard: false, backdrop: 'static', animation: false, })
            .componentInstance.mistake = "The Selected Video is less than 10 minutes long."
          this.activeModal.close();
        }
        else if(video.items[0].snippet.liveBroadcastContent === 'live' && video.items[0].snippet.liveBroadcastContent === 'completed '){
          this.modalService.open(ImportYoutubeVideoComponent, { size: 'lg', scrollable: true, centered: true , keyboard: false, backdrop: 'static', animation: false, })
            .componentInstance.mistake = "The Selected Livestream must be set to 'upcoming'."
          this.activeModal.close();
        }
        else{
          this.videoInfo.setYoutubeAllVideoInfo(this.uploadedVideos)
          this.videoInfo.setSelectedMainYoutubeVideoInfo(video);
          this.modalService.open(CompleteVideoComponent, { size: 'lg', scrollable: true, centered: true , keyboard: false, animation: false, backdrop: 'static' });
          this.activeModal.close();
        }
      }
    })
  }

  getNextVideos(){
    this.youtubeAPI.fetchLatestYouTubeVideos(this.nextPageToken).subscribe({
      next: (data:YoutubeVideoResponse) => {
        const newData = data.items.map(item => {
          return {
            videoId: item.id.videoId,
            description: item.snippet.description,
            publishTime: item.snippet.publishTime,
            thumbnails: item.snippet.thumbnails,
            title: item.snippet.title
          };
        });
        this.uploadedVideos.push(...newData)
        this.nextPageToken = data.nextPageToken
        this.isScrollHandlerActive = true
      },
      error: (err:HttpErrorResponse) => {
        this.isScrollHandlerActive = true
        this.alertService.addAlert(err.message, "danger")
      },
      complete: () => {}
    }); 
  }

  getNextBroadcast(){
    this.youtubeAPI.fetchLatestLiveYouTubeVideos(this.nextPageToken).subscribe({
      next: (data:YoutubeVideoResponse) => {
        const newData = data.items.map(item => {
          return {
            videoId: item.id,
            description: item.snippet.description,
            publishTime: item.snippet.publishTime,
            thumbnails: item.snippet.thumbnails,
            title: item.snippet.title
          };
        });
        this.uploadedVideos.push(...newData)
        this.nextPageToken = data.nextPageToken
        this.isScrollHandlerActive = true
      },
      error: (err:HttpErrorResponse) => {
        this.isScrollHandlerActive = true
        this.alertService.addAlert(err.message, "danger")
      },
      complete: () => {}
    }); 
  }

  @HostListener('scroll', ['$event'])
  onScroll(event: any) {
    if (this.isScrollHandlerActive) {
      const scrollableContent = this.elementRef.nativeElement.querySelector('.modal-body');
      const windowHeight = 'innerHeight' in window ? window.innerHeight : document.documentElement.offsetHeight;
      const docHeight = scrollableContent.scrollHeight;
      const windowBottom = scrollableContent.scrollTop + windowHeight;

      if (windowBottom >= docHeight - 50) {
        this.isScrollHandlerActive = false; // Disable the event handler while loading data
        if(this.nextPageToken){
          if(this.isChecked){
            this.getNextBroadcast()
          }
          else{
            this.getNextVideos()
          }
        }
      }
    }
  }
}