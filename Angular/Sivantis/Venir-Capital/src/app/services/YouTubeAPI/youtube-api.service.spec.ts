import { TestBed } from '@angular/core/testing';

import { YoutubeAPIService } from './youtube-api.service';

describe('YoutubeAPIService', () => {
  let service: YoutubeAPIService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(YoutubeAPIService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
