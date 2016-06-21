
package com.supermy.im.mq;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;


@Configuration
@EnableAutoConfiguration
public class RabbitMQConfig implements CommandLineRunner {

//	final static String queueName = "spring-boot";
	final static String queueName = "jamesmo";
	final static String virtualHost="/abc";
	String exchange = "jamesmo-exchanges";


	@Autowired
	AnnotationConfigApplicationContext context;

	@Autowired
	RabbitTemplate rabbitTemplate;

    @Bean
    public ConnectionFactory connectionFactory() {
		//CachingConnectionFactory connectionFactory = new CachingConnectionFactory("192.168.59.103");//mq 是docker链接名称//192.168.59.103
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory("127.0.0.1");//mq 是docker链接名称//192.168.59.103

		connectionFactory.setUsername("abc");
		connectionFactory.setPassword("abc");
		connectionFactory.setVirtualHost(virtualHost);

//		connectionFactory.setUsername("guest");
//		connectionFactory.setPassword("guest");

        //connectionFactory.setPort(5672);//默认5672
        //Connection connection = connectionFactory.createConnection();

        return  connectionFactory;
    }

    @Bean
	Queue queue() {
		return new Queue(queueName, false);
	}

	@Bean
	TopicExchange exchange() {
		return new TopicExchange(exchange);
	}
//	TopicExchange exchange() {
//		return new TopicExchange("spring-boot-exchange");
//	}

	@Bean
	Binding binding(Queue queue, TopicExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with(queueName);
	}

	/**
	 * 消息消费
	 *
	 * @param connectionFactory
	 * @param listenerAdapter
     * @return
     */
	@Bean
	SimpleMessageListenerContainer container(ConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setQueueNames(queueName);
		//container.setAcknowledgeMode(AcknowledgeMode.MANUAL); //设置确认模式手工确认
		container.setMessageListener(listenerAdapter);
//		container.setMessageListener(new ChannelAwareMessageListener() {
//
//			@Override
//			public void onMessage(Message message, Channel channel) throws Exception {
//				byte[] body = message.getBody();
//				System.out.println("receive msg : " + new String(body));
//				channel.basicAck(message.getMessageProperties().getDeliveryTag(), false); //确认消息成功消费
//			}
//		});

		return container;
	}


    @Bean
    Receiver receiver() {
        return new Receiver();
    }

	@Bean
	MessageListenerAdapter listenerAdapter(Receiver receiver) {
		return new MessageListenerAdapter(receiver, "receiveMessage");
	}

//    public static void main(String[] args) throws InterruptedException {
//
//        SpringApplication.run(RabbitMQConfig.class, args);
//
//    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Waiting five seconds...");
        Thread.sleep(5000);
        System.out.println("Sending message...");

        //调试消息队列信息......
        System.out.println("rabbit host:"+rabbitTemplate.getConnectionFactory().getHost());
        System.out.println("rabbit port:"+rabbitTemplate.getConnectionFactory().getPort());
        System.out.println("rabbit virtual host:" + rabbitTemplate.getConnectionFactory().getVirtualHost());

		/**
		 * 消息生产
		 */
		rabbitTemplate.convertAndSend(queueName, "Hello from RabbitMQ! 你好。");
		/**
		 * 消息消费
		 */
        receiver().getLatch().await(10000, TimeUnit.MILLISECONDS);
        context.close();
    }
}
