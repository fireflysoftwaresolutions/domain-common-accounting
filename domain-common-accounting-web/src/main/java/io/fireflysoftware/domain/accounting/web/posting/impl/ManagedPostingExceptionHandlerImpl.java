/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.web.posting.impl;
import io.fireflysoftware.domain.accounting.core.posting.*;
import io.fireflysoftware.domain.accounting.web.posting.ManagedLedgerPostingsController;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice(assignableTypes=ManagedLedgerPostingsController.class)
public class ManagedPostingExceptionHandlerImpl {
    @ExceptionHandler(ManagedPostingPreDispatchException.class)
    public ResponseEntity<ManagedPostingPreDispatchError> preDispatch(ManagedPostingPreDispatchException error){return ResponseEntity.status(error.getStatusCode()).contentType(MediaType.APPLICATION_JSON).cacheControl(CacheControl.noStore()).body(new ManagedPostingPreDispatchError(error.getStatusCode().value(),"MANAGED_POSTING_PRE_DISPATCH_REJECTED","PRE_DISPATCH"));}
}
