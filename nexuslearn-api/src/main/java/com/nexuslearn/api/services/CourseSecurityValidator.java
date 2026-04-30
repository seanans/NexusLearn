package com.nexuslearn.api.services;

import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.CourseMember;
import com.nexuslearn.api.models.CourseMemberId;
import com.nexuslearn.api.models.CourseRole;
import com.nexuslearn.api.models.User;
import com.nexuslearn.api.repositories.CourseMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CourseSecurityValidator {

    private final CourseMemberRepository courseMemberRepository;

    public void validateAccess(UUID courseId, User user, boolean requireElevatedPrivileges) {
        CourseMemberId courseMemberId = new CourseMemberId(user.getId(), courseId);

        CourseMember courseMember = courseMemberRepository.findById(courseMemberId).orElseThrow(() -> new AppException("Access Denied: You are not a member of this course", HttpStatus.FORBIDDEN));
        if (requireElevatedPrivileges) {
            CourseRole courseRole = courseMember.getRole();
            if (courseRole != CourseRole.TEACHER && courseRole != CourseRole.ASSISTANT) {
                throw new AppException("Access Denied: Insufficient privileges", HttpStatus.FORBIDDEN);
            }
        }
    }

    public CourseRole getUserRoleInCourse(UUID courseId, User user) {
        CourseMemberId courseMemberId = new CourseMemberId(user.getId(), courseId);
        return courseMemberRepository.getRoleById(courseId, user.getId()).orElseThrow(() -> new AppException("You are not a member of this course", HttpStatus.FORBIDDEN));
    }
}
