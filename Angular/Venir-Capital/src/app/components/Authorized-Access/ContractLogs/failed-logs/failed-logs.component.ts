import { Component, HostListener, OnInit } from '@angular/core';
import { ContractLogs } from 'src/app/models/ContractLogs/contractlogs';
import { ContractLogsService } from 'src/app/services/Contracts/Logs/contract-logs.service';

@Component({
  selector: 'app-failed-logs',
  templateUrl: './failed-logs.component.html',
  styleUrls: ['./failed-logs.component.css']
})
export class FailedLogsComponent implements OnInit{
  failedLogs: ContractLogs[] = [];
  lastCreationDate: string | undefined;
  lastLog = false

  constructor(private contractLogsService: ContractLogsService) { }

  ngOnInit(): void {
    this.loadLatestLogs();
  }
  
  getDate(creationDate: number):Date {
    const timeStamp = creationDate * 1000
    return new Date(timeStamp);
  }

  loadLatestLogs() {
    if(!this.lastLog){
      this.contractLogsService.getAllFailedLogs(this.lastCreationDate).subscribe({
        next: (data:any) => {
          this.failedLogs.push(...data);
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