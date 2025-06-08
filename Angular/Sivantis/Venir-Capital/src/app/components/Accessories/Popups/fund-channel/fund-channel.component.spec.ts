import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FundChannelComponent } from './fund-channel.component';

describe('FundChannelComponent', () => {
  let component: FundChannelComponent;
  let fixture: ComponentFixture<FundChannelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ FundChannelComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FundChannelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
