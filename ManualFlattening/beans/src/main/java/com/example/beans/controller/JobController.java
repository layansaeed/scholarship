package com.example.beans.controller;

import com.example.beans.model.JobExecutorBatchRequest;
import com.example.beans.service.job.JobDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/v1/job-executor")
public class JobController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private final JobDetailsService jobService;

    public JobController(JobDetailsService jobService) {
        this.jobService = jobService;
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
    public ResponseEntity<?> runJobsByAuditIds2(@RequestBody JobExecutorBatchRequest auditIds ) {
        try {
            System.out.println("audit id list size: " + auditIds.getJobs().size());
            System.out.println("audit id list iterator: " + auditIds.getJobs().listIterator().toString());

            jobService.runJobsByAuditIds(auditIds);
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

}