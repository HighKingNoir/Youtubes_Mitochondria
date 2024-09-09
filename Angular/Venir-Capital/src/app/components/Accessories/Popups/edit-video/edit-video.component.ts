import { HttpErrorResponse } from '@angular/common/http';
import { Component, Input, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, FormControl, Validators, ValidatorFn, AbstractControl, ValidationErrors } from '@angular/forms';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ContentRequestPayload } from 'src/app/models/Content/ContentRequestPayload';
import { CreatedContentDetails } from 'src/app/models/Content/CreatedContentDetails';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ContentService } from 'src/app/services/Content/Content/content.service';
import { UserService } from 'src/app/services/User/user-service';
import { YoutubeAPIService } from 'src/app/services/YouTubeAPI/youtube-api.service';
import { SelectTrailerVideoComponent } from '../CreateVideo/SelectVideo/select-trailer-video/select-trailer-video.component';
import { UploadVideoInfoService } from 'src/app/services/Content/VideoInfo/upload-video-info.service';
import { EditRequestPayload } from 'src/app/models/Content/EditRequestPayload';
import { GoogleAPIService } from 'src/app/services/GoogleAPI/google-api.service';

@Component({
  selector: 'app-edit-video',
  templateUrl: './edit-video.component.html',
  styleUrls: ['./edit-video.component.css']
})
export class EditVideoComponent implements OnInit{
  @Input() video!: CreatedContentDetails
  currentTitleLength = 0;
  trailerID = '';
  showTitleCounter = false;
  currentDescriptionLength = 0;
  showDescriptionCounter = false;
  trailerMessage?: string
  JWT: string = window.localStorage.getItem('token') || '{}'
  
  EditVidoForm: FormGroup;

  
  constructor(
    public activeModal: NgbActiveModal,
    private modalService: NgbModal, 
    private userService: UserService, 
    private videoInfo: UploadVideoInfoService,
    private contentService: ContentService, 
    private youtubeAPI: YoutubeAPIService,
    private formBuilder: FormBuilder,
    private authServce:AuthenticationService,
    public alertService:AlertService,
    private readonly googleAPIService: GoogleAPIService,
  ){

    this.EditVidoForm = this.formBuilder.group({
      Description: new FormControl('', [Validators.required]), // Remove square brackets
      VideoTitle: new FormControl('', [Validators.required]), // Remove square brackets
    });
  }


  ngOnInit(): void {
    this.trailerID = this.video.youtubeTrailerVideoID
    this.EditVidoForm.get('Description')?.setValue(this.video.description)
    this.EditVidoForm.get('VideoTitle')?.setValue(this.video.contentName)
  }


   
  
  

  swapTrailer(){
    if(this.googleAPIService.isLoggedIn()){
      this.youtubeAPI.fetchLatestYouTubeVideos().subscribe(fetchedVideos => {
      this.videoInfo.setYoutubeAllVideoInfo(fetchedVideos)
      this.modalService.open(SelectTrailerVideoComponent, { size: 'lg', scrollable: true, keyboard: false, animation: false, backdrop: 'static' }).result.then(() =>{
        this.trailerID = this.videoInfo.getSelectedTrailerYoutubeVideoInfo().videoId
        if(this.trailerID){
          this.youtubeAPI.fetchYoutTubeVideoByID(this.trailerID).subscribe(video =>{
            if(video[0].status.privacyStatus === "private"){
              this.trailerMessage = "Your trailer is set to private. Consider setting your trailer to 'public' to allow everyone to view it."
            }
            else{
              this.trailerMessage = undefined
            }
          })
        }});
      });
    }
    else{
      this.trailerMessage = "Login to your Youtube account to get your trailer."
    }
  }





  /*
  Creates a new video
    On success, Creates a new content entity with content type set to Active
    On failure, N/A
  */
  submit(){
    const JWT: string = window.localStorage.getItem('token') || ''
    this.authServce.checkJWTExpiration(JWT).then((JWTResult) => {
      const editRequestPayload: EditRequestPayload = {
        contentID: this.video.contentId,
        
        contentName: this.EditVidoForm.get("VideoTitle")?.value,
        description: this.EditVidoForm.get("Description")?.value,
        youtubeTrailerVideoID: this.trailerID,
      }
      this.contentService.editVideo(editRequestPayload).subscribe({
        next: () => {
        },
        error: (error: HttpErrorResponse) => {
          this.alertService.addAlert(error.message, 'danger')
        },
        complete: () => {
          this.videoInfo.setEditedVideo(editRequestPayload);
          this.alertService.addAlert("Content Edited", 'success')
          this.activeModal.close('Content Successfully Edited')
        }
      })
    })
    
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

