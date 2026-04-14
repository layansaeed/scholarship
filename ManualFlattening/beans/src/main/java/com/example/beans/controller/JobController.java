package com.example.beans.controller;

import com.example.beans.model.JobExecutorBatchRequest;
import com.example.beans.service.job.JobExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/v1/job-executor")
public class JobController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private final JobExecutionService jobExecutionService;
    public JobController(JobExecutionService jobExecutionService) {
        this.jobExecutionService = jobExecutionService;
    }


//    @PostMapping("/run/{nin}")
//    public ResponseEntity<?> runJobByAuditId(@RequestParam("audit_id") Long auditId,
//                                             @PathVariable("nin") Long nin) {
//        try {
//            jobService.runJobByAuditId(auditId,nin);
//            return ResponseEntity.ok().build();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            return ResponseEntity.badRequest().body(ex.getMessage());
//        }
//    }
@PostMapping()
public ResponseEntity<?> runJobs(@RequestBody JobExecutorBatchRequest request) {
    try {
        System.out.println("job list size: " + request.getJobs().size());
        jobExecutionService.runJobsByExecutionIds(request);
        return ResponseEntity.ok().build();
    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}

        @GetMapping("/alive")
        public ResponseEntity<?> alive() {
            logger.info(" alive");
//        String ip = request.getRemoteAddr();
//        int port = request.getRemotePort();
            logger.info("Alive check called ");
            return ResponseEntity.ok().build();
        }

    /**
     * New endpoint:
     * executes one full job using only jobId from path variable
     */
    @PostMapping("/run/{jobId}")
    public ResponseEntity<?> runFullJob(@PathVariable Long jobId) {
        try {
            jobExecutionService.runFullJobByJobId(jobId);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

}