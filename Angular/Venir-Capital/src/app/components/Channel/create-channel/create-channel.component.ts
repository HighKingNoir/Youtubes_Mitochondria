import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { FormGroup, FormArray, FormControl, Validators, FormBuilder } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { DateTime } from 'luxon';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { ChannelRequestPayload, ChannelService } from 'src/app/services/Channel/channel.service';
import { UserService } from 'src/app/services/User/user-service';
import { SelectPlatformComponent } from '../../Accessories/Popups/select-platform/select-platform.component';
import { GoogleAPIService } from 'src/app/services/GoogleAPI/google-api.service';
import { CreateChannelService } from 'src/app/services/CreateChannel/create-channel.service';
import { Channels, StreamerInfo } from 'src/app/models/Channels/Channels';
import { ConfirmationComponent } from '../../Accessories/Popups/confirmation/confirmation.component';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { NewChannelRequestedComponent } from '../../Accessories/Popups/new-channel-requested/new-channel-requested.component';

@Component({
  selector: 'app-create-channel',
  templateUrl: './create-channel.component.html',
  styleUrls: ['./create-channel.component.css']
})
export class CreateChannelComponent implements OnInit{
logoSelectionString = "Select Logo"
enteredChannelLogo?: string;

ChannelRequestForm: FormGroup;
formControls: FormArray;
channelLogoError = false

@ViewChild('channelNameInput', { static: false }) channelNameInput!: ElementRef;


constructor(
  public alertService:AlertService ,
  private userService: UserService, 
  private channelService: ChannelService, 
  private fb: FormBuilder, 
  private modalService: NgbModal,
  private createChannelService:CreateChannelService,
  private authServce: AuthenticationService,
){
    const channelLogo = this.createChannelService.getChannelLogo()
    if(channelLogo){
      this.enteredChannelLogo = channelLogo;
      this.logoSelectionString = "Change Logo"
    }
    this.formControls = this.fb.array([]);


    // Loop through each item in the streamerInfo array and create a FormControl for averageWeeklyViewers
    this.ChannelRequestForm = this.fb.group({
      formControls: this.formControls,
      channelName: new FormControl(this.createChannelService.getChannelName(),[Validators.required, Validators.pattern('^[a-zA-Z0-9_-]{3,20}$')], [this.channelService.validateChannelNameAvailability()]),
    });
  }
  ngOnInit(): void {
    this.createChannelService.getStreamerInfo().forEach((streamerInfo) => {
      this.formControls.push(
        new FormGroup({
          streamerId: new FormControl(streamerInfo.streamerId),
          platform: new FormControl(streamerInfo.platform),
          username: new FormControl(streamerInfo.username),
          averageWeeklyViewers: new FormControl(0, [Validators.min(0)]),
        })
      );
    });
  }



  updateLocalStorage(index: number) {
    // Update the specific control's value in the form controls array
    const updatedValue = this.formControls.at(index).get('averageWeeklyViewers')?.value;
    // Update the streamerInfo array in your createChannelService
    const streamerInfo = this.createChannelService.getStreamerInfo();
    streamerInfo[index].averageWeeklyViewers = updatedValue;
    this.createChannelService.setStreamerInfo(streamerInfo);
  }

  

  handleInputBlur() {
    const channelName = this.channelNameInput.nativeElement.value;
    this.createChannelService.setChannelName(channelName);
  }

  addFormControl() {
    this.modalService.open(SelectPlatformComponent, {size: 'md', scrollable: true,centered: true , animation: false})
  }

  onLogoChange(event: any): void {
    const selectedFile = event.target.files[0];
    if (selectedFile) {
      // Check if the selected file is less than 4MB
      if (selectedFile.size <= 4096 * 1024) {
        // Handle the selected logo file (e.g., upload to server)
        this.logoSelectionString = "Change Logo";
        const reader = new FileReader();
        reader.readAsDataURL(selectedFile);
        reader.onload = (event: any) => {
          this.enteredChannelLogo = event.target.result;
          if(this.enteredChannelLogo){
            this.createChannelService.setChannelLogo(this.enteredChannelLogo);
            this.channelLogoError = false;
          }
        }
      } else {
        // File is too large, display an error message or take appropriate action
        this.alertService.addAlert("Selected file is larger than 4MB", "danger")
      }
    }
  }

  removeFormControl(index: number) {
    this.formControls.removeAt(index);
    this.createChannelService.removeStreamerInfo(index)
  }

  getUserTimezoneAbbreviation(): string {
    const userTimezone = DateTime.local().setZone(
      Intl.DateTimeFormat().resolvedOptions().timeZone
    );
    return userTimezone.offsetNameShort || 'EST';
  }


  Submit(){
    if(this.enteredChannelLogo){
      const modalRef = this.modalService.open(ConfirmationComponent, {size: 'md', scrollable: true,centered: true , animation: false})
      modalRef.componentInstance.message = "If your channel(s) do not exceed the minimum required average weekly viewers of " + this.channelService.getMinAWV() + ", there's a likelihood your request will get denied. Do you wish to continue?"
      modalRef.result.then(result => {
        if(result === 'continue'){
          const JWT = window.localStorage.getItem('token') || ''
          this.authServce.checkJWTExpiration(JWT).then((JWTResult) => {
            this.sendRequest()
          })
        }
      })
    }
    else{
      this.channelLogoError = true;
    }
  }


  sendRequest(){
    if(this.enteredChannelLogo){
      const channelRequestPayload: ChannelRequestPayload = {
        channelLogo: this.enteredChannelLogo,
        channelName: this.ChannelRequestForm.get('channelName')?.value,
        steamerInfo: this.formControls.value,
        timezone: this.getUserTimezoneAbbreviation() || "EST"
      }
      this.channelService.newChannel(channelRequestPayload).subscribe({
        
        next: (channel:Channels) => {
          this.logoSelectionString = "Select Logo"
          this.ChannelRequestForm.reset()
          this.formControls = this.fb.array([]);
          this.channelService.setChannel(channel)
          this.createChannelService.clearLocalStorage()
          const modalRef = this.modalService.open(NewChannelRequestedComponent, {size: 'md', scrollable: true,centered: true , animation: false})
          modalRef.componentInstance.channelName = channelRequestPayload.channelName
        },
        error: (error) => {
          this.alertService.addAlert(error, "danger")
          
        },
        complete: () => {}
      })
    } else {
      this.channelLogoError = true;
    }
  }
  
}
