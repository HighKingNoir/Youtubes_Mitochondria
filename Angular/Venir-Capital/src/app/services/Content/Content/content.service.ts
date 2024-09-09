import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from 'src/Environment/environment';
import { BehaviorSubject, Observable } from 'rxjs';
import { CreatedContentDetails } from 'src/app/models/Content/CreatedContentDetails';
import { ContentRequestPayload } from 'src/app/models/Content/ContentRequestPayload';
import { PurchasedContentDetails } from 'src/app/models/Content/PurchasedContentDetails';
import { EditRequestPayload } from 'src/app/models/Content/EditRequestPayload';
import { CompleteVideoRequestPayload } from 'src/app/models/Content/CompleteVideoRequestPayload';
import { ReactivateContentRequestPayload } from 'src/app/models/Content/ReactivateContentRequestPayload';
import { SentVideoRequestPayload } from 'src/app/models/Content/SentVideoRequestPayload';
import { ReportContentRequestPayload } from 'src/app/models/Content/ReportContentRequestPayload';
import { ResolveReportRequestPayload } from 'src/app/models/Content/ResolveReportRequestPayload';

@Injectable({
  providedIn: 'root'
})
export class ContentService {
  maxNumberOfBuyers = 50
  private SivantisURL:string = environment.BackendURL + "Content";

  readonly shortFilmPrice: number = .05
  readonly sportsPrice: number = .10
  readonly concertPrice: number = .10
  readonly moviePrice: number = .20

  private createdContentSubject = new BehaviorSubject<CreatedContentDetails[]>([]);
  createdContentInfo$ = this.createdContentSubject.asObservable();

  constructor(private httpClient: HttpClient) {
    
  }
  

  //Creates content that has already been uploaded and has a url (Params: ContentRequestPayload)
  createCompleteContent(ContentRequestPayload: ContentRequestPayload): Observable<any>{
    return this.httpClient.post(this.SivantisURL + '/Complete', ContentRequestPayload, {responseType: 'json'});
  }

  //Creates content that HASN'T been uploaded and DOESN'T have a url (Params: ContentRequestPayload)
  createInDevelopmentContent(ContentRequestPayload: ContentRequestPayload): Observable<any>{
    return this.httpClient.post(this.SivantisURL + '/InDevelopment', ContentRequestPayload, {responseType: 'json'});
  }

  //Returns all content in the Database
  fetchAllContent(): Observable<any>{
    return this.httpClient.get(this.SivantisURL + '/All', {responseType: 'json'});
  }

  //Returns one content in the Database
  fetchSingleContent(contentId: string): Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/Single/${contentId}`, {responseType: 'json'});
  }

  //Returns all content that has content.type set to Active
  fetchAllActiveContent(activeDate?: string) {
    if(activeDate){
      return this.httpClient.get(`${this.SivantisURL}/All/Active`, { params: { activeDate }, responseType: 'json' });
    }
    return this.httpClient.get(`${this.SivantisURL}/All/Active`, { responseType: 'json' });
  }

  fetchAllActiveInventiveContent(activeDate?: string) {
    if(activeDate){
      return this.httpClient.get(`${this.SivantisURL}/All/Active/Inventions`, { params: { activeDate }, responseType: 'json' });
    }
    return this.httpClient.get(`${this.SivantisURL}/All/Active/Inventions`, { responseType: 'json' });
  }

  searchVideos(query: string, start: number){
    return this.httpClient.get(`${this.SivantisURL}/Search/Videos`, { params: { query, start }, responseType: 'json' });
  }

  fetchAllActiveInnovativeContent(activeDate?: string) {
    if(activeDate){
      return this.httpClient.get(`${this.SivantisURL}/All/Active/Innovation`, { params: { activeDate }, responseType: 'json' });
    }
    return this.httpClient.get(`${this.SivantisURL}/All/Active/Innovation`, { responseType: 'json' });
  }

  fetchAllActiveShortFilmContent(activeDate?: string) {
    if(activeDate){
      return this.httpClient.get(`${this.SivantisURL}/All/Active/ShortFilm`, { params: { activeDate }, responseType: 'json' });
    }
    return this.httpClient.get(`${this.SivantisURL}/All/Active/ShortFilm`, { responseType: 'json' });
  }

  fetchAllActiveMovies(activeDate?: string) {
    if(activeDate){
      return this.httpClient.get(`${this.SivantisURL}/All/Active/Movies`, { params: { activeDate }, responseType: 'json' });
    }
    return this.httpClient.get(`${this.SivantisURL}/All/Active/Movies`, { responseType: 'json' });
  }

  fetchAllActiveSports(activeDate?: string) {
    if(activeDate){
      return this.httpClient.get(`${this.SivantisURL}/All/Active/Sports`, { params: { activeDate }, responseType: 'json' });
    }
    return this.httpClient.get(`${this.SivantisURL}/All/Active/Sports`, { responseType: 'json' });
  }

  fetchAllActiveConcerts(activeDate?: string) {
    if(activeDate){
      return this.httpClient.get(`${this.SivantisURL}/All/Active/Concerts`, { params: { activeDate }, responseType: 'json' });
    }
    return this.httpClient.get(`${this.SivantisURL}/All/Active/Concerts`, { responseType: 'json' });
  }

  fetchAllActiveMostHypeContent(hypeValue?: number) {
    if(hypeValue){
      return this.httpClient.get(`${this.SivantisURL}/All/Active/Hype`, { params: { hypeValue }, responseType: 'json' });
    }
    return this.httpClient.get(`${this.SivantisURL}/All/Active/Hype`, { responseType: 'json' });
  }

  getAllActiveUpcomingReleaseContent(start: number) {
    return this.httpClient.get(`${this.SivantisURL}/All/Active/Upcoming`, { params: { start }, responseType: 'json' });
  }

  getAllPayLaterContent(JWT: string, start: number) {
    return this.httpClient.get(`${this.SivantisURL}/PayLater`, { params: { JWT, start }, responseType: 'json' });
  }

  //Returns all content a user created (Params: UserID)
  fetchAllUserCreatedContent(JWT: string, activeDate: string | undefined): Observable<any>{
    if(activeDate){
      return this.httpClient.get(`${this.SivantisURL}/Created/${JWT}`, { params: { activeDate }, responseType: 'json' });
    }
    return this.httpClient.get(`${this.SivantisURL}/Created/${JWT}`, { responseType: 'json' });
  }

  fetchAllCreatedContentFromUser(username: string, activeDate: string | undefined): Observable<any>{
    if(activeDate){
      return this.httpClient.get(`${this.SivantisURL}/All/User`, { params: { username ,activeDate }, responseType: 'json' });
    }
    return this.httpClient.get(`${this.SivantisURL}/All/User`, { params: { username }, responseType: 'json' });
  }

  //Returns all content a user purchased (Params: UserID)
  fetchAllUserPurchasedContent(JWT: string, paymentDate: string | undefined): Observable<any>{
    if(paymentDate){
      return this.httpClient.get(`${this.SivantisURL}/Purchased/${JWT}`, { params: { paymentDate }, responseType: 'json' });
    }
    return this.httpClient.get(`${this.SivantisURL}/Purchased/${JWT}`, { responseType: 'json' });
  }

  //Returns all content a user purchased (Params: UserID)
  fetchAllChannelPurchasedContent(channelName: string, paymentDate: string | undefined): Observable<any>{
    if(paymentDate){
      return this.httpClient.get(`${this.SivantisURL}/Channel/Purchased/${channelName}`, { params: { paymentDate }, responseType: 'json' });
    }
    return this.httpClient.get(`${this.SivantisURL}/Channel/Purchased/${channelName}`, { responseType: 'json' });
  }

  //Returns one conent entity (Params: ContentID)  
  fetchSortedBuyers(ContentID: string): Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/sortBuyers/${ContentID}`, {responseType: 'json'})
  }

  fetchChannelSortedBuyers(ContentID: string): Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/sortBuyers/Channel/${ContentID}`, {responseType: 'json'})
  }

  fetchAllUserActiveHype(JWT: string): Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/Active/Hype/${JWT}`, {responseType: 'text'})
  }

  editVideo(editRequestPayload: EditRequestPayload): Observable<any>{
    return this.httpClient.put(`${this.SivantisURL}/Edit`,editRequestPayload, {responseType: 'text'})
  }

  completeVideo(completeVideoRequestPayload: CompleteVideoRequestPayload): Observable<any>{
    return this.httpClient.put(`${this.SivantisURL}/Complete/InDevelopment`,completeVideoRequestPayload, {responseType: 'text'})
  }

  reactivateVideo(reactivateContentRequestPayload:ReactivateContentRequestPayload): Observable<any>{
    return this.httpClient.put(`${this.SivantisURL}/Reactivate`,reactivateContentRequestPayload, {responseType: 'text'})
  }

  sentVideo(sentVideoRequestPayload:SentVideoRequestPayload): Observable<any>{
    return this.httpClient.patch(`${this.SivantisURL}/Videos`,sentVideoRequestPayload, {responseType: 'text'})
  }

  reportVideo(reportContentRequestPayload:ReportContentRequestPayload){
    return this.httpClient.patch(`${this.SivantisURL}/Report`,reportContentRequestPayload, {responseType: 'text'})
  }

  viewReports(createdDate:string, reportRate?:number){
    if(reportRate){
      return this.httpClient.get(`${this.SivantisURL}/Report/View`, {params: { createdDate,  reportRate},responseType: 'json'})
    }
    return this.httpClient.get(`${this.SivantisURL}/Report/View`, {responseType: 'json'})
  }

  deleteCreatedContent(JWT: string, contentID: string): Observable<any>{
    return this.httpClient.delete(`${this.SivantisURL}/Delete/${JWT}/${contentID}`, {responseType: 'text'})
  }

  deletePurchasedContent(JWT: string, contentID: string): Observable<any>{
    return this.httpClient.delete(`${this.SivantisURL}/Delete/Purchased/${JWT}/${contentID}`, {responseType: 'text'})
  }

  deleteChannelPurchasedContent(JWT: string, contentID: string, channelID: string): Observable<any>{
    return this.httpClient.delete(`${this.SivantisURL}/Delete/Channel/Purchased/${JWT}/${contentID}/${channelID}`, {responseType: 'text'})
  }

  resolveReport(resolveReportRequestPayload: ResolveReportRequestPayload){
    return this.httpClient.patch(`${this.SivantisURL}/Report/Resolve`, resolveReportRequestPayload, {responseType: 'json'})
  }

  resolveAllReports(resolveReportRequestPayload: ResolveReportRequestPayload){
    return this.httpClient.patch(`${this.SivantisURL}/Report/Resolve/All`, resolveReportRequestPayload, {responseType: 'json'})
  }

  ignoreReport(resolveReportRequestPayload: ResolveReportRequestPayload){
    return this.httpClient.patch(`${this.SivantisURL}/Report/Ignore`, resolveReportRequestPayload, {responseType: 'json'})
  }

  ignoreAllReports(resolveReportRequestPayload: ResolveReportRequestPayload){
    return this.httpClient.patch(`${this.SivantisURL}/Report/Ignore/All`, resolveReportRequestPayload, {responseType: 'json'})
  }


  initualizeAllUserCreatedContent(ContentInfo: CreatedContentDetails[]){
    return this.createdContentSubject.next([...ContentInfo]);
  }

  updateAllUserCreatedContent(ContentInfo: CreatedContentDetails[]){
    return this.createdContentSubject.next([...this.createdContentSubject.getValue(), ...ContentInfo]);
  }

  
  addCreatedContent(ContentInfo: CreatedContentDetails){
    this.createdContentSubject.next([ContentInfo, ...this.createdContentSubject.getValue()])
  }



}
