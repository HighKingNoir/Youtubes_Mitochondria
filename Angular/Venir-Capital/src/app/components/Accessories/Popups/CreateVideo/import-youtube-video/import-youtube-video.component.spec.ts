import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ImportYoutubeVideoComponent } from './import-youtube-video.component';

describe('ImportYoutubeVideoComponent', () => {
  let component: ImportYoutubeVideoComponent;
  let fixture: ComponentFixture<ImportYoutubeVideoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ImportYoutubeVideoComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ImportYoutubeVideoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
