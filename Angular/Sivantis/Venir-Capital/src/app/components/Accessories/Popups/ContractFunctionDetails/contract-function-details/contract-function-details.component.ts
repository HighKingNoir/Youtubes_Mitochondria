import { Component, Input } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ContractLogs } from 'src/app/models/ContractLogs/contractlogs';

@Component({
  selector: 'app-contract-function-details',
  templateUrl: './contract-function-details.component.html',
  styleUrl: './contract-function-details.component.css',
  standalone: false
})
export class ContractFunctionDetailsComponent {

  @Input()failedLog!: ContractLogs;

  constructor(public activeModal: NgbActiveModal,){
    console.log(this.failedLog)
  }

}
