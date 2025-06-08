import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Chart } from 'chart.js';
import { Subscription, interval, switchMap } from 'rxjs';
import { environment } from 'src/Environment/environment';
import { AdminDashboardChangesResponse } from 'src/app/models/AdminDashboard/AdminDashboardChangesResponse';
import { AdminDashboardChartInfo } from 'src/app/models/AdminDashboard/AdminDashboardChartInfo';
import { AdminDashboardToDoResponse } from 'src/app/models/AdminDashboard/AdminDashboardToDoResponse';
import { Channels } from 'src/app/models/Channels/Channels';
import { User } from 'src/app/models/Users/User';
import { AdminDashboardService } from 'src/app/services/AdminDashboard/admin-dashboard.service';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { ConnectWalletService } from 'src/app/services/Mana/ConnectPersonalWallet/connect-wallet.service';
import { UserService } from 'src/app/services/User/user-service';

@Component({
    selector: 'app-admin-dashboard',
    templateUrl: './admin-dashboard.component.html',
    styleUrls: ['./admin-dashboard.component.css'],
    standalone: false
})
export class AdminDashboardComponent implements OnInit, OnDestroy {

  private CompanyWallet: string
  private TrustWallet: string 
  private MetaMaskWallet: string 
  private CoinBaseWallet: string 
  private ExodusWallet: string 
  private EnkryptWallet: string 
  private CryptoDotcomWallet: string

  CompanyWalletbalance = 0
  TrustWalletbalance = 0
  CoinBaseWalletbalance = 0
  MetaMaskWalletbalance = 0
  ExodusWalletbalance = 0
  Enkryptbalance = 0
  CryptoDotcomWalletbalance = 0

  decentralandLogo = 'assets/decentraland-mana-logo.png'
  polygonLogo = "assets/matic-logo.png"
  activeDuration = 'year';
  MANA_PRICE = 0;
  countdown = 60
  private intervalSubscription?: Subscription;
  connectedWallet?: string;

  labelData: string[] = []
  manaValueData: number[] = []
  dollarValueData: number[] = []
  expenseData: number[] = []
  dashboardChartInfo: AdminDashboardChartInfo[] = []

  manaChange: number[] = []
  userChange: number[] = []
  channelChange: number[] = []
  videoChange: number[] = []
  transactionChange: number[] = []

  failedLogCount = 0
  channelRequestCount = 0
  reportedContentCount = 0

  channelSearchForm: FormGroup;
  channelSearchResults: Channels[] = [];
  userSearchForm: FormGroup;
  userSearchResults: User[] = [];
  
  channelRequest = 0
  pendingLogs = 0
  failedLogs = 0

  chart?: Chart;
  
  // New properties for wallet toggling
  firstStageWallets: any[] = [];
  secondStageWallets: any[] = [];
  thirdStageWallets: any[] = [];
  currentIndex = 0;
  private toggleInterval?: any;

  constructor(
    public alertService:AlertService,
    private adminDashboardService:AdminDashboardService,
    private connectWalletService: ConnectWalletService,
    private userService:UserService,
    private formBuilder: FormBuilder,
  ){
    this.channelSearchForm = this.formBuilder.group({
      query: ['']
    });

    this.userSearchForm = this.formBuilder.group({
      query: ['']
    });
    
    if(environment.production){
      this.CompanyWallet = "0x5dd76AfEF498Fab393dC0115346B201033F6547C"
      this.TrustWallet = "0x804Fed826b3eFA941aeF994a523330934B8f2089"
      this.MetaMaskWallet = "0x5d496e6115F4d1f25F0304F43e37268e425fF2A8"
      this.CoinBaseWallet = "0xb44cb44F584FC22981980008d3a7e2b73b7949c0"
      this.ExodusWallet = "0x0c4E319bD9F0Fb1E41D5F954C9d8ad0ED4f408E9"
      this.EnkryptWallet = "0x7DbA5a5ECe2e99097C69F8f0FdAc8ab7E4260F7d"
      this.CryptoDotcomWallet = "0x1bA5DA7B6a3A6294b6336b79bCF80fE886CdCbc1"
    }
    else{
      this.CompanyWallet = "0xDfCf5fE5509Ad56f72421db42f7F66290E21381F"
      this.TrustWallet = "0x3a609Dc5B328843bad410CE519B2E511708d4d15"
      this.MetaMaskWallet = "0x21b43070CeeEA01d1e85b87D63F858aEB346f0c3"
      this.CoinBaseWallet = "0xDF140488Ec2B3e02500CfA0F70046204826b1fC3"
      this.ExodusWallet = "0x184E563e8cBFF98982740F34A91957f7CA723DaE"
      this.EnkryptWallet = "0x995330F24C0F3B1E7De86552C391F0918Ee2df44"
      this.CryptoDotcomWallet = "0x5A6Cd620a2416c7e04c2E13A8C996BceB342C915"
    }

    // Initialize the wallets array
    this.firstStageWallets = [
      {
        header: 'Trust',
        balance: this.TrustWalletbalance,
        balanceKey: 'TrustWalletbalance'
      },
      {
        header: 'CoinBase',
        balance: this.CoinBaseWalletbalance,
        balanceKey: 'CoinBaseWalletaBlence'
      }
    ];
    this.secondStageWallets = [
      {
        header: 'MetaMask ',
        balance: this.MetaMaskWalletbalance,
        balanceKey: 'MetaMaskWalletbalance'
      },
      {
        header: 'Exodius',
        balance: this.ExodusWalletbalance,
        balanceKey: 'ExodusWalletbalance'
      }
    ];
    this.thirdStageWallets = [
      {
        header: 'Enkrypt',
        balance: this.Enkryptbalance,
        balanceKey: 'Enkryptbalance'
      },
      {
        header: 'Crypto.com',
        balance: this.CryptoDotcomWalletbalance,
        balanceKey: 'CryptoDotcomWalletbalance'
      }
    ];
  }

  ngOnInit(): void {
    if(this.userService.getUserID() === ''){
      this.userService.userInfo$.subscribe(() => {
        this.initialization()
      })
    }
    else{
      this.initialization()
    }

    // Start the wallet toggling interval
    this.toggleInterval = setInterval(() => {
      this.currentIndex = (this.currentIndex + 1) % 2;
    }, 5000);
  }

  initialization(){
    this.adminDashboardService.getLatestManaPrice().subscribe(price => {
      this.MANA_PRICE = Number(price)
      this.getChanges()
    });

    setInterval(() => {
      this.updateCountdown();
    }, 1000);

    this.intervalSubscription = interval(60000) 
      .pipe(
        switchMap(() => this.adminDashboardService.getLatestManaPrice())
      )
      .subscribe(price => {
        this.MANA_PRICE = Number(price);
        this.getChanges(); 
        this.countdown = 60;
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
    if (this.toggleInterval) {
      clearInterval(this.toggleInterval);
    }
  }

  searchChannels() {
    const query = this.channelSearchForm.get('query')?.value;
    if(query){
      this.adminDashboardService.searchChannels(query).subscribe((results) => {
        this.channelSearchResults = results;
      });
    }
    
  }

  searchUsers() {
    const query = this.userSearchForm.get('query')?.value;
    if(query){
      this.adminDashboardService.searchUser(query).subscribe((results) => {
        this.userSearchResults = results;
      });
    }
    
  }

  private getChanges() {
    this.adminDashboardService.getChanges().subscribe((changes:AdminDashboardChangesResponse) => {
      this.manaChange = changes.manaChange;
      this.userChange = changes.userChange
      this.videoChange = changes.videoChange;
      this.channelChange = changes.channelChange;
      this.transactionChange = changes.transactionChange
    });

    this.adminDashboardService.getTodos().subscribe((todos:AdminDashboardToDoResponse) => {
      this.failedLogCount = todos.failedLogCount
      this.channelRequestCount = todos.channelRequestCount
      this.reportedContentCount = todos.reportedContentCount
    })

    this.updatebalances();

    this.updateChart(this.activeDuration)
  }

  updatebalances(){
      this.connectWalletService.accountManabalance(this.CompanyWallet).then(balance => {
        this.CompanyWalletbalance = balance;
      })
      this.connectWalletService.accountMaticbalance(this.TrustWallet).then(balance => {
        this.TrustWalletbalance = balance;
        this.firstStageWallets[0].balance = balance;
      })
      this.connectWalletService.accountMaticbalance(this.CoinBaseWallet).then(balance => {
        this.CoinBaseWalletbalance = balance
        this.firstStageWallets[1].balance = balance;
      })
      this.connectWalletService.accountMaticbalance(this.MetaMaskWallet).then(balance => {
        this.MetaMaskWalletbalance = balance;
        this.secondStageWallets[0].balance = balance;
      })
      this.connectWalletService.accountMaticbalance(this.ExodusWallet).then(balance => {
        this.ExodusWalletbalance = balance
        this.secondStageWallets[1].balance = balance;
      })
      this.connectWalletService.accountMaticbalance(this.EnkryptWallet).then(balance => {
        this.Enkryptbalance = balance
        this.thirdStageWallets[0].balance = balance;
      })
      this.connectWalletService.accountMaticbalance(this.CryptoDotcomWallet).then(balance => {
        this.CryptoDotcomWalletbalance = balance
        this.thirdStageWallets[1].balance = balance;
      })
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
    this.adminDashboardService.getYearChart().subscribe(chartInfo => {
      this.dashboardChartInfo = chartInfo
      this.labelData = []
      this.manaValueData = []
      this.dollarValueData = []
      this.expenseData = []
      for(let index = 0; index < this.dashboardChartInfo.length; index++){
        this.labelData.push(this.dashboardChartInfo[index].label)
        this.manaValueData.push(this.dashboardChartInfo[index].manaValue)
        this.dollarValueData.push(this.dashboardChartInfo[index].manaValue * this.MANA_PRICE)
        this.expenseData.push(this.dashboardChartInfo[index].expenses)
      }
      this.activeDuration = "year"
      this.renderChart(this.labelData, this.manaValueData, this.dollarValueData, this.expenseData)
    })
  }

  monthChart(){
    this.adminDashboardService.getMonthChart().subscribe(chartInfo => {
      this.dashboardChartInfo = chartInfo
      this.labelData = []
      this.manaValueData = []
      this.dollarValueData = []
      this.expenseData = []
      for(let index = 0; index < this.dashboardChartInfo.length; index++){
        this.labelData.push(this.dashboardChartInfo[index].label)
        this.manaValueData.push(this.dashboardChartInfo[index].manaValue)
        this.dollarValueData.push(this.dashboardChartInfo[index].manaValue * this.MANA_PRICE)
        this.expenseData.push(this.dashboardChartInfo[index].expenses)
      }
      this.activeDuration = "month"
      this.renderChart(this.labelData, this.manaValueData, this.dollarValueData, this.expenseData)
    })
  }

  weekChart(){
    this.adminDashboardService.getWeekChart().subscribe(chartInfo => {
      this.dashboardChartInfo = chartInfo
      this.labelData = []
      this.manaValueData = []
      this.dollarValueData = []
      this.expenseData = []
      for(let index = 0; index < this.dashboardChartInfo.length; index++){
        this.labelData.push(this.dashboardChartInfo[index].label)
        this.manaValueData.push(this.dashboardChartInfo[index].manaValue)
        this.dollarValueData.push(this.dashboardChartInfo[index].manaValue * this.MANA_PRICE)
        this.expenseData.push(this.dashboardChartInfo[index].expenses)
      }
      this.activeDuration = "week"
      this.renderChart(this.labelData, this.manaValueData, this.dollarValueData, this.expenseData)
    })
  }


  renderChart(labelData: string[], manaValueData: number[], dollarValueData: number[], expenseData: number[]) {
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
        },
        {
          label: 'Matic Expenses',
          data: expenseData,
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

  connectWallet(){
    this.connectWalletService.connectAccount().then(() => {
      this.connectedWallet = this.connectWalletService.getConnectedAccount();
      this.updatebalances()
    });
  }


  formateTotals(value: number): string {
    if(value){
      if(value >= 1000000000){
      // Convert to billion format (X.XB)
      return (value / 1000000000).toFixed(2) + 'B';
      }
      else if (value >= 1000000) {
        // Convert to million format (X.XM)
        return (value / 1000000).toFixed(2) + 'M';
      } else if (value >= 1000) {
        // Convert to thousand format (X.XK)
        return (value / 1000).toFixed(2) + 'K';
      } 
        return value.toFixed(0)
    }
    return Number(0).toFixed(0)
  }

}
