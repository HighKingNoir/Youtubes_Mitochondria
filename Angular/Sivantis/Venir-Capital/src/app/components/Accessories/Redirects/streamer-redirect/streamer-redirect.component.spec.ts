import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StreamerRedirectComponent } from './streamer-redirect.component';

describe('StreamerRedirectComponent', () => {
  let component: StreamerRedirectComponent;
  let fixture: ComponentFixture<StreamerRedirectComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [StreamerRedirectComponent]
    });
    fixture = TestBed.createComponent(StreamerRedirectComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
