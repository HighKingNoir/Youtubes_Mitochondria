import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MessageMainPageComponent } from './message-main-page.component';

describe('MessageMainPageComponent', () => {
  let component: MessageMainPageComponent;
  let fixture: ComponentFixture<MessageMainPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ MessageMainPageComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MessageMainPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
