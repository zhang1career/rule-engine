package lab.zhang.rule.rule_engine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lab.zhang.rule.rule_engine.entity.EvalLogEntity;
import lab.zhang.rule.rule_engine.mapper.EvalLogMapper;
import lab.zhang.rule.rule_engine.service.EvalLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Eval log service implementation
 * Batches eval logs and inserts them periodically or when batch size is reached
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Service
public class EvalLogServiceImpl extends ServiceImpl<EvalLogMapper, EvalLogEntity> implements EvalLogService {

    @Value("${rule.log.batch.enabled:true}")
    private boolean batchEnabled;

    @Value("${rule.log.batch.batch-size:100}")
    private int batchSize;

    @Value("${rule.log.batch.flush-interval-ms:1000}")
    private long flushIntervalMs;

    private final List<EvalLogEntity> batchQueue = new ArrayList<>();
    private final Lock batchLock = new ReentrantLock();

    @PostConstruct
    public void init() {
        if (batchEnabled) {
            log.info("[init] eval log service initialized: batchSize={}, flushIntervalMs={}", batchSize, flushIntervalMs);
        } else {
            log.info("[init] eval log service disabled, using direct insert");
        }
    }

    @Override
    @Transactional
    public void addLog(EvalLogEntity evalLog) {
        if (!batchEnabled) {
            // If batch is disabled, insert directly
            save(evalLog);
            return;
        }

        batchLock.lock();
        try {
            batchQueue.add(evalLog);
            if (batchQueue.size() >= batchSize) {
                flushInternal();
            }
        } finally {
            batchLock.unlock();
        }
    }

    @Override
    @Transactional
    public void flush() {
        if (!batchEnabled) {
            return;
        }

        batchLock.lock();
        try {
            flushInternal();
        } finally {
            batchLock.unlock();
        }
    }

    @Override
    public int getBatchSize() {
        batchLock.lock();
        try {
            return batchQueue.size();
        } finally {
            batchLock.unlock();
        }
    }

    @Override
    @Transactional
    public void insertBatch(List<EvalLogEntity> logList) {
        if (logList == null || logList.isEmpty()) {
            return;
        }

        try {
            saveBatch(logList);
            if (log.isDebugEnabled()) {
                log.debug("[eval] eval logs batch inserted, size={}", logList.size());
            }
        } catch (Exception e) {
            log.error("[eval] failed to batch insert eval logs: {}", e.getMessage(), e);
        }
    }

    /**
     * Flush batch queue (must be called with lock held)
     */
    @Transactional
    public void flushInternal() {
        if (batchQueue.isEmpty()) {
            return;
        }

        List<EvalLogEntity> toInsert = new ArrayList<>(batchQueue);
        batchQueue.clear();
        // Release lock before database operation to avoid blocking other threads
        batchLock.unlock();

        try {
            insertBatch(toInsert);
        } finally {
            // Re-acquire lock after database operation
            batchLock.lock();
        }
    }

    /**
     * Scheduled flush based on time interval
     */
    @Transactional
    @Scheduled(fixedDelayString = "${rule.log.batch.flush-interval-ms:1000}")
    public void scheduledFlush() {
        if (!batchEnabled) {
            return;
        }

        batchLock.lock();
        try {
            if (!batchQueue.isEmpty()) {
                flushInternal();
            }
        } finally {
            batchLock.unlock();
        }
    }

    @PreDestroy
    @Transactional
    public void shutdown() {
        flush();
        log.info("Eval log batch service shutdown, flushed remaining logs");
    }
}

