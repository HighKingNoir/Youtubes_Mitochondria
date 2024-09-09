import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, Subject, catchError, debounceTime, map, of, switchMap } from 'rxjs';
import { environment } from 'src/Environment/environment';
import { Channels, StreamerInfo } from 'src/app/models/Channels/Channels';
import { AuthenticationService } from '../Auth/authentication.service';
import { AsyncValidatorFn, AbstractControl, ValidationErrors } from '@angular/forms';
import { HomeSidebarComponent } from 'src/app/components/Accessories/Layout/home-sidebar/home-sidebar.component';

@Injectable({
  providedIn: 'root'
})
export class ChannelService {
  private channelManaBalance?: number
  private maxNumberOfChannels = 1
  private currentChannelAWV = 0
  readonly minAWV: number = 1000;
  private SivantisURL:string = environment.BackendURL + "Channel";

  private channelSubject = new Subject<Channels>()
  channel$ = this.channelSubject.asObservable();

  constructor(
    private httpClient: HttpClient,
    private authService:AuthenticationService
  ) { }


  setChannel(Channel: Channels){
    this.channelSubject.next(Channel);
  }

  getChannelManaBalance(){
    return this.channelManaBalance
  }

  setChannelManaBalance(manabalance: number | undefined){
    return this.channelManaBalance = manabalance
  }

  getMaxNumberOfChannels(){
    return this.maxNumberOfChannels
  }

  getMinAWV(){
    return this.minAWV
  }

  getChannelAWV(){
    return this.currentChannelAWV
  }

  setChannelAWV(CurrentChannelAWV: number){
    return this.currentChannelAWV = CurrentChannelAWV
  }

  newChannel(channelRequestPayload: ChannelRequestPayload): Observable<any>{
    return this.httpClient.post(this.SivantisURL + '/NewChannel', channelRequestPayload, {responseType: 'json'});
  }

  resubmitChannel(resubmitChannelRequestPayload: ResubmitChannelRequestPayload): Observable<any>{
    return this.httpClient.put(this.SivantisURL + '/Resubmit', resubmitChannelRequestPayload, {responseType: 'text'});
  }



  approveChannel(approveChannel: ChannelStreamerInfoRequestPayload): Observable<any>{
    return this.httpClient.put(this.SivantisURL + '/Request/Approve',approveChannel, {responseType: 'text'});
  }

  validateChannelNameAvailability(): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> => {
      // Debounce user input for 300 milliseconds to reduce API requests
      return control.valueChanges.pipe(
        debounceTime(500),
        switchMap((channelName: string) => {
          if (!channelName || channelName === '') {
            // Username is empty; no need to make a request
            return of(null);
          }
          // Make an HTTP request to check Channel Name availability
          return this.httpClient.get(`${this.SivantisURL}/Exist/Channel/${channelName}`).pipe(
            map((notAvailable) => {
              if (notAvailable) {
                control.setErrors({ channelNameTaken: true })
                return of({ channelNameTaken: true }); // Channel Name is already taken
              } else {
                return of(null); // channelName is availabe
              }
            }),
            catchError(() => of(null)) // Handle errors gracefully
          );
        })
      );
    };
  }

  editChannel(editChannelRequestPayload: EditChannelRequestPayload){
    return this.httpClient.put(`${this.SivantisURL}/Edit`, editChannelRequestPayload ,{responseType: 'json'})
  }

  channelExist(platform: string, streamerID: string){
    return this.httpClient.get(`${this.SivantisURL}/Exist/Channel/${platform}/${streamerID}`, {responseType: 'text'})
  }

  disapproveChannel(disapproveChannelResponsePayload: DisapproveChannelResponsePayload): Observable<any>{
    return this.httpClient.put(this.SivantisURL + '/Request/Disapprove', disapproveChannelResponsePayload, {responseType: 'text'});
  }

  getChannelRequest(resubmissionDate?: string): Observable<any>{
    if(resubmissionDate){
      return this.httpClient.get(`${this.SivantisURL}/Request`, { params: { resubmissionDate }, responseType: 'json' });
    }
    return this.httpClient.get(`${this.SivantisURL}/Request`, { responseType: 'json' });
  }

  getChannelNeedingUpdate(start: number): Observable<any>{
    return this.httpClient.get(this.SivantisURL + '/AWV/Update', { params: { start },responseType: 'json'});
  }

  getAllUserChannels(): Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/User`, {responseType: 'json'});
  }

  getAllActiveChannels(): Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/Active`, {responseType: 'json'});
  }

  getRandomActiveChannels(excludedChannelIds: string[] | undefined): Observable<any> {
    const parameters = excludedChannelIds ? { excludedChannelIds: excludedChannelIds.join(',') } : {};
    return this.httpClient.get(`${this.SivantisURL}/Active/Random`, { params: parameters as any, responseType: 'json'});
  }

  getChannel(channelName:string): Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/Single/${channelName}`, {responseType: 'json'});
  }

  payForContent(ChannelPaymentRequest: ChannelPaymentRequestPayload): Observable<any>{
    return this.httpClient.post(`${this.SivantisURL}/Pay`,ChannelPaymentRequest, {responseType: 'text'});
  }

  watchNowPayLater(watchNowPayLaterRequestPayload: WatchNowPayLaterRequestPayload): Observable<any>{
    return this.httpClient.post(`${this.SivantisURL}/WatchNowPayLater`,watchNowPayLaterRequestPayload, {responseType: 'text'});
  }

  searchChannels(query: string, start: number){
    return this.httpClient.get(`${this.SivantisURL}/Search/Channels`, { params: { query, start }, responseType: 'json' });
  }

  refundChannelContent(channelRefundPaymentRequest: ChannelPaymentRequestPayload): Observable<any>{
    return this.httpClient.post(`${this.SivantisURL}/Refund`,channelRefundPaymentRequest, {responseType: 'text'});
  }

  getChannelSubscriptions(): Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/ChannelSubscriptions`, {responseType: 'json'})
  }

  getChannelSubscriptionsEvents(): Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/ChannelSubscriptions/Events`, {responseType: 'json'})
  }

  getAllChannelsNeedingUpdate(start: number): Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/AWV/Update`, {params: { start }, responseType: 'json'})
  }

  updateAverageWeeklyViewers(updateChannel: ChannelStreamerInfoRequestPayload){
    return this.httpClient.put(this.SivantisURL + '/AWV/Update/Channel', updateChannel, {responseType: 'text'});
  }

  getAllChannelUpdateCount(): Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/AWV/Update/Count`, {responseType: 'text'})
  }

}

export interface ChannelStreamerInfoRequestPayload{
  channelId: string
  streamerInfo: StreamerInfo[];
}

export interface ChannelRequestPayload{
  channelName:string;
  channelLogo:string;
  steamerInfo: StreamerInfo[];
  timezone: string
}

export interface ResubmitChannelRequestPayload{
  channelName:string;
  steamerInfo: StreamerInfo[];
}

export interface WatchNowPayLaterRequestPayload {
  channelName: string;
  contentID: string;
  paymentIncrements: number;
}

export interface ChannelPaymentRequestPayload {
  channelName: string
  contentID: string
}

export interface DisapproveChannelResponsePayload{
  channelID: string,
  reason: string,
}

export interface EditChannelRequestPayload{
  channelLogo:string;
  channelBanner:string;
  channelDescription:string;
  channelID: string;
  streamerLinks: Array<StreamerLinks>
}

export interface StreamerLinks {
  platform:string;
  username:string;
  youtubeChannelId:string;
}

export interface ChannelData {
  channelName: string;
  channelLogo?: string;
  streamerInfo: StreamerInfo[];
}