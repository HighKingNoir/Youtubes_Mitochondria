import { Component, HostListener, OnInit } from '@angular/core';
import { ContractLogs } from 'src/app/models/ContractLogs/contractlogs';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ContractLogsService } from 'src/app/services/Contracts/Logs/contract-logs.service';

@Component({
  selector: 'app-unresolved-logs',
  templateUrl: './unresolved-logs.component.html',
  styleUrls: ['./unresolved-logs.component.css']
})
export class UnresolvedLogsComponent implements OnInit{
  UnresolvedLogs: ContractLogs[] = [];
  lastCreationDate: string | undefined;
  lastLog = false

  constructor(
    private contractLogsService: ContractLogsService,
    private authService: AuthenticationService,
    public alertService: AlertService
  ) { }

  ngOnInit(): void {
    this.loadLatestLogs();
  }
  
  getDate(creationDate: number):Date {
    const timeStamp = creationDate * 1000
    return new Date(timeStamp);
  }

  resolveLog(logID: string){
    const JWT: string = window.localStorage.getItem('token') || '';
    this.authService.checkJWTExpiration(JWT).then(() => {
      this.contractLogsService.resolveLog(logID).subscribe({
        next: () => {
          const indexToDelete = this.UnresolvedLogs.findIndex(
            (v) => v.logId === logID
          );
          if (indexToDelete !== -1) {
            this.UnresolvedLogs.splice(indexToDelete, 1);
          }
        },
        error: (err) => {
          this.alertService.addAlert(err, "danger")
        },
        complete: () => {}
      });
    });
  }

  loadLatestLogs() {
    if(!this.lastLog){
      this.contractLogsService.getAllUnresolvedLogs(this.lastCreationDate).subscribe({
        next: (data:any) => {
          this.UnresolvedLogs.push(...data);
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