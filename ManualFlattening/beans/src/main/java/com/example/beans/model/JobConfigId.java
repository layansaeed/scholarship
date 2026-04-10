package com.example.beans.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class JobConfigId implements Serializable {

    @Column(name = "JOB_ID")
    private Long jobId;

    @Column(name = "[KEY]")
    private String key;

    public JobConfigId(Long jobId, String key) {
        this.jobId = jobId;
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobConfigId)) return false;
        JobConfigId that = (JobConfigId) o;
        return Objects.equals(jobId, that.jobId)
                && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, key);
    }
}