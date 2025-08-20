package com.ruoyi.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import com.ruoyi.gateway.handler.SentinelFallbackHandler;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

/**
 * 网关限流配置,当前项目通过nacos配置中心动态加载 route维度限流 及 实现自定义API分组限流
 * 支持针对请求中的参数、Header、来源 IP 等进行定制化的限流
 * 优先级：route维度限流 > 自定义API分组限流
 *
 * 若需要使用代码配置规则：
 * 详情见官网：https://sentinelguard.io/zh-cn/docs/api-gateway-flow-control.html
 * 代码示例见github：https://github.com/alibaba/Sentinel/blob/master/sentinel-demo/sentinel-demo-spring-cloud-gateway/src/main/java/com/alibaba/csp/sentinel/demo/spring/sc/gateway/GatewayConfiguration.java
 * 
 * @author ruoyi
 */
@Configuration
public class GatewayConfig
{
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelFallbackHandler sentinelGatewayExceptionHandler()
    {
        return new SentinelFallbackHandler();
    }


    @PostConstruct
    public void doInit()
    {
        /**
         * 加载网关限流规则，nacos配置规则 和 编码配置规则不要同时使用，当前项目使用nacos配置规则
         * 编码配置规则见代码示例github
         */
//        initGatewayRules();
    }

    /**
     * 网关限流规则
     */
    private void initGatewayRules()
    {
        Set<GatewayFlowRule> rules = new HashSet<>();
        rules.add(new GatewayFlowRule("system-api")
                .setCount(2) // 限流阈值
                .setIntervalSec(60)); // 统计时间窗口，单位是秒，默认是 1 秒
        rules.add(new GatewayFlowRule("code-api")
                .setCount(2) // 限流阈值
                .setIntervalSec(60));
        // 加载网关限流规则
        GatewayRuleManager.loadRules(rules);
        // 加载限流分组
        initCustomizedApis();
    }

    /**
     * 限流分组
     */
    private void initCustomizedApis()
    {
        Set<ApiDefinition> definitions = new HashSet<>();
        // ruoyi-system 组
        ApiDefinition api1 = new ApiDefinition("system-api").setPredicateItems(new HashSet<ApiPredicateItem>()
        {
            private static final long serialVersionUID = 1L;
            {
                // 匹配 /user 以及其子路径的所有请求
                add(new ApiPathPredicateItem().setPattern("/system/user/**")
                        .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
            }
        });
        // ruoyi-gen 组
        ApiDefinition api2 = new ApiDefinition("code-api").setPredicateItems(new HashSet<ApiPredicateItem>()
        {
            private static final long serialVersionUID = 1L;
            {
                // 只匹配 /job/list
                add(new ApiPathPredicateItem().setPattern("/code/gen/list"));
            }
        });
        definitions.add(api1);
        definitions.add(api2);
        // 加载限流分组
        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
    }
}