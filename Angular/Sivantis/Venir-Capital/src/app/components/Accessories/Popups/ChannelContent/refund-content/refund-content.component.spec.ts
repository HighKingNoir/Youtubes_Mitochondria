import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RefundContentComponent } from './refund-content.component';

describe('RefundContentComponent', () => {
  let component: RefundContentComponent;
  let fixture: ComponentFixture<RefundContentComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ RefundContentComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RefundContentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
