package com.example.beans.model;


import lombok.Data;

import java.util.List;

@Data
public class JobExecutorBatchRequest {
  private   List<JobExecutorRequest> jobs ;
}
