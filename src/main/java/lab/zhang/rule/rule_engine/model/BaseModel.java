package lab.zhang.rule.rule_engine.model;

import lombok.Data;

import java.util.Date;

@Data
abstract public class BaseModel {

    Date createTime;

    Date updateTime;


    public void setCreateTimeByTimestamp(Integer timestamp) {
        this.createTime = new Date((long) timestamp * 1000);
    }

    public int getCreateTimeInTimestamp() {
        if (this.createTime == null) {
            return 0;
        }
        return (int) (this.createTime.getTime() / 1000);
    }

    public void setUpdateTimeByTimestamp(Integer timestamp) {
        this.updateTime = new Date((long) timestamp * 1000);
    }

    public int getUpdateTimeInTimestamp() {
        if (this.createTime == null) {
            return 0;
        }
        return (int) (this.updateTime.getTime() / 1000);
    }
}
