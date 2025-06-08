import { AbstractControl, ValidationErrors, ValidatorFn } from "@angular/forms";


export const matchPassword : ValidatorFn = (control: AbstractControl):ValidationErrors|null =>{
   const password = control.get('Password');
   const confirmpassword = control.get('ConfirmPassword');
   if(password && confirmpassword && password?.value != confirmpassword?.value){
      return {passwordmatcherror : true }
   }
   return null; 
}

export const matchNewPassword : ValidatorFn = (control: AbstractControl):ValidationErrors|null =>{
    const password = control.get('newPassword');
    const confirmpassword = control.get('confirmNewPassword');
    if(password && confirmpassword && password?.value != confirmpassword?.value){
       return {passwordmatcherror : true }
    }
    return null; 
 }