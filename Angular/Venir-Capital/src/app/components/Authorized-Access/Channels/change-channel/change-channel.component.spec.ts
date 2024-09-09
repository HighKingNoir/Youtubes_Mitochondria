import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChangeChannelComponent } from './change-channel.component';

describe('ChangeChannelComponent', () => {
  let component: ChangeChannelComponent;
  let fixture: ComponentFixture<ChangeChannelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ChangeChannelComponent]
    });
    fixture = TestBed.createComponent(ChangeChannelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
