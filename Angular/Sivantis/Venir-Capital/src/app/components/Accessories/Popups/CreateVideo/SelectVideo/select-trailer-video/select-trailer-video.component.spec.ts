import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SelectTrailerVideoComponent } from './select-trailer-video.component';

describe('SelectTrailerVideoComponent', () => {
  let component: SelectTrailerVideoComponent;
  let fixture: ComponentFixture<SelectTrailerVideoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SelectTrailerVideoComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SelectTrailerVideoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
