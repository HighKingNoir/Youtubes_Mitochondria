import { HttpClient, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from 'src/Environment/environment';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { User} from 'src/app/models/Users/User';



@Injectable({
  providedIn: 'root'
})
export class UserService {
  private SivantisURL:string = environment.BackendURL + "Users";
  private User: User;

  private userSubject = new Subject<User>();
  userInfo$ = this.userSubject.asObservable();

  constructor(private httpClient: HttpClient) {
    this.User = {
      userId: '',
      username: '',
      email: '',
      rank: 1,
      totalHype: 0,
      personalWallet: '',
      role: "USER",
      payLater: [],
      channelSubscribedTo: [],
      allowedDevelopingVideos: 0,
      mfaEnabled: false,
      videosPosted: 0
    }
  }


  //Gets All users
  getAllUsers(): Observable<any>{
    return this.httpClient.get(this.SivantisURL + '/All', {responseType: 'json'});
  }

  //Gets a single user (Param: Jwt Token)
  getUserFromServer(): Observable<any>{
    const Token = localStorage.getItem('token') || '{}';
    return this.httpClient.get(`${this.SivantisURL}/User/${Token}`, {responseType: 'json'});
  }

  getNextWalletChangeTime(JWT: string): Observable<any>{
    return this.httpClient.get(`${this.SivantisURL}/PersonalWallet/NextWalletChange/${JWT}`, {responseType: 'text'});
  }

  getLeaderBoard(totalHype?: number) {
    if(totalHype){
      return this.httpClient.get(`${this.SivantisURL}/Leaderboard`, { params: { totalHype }, responseType: 'json' });
    }
    return this.httpClient.get(`${this.SivantisURL}/Leaderboard`, { responseType: 'json' });
  }

  getUserFromLeaderBoard(userId: string) {
    return this.httpClient.get(`${this.SivantisURL}/Leaderboard/${userId}`, { responseType: 'json' });
  }

  //Sets personal wallet when a user connects their polygon wallet (Param: UserID & Wallet address)
  setUserPersonalWallet(personalWalletPayload: PersonalWalletPayload): Observable<any>{
    return this.httpClient.patch(`${this.SivantisURL}/PersonalWallet`, personalWalletPayload, {responseType: 'json'})
  }

  addToPayLater(contentId: string): Observable<any>{
    return this.httpClient.put(`${this.SivantisURL}/PayLater/Add/${contentId}`, null, {responseType: 'text'})
  }

  changeUsername(usernameChangePayload:UsernameChangePayload): Observable<any>{
    return this.httpClient.put(`${this.SivantisURL}/Change/Username`, usernameChangePayload, {responseType: 'json'})
  }

  changePassword(passwordChangePayload:PasswordChangePayload): Observable<any>{
    return this.httpClient.put(`${this.SivantisURL}/Change/Password`, passwordChangePayload, {responseType: 'json'})
  }

  removeFromPayLater(contentId: string): Observable<any>{
    return this.httpClient.put(`${this.SivantisURL}/PayLater/Remove/${contentId}`, null, {responseType: 'text'})
  }

  subscribeToChannel(contentId: string): Observable<any>{
    return this.httpClient.put(`${this.SivantisURL}/Subscribe/${contentId}`, null, {responseType: 'text'})
  }

  unsubscribeToChannel(contentId: string): Observable<any>{
    return this.httpClient.put(`${this.SivantisURL}/Unsubscribe/${contentId}`, null, {responseType: 'text'})
  }

  deleteUser(code: string): Observable<any>{
    return this.httpClient.put(`${this.SivantisURL}/Delete`, null, {params: { code }, responseType: 'json'})
  }

  toggle2FA(code: string): Observable<any>{
    return this.httpClient.put(`${this.SivantisURL}/Toggle2FA`, null, { params: { code }, responseType: 'json' });
  }

  getPersonalWallet(): string {
    return this.User.personalWallet
  }

  setPersonalWallet(personalWallet: string) {
    this.User.personalWallet = personalWallet
  }

  getInMemoryUser(): User{
    return this.User;
  }

  //Gets a user's username 
  getUsername(): string{
    return this.User.username;
  }

  getMfaEnabled(): boolean{
    return this.User.mfaEnabled;
  }

  //Gets a user's ID 
  getUserID(): string{
    return this.User.userId
  }

  //Gets a user's Email
  getUserEmail(): string{
    return this.User.email
  }

  getUserRole(): string{
    return this.User.role
  }

  getUserRank(): number{
    return this.User.rank
  }

  getUserHype(): number{
    return this.User.totalHype
  }

  getUserVideosPosted(): number{
    return this.User.videosPosted
  }

  setFirstUserVideosPosted(){
    this.User.videosPosted = 1
  }


  getUserPayLater(): Array<string>{
    return this.User.payLater
  }

  getChannelsSubscribedTo(): Array<string>{
    return this.User.channelSubscribedTo
  }

  getAllowedDevelopingVideos(): number{
    return this.User.allowedDevelopingVideos
  }

  UpdateUser(UserInfo: User){
    this.User = UserInfo
    this.userSubject.next(UserInfo);
    
  }
}

export interface PersonalWalletPayload{
  personalWallet: string;
  code: string
}

export interface UsernameChangePayload{
  username: string
  code: string
}


export interface PasswordChangePayload{
  currentPassword: string
  newPassword: string
  code: string
}
