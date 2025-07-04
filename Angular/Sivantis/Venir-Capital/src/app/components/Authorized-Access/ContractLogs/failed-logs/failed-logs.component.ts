import { Component, HostListener, OnInit } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ContractFunctionDetailsComponent } from 'src/app/components/Accessories/Popups/ContractFunctionDetails/contract-function-details/contract-function-details.component';
import { ContractLogs } from 'src/app/models/ContractLogs/contractlogs';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { ContractLogsService } from 'src/app/services/Contracts/Logs/contract-logs.service';

@Component({
    selector: 'app-failed-logs',
    templateUrl: './failed-logs.component.html',
    styleUrls: ['./failed-logs.component.css'],
    standalone: false
})
export class FailedLogsComponent implements OnInit{
  failedLogs: ContractLogs[] = [];
  lastCreationDate: string | undefined;
  lastLog = false

  constructor(
    private contractLogsService: ContractLogsService,
    private modalService: NgbModal, 
    public alertService: AlertService
  ) { }

  ngOnInit(): void {
    this.loadLatestLogs();
  }
  
  getDate(creationDate: number):Date {
    const timeStamp = creationDate * 1000
    return new Date(timeStamp);
  }

  openContractFunctionDetails(failedLog: ContractLogs){
      const modalRef = this.modalService.open(ContractFunctionDetailsComponent, { size: 'md', scrollable: true, centered: true , animation: false, })
      modalRef.componentInstance.failedLog = failedLog
      modalRef.result.then((result) => {
      if(result === "successful"){
        const indexToRemove = this.failedLogs.findIndex(log => log.logId === failedLog.logId);
    
        if (indexToRemove !== -1) {
          this.failedLogs.splice(indexToRemove, 1); // Remove 1 item at the specified index
        }
      
      }
    })
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