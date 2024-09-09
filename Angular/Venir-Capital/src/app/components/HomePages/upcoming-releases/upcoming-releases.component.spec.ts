import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UpcomingReleasesComponent } from './upcoming-releases.component';

describe('UpcomingReleasesComponent', () => {
  let component: UpcomingReleasesComponent;
  let fixture: ComponentFixture<UpcomingReleasesComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [UpcomingReleasesComponent]
    });
    fixture = TestBed.createComponent(UpcomingReleasesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
