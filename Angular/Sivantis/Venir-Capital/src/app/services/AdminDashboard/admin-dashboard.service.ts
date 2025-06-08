import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'src/Environment/environment';

@Injectable({
  providedIn: 'root'
})
export class AdminDashboardService {

  private SivantisURL:string = environment.BackendURL + "AdminDashboard";

  constructor(private httpClient: HttpClient) { }

  getYearChart(): Observable<any>{
    return this.httpClient.get(this.SivantisURL + `/Chart/Year`, {responseType: 'json'});
  }

  getMonthChart(): Observable<any>{
    return this.httpClient.get(this.SivantisURL + `/Chart/Month`, {responseType: 'json'});
  }

  getWeekChart(): Observable<any>{
    return this.httpClient.get(this.SivantisURL + `/Chart/Week`, {responseType: 'json'});
  }

  getChanges(): Observable<any>{
    return this.httpClient.get(this.SivantisURL + `/Changes`, {responseType: 'json'});
  }

  getTodos(): Observable<any>{
    return this.httpClient.get(this.SivantisURL + `/Todo`, {responseType: 'json'});
  }

  getLatestManaPrice(): Observable<any>{
    return this.httpClient.get(this.SivantisURL + `/Latest/ManaPrice`, {responseType: 'text'});
  }

  searchChannels(query: string): Observable<any> {
    return this.httpClient.get(`${this.SivantisURL}/Search/Channel?query=${query}`);
  }

  searchUser(query: string): Observable<any> {
    return this.httpClient.get(`${this.SivantisURL}/Search/User?query=${query}`);
  }
}
