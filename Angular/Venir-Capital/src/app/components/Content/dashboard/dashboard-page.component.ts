import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Chart, registerables } from 'chart.js';
import { Subscription, interval, switchMap } from 'rxjs';
import { CreatedContentDetails } from 'src/app/models/Content/CreatedContentDetails';
import { DashboardChangesResponse } from 'src/app/models/Dashboard/DashboardChangesResponse';
import { DashboardChartInfo } from 'src/app/models/Dashboard/DashboardChartInfo';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { DashboardService } from 'src/app/services/Dashboard/dashboard.service';
import { UserService } from 'src/app/services/User/user-service';
Chart.register(...registerables)

@Component({
  selector: 'app-dashboard-page',
  templateUrl: './dashboard-page.component.html',
  styleUrls: ['./dashboard-page.component.css']
})
export class DashboardPageComponent implements OnInit, OnDestroy {

  decentralandLogo = 'assets/decentraland-mana-logo.png'
  SivantisToken = "assets/SivantisToken.png"
  activeDuration = 'year';
  Titles = ["Apprentice", "Sentinel", "Crusader", "Enforcer", "Templar", "Paladin", "Prodigy", "Titan", "Champion", "High King", "Emperor"]
  HypeCap = [1000 , 5000, 10000, 25000, 50000, 100000, 250000, 500000, 1000000, 999999999]
  MANA_PRICE = 0;
  countdown = 180
  private intervalSubscription?: Subscription;

  userRankData: number[] = []
  labelData: string[] = []
  manaValueData: number[] = []
  dollarValueData: number[] = []
  dashboardChartInfo: DashboardChartInfo[] = []

  manaChange: number[] = [0]
  hypeChange: number[] = [0]
  videoChange: number[] = []

  topVideos: CreatedContentDetails[] = []
  upcomingVidoes: CreatedContentDetails[] = []

  chart?: Chart;
  

  constructor(
    private route: ActivatedRoute,
    public alertService:AlertService,
    private dashboardService:DashboardService,
    private userService:UserService,
  ){
    
  }

  ngOnInit(): void {
    this.dashboardService.getLatestManaPrice().subscribe(price => {
      this.MANA_PRICE = Number(price)
      this.getChanges()

      this.dashboardService.getTopVideos().subscribe(topVideos => {
        this.topVideos = topVideos
      });
  
      this.dashboardService.getUpcomingReleases().subscribe(upcomingReleases => {
        this.upcomingVidoes = upcomingReleases
      });

    });

    

    setInterval(() => {
      this.updateCountdown();
    }, 1000);

    this.intervalSubscription = interval(180000) 
      .pipe(
        switchMap(() => this.dashboardService.getLatestManaPrice())
      )
      .subscribe(price => {
        this.MANA_PRICE = Number(price);
        this.getChanges(); 
        this.countdown = 180;
      });
  }
  

  updateCountdown() {
    if (this.countdown > 0) {
      this.countdown--;
    }
  }

  ngOnDestroy(): void {
    if (this.intervalSubscription) {
      this.intervalSubscription.unsubscribe();
    }
  }

  private getChanges() {
    this.dashboardService.getChanges().subscribe((changes:DashboardChangesResponse) => {
      this.manaChange = changes.manaChange;
      this.hypeChange = changes.hypeChange;
      this.videoChange = changes.videoChange;
      this.userRankData = changes.rankChange;
    });

    this.updateChart(this.activeDuration)
  }


  updateChart(activeDuration: string){
    switch(activeDuration){
      case 'year': 
        this.yearChart()
      break;
      case 'month': 
        this.monthChart()
      break;
      case 'week': 
        this.weekChart()
      break;
    }
  }

  yearChart(){
    this.dashboardService.getYearChart().subscribe(chartInfo => {
      this.dashboardChartInfo = chartInfo
      this.labelData = []
      this.manaValueData = []
      this.dollarValueData = []
      for(let index = 0; index < this.dashboardChartInfo.length; index++){
        this.labelData.push(this.dashboardChartInfo[index].label)
        this.manaValueData.push(this.dashboardChartInfo[index].value)
        this.dollarValueData.push(this.dashboardChartInfo[index].value * this.MANA_PRICE)
      }
      this.activeDuration = "year"
      this.renderChart(this.labelData, this.manaValueData, this.dollarValueData)
    })
  }

  monthChart(){
    this.dashboardService.getMonthChart().subscribe(chartInfo => {
      this.dashboardChartInfo = chartInfo
      this.labelData = []
      this.manaValueData = []
      this.dollarValueData = []
      for(let index = 0; index < this.dashboardChartInfo.length; index++){
        this.labelData.push(this.dashboardChartInfo[index].label)
        this.manaValueData.push(this.dashboardChartInfo[index].value)
        this.dollarValueData.push(this.dashboardChartInfo[index].value * this.MANA_PRICE)
      }
      this.activeDuration = "month"
      this.renderChart(this.labelData, this.manaValueData, this.dollarValueData)
    })
  }

  weekChart(){
    this.dashboardService.getWeekChart().subscribe(chartInfo => {
      this.dashboardChartInfo = chartInfo
      this.labelData = []
      this.manaValueData = []
      this.dollarValueData = []
      for(let index = 0; index < this.dashboardChartInfo.length; index++){
        this.labelData.push(this.dashboardChartInfo[index].label)
        this.manaValueData.push(this.dashboardChartInfo[index].value)
        this.dollarValueData.push(this.dashboardChartInfo[index].value * this.MANA_PRICE)
      }
      this.activeDuration = "week"
      this.renderChart(this.labelData, this.manaValueData, this.dollarValueData)
    })
  }


  renderChart(labelData: string[], manaValueData: number[], dollarValueData: number[]) {
    if(this.chart){
      this.chart.destroy()
    }
    this.chart = new Chart("canvas", {
      type: 'line',
      data: {
        labels: labelData,
        datasets: [{
          label: 'Mana',
          data: manaValueData,
          borderWidth: 4,
          tension: 0.4,
        },
        {
          label: 'Dollar',
          data: dollarValueData,
          borderWidth: 4,
          tension: 0.4,
        }
      ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true
          }
        },
        plugins: {
          tooltip: {
            callbacks: {
              label: function (context) {
                return context.dataset.label + ' : ' + context.parsed.y;
              }
            }
          }
        }
      }
    });
  }

}
