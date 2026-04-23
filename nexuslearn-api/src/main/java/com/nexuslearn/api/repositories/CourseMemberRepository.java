package com.nexuslearn.api.repositories;

import com.nexuslearn.api.models.CourseMember;
import com.nexuslearn.api.models.CourseMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseMemberRepository extends JpaRepository<CourseMember, CourseMemberId> {
}