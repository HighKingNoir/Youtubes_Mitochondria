import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TransakPopupComponent } from './transak-popup.component';

describe('TransakPopupComponent', () => {
  let component: TransakPopupComponent;
  let fixture: ComponentFixture<TransakPopupComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransakPopupComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(TransakPopupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
