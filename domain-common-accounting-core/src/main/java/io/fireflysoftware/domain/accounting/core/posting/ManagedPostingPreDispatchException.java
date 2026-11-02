/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.core.posting;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;
/** This invocation has not dispatched a financial Core call; prior attempts are not characterized. */
public final class ManagedPostingPreDispatchException extends ResponseStatusException {
    public ManagedPostingPreDispatchException(HttpStatusCode status){super(status,"Managed posting rejected before financial dispatch; retain any prior original action");}
}
