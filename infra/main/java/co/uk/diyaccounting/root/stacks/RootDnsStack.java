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
 */
public class RootDnsStack extends Stack {

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

        /** Account IDs to trust for cross-account Route53 record management. Empty list to skip role creation. */
        @Value.Default
        default List<String> route53DelegateAccountIds() {
            return List.of();
        }

        static ImmutableRootDnsStackProps.Builder builder() {
            return ImmutableRootDnsStackProps.builder();
        }
    }

    public RootDnsStack(final Construct scope, final String id, final RootDnsStackProps props) {
        super(scope, id, StackProps.builder().env(props.getEnv()).build());

        // Cost allocation tags
        Tags.of(this).add("Application", "@support-at-diyaccounting/submit.diyaccounting.co.uk/root-dns");
        Tags.of(this).add("CostCenter", "@support-at-diyaccounting/submit.diyaccounting.co.uk");
        Tags.of(this).add("Owner", "@support-at-diyaccounting/submit.diyaccounting.co.uk");
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

        // Cross-account IAM role for Route53 record management
        // Allows submit stacks in other accounts to create DNS records in this hosted zone
        if (!props.route53DelegateAccountIds().isEmpty()) {
            var principals = props.route53DelegateAccountIds().stream()
                    .map(AccountPrincipal::new)
                    .toArray(IPrincipal[]::new);
            var delegateRole = Role.Builder.create(this, "Route53DelegateRole")
                    .roleName("root-route53-record-delegate")
                    .assumedBy(new CompositePrincipal(principals))
                    .description("Allows submit accounts to create Route53 records in the root hosted zone")
                    .build();
            delegateRole.addToPolicy(PolicyStatement.Builder.create()
                    .actions(List.of("route53:ChangeResourceRecordSets", "route53:GetHostedZone"))
                    .resources(List.of("arn:aws:route53:::hostedzone/" + props.hostedZoneId()))
                    .build());
            cfnOutput(this, "Route53DelegateRoleArn", delegateRole.getRoleArn());
            infof(
                    "Created Route53 delegate role for accounts: %s",
                    String.join(", ", props.route53DelegateAccountIds()));
        }

        infof("RootDnsStack %s created", this.getNode().getId());
    }
}
