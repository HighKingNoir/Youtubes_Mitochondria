import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GoogleLoginRedirectComponent } from './google-login-redirect.component';

describe('GoogleLoginRedirectComponent', () => {
  let component: GoogleLoginRedirectComponent;
  let fixture: ComponentFixture<GoogleLoginRedirectComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [GoogleLoginRedirectComponent]
    });
    fixture = TestBed.createComponent(GoogleLoginRedirectComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
