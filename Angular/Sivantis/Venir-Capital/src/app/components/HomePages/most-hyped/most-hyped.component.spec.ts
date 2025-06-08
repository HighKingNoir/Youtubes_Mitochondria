import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MostHypedComponent } from './most-hyped.component';

describe('MostHypedComponent', () => {
  let component: MostHypedComponent;
  let fixture: ComponentFixture<MostHypedComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [MostHypedComponent]
    });
    fixture = TestBed.createComponent(MostHypedComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
