package com.nexuslearn.api.repositories;

import com.nexuslearn.api.models.CourseMember;
import com.nexuslearn.api.models.CourseMemberId;
import com.nexuslearn.api.models.CourseRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseMemberRepository extends JpaRepository<CourseMember, CourseMemberId> {
    Optional<CourseRole> getRoleById(CourseMemberId courseMemberId);
}