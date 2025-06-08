import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CryptoWalletPageComponent } from './crypto-wallet-page.component';

describe('CryptoWalletPageComponent', () => {
  let component: CryptoWalletPageComponent;
  let fixture: ComponentFixture<CryptoWalletPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CryptoWalletPageComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CryptoWalletPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
