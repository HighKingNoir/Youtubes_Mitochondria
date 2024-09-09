import { Component, Input } from '@angular/core';
import { AbstractControl, FormBuilder, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { NgbActiveModal, NgbDateStruct, NgbDatepickerConfig } from '@ng-bootstrap/ng-bootstrap';
import { UploadVideoInfoService } from 'src/app/services/Content/VideoInfo/upload-video-info.service';

@Component({
  selector: 'app-confirmation',
  templateUrl: './confirmation.component.html',
  styleUrls: ['./confirmation.component.css']
})
export class ConfirmationComponent {

  @Input() message = '';
  @Input() videoConfirmation?: VideoConfirmation;
  @Input() reactivateContent = false;
  @Input() youtubeVideoInfo?: any

  releaseDateForm: FormGroup
  constructor(
    public activeModal: NgbActiveModal,
    private formBuilder: FormBuilder,
    private config:NgbDatepickerConfig,
    private videoInfo: UploadVideoInfoService,
  ){
    const releaseDate = new Date();
    releaseDate.setDate(releaseDate.getDate() + 2);
    this.config.minDate = { year: releaseDate.getFullYear(), month: releaseDate.getMonth() + 1, day: releaseDate.getDate() };
    const minDate = new Date(releaseDate.getFullYear(), releaseDate.getMonth() + 1, releaseDate.getDate());

    this.config.markDisabled = (date: NgbDateStruct) => {
      const selected = new Date(date.year, date.month - 1, date.day);
      return selected <= releaseDate;
    };

    this.releaseDateForm = this.formBuilder.group({
      ReleaseDate: new FormControl('', [Validators.required, this.minDateValidator(minDate)]), // Remove square brackets
    });
  }

  continue(){
    if(this.reactivateContent){
      this.videoInfo.setNewReleaseDate(this.releaseDateForm.get('ReleaseDate')?.value)
    }
    this.activeModal.close('continue')
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


}

export interface VideoConfirmation{
    contentName: string;
    description: string;
    thumbnail: string;
}