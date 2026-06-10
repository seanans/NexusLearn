import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseChatComponent } from './course-chat.component';

describe('CourseChatComponent', () => {
  let component: CourseChatComponent;
  let fixture: ComponentFixture<CourseChatComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CourseChatComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CourseChatComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
