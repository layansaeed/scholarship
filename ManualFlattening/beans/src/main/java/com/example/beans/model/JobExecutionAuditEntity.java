package com.example.beans.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "JOB_EXECUTION_AUDIT", schema = "EXT")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AUDIT_ID")
    private Long auditId;

    /**
     * Many audit rows have one status from JOB_EXECUTION_STATUS
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "STATUS", referencedColumnName = "STATUS", nullable = false, foreignKey = @ForeignKey(name = "FK_AUDIT_STATUS"))
    private JobExecutionStatus status;

    @Column(name = "END_TIME")
    private LocalDateTime endTime;

}
