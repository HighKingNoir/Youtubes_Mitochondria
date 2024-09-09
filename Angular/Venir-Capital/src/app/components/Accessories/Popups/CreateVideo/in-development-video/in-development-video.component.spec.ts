import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InDevelopmentVideoComponent } from './in-development-video.component';

describe('InDevelopmentVideoComponent', () => {
  let component: InDevelopmentVideoComponent;
  let fixture: ComponentFixture<InDevelopmentVideoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ InDevelopmentVideoComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InDevelopmentVideoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
