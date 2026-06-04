import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseSettingsComponent } from './course-settings.component';

describe('CourseSettingsComponent', () => {
  let component: CourseSettingsComponent;
  let fixture: ComponentFixture<CourseSettingsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CourseSettingsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CourseSettingsComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
