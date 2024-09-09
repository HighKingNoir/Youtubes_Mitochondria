import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from 'src/Environment/environment';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PaymentServiceService {
  
  private SivantisURL:string = environment.BackendURL + 'Payment'

  constructor(private httpClient: HttpClient) { }

  newPayment(PaymentRequestPayload: PaymentRequestPayload): Observable<any>{
    return this.httpClient.post(this.SivantisURL, PaymentRequestPayload, {responseType: 'text'});
  }

  updatePayment(PaymentRequestPayload: PaymentRequestPayload) : Observable<any>{
    return this.httpClient.put(this.SivantisURL + '/update', PaymentRequestPayload, {responseType: "text"});
  }
 
  refundPayment(RefundRequestPayload: RefundRequestPayload) : Observable<any>{
    return this.httpClient.patch(`${this.SivantisURL}/refund` , RefundRequestPayload , {responseType: "text"});
  }

  getUserPayment(JWT:string, contentID:string) : Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/${JWT}/${contentID}`, {responseType: "json"});
  }

  getChannelPayment(channelName:string, contentID:string) : Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/Channel/${channelName}/${contentID}`, {responseType: "json"});
  }


  getAllUserPayment(JWT:string) : Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/${JWT}` , {responseType: "json"});
  }

  // getAllChannelPayment(channelName:String) : Observable<any>{
  //   return this.httpClient.get(`${this.SivantisURL}/Channel/${channelName}` , {responseType: "json"});
  // }

  
}

export interface RefundRequestPayload{
  contentID: string;
  transactionHash: string;
}

export interface PaymentRequestPayload{
  contentID: string
  manaAmount: string
  dollarAmount: number
  transactionHash: string;
}