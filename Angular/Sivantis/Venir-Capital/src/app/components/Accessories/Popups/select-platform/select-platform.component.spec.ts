import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SelectPlatformComponent } from './select-platform.component';

describe('SelectPlatformComponent', () => {
  let component: SelectPlatformComponent;
  let fixture: ComponentFixture<SelectPlatformComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [SelectPlatformComponent]
    });
    fixture = TestBed.createComponent(SelectPlatformComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
