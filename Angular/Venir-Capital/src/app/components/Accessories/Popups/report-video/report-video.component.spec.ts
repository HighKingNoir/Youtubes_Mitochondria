import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReportVideoComponent } from './report-video.component';

describe('ReportVideoComponent', () => {
  let component: ReportVideoComponent;
  let fixture: ComponentFixture<ReportVideoComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ReportVideoComponent]
    });
    fixture = TestBed.createComponent(ReportVideoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
