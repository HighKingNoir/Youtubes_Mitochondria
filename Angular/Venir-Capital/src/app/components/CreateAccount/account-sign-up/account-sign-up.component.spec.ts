import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AccountSignUpComponent } from './account-sign-up.component';

describe('AccountSignUpComponent', () => {
  let component: AccountSignUpComponent;
  let fixture: ComponentFixture<AccountSignUpComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ AccountSignUpComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AccountSignUpComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
