import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CoursePeopleComponent } from './course-people.component';

describe('CoursePeopleComponent', () => {
  let component: CoursePeopleComponent;
  let fixture: ComponentFixture<CoursePeopleComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CoursePeopleComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(CoursePeopleComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
