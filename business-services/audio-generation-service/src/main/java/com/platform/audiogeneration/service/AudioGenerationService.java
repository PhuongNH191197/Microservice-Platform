package com.platform.audiogeneration.service;

import com.platform.audiogeneration.client.AiMediaWorkerClient;
import com.platform.audiogeneration.dto.request.GenerateAudioRequest;
import com.platform.audiogeneration.dto.response.AudioJobResponse;
import com.platform.audiogeneration.entity.AudioJob;
import com.platform.audiogeneration.entity.AudioJob.JobStatus;
import com.platform.audiogeneration.repository.AudioJobRepository;
import com.platform.common.core.exception.BaseException;
import com.platform.common.core.exception.CommonErrorCode;
import com.platform.common.rmq.RmqExchanges;
import com.platform.common.rmq.RmqRoutingKeys;
import com.platform.common.rmq.event.AudioGeneratedEvent;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AudioGenerationService {
    private static final Logger log = LoggerFactory.getLogger(AudioGenerationService.class);
    private static final int MAX_ACTIVE_JOBS = 5;

    private final AudioJobRepository jobRepository;
    private final AiMediaWorkerClient aiClient;
    private final RabbitTemplate rabbitTemplate;

    public AudioGenerationService(AudioJobRepository jobRepository,
                                  AiMediaWorkerClient aiClient,
                                  RabbitTemplate rabbitTemplate) {
        this.jobRepository = jobRepository;
        this.aiClient = aiClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public AudioJobResponse submitJob(Long userId, GenerateAudioRequest request) {
        long active = jobRepository.countActiveJobsByUserId(userId);
        if (active >= MAX_ACTIVE_JOBS) {
            throw new BaseException(CommonErrorCode.COMMON_BAD_REQUEST, "Max " + MAX_ACTIVE_JOBS + " active jobs");
        }
        AudioJob job = new AudioJob(userId, request.prompt(), request.voiceId());
        jobRepository.save(job);
        processJobAsync(job.getId());
        return toResponse(job);
    }

    @Async
    public void processJobAsync(Long jobId) {
        AudioJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.COMMON_NOT_FOUND));
        try {
            job.setStatus(JobStatus.PROCESSING);
            jobRepository.save(job);

            byte[] audio = aiClient.generateTts(Map.of(
                "text", job.getPrompt(),
                "voice", job.getVoiceId() != null ? job.getVoiceId() : "vi-VN-HoaiMyNeural",
                "output_format", "audio-24khz-48kbitrate-mono-mp3"
            ));

            String url = "minio://audio-bucket/" + jobId + ".mp3"; // placeholder; actual MinIO upload via FileService
            job.setResultUrl(url);
            job.setStatus(JobStatus.COMPLETED);
            jobRepository.save(job);

            publishEvent(job);
        } catch (Exception e) {
            log.error("Job {} failed", jobId, e);
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            jobRepository.save(job);
        }
    }

    public List<AudioJobResponse> getUserJobs(Long userId) {
        return jobRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toResponse).toList();
    }

    public AudioJobResponse getJob(Long jobId, Long userId) {
        AudioJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.COMMON_NOT_FOUND));
        if (!job.getUserId().equals(userId)) {
            throw new BaseException(CommonErrorCode.COMMON_FORBIDDEN);
        }
        return toResponse(job);
    }

    private void publishEvent(AudioJob job) {
        AudioGeneratedEvent event = new AudioGeneratedEvent(
            job.getUserId(), job.getId().toString(), job.getResultUrl(),
            job.getStatus().name(), System.currentTimeMillis());
        rabbitTemplate.convertAndSend(RmqExchanges.AUDIO_EVENTS, RmqRoutingKeys.AUDIO_GENERATED, event);
    }

    private AudioJobResponse toResponse(AudioJob job) {
        return new AudioJobResponse(job.getId(), job.getPrompt(), job.getVoiceId(),
            job.getStatus(), job.getResultUrl(), job.getErrorMessage(), job.getCreatedAt());
    }
}
