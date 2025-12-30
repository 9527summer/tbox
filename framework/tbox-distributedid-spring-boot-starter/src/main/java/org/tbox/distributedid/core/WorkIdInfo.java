package org.tbox.distributedid.core;


public class WorkIdInfo {

    private Long nodeId;

    public WorkIdInfo() {
    }

    public WorkIdInfo(Long nodeId) {
        this.nodeId = nodeId;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }
}