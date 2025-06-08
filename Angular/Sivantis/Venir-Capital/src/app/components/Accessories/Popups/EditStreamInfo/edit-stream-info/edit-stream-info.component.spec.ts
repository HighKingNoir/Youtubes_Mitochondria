import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditStreamInfoComponent } from './edit-stream-info.component';

describe('EditStreamInfoComponent', () => {
  let component: EditStreamInfoComponent;
  let fixture: ComponentFixture<EditStreamInfoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditStreamInfoComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EditStreamInfoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
