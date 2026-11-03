/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.core.posting.impl;
import io.fireflysoftware.domain.accounting.core.posting.*;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import reactor.core.publisher.Mono;
@CommandHandlerComponent
@ConditionalOnProperty(prefix="firefly.accounting.managed-posting",name="enabled",havingValue="true")
public class ManagedPostingCommandHandlerImpl extends CommandHandler<ManagedPostingCommand,ManagedPostingResponse>{
    private final ManagedPostingExecution postings;
    public ManagedPostingCommandHandlerImpl(ManagedPostingExecution postings){this.postings=postings;}
    @Override protected Mono<ManagedPostingResponse> doHandle(ManagedPostingCommand command){return postings.execute(command);}
}
