package org.tbox.distributedid.core;


public class WorkIdInfo {

    private Long workId;

    private Long dataCenterId;

    public WorkIdInfo() {
    }

    public WorkIdInfo(Long workId, Long dataCenterId) {
        this.workId = workId;
        this.dataCenterId = dataCenterId;
    }

    public Long getWorkId() {
        return workId;
    }

    public void setWorkId(Long workId) {
        this.workId = workId;
    }

    public Long getDataCenterId() {
        return dataCenterId;
    }

    public void setDataCenterId(Long dataCenterId) {
        this.dataCenterId = dataCenterId;
    }
}
