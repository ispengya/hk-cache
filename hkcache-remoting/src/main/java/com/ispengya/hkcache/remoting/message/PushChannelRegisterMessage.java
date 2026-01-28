package com.ispengya.hkcache.remoting.message;

import java.io.Serializable;

public class PushChannelRegisterMessage implements Serializable {

    private String instanceId;

    public PushChannelRegisterMessage() {
    }

    public PushChannelRegisterMessage(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}
