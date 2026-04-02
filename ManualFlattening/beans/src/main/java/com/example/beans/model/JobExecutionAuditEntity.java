package com.example.beans.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "JOB_EXECUTION_AUDIT", schema = "EXT")
public class JobExecutionAuditEntity {

    @Id
    @Column(name = "AUDIT_ID")
    private Long auditId;

    @Column(name = "JOB_ID")
    private Long jobId;

    @Column(name = "SERVER_ID")
    private Long serverId;

    @Column(name = "NIN_RANGE_START")
    private Long ninRangeStart;

    @Column(name = "NIN_RANGE_END")
    private Long ninRangeEnd;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "START_TIME")
    private LocalDateTime startTime;

    @Column(name = "END_TIME")
    private LocalDateTime endTime;

    @Column(name = "RECORD_COUNT")
    private Long recordCount;

    @Column(name = "CONSUMED_BY")
    private String consumedBy;
}