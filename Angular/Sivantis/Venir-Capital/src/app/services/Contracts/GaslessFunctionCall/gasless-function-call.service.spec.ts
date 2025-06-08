import { TestBed } from '@angular/core/testing';

import { GaslessFunctionCallService } from './gasless-function-call.service';

describe('GaslessFunctionCallService', () => {
  let service: GaslessFunctionCallService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GaslessFunctionCallService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
