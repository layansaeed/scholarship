package com.example.beans.controller;

import com.example.beans.service.job.JobDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job")
public class JobController {

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

    ////    //list of audit (sort strategy depend on priority column in job table)
//    @PostMapping()
//    public ResponseEntity<?> runJobsByAuditIds1(@RequestBody List<Long> auditIds, //{"auditIds": [2, 15]}
//
//     @PathVariable("nin") Long nin) {
//        try {
//            jobService.runJobsByAuditIds(auditIds,nin);
//            return ResponseEntity.ok().build();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            return ResponseEntity.badRequest().body(ex.getMessage());
//        }
//    }

    //[2, 15]
    @PostMapping("/forAll")
    public ResponseEntity<?> runJobsByAuditIds2(@RequestBody List<Long> auditIds ) {
        try {
        jobService.runJobsByAuditIds(auditIds);
        return ResponseEntity.ok().build();
    } catch (Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}


//POST /api/job/run/param?audit_ids=5,2,9
//POST /api/job/run/param?audit_ids=5&audit_ids=2&audit_ids=9 -> is safer
//@PostMapping("/{nin}")
//public ResponseEntity<?> runJobsByAuditIds(@RequestParam("audit_ids") List<Long> auditIds,
//                                           @PathVariable("nin") Long nin) {
//    try {
//        jobService.runJobsByAuditIds(auditIds,nin);
//        return ResponseEntity.ok().build();
//    } catch (Exception ex) {
//        ex.printStackTrace();
//        return ResponseEntity.badRequest().body(ex.getMessage());
//    }
//}
}