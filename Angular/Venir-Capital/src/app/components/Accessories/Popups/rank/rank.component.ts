import { Component, OnInit } from '@angular/core';
import { ContentService } from 'src/app/services/Content/Content/content.service';
import { UserService } from 'src/app/services/User/user-service';

@Component({
  selector: 'app-rank',
  templateUrl: './rank.component.html',
  styleUrls: ['./rank.component.css']
})
export class RankComponent implements OnInit{
  userRank = 1
  userHype = 0
  allActiveHype = 0
  basePay = 750
  romanNumerals = ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X', 'XI'];
  HypeCap = [1000 , 5000, 10000, 25000, 50000, 100000, 250000, 500000, 1000000, 999999999]
  Titles = ["Apprentice", "Sentinel", "Crusader", "Enforcer", "Templar", "Paladin", "Prodigy", "Titan", "Champion", "High King", "Emperor"]
  SivantisToken = "assets/SivantisToken.png"

  constructor(
    private contentService:ContentService,
    private userService:UserService,
  ){

  }
  
  ngOnInit(): void {
    const JWT = localStorage.getItem('token') || ''
    this.contentService.fetchAllUserActiveHype(JWT).subscribe(hype => {
      this.allActiveHype = hype
    })
    if(this.userService.getUserID() === ''){
      this.userService.userInfo$.subscribe(() => {
        this.userHype = this.userService.getUserHype();
        this.userRank = this.userService.getUserRank();
      })
    }
    else{
      this.userHype = this.userService.getUserHype();
      this.userRank = this.userService.getUserRank();
    }
  }



  getUserProgress(){
    const progress = this.userHype / this.rankProgression(this.userRank) * 100
    return progress;
  }

  getActiveProgress(){
    const progress = (this.getUserProgress() + this.allActiveHype)  / this.rankProgression(this.userRank) * 100
    return progress;
  }

  rankTitles(rank:number){
    return this.Titles[rank - 1];
  }

  romanNumeral(rank: number): string {
    return this.romanNumerals[rank - 1];
  }

  rankProgression(rank: number): number{
    if(rank == 11){
      return 999999999;
    }
    return this.HypeCap[rank - 1];
  }


}
