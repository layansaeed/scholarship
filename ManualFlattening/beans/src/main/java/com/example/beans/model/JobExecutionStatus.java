package com.example.beans.model;

import com.example.beans.constant.ExecutionStatus;
import lombok.Data;

import jakarta.persistence.*;

@Entity
@Table(name = "JOB_EXECUTION_STATUS", schema = "EXT")
@Data
public class JobExecutionStatus {
    @Id
    @Column(name = "STATUS")
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;

    @Column(name = "DESCRIPTION")
    private String description;

}
 