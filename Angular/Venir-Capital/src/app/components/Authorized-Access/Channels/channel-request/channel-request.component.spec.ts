import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChannelRequestComponent } from './channel-request.component';

describe('ChannelRequestComponent', () => {
  let component: ChannelRequestComponent;
  let fixture: ComponentFixture<ChannelRequestComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ChannelRequestComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChannelRequestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
