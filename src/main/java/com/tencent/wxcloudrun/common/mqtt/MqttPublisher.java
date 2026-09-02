package com.tencent.wxcloudrun.common.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * MQTT 消息发布服务。
 * 所有 topic 自动加上配置的 topicPrefix，避免硬编码。
 */
@Service
public class MqttPublisher {

    private static final Logger log = LoggerFactory.getLogger(MqttPublisher.class);

    private final MqttClient mqttClient;
    private final MqttProperties properties;

    public MqttPublisher(MqttClient mqttClient, MqttProperties properties) {
        this.mqttClient = mqttClient;
        this.properties = properties;
    }

    /**
     * 发布消息，自动加 topicPrefix，QoS 0（最多一次）。
     */
    public boolean publish(String topicSuffix, String payload) {
        return publish(topicSuffix, payload, 0);
    }

    /**
     * 发布消息。
     * @param topicSuffix 主题后缀，如 "device/dev-001/cmd"
     * @param payload     消息内容（字符串）
     * @param qos         0-最多一次  1-至少一次  2-恰好一次
     */
    public boolean publish(String topicSuffix, String payload, int qos) {
        String topic = properties.getTopicPrefix() + "/" + topicSuffix;
        if (qos < 0 || qos > 2) {
            log.warn("[MQTT] 非法 QoS={}，已降级为 0", qos);
            qos = 0;
        }
        try {
            if (!mqttClient.isConnected()) {
                log.warn("[MQTT] 客户端未连接，跳过发布 topic={}", topic);
                return false;
            }
            MqttMessage msg = new MqttMessage(payload.getBytes("UTF-8"));
            msg.setQos(qos);
            mqttClient.publish(topic, msg);
            log.info("[MQTT] publish topic={} qos={} len={}", topic, qos, payload.length());
            return true;
        } catch (MqttException e) {
            log.error("[MQTT] publish 失败 topic={} reason={}", topic, e.getReasonCode(), e);
            return false;
        } catch (Exception e) {
            log.error("[MQTT] publish 异常 topic={}", topic, e);
            return false;
        }
    }

    /** 当前连接状态（测试用）。 */
    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }
}