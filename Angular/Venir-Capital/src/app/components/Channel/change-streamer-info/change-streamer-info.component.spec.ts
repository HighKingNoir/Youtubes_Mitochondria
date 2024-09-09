import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChangeStreamerInfoComponent } from './change-streamer-info.component';

describe('ChangeStreamerInfoComponent', () => {
  let component: ChangeStreamerInfoComponent;
  let fixture: ComponentFixture<ChangeStreamerInfoComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ChangeStreamerInfoComponent]
    });
    fixture = TestBed.createComponent(ChangeStreamerInfoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
