import { HttpErrorResponse } from '@angular/common/http';
import { Component, Input } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ContractLogs } from 'src/app/models/ContractLogs/contractlogs';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { ContractLogsService } from 'src/app/services/Contracts/Logs/contract-logs.service';

@Component({
  selector: 'app-contract-function-details',
  templateUrl: './contract-function-details.component.html',
  styleUrl: './contract-function-details.component.css',
  standalone: false
})
export class ContractFunctionDetailsComponent {

  @Input()failedLog!: ContractLogs;

  constructor(
    public activeModal: NgbActiveModal,
    private contractLogService: ContractLogsService,
    private alertService: AlertService
  ){
    
  }

   resolveLog(){
    if(this.failedLog.contractEnum === 'MultiCall' || this.failedLog.contractEnum === 'ClientMultiCall'){
      this.contractLogService.resolveMultiFunctionContactLog(this.failedLog.logId).subscribe({
        next: (result) => {
          this.alertService.addAlert(result, "success")
          this.activeModal.close("successful")
        },
        error: (err:HttpErrorResponse) => {
          this.alertService.addAlert(err.message, "danger")
        },
        complete: () => {}
      }); 
    }
    else{
      this.contractLogService.resolveSingleFunctionContactLog(this.failedLog.logId).subscribe({
        next: (result) => {
          this.alertService.addAlert(result, "success")
          this.activeModal.close("successful")
        },
        error: (err:HttpErrorResponse) => {
          this.alertService.addAlert(err.message, "danger")
        },
        complete: () => {}
      }); 
    }
  }

  splitLog(){
    this.contractLogService.splitMultiCall(this.failedLog.logId).subscribe({
      next: (result) => {
        this.alertService.addAlert(result, "success")
        this.activeModal.close("successful")
      },
      error: (err:HttpErrorResponse) => {
        this.alertService.addAlert(err.message, "danger")
      },
      complete: () => {}
    }); 
  }

  resolveOnChain(){
    this.contractLogService.resolveContactLogOnChain(this.failedLog.logId).subscribe({
      next: (result) => {
        this.alertService.addAlert(result, "success")
        this.activeModal.close("successful")
      },
      error: (err:HttpErrorResponse) => {
        this.alertService.addAlert(err.message, "danger")
      },
      complete: () => {}
    }); 
  }

}
