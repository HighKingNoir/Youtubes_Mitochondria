import { TestBed } from '@angular/core/testing';

import { EditChannelService } from './edit-channel.service';

describe('EditChannelService', () => {
  let service: EditChannelService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EditChannelService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
