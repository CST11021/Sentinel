

### Dashboard集成zk持久化



#### Dashboard工程修改

##### 1、pom.xml修改：

引入zk包（工程已经引入，无需手动引，只需将scope去掉即可）

```xml
<!--for Zookeeper rule publisher sample-->
<dependency>
    <groupId>org.apache.curator</groupId>
    <artifactId>curator-recipes</artifactId>
    <version>${curator.version}</version>
    <scope>test</scope>
</dependency>
```

去掉`<scope>test</scope>`

##### 2、实现DynamicRuleProvider和DynamicRulePublisher

在test目录中，sentinel提供了使用Zookeeper来同步流控规则的实现，我们直接复制到`com.alibaba.csp.sentinel.dashboard.rule`包中，并根据样例写出降级规则的实现，目录如图：

 <img src="assets/image-20210930171400627.png" alt="image-20210930171400627" style="zoom:50%;" />



 <img src="assets/image-20210930171806336.png" alt="image-20210930171806336" style="zoom:50%;" />

##### 3、修改规则ID的生成器

这里算是有个小bug吧，规则的ID生成器如`InMemDegradeRuleStore`使用的是`private static AtomicLong ids = new AtomicLong(0);`这样如果Dashboard重新部署的话，就会导致生成规则的id又从0开始了，这样有可能会导致新创建规则的时候，会将老规则给覆盖掉，做如下修改：

 <img src="assets/image-20210930171618262.png" alt="image-20210930171618262" style="zoom:50%;" />

`InMemoryRuleRepositoryAdapter`这个类的子类都有这个问题，可以一起修改。



#### 客户端的修改

##### 1、pom依赖


```xml
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-datasource-zookeeper</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.zookeeper</groupId>
    <artifactId>zookeeper</artifactId>
    <version>3.4.14</version>
    <exclusions>
        <exclusion>
            <artifactId>slf4j-log4j12</artifactId>
            <groupId>org.slf4j</groupId>
        </exclusion>
    </exclusions>
</dependency>

```

##### 2、添加zk配置类

```java
package com.alibaba.csp.sentinel.demo.annotation.aop.zk.config;

import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.zookeeper.ZookeeperDataSource;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.List;

@Configuration
public class ZookeeperDataSourceConfig {

    private static final String remoteAddress = "127.0.0.1:2181";
    
    @Value("${spring.application.name}")
    private String applicationName;
    
    @PostConstruct
    public void initSentinelDataSourceInitFuncConfig() {
    
        String appName = StringUtils.hasText(System.getProperty("project.name")) ?
                System.getProperty("project.name") : applicationName;
    
        // 流控规则
        final String flowPath = "/sentinel_rule_config/" + appName + "/flow";
        ReadableDataSource<String, List<FlowRule>> redisFlowDataSource = new ZookeeperDataSource<>(remoteAddress, flowPath,
                source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {
                }));
        FlowRuleManager.register2Property(redisFlowDataSource.getProperty());
    
        // 降级规则
        final String degradePath = "/sentinel_rule_config/" + appName + "/degrade";
        ReadableDataSource<String, List<DegradeRule>> redisDegradeDataSource = new ZookeeperDataSource<>(remoteAddress, degradePath,
                source -> JSON.parseObject(source, new TypeReference<List<DegradeRule>>() {
                }));
        DegradeRuleManager.register2Property(redisDegradeDataSource.getProperty());
    }

}
```



