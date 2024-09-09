import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'timestampConverter'
})
export class TimestampConverterPipe implements PipeTransform {

  transform(timestamp: number): unknown {
    const currentTimestamp = Math.floor(Date.now() / 1000); // Current timestamp in seconds
    const timeDifference = currentTimestamp - timestamp;

    if (timeDifference < 60) {
      return timeDifference === 1 ? "1 sec" : `${timeDifference.toFixed(0)} secs`;
    } else if (timeDifference < 3600) {
      return timeDifference < 120 ? "1 min" : `${Math.floor(timeDifference / 60)} mins`;
    } else if (timeDifference < 86400) {
      return timeDifference < 7200 ? "1 hr" : `${Math.floor(timeDifference / 3600)} hrs`;
    } else if (timeDifference < 31536000) {
      return timeDifference < 172800 ? "1 day" : `${Math.floor(timeDifference / 86400)} days`;
    } else {
      return timeDifference < 63072000 ? "1 year" : `${Math.floor(timeDifference / 31536000)} years`;
    }
  }

}
