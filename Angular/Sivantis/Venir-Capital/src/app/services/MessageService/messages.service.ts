import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { environment } from 'src/Environment/environment';
import { Messages } from 'src/app/models/Messages/messages';

@Injectable({
  providedIn: 'root'
})
export class MessagesService {
  private SivantisBackendURL:string = environment.BackendURL + 'Message';

  private userProfileSubject = new Subject<Messages>()
  userProfile$ = this.userProfileSubject.asObservable();

  constructor(private httpClient: HttpClient) {
  }

  setMessage(newMessage:Messages){
    this.userProfileSubject.next(newMessage)
  }





 
  getAllMessages(JWT: string, start: number): Observable<any>{
    return this.httpClient.get(`${this.SivantisBackendURL}/All`, { params: { JWT, start }, responseType: 'json'});
  }

  getAllUnreadMessages(JWT: string): Observable<any>{
    return this.httpClient.get(`${this.SivantisBackendURL}/${JWT}/Unread`, {responseType: 'json'});
  }

  getMessageFromServer(messageID: string): Observable<any>{
    return this.httpClient.get(`${this.SivantisBackendURL}/View/${messageID}`, {responseType: 'json'});
  }

  markAsRead(messageId: string): Observable<any>{
    return this.httpClient.patch(`${this.SivantisBackendURL}/Read/${messageId}`, {responseType: 'text'});
  }

  fundChannelMessage(fundChannelPayload: FundChannelPayload): Observable<any>{
    return this.httpClient.post(`${this.SivantisBackendURL}/FundChannel`, fundChannelPayload, {responseType: 'text'});
  }

  userWithdrawMessage(userWithdrawRequestPayload: UserWithdrawRequestPayload): Observable<any>{
    return this.httpClient.post(`${this.SivantisBackendURL}/User/Withdraw`, userWithdrawRequestPayload, {responseType: 'text'});
  }
}

export interface FundChannelPayload{
  channelName: string,
  manaAmount: string,
  transactionHash: string,
}

export interface UserWithdrawRequestPayload{
  dollarAmount: number,
  transactionHash: string,
}

