/*
 * SPDX-License-Identifier: LicenseRef-PolyForm-Internal-Use-1.0.0
 * Copyright (C) 2006-2026 DIY Accounting Limited
 */

package co.uk.diyaccounting.root.stacks;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.assertions.Match;
import software.amazon.awscdk.assertions.Template;

class RootDnsStackTest {

    private static RootDnsStack synthRootDnsStack(String ciDiyaGlDomain, String prodDiyaGlDomain) {
        App app = new App();
        return new RootDnsStack(
                app,
                "TestRootDnsStack",
                RootDnsStack.RootDnsStackProps.builder()
                        .env(Environment.builder()
                                .account("887764105431")
                                .region("us-east-1")
                                .build())
                        .hostedZoneName("diyaccounting.co.uk")
                        .hostedZoneId("Z0315522208PWZSSBI9AL")
                        .ciDiyaGlCloudFrontDomain(ciDiyaGlDomain)
                        .prodDiyaGlCloudFrontDomain(prodDiyaGlDomain)
                        .delegateAccountIds(List.of("064390746177"))
                        .build());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> delegatePolicyStatements(Template template) {
        Map<String, Object> resources = (Map<String, Object>) template.toJSON().get("Resources");
        return resources.values().stream()
                .map(resource -> (Map<String, Object>) resource)
                .filter(resource -> "AWS::IAM::Policy".equals(resource.get("Type")))
                .map(resource -> (Map<String, Object>) resource.get("Properties"))
                .map(properties -> (Map<String, Object>) properties.get("PolicyDocument"))
                .flatMap(policyDocument -> ((List<Map<String, Object>>) policyDocument.get("Statement")).stream())
                .filter(statement -> {
                    Object action = statement.get("Action");
                    List<?> actions = action instanceof List<?> list ? list : List.of(action);
                    return actions.contains("route53:ChangeResourceRecordSets")
                            || actions.contains("route53:ListHostedZonesByName");
                })
                .toList();
    }

    @Test
    void createsDiyaGlHostedZonesAndTwelveAliasResources() {
        Template template = Template.fromStack(synthRootDnsStack("ci.cloudfront.net", "prod.cloudfront.net"));

        template.resourceCountIs("AWS::Route53::HostedZone", 2);
        template.hasResourceProperties("AWS::Route53::HostedZone", Match.objectLike(Map.of("Name", "diya-gl.co.uk.")));
        template.hasResourceProperties("AWS::Route53::HostedZone", Match.objectLike(Map.of("Name", "diya-gl.com.")));

        template.resourceCountIs("Custom::AWS", 12);
    }

    @Test
    void delegateRolePolicyListsZonesByNameAndGrantsThreeHostedZoneArns() {
        Template template = Template.fromStack(synthRootDnsStack("ci.cloudfront.net", "prod.cloudfront.net"));

        var statements = delegatePolicyStatements(template);

        boolean hasListHostedZonesByName = statements.stream().anyMatch(statement -> {
            Object action = statement.get("Action");
            return "route53:ListHostedZonesByName".equals(action) && "*".equals(statement.get("Resource"));
        });
        assertTrue(hasListHostedZonesByName, "expected a route53:ListHostedZonesByName statement on Resource *");

        boolean hasThreeZoneArnChangeStatement = statements.stream().anyMatch(statement -> {
            Object action = statement.get("Action");
            List<?> actions = action instanceof List<?> list ? list : List.of(action);
            Object resource = statement.get("Resource");
            return actions.contains("route53:ChangeResourceRecordSets")
                    && resource instanceof List<?> resources
                    && resources.size() == 3;
        });
        assertTrue(
                hasThreeZoneArnChangeStatement,
                "expected a ChangeResourceRecordSets statement with three hosted zone ARNs");
    }

    @Test
    void skipsDiyaGlAliasesWhenDomainsAreBlank() {
        Template template = Template.fromStack(synthRootDnsStack("", ""));

        template.resourceCountIs("AWS::Route53::HostedZone", 2);
        template.resourceCountIs("Custom::AWS", 0);
    }
}
