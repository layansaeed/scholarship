package sa.nrd.job.execute.repository;

import sa.nrd.job.execute.model.JobExecutionAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobExecutionAuditJpaRepository extends JpaRepository<JobExecutionAuditEntity, Long> {
}