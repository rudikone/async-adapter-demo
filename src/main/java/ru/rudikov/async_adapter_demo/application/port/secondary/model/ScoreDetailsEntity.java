package ru.rudikov.async_adapter_demo.application.port.secondary.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import org.springframework.data.annotation.Id;

import java.util.Objects;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
public class ScoreDetailsEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private String subject;

    private Double avgScore;

    public ScoreDetailsEntity() {
    }

    public ScoreDetailsEntity(String subject, Double avgScore) {
        this.subject = subject;
        this.avgScore = avgScore;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Double getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(Double avgScore) {
        this.avgScore = avgScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScoreDetailsEntity that = (ScoreDetailsEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(subject, that.subject) && Objects.equals(avgScore, that.avgScore);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, subject, avgScore);
    }
}
