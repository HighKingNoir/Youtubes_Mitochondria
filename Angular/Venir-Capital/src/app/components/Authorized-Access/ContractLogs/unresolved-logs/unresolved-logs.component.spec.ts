import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UnresolvedLogsComponent } from './unresolved-logs.component';

describe('UnresolvedLogsComponent', () => {
  let component: UnresolvedLogsComponent;
  let fixture: ComponentFixture<UnresolvedLogsComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [UnresolvedLogsComponent]
    });
    fixture = TestBed.createComponent(UnresolvedLogsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
