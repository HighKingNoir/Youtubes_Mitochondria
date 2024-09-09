import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { OAuthService } from 'angular-oauth2-oidc';
import { UploadVideoInfoService } from '../Content/VideoInfo/upload-video-info.service';
import { Observable, of, switchMap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class YoutubeAPIService {

  constructor(
    private http: HttpClient, 
    private readonly oAuthService: OAuthService, 
  ) { }

  fetchLatestYouTubeVideos(nextPageToken?: string): Observable<any> {
    const accessToken = this.oAuthService.getAccessToken();

    const endpoint = 'https://youtube.googleapis.com/youtube/v3/search'

    let params = new HttpParams()
      .set('part', 'id,snippet')
      .set('maxResults', '50')
      .set('forMine', 'true')
      .set('order', 'date')
      .set('type', 'video')
      .set('access_token', accessToken);
      
    if (nextPageToken) {
      params = params.set('pageToken', nextPageToken);
    }
    const headers = new HttpHeaders()
      .set('Authorization', `Bearer ${accessToken}`);

    return this.fetchVideos(endpoint, params, headers)
    
  }

  fetchLatestLiveYouTubeVideos(nextPageToken?: string): Observable<any> {
    const accessToken = this.oAuthService.getAccessToken();

    const endpoint = 'https://youtube.googleapis.com/youtube/v3/liveBroadcasts'

    let params = new HttpParams()
      .set('part', 'id,snippet')
      .set('maxResults', '50')
      .set('mine', 'true')
      .set('access_token', accessToken);
      
    if (nextPageToken) {
      params = params.set('pageToken', nextPageToken);
    }
    const headers = new HttpHeaders()
      .set('Authorization', `Bearer ${accessToken}`);

    return this.fetchVideos(endpoint, params, headers)
  }
    
  fetchYoutTubeVideoByID(youtubeID: string): Observable<any> {
    const accessToken = this.oAuthService.getAccessToken();

    const endpoint = 'https://youtube.googleapis.com/youtube/v3/videos'

    const params = new HttpParams()
      .set('part', 'id,snippet,contentDetails,status')
      .set('id', youtubeID)
      .set('fields', 'items(id,snippet(title,description,thumbnails/high,liveBroadcastContent),contentDetails/duration,status/privacyStatus)')
   

    const headers = new HttpHeaders()
      .set('Authorization', `Bearer ${accessToken}`);

    return this.fetchVideos(endpoint, params, headers)
  }


   fetchVideos(endpoint: string,params: HttpParams, headers: HttpHeaders): Observable<any> {
    return this.http.get(endpoint, { params, headers })
  }
}
export interface YoutubeVideoResponse {
  items: any[];
  nextPageToken?: string;
}











