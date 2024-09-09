import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AuthorizedUserHomeComponent } from './authorized-user-home.component';

describe('AuthorizedUserHomeComponent', () => {
  let component: AuthorizedUserHomeComponent;
  let fixture: ComponentFixture<AuthorizedUserHomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ AuthorizedUserHomeComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AuthorizedUserHomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
