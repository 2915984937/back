package com.tencent.wxcloudrun.common.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MqttPublisherTest {

    @Mock MqttClient mqttClient;

    MqttProperties props;
    MqttPublisher publisher;

    @BeforeEach
    void setUp() {
        props = new MqttProperties();
        props.setTopicPrefix("study-room");
        props.setBrokerUrl("ssl://broker.test:8883");
        publisher = new MqttPublisher(mqttClient, props);
    }

    @Test
    @DisplayName("连接正常时 publish 调 mqttClient，topic 自动加前缀，payload 正确")
    void publish_success() throws Exception {
        when(mqttClient.isConnected()).thenReturn(true);
        IMqttDeliveryToken token = mock(IMqttDeliveryToken.class);
        when(mqttClient.publish(anyString(), any(MqttMessage.class))).thenReturn(token);

        boolean ok = publisher.publish("device/dev-001/cmd", "{\"on\":true}");

        assertTrue(ok);
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MqttMessage> msgCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(topicCaptor.capture(), msgCaptor.capture());
        assertEquals("study-room/device/dev-001/cmd", topicCaptor.getValue());
        assertEquals("{\"on\":true}", new String(msgCaptor.getValue().getPayload(), "UTF-8"));
        assertEquals(0, msgCaptor.getValue().getQos()); // 默认 QoS 0
    }

    @Test
    @DisplayName("指定 QoS 时正确传入")
    void publish_withQos() throws Exception {
        when(mqttClient.isConnected()).thenReturn(true);
        IMqttDeliveryToken token = mock(IMqttDeliveryToken.class);
        when(mqttClient.publish(anyString(), any(MqttMessage.class))).thenReturn(token);

        publisher.publish("test", "hi", 1);

        ArgumentCaptor<MqttMessage> msgCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(anyString(), msgCaptor.capture());
        assertEquals(1, msgCaptor.getValue().getQos());
    }

    @Test
    @DisplayName("非法 QoS 降级为 0")
    void publish_invalidQos() throws Exception {
        when(mqttClient.isConnected()).thenReturn(true);
        IMqttDeliveryToken token = mock(IMqttDeliveryToken.class);
        when(mqttClient.publish(anyString(), any(MqttMessage.class))).thenReturn(token);

        publisher.publish("t", "p", 99);

        ArgumentCaptor<MqttMessage> msgCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttClient).publish(anyString(), msgCaptor.capture());
        assertEquals(0, msgCaptor.getValue().getQos());
    }

    @Test
    @DisplayName("客户端未连接 → 不调 publish，返回 false")
    void publish_notConnected() throws Exception {
        when(mqttClient.isConnected()).thenReturn(false);

        boolean ok = publisher.publish("t", "p");

        assertFalse(ok);
        verify(mqttClient, never()).publish(anyString(), any(MqttMessage.class));
    }

    @Test
    @DisplayName("mqttClient.publish 抛 MqttException → 返回 false")
    void publish_mqttException() throws Exception {
        when(mqttClient.isConnected()).thenReturn(true);
        when(mqttClient.publish(anyString(), any(MqttMessage.class)))
            .thenThrow(new MqttException(MqttException.REASON_CODE_SERVER_CONNECT_ERROR));

        boolean ok = publisher.publish("t", "p");

        assertFalse(ok);
    }

    @Test
    @DisplayName("isConnected 透传底层状态")
    void isConnected() {
        when(mqttClient.isConnected()).thenReturn(true, false);
        assertTrue(publisher.isConnected());
        assertFalse(publisher.isConnected());
    }

    @Test
    @DisplayName("空 payload 正常发布（UTF-8 编码）")
    void publish_emptyPayload() throws Exception {
        when(mqttClient.isConnected()).thenReturn(true);
        IMqttDeliveryToken token = mock(IMqttDeliveryToken.class);
        when(mqttClient.publish(anyString(), any(MqttMessage.class))).thenReturn(token);

        boolean ok = publisher.publish("t", "");

        assertTrue(ok);
        verify(mqttClient).publish(anyString(), any(MqttMessage.class));
    }
}