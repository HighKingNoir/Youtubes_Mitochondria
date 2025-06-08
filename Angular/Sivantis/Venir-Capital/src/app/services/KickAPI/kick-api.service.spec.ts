import { TestBed } from '@angular/core/testing';

import { KickApiService } from './kick-api.service';

describe('KickApiService', () => {
  let service: KickApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(KickApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
