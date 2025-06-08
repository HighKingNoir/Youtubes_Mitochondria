import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FailedLogsComponent } from './failed-logs.component';

describe('FailedLogsComponent', () => {
  let component: FailedLogsComponent;
  let fixture: ComponentFixture<FailedLogsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ FailedLogsComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FailedLogsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
