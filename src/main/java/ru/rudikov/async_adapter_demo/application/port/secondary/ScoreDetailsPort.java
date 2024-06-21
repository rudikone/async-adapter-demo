package ru.rudikov.async_adapter_demo.application.port.secondary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.rudikov.async_adapter_demo.application.port.secondary.model.ScoreDetailsEntity;

import java.util.List;

@Repository
public interface ScoreDetailsPort extends JpaRepository<ScoreDetailsEntity, Long> {

    default void save(String studentId, String subject, Double avgScore) {
        var entity = new ScoreDetailsEntity(studentId, subject, avgScore);
        this.save(entity);
    }

    List<ScoreDetailsEntity> findAllByStudentId(String studentId);
}
