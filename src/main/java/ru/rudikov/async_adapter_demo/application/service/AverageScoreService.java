package ru.rudikov.async_adapter_demo.application.service;

import org.springframework.stereotype.Service;
import ru.rudikov.async_adapter_demo.application.port.primary.ScoringPort;
import ru.rudikov.async_adapter_demo.application.port.secondary.ScoreDetailsPort;
import ru.rudikov.async_adapter_demo.application.port.secondary.SubjectPort;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Service
public class AverageScoreService implements ScoringPort {

    private final List<SubjectPort> ports;
    private final ExecutorService myExecutor;

    private final ScoreDetailsPort scoreDetailsPort;

    public AverageScoreService(List<SubjectPort> ports, ExecutorService myExecutor, ScoreDetailsPort scoreDetailsPort) {
        this.ports = ports;
        this.myExecutor = myExecutor;
        this.scoreDetailsPort = scoreDetailsPort;
    }

    @Override
    public Double getScore(String studentId) {
        List<Future<Double>> scores = new ArrayList<>();

        for (SubjectPort port : ports) {
            scores.add(myExecutor.submit(() -> {
                var avgScore = port.getAverageScore(studentId);
                var subject = port.getSubjectType().name();
                scoreDetailsPort.save(subject, avgScore);
                return avgScore;
            }));
        }

        double totalScore = 0.0;
        for (Future<Double> score : scores) {
            try {
                totalScore += score.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        return totalScore / ports.size();
    }
}
