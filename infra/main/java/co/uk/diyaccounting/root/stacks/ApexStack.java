/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright (C) 2025-2026 DIY Accounting Ltd
 */

package co.uk.diyaccounting.root.stacks;

import static co.uk.diyaccounting.root.utils.Kind.infof;
import static co.uk.diyaccounting.root.utils.KindCdk.cfnOutput;
import static co.uk.diyaccounting.root.utils.KindCdk.ensureLogGroupWithDependency;

import co.uk.diyaccounting.root.SubmitSharedNames;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;
import software.amazon.awscdk.ArnComponents;
import software.amazon.awscdk.AssetHashType;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Expiration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Size;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Tags;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.cloudfront.AllowedMethods;
import software.amazon.awscdk.services.cloudfront.BehaviorOptions;
import software.amazon.awscdk.services.cloudfront.CfnDistribution;
import software.amazon.awscdk.services.cloudfront.Distribution;
import software.amazon.awscdk.services.cloudfront.HeadersFrameOption;
import software.amazon.awscdk.services.cloudfront.IOrigin;
import software.amazon.awscdk.services.cloudfront.OriginRequestPolicy;
import software.amazon.awscdk.services.cloudfront.ResponseCustomHeader;
import software.amazon.awscdk.services.cloudfront.ResponseCustomHeadersBehavior;
import software.amazon.awscdk.services.cloudfront.ResponseHeadersContentSecurityPolicy;
import software.amazon.awscdk.services.cloudfront.ResponseHeadersContentTypeOptions;
import software.amazon.awscdk.services.cloudfront.ResponseHeadersCorsBehavior;
import software.amazon.awscdk.services.cloudfront.ResponseHeadersFrameOptions;
import software.amazon.awscdk.services.cloudfront.ResponseHeadersPolicy;
import software.amazon.awscdk.services.cloudfront.ResponseHeadersReferrerPolicy;
import software.amazon.awscdk.services.cloudfront.ResponseHeadersStrictTransportSecurity;
import software.amazon.awscdk.services.cloudfront.ResponseHeadersXSSProtection;
import software.amazon.awscdk.services.cloudfront.ResponseSecurityHeadersBehavior;
import software.amazon.awscdk.services.cloudfront.S3OriginAccessControl;
import software.amazon.awscdk.services.cloudfront.SSLMethod;
import software.amazon.awscdk.services.cloudfront.Signing;
import software.amazon.awscdk.services.cloudfront.ViewerProtocolPolicy;
import software.amazon.awscdk.services.cloudfront.origins.S3BucketOrigin;
import software.amazon.awscdk.services.cloudfront.origins.S3BucketOriginWithOACProps;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.FunctionUrlAuthType;
import software.amazon.awscdk.services.lambda.Permission;
import software.amazon.awscdk.services.logs.CfnDelivery;
import software.amazon.awscdk.services.logs.CfnDeliveryDestination;
import software.amazon.awscdk.services.logs.CfnDeliveryDestinationProps;
import software.amazon.awscdk.services.logs.CfnDeliveryProps;
import software.amazon.awscdk.services.logs.CfnDeliverySource;
import software.amazon.awscdk.services.logs.CfnDeliverySourceProps;
import software.amazon.awscdk.services.logs.ILogGroup;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneAttributes;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.Source;
import software.constructs.Construct;

public class ApexStack extends Stack {

    public Bucket holdingBucket;
    public final Distribution distribution;
    public final Permission distributionInvokeFnUrl;
    public final String aliasRecordDomainName;
    public final String aliasRecordV6DomainName;
    public final BucketDeployment webDeployment;

    @Value.Immutable
    public interface ApexStackProps extends StackProps, SubmitStackProps {
        @Override
        Environment getEnv();

        @Override
        @Value.Default
        default Boolean getCrossRegionReferences() {
            return null;
        }

        @Override
        String envName();

        @Override
        String deploymentName();

        @Override
        String resourceNamePrefix();

        @Override
        String cloudTrailEnabled();

        @Override
        SubmitSharedNames sharedNames();

        String hostedZoneName();

        String hostedZoneId();

        String certificateArn();

        String holdingDocRootPath();

        /** Logging TTL in days */
        int accessLogGroupRetentionPeriodDays();

        static ImmutableApexStackProps.Builder builder() {
            return ImmutableApexStackProps.builder();
        }
    }

    public ApexStack(final Construct scope, final String id, final ApexStackProps props) {
        this(scope, id, null, props);
    }

    public ApexStack(final Construct scope, final String id, final StackProps stackProps, final ApexStackProps props) {
        super(
                scope,
                id,
                StackProps.builder()
                        .env(props.getEnv()) // enforce region from props
                        .description(stackProps != null ? stackProps.getDescription() : null)
                        .stackName(stackProps != null ? stackProps.getStackName() : null)
                        .terminationProtection(stackProps != null ? stackProps.getTerminationProtection() : null)
                        .analyticsReporting(stackProps != null ? stackProps.getAnalyticsReporting() : null)
                        .synthesizer(stackProps != null ? stackProps.getSynthesizer() : null)
                        .crossRegionReferences(stackProps != null ? stackProps.getCrossRegionReferences() : null)
                        .build());

        // Apply cost allocation tags for all resources in this stack
        Tags.of(this).add("Environment", props.envName());
        Tags.of(this).add("Application", "@support-at-diyaccounting/submit.diyaccounting.co.uk/cdk.json");
        Tags.of(this).add("CostCenter", "@support-at-diyaccounting/submit.diyaccounting.co.uk");
        Tags.of(this).add("Owner", "@support-at-diyaccounting/submit.diyaccounting.co.uk");
        Tags.of(this).add("Project", "@support-at-diyaccounting/submit.diyaccounting.co.uk");
        Tags.of(this).add("DeploymentName", props.deploymentName());
        Tags.of(this).add("Stack", "EdgeStack");
        Tags.of(this).add("ManagedBy", "aws-cdk");

        // Enhanced cost optimization tags
        Tags.of(this).add("BillingPurpose", "authentication-infrastructure");
        Tags.of(this).add("ResourceType", "serverless-web-app");
        Tags.of(this).add("Criticality", "low");
        Tags.of(this).add("DataClassification", "public");
        Tags.of(this).add("BackupRequired", "false");
        Tags.of(this).add("MonitoringEnabled", "true");

        // Hosted zone (must exist)
        IHostedZone zone = HostedZone.fromHostedZoneAttributes(
                this,
                props.resourceNamePrefix() + "-Zone",
                HostedZoneAttributes.builder()
                        .hostedZoneId(props.hostedZoneId())
                        .zoneName(props.hostedZoneName())
                        .build());
        String recordName = props.hostedZoneName().equals(props.sharedNames().holdingDomainName)
                ? null
                : (props.sharedNames().holdingDomainName.endsWith("." + props.hostedZoneName())
                        ? props.sharedNames()
                                .holdingDomainName
                                .substring(
                                        0,
                                        props.sharedNames().holdingDomainName.length()
                                                - (props.hostedZoneName().length() + 1))
                        : props.sharedNames().holdingDomainName);

        // TLS certificate from existing ACM (must be in us-east-1 for CloudFront)
        var cert =
                Certificate.fromCertificateArn(this, props.resourceNamePrefix() + "-WebCert", props.certificateArn());

        // Create the origin bucket — no explicit bucketName so each account gets a unique name
        // (S3 bucket names are globally unique; hardcoding causes collisions during account migration)
        this.holdingBucket = Bucket.Builder.create(this, props.resourceNamePrefix() + "-OriginBucket")
                .versioned(false)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .encryption(BucketEncryption.S3_MANAGED)
                .removalPolicy(RemovalPolicy.DESTROY)
                .autoDeleteObjects(true)
                .build();
        infof("Created origin bucket %s", this.holdingBucket.getNode().getId());

        this.holdingBucket.addToResourcePolicy(PolicyStatement.Builder.create()
                .sid("AllowCloudFrontReadViaOAC")
                .principals(List.of(new ServicePrincipal("cloudfront.amazonaws.com")))
                .actions(List.of("s3:GetObject"))
                .resources(List.of(this.holdingBucket.getBucketArn() + "/*"))
                .conditions(Map.of(
                        // Limit to distributions in your account (no distribution ARN token needed)
                        "StringEquals",
                        Map.of("AWS:SourceAccount", this.getAccount()),
                        "ArnLike",
                        Map.of("AWS:SourceArn", "arn:aws:cloudfront::" + this.getAccount() + ":distribution/*")))
                .build());

        S3OriginAccessControl oac = S3OriginAccessControl.Builder.create(this, "MyOAC")
                .signing(Signing.SIGV4_ALWAYS) // NEVER // SIGV4_NO_OVERRIDE
                .build();
        IOrigin localOrigin = S3BucketOrigin.withOriginAccessControl(
                this.holdingBucket,
                S3BucketOriginWithOACProps.builder().originAccessControl(oac).build());
        infof("Created BucketOrigin with bucket: %s", this.holdingBucket.getBucketName());

        // Define a custom Response Headers Policy with CSP that allows AWS RUM client + dataplane
        ResponseHeadersPolicy webResponseHeadersPolicy = ResponseHeadersPolicy.Builder.create(
                        this, props.resourceNamePrefix() + "-ApexWebHeadersPolicy")
                .responseHeadersPolicyName(props.resourceNamePrefix() + "-apex-whp")
                .comment("CORS + security headers with CSP allowing CloudWatch RUM client & dataplane")
                .corsBehavior(ResponseHeadersCorsBehavior.builder()
                        .accessControlAllowCredentials(false)
                        .accessControlAllowHeaders(List.of("*"))
                        .accessControlAllowMethods(List.of("GET", "HEAD", "OPTIONS"))
                        .accessControlAllowOrigins(List.of("*"))
                        .accessControlExposeHeaders(List.of())
                        .accessControlMaxAge(Duration.seconds(600))
                        .originOverride(true)
                        .build())
                .securityHeadersBehavior(ResponseSecurityHeadersBehavior.builder()
                        .contentSecurityPolicy(ResponseHeadersContentSecurityPolicy.builder()
                                .contentSecurityPolicy("default-src 'self'; "
                                        + "script-src 'self' 'unsafe-inline' https://client.rum.us-east-1.amazonaws.com https://unpkg.com https://www.googletagmanager.com; "
                                        + "connect-src 'self' https://dataplane.rum.eu-west-2.amazonaws.com https://api.ipify.org https://ipapi.co https://httpbin.org https://*.google-analytics.com https://www.googletagmanager.com; "
                                        + "img-src 'self' data: https://avatars.githubusercontent.com https://github.com https://www.google-analytics.com https://www.googletagmanager.com; "
                                        + "style-src 'self' 'unsafe-inline' https://unpkg.com; "
                                        + "frame-ancestors 'none'; "
                                        + "form-action 'self';")
                                .override(true)
                                .build())
                        .strictTransportSecurity(ResponseHeadersStrictTransportSecurity.builder()
                                .accessControlMaxAge(Duration.days(365))
                                .includeSubdomains(true)
                                .override(true)
                                .build())
                        .contentTypeOptions(ResponseHeadersContentTypeOptions.builder()
                                .override(true)
                                .build())
                        .frameOptions(ResponseHeadersFrameOptions.builder()
                                .frameOption(HeadersFrameOption.DENY)
                                .override(true)
                                .build())
                        .referrerPolicy(ResponseHeadersReferrerPolicy.builder()
                                .referrerPolicy(
                                        software.amazon.awscdk.services.cloudfront.HeadersReferrerPolicy
                                                .STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                                .override(true)
                                .build())
                        .xssProtection(ResponseHeadersXSSProtection.builder()
                                .protection(true)
                                .modeBlock(true)
                                .override(true)
                                .build())
                        .build())
                .customHeadersBehavior(ResponseCustomHeadersBehavior.builder()
                        .customHeaders(List.of(
                                ResponseCustomHeader.builder()
                                        .header("Cross-Origin-Opener-Policy")
                                        .value("same-origin")
                                        .override(true)
                                        .build(),
                                ResponseCustomHeader.builder()
                                        .header("Cross-Origin-Embedder-Policy")
                                        .value("require-corp")
                                        .override(true)
                                        .build(),
                                ResponseCustomHeader.builder()
                                        .header("Cross-Origin-Resource-Policy")
                                        .value("same-origin")
                                        .override(true)
                                        .build(),
                                ResponseCustomHeader.builder()
                                        .header("Server")
                                        .value("DIY-Accounting")
                                        .override(true)
                                        .build()))
                        .build())
                .build();

        BehaviorOptions localBehaviorOptions = BehaviorOptions.builder()
                .origin(localOrigin)
                .allowedMethods(AllowedMethods.ALLOW_GET_HEAD_OPTIONS)
                .originRequestPolicy(OriginRequestPolicy.CORS_S3_ORIGIN)
                .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                .responseHeadersPolicy(webResponseHeadersPolicy)
                .compress(true)
                .build();

        // Ensure distribution access log group exists (idempotent creation)
        ILogGroup distributionAccessLogGroup = ensureLogGroupWithDependency(
                        this,
                        props.resourceNamePrefix() + "-DistributionAccessLogGroup",
                        props.sharedNames().distributionAccessLogGroupName)
                .logGroup();

        // CloudFront distribution for the web origin and all the URL Lambdas.
        this.distribution = Distribution.Builder.create(this, props.resourceNamePrefix() + "-ApexWebDist")
                .defaultBehavior(localBehaviorOptions)
                .domainNames(List.of(props.sharedNames().holdingDomainName))
                .certificate(cert)
                .defaultRootObject("index.html")
                .enableLogging(false) // legacy S3 logging off
                .enableIpv6(true)
                .sslSupportMethod(SSLMethod.SNI)
                .build();
        Tags.of(this.distribution).add("OriginFor", props.sharedNames().holdingDomainName);

        // Configure CloudFront standard access logging to CloudWatch Logs (pending CDK high-level support).
        CfnDistribution cfnDist = (CfnDistribution) this.distribution.getNode().getDefaultChild();
        assert cfnDist != null;

        // 2. Compute the CloudFront distribution ARN for the delivery source
        String distributionArn = Stack.of(this)
                .formatArn(ArnComponents.builder()
                        .service("cloudfront")
                        .region("") // CloudFront is global
                        .resource("distribution")
                        .resourceName(this.distribution.getDistributionId())
                        .build());

        // 3. CloudWatch Logs destination that points at your log group
        CfnDeliveryDestination cfLogsDestination = new CfnDeliveryDestination(
                this,
                props.resourceNamePrefix() + "-CfAccessLogsDestination",
                CfnDeliveryDestinationProps.builder()
                        // Name is arbitrary; keep it stable but does not need to be the log group name
                        .name(props.sharedNames().distributionAccessLogDeliveryHoldingDestinationName)
                        .destinationResourceArn(distributionAccessLogGroup.getLogGroupArn())
                        .outputFormat("json") // or "w3c"/"parquet" if you prefer
                        .build());

        // 4. Delivery source that represents the CloudFront distribution
        CfnDeliverySource cfLogsSource = new CfnDeliverySource(
                this,
                props.resourceNamePrefix() + "-CfAccessLogsSource",
                CfnDeliverySourceProps.builder()
                        .name(props.sharedNames()
                                .distributionAccessLogDeliveryHoldingSourceName) // <-- use the shared variable
                        .logType("ACCESS_LOGS") // required for CloudFront
                        .resourceArn(distributionArn) // ARN of the distribution
                        .build());

        // 5. Delivery that connects source to destination
        CfnDelivery cfLogsDelivery = new CfnDelivery(
                this,
                props.resourceNamePrefix() + "-CfAccessLogsDelivery",
                CfnDeliveryProps.builder()
                        // *** IMPORTANT: must exactly match the Name above ***
                        .deliverySourceName(props.sharedNames().distributionAccessLogDeliveryHoldingSourceName)
                        .deliveryDestinationArn(cfLogsDestination.getAttrArn())
                        // optional: customise fields and delimiter
                        // .fieldDelimiter("\t")
                        // .recordFields(List.of("date", "time", "x-edge-location", "c-ip",
                        //                       "cs-method", "cs-host", "cs-uri-stem", "sc-status"))
                        .build());

        // *** CRITICAL: enforce creation order so source exists before delivery ***
        cfLogsDelivery.addDependency(cfLogsSource);

        // Grant CloudFront access to the origin lambdas
        this.distributionInvokeFnUrl = Permission.builder()
                .principal(new ServicePrincipal("cloudfront.amazonaws.com"))
                .action("lambda:InvokeFunctionUrl")
                .functionUrlAuthType(FunctionUrlAuthType.NONE)
                .sourceArn(this.distribution.getDistributionArn())
                .build();

        // Idempotent UPSERT of Route53 A/AAAA alias to CloudFront (replaces deprecated deleteExisting)
        co.uk.diyaccounting.root.utils.Route53AliasUpsert.upsertAliasToCloudFront(
                this, props.resourceNamePrefix() + "-AliasRecord", zone, recordName, this.distribution.getDomainName());
        this.aliasRecordDomainName = (recordName == null || recordName.isBlank())
                ? zone.getZoneName()
                : (recordName + "." + zone.getZoneName());
        this.aliasRecordV6DomainName = this.aliasRecordDomainName;

        // Deploy the web website files to the web website bucket and invalidate distribution
        // Resolve the document root path from props to avoid path mismatches between generation and deployment
        var publicDir = Paths.get(props.holdingDocRootPath()).toAbsolutePath().normalize();
        infof("Using public doc root: %s".formatted(publicDir));
        var webDocRootSource = Source.asset(
                publicDir.toString(),
                AssetOptions.builder().assetHashType(AssetHashType.SOURCE).build());
        this.webDeployment = BucketDeployment.Builder.create(
                        this, props.resourceNamePrefix() + "-DocRootToWebOriginDeployment")
                .sources(List.of(webDocRootSource))
                .destinationBucket(this.holdingBucket)
                .distribution(distribution)
                .distributionPaths(List.of("/index.html"))
                .retainOnDelete(true)
                .expires(Expiration.after(Duration.minutes(5)))
                .prune(false)
                .memoryLimit(1024)
                .ephemeralStorageSize(Size.gibibytes(2))
                .build();

        // Outputs
        cfnOutput(this, "BaseUrl", props.sharedNames().baseUrl);
        cfnOutput(this, "CertificateArn", cert.getCertificateArn());
        cfnOutput(this, "ApexWebDistributionDomainName", this.distribution.getDomainName());
        cfnOutput(this, "DistributionId", this.distribution.getDistributionId());
        cfnOutput(this, "AliasRecord", this.aliasRecordDomainName);
        cfnOutput(this, "AliasRecordV6", this.aliasRecordV6DomainName);

        infof("ApexStack %s created successfully for %s", this.getNode().getId(), props.sharedNames().baseUrl);
    }
}
