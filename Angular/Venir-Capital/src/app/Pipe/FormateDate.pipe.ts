import { Pipe, PipeTransform } from '@angular/core';


@Pipe({
  name: 'formateDate'
})
export class FormateDatePipe implements PipeTransform {
  transform(date: number[]): string {
    const monthNames = [
      'Jan', 'Feb', 'March', 'April', 'May', 'June', 'July',
      'Aug', 'Sept', 'Oct', 'Nov', 'Dec'
    ];

    
    const month = monthNames[date[1] - 1];
    const day = date[2];
    const year = date[0];

    const daySuffix = this.getDaySuffix(day);

    return `${month} ${day}${daySuffix}, ${year}`;
  }

  private getDaySuffix(day: number): string {
    if (day >= 11 && day <= 13) {
      return 'th';
    }

    switch (day % 10) {
      case 1:
        return 'st';
      case 2:
        return 'nd';
      case 3:
        return 'rd';
      default:
        return 'th';
    }
  }
}
