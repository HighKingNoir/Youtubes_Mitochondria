import { CanActivateFn} from '@angular/router';
import { UserService } from '../User/user-service';
import { inject } from '@angular/core';
import { environment } from 'src/Environment/environment';


export const authorizedPersonnel: CanActivateFn = () =>{
  if(environment.production){
    const user = inject(UserService);
    const role = user.getUserRole();
    if(role != "USER"){
      return true;
    }
    return false
  }
  return true
}
  

export const adminOnly: CanActivateFn = () =>{
  if(environment.production){
    const user = inject(UserService);
    const role = user.getUserRole();
    if(role == "ADMIN"){
      return true;
    }
    return false
  }
  return true
}
  
  
  
