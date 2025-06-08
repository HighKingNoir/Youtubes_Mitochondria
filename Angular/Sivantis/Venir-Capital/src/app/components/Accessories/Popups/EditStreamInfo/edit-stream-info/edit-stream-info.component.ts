import { Component, Input } from '@angular/core';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { StreamerInfo } from 'src/app/models/Channels/Channels';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { SelectPlatformComponent } from '../../select-platform/select-platform.component';

@Component({
  selector: 'app-edit-stream-info',
  standalone: false,
  templateUrl: './edit-stream-info.component.html',
  styleUrl: './edit-stream-info.component.css'
})
export class EditStreamInfoComponent {
  @Input() streamerInfo!: StreamerInfo[]


  constructor(
    public activeModal: NgbActiveModal,
    private alertService: AlertService,
    private modalService: NgbModal,
  ){

  }

  addStreamerInfo() {
      this.modalService.open(SelectPlatformComponent, {size: 'md', scrollable: true,centered: true , animation: false})
  }

  removeStreamerInfo(index: number) {
    if (index !== -1) {
        this.streamerInfo.splice(index, 1);
      }

  }
}
