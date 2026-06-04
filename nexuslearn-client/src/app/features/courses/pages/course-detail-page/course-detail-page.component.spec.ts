import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseDetailPageComponent } from './course-detail-page.component';

describe('CourseDetailPageComponent', () => {
  let component: CourseDetailPageComponent;
  let fixture: ComponentFixture<CourseDetailPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CourseDetailPageComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CourseDetailPageComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
