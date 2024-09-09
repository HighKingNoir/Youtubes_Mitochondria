import { Component, Input, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, FormControl, Validators, FormArray } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Channels } from 'src/app/models/Channels/Channels';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ChannelService, EditChannelRequestPayload, StreamerLinks } from 'src/app/services/Channel/channel.service';
import { UserService } from 'src/app/services/User/user-service';

@Component({
  selector: 'app-edit-channel',
  templateUrl: './edit-channel.component.html',
  styleUrls: ['./edit-channel.component.css']
})
export class EditChannelComponent implements OnInit{
  @Input() channel!: Channels
  bannerSelectionString = "Select Banner"
  editChannelForm: FormGroup;
  currentDescriptionLength = 0;
  showDescriptionCounter = false;
  hasChanges = false;
  
  formControls: FormArray;

  initialFormValues: { channelBanner: string, channelLogo: string, channelDescription: string, formControls: StreamerLinks[] } = {
    channelBanner: '',
    channelLogo: '',
    channelDescription: '',
    formControls: []
  };

  constructor(
    private formBuilder: FormBuilder,
    public activeModal: NgbActiveModal,
    private alertService: AlertService,
    private userService: UserService,
    private authService: AuthenticationService,
    private channelService: ChannelService,
    private fb: FormBuilder, 
  ){

    this.formControls = this.fb.array([]);
    this.editChannelForm = this.formBuilder.group({
      formControls: this.formControls,
      channelBanner: new FormControl('', [Validators.required]), // Remove square brackets
      channelLogo: new FormControl('', [Validators.required]), // Remove square brackets
      channelDescription: new FormControl('', [Validators.required]), // Remove square brackets
    });

  }

  ngOnInit(): void {
    this.editChannelForm.get('channelLogo')?.setValue(this.channel.channelLogo) 
    if(this.channel.channelDescription){
      this.currentDescriptionLength = this.channel.channelDescription.length
      this.editChannelForm.get('channelDescription')?.setValue(this.channel.channelDescription)
      this.initialFormValues.channelDescription = this.channel.channelDescription;
    }
    if(this.channel.channelBanner){
      this.bannerSelectionString = "Change Banner";
      this.editChannelForm.get('channelBanner')?.setValue(this.channel.channelBanner)
      this.initialFormValues.channelBanner = this.channel.channelBanner;
    }
    this.initialFormValues.channelLogo = this.channel.channelLogo;
    this.editChannelForm.valueChanges.subscribe(() => {
      this.hasChanges = this.hasFormChanges()
    });
    this.channel.streamerInfo.forEach((streamerInfo) => {
      this.formControls.push(
        new FormGroup({
          platform: new FormControl(streamerInfo.platform),
          username: new FormControl(streamerInfo.username),
          youtubeChannelId: new FormControl(streamerInfo.youtubeChannelId),
        })
      );
    });
    this.initialFormValues.formControls = this.formControls.value
    this.hasChanges = this.hasFormChanges()
  }

  updateYoutubeChannelId(index: number, event: any) {
    const newValue = event.target.value;
    if(newValue === ''){
      this.formControls.value[index].youtubeChannelId = null
    }
    else{
      this.formControls.value[index].youtubeChannelId = newValue;
    }
    this.hasChanges = this.hasFormChanges()
  }

  hasFormChanges(): boolean {
    const currentFormValues = this.editChannelForm.value;
    // Compare each field with the initial value
    if (
      currentFormValues.channelBanner === this.initialFormValues.channelBanner &&
      currentFormValues.channelLogo === this.initialFormValues.channelLogo &&
      currentFormValues.channelDescription === this.initialFormValues.channelDescription &&
      this.areStreamerLinksArraysEqual(currentFormValues.formControls, this.initialFormValues.formControls)
      ) {
      return false; // Differences found
    }
  
    return true; // No differences found
  }

  private areStreamerLinksArraysEqual(arr1: StreamerLinks[], arr2: StreamerLinks[]): boolean {
    if (arr1.length !== arr2.length) {
      return false; // Arrays have different lengths, so they can't be equal
    }
    
    for (let i = 0; i < arr1.length; i++) {
      const link1 = arr1[i];
      const link2 = arr2[i];
      
      if (link1.platform !== link2.platform || 
          link1.username !== link2.username || 
          link1.youtubeChannelId !== link2.youtubeChannelId) {
        return false; // Elements at the same index are not equal
      }
    }
    
    return true; // All elements are equal
  }
  
  streamLink(platform: string, to: string){
    switch(platform){
      case 'Twitch':
        return `twitch.tv/${to}`
      case "Kick":
        return `kick.com/${to}`
    }
    return ''
  }

  onBannerChange(event: any): void {
    const selectedFile = event.target.files[0];
    if (selectedFile) {
      // Check if the selected file is less than 6MB
      if (selectedFile.size <= 6144 * 1024) {
        // Handle the selected logo file (e.g., upload to server)
        this.bannerSelectionString = "Change Banner";
        const reader = new FileReader();
        reader.readAsDataURL(selectedFile);
        reader.onload = (event: any) => {
          this.editChannelForm.get('channelBanner')?.setValue(event.target.result);
        }
      } else {
        // File is too large, display an error message or take appropriate action
        this.alertService.addAlert("Selected file is larger than 6MB", "danger")
      }
    }
  }



  onLogoChange(event: any): void {
    const selectedFile = event.target.files[0];
    if (selectedFile) {
      // Check if the selected file is less than 4MB
      if (selectedFile.size <= 4096 * 1024) {
        // Handle the selected logo file (e.g., upload to server)
        const reader = new FileReader();
        reader.readAsDataURL(selectedFile);
        reader.onload = (event: any) => {
          this.editChannelForm.get('channelLogo')?.setValue(event.target.result)
        }
      } else {
        // File is too large, display an error message or take appropriate action
        this.alertService.addAlert("Selected file is larger than 4MB", "danger")
      }
    }
  }



  updateDescriptionCounter() {
    this.showDescriptionCounter = true;
    const maxLength = 5000;
    this.currentDescriptionLength = (document.getElementById('channelDescription') as HTMLTextAreaElement).value.length;
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

  editChannel(){
    const JWT: string = window.localStorage.getItem('token') || ''
    this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
      const editChannelRequestPayload: EditChannelRequestPayload = {
        channelBanner: this.editChannelForm.get('channelBanner')?.value,
        channelDescription: this.editChannelForm.get('channelDescription')?.value,
        channelID: this.channel.channelId,
        streamerLinks: this.formControls.value,
        channelLogo: this.editChannelForm.get('channelLogo')?.value
      }
      this.channelService.editChannel(editChannelRequestPayload).subscribe({
        next: (channel) => {
          this.channelService.setChannel(channel as Channels)
          this.activeModal.close('success')
        },
        error: (err) => {
          this.alertService.addAlert(err, "danger")
        },
        complete() {
            
        },
      })
    })
  }

}
