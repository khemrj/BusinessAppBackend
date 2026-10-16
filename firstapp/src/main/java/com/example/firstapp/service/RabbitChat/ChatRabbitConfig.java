package com.example.firstapp.service.RabbitChat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Topology:
 *   chat.events (topic)
 *     message.created.{0..SHARDS-1} --> chat.persist.q.{shard}   (1 active consumer each: preserves per-conversation order)
 *     message.created.*             --> chat.push.q             (offline push)
 *   failures after delivery-limit    --> chat.dlx --> chat.dead.q
 *
 * shard = conversationId % SHARDS, so every message of one conversation goes through ONE queue, ONE consumer.
 * Pick SHARDS generously now (changing it later means draining queues first).
 */
@Configuration
public class ChatRabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(ChatRabbitConfig.class);

    public static final String EXCHANGE = "chat.events";
    public static final String DLX = "chat.dlx";
    public static final String PUSH_Q = "chat.push.q";
    public static final String DEAD_Q = "chat.dead.q";
    public static final int SHARDS = 8;

    public static String persistQueue(int shard) { return "chat.persist.q." + shard; }

    public static String routingKey(Long conversationId) {
        return "message.created." + Math.floorMod(conversationId, SHARDS);
    }

    /** Used by @RabbitListener via SpEL and by the health indicator. */
    public static String[] persistQueueNames() {
        return IntStream.range(0, SHARDS).mapToObj(ChatRabbitConfig::persistQueue).toArray(String[]::new);
    }

    @Bean TopicExchange chatExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DLX).durable(true).build();
    }

    @Bean Declarables persistShards() {
        List<Declarable> all = new ArrayList<>();
        for (int i = 0; i < SHARDS; i++) {
            Queue q = QueueBuilder.durable(persistQueue(i)).quorum()
                    .singleActiveConsumer()                 // extra app instances stand by for failover
                    .deadLetterExchange(DLX).deadLetterRoutingKey("dead")
                    .deliveryLimit(5)
                    .build();
            all.add(q);
            all.add(BindingBuilder.bind(q).to(chatExchange()).with("message.created." + i));
        }
        return new Declarables(all);
    }

    @Bean Queue pushQueue() {
        return QueueBuilder.durable(PUSH_Q).quorum()
                .deadLetterExchange(DLX).deadLetterRoutingKey("dead")
                .deliveryLimit(3)
                .ttl(60 * 60 * 1000)
                .build();
    }

    @Bean Queue deadQueue() { return QueueBuilder.durable(DEAD_Q).quorum().build(); }

    @Bean Binding pushBinding() {
        return BindingBuilder.bind(pushQueue()).to(chatExchange()).with("message.created.*");
    }

    @Bean Binding deadBinding() {
        return BindingBuilder.bind(deadQueue()).to(deadLetterExchange()).with("dead");
    }

    @Bean MessageConverter jsonConverter() { return new Jackson2JsonMessageConverter(); }

    @Bean RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(converter);
        t.setMandatory(true);
        t.setReturnsCallback(r -> log.error("UNROUTABLE message: key={} reply={}", r.getRoutingKey(), r.getReplyText()));
        return t;
    }
}
