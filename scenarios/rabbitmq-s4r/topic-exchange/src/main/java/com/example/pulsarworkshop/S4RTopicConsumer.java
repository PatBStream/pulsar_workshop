package com.example.pulsarworkshop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.pulsarworkshop.exception.WorkshopRuntimException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class S4RTopicConsumer extends S4RCmdApp {
    private final static String APP_NAME = "S4RTopicConsumer";
    static { System.setProperty("log_file_base_name", getLogFileName(API_TYPE, APP_NAME)); }
    private final static Logger logger = LoggerFactory.getLogger(S4RTopicConsumer.class);
    DefaultConsumer consumer;
    public S4RTopicConsumer(String appName, String[] inputParams) {
        super(appName, inputParams);
    }

    public static void main(String[] args) {
        PulsarWorkshopCmdApp workshopApp = new S4RTopicConsumer("S4RTopicConsumer", args);
        int exitCode = workshopApp.runCmdApp();
        System.exit(exitCode);
    }

    @Override
    public void execute() {
        try {
            S4RFactory= new ConnectionFactory();
            S4RFactory.setHost(S4RRabbitMQHost);
            S4RFactory.setPort(S4RPort);
            S4RFactory.setUsername(S4RUser);
            S4RFactory.setPassword(S4RPassword);
            S4RFactory.setVirtualHost(S4RVirtualHost);
            if(AstraInUse) {
                S4RFactory.useSslProtocol();    
            }
            connection = S4RFactory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(S4RExchangeName, BuiltinExchangeType.TOPIC);
            channel.queueDeclare(S4RQueueName, true, false, false, null);
            channel.queueBind(S4RQueueName, S4RExchangeName, S4RRoutingKey); //Routing key is ignored in "fanout" exchange
            logger.info("Queue name: " + S4RQueueName + " Exchange name: " + S4RExchangeName + " RoutingKey: " + S4RRoutingKey);
            consumer = new DefaultConsumer(channel) {
                @Override
                 public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        String message = new String(body, "UTF-8");
                        // process the message
                        logger.info("SR4 Consumer received message count: " + MsgReceived + " Message: " + message + " RoutingKey: " +
                        envelope.getRoutingKey());
                        MsgReceived++;
                 }
            };
            channel.basicConsume(S4RQueueName, true, consumer);
            logger.info("SR4 Consumer created for queue " + S4RQueueName + " running until " + numMsg + " messages are received.");
            while (numMsg > MsgReceived) {
                Thread.sleep(2000);    
            }
        } catch (Exception e) {
            throw new WorkshopRuntimException("Unexpected error when consuming S4R messages: " + e.getMessage());   
        }
    }

    @Override
    public void termCmdApp() {
        try {
            channel.close();
            connection.close();
        } catch (IOException ioe) {
            throw new WorkshopRuntimException("Unexpected error when shutting down S4R Consumer IO Exception: " + ioe.getMessage());  
        } catch (TimeoutException te) {
            throw new WorkshopRuntimException("Unexpected error when shutting down S4R Consumer Timeout Exception: " + te.getMessage());  
        }
    }
}
