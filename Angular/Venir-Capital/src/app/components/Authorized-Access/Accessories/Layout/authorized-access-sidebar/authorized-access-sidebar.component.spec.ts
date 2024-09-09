import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AuthorizedAccessSidebarComponent } from './authorized-access-sidebar.component';

describe('AuthorizedAccessSidebarComponent', () => {
  let component: AuthorizedAccessSidebarComponent;
  let fixture: ComponentFixture<AuthorizedAccessSidebarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ AuthorizedAccessSidebarComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AuthorizedAccessSidebarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
