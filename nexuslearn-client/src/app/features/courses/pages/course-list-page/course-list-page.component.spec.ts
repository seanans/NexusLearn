import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseListPageComponent } from './course-list-page.component';

describe('CourseListPageComponent', () => {
  let component: CourseListPageComponent;
  let fixture: ComponentFixture<CourseListPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CourseListPageComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CourseListPageComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
