import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FirstVideosComponent } from './first-videos.component';

describe('FirstVideosComponent', () => {
  let component: FirstVideosComponent;
  let fixture: ComponentFixture<FirstVideosComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [FirstVideosComponent]
    });
    fixture = TestBed.createComponent(FirstVideosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
