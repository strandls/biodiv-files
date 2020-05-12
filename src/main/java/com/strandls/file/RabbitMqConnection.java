/**
 * 
 */
package com.strandls.file;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.strandls.file.util.PropertyFileUtil;

/**
 * @author Abhishek Rudra
 *
 */
public class RabbitMqConnection {

	public static final String EXCHANGE;
	public static final String QUEUE;
	public static final String ROUTING_KEY;
	private static final String HOST;
	private static final Integer PORT;
	private static final String USERNAME;
	private static final String PASSWORD;

	static {
		Properties props = PropertyFileUtil.fetchProperty("config.properties");
		EXCHANGE = props.getProperty("rabbitmq_exchange");
		QUEUE = props.getProperty("rabbitmq_queue");
		ROUTING_KEY = props.getProperty("rabbitmq_routingKey");
		HOST = props.getProperty("rabbitmq_host");
		PORT = Integer.parseInt(props.getProperty("rabbitmq_port"));
		USERNAME = props.getProperty("rabbitmq_username");
		PASSWORD = props.getProperty("rabbitmq_password");
	}

	public Channel setRabbitMQConnetion() throws IOException, TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(HOST);
		factory.setPort(PORT);
		factory.setUsername(USERNAME);
		factory.setPassword(PASSWORD);
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();
		channel.exchangeDeclare(EXCHANGE, "direct");
		channel.queueDeclare(QUEUE, false, false, false, null);
		channel.queueBind(QUEUE, EXCHANGE, ROUTING_KEY);
		return channel;
	}
}
