package io.github.divios.airanchorgatewayjava.data.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Data
@Document("locations")
@Builder
public class TransactionModel {

    @MongoId
    private String id;

    private String sender;
    private String signer;
    private String ca;
    private String hash;

}
