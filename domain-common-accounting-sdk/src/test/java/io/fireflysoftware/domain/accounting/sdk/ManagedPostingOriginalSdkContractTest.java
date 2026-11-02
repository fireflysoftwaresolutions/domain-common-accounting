/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.sdk;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
/** Unchanged original generator consumes the complete actual controller producer. */
class ManagedPostingOriginalSdkContractTest {
    @Test void canonicalAliasesAndEveryOldAndManagedOperationComeFromTheActualProducer()throws Exception{var raw=Files.readAllBytes(Path.of("src/main/resources/openapi.yml"));assertTrue(raw.length>0&&raw.length<=4194304);try(var a=getClass().getClassLoader().getResourceAsStream("openapi.yml");var b=getClass().getClassLoader().getResourceAsStream("api-spec/openapi.yml")){assertNotNull(a);assertNotNull(b);assertArrayEquals(raw,a.readAllBytes());assertArrayEquals(raw,b.readAllBytes());}var actual=new ObjectMapper(new YAMLFactory()).readTree(raw);var prior=new ObjectMapper().readTree(Files.readAllBytes(Path.of("../domain-common-accounting-web/target/managed-posting-producer/openapi.json")));assertEquals(prior.path("paths"),actual.path("paths"));var operations=new TreeSet<String>();actual.path("paths").fields().forEachRemaining(path->path.getValue().fields().forEachRemaining(method->{if(method.getValue().has("operationId"))assertTrue(operations.add(method.getValue().path("operationId").asText()));}));assertEquals(Set.of("postJournal","readJournal","readAccount","postLedgerJournal","recoverLedgerJournal"),operations);}
    @Test void actualGeneratedManagedReceiptRetainsInstantWithoutTimezoneConversion()throws Exception{var type=Class.forName("io.fireflysoftware.domain.accounting.sdk.model.ManagedPostingReceipt");assertEquals(java.time.Instant.class,type.getMethod("getRecordedAt").getReturnType());assertNotNull(Class.forName("io.fireflysoftware.domain.accounting.sdk.api.ManagedLedgerJournalsApi"));}
}
