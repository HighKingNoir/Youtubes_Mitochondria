import { TestBed } from '@angular/core/testing';

import { HandleReportService } from './handle-report.service';

describe('HandleReportService', () => {
  let service: HandleReportService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(HandleReportService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
