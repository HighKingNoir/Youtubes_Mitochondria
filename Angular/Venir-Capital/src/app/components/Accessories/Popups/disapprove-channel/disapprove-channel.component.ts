import { Component, Input } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ChannelService, DisapproveChannelResponsePayload } from 'src/app/services/Channel/channel.service';

@Component({
  selector: 'app-disapprove-channel',
  templateUrl: './disapprove-channel.component.html',
  styleUrls: ['./disapprove-channel.component.css']
})
export class DisapproveChannelComponent {

  currentDescriptionLength = 0;
  showDescriptionCounter = false;

  @Input() channelId!: string
  @Input() channelName!: string

  reasonForm: FormGroup;

  constructor(
    private activeModal: NgbActiveModal,
    private formBuilder: FormBuilder,
    private authService:AuthenticationService,
    private channelService:ChannelService,
    private alertService: AlertService,
  ){

    this.reasonForm = this.formBuilder.group({
      reason: new FormControl('', [Validators.required]), // Remove square brackets
    });

  }

  submit(){
    const JWT = window.localStorage.getItem('token') || ''
    this.authService.checkJWTExpiration(JWT).then(() => {
      const disapproveChannelResponsePayload: DisapproveChannelResponsePayload = {
        channelID: this.channelId,
        reason: this.reasonForm.get("reason")?.value
      }
      this.channelService.disapproveChannel(disapproveChannelResponsePayload).subscribe({
        next: () => {
          this.activeModal.close("success")
        },
        error: () => {
          this.alertService.addAlert("Unable to send Payload", "danger")
        },
        complete: () => {}
      })
    })
  }
  
}
