import { Component, Input } from '@angular/core';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { StreamerInfo } from 'src/app/models/Channels/Channels';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { SelectPlatformComponent } from '../../select-platform/select-platform.component';
import { ChannelService } from 'src/app/services/Channel/channel.service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { HttpErrorResponse } from '@angular/common/http';


@Component({
  selector: 'app-edit-stream-info',
  standalone: false,
  templateUrl: './edit-stream-info.component.html',
  styleUrl: './edit-stream-info.component.css'
})
export class EditStreamInfoComponent {
  @Input() streamerInfo: StreamerInfo[] = []
  @Input() channelName: string = ''


  constructor(
    public activeModal: NgbActiveModal,
    private alertService: AlertService,
    private modalService: NgbModal,
    private channelService: ChannelService,
    private authService: AuthenticationService,
  ){

  }

  addStreamerInfo() {
    localStorage.setItem("addStreamerInfoRedirect",this.channelName)
    this.modalService.open(SelectPlatformComponent, {size: 'md', scrollable: true,centered: true , animation: false})
  }

  removeStreamerInfo(index: number) {
    if (index !== -1) {
      const JWT = localStorage.getItem('token') || ''
      this.authService.checkJWTExpiration(JWT).then(() => {
        console.log(this.channelName)
        this.channelService.removeStreamerInfo(this.channelName, index).subscribe({
          next: (data:any) => {
            this.alertService.addAlert(data, 'success')
            this.streamerInfo.splice(index, 1);
            this.activeModal.close("successful")
          },
          error: (err:HttpErrorResponse) => {
            this.alertService.addAlert(err.message, "danger")
          },
          complete: () => {}
        });
      })
    }
  }
}
