
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WarchestWithdrawComponent } from './warchest-withdraw.component';

describe('WarchestWithdrawComponent', () => {
  let component: WarchestWithdrawComponent;
  let fixture: ComponentFixture<WarchestWithdrawComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ WarchestWithdrawComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(WarchestWithdrawComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
