import { TestBed } from '@angular/core/testing';

import { ChannelServiceContract } from './channel-service-contract.service';

describe('ChannelServiceContractService', () => {
  let service: ChannelServiceContract;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ChannelServiceContract);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
