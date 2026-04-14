package com.example.beans.model;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Table(name = "JOB_DETAILS", schema = "EXT")
@Data
public class JobDetailsEntity {

    @Id
    @Column(name = "JOB_ID")
    private Long jobId;

    @Column(name = "JOB_NAME")
    private String jobName;
}