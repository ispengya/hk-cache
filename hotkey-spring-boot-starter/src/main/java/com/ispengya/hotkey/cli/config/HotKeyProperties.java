package com.ispengya.hotkey.cli.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * HotKeyProperties 定义 hkcache 客户端在 Spring Boot 中的外部化配置，
 * 通过配置文件中以 "hkcache" 为前缀的属性进行绑定。
 *
 * 当前支持多实例配置信息，便于为不同实例创建独立的 HotKeyClient。
 *
 * @author ispengya
 */
@ConfigurationProperties(prefix = "hkcache")
public class HotKeyProperties {

    private boolean enabled = true;

    private List<InstanceConfig> instances = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<InstanceConfig> getInstances() {
        return instances;
    }

    public void setInstances(List<InstanceConfig> instances) {
        this.instances = instances;
    }
}
