import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContractFunctionDetailsComponent } from './contract-function-details.component';

describe('ContractFunctionDetailsComponent', () => {
  let component: ContractFunctionDetailsComponent;
  let fixture: ComponentFixture<ContractFunctionDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContractFunctionDetailsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ContractFunctionDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
