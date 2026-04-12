package com.example.beans.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "JOB_EXECUTION", schema = "EXT")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionEntity {

    @Id
    @Column(name = "EXECUTION_ID")
    private Long executionId;

    @Column(name = "JOB_ID")
    private Long jobId;

    @Column(name = "SERVER_ID")
    private Long serverId;

    @Column(name = "NIN_RANGE_START")
    private Long ninRangeStart;

    @Column(name = "NIN_RANGE_END")
    private Long ninRangeEnd;

    @Column(name = "JOB_NAME")
    private String jobName;
}