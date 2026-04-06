package com.example.beans.repository;

import com.example.beans.constant.ExecutionStatus;
import com.example.beans.model.JobExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobExecutionStatusRepository extends JpaRepository<JobExecutionStatus, ExecutionStatus> {
    JobExecutionStatus findByStatus(ExecutionStatus status);
}
 