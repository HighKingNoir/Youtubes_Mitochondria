import { AbstractControl, ValidationErrors, ValidatorFn } from "@angular/forms";


export const matchpassword : ValidatorFn = (control: AbstractControl):ValidationErrors|null =>{
   const password = control.get('Password');
   const confirmpassword = control.get('ConfirmPassword');
   if(password && confirmpassword && password?.value != confirmpassword?.value){
      return {passwordmatcherror : true }
   }
   return null; 
}