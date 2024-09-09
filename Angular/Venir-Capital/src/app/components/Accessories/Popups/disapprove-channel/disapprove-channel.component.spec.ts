import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DisapproveChannelComponent } from './disapprove-channel.component';

describe('DisapproveChannelComponent', () => {
  let component: DisapproveChannelComponent;
  let fixture: ComponentFixture<DisapproveChannelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [DisapproveChannelComponent]
    });
    fixture = TestBed.createComponent(DisapproveChannelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
