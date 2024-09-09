import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Subscription } from 'rxjs';
import { PricingService } from 'src/app/services/Mana/PricingService/pricing.service';

@Component({
  selector: 'app-watch-now-pay-later',
  templateUrl: './watch-now-pay-later.component.html',
  styleUrls: ['./watch-now-pay-later.component.css']
})
export class WatchNowPayLaterComponent implements OnInit, OnDestroy{

  @Input() refundManaAmount!: number;

  priceSubscription!: Subscription;
  MANA_PRICE = 0;

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
      },
      error: (err) => {
        this.priceSubscription.unsubscribe();
      },
      complete: () => {
          
      },
    });

  }

  ngOnDestroy(): void {
    this.priceSubscription.unsubscribe();
  }
}
