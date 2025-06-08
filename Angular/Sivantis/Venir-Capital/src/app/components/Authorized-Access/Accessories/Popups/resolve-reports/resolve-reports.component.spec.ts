import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResolveReportsComponent } from './resolve-reports.component';

describe('ResolveReportsComponent', () => {
  let component: ResolveReportsComponent;
  let fixture: ComponentFixture<ResolveReportsComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ResolveReportsComponent]
    });
    fixture = TestBed.createComponent(ResolveReportsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
