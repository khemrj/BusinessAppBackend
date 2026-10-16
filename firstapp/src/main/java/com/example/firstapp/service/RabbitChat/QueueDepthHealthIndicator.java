package com.example.firstapp.service.RabbitChat;


import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.util.Properties;

/** Aggregates all persist shards + the dead-letter queue. Component name in /health/deep: "queueDepth". */
@Component("queueDepth")
public class QueueDepthHealthIndicator extends AbstractHealthIndicator {

    public static final Status DEGRADED = new Status("DEGRADED");
    private static final int BACKLOG_WARN = 10_000;

    private final AmqpAdmin admin;

    public QueueDepthHealthIndicator(AmqpAdmin admin) {
        super("Queue depth health check failed");
        this.admin = admin;
    }

    @Override
    protected void doHealthCheck(Health.Builder b) {
        int totalDepth = 0;
        int minConsumers = Integer.MAX_VALUE;
        for (String q : ChatRabbitConfig.persistQueueNames()) {
            Properties p = admin.getQueueProperties(q);
            if (p == null) { b.down().withDetail("reason", "missing queue " + q); return; }
            totalDepth += (Integer) p.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
            minConsumers = Math.min(minConsumers, (Integer) p.get(RabbitAdmin.QUEUE_CONSUMER_COUNT));
        }
        Properties dead = admin.getQueueProperties(ChatRabbitConfig.DEAD_Q);
        int deadDepth = dead == null ? 0 : (Integer) dead.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);

        b.withDetail("persistDepth", totalDepth)
         .withDetail("minConsumersPerShard", minConsumers)
         .withDetail("deadLetterDepth", deadDepth);

        if (minConsumers == 0)                              b.down().withDetail("reason", "a shard has no consumer");
        else if (deadDepth > 0 || totalDepth > BACKLOG_WARN) b.status(DEGRADED);
        else                                                b.up();
    }
}
