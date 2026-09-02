import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * 独立 MQTT 连通性测试 —— 不依赖 Spring，纯 main 方法。
 * 用来确认 Paho 1.2.5 + EMQX broker 本身能不能连上，
 * 排除 Spring 环境干扰。
 *
 * 编译：
 *   mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt
 *   javac -cp "@cp.txt" src/test/java/StandaloneMqttTest.java
 * 运行：
 *   java -cp "@cp.txt;src/test/java" StandaloneMqttTest
 */
public class StandaloneMqttTest {

    // EMQX broker 地址（你提供的）
    static final String HOST = "f1e96469.ala.cn-hangzhou.emqxsl.cn";
    static final String BROKER_TCP = "tcp://" + HOST + ":1883";
    static final String BROKER_SSL = "ssl://" + HOST + ":8883";

    // 你在 EMQX 控制台创建的认证用户
    static final String USERNAME = "user1";
    static final String PASSWORD = "password1";   // TODO: 改成你实际设的密码

    static final String TEST_TOPIC = "study-room/ping";
    static final String TEST_PAYLOAD = "hello from standalone test " + System.currentTimeMillis();

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  EMQX 独立连通性测试（不依赖 Spring）");
        System.out.println("========================================");

        // 先测 TCP（最基础，能通说明网络和认证没问题）
        testConnect("TCP", BROKER_TCP);

        System.out.println();

        // 再测 TLS
        testConnect("TLS", BROKER_SSL);
    }

    static void testConnect(String label, String broker) {
        System.out.println("---------- 测试 " + label + " ----------");
        System.out.println("broker   = " + broker);
        System.out.println("username = " + USERNAME);

        MqttClient client = null;
        try {
            // === 1. 创建 client（完全照 EMQX 官方示例） ===
            String clientId = MqttClient.generateClientId();
            System.out.println("clientId = " + clientId);

            client = new MqttClient(broker, clientId, new MemoryPersistence());

            // === 2. 设回调 ===
            client.setCallback(new MqttCallback() {
                @Override public void connectionLost(Throwable cause) {
                    System.out.println("  [callback] connectionLost: " + cause);
                }
                @Override public void messageArrived(String topic, MqttMessage msg) {
                    System.out.println("  [callback] messageArrived topic=" + topic
                        + " payload=" + new String(msg.getPayload()));
                }
                @Override public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("  [callback] deliveryComplete");
                }
            });

            // === 3. 构建连接选项（只设认证） ===
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setUserName(USERNAME);
            opts.setPassword(PASSWORD.toCharArray());
            // 不设 cleanSession / automaticReconnect 等，让 Paho 用默认值

            // === 4. 连接 ===
            System.out.println("Connecting to " + broker + " ...");
            client.connect(opts);

            if (client.isConnected()) {
                System.out.println("✅ 连接成功！");

                // === 5. 尝试发布一条消息 ===
                System.out.println("Publishing to " + TEST_TOPIC + " ...");
                MqttMessage msg = new MqttMessage(TEST_PAYLOAD.getBytes("UTF-8"));
                msg.setQos(0);
                client.publish(TEST_TOPIC, msg);
                System.out.println("✅ 发布成功！payload=" + TEST_PAYLOAD);

                // === 6. 等 2 秒再断开，看看有没有回调 ===
                Thread.sleep(2000);
            } else {
                System.out.println("❌ connect() 返回后 isConnected=false");
            }

        } catch (MqttException e) {
            System.out.println("❌ MqttException");
            System.out.println("   reasonCode = " + e.getReasonCode());
            System.out.println("   message    = " + e.getMessage());
            e.printStackTrace(System.out);
        } catch (Throwable t) {
            // 兜底：NPE / Error 全部打堆栈
            System.out.println("❌ " + t.getClass().getSimpleName() + ": " + t.getMessage());
            t.printStackTrace(System.out);
        } finally {
            if (client != null && client.isConnected()) {
                try {
                    client.disconnect();
                    System.out.println("已断开");
                } catch (MqttException ignored) {}
            }
        }
    }
}
