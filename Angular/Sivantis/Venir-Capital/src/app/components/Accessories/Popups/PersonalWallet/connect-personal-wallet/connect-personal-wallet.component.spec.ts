import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConnectPersonalWalletComponent } from './connect-personal-wallet.component';

describe('ConnectPersonalWalletComponent', () => {
  let component: ConnectPersonalWalletComponent;
  let fixture: ComponentFixture<ConnectPersonalWalletComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ConnectPersonalWalletComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConnectPersonalWalletComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
