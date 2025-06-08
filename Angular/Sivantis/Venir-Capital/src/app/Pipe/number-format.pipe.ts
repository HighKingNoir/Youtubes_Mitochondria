import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'numberFormat',
    standalone: false
})
export class NumberFormatPipe implements PipeTransform {
  transform(value: number): string {
    if (value >= 1_000_000_000) {
      const scaled = value / 1_000_000_000;
      return this.formatNumber(scaled) + 'B';
    } else if (value >= 1_000_000) {
      const scaled = value / 1_000_000;
      return this.formatNumber(scaled) + 'M';
    } else if (value >= 1_000) {
      const scaled = value / 1_000;
      return this.formatNumber(scaled) + 'K';
    } else {
      return this.formatNumber(value);
    }
  }

  private formatNumber(num: number): string {
    if (num % 1 === 0) {
      return num.toFixed(0); // no decimals for whole numbers like 10.00 => "10"
    } else if ((num * 10) % 1 === 0) {
      return num.toFixed(1); // one decimal like 10.10 => "10.1"
    } else {
      return num.toFixed(2); // two decimals like 10.01
    }
  }
}
