import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddStreamerRedirectComponent } from './add-streamer-redirect.component';

describe('AddStreamerRedirectComponent', () => {
  let component: AddStreamerRedirectComponent;
  let fixture: ComponentFixture<AddStreamerRedirectComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AddStreamerRedirectComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AddStreamerRedirectComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
