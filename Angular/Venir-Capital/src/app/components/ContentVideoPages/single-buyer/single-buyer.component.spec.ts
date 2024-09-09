import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SingleBuyerComponent } from './single-buyer.component';

describe('SingleBuyerComponent', () => {
  let component: SingleBuyerComponent;
  let fixture: ComponentFixture<SingleBuyerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SingleBuyerComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SingleBuyerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
