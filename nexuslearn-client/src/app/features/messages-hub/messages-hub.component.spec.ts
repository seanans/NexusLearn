import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MessagesHubComponent } from './messages-hub.component';

describe('MessagesHubComponent', () => {
  let component: MessagesHubComponent;
  let fixture: ComponentFixture<MessagesHubComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MessagesHubComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(MessagesHubComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
