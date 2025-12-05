package lab.zhang.rule.rule_engine.entity;

import lab.zhang.rule.rule_engine.util.TimeUtil;
import lombok.Data;

@Data
abstract public class BaseEntity {

    /**
     * Create time (UNIX timestamp in seconds)
     */
    protected Integer ct;

    /**
     * Update time (UNIX timestamp in seconds)
     */
    protected Integer ut;


    public void setTimeOnCreate() {
        // Set time fields (UNIX timestamp in seconds)
        long currentTime = TimeUtil.getCurrentTime();
        this.ct = (int) currentTime;
        this.ut = (int) currentTime;
    }

    public void setTimeOnUpdate() {
        // Set update time (UNIX timestamp in seconds)
        long currentTime = TimeUtil.getCurrentTime();
        this.ut = (int) currentTime;
    }
}
