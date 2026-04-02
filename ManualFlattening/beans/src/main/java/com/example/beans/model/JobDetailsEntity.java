package com.example.beans.model;

import com.example.beans.constant.IntegrationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "JOB_DETAILS", schema = "EXT")
public class JobDetailsEntity {

    @Id
    @Column(name = "JOB_ID")
    private Long jobId;

    @Column(name = "JOB_NAME")
    @Enumerated(EnumType.STRING)
    private IntegrationType jobName;

    @Column(name = "JOB_DESC")
    private String jobDesc;

    @Column(name = "SERVICE_NAME")
    private String serviceName;

    @Column(name = "FREQUENCY_ID")
    private Integer frequencyId;

    @Column(name = "TEMP_TABLE")
    private String tempTable;

    @Column(name = "PRIORITY")
    private Integer priority;

    @Column(name = "INPUTS")
    private String inputs;

    @Column(name = "LAST_JOB_DATE")
    private LocalDateTime lastJobDate;

    @Column(name = "ACTIVE")
    private Boolean active;
}