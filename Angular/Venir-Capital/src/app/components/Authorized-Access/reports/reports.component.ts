import { Component, HostListener, OnInit } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { CreatedContentDetails } from 'src/app/models/Content/CreatedContentDetails';
import { ContentService } from 'src/app/services/Content/Content/content.service';
import { ResolveReportsComponent } from '../Accessories/Popups/resolve-reports/resolve-reports.component';
import { HandleReportService } from 'src/app/services/Report/handle-report.service';
import { AlertService } from 'src/app/services/Alerts/alert.service';


@Component({
  selector: 'app-reports',
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.css']
})
export class ReportsComponent implements OnInit{
  private createdDate = '';
  private lastReport = false
  private reportRate?: number 
  
  Content: CreatedContentDetails[] = []
  private isScrollHandlerActive = false;

  constructor(
    private contentService: ContentService,
    private modalService: NgbModal,
    private handleReportService: HandleReportService,
    public alertService: AlertService,
  ) {
   }

  ngOnInit(): void {
    this.contentService.viewReports(this.createdDate, this.reportRate).subscribe({
      next: (data:any) => {
        this.Content.push(...data);
        if (data.length == 50) {
          this.createdDate = data[data.length - 1].createdDate;
          this.reportRate = data[data.length - 1].reportRate;
          this.isScrollHandlerActive = true
        }
        else{
          this.lastReport = true
        }
      },
      error: () => {},
      complete: () => {}
    });
  }



  resolveReport(content: CreatedContentDetails){
    const modalRef = this.modalService.open(ResolveReportsComponent, { size: 'lg', scrollable: true, centered: true , keyboard: false, backdrop: 'static', animation: false });
    modalRef.componentInstance.content = content
    modalRef.result.then((result) => {
      if(result === 'Done'){
        const newContent = this.handleReportService.getContent()
        const indexToEdit = this.Content.findIndex(
          (v) => v.contentId === newContent.contentId
        );
        if (indexToEdit != -1) {
          if(newContent.reportRate != null){
            this.Content[indexToEdit] = newContent
          }
          else{
            this.Content.splice(indexToEdit, 1)
          }
        }
      }
    })
  }


  getColor(reportRate: number): string {

    if(reportRate == 0){
      return 'yellow'
    }
    else if(reportRate == 50){
      return 'orange'
    }
    else if(reportRate == 100){
      return 'rgb(230, 40, 40)'
    }

    // Define the color thresholds and corresponding colors
    const colorThresholds = [0, 25, 50, 75, 100];
    const colors = ['yellow', 'rgb(255, 215, 0)', 'orange', 'rgb(255, 165, 0)', 'rgb(230, 40, 40)']; // 'rgb(255, 215, 0)' is a shade of yellow-orange

    // Find the index of the color based on the reportRate
    let colorIndex = 0;
    for (let i = 0; i < colorThresholds.length; i++) {
        if (reportRate >= colorThresholds[i]) {
            colorIndex = i;
        } else {
            break;
        }
    }

    // Calculate the ratio between the current and next threshold
    const ratio = (reportRate - colorThresholds[colorIndex]) / (colorThresholds[colorIndex + 1] - colorThresholds[colorIndex]);

    // Interpolate between the colors using the ratio
    const color1 = colors[colorIndex];
    const color2 = colors[colorIndex + 1];
    const blendedColor = this.interpolateColor(color1, color2, ratio);
    return blendedColor;
}

  interpolateColor(color1: string, color2: string, ratio: number): string {
    const hex = function(x: number): string {
        x = Math.round(x);
        const hexDigits = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'];
        return isNaN(x) ? '00' : hexDigits[(x - x % 16) / 16] + hexDigits[x % 16];
    };

    const r1 = parseInt(color1.substring(1, 3), 16);
    const g1 = parseInt(color1.substring(3, 5), 16);
    const b1 = parseInt(color1.substring(5, 7), 16);

    const r2 = parseInt(color2.substring(1, 3), 16);
    const g2 = parseInt(color2.substring(3, 5), 16);
    const b2 = parseInt(color2.substring(5, 7), 16);

    const r = Math.round(r1 * (1 - ratio) + r2 * ratio);
    const g = Math.round(g1 * (1 - ratio) + g2 * ratio);
    const b = Math.round(b1 * (1 - ratio) + b2 * ratio);

    return '#' + hex(r) + hex(g) + hex(b);
}

  @HostListener('window:scroll', ['$event'])
  onScroll(event: any) {
    if(this.isScrollHandlerActive){
      const windowHeight = 'innerHeight' in window ? window.innerHeight : document.documentElement.offsetHeight;
      const body = document.body, html = document.documentElement;
      const docHeight = Math.max(body.scrollHeight, body.offsetHeight, html.clientHeight, html.scrollHeight, html.offsetHeight);
      const windowBottom = window.scrollY + windowHeight; // Calculate window bottom correctly

      if (windowBottom >= docHeight - 200) {
        this.isScrollHandlerActive = false;
        if(this.lastReport == false){
          this.contentService.viewReports(this.createdDate, this.reportRate).subscribe({
            next: (data:any) => {
              this.Content.push(...data);
              if (data.length == 50) {
                this.createdDate = data[data.length - 1].createdDate;
                this.reportRate = data[data.length - 1].reportRate
                if(this.reportRate == 0){
                  this.lastReport = true
                }
              }
              else{
                this.lastReport = true
              }
              this.isScrollHandlerActive = true;
            },
            error: () => {
              this.isScrollHandlerActive = true;
            },
            complete: () => {}
          });
        }
      }
    }
  }


}
