import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CompleteVideoComponent } from './complete-video.component';

describe('CompleteVideoComponent', () => {
  let component: CompleteVideoComponent;
  let fixture: ComponentFixture<CompleteVideoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CompleteVideoComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CompleteVideoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
