import { Component, HostListener, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CreatedContentDetails } from 'src/app/models/Content/CreatedContentDetails';
import { PurchasedContentResponse } from 'src/app/models/Content/PurchasedContentResponse';
import { AlertService } from 'src/app/services/Alerts/alert.service';
import { AuthenticationService } from 'src/app/services/Auth/authentication.service';
import { ContentService } from 'src/app/services/Content/Content/content.service';
import { UserService } from 'src/app/services/User/user-service';
import { VideoConfirmation, ConfirmationComponent } from '../../Accessories/Popups/confirmation/confirmation.component';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { ReportVideoComponent } from '../../Accessories/Popups/report-video/report-video.component';

@Component({
  selector: 'app-purchased-page',
  templateUrl: './purchased-page.component.html',
  styleUrls: ['./purchased-page.component.css']
})
export class PurchasedPageComponent implements OnInit{
  UserPurchasedVideos: PurchasedContentResponse = {
    content: [],
    payment: [],
  }; 
  selectedIndex = -1;
  decentralandLogo = 'assets/decentraland-mana-logo.png'
  private paymentDate?: string
  private lastVideo = false
  private isScrollHandlerActive = false;
  videoIndices: number[] = [];

  constructor(
    private route: ActivatedRoute,
    private modalService: NgbModal, 
    private authService: AuthenticationService, 
    private contentService: ContentService,
    public alertService:AlertService
  ){

  }
  
  

  ngOnInit(): void {
    const JWT = localStorage.getItem('token') || ''
    this.contentService.fetchAllUserPurchasedContent(JWT, this.paymentDate).subscribe({
      next: (data:PurchasedContentResponse) => {
        this.UserPurchasedVideos.content.push(...data.content)
        this.UserPurchasedVideos.payment.push(...data.payment)
        this.videoIndices = Array.from({ length: this.UserPurchasedVideos.payment.length }, (_, i) => i);
        if (data.payment.length == 50) {
          this.paymentDate = data.payment[data.payment.length - 1].paymentDate.toString();
          this.isScrollHandlerActive = true
        }
        else{
          this.lastVideo = true
        }
      },
      error: () => {},
      complete: () => {}
    });
  }

  reportVideo(contentID: string){
    const modalRef = this.modalService.open(ReportVideoComponent, { size: 'md', scrollable: true, animation: false, centered: true });
    modalRef.componentInstance.contentID = contentID
  }

  deleteVideo(video: CreatedContentDetails){
    const videoConfirmation:VideoConfirmation = {
      contentName: video.contentName,
      description: video.description,
      thumbnail: video.thumbnail,
    }
    const modalRef = this.modalService.open(ConfirmationComponent, {size: 'md', scrollable: true,centered: true , animation: false})
    modalRef.componentInstance.message = "Are you sure you want to Delete this video?"
    modalRef.componentInstance.videoConfirmation = videoConfirmation
    modalRef.result.then(result => {
      if(result === 'continue'){
        const JWT = window.localStorage.getItem('token') || ''
        this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
          this.contentService.deletePurchasedContent(JWTResult, video.contentId).subscribe({
            next: () => {
              const indexToDelete = this.UserPurchasedVideos.content.findIndex(
                (v) => v.contentId === video.contentId
              );
              if (indexToDelete !== -1) {
                this.videoIndices.splice(indexToDelete, 1);
              }
            },
            error: (error) => {
              this.alertService.addAlert(error, 'danger')
            },
            complete: () => {
              this.alertService.addAlert("Content Deleted", 'success')
            }
          })
        })
      }
    })
  }

  getNextPurchases(){
    const JWT = window.localStorage.getItem('token') || ''
    this.authService.checkJWTExpiration(JWT).then((JWTResult) => {
      this.contentService.fetchAllUserPurchasedContent(JWTResult, this.paymentDate).subscribe({
        next: (data: PurchasedContentResponse) => {
          this.UserPurchasedVideos.content.push(...data.content)
          this.UserPurchasedVideos.payment.push(...data.payment)
          this.videoIndices = Array.from({ length: this.UserPurchasedVideos.payment.length }, (_, i) => i);
          if (data.payment.length == 50) {
            this.paymentDate = data.payment[data.payment.length - 1].paymentDate.toString();
          }
          else{
            this.lastVideo = true
          }
        },
        error: () => {},
        complete: () => {}
      });
    })
  }

  viewPage(contentID:string, contentType: string){
    if(this.isAuction(contentType)){
      window.open(`/Auction/${contentID}`, '_blank');
    }
    else{
      window.open(`/Buy/${contentID}`, '_blank');
    }
  }

  watchVideo(videoId: string){
    window.open(`https://www.youtube.com/watch?v=${videoId}`, '_blank');
  }

  getStatusClass(status: string) {
    switch (status) {
      case 'RefundedPurchase':
        return 'status-red';
      case 'PendingPurchase':
        return 'status-yellow';
      case 'Purchased':
        return 'status-green';
      default:
        return 'status-grey';
    }
  }

  getStatus(status: string) {
    switch (status) {
      case 'RefundedPurchase':
        return 'Refunded';
      case 'PendingPurchase':
        return 'Pending';
      case 'Purchased':
        return 'Purchased';
      default:
        return 'Inactive';
    }
  }

  editVideo(index: number) {
    this.selectedIndex = index;
  }

  isAuction(contentType:string):boolean{
    return contentType === "Invention" || contentType === "Innovation";
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
        if(this.lastVideo == false){
          this.getNextPurchases()
        }
      }
    }
  }
  
}
