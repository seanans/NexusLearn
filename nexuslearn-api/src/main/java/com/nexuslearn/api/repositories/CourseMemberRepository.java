package com.nexuslearn.api.repositories;

import com.nexuslearn.api.models.CourseMember;
import com.nexuslearn.api.models.CourseMemberId;
import com.nexuslearn.api.models.CourseRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CourseMemberRepository extends JpaRepository<CourseMember, CourseMemberId> {
    @Query("SELECT cm.role FROM CourseMember cm WHERE cm.course.id = :courseId AND cm.user.id = :userId")
    Optional<CourseRole> getRoleById(@Param("courseId") UUID courseId, @Param("userId") UUID userId);
}