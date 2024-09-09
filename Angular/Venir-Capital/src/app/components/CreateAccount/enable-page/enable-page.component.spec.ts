import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EnablePageComponent } from './enable-page.component';

describe('EnablePageComponent', () => {
  let component: EnablePageComponent;
  let fixture: ComponentFixture<EnablePageComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [EnablePageComponent]
    });
    fixture = TestBed.createComponent(EnablePageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
