import { TestBed } from '@angular/core/testing';

import { PayToRankUpServiceService } from './pay-to-rank-up-service.service';

describe('PayToRankUpServiceService', () => {
  let service: PayToRankUpServiceService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PayToRankUpServiceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
