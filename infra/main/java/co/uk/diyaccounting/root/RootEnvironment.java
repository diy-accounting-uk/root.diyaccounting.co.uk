/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright (C) 2025-2026 DIY Accounting Ltd
 */

package co.uk.diyaccounting.root;

import static co.uk.diyaccounting.root.utils.Kind.envOr;
import static co.uk.diyaccounting.root.utils.Kind.infof;

import co.uk.diyaccounting.root.stacks.ApexStack;
import co.uk.diyaccounting.root.stacks.CostReportingStack;
import co.uk.diyaccounting.root.stacks.CostReportingUE1Stack;
import co.uk.diyaccounting.root.stacks.CostReportingUE1Stack.LinkedAccountBudget;
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
    public final CostReportingStack costReportingStack;
    public final CostReportingUE1Stack costReportingUE1Stack;

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

        // Cost instrumentation: one bucket + catalogue in eu-west-2 alongside the rest of the
        // estate, one sibling stack in us-east-1 for the billing control-plane resources that
        // only run there (see CostReportingUE1Stack's class comment). The management account ID
        // is a literal here, not derived from CDK_DEFAULT_ACCOUNT: cost filters and cost-category
        // rules for the other five accounts are already literal cross-account IDs, since none of
        // those accounts is ever "this stack's account", so the management account is written the
        // same way for consistency and to keep synth working with no AWS credentials configured.
        String managementAccount = "887764105431";
        String costReportsBucketName = "diy-accounting-cost-reports-" + managementAccount;
        String euWest2Region = "eu-west-2";
        Environment euWest2Env = Environment.builder()
                .region(euWest2Region)
                .account(managementAccount)
                .build();
        Environment costReportingUsEast1Env = Environment.builder()
                .region("us-east-1")
                .account(managementAccount)
                .build();

        String costReportingStackId = "root-CostReportingStack";
        infof("Synthesizing stack %s", costReportingStackId);
        this.costReportingStack = new CostReportingStack(
                app,
                costReportingStackId,
                CostReportingStack.CostReportingStackProps.builder()
                        .env(euWest2Env)
                        .bucketName(costReportsBucketName)
                        .glueDatabaseName("cost_and_usage")
                        .athenaWorkGroupName("diy-accounting-cost-reports")
                        .build());

        String costReportingUE1StackId = "root-CostReportingUE1Stack";
        infof("Synthesizing stack %s", costReportingUE1StackId);
        this.costReportingUE1Stack = new CostReportingUE1Stack(
                app,
                costReportingUE1StackId,
                CostReportingUE1Stack.CostReportingUE1StackProps.builder()
                        .env(costReportingUsEast1Env)
                        .exportBucketName(costReportsBucketName)
                        .exportBucketRegion(euWest2Region)
                        .linkedAccounts(linkedAccountBudgets(managementAccount))
                        .organisationBudgetLimitUsd(200)
                        .alertEmail("admin@diyaccounting.co.uk")
                        .build());
    }

    /** The six linked accounts, their Workload cost-category names and monthly budgets. */
    private static List<LinkedAccountBudget> linkedAccountBudgets(String managementAccount) {
        return List.of(
                new LinkedAccountBudget(managementAccount, "management", 25),
                new LinkedAccountBudget("283165661847", "gateway", 5),
                new LinkedAccountBudget("064390746177", "spreadsheets", 15),
                new LinkedAccountBudget("367191799875", "submit-ci", 30),
                new LinkedAccountBudget("972912397388", "submit-prod", 120),
                new LinkedAccountBudget("914216784828", "submit-backup", 5));
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
