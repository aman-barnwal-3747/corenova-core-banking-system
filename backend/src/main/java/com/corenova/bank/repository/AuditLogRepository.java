package com.corenova.bank.repository;

import com.corenova.bank.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByPerformedByOrderByPerformedAtDesc(String performedBy, Pageable pageable);
    Page<AuditLog> findByActionOrderByPerformedAtDesc(String action, Pageable pageable);
    Page<AuditLog> findByEntityTypeAndEntityIdOrderByPerformedAtDesc(String entityType, String entityId, Pageable pageable);
    Page<AuditLog> findByPerformedAtBetweenOrderByPerformedAtDesc(LocalDateTime from, LocalDateTime to, Pageable pageable);
    long countByPerformedAtAfter(LocalDateTime from);
}
