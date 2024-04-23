package it.gov.pagopa.gpd.upload.utils;

import io.micronaut.context.annotation.Context;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Set;

@Singleton
@Context
@Slf4j
public class GPDValidator<T> {
    private final Validator validator;

    @Inject
    public GPDValidator(Validator validator) {
        this.validator = validator;
    }

    public boolean isValid(T model) throws IOException {
        log.info("[GPDValidator@isValid] Starting validation for object related to " + model.hashCode());
        Set<ConstraintViolation<T>> constraintViolations;
        constraintViolations = validator.validate(model);
        if(!constraintViolations.isEmpty()) {
            log.error("[Error][GPDValidator@isValid] Validation error for object related to " + model.hashCode());
            for(ConstraintViolation<T> cv : constraintViolations) {
                log.error("[Error][GPDValidator@isValid] Invalid value: " + cv.getInvalidValue());
                log.error("[Error][GPDValidator@isValid] Invalid value message: " + cv.getMessage());
                log.error("[Error][GPDValidator@isValid] Invalid value descriptor: " + cv.getConstraintDescriptor());
            }
            return false;
        }
        log.info("[GPDValidator@isValid] PaymentPosition with id " + model.hashCode() + " validated");
        return true;
    }
}
