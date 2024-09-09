import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WatchNowPayLaterComponent } from './watch-now-pay-later.component';

describe('WatchNowPayLaterComponent', () => {
  let component: WatchNowPayLaterComponent;
  let fixture: ComponentFixture<WatchNowPayLaterComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [WatchNowPayLaterComponent]
    });
    fixture = TestBed.createComponent(WatchNowPayLaterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
