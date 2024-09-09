import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'addressShortener'
})
export class AddressShortenerPipe implements PipeTransform {
  transform(address: string, visibleChars = 4): string {
    if (!address) return '';

    const prefix = address.slice(0, visibleChars + 2); // Add 2 to account for "0x" prefix
    const suffix = address.slice(-visibleChars);

    return `${prefix}...${suffix}`;
  }
}
