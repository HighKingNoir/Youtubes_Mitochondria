import { ValidatorFn, AbstractControl, ValidationErrors } from "@angular/forms";


  export const isNumber : ValidatorFn = (control: AbstractControl):ValidationErrors|null =>{
    const value = control.value;
    if (isNaN(value) || typeof value !== 'number') {
      return { 'notANumber': true }; // Return an error if not a number
    }
    return null; // Return null if it's a valid number
 }