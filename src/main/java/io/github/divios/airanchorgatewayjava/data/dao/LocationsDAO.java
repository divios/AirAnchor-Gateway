package io.github.divios.airanchorgatewayjava.data.dao;

import io.github.divios.airanchorgatewayjava.data.model.TransactionModel;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LocationsDAO extends MongoRepository<TransactionModel, String> {
}
