package io.github.divios.airanchorgatewayjava;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import com.rabbitmq.client.Channel;
import io.github.divios.airanchorgatewayjava.core.services.Receiver;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import sawtooth.sdk.signing.Context;
import sawtooth.sdk.signing.CryptoFactory;

@Configuration
@EnableRabbit
@EnableAsync
@EnableScheduling
public class GatewayConfiguration {

    @Bean
    Context context() {
        return CryptoFactory.createContext("secp256k1");
    }

    @Bean
    @Primary
    @Qualifier("json")
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @Qualifier("cbor")
    CBORMapper cborMapper() {
        return new CBORMapper();
    }

    @Bean
    Queue caQueue() {
        return new Queue("ca_queue", true);
    }

    @Bean
    Queue gatewayQueue() {
        return new Queue("gateway_queue", true);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        var rabbitT = new RabbitTemplate(connectionFactory);
        rabbitT.setMessageConverter(new Jackson2JsonMessageConverter());

        return rabbitT;
    }

}
