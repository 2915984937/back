package com.tencent.wxcloudrun.common.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MQTT 配置类：按 EMQX 官方示例极简实现。
 * 连接放到独立线程异步执行，避免在 Spring Boot 启动阶段同步 connect() 可能的 NPE。
 */
@Configuration
@ConditionalOnProperty(name = "app.mqtt.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MqttProperties.class)
public class MqttConfig {

    private static final Logger log = LoggerFactory.getLogger(MqttConfig.class);

    private final MqttProperties props;
    private volatile MqttClient client;
    private final AtomicBoolean connecting = new AtomicBoolean(false);

    public MqttConfig(MqttProperties props) {
        this.props = props;
    }

    @Bean
    public MqttClient mqttClient() {
        String broker = props.getBrokerUrl();
        String clientId = props.getClientId();

        if (blank(broker)) {
            log.warn("[MQTT] broker-url 为空，MQTT 功能不可用");
            return null;
        }
        if (blank(clientId)) {
            clientId = MqttClient.generateClientId();
            log.info("[MQTT] client-id 未配置，Paho 自动生成 {}", clientId);
        }

        // 打印所有配置，方便在云托管日志里排查
        log.info("[MQTT] 配置快照 broker={} clientId={} username={} topicPrefix={}",
            broker, clientId, props.getUsername(), props.getTopicPrefix());

        try {
            this.client = new MqttClient(broker, clientId, new MemoryPersistence());

            client.setCallback(new MqttCallbackExtended() {
                @Override public void connectComplete(boolean reconnect, String serverURI) {
                    log.info("[MQTT] ✅ {} broker={}", reconnect ? "重连成功" : "连接成功", serverURI);
                }
                @Override public void connectionLost(Throwable cause) {
                    log.warn("[MQTT] ⚠️ 连接断开 cause={}", cause != null ? cause.getMessage() : "unknown");
                }
                @Override public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) {
                    log.info("[MQTT] 📨 收到 topic={} qos={} payload={}", topic, message.getQos(),
                        new String(message.getPayload()));
                }
                @Override public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });
        } catch (MqttException e) {
            log.error("[MQTT] 创建 MqttClient 失败 reasonCode={} msg={}", e.getReasonCode(), e.getMessage());
        }
        return client;
    }

    @Bean
    public MqttPublisher mqttPublisher(org.springframework.beans.factory.ObjectProvider<MqttClient> clientProvider) {
        return new MqttPublisher(clientProvider.getIfAvailable(), props);
    }

    /** Spring Boot 启动完成后异步触发连接，不阻塞主线程。 */
    @EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void onReady() {
        if (client == null) return;
        if (!connecting.compareAndSet(false, true)) return;

        String broker = props.getBrokerUrl();
        String username = props.getUsername();
        new Thread(() -> connect(broker, username), "mqtt-connect").start();
    }

    private void connect(String broker, String username) {
        try {
            // 完全按照 EMQX 官方示例，只设最基本的选项
            MqttConnectOptions opts = new MqttConnectOptions();
            if (!blank(username)) opts.setUserName(username);
            if (!blank(props.getPassword())) opts.setPassword(props.getPassword().toCharArray());
            opts.setCleanSession(props.isCleanSession());
            opts.setAutomaticReconnect(props.isAutomaticReconnect());

            log.info("[MQTT] 🔌 开始连接 broker={} clientId={} auth={}",
                broker, props.getClientId(), blank(username) ? "无" : "有(" + username + ")");

            client.connect(opts);
        } catch (MqttException e) {
            log.error("[MQTT] ❌ 连接失败 reasonCode={} msg={}", e.getReasonCode(), e.getMessage());
        } catch (Throwable t) {
            // 兜底：任何 RuntimeException / Error 都打完整堆栈
            log.error("[MQTT] ❌ 连接异常 type={} msg={}", t.getClass().getName(), t.getMessage(), t);
        } finally {
            connecting.set(false);
        }
    }

    @PreDestroy
    public void disconnect() {
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
                log.info("[MQTT] 已断开 clientId={}", props.getClientId());
            } catch (MqttException e) {
                log.warn("[MQTT] 断开异常", e);
            }
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isEmpty();
    }
}
