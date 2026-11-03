/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.web.openapi;
import org.fireflyframework.web.openapi.EnableOpenApiGen;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.RestController;
/** Original build producer discovers actual controllers only; it supplies no financial authority. */
@EnableOpenApiGen
@ComponentScan(basePackages="io.fireflysoftware.domain.accounting.web",useDefaultFilters=false,includeFilters=@ComponentScan.Filter(type=FilterType.ANNOTATION,classes=RestController.class))
public class OpenApiGenApplication {public static void main(String[] args){SpringApplication.run(OpenApiGenApplication.class,args);}}
