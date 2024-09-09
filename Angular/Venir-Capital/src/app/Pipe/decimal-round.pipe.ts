import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'decimalRound'
})
export class DecimalRoundPipe implements PipeTransform {
  transform(value: number): number {
    return Math.round(value * 10000) / 10000;
  }
}