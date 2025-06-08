import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TransferManaComponent } from './transfer-mana.component';

describe('TransferManaComponent', () => {
  let component: TransferManaComponent;
  let fixture: ComponentFixture<TransferManaComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TransferManaComponent]
    });
    fixture = TestBed.createComponent(TransferManaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
