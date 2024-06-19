package ru.rudikov.async_adapter_demo.application.port.secondary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rudikov.async_adapter_demo.application.port.secondary.model.ScoreDetailsEntity;

@Repository
public interface ScoreDetailsPort extends JpaRepository<ScoreDetailsEntity, Long> {

    default void save(String subject, Double avgScore) {
        var entity = new ScoreDetailsEntity(subject, avgScore);
        this.save(entity);
    }
}
