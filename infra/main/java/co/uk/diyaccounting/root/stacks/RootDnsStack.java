/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright (C) 2025-2026 DIY Accounting Ltd
 */

package co.uk.diyaccounting.root.stacks;

import static co.uk.diyaccounting.root.utils.Kind.infof;
import static co.uk.diyaccounting.root.utils.KindCdk.cfnOutput;

import co.uk.diyaccounting.root.utils.Route53AliasUpsert;
import java.util.List;
import org.immutables.value.Value;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Tags;
import software.amazon.awscdk.services.iam.AccountPrincipal;
import software.amazon.awscdk.services.iam.ArnPrincipal;
import software.amazon.awscdk.services.iam.CompositePrincipal;
import software.amazon.awscdk.services.iam.IPrincipal;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneAttributes;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.constructs.Construct;

/**
 * RootDnsStack: Manages Route53 alias records in the root account zone
 * for the gateway and spreadsheets CloudFront distributions.
 * <p>
 * Records:
 * - ci-gateway.diyaccounting.co.uk → CI gateway CloudFront
 * - prod-gateway.diyaccounting.co.uk → prod gateway CloudFront
 * - ci-spreadsheets.diyaccounting.co.uk → CI spreadsheets CloudFront
 * - prod-spreadsheets.diyaccounting.co.uk → prod spreadsheets CloudFront
 * - diyaccounting.co.uk (apex) → prod gateway CloudFront
 * - www.diyaccounting.co.uk → prod gateway CloudFront
 * - spreadsheets.diyaccounting.co.uk → prod spreadsheets CloudFront
 * - ci-holding.spreadsheets.diyaccounting.co.uk → CI spreadsheets holding CloudFront
 * - holding.spreadsheets.diyaccounting.co.uk → prod spreadsheets holding CloudFront
 * - local.submit.diyaccounting.co.uk → 127.0.0.1 (developer loopback, for the local TLS front door)
 */
public class RootDnsStack extends Stack {

    // Operator's AWS Identity Center (SSO) role in this account. root-certbot-dns01 trusts this
    // identity so `aws --profile certbot-local sts assume-role` works from the operator's machine.
    private static final String OPERATOR_SSO_ROLE_ARN =
            "arn:aws:iam::887764105431:role/aws-reserved/sso.amazonaws.com/eu-west-2/AWSReservedSSO_AdministratorAccess_8d2385a3b85cdf3b";

    @Value.Immutable
    public interface RootDnsStackProps extends StackProps {
        @Override
        Environment getEnv();

        String hostedZoneName();

        String hostedZoneId();

        /** CloudFront domain name for ci-gateway (e.g. d1234abcdef.cloudfront.net). Empty to skip. */
        @Value.Default
        default String ciGatewayCloudFrontDomain() {
            return "";
        }

        /** CloudFront domain name for prod-gateway. Empty to skip. */
        @Value.Default
        default String prodGatewayCloudFrontDomain() {
            return "";
        }

        /** CloudFront domain name for ci-spreadsheets (e.g. d5678efghij.cloudfront.net). Empty to skip. */
        @Value.Default
        default String ciSpreadsheetsCloudFrontDomain() {
            return "";
        }

        /** CloudFront domain name for prod-spreadsheets. Empty to skip. */
        @Value.Default
        default String prodSpreadsheetsCloudFrontDomain() {
            return "";
        }

        /** CloudFront domain name for apex (diyaccounting.co.uk). Empty to skip. */
        @Value.Default
        default String apexCloudFrontDomain() {
            return "";
        }

        /** CloudFront domain name for www.diyaccounting.co.uk. Empty to skip. */
        @Value.Default
        default String wwwCloudFrontDomain() {
            return "";
        }

        /** CloudFront domain name for spreadsheets.diyaccounting.co.uk. Empty to skip. */
        @Value.Default
        default String spreadsheetsCloudFrontDomain() {
            return "";
        }

        /** CloudFront domain name for ci-holding.spreadsheets.diyaccounting.co.uk. Empty to skip. */
        @Value.Default
        default String ciSpreadsheetsHoldingCloudFrontDomain() {
            return "";
        }

        /** CloudFront domain name for holding.spreadsheets.diyaccounting.co.uk. Empty to skip. */
        @Value.Default
        default String prodSpreadsheetsHoldingCloudFrontDomain() {
            return "";
        }

        /** Loopback IP for local.submit.diyaccounting.co.uk (developer machines). Empty to skip. */
        @Value.Default
        default String localSubmitTargetIp() {
            return "";
        }

        /** Service account IDs the cross-account delegate role trusts. Empty list to skip role creation. */
        @Value.Default
        default List<String> delegateAccountIds() {
            return List.of();
        }

        static ImmutableRootDnsStackProps.Builder builder() {
            return ImmutableRootDnsStackProps.builder();
        }
    }

    public RootDnsStack(final Construct scope, final String id, final RootDnsStackProps props) {
        super(scope, id, StackProps.builder().env(props.getEnv()).build());

        // Cost allocation tags
        Tags.of(this).add("Application", "@diy-accounting-uk/submit.diyaccounting.co.uk/root-dns");
        Tags.of(this).add("CostCenter", "@diy-accounting-uk/submit.diyaccounting.co.uk");
        Tags.of(this).add("Owner", "@diy-accounting-uk/submit.diyaccounting.co.uk");
        Tags.of(this).add("Stack", "RootDnsStack");
        Tags.of(this).add("ManagedBy", "aws-cdk");
        Tags.of(this).add("BillingPurpose", "dns-management");

        // Look up the hosted zone in the root account
        IHostedZone zone = HostedZone.fromHostedZoneAttributes(
                this,
                "RootZone",
                HostedZoneAttributes.builder()
                        .hostedZoneId(props.hostedZoneId())
                        .zoneName(props.hostedZoneName())
                        .build());

        // Phase 1: Gateway DNS records
        if (!props.ciGatewayCloudFrontDomain().isBlank()) {
            infof("Creating ci-gateway alias to %s", props.ciGatewayCloudFrontDomain());
            Route53AliasUpsert.upsertAliasToCloudFront(
                    this, "CiGateway", zone, "ci-gateway", props.ciGatewayCloudFrontDomain());
            cfnOutput(this, "CiGatewayDomain", "ci-gateway." + props.hostedZoneName());
        }

        if (!props.prodGatewayCloudFrontDomain().isBlank()) {
            infof("Creating prod-gateway alias to %s", props.prodGatewayCloudFrontDomain());
            Route53AliasUpsert.upsertAliasToCloudFront(
                    this, "ProdGateway", zone, "prod-gateway", props.prodGatewayCloudFrontDomain());
            cfnOutput(this, "ProdGatewayDomain", "prod-gateway." + props.hostedZoneName());
        }

        // Spreadsheets DNS records
        if (!props.ciSpreadsheetsCloudFrontDomain().isBlank()) {
            infof("Creating ci-spreadsheets alias to %s", props.ciSpreadsheetsCloudFrontDomain());
            Route53AliasUpsert.upsertAliasToCloudFront(
                    this, "CiSpreadsheets", zone, "ci-spreadsheets", props.ciSpreadsheetsCloudFrontDomain());
            cfnOutput(this, "CiSpreadsheetsDomain", "ci-spreadsheets." + props.hostedZoneName());
        }

        if (!props.prodSpreadsheetsCloudFrontDomain().isBlank()) {
            infof("Creating prod-spreadsheets alias to %s", props.prodSpreadsheetsCloudFrontDomain());
            Route53AliasUpsert.upsertAliasToCloudFront(
                    this, "ProdSpreadsheets", zone, "prod-spreadsheets", props.prodSpreadsheetsCloudFrontDomain());
            cfnOutput(this, "ProdSpreadsheetsDomain", "prod-spreadsheets." + props.hostedZoneName());
        }

        // Spreadsheets holding pages; the distributions live in the spreadsheets account,
        // which creates no Route53 records of its own.
        if (!props.ciSpreadsheetsHoldingCloudFrontDomain().isBlank()) {
            infof("Creating ci-holding.spreadsheets alias to %s", props.ciSpreadsheetsHoldingCloudFrontDomain());
            Route53AliasUpsert.upsertAliasToCloudFront(
                    this,
                    "CiSpreadsheetsHolding",
                    zone,
                    "ci-holding.spreadsheets",
                    props.ciSpreadsheetsHoldingCloudFrontDomain());
            cfnOutput(this, "CiSpreadsheetsHoldingDomain", "ci-holding.spreadsheets." + props.hostedZoneName());
        }

        if (!props.prodSpreadsheetsHoldingCloudFrontDomain().isBlank()) {
            infof("Creating holding.spreadsheets alias to %s", props.prodSpreadsheetsHoldingCloudFrontDomain());
            Route53AliasUpsert.upsertAliasToCloudFront(
                    this,
                    "ProdSpreadsheetsHolding",
                    zone,
                    "holding.spreadsheets",
                    props.prodSpreadsheetsHoldingCloudFrontDomain());
            cfnOutput(this, "ProdSpreadsheetsHoldingDomain", "holding.spreadsheets." + props.hostedZoneName());
        }

        // Phase 2: Production domain DNS records (go-live switchover)
        if (!props.apexCloudFrontDomain().isBlank()) {
            infof("Creating apex alias to %s", props.apexCloudFrontDomain());
            Route53AliasUpsert.upsertAliasToCloudFront(this, "Apex", zone, null, props.apexCloudFrontDomain());
            cfnOutput(this, "ApexDomain", props.hostedZoneName());
        }

        if (!props.wwwCloudFrontDomain().isBlank()) {
            infof("Creating www alias to %s", props.wwwCloudFrontDomain());
            Route53AliasUpsert.upsertAliasToCloudFront(this, "Www", zone, "www", props.wwwCloudFrontDomain());
            cfnOutput(this, "WwwDomain", "www." + props.hostedZoneName());
        }

        if (!props.spreadsheetsCloudFrontDomain().isBlank()) {
            infof("Creating spreadsheets alias to %s", props.spreadsheetsCloudFrontDomain());
            Route53AliasUpsert.upsertAliasToCloudFront(
                    this, "Spreadsheets", zone, "spreadsheets", props.spreadsheetsCloudFrontDomain());
            cfnOutput(this, "SpreadsheetsDomain", "spreadsheets." + props.hostedZoneName());
        }

        // Developer loopback record: every machine that resolves this name reaches its own
        // localhost, where the local server terminates TLS with a Let's Encrypt certificate.
        if (!props.localSubmitTargetIp().isBlank()) {
            infof("Creating local.submit A record to %s", props.localSubmitTargetIp());
            Route53AliasUpsert.upsertARecord(this, "LocalSubmit", zone, "local.submit", props.localSubmitTargetIp());
            cfnOutput(this, "LocalSubmitDomain", "local.submit." + props.hostedZoneName());
        }

        // IAM role for certbot's route53 DNS-01 plugin, run on the operator's machine to issue and
        // renew the local.submit certificate. Route53 cannot scope ChangeResourceRecordSets below
        // zone level, so this zone is the tightest grant available.
        var certbotRole = Role.Builder.create(this, "CertbotDns01Role")
                .roleName("root-certbot-dns01")
                .assumedBy(new ArnPrincipal(OPERATOR_SSO_ROLE_ARN))
                .description("Allows certbot's route53 DNS-01 plugin to issue the local.submit certificate")
                .build();
        certbotRole.addToPolicy(PolicyStatement.Builder.create()
                .actions(List.of("route53:ListHostedZones", "route53:GetChange"))
                .resources(List.of("*"))
                .build());
        certbotRole.addToPolicy(PolicyStatement.Builder.create()
                .actions(List.of("route53:ChangeResourceRecordSets"))
                .resources(List.of("arn:aws:route53:::hostedzone/" + props.hostedZoneId()))
                .build());
        cfnOutput(this, "CertbotDns01RoleArn", certbotRole.getRoleArn());

        // Cross-account IAM role for Route53 record management, reached by sts:AssumeRole from each
        // service account's own deployment role, so no GitHub OIDC trust into this account is needed.
        if (!props.delegateAccountIds().isEmpty()) {
            var principals = props.delegateAccountIds().stream()
                    .map(AccountPrincipal::new)
                    .toArray(IPrincipal[]::new);
            var delegateRole = Role.Builder.create(this, "Route53DelegateRole")
                    .roleName("root-route53-record-delegate")
                    .assumedBy(new CompositePrincipal(principals))
                    .description("Allows service accounts to create Route53 records in the root hosted zone")
                    .build();
            delegateRole.addToPolicy(PolicyStatement.Builder.create()
                    .actions(List.of("route53:ChangeResourceRecordSets", "route53:GetHostedZone"))
                    .resources(List.of("arn:aws:route53:::hostedzone/" + props.hostedZoneId()))
                    .build());
            cfnOutput(this, "Route53DelegateRoleArn", delegateRole.getRoleArn());
            infof("Created Route53 delegate role for accounts: %s", String.join(", ", props.delegateAccountIds()));
        }

        infof("RootDnsStack %s created", this.getNode().getId());
    }
}
