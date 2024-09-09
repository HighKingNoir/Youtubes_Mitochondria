import { Component } from '@angular/core';
import { ActivatedRoute, Route, Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AccountSignUpComponent } from 'src/app/components/CreateAccount/account-sign-up/account-sign-up.component';
import { LoginAccountComponent } from 'src/app/components/Login/login-account/login-account.component';
import { Messages } from 'src/app/models/Messages/messages';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { GoogleAPIService } from 'src/app/services/GoogleAPI/google-api.service';
import { MessagesService } from 'src/app/services/MessageService/messages.service';
import { UserService } from 'src/app/services/User/user-service';

@Component({
  selector: 'app-header-search',
  templateUrl: './header-search.component.html',
  styleUrls: ['./header-search.component.css']
})
export class HeaderSearchComponent {
  Logo = "assets/Logo.png"
  isDropdownOpen = false;
  unReadMessages: Messages[] = []
  calculatedValue: number
  searchText = 'Videos...'
  searchQuery = '';
  isLoggedIn: boolean;

  constructor(
    private userService: UserService, 
    private messageService: MessagesService,
    private router: Router,
    private route: ActivatedRoute,
    private googleAuthService:GoogleAPIService,
    private authService:AuthenticationService,
    private modalService: NgbModal, 
  ){
    this.calculatedValue = this.calculateValue();
    if(this.router.url.startsWith("/Channels") || this.router.url.startsWith("/Subscriptions")){
      this.searchText = 'Channels...'
    }
    if(localStorage.getItem('token')){
      this.isLoggedIn = true
    }
    else{
      this.isLoggedIn = false
    }
    this.authService.isLoggedInProfile$.subscribe(isLoggedInStatus => {
      this.isLoggedIn = isLoggedInStatus
      if(isLoggedInStatus){
        this.userService.getUserFromServer().subscribe(user => {
          this.userService.UpdateUser(user)
        })
        const JWT = localStorage.getItem('token') || ''
        this.messageService.getAllUnreadMessages(JWT).subscribe(messages =>{
          this.unReadMessages = messages
        })
      }
    })

}
  

  ngOnInit(): void {
    if(this.router.url.startsWith("/Search")){
      this.route.queryParams.subscribe(params => {
        this.searchQuery = params['searchQuery'];
        if(params['isVideoSearch'] === 'false'){
          this.searchText = 'Channels...'
        }
      });
    }

    if(this.isLoggedIn){
        this.userService.getUserFromServer().subscribe(user => {
          this.userService.UpdateUser(user)
        })
        const JWT = localStorage.getItem('token') || ''
        this.messageService.getAllUnreadMessages(JWT).subscribe(messages =>{
          this.unReadMessages = messages
        })
      }
    }
    

  logIn(){
    this.modalService.open(LoginAccountComponent, { size: 'md', scrollable: true, centered: true , animation: false, })
  }

  signUp(){
    this.modalService.open(AccountSignUpComponent, { size: 'md', scrollable: true, centered: true , animation: false, })
  }

  search() {
    if(this.router.url.startsWith("/Search")){
      this.route.queryParams.subscribe(params => {
        this.router.navigate(['/Search'], {
          queryParams: { searchQuery: this.searchQuery, isVideoSearch: params['isVideoSearch'] }
        });
      });
    }
    else{
      if(this.searchText === 'Videos...'){
        this.router.navigate(['/Search'], {
          queryParams: { searchQuery: this.searchQuery, isVideoSearch: true.toString() }
        });
      }
      else{
        this.router.navigate(['/Search'], {
          queryParams: { searchQuery: this.searchQuery, isVideoSearch: false.toString() }
        });
      }
    }
    
  }

  
  /*
  Logs out user
    On success, JWT token in local storage will be removed
    On failure, N/A
  */
  LogOut(){
    this.googleAuthService.logOut()
    this.authService.logOut()
  }

  calculateValue(): number {
    // Get the height in pixels (90vh)
    const heightInPixels = window.innerHeight * 0.9; // 90vh
  
    // Subtract 80px
    const subtractedValue = heightInPixels - 80; // Subtract 80px
  
    // Divide by 40px and return the whole number
    const result = Math.floor(subtractedValue / 40);
  
    return result;
  }

  addSpaceBeforeUppercase(value: string): string {
    return value.replace(/([A-Z])/g, ' $1');
  }

  selectMessage(message: Messages) {
    localStorage.setItem('selectedMessage', JSON.stringify(message));
    this.router.navigateByUrl(`/Messages`)
  }

  readMessage(messageId: string){
    this.messageService.markAsRead(messageId).subscribe(() => {
      // Find the index of the message with the given messageId in the unReadMessages array
      const index = this.unReadMessages.findIndex(message => message.messageId === messageId);
  
      // If the message is found, remove it from the array
      if (index !== -1) {
        this.unReadMessages.splice(index, 1);
      }
    });
  } 

  toggleDropdown() {
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  Authorized(): boolean{
    return this.userService.getUserRole() != "USER";
  }
}
