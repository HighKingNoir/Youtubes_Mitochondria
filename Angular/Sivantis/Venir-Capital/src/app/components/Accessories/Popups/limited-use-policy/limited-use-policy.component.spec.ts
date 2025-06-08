import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LimitedUsePolicyComponent } from './limited-use-policy.component';

describe('LimitedUsePolicyComponent', () => {
  let component: LimitedUsePolicyComponent;
  let fixture: ComponentFixture<LimitedUsePolicyComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [LimitedUsePolicyComponent]
    });
    fixture = TestBed.createComponent(LimitedUsePolicyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
