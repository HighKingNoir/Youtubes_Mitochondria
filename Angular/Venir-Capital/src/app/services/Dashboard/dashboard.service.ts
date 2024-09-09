import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'src/Environment/environment';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private SivantisURL:string = environment.BackendURL + "Dashboard";

  constructor(private httpClient: HttpClient) { }

  //Creates content that has already been uploaded and has a url (Params: ContentRequestPayload)
  getYearChart(): Observable<any>{
    return this.httpClient.get(this.SivantisURL + `/Chart/Year`, {responseType: 'json'});
  }

  //Creates content that has already been uploaded and has a url (Params: ContentRequestPayload)
  getMonthChart(): Observable<any>{
    return this.httpClient.get(this.SivantisURL + `/Chart/Month`, {responseType: 'json'});
  }

  //Creates content that has already been uploaded and has a url (Params: ContentRequestPayload)
  getWeekChart(): Observable<any>{
    return this.httpClient.get(this.SivantisURL + `/Chart/Week`, {responseType: 'json'});
  }

  getChanges(): Observable<any>{
    return this.httpClient.get(this.SivantisURL + `/Changes`, {responseType: 'json'});
  }

  getTopVideos(): Observable<any>{
    return this.httpClient.get(this.SivantisURL + `/Video/Top`, {responseType: 'json'});
  }

  getUpcomingReleases(): Observable<any>{
    return this.httpClient.get(this.SivantisURL + `/Video/Upcoming`, {responseType: 'json'});
  }

  getLatestManaPrice(): Observable<any>{
    return this.httpClient.get(this.SivantisURL + `/Latest/ManaPrice`, {responseType: 'text'});
  }

}
