import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, FormControl, Validators, ValidatorFn, AbstractControl, ValidationErrors } from '@angular/forms';
import { NgbActiveModal, NgbModal, NgbDatepickerConfig, NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { ContentRequestPayload } from 'src/app/models/Content/ContentRequestPayload';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ContentService } from 'src/app/services/Content/Content/content.service';
import { UploadVideoInfoService } from 'src/app/services/Content/VideoInfo/upload-video-info.service';
import { GoogleAPIService, UserInfo} from 'src/app/services/GoogleAPI/google-api.service';
import { UserService } from 'src/app/services/User/user-service';
import { YoutubeVideoResponse, YoutubeAPIService } from 'src/app/services/YouTubeAPI/youtube-api.service';
import { SelectTrailerVideoComponent } from '../SelectVideo/select-trailer-video/select-trailer-video.component';

@Component({
  selector: 'app-in-development-video',
  templateUrl: './in-development-video.component.html',
  styleUrls: ['./in-development-video.component.css']
})
export class InDevelopmentVideoComponent implements OnInit{
  GoogleUser!: UserInfo
  youtubeTrailerVideoInfo!: any
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
    releaseDate.setDate(releaseDate.getDate() + 6);
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
              this.youtubeTrailerVideoInfo = video.items[0];
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
        
        youtubeMainVideoID: '',
        youtubeTrailerVideoID: this.youtubeTrailerVideoInfo.id,
        numbBidders: 0,
        description: this.StageTwoForm.get("Description")?.value,
        startingCost: 0,
        releaseDate: this.StageTwoForm.get("ReleaseDate")?.value,      
        privacyStatus: null,
        thumbnail: this.youtubeTrailerVideoInfo.snippet.thumbnails.high.url,
        duration: null,
        youtubeProfilePicture: this.GoogleUser.info.picture,
        youtubeUsername: this.GoogleUser.info.name,
        googleSubject: this.GoogleUser.info.sub,
        liveBroadcastContent: '',
      }
      this.loading = "Loading..."
      if(this.isAuction(this.StageOneForm.get("VideoType")?.value)){
        contentRequestPayload.numbBidders = this.StageOneForm.get("NumBidders")?.value;
        contentRequestPayload.startingCost = this.StageOneForm.get("StartingCost")?.value;
      }
      this.contentService.createInDevelopmentContent(contentRequestPayload).subscribe({
        next: (data) => {
          this.contentService.addCreatedContent(data)
        },
        error: (error: HttpErrorResponse) => {
          this.alertService.addAlert(error.message, 'danger')
          this.loading = undefined
        },
        complete: () => {
          this.alertService.addAlert("Content Created", 'success')
          this.activeModal.close('Content Successfully Created')
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

