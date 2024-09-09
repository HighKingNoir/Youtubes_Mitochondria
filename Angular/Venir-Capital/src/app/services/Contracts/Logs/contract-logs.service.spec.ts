import { TestBed } from '@angular/core/testing';

import { ContractLogsService } from './contract-logs.service';

describe('ContractLogsService', () => {
  let service: ContractLogsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ContractLogsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
