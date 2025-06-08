import { Injectable } from '@angular/core';
import { releaseDateInfo } from 'src/app/models/Content/ContentRequestPayload';
import { EditRequestPayload } from 'src/app/models/Content/EditRequestPayload';



@Injectable({
  providedIn: 'root'
})

export class UploadVideoInfoService {
  youtubeAllVideoInfo!: any[];
  youtubeMainVideoInfo!: any;
  youtubeTrailerVideoInfo!: any;
  editedVideo: EditRequestPayload;
  newReleaseDate: releaseDateInfo

  constructor() {
    this.editedVideo = {
      contentID: '',
      contentName: '',
      description: '',
      youtubeTrailerVideoID: ''
    }
    this.newReleaseDate = {
      year: 0,
      month: 0,
      day: 0
    }
  }

   //returns YoutubeVideoInfo
   getYoutubeAllVideoInfo(){
    return this.youtubeAllVideoInfo;
  }

  //Updates the variables in the YoutubeVideoInfo
  setYoutubeAllVideoInfo(VideoInfo: any[]){
    this.youtubeAllVideoInfo = VideoInfo;
  }

   //returns YoutubeVideoInfo
   getSelectedMainYoutubeVideoInfo(){
    return this.youtubeMainVideoInfo;
  }

  //Updates the variables in the YoutubeVideoInfo
  setSelectedMainYoutubeVideoInfo(VideoInfo: any){
    this.youtubeMainVideoInfo = VideoInfo;
  }

   //returns YoutubeVideoInfo
   getSelectedTrailerYoutubeVideoInfo(){
    return this.youtubeTrailerVideoInfo;
  }

  //Updates the variables in the YoutubeVideoInfo
  setSelectedTrailerVideoInfo(VideoInfo: any){
    this.youtubeTrailerVideoInfo = VideoInfo;
  }

  getEditedVideo(): EditRequestPayload{
    return this.editedVideo
  }

  setEditedVideo(video:EditRequestPayload){
    this.editedVideo = video;
  }

  getNewReleaseDate(): releaseDateInfo{
    return this.newReleaseDate
  }

  setNewReleaseDate(newReleaseDate:releaseDateInfo){
    this.newReleaseDate = newReleaseDate;
  }


}
