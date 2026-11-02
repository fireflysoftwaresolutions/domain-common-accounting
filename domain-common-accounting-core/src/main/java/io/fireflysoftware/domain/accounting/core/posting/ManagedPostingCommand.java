/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.core.posting;
import java.util.*;
import org.fireflyframework.cqrs.command.Command;
/** Immutable same-action submission/recovery; bearer credentials never enter CQRS payloads. */
public record ManagedPostingCommand(String tenantId,String bookId,UUID actionId,String expectedIntentDigest,String originalPostingJson,boolean recovery) implements Command<ManagedPostingResponse>{
    public ManagedPostingCommand{
        for(String id:List.of(tenantId,bookId)){var value=UUID.fromString(id);if(!value.toString().equals(id)||value.equals(new UUID(0,0)))throw invalid();}
        if(actionId==null||actionId.equals(new UUID(0,0))||expectedIntentDigest==null||!expectedIntentDigest.matches("[0-9a-f]{64}")||originalPostingJson==null||originalPostingJson.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>1048576)throw invalid();
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("Exact original managed posting required");}
    @Override public String toString(){return "ManagedPostingCommand[redacted]";}
}
