package io.github.divios.airanchorgatewayjava.core.services;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sawtooth.sdk.messaging.Future;
import sawtooth.sdk.messaging.ZmqStream;
import sawtooth.sdk.protobuf.BatchList;
import sawtooth.sdk.protobuf.Message;

@Slf4j
@Service
public class ZMQConnectionService {

    @Value("${zmq.connection.url}")
    private String zmqSocketUrl;

    private ZmqStream zmqStream;

    @PostConstruct
    private void init() {
        try {
            log.info("Attempting to connect to zmq socket in {}", zmqSocketUrl);
            zmqStream = new ZmqStream("tcp://" + zmqSocketUrl);
            log.info("Connected to zmq socket correctly");
        } catch (Exception e) {
            log.error("Error opening socket");
            System.exit(-1);
        }
    }

    public Future send(BatchList batchList) {
        return zmqStream.send(Message.MessageType.CLIENT_BATCH_SUBMIT_REQUEST,
                batchList.toByteString());
    }

}
