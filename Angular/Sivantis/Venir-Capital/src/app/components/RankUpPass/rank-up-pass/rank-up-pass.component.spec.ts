import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RankUpPassComponent } from './rank-up-pass.component';

describe('RankUpPassComponent', () => {
  let component: RankUpPassComponent;
  let fixture: ComponentFixture<RankUpPassComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RankUpPassComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RankUpPassComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
