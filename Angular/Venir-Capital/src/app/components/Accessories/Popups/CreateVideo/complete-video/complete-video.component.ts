import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, FormControl, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { NgbActiveModal, NgbDateStruct, NgbDatepickerConfig, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ContentRequestPayload } from 'src/app/models/Content/ContentRequestPayload';
import { ContentService } from 'src/app/services/Content/Content/content.service';
import { UploadVideoInfoService } from 'src/app/services/Content/VideoInfo/upload-video-info.service';
import { GoogleAPIService, UserInfo } from 'src/app/services/GoogleAPI/google-api.service';
import { UserService } from 'src/app/services/User/user-service';
import { YoutubeVideoResponse, YoutubeAPIService } from 'src/app/services/YouTubeAPI/youtube-api.service';
import { SelectTrailerVideoComponent } from '../SelectVideo/select-trailer-video/select-trailer-video.component';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { FirstVideosComponent } from '../../first-videos/first-videos.component';
import { CreatedContentDetails } from 'src/app/models/Content/CreatedContentDetails';

@Component({
  selector: 'app-complete-video',
  templateUrl: './complete-video.component.html',
  styleUrls: ['./complete-video.component.css']
})
export class CompleteVideoComponent implements OnInit{
  GoogleUser!: UserInfo
  youtubeVideoInfo: any
  maxBuyers: number;
  currentTitleLength = 0;
  trailerID?: string;
  showTitleCounter = false;
  currentDescriptionLength = 0;
  showDescriptionCounter = false;
  trailerMessage?: string
  showStageOne = true
  loading?: string
  
  StageOneForm: FormGroup;
  StageTwoForm: FormGroup;


  
  constructor(
    public activeModal: NgbActiveModal,
    private modalService: NgbModal, 
    private userService: UserService, 
    private contentService: ContentService, 
    private videoInfo: UploadVideoInfoService,
    private googleAPIService: GoogleAPIService,
    private youtubeAPI: YoutubeAPIService,
    private formBuilder: FormBuilder,
    private config:NgbDatepickerConfig,
    private authServce:AuthenticationService,
    public alertService:AlertService,
  ){
    this.googleAPIService.userProfileSubject.subscribe(user => {
      if(user){
        this.GoogleUser = user;
      }
    }) 


    this.maxBuyers = this.contentService.maxNumberOfBuyers
    this.StageOneForm = this.formBuilder.group({
      VideoType: new FormControl('', [Validators.required]), // Remove square brackets
      VideoTitle: new FormControl('', [Validators.required]), // Remove square brackets
      NumBidders: new FormControl('', [Validators.required]), // Remove square brackets
      StartingCost: new FormControl('', [Validators.required]), // Remove square brackets
    });
    const releaseDate = new Date();
    releaseDate.setDate(releaseDate.getDate() + 2);
    this.config.minDate = { year: releaseDate.getFullYear(), month: releaseDate.getMonth() + 1, day: releaseDate.getDate() };
    const minDate = new Date(releaseDate.getFullYear(), releaseDate.getMonth() + 1, releaseDate.getDate());

    this.config.markDisabled = (date: NgbDateStruct) => {
      const selected = new Date(date.year, date.month - 1, date.day);
      return selected <= releaseDate;
    };

    this.StageTwoForm = this.formBuilder.group({
      ReleaseDate: new FormControl('', [Validators.required, this.minDateValidator(minDate)]), // Remove square brackets
      Description: new FormControl('', [Validators.required]), // Remove square brackets
    });

  }


   
  
  ngOnInit(): void {
    this.youtubeVideoInfo = this.videoInfo.getSelectedMainYoutubeVideoInfo()
    this.StageOneForm.get("VideoTitle")?.setValue(this.youtubeVideoInfo.items[0].snippet.title)
    this.StageTwoForm.get("Description")?.setValue(this.youtubeVideoInfo.items[0].snippet.description)
  }

  greaterThanOrEqualOneHourFifteenMinutes(): boolean {
    const regex = /PT(\d+H)?(\d+M)?(\d+S)?/;
    const duration: string = this.youtubeVideoInfo.items[0].contentDetails.duration
    const matches = duration.match(regex);
  
    if (!matches) {
      return false; // Invalid duration format
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
  
    const totalMinutes = hours * 60 + minutes + seconds / 60;
  
    return totalMinutes >= 75; // Check if greater than or equal to 75 minutes (1 hour and 15 minutes)
  }

  isLive(): boolean{
    return this.youtubeVideoInfo.items[0].snippet.liveBroadcastContent === "upcoming"
  }

  onLinkTrailer(){
    if(!this.GoogleUser){
      this.alertService.addAlert("Error loading google user. Refreash Page", 'danger')
    }
    this.videoInfo.youtubeTrailerVideoInfo = undefined
    this.youtubeAPI.fetchLatestYouTubeVideos().subscribe((fetchedVideos: YoutubeVideoResponse) => {
      this.videoInfo.setYoutubeAllVideoInfo(fetchedVideos.items)
      const modalRef = this.modalService.open(SelectTrailerVideoComponent, { size: 'lg', scrollable: true, keyboard: false, animation: false, backdrop: 'static' })
      modalRef.componentInstance.nextPageToken = fetchedVideos.nextPageToken
      modalRef.result.then(() =>{
        this.trailerID = this.videoInfo.getSelectedTrailerYoutubeVideoInfo().videoId
        if(this.trailerID){
          this.youtubeAPI.fetchYoutTubeVideoByID(this.trailerID).subscribe(video =>{
            if(video.items[0].status.privacyStatus === "private"){
              this.trailerMessage = "Your trailer is set to private. Consider setting your trailer to 'public' to allow everyone to view it."
            }
            else{
              this.trailerMessage = undefined
            }
          })
        }
      });
    });
  }

  removeTrailer(){
    this.trailerID = undefined
    this.trailerMessage = undefined
  }

  isStageOneFormValid(): boolean {
    if(this.StageOneForm.get("VideoType")?.value){
      if(this.isAuction(this.StageOneForm.get("VideoType")?.value)){
        return this.StageOneForm.valid;
      }
      else{
        if(this.StageOneForm.get("VideoType")?.value && this.StageOneForm.get("VideoTitle")?.value){
          return this.StageOneForm.get("VideoType")!.valid && this.StageOneForm.get("VideoTitle")!.valid
        }
      }
    } 
    return false
  }

  /*
  Opens stage two of posting new content
    On success, the user and the video information they provided is sent to CreateVideo popup2
    On failure, N/A
  */
  next(){
    if(this.trailerID){
      this.showStageOne = false;
    }
    else{
      this.trailerMessage = "Trailer is Required for all videos. Select a Trailer to proceed."
    }
  }

  

  /*
  Creates a new video
    On success, Creates a new content entity with content type set to Active
    On failure, N/A
  */
  submit(){
    if(!this.GoogleUser){
      this.alertService.addAlert("Error loading google user. Refreash Page", 'danger')
    }
    const JWT = window.localStorage.getItem('token') || '{}'
    this.authServce.checkJWTExpiration(JWT).then((JWTResult) => {
      const contentRequestPayload: ContentRequestPayload =  { 
        contentName: this.StageOneForm.get("VideoTitle")?.value,
        contentType: this.StageOneForm.get("VideoType")?.value,
        youtubeMainVideoID: this.youtubeVideoInfo.items[0].id,
        youtubeTrailerVideoID: this.trailerID!,
        numbBidders: 0,
        description: this.StageTwoForm.get("Description")?.value,
        startingCost: 0,
        releaseDate: this.StageTwoForm.get("ReleaseDate")?.value,      
        privacyStatus: this.youtubeVideoInfo.items[0].status.privacyStatus,
        thumbnail: this.youtubeVideoInfo.items[0].snippet.thumbnails.high.url,
        duration: this.youtubeVideoInfo.items[0].contentDetails.duration,
        youtubeProfilePicture: this.GoogleUser.info.picture,
        youtubeUsername: this.GoogleUser.info.name,
        googleSubject: this.GoogleUser.info.sub,
        liveBroadcastContent: this.youtubeVideoInfo.items[0].snippet.liveBroadcastContent,
      }
      if(this.isAuction(this.StageOneForm.get("VideoType")?.value)){
        contentRequestPayload.numbBidders = this.StageOneForm.get("NumBidders")?.value;
        contentRequestPayload.startingCost = this.StageOneForm.get("StartingCost")?.value;
      }
      this.loading = "Loading..."
      this.contentService.createCompleteContent(contentRequestPayload).subscribe({
        next: (data: CreatedContentDetails) => {
          this.contentService.addCreatedContent(data)
          this.alertService.addAlert("Content Created", 'success')
          this.activeModal.close('Content Successfully Created')
          if(this.userService.getUserVideosPosted() == 0){
            this.userService.setFirstUserVideosPosted()
            const modalRef = this.modalService.open(FirstVideosComponent, {size: 'lg', scrollable: true,centered: true, keyboard: false, backdrop: 'static', animation: false})
            modalRef.componentInstance.releaseDate = data.releaseDate
          }

        },
        error: (error: HttpErrorResponse) => {
          this.alertService.addAlert(error.message, 'danger')
          this.loading = undefined
        },
        complete: () => {
        }
      })
    })
    
  }

  
  isAuction(videoType:string): boolean{
    return videoType == "Invention" || videoType == "Innovation"
  }

  

  updateTitleCounter() {
    this.showTitleCounter = true;
    const maxLength = 100;
    this.currentTitleLength = (document.getElementById('videotitle') as HTMLTextAreaElement).value.length;
    this.currentTitleLength = Math.min(this.currentTitleLength, maxLength);
  }

  resizeTitleTextarea(): void {
    const textarea = document.querySelector('textarea');
    if(textarea){
      textarea.style.height = 'auto';  // Reset the height to auto
      textarea.style.height = textarea.scrollHeight + 'px';  // Set the height based on content
    }
  }

  onTitleTextareaKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      event.preventDefault(); // Prevent creating a new line
    }
  }

  updateDescriptionCounter() {
    this.showDescriptionCounter = true;
    const maxLength = 5000;
    this.currentDescriptionLength = (document.getElementById('videoDescription') as HTMLTextAreaElement).value.length;
    this.currentDescriptionLength = Math.min(this.currentDescriptionLength, maxLength);
  }

  resizeDescriptionTextarea(): void {
    const textarea = document.querySelector('textarea');
    if(textarea){
      textarea.style.height = 'auto';  // Reset the height to auto
      textarea.style.height = textarea.scrollHeight + 'px';  // Set the height based on content
    }
  }

  onDescriptionTextareaKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      event.preventDefault(); // Prevent creating a new line
    }
  }

  minDateValidator(minDate: Date): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const selectedDate = control.value;
  
      if (selectedDate < minDate) {
        return { minDate: true }; // Validation failed
      }
      return null; // Validation passed
    };
  }
}






