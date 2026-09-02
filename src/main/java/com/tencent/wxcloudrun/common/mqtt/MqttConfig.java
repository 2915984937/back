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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.UUID;

/**
 * MQTT 配置类：创建 MqttClient、自动连接、打印连接事件日志。
 * 用 app.mqtt.enabled=true（默认）开启；本地调试不想连 broker 设 false 即可，所有 Bean 不创建。
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

    @Bean
    public MqttClient mqttClient() throws MqttException {
        String brokerUrl = props.getBrokerUrl();
        String clientId  = props.getClientId();

        if (blank(brokerUrl)) {
            throw new IllegalStateException("[MQTT] broker-url 未配置");
        }
        if (blank(clientId)) {
            clientId = "study-room-" + UUID.randomUUID().toString().replace("-", "");
        }

        MemoryPersistence persistence = new MemoryPersistence();
        this.client = new MqttClient(brokerUrl, clientId, persistence);

        client.setCallback(new MqttCallbackExtended() {
            @Override public void connectComplete(boolean reconnect, String serverURI) {
                log.info("[MQTT] {} broker={} clientId={}",
                    reconnect ? "重连成功" : "首次连接成功", serverURI, clientId);
            }
            @Override public void connectionLost(Throwable cause) {
                log.warn("[MQTT] 连接断开 clientId={} cause={}", clientId,
                    cause != null ? cause.getMessage() : "unknown");
            }
            @Override public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) {
                log.info("[MQTT] 收到消息 topic={} qos={} payload={}", topic, message.getQos(),
                    new String(message.getPayload()));
            }
            @Override public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });

        return client;
    }

    @Bean
    public MqttPublisher mqttPublisher(MqttClient client) {
        return new MqttPublisher(client, props);
    }

    /** null-safe helper：null 或 "" 都返回 true。 */
    private static boolean blank(String s) {
        return s == null || s.isEmpty();
    }

    @PostConstruct
    public void connect() {
        try {
            if (client == null) {
                log.warn("[MQTT] MqttClient 未创建，跳过连接");
                return;
            }

            MqttConnectOptions opts = new MqttConnectOptions();
            String username = props.getUsername();
            String password = props.getPassword();

            if (!blank(username)) opts.setUserName(username);
            if (!blank(password))  opts.setPassword(password.toCharArray());

            opts.setCleanSession(props.isCleanSession());
            opts.setAutomaticReconnect(props.isAutomaticReconnect());
            opts.setConnectionTimeout(props.getConnectTimeout());
            opts.setKeepAliveInterval(props.getKeepAlive());

            // 重要：不要手动 setSocketFactory！
            // Paho 对 ssl:// 协议内部有自己的 TLS 处理逻辑（SSLv3 兼容模式），
            // 手动注入 SSLSocketFactory.getDefault() 在 Java 8 容器镜像中会触发
            // "Unconnected sockets not implemented" 或 NPE（Paho 1.2.5 已知问题）。
            // 删掉这一行即可让 Paho 自己正确处理 TLS。

            log.info("[MQTT] 开始连接 broker={} clientId={} auth={}",
                props.getBrokerUrl(), props.getClientId(),
                blank(username) ? "无" : "有(" + username + ")");

            client.connect(opts);
        } catch (MqttException e) {
            log.error("[MQTT] 连接失败 reasonCode={} msg={}", e.getReasonCode(), e.getMessage(), e);
        } catch (Exception e) {
            // 打印完整堆栈定位 NPE 源头
            log.error("[MQTT] 连接异常 type={} msg={}", e.getClass().getSimpleName(), e.getMessage(), e);
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
}
