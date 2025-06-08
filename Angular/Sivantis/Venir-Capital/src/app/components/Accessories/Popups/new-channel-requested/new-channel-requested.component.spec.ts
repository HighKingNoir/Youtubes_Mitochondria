import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NewChannelRequestedComponent } from './new-channel-requested.component';

describe('NewChannelRequestedComponent', () => {
  let component: NewChannelRequestedComponent;
  let fixture: ComponentFixture<NewChannelRequestedComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [NewChannelRequestedComponent]
    });
    fixture = TestBed.createComponent(NewChannelRequestedComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
