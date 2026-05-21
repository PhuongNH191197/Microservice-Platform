package com.platform.crbtcoreadapter.service;

import com.platform.common.core.exception.BaseException;
import com.platform.common.core.exception.CommonErrorCode;
import com.platform.crbtcoreadapter.client.MytoneCmsClient;
import com.platform.crbtcoreadapter.client.MytoneCmsRequest;
import com.platform.crbtcoreadapter.client.MytoneCmsResponse;
import com.platform.crbtcoreadapter.dto.request.AssignRingtoneRequest;
import com.platform.crbtcoreadapter.dto.response.AssignmentResponse;
import com.platform.crbtcoreadapter.entity.RingtoneAssignment;
import com.platform.crbtcoreadapter.entity.RingtoneAssignment.SyncStatus;
import com.platform.crbtcoreadapter.repository.RingtoneAssignmentRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RingtoneAssignmentService {
    private static final Logger log = LoggerFactory.getLogger(RingtoneAssignmentService.class);

    private final RingtoneAssignmentRepository repository;
    private final MytoneCmsClient mytoneClient;

    public RingtoneAssignmentService(RingtoneAssignmentRepository repository, MytoneCmsClient mytoneClient) {
        this.repository = repository;
        this.mytoneClient = mytoneClient;
    }

    @Transactional
    public AssignmentResponse assign(Long userId, AssignRingtoneRequest request) {
        RingtoneAssignment assignment = new RingtoneAssignment(userId, request.msisdn(), request.ringtoneUrl());
        repository.save(assignment);
        syncToMytoneAsync(assignment.getId());
        return toResponse(assignment);
    }

    @Async
    public void syncToMytoneAsync(Long assignmentId) {
        RingtoneAssignment assignment = repository.findById(assignmentId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.COMMON_NOT_FOUND));
        try {
            assignment.setStatus(SyncStatus.SYNCING);
            repository.save(assignment);

            MytoneCmsResponse response = mytoneClient.assignRingtone(
                new MytoneCmsRequest(assignment.getMsisdn(), assignment.getRingtoneUrl(), "ASSIGN"));

            if (response != null && response.success()) {
                assignment.setStatus(SyncStatus.ACTIVE);
                assignment.setMytoneTransactionId(response.transactionId());
            } else {
                assignment.setStatus(SyncStatus.FAILED);
                assignment.setErrorMessage(response != null ? response.message() : "null response");
                assignment.setRetryCount(assignment.getRetryCount() + 1);
            }
            repository.save(assignment);
        } catch (Exception e) {
            log.error("Mytone sync {} failed", assignmentId, e);
            assignment.setStatus(SyncStatus.FAILED);
            assignment.setErrorMessage(e.getMessage());
            assignment.setRetryCount(assignment.getRetryCount() + 1);
            repository.save(assignment);
        }
    }

    @Transactional
    public AssignmentResponse remove(Long userId, Long assignmentId) {
        RingtoneAssignment assignment = repository.findById(assignmentId)
            .orElseThrow(() -> new BaseException(CommonErrorCode.COMMON_NOT_FOUND));
        if (!assignment.getUserId().equals(userId)) {
            throw new BaseException(CommonErrorCode.COMMON_FORBIDDEN);
        }
        MytoneCmsResponse response = mytoneClient.removeRingtone(assignment.getMsisdn());
        if (response != null && response.success()) {
            assignment.setStatus(SyncStatus.REMOVED);
        } else {
            assignment.setErrorMessage(response != null ? response.message() : "null response");
        }
        repository.save(assignment);
        return toResponse(assignment);
    }

    public List<AssignmentResponse> listByUser(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toResponse).toList();
    }

    private AssignmentResponse toResponse(RingtoneAssignment a) {
        return new AssignmentResponse(a.getId(), a.getMsisdn(), a.getRingtoneUrl(),
            a.getStatus(), a.getMytoneTransactionId(), a.getErrorMessage(), a.getCreatedAt());
    }
}
