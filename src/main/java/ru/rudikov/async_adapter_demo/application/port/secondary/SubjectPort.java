package ru.rudikov.async_adapter_demo.application.port.secondary;

import ru.rudikov.async_adapter_demo.application.port.secondary.model.SubjectType;

public interface SubjectPort {

    Double getAverageScore(String studentId);

    SubjectType getSubjectType();
}
