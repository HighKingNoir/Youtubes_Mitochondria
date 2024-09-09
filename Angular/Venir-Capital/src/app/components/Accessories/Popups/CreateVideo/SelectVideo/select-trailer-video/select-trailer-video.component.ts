import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, HostListener, Input, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { UploadVideoInfoService } from 'src/app/services/Content/VideoInfo/upload-video-info.service';
import { YoutubeVideoResponse, YoutubeAPIService } from 'src/app/services/YouTubeAPI/youtube-api.service';

@Component({
  selector: 'app-select-trailer-video',
  templateUrl: './select-trailer-video.component.html',
  styleUrls: ['./select-trailer-video.component.css']
})
export class SelectTrailerVideoComponent implements OnInit{
  hasSelectedVideo = false;
  selectedIndex = -1;
  uploadedVideos: any[]  
  private isScrollHandlerActive = false;
  

  @Input() nextPageToken?: string


  constructor(
    public activeModal: NgbActiveModal,
    private videoInfo: UploadVideoInfoService,
    private youtubeAPI: YoutubeAPIService,
    private alertService: AlertService,
    private elementRef: ElementRef,
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


  submitVideo(){
      this.videoInfo.setSelectedTrailerVideoInfo(this.uploadedVideos[this.selectedIndex]);
      this.activeModal.close();
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
          this.getNextVideos()
        }
      }
    }
  }

}

