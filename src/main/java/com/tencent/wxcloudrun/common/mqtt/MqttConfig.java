package com.tencent.wxcloudrun.common.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * MQTT 配置类 —— 严格对齐 EMQX 官方 TCP 示例。
 * 只有三件事：new MqttClient → setCallback → connect。
 * 不做任何额外配置（cleanSession / automaticReconnect / socketFactory 等），
 * 避免 Paho 内部因额外逻辑触发 NPE。
 */
@Configuration
@ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MqttProperties.class)
public class MqttConfig {

    private static final Logger log = LoggerFactory.getLogger(MqttConfig.class);

    private final MqttProperties props;
    private MqttClient client;

    public MqttConfig(MqttProperties props) {
        this.props = props;
    }

    /**
     * 完全按 EMQX 官方示例写：
     *   broker  = "tcp://broker.emqx.io:1883"
     *   clientId = MqttClient.generateClientId()
     *   opts.setUserName("emqx_user")
     *   opts.setPassword("emqx_password".toCharArray())
     */
    @PostConstruct
    public void init() {
        String broker = props.getBrokerUrl();
        String clientId = blank(props.getClientId()) ? MqttClient.generateClientId() : props.getClientId();
        String username = props.getUsername();
        String password = props.getPassword();

        // 打印所有入参，方便云托管日志里核对
        log.info("[MQTT] ============ EMQX 官方示例风格启动 ============");
        log.info("[MQTT] broker      = {}", broker);
        log.info("[MQTT] clientId    = {}", clientId);
        log.info("[MQTT] username    = {}", blank(username) ? "(空)" : username);
        log.info("[MQTT] hasPassword = {}", !blank(password));

        try {
            // === 1. 创建 MqttClient（完全同官方示例） ===
            MemoryPersistence persistence = new MemoryPersistence();
            this.client = new MqttClient(broker, clientId, persistence);

            // === 2. 设置回调（普通 MqttCallback，不是 Extended） ===
            this.client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("[MQTT] connectionLost：{}", cause != null ? cause.getMessage() : "unknown");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    log.info("[MQTT] messageArrived topic={} qos={} payload={}",
                        topic, message.getQos(), new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // QoS 1/2 才会回调
                }
            });

            // === 3. 构建连接选项（只设认证，不设其他） ===
            MqttConnectOptions connOpts = new MqttConnectOptions();
            if (!blank(username)) connOpts.setUserName(username);
            if (!blank(password))  connOpts.setPassword(password.toCharArray());
            // 注意：不设 cleanSession / automaticReconnect / connectionTimeout / keepAlive
            //       让 Paho 用默认值（cleanSession=true, automaticReconnect=false 等）

            log.info("[MQTT] Connecting to broker: {}", broker);

            // === 4. 同步 connect（和官方示例一样） ===
            this.client.connect(connOpts);

            log.info("[MQTT] ✅ Connected to broker: {}", broker);

        } catch (MqttException e) {
            log.error("[MQTT] ❌ MqttException reasonCode={} msg={}", e.getReasonCode(), e.getMessage());
        } catch (Throwable t) {
            // 兜底：NPE / Error / RuntimeException 全部打完整堆栈
            log.error("[MQTT] ❌ 连接异常 type={} msg={}", t.getClass().getName(), t.getMessage(), t);
        }
    }

    @Bean
    public MqttClient mqttClient() {
        return this.client;
    }

    @Bean
    public MqttPublisher mqttPublisher() {
        return new MqttPublisher(this.client, props);
    }

    @PreDestroy
    public void destroy() {
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
                log.info("[MQTT] Disconnected");
            } catch (MqttException e) {
                log.warn("[MQTT] disconnect failed", e);
            }
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isEmpty();
    }
}
