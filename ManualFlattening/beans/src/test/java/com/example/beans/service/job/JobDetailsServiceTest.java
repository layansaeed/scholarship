package com.example.beans.service.job;

import com.example.beans.model.JobDetailsEntity;
import com.example.beans.repository.JobDetailsJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

//This tells JUnit to enable Mockito in this test class(@Mock/@InjectMocks)
@ExtendWith(MockitoExtension.class)
class JobDetailsServiceTest {
//This creates a fake version of the repository->mock object->not the real DB repository
    @Mock
    private JobDetailsJpaRepository jobDetailsRepository;

    //This creates the service and injects the mocked repository into it.
    @InjectMocks
    private JobDetailsService jobDetailsService;

    @Test
    void getJobRequired_shouldReturnJobDetails_whenJobExists() {
        //prepare fake data
        Long jobId = 1L;

        //Create fake returned entity
        JobDetailsEntity jobDetailsEntity = new JobDetailsEntity();
        jobDetailsEntity.setJobId(jobId);
        jobDetailsEntity.setJobName("HRSD_DIS_ASS");

        //Tell mock repository what to return -> object of job details entity
        when(jobDetailsRepository.findById(jobId)).thenReturn(Optional.of(jobDetailsEntity));
        //method name that will be tested
        JobDetailsEntity result = jobDetailsService.getJobRequired(jobId);

        assertNotNull(result);
        assertEquals(jobId, result.getJobId());
        assertEquals("HRSD_DIS_ASS", result.getJobName());
    }

    @Test
    void getJobRequired_shouldThrowException_whenJobDoesNotExist() {
        Long jobId = 99L;

        //Tell mock repository to return empty
        when(jobDetailsRepository.findById(jobId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jobDetailsService.getJobRequired(jobId)
        );

        assertEquals("Job not found for id: 99", exception.getMessage());
    }
    //The remaining red text is only a Mockito warning
    // related to dynamic Java agent loading on Java 21, not a test failure
}