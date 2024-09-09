import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { NgbActiveModal, NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, Subject, Subscription } from 'rxjs';
import { ChannelService } from 'src/app/services/Channel/channel.service';
import { ChannelServiceContract } from 'src/app/services/Contracts/Channel/channel-service-contract.service';
import { ConnectWalletService } from 'src/app/services/Mana/ConnectPersonalWallet/connect-wallet.service';
import { PricingService } from 'src/app/services/Mana/PricingService/pricing.service';

@Component({
  selector: 'app-refund-content',
  templateUrl: './refund-content.component.html',
  styleUrls: ['./refund-content.component.css']
})
export class RefundContentComponent implements OnInit, OnDestroy{

  @Input() refundManaAmount!: number;

  priceSubscription!: Subscription;
  MANA_PRICE = 0;
  refundAmount  = ''

  private refundAmountSubject = new Subject<string>
  refundAmountInfo$ = this.refundAmountSubject.asObservable();

  constructor(
    private pricingService: PricingService, 
    public activeModal: NgbActiveModal,
    private cdr: ChangeDetectorRef)
  {

  }
  
  ngOnInit(): void {
    this.priceSubscription = this.pricingService.getManaPrices().subscribe({
      next: (price) => {
        this.MANA_PRICE = Number(price)
        this.refundAmountSubject.next(this.getRefundAmount())
      },
      error: (err) => {
        this.priceSubscription.unsubscribe();
      },
      complete: () => {
          
      },
    });
    this.refundAmountInfo$.subscribe(refundAmount =>{
      this.refundAmount = refundAmount
      this.cdr.detectChanges()
    })
  }

  ngOnDestroy(): void {
    this.priceSubscription.unsubscribe();
    this.refundAmountSubject.unsubscribe()
  }

  
  getRefundAmount():string{
    const refundAmount = this.refundManaAmount * this.MANA_PRICE * .9
    return (refundAmount).toFixed(2)
  }

}
