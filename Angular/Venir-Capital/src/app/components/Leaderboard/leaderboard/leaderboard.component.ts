import { Component, HostListener, OnInit } from '@angular/core';
import { Leaderboard } from 'src/app/models/Users/Leaderboard';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { UserService } from 'src/app/services/User/user-service';

@Component({
  selector: 'app-leaderboard',
  templateUrl: './leaderboard.component.html',
  styleUrls: ['./leaderboard.component.css']
})
export class LeaderboardComponent implements OnInit{

  Users: Leaderboard[] = []
  private isScrollHandlerActive = false;
  private lastVideo = false
  private totalHype?: number;

  constructor(
    public alertService:AlertService,
    private userService:UserService
  ){}
  
  
  ngOnInit(): void {
    this.userService.getLeaderBoard(this.totalHype).subscribe({
      next: (data:any) => {
        this.Users.push(...data);
        if (data.length == 50) {
          this.totalHype = data[data.length - 1].totalHype;
          this.isScrollHandlerActive = true
        }
        else{
          this.lastVideo = true
        }
      },
      error: () => {},
      complete: () => {}
    });
  }

  @HostListener('window:scroll', ['$event'])
  onScroll(event: any) {
    if(this.isScrollHandlerActive){
      const windowHeight = 'innerHeight' in window ? window.innerHeight : document.documentElement.offsetHeight;
      const body = document.body, html = document.documentElement;
      const docHeight = Math.max(body.scrollHeight, body.offsetHeight, html.clientHeight, html.scrollHeight, html.offsetHeight);
      const windowBottom = window.scrollY + windowHeight; // Calculate window bottom correctly

      if (windowBottom >= docHeight - 200) {
        this.isScrollHandlerActive = false;
        if(this.lastVideo == false){
          this.userService.getLeaderBoard(this.totalHype).subscribe({
            next: (data:any) => {
              this.Users.push(...data);
              if (data.length == 50) {
                this.totalHype = data[data.length - 1].totalHype;
              }
              else{
                this.lastVideo = true
              }
              this.isScrollHandlerActive = true;
            },
            error: () => {
              this.isScrollHandlerActive = true;
            },
            complete: () => {}
          });
        }
      }
    }
  }
}
