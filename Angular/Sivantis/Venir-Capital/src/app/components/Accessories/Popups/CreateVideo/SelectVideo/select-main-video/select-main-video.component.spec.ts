import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SelectMainVideoComponent } from './select-main-video.component';

describe('SelectMainVideoComponent', () => {
  let component: SelectMainVideoComponent;
  let fixture: ComponentFixture<SelectMainVideoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SelectMainVideoComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SelectMainVideoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
