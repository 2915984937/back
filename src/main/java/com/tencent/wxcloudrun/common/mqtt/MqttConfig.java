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
        MemoryPersistence persistence = new MemoryPersistence();
        this.client = new MqttClient(props.getBrokerUrl(), props.getClientId(), persistence);

        client.setCallback(new MqttCallbackExtended() {
            @Override public void connectComplete(boolean reconnect, String serverURI) {
                log.info("[MQTT] {} broker={} clientId={}",
                    reconnect ? "重连成功" : "首次连接成功", serverURI, props.getClientId());
            }
            @Override public void connectionLost(Throwable cause) {
                log.warn("[MQTT] 连接断开 clientId={} cause={}", props.getClientId(),
                    cause != null ? cause.getMessage() : "unknown");
            }
            @Override public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) {
                log.info("[MQTT] 收到消息 topic={} qos={} payload={}", topic, message.getQos(),
                    new String(message.getPayload()));
            }
            @Override public void deliveryComplete(IMqttDeliveryToken token) {
                // 发布完成回调（QoS 1/2 有意义，QoS 0 为空）
            }
        });

        return client;
    }

    @Bean
    public MqttPublisher mqttPublisher(MqttClient client) {
        return new MqttPublisher(client, props);
    }

    @PostConstruct
    public void connect() {
        try {
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setUserName(props.getUsername());
            opts.setPassword(props.getPassword().isEmpty() ? null : props.getPassword().toCharArray());
            opts.setCleanSession(props.isCleanSession());
            opts.setAutomaticReconnect(props.isAutomaticReconnect());
            opts.setConnectionTimeout(props.getConnectTimeout());
            opts.setKeepAliveInterval(props.getKeepAlive());
            opts.setSocketFactory(javax.net.ssl.SSLSocketFactory.getDefault());

            log.info("[MQTT] 开始连接 broker={} clientId={} auth={}",
                props.getBrokerUrl(), props.getClientId(),
                props.getUsername().isEmpty() ? "无" : "有(" + props.getUsername() + ")");
            client.connect(opts);
        } catch (MqttException e) {
            log.error("[MQTT] 连接失败 reasonCode={} msg={}", e.getReasonCode(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("[MQTT] 连接异常", e);
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