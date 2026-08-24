/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright (C) 2025-2026 DIY Accounting Ltd
 */

package co.uk.diyaccounting.root;

import static co.uk.diyaccounting.root.utils.Kind.envOr;
import static co.uk.diyaccounting.root.utils.Kind.infof;

import co.uk.diyaccounting.root.stacks.ApexStack;
import co.uk.diyaccounting.root.stacks.RootDnsStack;
import co.uk.diyaccounting.root.utils.KindCdk;
import java.util.Arrays;
import java.util.List;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;

/**
 * CDK entry point for the root account DNS management.
 * Deploys RootDnsStack which manages Route53 alias records
 * for gateway and spreadsheets CloudFront distributions, and
 * ApexStack (ci + prod) which serves the apex holding pages.
 * <p>
 * Deployed by deploy.yml (manual dispatch only).
 */
public class RootEnvironment {

    public final RootDnsStack rootDnsStack;
    public final ApexStack ciApexStack;
    public final ApexStack prodApexStack;

    public static void main(final String[] args) {
        App app = new App();

        var hostedZoneName = KindCdk.getContextValueString(app, "hostedZoneName", "diyaccounting.co.uk");
        var hostedZoneId = KindCdk.getContextValueString(app, "hostedZoneId", "");
        var ciGatewayCfDomain = envOr(
                "CI_GATEWAY_CLOUDFRONT_DOMAIN", KindCdk.getContextValueString(app, "ciGatewayCloudFrontDomain", ""));
        var prodGatewayCfDomain = envOr(
                "PROD_GATEWAY_CLOUDFRONT_DOMAIN",
                KindCdk.getContextValueString(app, "prodGatewayCloudFrontDomain", ""));
        var ciSpreadsheetsCfDomain = envOr(
                "CI_SPREADSHEETS_CLOUDFRONT_DOMAIN",
                KindCdk.getContextValueString(app, "ciSpreadsheetsCloudFrontDomain", ""));
        var prodSpreadsheetsCfDomain = envOr(
                "PROD_SPREADSHEETS_CLOUDFRONT_DOMAIN",
                KindCdk.getContextValueString(app, "prodSpreadsheetsCloudFrontDomain", ""));
        var apexCfDomain =
                envOr("APEX_CLOUDFRONT_DOMAIN", KindCdk.getContextValueString(app, "apexCloudFrontDomain", ""));
        var wwwCfDomain = envOr("WWW_CLOUDFRONT_DOMAIN", KindCdk.getContextValueString(app, "wwwCloudFrontDomain", ""));
        var spreadsheetsCfDomain = envOr(
                "SPREADSHEETS_CLOUDFRONT_DOMAIN",
                KindCdk.getContextValueString(app, "spreadsheetsCloudFrontDomain", ""));
        var ciSpreadsheetsHoldingCfDomain = envOr(
                "CI_SPREADSHEETS_HOLDING_CLOUDFRONT_DOMAIN",
                KindCdk.getContextValueString(app, "ciSpreadsheetsHoldingCloudFrontDomain", ""));
        var prodSpreadsheetsHoldingCfDomain = envOr(
                "PROD_SPREADSHEETS_HOLDING_CLOUDFRONT_DOMAIN",
                KindCdk.getContextValueString(app, "prodSpreadsheetsHoldingCloudFrontDomain", ""));

        // Comma-separated list of service account IDs the cross-account delegate roles trust
        var delegateAccountsCsv = envOr("DELEGATE_ACCOUNTS", "");
        List<String> delegateAccountIds = delegateAccountsCsv.isBlank()
                ? List.of()
                : Arrays.stream(delegateAccountsCsv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

        var root = new RootEnvironment(
                app,
                hostedZoneName,
                hostedZoneId,
                ciGatewayCfDomain,
                prodGatewayCfDomain,
                ciSpreadsheetsCfDomain,
                prodSpreadsheetsCfDomain,
                apexCfDomain,
                wwwCfDomain,
                spreadsheetsCfDomain,
                ciSpreadsheetsHoldingCfDomain,
                prodSpreadsheetsHoldingCfDomain,
                delegateAccountIds);
        app.synth();
        infof("CDK synth complete for root DNS environment");
    }

    public RootEnvironment(
            App app,
            String hostedZoneName,
            String hostedZoneId,
            String ciGatewayCfDomain,
            String prodGatewayCfDomain,
            String ciSpreadsheetsCfDomain,
            String prodSpreadsheetsCfDomain,
            String apexCfDomain,
            String wwwCfDomain,
            String spreadsheetsCfDomain,
            String ciSpreadsheetsHoldingCfDomain,
            String prodSpreadsheetsHoldingCfDomain,
            List<String> delegateAccountIds) {
        // Root account DNS management runs in us-east-1 (Route53 is global but CDK needs a region)
        Environment usEast1Env = Environment.builder()
                .region("us-east-1")
                .account(KindCdk.buildPrimaryEnvironment().getAccount())
                .build();

        String stackId = "root-RootDnsStack";
        infof("Synthesizing stack %s", stackId);

        this.rootDnsStack = new RootDnsStack(
                app,
                stackId,
                RootDnsStack.RootDnsStackProps.builder()
                        .env(usEast1Env)
                        .hostedZoneName(hostedZoneName)
                        .hostedZoneId(hostedZoneId)
                        .ciGatewayCloudFrontDomain(ciGatewayCfDomain)
                        .prodGatewayCloudFrontDomain(prodGatewayCfDomain)
                        .ciSpreadsheetsCloudFrontDomain(ciSpreadsheetsCfDomain)
                        .prodSpreadsheetsCloudFrontDomain(prodSpreadsheetsCfDomain)
                        .apexCloudFrontDomain(apexCfDomain)
                        .wwwCloudFrontDomain(wwwCfDomain)
                        .spreadsheetsCloudFrontDomain(spreadsheetsCfDomain)
                        .ciSpreadsheetsHoldingCloudFrontDomain(ciSpreadsheetsHoldingCfDomain)
                        .prodSpreadsheetsHoldingCloudFrontDomain(prodSpreadsheetsHoldingCfDomain)
                        .delegateAccountIds(delegateAccountIds)
                        .build());

        // Apex holding pages: one distribution per environment, both in the management account.
        String ciApexStackId = "ci-root-ApexStack";
        infof("Synthesizing stack %s", ciApexStackId);
        this.ciApexStack = new ApexStack(
                app,
                ciApexStackId,
                ApexStack.ApexStackProps.builder()
                        .env(usEast1Env)
                        .envName("ci")
                        .deploymentName("ci")
                        .resourceNamePrefix("ci-root")
                        .cloudTrailEnabled("false")
                        .sharedNames(buildHoldingSharedNames("ci", hostedZoneName, usEast1Env.getAccount()))
                        .hostedZoneName(hostedZoneName)
                        .hostedZoneId(hostedZoneId)
                        .holdingDocRootPath("../web/holding")
                        .liveDomainNames(apexFailoverDomainNames("ci", hostedZoneName))
                        .accessLogGroupRetentionPeriodDays(90)
                        .build());

        String prodApexStackId = "prod-root-ApexStack";
        infof("Synthesizing stack %s", prodApexStackId);
        this.prodApexStack = new ApexStack(
                app,
                prodApexStackId,
                ApexStack.ApexStackProps.builder()
                        .env(usEast1Env)
                        .envName("prod")
                        .deploymentName("prod")
                        .resourceNamePrefix("prod-root")
                        .cloudTrailEnabled("false")
                        .sharedNames(buildHoldingSharedNames("prod", hostedZoneName, usEast1Env.getAccount()))
                        .hostedZoneName(hostedZoneName)
                        .hostedZoneId(hostedZoneId)
                        .holdingDocRootPath("../web/holding")
                        .liveDomainNames(apexFailoverDomainNames("prod", hostedZoneName))
                        .accessLogGroupRetentionPeriodDays(90)
                        .build());
    }

    /**
     * The live domains the apex holding distribution serves during a failover. deploy-holding.yml
     * moves exactly these aliases onto the holding distribution, and CloudFront refuses an alias the
     * distribution's certificate does not cover, so ApexStack also issues its certificate over them.
     */
    private static List<String> apexFailoverDomainNames(String envName, String hostedZoneName) {
        return "prod".equals(envName)
                ? List.of(hostedZoneName, "www." + hostedZoneName, "prod-gateway." + hostedZoneName)
                : List.of(envName + "-gateway." + hostedZoneName);
    }

    /**
     * Builds the shared-names bundle ApexStack needs, scoped to the apex holding domain
     * for the given environment. subDomainName is "holding" so the derived domain fields
     * (envDomainName/publicDomainName) line up with the actual holding domain name.
     */
    private static SubmitSharedNames buildHoldingSharedNames(String envName, String hostedZoneName, String account) {
        var props = new SubmitSharedNames.SubmitSharedNamesProps();
        props.hostedZoneName = hostedZoneName;
        props.envName = envName;
        props.subDomainName = "holding";
        props.deploymentName = envName;
        props.regionName = "us-east-1";
        props.awsAccount = account;
        return new SubmitSharedNames(props);
    }
}
