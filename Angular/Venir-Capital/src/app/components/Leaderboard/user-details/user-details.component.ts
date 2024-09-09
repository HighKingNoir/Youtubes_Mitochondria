import { Component, HostListener, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CreatedContentDetails } from 'src/app/models/Content/CreatedContentDetails';
import { Leaderboard } from 'src/app/models/Users/Leaderboard';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { ContentService } from 'src/app/services/Content/Content/content.service';
import { UserService } from 'src/app/services/User/user-service';

@Component({
  selector: 'app-user-details',
  templateUrl: './user-details.component.html',
  styleUrls: ['./user-details.component.css']
})
export class UserDetailsComponent implements OnInit{
  userDetails: Leaderboard
  private activeDate?: string;
  userContent: CreatedContentDetails[] = []
  private isScrollHandlerActive = false;
  private lastVideo = false

  constructor(
    private route: ActivatedRoute,
    private userService: UserService,
    public alertService:AlertService,
    private contentService: ContentService
  ){
    this.userDetails = {
      username: '',
      rank: 1,
      totalHype: 0,
      videosPosted: 0
    }
  }
  
  ngOnInit(): void {
    const username: string = this.route.snapshot.params['username'];
    this.userService.getUserFromLeaderBoard(username).subscribe((user:any) => {
      this.userDetails = user
    })
    this.contentService.fetchAllCreatedContentFromUser(username, this.activeDate).subscribe({
      next: (data:any) => {
        this.userContent.push(...data);
        if (data.length == 50) {
          this.activeDate = data[data.length - 1].activeDate;
          this.isScrollHandlerActive = true
        }
        else{
          this.lastVideo = true
        }
      },
      error: () => {},
      complete: () => {}
    })
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
          const username: string = this.route.snapshot.params['username'];
          this.contentService.fetchAllCreatedContentFromUser(username, this.activeDate).subscribe({
            next: (data:any) => {
              this.userContent.push(...data);
              if (data.length == 50) {
                this.activeDate = data[data.length - 1].activeDate;
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
