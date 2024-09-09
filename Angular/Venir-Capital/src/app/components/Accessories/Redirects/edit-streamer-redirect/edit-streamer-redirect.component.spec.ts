import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditStreamerRedirectComponent } from './edit-streamer-redirect.component';

describe('EditStreamerRedirectComponent', () => {
  let component: EditStreamerRedirectComponent;
  let fixture: ComponentFixture<EditStreamerRedirectComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [EditStreamerRedirectComponent]
    });
    fixture = TestBed.createComponent(EditStreamerRedirectComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
