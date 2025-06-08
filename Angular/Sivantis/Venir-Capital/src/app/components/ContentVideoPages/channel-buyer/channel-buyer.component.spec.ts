import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChannelBuyerComponent } from './channel-buyer.component';

describe('ChannelBuyerComponent', () => {
  let component: ChannelBuyerComponent;
  let fixture: ComponentFixture<ChannelBuyerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ChannelBuyerComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChannelBuyerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
