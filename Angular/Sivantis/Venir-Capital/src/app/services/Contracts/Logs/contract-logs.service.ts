import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'src/Environment/environment';
import { ContractLogs } from 'src/app/models/ContractLogs/contractlogs';

@Injectable({
  providedIn: 'root'
})
export class ContractLogsService {

  ContractLogs!: ContractLogs[];
  SivantisBackendURL:string = environment.BackendURL! + "ContractLogs";

  constructor(private httpClient: HttpClient) { }

 
  getAllFailedLogs(creationDate?: string): Observable<any>{
    if(creationDate){
      return this.httpClient.get(`${this.SivantisBackendURL}/View/Failed`, { params: { creationDate }, responseType: 'json' });
    }
    return this.httpClient.get(`${this.SivantisBackendURL}/View/Failed`, { responseType: 'json' });
  }


  
  getLatestLogs(creationDate?: string) {
    if(creationDate){
      return this.httpClient.get(`${this.SivantisBackendURL}/View/Latest`, { params: { creationDate }, responseType: 'json' });
    }
    return this.httpClient.get(`${this.SivantisBackendURL}/View/Latest`, { responseType: 'json' });
  }

  resolveSingleFunctionContactLog(logID: string) {
    return this.httpClient.put(`${this.SivantisBackendURL}/Resolve/Single?logID=${logID}`, null, { responseType: 'text' });
  }

  resolveMultiFunctionContactLog(logID: string) {
    return this.httpClient.put(`${this.SivantisBackendURL}/Resolve/Multi?logID=${logID}`, null, { responseType: 'text' });
  }


  splitMultiCall(logID: string) {
    return this.httpClient.put(`${this.SivantisBackendURL}/Resolve/Split?logID=${logID}`, null, { responseType: 'text' });
  }

  resolveContactLogOnChain(logID: string) {
    return this.httpClient.put(`${this.SivantisBackendURL}/Resolve/OnChain?logID=${logID}`, null, { responseType: 'text' });
  }

  
}
