package com.nexuslearn.api.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "channels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Channel extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ChannelType type;

    @Column
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "reference_id")
    private UUID referenceId;

    @OneToMany(mappedBy = "channel", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChannelMember> members = new ArrayList<>();
}
