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

  getAllUnresolvedLogs(creationDate?: string): Observable<any>{
    if(creationDate){
      return this.httpClient.get(`${this.SivantisBackendURL}/View/Unresolved`, { params: { creationDate }, responseType: 'json' });
    }
    return this.httpClient.get(`${this.SivantisBackendURL}/View/Unresolved`, { responseType: 'json' });
  }
  
  getLatestLogs(creationDate?: string) {
    if(creationDate){
      return this.httpClient.get(`${this.SivantisBackendURL}/View/Latest`, { params: { creationDate }, responseType: 'json' });
    }
    return this.httpClient.get(`${this.SivantisBackendURL}/View/Latest`, { responseType: 'json' });
  }

  resolveLog(logID: string) {
    return this.httpClient.put(`${this.SivantisBackendURL}/Resolve?logID=${logID}`, null, { responseType: 'json' });
  }

  
}
