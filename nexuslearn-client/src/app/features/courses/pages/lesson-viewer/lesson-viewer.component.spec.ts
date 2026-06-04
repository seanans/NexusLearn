import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LessonViewerComponent } from './lesson-viewer.component';

describe('LessonViewerComponent', () => {
  let component: LessonViewerComponent;
  let fixture: ComponentFixture<LessonViewerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LessonViewerComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LessonViewerComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
