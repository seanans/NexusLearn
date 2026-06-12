import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MiniChatComponent } from './mini-chat.component';

describe('MiniChatComponent', () => {
  let component: MiniChatComponent;
  let fixture: ComponentFixture<MiniChatComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MiniChatComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(MiniChatComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
