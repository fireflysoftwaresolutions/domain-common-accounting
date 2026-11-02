/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.domain.accounting.core.posting;
import java.net.URI;
import java.util.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
/** Fixed routing limits do not grant financial permission; the current Core authorities remain mandatory. */
@ConfigurationProperties("firefly.accounting.managed-posting")
public record ManagedPostingSettings(boolean enabled,URI endpoint,String issuer,URI jwkSetUri,String domainAudience,String targetAudience,
        String inboundClientId,String delegatingClientId,String registrationId,String platformTenant,UUID resourceTenantId,UUID bookId,
        String tenantClaim,String clientClaim,String kindClaim,String staffClaim,String scopeClaim,URI policyEndpoint,String policyDecisionPath,boolean allowLoopbackHttp){
    public ManagedPostingSettings{
        if(enabled){for(String value:List.of(issuer,domainAudience,targetAudience,inboundClientId,delegatingClientId,registrationId,platformTenant,tenantClaim,clientClaim,kindClaim,staffClaim,scopeClaim))text(value);
            for(URI value:List.of(endpoint,URI.create(issuer),jwkSetUri,policyEndpoint))secure(value,allowLoopbackHttp);
            if(policyDecisionPath==null||!policyDecisionPath.matches("[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*")||policyDecisionPath.length()>255||!Set.of("","/").contains(policyEndpoint.getRawPath())||!Set.of("","/").contains(endpoint.getRawPath())||domainAudience.equals(targetAudience)||resourceTenantId==null||resourceTenantId.equals(new UUID(0,0))||bookId==null||bookId.equals(new UUID(0,0))||Set.of(tenantClaim,clientClaim,kindClaim,staffClaim,scopeClaim).size()!=5)throw invalid();}
    }
    public static void secure(URI value,boolean loopback){if(value==null||value.getHost()==null||value.getRawUserInfo()!=null||value.getRawQuery()!=null||value.getRawFragment()!=null||!value.equals(value.normalize())
        ||!("https".equals(value.getScheme())||loopback&&"http".equals(value.getScheme())&&Set.of("127.0.0.1","localhost","::1","[::1]").contains(value.getHost())))throw invalid();}
    private static void text(String value){if(value==null||value.isBlank()||value.length()>512||value.codePoints().anyMatch(Character::isISOControl))throw invalid();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("Explicit managed posting caller and target configuration required");}
    @Override public String toString(){return "ManagedPostingSettings[redacted]";}
}
