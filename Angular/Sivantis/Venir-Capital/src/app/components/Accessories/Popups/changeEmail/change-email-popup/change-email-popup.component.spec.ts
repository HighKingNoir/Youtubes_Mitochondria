import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChangeEmailPopupComponent } from './change-email-popup.component';

describe('ChangeEmailPopupComponent', () => {
  let component: ChangeEmailPopupComponent;
  let fixture: ComponentFixture<ChangeEmailPopupComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChangeEmailPopupComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChangeEmailPopupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
