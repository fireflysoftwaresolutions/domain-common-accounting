/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.web.openapi.impl;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OriginalProducerSdkInputFixtureTest {
    @TempDir Path root;
    private Properties setup(byte[] original)throws Exception {
        root = root.toRealPath();
        var web=root.resolve("domain-common-accounting-web");Files.createDirectories(web.resolve("target/managed-posting-producer"));
        var canonical=root.resolve("domain-common-accounting-sdk/src/main/resources/openapi.yml");Files.createDirectories(canonical.getParent());Files.write(canonical,original);
        var properties=new Properties();properties.setProperty("accounting.domain.fixture.sdk-owned-hosted","true");properties.setProperty("accounting.domain.fixture.gate","a".repeat(64));properties.setProperty("accounting.domain.fixture.sdk-source-root",root.toString());
        properties.setProperty("accounting.domain.fixture.sdk-canonical-input",canonical.toString());
        properties.setProperty("accounting.domain.fixture.sdk-canonical-preimage-sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original)));return properties;
    }
    @Test void explicitOwnedFixtureCopiesExactRawBytesAndRetainsThePreimageReceipt()throws Exception {
        var original="retained original input\n".getBytes();var raw="actual emitted raw YAML\n".getBytes();var properties=setup(original);
        OriginalProducerSdkInputFixtureImpl.write(raw,root.resolve("domain-common-accounting-web"),properties);
        assertThat(Files.readAllBytes(Path.of(properties.getProperty("accounting.domain.fixture.sdk-canonical-input")))).isEqualTo(raw);
        assertThat(Files.readString(root.resolve("domain-common-accounting-web/target/managed-posting-producer/sdk-canonicalization.json"))).contains("preimageSha256","actualRawProducerSha256");
    }
    @Test void defaultOffAndWrongPreimageOrArbitraryPathCannotWriteTheCanonicalInput()throws Exception {
        var original="retained original input\n".getBytes();var properties=setup(original);var web=root.resolve("domain-common-accounting-web");
        OriginalProducerSdkInputFixtureImpl.write("unrequested".getBytes(),web,new Properties());
        var target=Path.of(properties.getProperty("accounting.domain.fixture.sdk-canonical-input"));assertThat(Files.readAllBytes(target)).isEqualTo(original);
        var missingMarker=(Properties)properties.clone();missingMarker.remove("accounting.domain.fixture.sdk-owned-hosted");
        assertThatThrownBy(()->OriginalProducerSdkInputFixtureImpl.write("bad".getBytes(),web,missingMarker)).isInstanceOf(IllegalStateException.class);
        properties.setProperty("accounting.domain.fixture.sdk-canonical-preimage-sha256","b".repeat(64));
        assertThatThrownBy(()->OriginalProducerSdkInputFixtureImpl.write("bad".getBytes(),web,properties)).isInstanceOf(IllegalStateException.class);
        properties.setProperty("accounting.domain.fixture.sdk-canonical-input",root.resolve("unowned.yml").toString());
        assertThatThrownBy(()->OriginalProducerSdkInputFixtureImpl.write("bad".getBytes(),web,properties)).isInstanceOf(IllegalStateException.class);
        assertThat(Files.readAllBytes(target)).isEqualTo(original);assertThat(root.resolve("unowned.yml")).doesNotExist();
    }
    @Test void aSymlinkCannotRedirectEvenTheDeclaredCanonicalAlias()throws Exception {
        var original="retained original input\n".getBytes();var properties=setup(original);var target=Path.of(properties.getProperty("accounting.domain.fixture.sdk-canonical-input"));
        var outside=root.resolve("retained.yml");Files.write(outside,original);Files.delete(target);Files.createSymbolicLink(target,outside);
        assertThatThrownBy(()->OriginalProducerSdkInputFixtureImpl.write("bad".getBytes(),root.resolve("domain-common-accounting-web"),properties)).isInstanceOf(IllegalStateException.class);
        assertThat(Files.readAllBytes(outside)).isEqualTo(original);
    }
}
