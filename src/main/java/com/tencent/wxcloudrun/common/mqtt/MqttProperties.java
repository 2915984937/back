package com.tencent.wxcloudrun.common.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MQTT 连接配置（app.mqtt.* 映射）。
 * 全部字段可被环境变量覆盖：EMQX_BROKER_URL / EMQX_USERNAME / EMQX_PASSWORD / EMQX_CLIENT_ID ...
 */
@ConfigurationProperties(prefix = "app.mqtt")
public class MqttProperties {

    private String brokerUrl;
    private String username = "";
    private String password = "";
    private String clientId = "study-room-back";
    private String topicPrefix = "study-room";
    private int connectTimeout = 10;
    private int keepAlive = 60;
    private boolean automaticReconnect = true;
    private boolean cleanSession = true;
    private boolean enabled = true;

    public String getBrokerUrl()       { return brokerUrl; }
    public void setBrokerUrl(String v) { this.brokerUrl = v; }
    public String getUsername()        { return username; }
    public void setUsername(String v)  { this.username = v; }
    public String getPassword()        { return password; }
    public void setPassword(String v)  { this.password = v; }
    public String getClientId()        { return clientId; }
    public void setClientId(String v)  { this.clientId = v; }
    public String getTopicPrefix()     { return topicPrefix; }
    public void setTopicPrefix(String v){ this.topicPrefix = v; }
    public int getConnectTimeout()     { return connectTimeout; }
    public void setConnectTimeout(int v){ this.connectTimeout = v; }
    public int getKeepAlive()          { return keepAlive; }
    public void setKeepAlive(int v)    { this.keepAlive = v; }
    public boolean isAutomaticReconnect(){ return automaticReconnect; }
    public void setAutomaticReconnect(boolean v){ this.automaticReconnect = v; }
    public boolean isCleanSession()    { return cleanSession; }
    public void setCleanSession(boolean v){ this.cleanSession = v; }
    public boolean isEnabled()         { return enabled; }
    public void setEnabled(boolean v)  { this.enabled = v; }
}