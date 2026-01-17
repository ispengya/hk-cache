package com.ispengya.hotkey.cli.detect;

/**
 * HotKeyDetector 负责采集访问轨迹或上报数据，
 * 用于后续的热 key 计算或统计分析。
 *
 * 默认实现为空实现，业务可以按需替换为接入实际探测链路的版本。
 *
 * @author ispengya
 */
public class HotKeyDetector {

    /**
     * 记录一次对指定 key 的访问。
     *
     * @param key 访问的业务 key
     */
    public void recordAccess(String key) {
    }
}

