import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-new-channel-requested',
  templateUrl: './new-channel-requested.component.html',
  styleUrls: ['./new-channel-requested.component.css']
})
export class NewChannelRequestedComponent {

  @Input() channelName!: string
  
  constructor(
    public activeModal: NgbActiveModal,
    private router: Router, 
  ){

  }

  routeToChannel(){
    this.activeModal.close('Channel Page')
    this.router.navigateByUrl(`/Channel/${this.channelName}`)
  }

}
