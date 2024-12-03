package it.gov.pagopa.gpd.upload.config.duplicate;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
public class NoDuplicateValidator implements ConstraintValidator<NoDuplicate, List<?>> {

    private String fieldName;

    @Override
    public void initialize(NoDuplicate constraintAnnotation) {
        this.fieldName = constraintAnnotation.fieldName();
    }

    @Override
    public boolean isValid(List<?> list, ConstraintValidatorContext context) {
        if (list == null) {
            return true;
        }

        if(Objects.equals(fieldName, "")) {
            HashSet<?> unique = new HashSet<>(list);
            return unique.size() == list.size();
        } else {
            Set<Object> seenValues = new HashSet<>();

            for (Object item : list) {
                try {
                    Field field = item.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(item);
                    if (!seenValues.add(value)) {
                        return false;
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    log.error("Exception while detect duplicates: {}, cause: {}", e.getMessage(), e.getCause().getMessage());
                    return false;
                }
            }

            return true;
        }
    }
}
