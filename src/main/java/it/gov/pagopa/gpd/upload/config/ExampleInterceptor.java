package it.gov.pagopa.gpd.upload.config;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.Nullable;

import jakarta.inject.Singleton;


@Singleton
@InterceptorBean(ExampleAnnotation.class)
public class ExampleInterceptor implements MethodInterceptor<Object, Object> {

    @Nullable
    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        // <ExampleInterceptor proxy code here>
        // @ExampleAnnotation to use it around method
        return context.proceed();
    }
}