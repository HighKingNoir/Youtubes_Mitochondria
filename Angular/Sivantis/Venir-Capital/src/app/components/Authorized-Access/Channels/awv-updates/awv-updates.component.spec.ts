import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AwvUpdatesComponent } from './awv-updates.component';

describe('AwvUpdatesComponent', () => {
  let component: AwvUpdatesComponent;
  let fixture: ComponentFixture<AwvUpdatesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ AwvUpdatesComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AwvUpdatesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
