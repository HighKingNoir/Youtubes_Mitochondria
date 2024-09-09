import { Component, Input, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-first-videos',
  templateUrl: './first-videos.component.html',
  styleUrls: ['./first-videos.component.css']
})
export class FirstVideosComponent implements OnInit {

  @Input() releaseDate: number[] = []
  dayBefore = ''
  twoDaysAfter = ''

  constructor(
    public activeModal:NgbActiveModal, 
  ){
  
  }

  ngOnInit(): void {
    const releaseDateJS = new Date(this.releaseDate[0], this.releaseDate[1] - 1, this.releaseDate[2]);

    const dayBefore = new Date(releaseDateJS);
    dayBefore.setDate(dayBefore.getDate() - 1);
    

    const twoDaysAfter = new Date(releaseDateJS);
    twoDaysAfter.setDate(twoDaysAfter.getDate() + 2);
    // Format the date string
    this.dayBefore = this.formatDate(dayBefore);
    this.twoDaysAfter = this.formatDate(twoDaysAfter);
  }

  private formatDate(date: Date) {
    const monthNames = [
      "Jan", "Feb", "Mar", "Apr", "May", "Jun",
      "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    ];
  
    const day = date.getDate();
    const month = monthNames[date.getMonth()];
    const year = date.getFullYear();
    const daySuffix = this.getDaySuffix(day);
  
    return `${month} ${day}${daySuffix}, ${year}`;
  }
  
  private getDaySuffix(day: number) {
    if (day >= 11 && day <= 13) {
      return "th";
    }
    const lastDigit = day % 10;
    switch (lastDigit) {
      case 1:
        return "st";
      case 2:
        return "nd";
      case 3:
        return "rd";
      default:
        return "th";
    }
  }
  

}
