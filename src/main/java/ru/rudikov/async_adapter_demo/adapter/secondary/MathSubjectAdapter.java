package ru.rudikov.async_adapter_demo.adapter.secondary;

import org.springframework.stereotype.Service;
import ru.rudikov.async_adapter_demo.application.port.secondary.SubjectPort;
import ru.rudikov.async_adapter_demo.application.port.secondary.model.SubjectType;

import java.time.Duration;
import java.util.Random;

import static java.time.temporal.ChronoUnit.SECONDS;
import static ru.rudikov.async_adapter_demo.application.port.secondary.model.SubjectType.MATH;

@Service
public class MathSubjectAdapter implements SubjectPort {

    @Override
    public SubjectType getSubjectType() {
        return MATH;
    }

    @Override
    public Double getAverageScore(String studentId) {
        try {
            Thread.sleep(Duration.of(new Random().nextLong(1, 10), SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return new Random().nextDouble(1, 5);
    }
}
