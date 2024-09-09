import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BrowseChannelComponent } from './browse-channel.component';

describe('BrowseChannelComponent', () => {
  let component: BrowseChannelComponent;
  let fixture: ComponentFixture<BrowseChannelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [BrowseChannelComponent]
    });
    fixture = TestBed.createComponent(BrowseChannelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
