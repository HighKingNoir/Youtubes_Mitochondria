import { Component } from '@angular/core';
import { UserService } from 'src/app/services/User/user-service';

@Component({
  selector: 'app-authorized-access-sidebar',
  templateUrl: './authorized-access-sidebar.component.html',
  styleUrls: ['./authorized-access-sidebar.component.css']
})
export class AuthorizedAccessSidebarComponent {

  constructor(private userService: UserService,){
    
  }


  AuthorizedAdmin(): boolean{
    return this.userService.getUserRole() == "ADMIN";
  }
}
