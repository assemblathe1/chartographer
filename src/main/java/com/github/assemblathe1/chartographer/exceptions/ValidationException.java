package com.github.assemblathe1.chartographer.exceptions;

import lombok.Data;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ValidationException extends RuntimeException {
    private final List<String> errorFieldsMessages;

    public ValidationException(List<String> errorFieldsMessages) {
        super(errorFieldsMessages.stream().collect(Collectors.joining(", ")));
        this.errorFieldsMessages = errorFieldsMessages;
    }
}
