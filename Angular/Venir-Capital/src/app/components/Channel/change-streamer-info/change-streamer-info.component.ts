import { Component, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, FormControl, Validators } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { DateTime } from 'luxon';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ChannelService, ResubmitChannelRequestPayload } from 'src/app/services/Channel/channel.service';
import { CreateChannelService } from 'src/app/services/CreateChannel/create-channel.service';
import { UserService } from 'src/app/services/User/user-service';
import { ConfirmationComponent } from '../../Accessories/Popups/confirmation/confirmation.component';
import { SelectPlatformComponent } from '../../Accessories/Popups/select-platform/select-platform.component';
import { Channels, StreamerInfo } from 'src/app/models/Channels/Channels';
import { ActivatedRoute } from '@angular/router';
import { EditChannelService } from 'src/app/services/EditChannel/edit-channel.service';
import { get } from 'http';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-change-streamer-info',
  templateUrl: './change-streamer-info.component.html',
  styleUrls: ['./change-streamer-info.component.css']
})
export class ChangeStreamerInfoComponent implements OnInit{
  formControls: FormArray;
  enteredChannelLogo = '';

  ChannelRequestForm: FormGroup;
  
  
  constructor(
    public alertService:AlertService ,
    private userService: UserService, 
    private channelService: ChannelService, 
    private fb: FormBuilder, 
    private modalService: NgbModal,
    private editChannelService:EditChannelService,
    private authServce: AuthenticationService,
    private route: ActivatedRoute,
  ){
      this.formControls = this.fb.array([]);
      
      this.ChannelRequestForm = this.fb.group({
        formControls: this.formControls,
        channelName: new FormControl('',[Validators.required, Validators.pattern('^[a-zA-Z0-9_-]{3,20}$')], [this.channelService.validateChannelNameAvailability()]),
      });
    }
  
  ngOnInit(): void {
    const editChannelData = localStorage.getItem('editChannelData');
    if(editChannelData){
      const channelLogo = this.editChannelService.getChannelLogo()
      if(channelLogo){
        this.enteredChannelLogo = channelLogo
      }
      this.ChannelRequestForm.get('channelName')?.setValue(this.editChannelService.getChannelName())
      this.setFormControls(this.editChannelService.getStreamerInfo())
    }
    else{
      this.channelService.getChannel(this.route.snapshot.params['name']).subscribe((channel:Channels) => {
        this.editChannelService.setChannelInfo(channel)
        this.ChannelRequestForm.get('channelName')?.setValue(channel.channelName)
        this.enteredChannelLogo = channel.channelLogo
        this.setFormControls(channel.streamerInfo)
      })
    }
  
  }

  setFormControls(streamerInfo: StreamerInfo[]){
    streamerInfo.forEach((streamerInfo) => {
      this.formControls.push(
        new FormGroup({
          streamerId: new FormControl(streamerInfo.streamerId),
          platform: new FormControl(streamerInfo.platform),
          username: new FormControl(streamerInfo.username),
          averageWeeklyViewers: new FormControl(0, [Validators.min(0)]),
        })
      );
    });
    this.ChannelRequestForm.get("formControls")?.setValue(this.formControls)
  }
  
  ResetChanges(){
    this.channelService.getChannel(this.route.snapshot.params['name']).subscribe((channel:Channels) => {
      this.editChannelService.setChannelInfo(channel)
      this.ChannelRequestForm.get('channelName')?.setValue(channel.channelName)
      this.enteredChannelLogo = channel.channelLogo
      this.formControls = this.fb.array([]);
      this.setFormControls(channel.streamerInfo)
    })
  }

    addFormControl() {
      this.modalService.open(SelectPlatformComponent, {size: 'md', scrollable: true,centered: true , animation: false})
    }
  
    removeFormControl(index: number) {
      this.formControls.removeAt(index);
      this.editChannelService.removeStreamerInfo(index)
    }
  
    getUserTimezoneAbbreviation(): string {
      const userTimezone = DateTime.local().setZone(
        Intl.DateTimeFormat().resolvedOptions().timeZone
      );
      return userTimezone.offsetNameShort || 'EST';
    }
  
  
    Submit(){
      const modalRef = this.modalService.open(ConfirmationComponent, {size: 'md', scrollable: true,centered: true , animation: false})
      modalRef.componentInstance.message = "If your channel(s) does not exceed the minimum required average weekly viewers of " + this.channelService.getMinAWV() + ", there's a likelihood your request will get denied. Do you wish to continue?"
      modalRef.result.then(result => {
        if(result === 'continue'){
          const JWT = localStorage.getItem('token') || ''
          this.authServce.checkJWTExpiration(JWT).then(() => {
            this.sendRequest()
          })
        }
      })
    }
  
  
  sendRequest(){
    const channelRequestPayload: ResubmitChannelRequestPayload = {
      channelName: this.ChannelRequestForm.get('channelName')?.value,
      steamerInfo: this.formControls.value,
    }
    this.channelService.resubmitChannel(channelRequestPayload).subscribe({
      next: () => {
        this.alertService.addAlert("Resubmission  Successful", "success")
        this.editChannelService.clearLocalStorage()
      },
      error: (err) => {
        this.alertService.addAlert(err, "danger")
      },
      complete: () => {}
    })
  } 

}
    

