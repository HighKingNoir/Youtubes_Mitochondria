import { TestBed } from '@angular/core/testing';

import { WarchestService } from './warchest.service';

describe('WarchestServiceService', () => {
  let service: WarchestService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(WarchestService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
