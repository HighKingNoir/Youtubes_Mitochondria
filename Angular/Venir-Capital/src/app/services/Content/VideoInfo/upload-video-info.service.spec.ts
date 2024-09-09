import { TestBed } from '@angular/core/testing';

import { UploadVideoInfoService } from './upload-video-info.service';

describe('UploadVideoInfoService', () => {
  let service: UploadVideoInfoService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(UploadVideoInfoService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
