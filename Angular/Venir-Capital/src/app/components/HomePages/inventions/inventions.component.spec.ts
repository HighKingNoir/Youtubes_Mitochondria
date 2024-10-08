import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InventionsComponent } from './inventions.component';

describe('InventionsComponent', () => {
  let component: InventionsComponent;
  let fixture: ComponentFixture<InventionsComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [InventionsComponent]
    });
    fixture = TestBed.createComponent(InventionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
