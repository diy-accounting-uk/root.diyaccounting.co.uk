/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright (C) 2025-2026 DIY Accounting Ltd
 */

package co.uk.diyaccounting.root.stacks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.assertions.Match;
import software.amazon.awscdk.assertions.Template;

class CostReportingStackTest {

    private static CostReportingStack synthCostReportingStack() {
        App app = new App();
        return new CostReportingStack(
                app,
                "TestCostReportingStack",
                CostReportingStack.CostReportingStackProps.builder()
                        .env(Environment.builder()
                                .account("887764105431")
                                .region("eu-west-2")
                                .build())
                        .bucketName("diy-accounting-cost-reports-887764105431")
                        .glueDatabaseName("cost_and_usage")
                        .athenaWorkGroupName("diy-accounting-cost-reports")
                        .build());
    }

    @Test
    void bucketBlocksPublicAccessAndArchivesAfter180Days() {
        Template template = Template.fromStack(synthCostReportingStack());

        var transitionToGlacierIr = Match.objectLike(Map.of("StorageClass", "GLACIER_IR", "TransitionInDays", 180));
        var lifecycleRule = Match.objectLike(Map.of("Transitions", Match.arrayWith(List.of(transitionToGlacierIr))));
        var publicAccessBlock = Match.objectLike(Map.of(
                "BlockPublicAcls", true,
                "BlockPublicPolicy", true,
                "IgnorePublicAcls", true,
                "RestrictPublicBuckets", true));

        template.hasResourceProperties(
                "AWS::S3::Bucket",
                Match.objectLike(Map.of(
                        "BucketName",
                        "diy-accounting-cost-reports-887764105431",
                        "PublicAccessBlockConfiguration",
                        publicAccessBlock,
                        "LifecycleConfiguration",
                        Match.objectLike(Map.of("Rules", Match.arrayWith(List.of(lifecycleRule)))))));
    }

    @Test
    void bucketPolicyGrantsBillingExportsPutObjectAndGetBucketPolicy() {
        Template template = Template.fromStack(synthCostReportingStack());

        assertEquals(1, template.findResources("AWS::S3::BucketPolicy").size(), "expected exactly one bucket policy");

        var sourceAccountCondition =
                Match.objectLike(Map.of("StringEquals", Match.objectLike(Map.of("aws:SourceAccount", "887764105431"))));
        var putObjectStatement =
                Match.objectLike(Map.of("Action", "s3:PutObject", "Condition", sourceAccountCondition));
        var getBucketPolicyStatement = Match.objectLike(Map.of("Action", "s3:GetBucketPolicy"));
        var policyDocument = Match.objectLike(
                Map.of("Statement", Match.arrayWith(List.of(putObjectStatement, getBucketPolicyStatement))));

        template.hasResourceProperties(
                "AWS::S3::BucketPolicy", Match.objectLike(Map.of("PolicyDocument", policyDocument)));
    }

    @Test
    void glueDatabaseAndAthenaWorkGroupAreCreated() {
        Template template = Template.fromStack(synthCostReportingStack());

        template.hasResourceProperties(
                "AWS::Glue::Database",
                Match.objectLike(Map.of("DatabaseInput", Match.objectLike(Map.of("Name", "cost_and_usage")))));

        template.hasResourceProperties(
                "AWS::Athena::WorkGroup",
                Match.objectLike(Map.of(
                        "Name",
                        "diy-accounting-cost-reports",
                        "WorkGroupConfiguration",
                        Match.objectLike(Map.of(
                                "ResultConfiguration",
                                Match.objectLike(Map.of(
                                        "OutputLocation",
                                        "s3://diy-accounting-cost-reports-887764105431/athena-results/")))))));
    }
}
