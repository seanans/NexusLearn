package com.nexuslearn.api.repositories;

import com.nexuslearn.api.models.Attachment;
import com.nexuslearn.api.models.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    List<Attachment> findByEntityIdAndEntityType(UUID entityId, EntityType entityType);
    List<Attachment> findByEntityIdInAndEntityType(List<UUID> entityIds, EntityType entityType);
    void deleteByEntityIdAndEntityType(UUID entityId, EntityType entityType);
}