/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright (C) 2025-2026 DIY Accounting Ltd
 */

package co.uk.diyaccounting.root.stacks;

import static co.uk.diyaccounting.root.utils.Kind.infof;
import static co.uk.diyaccounting.root.utils.KindCdk.cfnOutput;

import java.util.List;
import java.util.Map;
import org.immutables.value.Value;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Tags;
import software.amazon.awscdk.services.athena.CfnWorkGroup;
import software.amazon.awscdk.services.glue.CfnDatabase;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;
import software.amazon.awscdk.services.s3.LifecycleRule;
import software.amazon.awscdk.services.s3.StorageClass;
import software.amazon.awscdk.services.s3.Transition;
import software.constructs.Construct;

/**
 * Org-wide cost reporting storage: the CUR 2.0 Data Export destination bucket, its Glue
 * catalogue and the Athena workgroup that queries it. Lives in the management account,
 * eu-west-2, alongside the rest of the estate.
 *
 * <p>The export, budgets, anomaly monitor and cost category that reference this bucket live in
 * {@link CostReportingUE1Stack} instead of here: BCM Data Exports, Budgets and Cost Explorer are
 * only reachable from us-east-1, and a CloudFormation stack's declared region is where its native
 * resource types run, not just an SDK client setting. The bucket name is a plain string shared
 * between both stacks rather than a cross-stack reference, so the two stacks never need
 * cross-region references turned on.
 */
public class CostReportingStack extends Stack {

    public final Bucket exportBucket;
    public final CfnDatabase glueDatabase;
    public final CfnWorkGroup athenaWorkGroup;

    @Value.Immutable
    public interface CostReportingStackProps extends StackProps {
        @Override
        Environment getEnv();

        String bucketName();

        String glueDatabaseName();

        String athenaWorkGroupName();

        static ImmutableCostReportingStackProps.Builder builder() {
            return ImmutableCostReportingStackProps.builder();
        }
    }

    public CostReportingStack(final Construct scope, final String id, final CostReportingStackProps props) {
        super(scope, id, StackProps.builder().env(props.getEnv()).build());

        Tags.of(this).add("Application", "@diy-accounting-uk/root.diyaccounting.co.uk/cost-reporting");
        Tags.of(this).add("CostCenter", "@diy-accounting-uk/root.diyaccounting.co.uk");
        Tags.of(this).add("Owner", "@diy-accounting-uk/root.diyaccounting.co.uk");
        Tags.of(this).add("Stack", "CostReportingStack");
        Tags.of(this).add("ManagedBy", "aws-cdk");
        Tags.of(this).add("BillingPurpose", "cost-instrumentation");

        this.exportBucket = Bucket.Builder.create(this, "ExportBucket")
                .bucketName(props.bucketName())
                .encryption(BucketEncryption.S3_MANAGED)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .removalPolicy(RemovalPolicy.DESTROY)
                .autoDeleteObjects(true)
                .lifecycleRules(List.of(LifecycleRule.builder()
                        .id("ArchiveAfter180Days")
                        .transitions(List.of(Transition.builder()
                                .storageClass(StorageClass.GLACIER_INSTANT_RETRIEVAL)
                                .transitionAfter(Duration.days(180))
                                .build()))
                        .build()))
                .build();
        infof("Created cost export bucket %s", props.bucketName());

        // Two statements because GetBucketPolicy targets the bucket itself while PutObject
        // targets objects within it; one statement can't carry both resource shapes.
        this.exportBucket.addToResourcePolicy(PolicyStatement.Builder.create()
                .sid("AllowBillingExportsPutObject")
                .principals(List.of(
                        new ServicePrincipal("billingreports.amazonaws.com"),
                        new ServicePrincipal("bcm-data-exports.amazonaws.com")))
                .actions(List.of("s3:PutObject"))
                .resources(List.of(this.exportBucket.getBucketArn() + "/*"))
                .conditions(Map.of("StringEquals", Map.of("aws:SourceAccount", this.getAccount())))
                .build());
        this.exportBucket.addToResourcePolicy(PolicyStatement.Builder.create()
                .sid("AllowBillingExportsGetBucketPolicy")
                .principals(List.of(
                        new ServicePrincipal("billingreports.amazonaws.com"),
                        new ServicePrincipal("bcm-data-exports.amazonaws.com")))
                .actions(List.of("s3:GetBucketPolicy"))
                .resources(List.of(this.exportBucket.getBucketArn()))
                .conditions(Map.of("StringEquals", Map.of("aws:SourceAccount", this.getAccount())))
                .build());

        this.glueDatabase = CfnDatabase.Builder.create(this, "GlueDatabase")
                .catalogId(this.getAccount())
                .databaseInput(CfnDatabase.DatabaseInputProperty.builder()
                        .name(props.glueDatabaseName())
                        .description("Org-wide CUR 2.0 cost and usage data")
                        .build())
                .build();

        // recursiveDeleteOption matters for teardown: deleting a workgroup that still holds
        // saved queries fails without it.
        this.athenaWorkGroup = CfnWorkGroup.Builder.create(this, "AthenaWorkGroup")
                .name(props.athenaWorkGroupName())
                .description("Athena workgroup for the org-wide cost and usage export")
                .state("ENABLED")
                .recursiveDeleteOption(true)
                .workGroupConfiguration(CfnWorkGroup.WorkGroupConfigurationProperty.builder()
                        .enforceWorkGroupConfiguration(true)
                        .publishCloudWatchMetricsEnabled(true)
                        .resultConfiguration(CfnWorkGroup.ResultConfigurationProperty.builder()
                                .outputLocation("s3://" + props.bucketName() + "/athena-results/")
                                .encryptionConfiguration(CfnWorkGroup.EncryptionConfigurationProperty.builder()
                                        .encryptionOption("SSE_S3")
                                        .build())
                                .build())
                        .build())
                .build();
        this.athenaWorkGroup.getNode().addDependency(this.exportBucket);

        cfnOutput(this, "ExportBucketName", this.exportBucket.getBucketName());
        cfnOutput(this, "GlueDatabaseName", props.glueDatabaseName());
        cfnOutput(this, "AthenaWorkGroupName", props.athenaWorkGroupName());

        infof("CostReportingStack %s created", this.getNode().getId());
    }
}
