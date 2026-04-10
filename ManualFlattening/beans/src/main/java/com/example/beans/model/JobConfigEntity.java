package com.example.beans.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "JOB_CONFIG", schema = "EXT")
public class JobConfigEntity {

    @EmbeddedId
    private JobConfigId id;

    @Column(name = "[VALUE]")
    private String configValue;

    public Long getJobId() {
        return id != null ? id.getJobId() : null;
    }

    public String getConfigKey() {
        return id != null ? id.getKey() : null;
    }
}