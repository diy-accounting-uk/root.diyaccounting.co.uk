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
    void createsSearchConsoleVerificationCnameRecords() {
        Template template = Template.fromStack(synthRootDnsStack("ci.cloudfront.net", "prod.cloudfront.net"));

        template.resourceCountIs("AWS::Route53::RecordSet", 3);
        template.hasResourceProperties(
                "AWS::Route53::RecordSet",
                Match.objectLike(Map.of(
                        "Name",
                        "blmvnokkkgkt.diyaccounting.co.uk.",
                        "Type",
                        "CNAME",
                        "ResourceRecords",
                        List.of("gv-oxvz26pb6fpjaf.dv.googlehosted.com"))));
        template.hasResourceProperties(
                "AWS::Route53::RecordSet",
                Match.objectLike(Map.of(
                        "Name",
                        "lbcjxmzamfi3.diya-gl.co.uk.",
                        "Type",
                        "CNAME",
                        "ResourceRecords",
                        List.of("gv-5364v5fcj5bxln.dv.googlehosted.com"))));
    }

    @Test
    void publishesADmarcRecordThatReportsToSupportAndRejectsNothing() {
        Template template = Template.fromStack(synthRootDnsStack("ci.cloudfront.net", "prod.cloudfront.net"));

        template.hasResourceProperties(
                "AWS::Route53::RecordSet",
                Match.objectLike(Map.of(
                        "Name",
                        "_dmarc.diyaccounting.co.uk.",
                        "Type",
                        "TXT",
                        "TTL",
                        "60",
                        "ResourceRecords",
                        List.of("\"v=DMARC1; p=none; rua=mailto:support@diyaccounting.co.uk\""))));
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
    void certbotRoleTrustsOnlyTheRenewalWorkflowOnMainAndTheOperatorAndCreatesNoProvider() {
        Template template = Template.fromStack(synthRootDnsStack("ci.cloudfront.net", "prod.cloudfront.net"));

        template.hasResourceProperties(
                "AWS::IAM::Role",
                Match.objectLike(
                        Map.of(
                                "RoleName",
                                "root-certbot-dns01",
                                "AssumeRolePolicyDocument",
                                Match.objectLike(
                                        Map.of(
                                                "Statement",
                                                Match.arrayWith(
                                                        List.of(
                                                                Match.objectLike(
                                                                        Map.of(
                                                                                "Action",
                                                                                "sts:AssumeRole",
                                                                                "Principal",
                                                                                Match.objectLike(
                                                                                        Map.of(
                                                                                                "AWS",
                                                                                                Match.anyValue())))),
                                                                Match.objectLike(
                                                                        Map.of(
                                                                                "Action",
                                                                                "sts:AssumeRoleWithWebIdentity",
                                                                                "Principal",
                                                                                Map.of(
                                                                                        "Federated",
                                                                                        "arn:aws:iam::887764105431:oidc-provider/token.actions.githubusercontent.com"),
                                                                                "Condition",
                                                                                Map.of(
                                                                                        "StringEquals",
                                                                                        Map.of(
                                                                                                "token.actions.githubusercontent.com:aud",
                                                                                                "sts.amazonaws.com",
                                                                                                "token.actions.githubusercontent.com:sub",
                                                                                                "repo:diy-accounting-uk/submit.diyaccounting.co.uk:environment:ci",
                                                                                                "token.actions.githubusercontent.com:job_workflow_ref",
                                                                                                "diy-accounting-uk/submit.diyaccounting.co.uk/.github/workflows/local-tls-renew.yml@refs/heads/main")))))))))));
        template.resourceCountIs("Custom::AWSCDKOpenIdConnectProvider", 0);
        template.resourceCountIs("AWS::IAM::OIDCProvider", 0);
    }

    @Test
    void skipsDiyaGlAliasesWhenDomainsAreBlank() {
        Template template = Template.fromStack(synthRootDnsStack("", ""));

        template.resourceCountIs("AWS::Route53::HostedZone", 2);
        template.resourceCountIs("Custom::AWS", 0);
    }

    private static Template synthStaticSites(Map<String, String> aliases) {
        App app = new App();
        return Template.fromStack(new RootDnsStack(
                app,
                "TestRootDnsStack",
                RootDnsStack.RootDnsStackProps.builder()
                        .env(Environment.builder()
                                .account("887764105431")
                                .region("us-east-1")
                                .build())
                        .hostedZoneName("diyaccounting.co.uk")
                        .hostedZoneId("Z0315522208PWZSSBI9AL")
                        .staticSiteAliases(aliases)
                        .build()));
    }

    @Test
    void writesOneAliasPerStaticSiteEntry() {
        Template template = synthStaticSites(Map.of(
                "ci-developer", "dci.cloudfront.net",
                "prod-developer", "dprod.cloudfront.net",
                "developer", "dprod.cloudfront.net"));

        // One A and one AAAA record per alias
        template.resourceCountIs("Custom::AWS", 6);
    }

    @Test
    void writesTheMarketingAliasesBesideTheDeveloperOnes() {
        Template template = synthStaticSites(Map.of(
                "ci-developer", "dci.cloudfront.net",
                "prod-developer", "dprod.cloudfront.net",
                "developer", "dprod.cloudfront.net",
                "ci-marketing", "mci.cloudfront.net",
                "prod-marketing", "mprod.cloudfront.net",
                "marketing", "mprod.cloudfront.net"));

        // One A and one AAAA record per alias
        template.resourceCountIs("Custom::AWS", 12);
    }

    @Test
    void skipsAStaticSiteEntryWhoseDomainIsBlank() {
        Template template = synthStaticSites(Map.of("ci-developer", "", "developer", "dprod.cloudfront.net"));

        template.resourceCountIs("Custom::AWS", 2);
    }
}
