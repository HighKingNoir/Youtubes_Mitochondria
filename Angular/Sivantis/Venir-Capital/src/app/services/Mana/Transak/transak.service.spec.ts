import { TestBed } from '@angular/core/testing';

import { TransakService } from './transak.service';

describe('TransakService', () => {
  let service: TransakService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TransakService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
