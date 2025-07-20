package com.elpandor.hlh.common.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(
        value = {"dateCreation", "dateModification"},
        allowGetters = true
)
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public abstract class AuditModel {

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_creation", updatable = false)
    @CreatedDate
    private Instant dateCreation;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_modification")
    @LastModifiedDate
    private Instant dateModification;

    @CreatedBy
    @Size(max = 50)
    @Column(name = "userid", length = 50, updatable = false)
    protected String createdBy;

    @LastModifiedBy
    @Size(max = 50)
    @Column(name = "modify_by", length = 50)
    protected String lastModifiedBy;
}

