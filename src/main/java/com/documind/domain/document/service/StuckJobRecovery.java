package com.documind.domain.document.service;

import com.documind.domain.document.entity.EmbeddingJob;
import com.documind.domain.document.entity.enums.JobStatus;
import com.documind.domain.document.repository.EmbeddingJobRepository;
import com.documind.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StuckJobRecovery {
    private static final List<JobStatus> FINISHED = List.of(JobStatus.COMPLETED, JobStatus.FAILED);
    private static final String INTERRUPTED_MESSAGE = "서버가 처리 도중 종료되어 작업이 중단되었습니다.";

    private final EmbeddingJobRepository embeddingJobRepository;
    private final Instant bootTime = Instant.now();

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void failInterruptedJobs() {
        List<EmbeddingJob> stuck = embeddingJobRepository.findByStatusNotInAndCreatedAtBefore(FINISHED, bootTime);

        if (stuck.isEmpty()) {
            return;
        }
        stuck.forEach(job -> job.markFailed(ErrorCode.JOB_INTERRUPTED.getCode(), INTERRUPTED_MESSAGE));
        log.warn("중단된 작업 {}건을 FAILED로 변경: jobIds={}",
                stuck.size(), stuck.stream().map(EmbeddingJob::getId).toList());
    }
}
