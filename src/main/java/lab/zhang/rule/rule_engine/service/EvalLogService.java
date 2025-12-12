package lab.zhang.rule.rule_engine.service;

import lab.zhang.rule.rule_engine.entity.EvalLogEntity;

import java.util.List;

/**
 * Eval log batch service interface
 * Handles batch insertion of eval logs
 *
 * @author Rongjin Zhang
 */
public interface EvalLogService {

    /**
     * Add eval log to batch queue
     * The log will be inserted in batch when batch size or flush interval is reached
     *
     * @param evalLog eval log entity to add
     */
    void addLog(EvalLogEntity evalLog);

    /**
     * Flush pending logs immediately
     * Force insert all pending logs in batch
     */
    void flush();

    /**
     * Get current batch size
     *
     * @return current number of pending logs
     */
    int getBatchSize();

    /**
     * Insert logs in batch
     * This method is called internally by the batch service
     *
     * @param logList list of eval log entities to insert
     */
    void insertBatch(List<EvalLogEntity> logList);
}

