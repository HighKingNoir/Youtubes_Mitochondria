import { HttpErrorResponse } from '@angular/common/http';
import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ReportContentRequestPayload } from 'src/app/models/Content/ReportContentRequestPayload';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ContentService } from 'src/app/services/Content/Content/content.service';
import { UserService } from 'src/app/services/User/user-service';

@Component({
  selector: 'app-report-video',
  templateUrl: './report-video.component.html',
  styleUrls: ['./report-video.component.css']
})
export class ReportVideoComponent implements OnInit{
  userEmail = ''
  reportVideoForm: FormGroup;
  currentReportLength = 0;

  @Input() contentID = '';

  constructor(
    private userService:UserService,
    private formBuilder: FormBuilder,
    private authService: AuthenticationService,
    private contentService: ContentService,
    private alertService: AlertService,
    public activeModal: NgbActiveModal,
  ){
    this.reportVideoForm = this.formBuilder.group({
      report: new FormControl('', [Validators.required]), // Remove square brackets
    });
  }

  ngOnInit(): void {
    this.userEmail = this.userService.getUserEmail()
  }

  submit(){
    const JWT: string = window.localStorage.getItem('token') || ''
    this.authService.checkJWTExpiration(JWT).then(() => {
      const reportContentRequestPayload: ReportContentRequestPayload = {
        contentID: this.contentID,
        report: this.reportVideoForm.get('report')?.value
      }
      this.contentService.reportVideo(reportContentRequestPayload).subscribe({
        next: () => {
        },
        error: (error) => {
          this.alertService.addAlert(error, 'danger')
        },
        complete: () => {
          this.alertService.addAlert("Video Successfully Reported", 'success')
          this.activeModal.close('Content Successfully Reported')
        }
      })
    })
  }

  updateReportCounter() {
    const maxLength = 300;
    this.currentReportLength = (document.getElementById('reportText') as HTMLTextAreaElement).value.length;
    this.currentReportLength = Math.min(this.currentReportLength, maxLength);
  }

  resizeReportTextarea(): void {
    const textarea = document.querySelector('textarea');
    if(textarea){
      textarea.style.height = 'auto';  // Reset the height to auto
      textarea.style.height = textarea.scrollHeight + 'px';  // Set the height based on content
    }
  }

  onReportTextareaKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      event.preventDefault(); // Prevent creating a new line
    }
  }

}
