import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'manaStringFormat' })
export class manaStringFormatPipe implements PipeTransform {
  transform(manaString: string): string {
    const value = Number(manaString)
    if(value >= 1000000000){
    // Convert to billion format (X.XB)
    return (value / 1000000000).toFixed(2) + 'B';
    }
    else if (value >= 1000000) {
      // Convert to million format (X.XM)
      return (value / 1000000).toFixed(2) + 'M';
    } else if (value >= 1000) {
      // Convert to thousand format (X.XK)
      return (value / 1000).toFixed(2) + 'K';
    } else if (value == 0){
      return value.toFixed(0)
    }
      return value.toFixed(2);
  }
}
