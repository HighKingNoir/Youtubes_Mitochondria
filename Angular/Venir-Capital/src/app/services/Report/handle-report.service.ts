import { Injectable } from '@angular/core';
import { CreatedContentDetails } from 'src/app/models/Content/CreatedContentDetails';

@Injectable({
  providedIn: 'root'
})
export class HandleReportService {

  content!: CreatedContentDetails

  constructor() { }

  setContent(content: CreatedContentDetails){
    this.content = content
  }

  getContent(){
    return this.content
  }

}
