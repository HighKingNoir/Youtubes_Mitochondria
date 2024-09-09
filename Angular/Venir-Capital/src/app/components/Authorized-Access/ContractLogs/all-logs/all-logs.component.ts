import { Component, HostListener, OnInit } from '@angular/core';
import { ContractLogs } from 'src/app/models/ContractLogs/contractlogs';
import { ContractLogsService } from 'src/app/services/Contracts/Logs/contract-logs.service';
import { UserService } from 'src/app/services/User/user-service';

@Component({
  selector: 'app-all-logs',
  templateUrl: './all-logs.component.html',
  styleUrls: ['./all-logs.component.css']
})
export class AllLogsComponent implements OnInit{
  allLogs: ContractLogs[] = [];
  lastCreationDate?: string;
  lastLog = false

  constructor(
    private contractLogsService: ContractLogsService,
  ) {
   }

  ngOnInit(): void {
    this.loadLatestLogs();
  }


  
  getDate(creationDate: number):Date {
    const timeStamp = creationDate * 1000
    return new Date(timeStamp);
  }
  

  loadLatestLogs() {
    if(!this.lastLog){
      this.contractLogsService.getLatestLogs(this.lastCreationDate).subscribe({
        next: (data:any) => {
          this.allLogs.push(...data);
          if (data.length == 50) {
            this.lastCreationDate = data[data.length - 1].creationDate;
          }
          else{
            this.lastLog = true
          }
        },
        error: (err) => {

        },
        complete: () => {}
      });
    }
    
  }

  getStatusClass(status:string): string{
    switch (status) {
      case 'Completed':
        return 'complete';
      default:
        return 'failed';
    }
  }

  @HostListener('window:scroll', ['$event'])
  onScroll(event: any) {
    const windowHeight = 'innerHeight' in window ? window.innerHeight : document.documentElement.offsetHeight;
    const body = document.body, html = document.documentElement;
    const docHeight = Math.max(body.scrollHeight, body.offsetHeight, html.clientHeight, html.scrollHeight, html.offsetHeight);
    const windowBottom = window.scrollY + windowHeight; // Calculate window bottom correctly

    if (windowBottom >= docHeight - 50) {
      this.loadLatestLogs();
    }
  }


}
