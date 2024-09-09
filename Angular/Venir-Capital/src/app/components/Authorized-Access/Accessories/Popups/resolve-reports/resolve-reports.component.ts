import { ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ContentReports, CreatedContentDetails } from 'src/app/models/Content/CreatedContentDetails';
import { ResolveReportRequestPayload } from 'src/app/models/Content/ResolveReportRequestPayload';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ContentService } from 'src/app/services/Content/Content/content.service';
import { HandleReportService } from 'src/app/services/Report/handle-report.service';

@Component({
  selector: 'app-resolve-reports',
  templateUrl: './resolve-reports.component.html',
  styleUrls: ['./resolve-reports.component.css']
})
export class ResolveReportsComponent implements OnInit{
  @Input() content!: CreatedContentDetails

  constructor(
    private handelReportService: HandleReportService,
    private contentService: ContentService,
    private authService: AuthenticationService,
    public alertService: AlertService,
    public activeModal: NgbActiveModal,
  ){

  }
  ngOnInit(): void {
    this.handelReportService.setContent(this.content)
  }

  refreashContent(contentID:string){
    const JWT: string = window.localStorage.getItem('token') || '';
    this.authService.checkJWTExpiration(JWT).then(() => {
      this.contentService.fetchSingleContent(contentID).subscribe(refreashedContent => {
        this.content = refreashedContent
        this.handelReportService.setContent(this.content)
      })
    })
    
  }

  resolveAll(){
    const resolveReportRequestPayload: ResolveReportRequestPayload = {
      contentID: this.content.contentId,
      reporterIDs: this.getAllUnresolvedReporterIds(this.content.contentReports)
    }
    const JWT: string = window.localStorage.getItem('token') || '';
    this.authService.checkJWTExpiration(JWT).then(() => {
      this.contentService.resolveAllReports(resolveReportRequestPayload).subscribe({
        next: (newContent: any) => {
          this.content = newContent
          this.handelReportService.setContent(this.content)
          this.alertService.addAlert("All Reports Resolved", "success")
          this.activeModal.close('Done')
        },
        error: (err) => {
          this.alertService.addAlert(err, "danger")
          this.refreashContent(this.content.contentId)
        },
        complete: () => {
            
        },
      })
    })
  }

  resolve(reporterID: string){
    const reporterIDs = new Array<string>
    reporterIDs.push(reporterID);
    const resolveReportRequestPayload: ResolveReportRequestPayload = {
      contentID: this.content.contentId,
      reporterIDs: reporterIDs
    }
    const JWT: string = window.localStorage.getItem('token') || '';
    this.authService.checkJWTExpiration(JWT).then(() => {
      this.contentService.resolveReport(resolveReportRequestPayload).subscribe({
        next: (newContent: any) => {
          const index = this.content.contentReports.findIndex(report => report.reporterID === reporterID);
          if(index != -1){
            this.content.contentReports[index].isResolved = true
          }
          this.handelReportService.setContent(newContent)
          this.alertService.addAlert("Report Resolved", "success")
        },
        error: (err) => {
          this.alertService.addAlert(err, "danger")
        },
        complete: () => {
            
        },
      })
    })
  }

  ignore(reporterID: string){
    const reporterIDs = new Array<string>
    reporterIDs.push(reporterID);
    const resolveReportRequestPayload: ResolveReportRequestPayload = {
      contentID: this.content.contentId,
      reporterIDs: reporterIDs
    }
    const JWT: string = window.localStorage.getItem('token') || '';
    this.authService.checkJWTExpiration(JWT).then(() => {
      this.contentService.ignoreReport(resolveReportRequestPayload).subscribe({
        next: (newContent: any) => {
          const index = this.content.contentReports.findIndex(report => report.reporterID === reporterID);
          if(index != -1){
            this.content.contentReports[index].isResolved = true
          }
          this.handelReportService.setContent(newContent)
          this.alertService.addAlert("Report Ignored", "success")
        },
        error: (err) => {
          this.alertService.addAlert(err, "danger")
        },
        complete: () => {
            
        },
      })
    })
  }

  ignoreAll(){
    const resolveReportRequestPayload: ResolveReportRequestPayload = {
      contentID: this.content.contentId,
      reporterIDs: this.getAllUnresolvedReporterIds(this.content.contentReports)
    }
    const JWT: string = window.localStorage.getItem('token') || '';
    this.authService.checkJWTExpiration(JWT).then(() => {
      this.contentService.ignoreAllReports(resolveReportRequestPayload).subscribe({
        next: (newContent: any) => {
          this.content = newContent
          this.handelReportService.setContent(this.content)
          this.alertService.addAlert("All Reports Ignored", "success")
          this.activeModal.close('Done')
        },
        error: (err) => {
          this.alertService.addAlert(err, "danger")
          this.refreashContent(this.content.contentId)
        },
        complete: () => {
            
        },
      })
    })
  }

  getAllUnresolvedReporterIds(allReports: ContentReports[]): Array<string>{
    const reporterIDs = new Array<string>
    for (let index = 0; index < allReports.length; index++) {
      if(!allReports[index].isResolved){
        reporterIDs.push(allReports[index].reporterID)
      }
    }
    return reporterIDs;
  }

}
