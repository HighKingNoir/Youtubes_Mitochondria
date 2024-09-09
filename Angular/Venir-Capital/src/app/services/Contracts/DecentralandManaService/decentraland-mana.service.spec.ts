import { TestBed } from '@angular/core/testing';

import { DecentralandManaService } from './decentraland-mana.service';

describe('DecentralandManaService', () => {
  let service: DecentralandManaService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DecentralandManaService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
