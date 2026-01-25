package com.ispengya.hkcache.server.core;

import java.util.Set;

/**
 * InstanceRegistry 定义实例注册表接口。
 *
 * <p>用于获取当前活跃或已知的实例 ID 列表，以便调度器遍历执行计算任务。</p>
 *
 * @author ispengya
 */
public interface InstanceRegistry {

    /**
     * 列出所有实例 ID。
     *
     * @return 实例 ID 集合
     */
    Set<String> listInstanceIds();
}
