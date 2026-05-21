package com.platform.audiogeneration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.platform.audiogeneration.client.AiMediaWorkerClient;
import com.platform.audiogeneration.dto.request.GenerateAudioRequest;
import com.platform.audiogeneration.dto.response.AudioJobResponse;
import com.platform.audiogeneration.entity.AudioJob;
import com.platform.audiogeneration.entity.AudioJob.JobStatus;
import com.platform.audiogeneration.repository.AudioJobRepository;
import com.platform.common.core.exception.BaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class AudioGenerationServiceTest {
    @Mock AudioJobRepository jobRepository;
    @Mock AiMediaWorkerClient aiClient;
    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks AudioGenerationService service;

    @Test
    void submit_shouldRejectWhenLimitReached() {
        when(jobRepository.countActiveJobsByUserId(1L)).thenReturn(5L);
        assertThrows(BaseException.class,
            () -> service.submitJob(1L, new GenerateAudioRequest("hello", "vi-VN-HoaiMyNeural")));
    }

    @Test
    void submit_shouldPersistPendingJob() {
        when(jobRepository.countActiveJobsByUserId(1L)).thenReturn(0L);
        when(jobRepository.save(any(AudioJob.class))).thenAnswer(inv -> {
            AudioJob j = inv.getArgument(0);
            return j;
        });
        AudioJobResponse resp = service.submitJob(1L, new GenerateAudioRequest("hello", "vi-VN-HoaiMyNeural"));
        assertEquals(JobStatus.PENDING, resp.status());
        assertEquals("hello", resp.prompt());
    }
}
