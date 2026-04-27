package com.nexuslearn.api.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "assignment_submissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "user_id"}) // [cite: 1159]
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentSubmission extends BaseEntity{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "submission_text", columnDefinition = "TEXT")
    private String submissionText;

    @Column(name = "score")
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String feedback;
}
