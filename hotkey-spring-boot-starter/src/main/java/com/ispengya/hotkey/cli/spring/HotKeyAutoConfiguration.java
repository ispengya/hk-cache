package com.ispengya.hotkey.cli.spring;

import cn.hutool.core.collection.CollUtil;
import com.ispengya.hotkey.cli.config.HotKeyProperties;
import com.ispengya.hotkey.cli.config.InstanceConfig;
import com.ispengya.hotkey.cli.core.CacheTemplate;
import com.ispengya.hotkey.cli.core.HotKeyClient;
import com.ispengya.hotkey.cli.core.PostLoadAction;
import com.ispengya.hotkey.cli.detect.HotKeyDetector;
import com.ispengya.hotkey.cli.detect.HotKeySet;
import com.ispengya.hotkey.cli.detect.HotKeyDetector;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * HotKeyAutoConfiguration 提供 hkcache 在 Spring Boot 环境下的自动装配，
 * 在检测到 HotKeyClient 存在且配置开启时，自动注册相关 Bean。
 *
 * 默认实现基于本地缓存和空实现的探测与监控，后续可按需替换。
 *
 * @author ispengya
 */
@Configuration
@ConditionalOnClass(HotKeyClient.class)
@EnableConfigurationProperties(HotKeyProperties.class)
public class HotKeyAutoConfiguration {


    /**
     * 注册 HotKeyClientManager，用于集中管理多实例客户端。
     *
     * @return HotKeyClientManager 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public HotKeyClientManager hkcacheHotKeyClientManager() {
        return new HotKeyClientManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public PostLoadAction hkcachePostLoadAction() {
        return new PostLoadAction() {
            @Override
            public <T> void onLoaded(String key, T value, boolean fromCache, long costNanos) {
                // 空实现
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public HotKeyDetector hkcacheHotKeyDetector() {
        return new HotKeyDetector();
    }

    @Bean
    @ConditionalOnMissingBean
    public HotKeySet hkcacheHotKeySet() {
        return new HotKeySet();
    }
    

    /**
     * 注册默认的 HotKeyClient。
     *
     * @param properties       外部化配置
     * @param detector         热 key 探测器
     * @param hotKeySet        热 key 视图
     * @param postLoadAction   后置处理回调
     * @param hotKeyClientManager 客户端管理器
     * @return 默认 HotKeyClient 实例
     */
    @Bean(name = HotKeyBeanNames.DEFAULT_CLIENT)
    @ConditionalOnMissingBean
    public HotKeyClient hkcacheHotKeyClient(HotKeyProperties properties,
                                            HotKeyDetector detector,
                                            HotKeySet hotKeySet,
                                            PostLoadAction postLoadAction,
                                            ApplicationContext applicationContext,
                                            HotKeyClientManager hotKeyClientManager) {
        List<InstanceConfig> instances = properties.getInstances();
        if (CollUtil.isEmpty(instances)) {
            InstanceConfig defaultConfig = new InstanceConfig();
            defaultConfig.setInstanceName("default");
            instances = new ArrayList<>();
            instances.add(defaultConfig);
        }
        HotKeyClientFactory factory = new HotKeyClientFactory();
        HotKeyClient defaultClient = null;
        for (InstanceConfig config : instances) {
            String instanceName = config.getInstanceName();
            if (StringUtils.isEmpty(instanceName)) {
                instanceName = "default";
            }
            String templateBeanName = buildCacheTemplateBeanName(instanceName);
            CacheTemplate cacheTemplate = applicationContext.getBean(templateBeanName, CacheTemplate.class);
            HotKeyClient client = factory.createHotKeyClient(instanceName, detector, hotKeySet, cacheTemplate, postLoadAction);
            hotKeyClientManager.register(instanceName, client);
            if (defaultClient == null) {
                defaultClient = client;
            }
        }
        return defaultClient;
    }

    private String buildCacheTemplateBeanName(String instanceName) {
        return "hkcache." + (StringUtils.isEmpty(instanceName) ? "default" : instanceName) + ".cacheTemplate";
    }
}
