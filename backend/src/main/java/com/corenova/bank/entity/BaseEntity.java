package com.corenova.bank.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ================================================================
 *  BaseEntity – Abstract Auditable Base
 *
 *  All CoreNova Bank entities extend this class to inherit:
 *    • Auto-populated createdAt / updatedAt timestamps
 *    • Auto-populated createdBy / updatedBy from Security Context
 *    • Consistent JPA auditing via @EntityListeners
 *
 *  This mirrors Finacle's internal entity audit pattern for
 *  regulatory traceability and compliance reporting.
 * ================================================================
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * Timestamp when the record was first created.
     * Set once on insert — never updated thereafter.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp of the last modification to this record.
     * Updated automatically on every save/merge.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Username / Employee ID of the user who created this record.
     * Populated from Spring Security context.
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    /**
     * Username / Employee ID of the user who last modified this record.
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
