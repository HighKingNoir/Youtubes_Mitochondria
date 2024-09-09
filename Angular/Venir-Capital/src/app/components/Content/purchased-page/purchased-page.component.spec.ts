import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PurchasedPageComponent } from './purchased-page.component';

describe('PurchasedPageComponent', () => {
  let component: PurchasedPageComponent;
  let fixture: ComponentFixture<PurchasedPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ PurchasedPageComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PurchasedPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
