import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterOutlet, RouterLink } from '@angular/router';
import { AsyncPipe } from '@angular/common';
import { Observable, switchMap } from 'rxjs';
import { CourseService } from '../../services/course.service';
import { CourseResponse, CourseRole } from '../../models/course.models';

@Component({
  selector: 'app-course-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, AsyncPipe],
  templateUrl: './course-layout.component.html',
  styleUrls: ['./course-layout.component.scss']
})
export class CourseLayoutComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private courseService = inject(CourseService);

  course$!: Observable<CourseResponse>;
  CourseRole = CourseRole;

  ngOnInit(): void {
    this.course$ = this.route.paramMap.pipe(
      switchMap(params => this.courseService.getCourseById(params.get('id')!))
    );
  }
}
