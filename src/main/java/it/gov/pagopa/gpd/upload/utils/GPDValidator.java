package it.gov.pagopa.gpd.upload.utils;

import io.micronaut.context.annotation.Context;
import io.micronaut.http.HttpStatus;
import it.gov.pagopa.gpd.upload.exception.AppException;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Singleton
@Context
@Slf4j
public class GPDValidator<T> {

    public boolean isValid(T model) throws IOException {
        ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<T>> constraintViolations;
        constraintViolations = validator.validate(model);

        if(!constraintViolations.isEmpty()) {
            Set<String> invalidValues = new HashSet<>();
            log.error("[Error][GPDValidator@isValid] Validation error for object related to {}", model.hashCode());
            for(ConstraintViolation<T> cv : constraintViolations) {
                log.error(String.format("[Error][GPDValidator@isValid] Invalid value: %s, invalid value message: %s",
                        cv.getInvalidValue(), cv.getMessage()));
                invalidValues.add(cv.getMessage());
            }
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID DEBT POSITIONS",
                    "The format of the debt positions in the uploaded file is invalid. Invalid values: " + invalidValues);
        }

        log.info("[GPDValidator@isValid] PaymentPosition with id {} validated", model.hashCode());
        return true;
    }
}
