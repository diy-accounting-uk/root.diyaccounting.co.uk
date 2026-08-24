/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright (C) 2025-2026 DIY Accounting Ltd
 */

package co.uk.diyaccounting.root;

import static co.uk.diyaccounting.root.utils.ResourceNameUtils.buildDashedDomainName;
import static co.uk.diyaccounting.root.utils.ResourceNameUtils.convertDotSeparatedToDashSeparated;

import co.uk.diyaccounting.root.utils.ResourceNameUtils;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;

public class SubmitSharedNames {

    public static class PublishedLambda {
        public final HttpMethod method;
        public final String urlPath;
        public final String summary;
        public final String description;
        public final String operationId;
        public final List<ApiParameter> parameters;

        public PublishedLambda(
                HttpMethod method, String urlPath, String summary, String description, String operationId) {
            this(method, urlPath, summary, description, operationId, List.of());
        }

        public PublishedLambda(
                HttpMethod method,
                String urlPath,
                String summary,
                String description,
                String operationId,
                List<ApiParameter> parameters) {
            this.method = method;
            this.urlPath = urlPath;
            this.summary = summary;
            this.description = description;
            this.operationId = operationId;
            this.parameters = parameters != null ? parameters : List.of();
        }
    }

    public static class ApiParameter {
        public final String name;
        public final String in;
        public final boolean required;
        public final String description;

        public ApiParameter(String name, String in, boolean required, String description) {
            this.name = name;
            this.in = in;
            this.required = required;
            this.description = description;
        }
    }

    public final List<PublishedLambda> publishedApiLambdas = new ArrayList<>();

    public String hostedZoneName;
    public String deploymentDomainName;
    public String envDomainName;
    public String publicDomainName;
    public String cognitoDomainName;
    public String holdingDomainName;
    public String simulatorDomainName;
    public String baseUrl;
    public String envBaseUrl;
    public String publicBaseUrl;
    public String dashedDeploymentDomainName;
    public String cognitoBaseUri;
    public String trailName;
    public String provisionedConcurrencyAliasName;

    public String receiptsTableName;
    public String bundlesTableName;
    // TODO: Move async table names to LambdaNames
    public String bundlePostAsyncRequestsTableName;
    public String bundleDeleteAsyncRequestsTableName;
    public String hmrcVatReturnPostAsyncRequestsTableName;
    public String hmrcVatReturnGetAsyncRequestsTableName;
    public String hmrcVatObligationGetAsyncRequestsTableName;
    public String hmrcApiRequestsTableName;
    public String passesTableName;
    public String bundleCapacityTableName;
    public String activityBusName;
    public String subscriptionsTableName;
    public String holdingBucketName;
    public String originBucketName;
    public String originAccessLogBucketName;
    public String distributionAccessLogGroupName;
    public String distributionAccessLogDeliveryHoldingSourceName;
    public String distributionAccessLogDeliveryOriginSourceName;
    public String distributionAccessLogDeliveryHoldingDestinationName;
    public String distributionAccessLogDeliveryOriginDestinationName;
    public String ew2SelfDestructLogGroupName;
    public String ue1SelfDestructLogGroupName;
    public String apiAccessLogGroupName;

    public String envResourceNamePrefix;
    public String observabilityStackId;
    public String observabilityUE1StackId;
    public String dataStackId;
    public String identityStackId;
    public String apexStackId;
    public String backupStackId;
    public String activityStackId;
    public String simulatorStackId;
    public String ecrStackId;
    public String ue1EcrStackId;
    public String ecrRepositoryArn;
    public String ecrRepositoryName;
    public String ecrLogGroupName;
    public String ecrPublishRoleName;
    public String ue1EcrRepositoryArn;
    public String ue1EcrRepositoryName;
    public String ue1EcrLogGroupName;
    public String ue1EcrPublishRoleName;

    public String appResourceNamePrefix;
    public String authStackId;
    public String hmrcStackId;
    public String accountStackId;
    public String apiStackId;
    public String opsStackId;
    public String selfDestructStackId;

    public String cognitoTokenPostIngestLambdaHandler;
    public String cognitoTokenPostIngestLambdaFunctionName;
    public String cognitoTokenPostIngestLambdaArn;
    public String cognitoTokenPostIngestProvisionedConcurrencyLambdaAliasArn;
    public HttpMethod cognitoTokenPostLambdaHttpMethod;
    public String cognitoTokenPostLambdaUrlPath;
    public boolean cognitoTokenPostLambdaJwtAuthorizer;
    public boolean cognitoTokenPostLambdaCustomAuthorizer;

    public String customAuthorizerIngestLambdaHandler;
    public String customAuthorizerIngestLambdaFunctionName;
    public String customAuthorizerIngestLambdaArn;
    public String customAuthorizerIngestProvisionedConcurrencyLambdaAliasArn;

    public String bundleGetIngestLambdaHandler;
    public String bundleGetIngestLambdaFunctionName;
    public String bundleGetIngestLambdaArn;
    public String bundleGetIngestProvisionedConcurrencyLambdaAliasArn;
    public HttpMethod bundleGetLambdaHttpMethod;
    public String bundleGetLambdaUrlPath;
    public boolean bundleGetLambdaJwtAuthorizer;
    public boolean bundleGetLambdaCustomAuthorizer;

    // TODO: Replace individual attributes with LambdaNames instances
    public LambdaNames bundlePost;
    public String bundlePostIngestLambdaHandler;
    public String bundlePostIngestLambdaFunctionName;
    public String bundlePostIngestLambdaArn;
    public String bundlePostIngestProvisionedConcurrencyLambdaAliasArn;
    public String bundlePostWorkerLambdaHandler;
    public String bundlePostWorkerLambdaFunctionName;
    public String bundlePostWorkerLambdaArn;
    public String bundlePostWorkerProvisionedConcurrencyLambdaAliasArn;
    public String bundlePostLambdaQueueName;
    public String bundlePostLambdaDeadLetterQueueName;
    public HttpMethod bundlePostLambdaHttpMethod;
    public String bundlePostLambdaUrlPath;
    public boolean bundlePostLambdaJwtAuthorizer;
    public boolean bundlePostLambdaCustomAuthorizer;

    public String bundleDeleteIngestLambdaHandler;
    public String bundleDeleteIngestLambdaFunctionName;
    public String bundleDeleteIngestLambdaArn;
    public String bundleDeleteIngestProvisionedConcurrencyLambdaAliasArn;
    public String bundleDeleteWorkerLambdaHandler;
    public String bundleDeleteWorkerLambdaFunctionName;
    public String bundleDeleteWorkerLambdaArn;
    public String bundleDeleteWorkerProvisionedConcurrencyLambdaAliasArn;
    public String bundleDeleteLambdaQueueName;
    public String bundleDeleteLambdaDeadLetterQueueName;
    public HttpMethod bundleDeleteLambdaHttpMethod;
    public String bundleDeleteLambdaUrlPath;
    public boolean bundleDeleteLambdaJwtAuthorizer;
    public boolean bundleDeleteLambdaCustomAuthorizer;

    public String hmrcTokenPostIngestLambdaHandler;
    public String hmrcTokenPostIngestLambdaFunctionName;
    public String hmrcTokenPostIngestLambdaArn;
    public String hmrcTokenPostIngestProvisionedConcurrencyLambdaAliasArn;
    public HttpMethod hmrcTokenPostLambdaHttpMethod;
    public String hmrcTokenPostLambdaUrlPath;
    public boolean hmrcTokenPostLambdaJwtAuthorizer;
    public boolean hmrcTokenPostLambdaCustomAuthorizer;

    public String hmrcVatReturnPostIngestLambdaHandler;
    public String hmrcVatReturnPostIngestLambdaFunctionName;
    public String hmrcVatReturnPostIngestLambdaArn;
    public String hmrcVatReturnPostIngestProvisionedConcurrencyLambdaAliasArn;
    public String hmrcVatReturnPostWorkerLambdaHandler;
    public String hmrcVatReturnPostWorkerLambdaFunctionName;
    public String hmrcVatReturnPostWorkerLambdaArn;
    public String hmrcVatReturnPostWorkerProvisionedConcurrencyLambdaAliasArn;
    public String hmrcVatReturnPostLambdaQueueName;
    public String hmrcVatReturnPostLambdaDeadLetterQueueName;
    public HttpMethod hmrcVatReturnPostLambdaHttpMethod;
    public String hmrcVatReturnPostLambdaUrlPath;
    public boolean hmrcVatReturnPostLambdaJwtAuthorizer;
    public boolean hmrcVatReturnPostLambdaCustomAuthorizer;

    public String hmrcVatObligationGetIngestLambdaHandler;
    public String hmrcVatObligationGetIngestLambdaFunctionName;
    public String hmrcVatObligationGetIngestLambdaArn;
    public String hmrcVatObligationGetIngestProvisionedConcurrencyLambdaAliasArn;
    public String hmrcVatObligationGetWorkerLambdaHandler;
    public String hmrcVatObligationGetWorkerLambdaFunctionName;
    public String hmrcVatObligationGetWorkerLambdaArn;
    public String hmrcVatObligationGetWorkerProvisionedConcurrencyLambdaAliasArn;
    public String hmrcVatObligationGetLambdaQueueName;
    public String hmrcVatObligationGetLambdaDeadLetterQueueName;
    public HttpMethod hmrcVatObligationGetLambdaHttpMethod;
    public String hmrcVatObligationGetLambdaUrlPath;
    public boolean hmrcVatObligationGetLambdaJwtAuthorizer;
    public boolean hmrcVatObligationGetLambdaCustomAuthorizer;

    public String hmrcVatReturnGetIngestLambdaHandler;
    public String hmrcVatReturnGetIngestLambdaFunctionName;
    public String hmrcVatReturnGetIngestLambdaArn;
    public String hmrcVatReturnGetIngestProvisionedConcurrencyLambdaAliasArn;
    public String hmrcVatReturnGetWorkerLambdaHandler;
    public String hmrcVatReturnGetWorkerLambdaFunctionName;
    public String hmrcVatReturnGetWorkerLambdaArn;
    public String hmrcVatReturnGetWorkerProvisionedConcurrencyLambdaAliasArn;
    public String hmrcVatReturnGetLambdaQueueName;
    public String hmrcVatReturnGetLambdaDeadLetterQueueName;
    public HttpMethod hmrcVatReturnGetLambdaHttpMethod;
    public String hmrcVatReturnGetLambdaUrlPath;
    public boolean hmrcVatReturnGetLambdaJwtAuthorizer;
    public boolean hmrcVatReturnGetLambdaCustomAuthorizer;

    public String receiptGetIngestLambdaHandler;
    public String receiptGetIngestLambdaFunctionName;
    public String receiptGetIngestLambdaArn;
    public String receiptGetIngestProvisionedConcurrencyLambdaAliasArn;
    public HttpMethod receiptGetLambdaHttpMethod;
    public String receiptGetLambdaUrlPath;
    public String receiptGetByNameLambdaUrlPath;
    public boolean receiptGetLambdaJwtAuthorizer;
    public boolean receiptGetLambdaCustomAuthorizer;

    public String supportTicketPostIngestLambdaHandler;
    public String supportTicketPostIngestLambdaFunctionName;
    public String supportTicketPostIngestLambdaArn;
    public String supportTicketPostIngestProvisionedConcurrencyLambdaAliasArn;
    public HttpMethod supportTicketPostLambdaHttpMethod;
    public String supportTicketPostLambdaUrlPath;
    public boolean supportTicketPostLambdaJwtAuthorizer;
    public boolean supportTicketPostLambdaCustomAuthorizer;

    public String interestPostIngestLambdaHandler;
    public String interestPostIngestLambdaFunctionName;
    public String interestPostIngestLambdaArn;
    public String interestPostIngestProvisionedConcurrencyLambdaAliasArn;
    public HttpMethod interestPostLambdaHttpMethod;
    public String interestPostLambdaUrlPath;
    public boolean interestPostLambdaJwtAuthorizer;
    public boolean interestPostLambdaCustomAuthorizer;

    public String passGetIngestLambdaHandler;
    public String passGetIngestLambdaFunctionName;
    public String passGetIngestLambdaArn;
    public String passGetIngestProvisionedConcurrencyLambdaAliasArn;
    public HttpMethod passGetLambdaHttpMethod;
    public String passGetLambdaUrlPath;
    public boolean passGetLambdaJwtAuthorizer;
    public boolean passGetLambdaCustomAuthorizer;

    public String passPostIngestLambdaHandler;
    public String passPostIngestLambdaFunctionName;
    public String passPostIngestLambdaArn;
    public String passPostIngestProvisionedConcurrencyLambdaAliasArn;
    public HttpMethod passPostLambdaHttpMethod;
    public String passPostLambdaUrlPath;
    public boolean passPostLambdaJwtAuthorizer;
    public boolean passPostLambdaCustomAuthorizer;

    public String passAdminPostIngestLambdaHandler;
    public String passAdminPostIngestLambdaFunctionName;
    public String passAdminPostIngestLambdaArn;
    public String passAdminPostIngestProvisionedConcurrencyLambdaAliasArn;
    public HttpMethod passAdminPostLambdaHttpMethod;
    public String passAdminPostLambdaUrlPath;
    public boolean passAdminPostLambdaJwtAuthorizer;
    public boolean passAdminPostLambdaCustomAuthorizer;

    // Pass Generate POST Lambda (JWT auth - user pass generation via tokens)
    public String passGeneratePostIngestLambdaHandler;
    public String passGeneratePostIngestLambdaFunctionName;
    public String passGeneratePostIngestLambdaArn;
    public String passGeneratePostIngestProvisionedConcurrencyLambdaAliasArn;
    public HttpMethod passGeneratePostLambdaHttpMethod;
    public String passGeneratePostLambdaUrlPath;
    public boolean passGeneratePostLambdaJwtAuthorizer;
    public boolean passGeneratePostLambdaCustomAuthorizer;

    // Pass My Passes GET Lambda (JWT auth - list user's generated passes)
    public String passMyPassesGetIngestLambdaHandler;
    public String passMyPassesGetIngestLambdaFunctionName;
    public String passMyPassesGetIngestLambdaArn;
    public String passMyPassesGetIngestProvisionedConcurrencyLambdaAliasArn;
    public HttpMethod passMyPassesGetLambdaHttpMethod;
    public String passMyPassesGetLambdaUrlPath;
    public boolean passMyPassesGetLambdaJwtAuthorizer;
    public boolean passMyPassesGetLambdaCustomAuthorizer;

    public String bundleCapacityReconcileLambdaHandler;
    public String bundleCapacityReconcileLambdaFunctionName;
    public String bundleCapacityReconcileLambdaArn;
    public String bundleCapacityReconcileProvisionedConcurrencyLambdaAliasArn;

    // Session Beacon POST Lambda (public, no auth)
    public String sessionBeaconPostIngestLambdaHandler;
    public String sessionBeaconPostIngestLambdaFunctionName;
    public String sessionBeaconPostIngestLambdaArn;
    public String sessionBeaconPostIngestProvisionedConcurrencyLambdaAliasArn;
    public HttpMethod sessionBeaconPostLambdaHttpMethod;
    public String sessionBeaconPostLambdaUrlPath;
    public boolean sessionBeaconPostLambdaJwtAuthorizer;
    public boolean sessionBeaconPostLambdaCustomAuthorizer;

    // Billing Lambda names
    public String billingCheckoutPostIngestLambdaHandler;
    public String billingCheckoutPostIngestLambdaFunctionName;
    public String billingCheckoutPostIngestLambdaArn;
    public String billingCheckoutPostIngestProvisionedConcurrencyLambdaAliasArn;
    public HttpMethod billingCheckoutPostLambdaHttpMethod;
    public String billingCheckoutPostLambdaUrlPath;
    public boolean billingCheckoutPostLambdaJwtAuthorizer;
    public boolean billingCheckoutPostLambdaCustomAuthorizer;

    public String billingPortalGetIngestLambdaHandler;
    public String billingPortalGetIngestLambdaFunctionName;
    public String billingPortalGetIngestLambdaArn;
    public String billingPortalGetIngestProvisionedConcurrencyLambdaAliasArn;
    public HttpMethod billingPortalGetLambdaHttpMethod;
    public String billingPortalGetLambdaUrlPath;
    public boolean billingPortalGetLambdaJwtAuthorizer;
    public boolean billingPortalGetLambdaCustomAuthorizer;

    public String billingRecoverPostIngestLambdaHandler;
    public String billingRecoverPostIngestLambdaFunctionName;
    public String billingRecoverPostIngestLambdaArn;
    public String billingRecoverPostIngestProvisionedConcurrencyLambdaAliasArn;
    public HttpMethod billingRecoverPostLambdaHttpMethod;
    public String billingRecoverPostLambdaUrlPath;
    public boolean billingRecoverPostLambdaJwtAuthorizer;
    public boolean billingRecoverPostLambdaCustomAuthorizer;

    public String billingWebhookPostIngestLambdaHandler;
    public String billingWebhookPostIngestLambdaFunctionName;
    public String billingWebhookPostIngestLambdaArn;
    public String billingWebhookPostIngestProvisionedConcurrencyLambdaAliasArn;
    public HttpMethod billingWebhookPostLambdaHttpMethod;
    public String billingWebhookPostLambdaUrlPath;
    public boolean billingWebhookPostLambdaJwtAuthorizer;
    public boolean billingWebhookPostLambdaCustomAuthorizer;

    public String billingStackId;

    // Telegram forwarder Lambda (EventBridge target, not API)
    public String activityTelegramForwarderLambdaHandler;
    public String activityTelegramForwarderLambdaFunctionName;
    public String activityTelegramForwarderLambdaArn;
    public String activityTelegramForwarderProvisionedConcurrencyLambdaAliasArn;

    public String selfDestructLambdaHandler;
    public String selfDestructLambdaFunctionName;
    public String selfDestructLambdaArn;
    public String selfDestructProvisionedConcurrencyLambdaAliasArn;

    public String edgeStackId;
    public String publishStackId;

    public static class SubmitSharedNamesProps {
        public String hostedZoneName;
        public String envName;
        public String subDomainName;
        public String deploymentName;
        public String regionName;
        public String awsAccount;
    }

    private SubmitSharedNames() {}

    // Common HTTP response codes to be referenced across infra generators and stacks
    public static class Responses {
        public static final String OK = "200";
        public static final String ACCEPTED = "202";
        public static final String UNAUTHORIZED = "401";
        public static final String FORBIDDEN = "403";
        public static final String NOT_FOUND = "404";
        public static final String SERVER_ERROR = "500";
    }

    public static SubmitSharedNames forDocs() {
        SubmitSharedNamesProps p = new SubmitSharedNamesProps();
        p.hostedZoneName = "example.com";
        p.envName = "docs";
        p.subDomainName = "submit";
        p.deploymentName = "docs";
        p.regionName = "eu-west-2";
        p.awsAccount = "111111111111";
        return new SubmitSharedNames(p);
    }

    public SubmitSharedNames(SubmitSharedNamesProps props) {
        this();
        this.hostedZoneName = props.hostedZoneName;
        this.envDomainName = "%s-%s.%s".formatted(props.envName, props.subDomainName, props.hostedZoneName);
        if ("prod".equals(props.envName)) {
            this.publicDomainName = "%s.%s".formatted(props.subDomainName, props.hostedZoneName);
        } else {
            this.publicDomainName = this.envDomainName;
        }
        this.cognitoDomainName = "%s-auth.%s".formatted(props.envName, props.hostedZoneName);
        this.holdingDomainName = "prod".equals(props.envName)
                ? "holding.%s".formatted(props.hostedZoneName)
                : "%s-holding.%s".formatted(props.envName, props.hostedZoneName);
        this.simulatorDomainName = "%s-simulator.%s".formatted(props.envName, props.hostedZoneName);
        this.deploymentDomainName = "%s.%s.%s"
                .formatted(
                        props.deploymentName,
                        props.subDomainName,
                        props.hostedZoneName); // TODO -> deploymentDomainName
        // this.defaultAliasName = "zero";
        this.provisionedConcurrencyAliasName = "pc";
        this.baseUrl = "https://%s/".formatted(this.deploymentDomainName);
        this.dashedDeploymentDomainName = buildDashedDomainName(this.deploymentDomainName);

        this.envBaseUrl = "https://%s/".formatted(this.envDomainName);
        this.publicBaseUrl = "https://%s/".formatted(this.publicDomainName);
        // Use envName directly for consistency with stack IDs (e.g., ci-env-IdentityStack → ci-env-user-pool)
        this.envResourceNamePrefix = "%s-env".formatted(props.envName);
        this.observabilityStackId = "%s-env-ObservabilityStack".formatted(props.envName);
        this.observabilityUE1StackId = "%s-env-ObservabilityUE1Stack".formatted(props.envName);
        this.dataStackId = "%s-env-DataStack".formatted(props.envName);
        this.identityStackId = "%s-env-IdentityStack".formatted(props.envName);
        this.apexStackId = "%s-env-ApexStack".formatted(props.envName);
        this.backupStackId = "%s-env-BackupStack".formatted(props.envName);
        this.activityStackId = "%s-env-ActivityStack".formatted(props.envName);
        this.simulatorStackId = "%s-env-SimulatorStack".formatted(props.envName);
        this.ecrStackId = "%s-env-EcrStack".formatted(props.envName);
        this.ue1EcrStackId = "%s-env-EcrUE1Stack".formatted(props.envName);
        this.ecrRepositoryArn = "arn:aws:ecr:%s:%s:repository/%s-ecr"
                .formatted(props.regionName, props.awsAccount, this.envResourceNamePrefix);
        this.ecrRepositoryName = "%s-ecr".formatted(this.envResourceNamePrefix);
        this.ecrLogGroupName = "/aws/ecr/%s".formatted(this.envResourceNamePrefix);
        this.ecrPublishRoleName = "%s-ecr-publish-role".formatted(this.envResourceNamePrefix);
        this.ue1EcrRepositoryArn =
                "arn:aws:ecr:us-east-1:%s:repository/%s-ecr".formatted(props.awsAccount, this.envResourceNamePrefix);
        this.ue1EcrRepositoryName = "%s-ecr-us-east-1".formatted(this.envResourceNamePrefix);
        this.ue1EcrLogGroupName = "/aws/ecr/%s-us-east-1".formatted(this.envResourceNamePrefix);
        this.ue1EcrPublishRoleName = "%s-ecr-publish-role-us-east-1".formatted(this.envResourceNamePrefix);
        this.cognitoBaseUri = "https://%s".formatted(this.cognitoDomainName);

        this.receiptsTableName = "%s-receipts".formatted(this.envResourceNamePrefix);
        this.bundlesTableName = "%s-bundles".formatted(this.envResourceNamePrefix);
        this.bundlePostAsyncRequestsTableName = "%s-bundle-post-async-requests".formatted(this.envResourceNamePrefix);
        this.bundleDeleteAsyncRequestsTableName =
                "%s-bundle-delete-async-requests".formatted(this.envResourceNamePrefix);
        this.hmrcVatReturnPostAsyncRequestsTableName =
                "%s-hmrc-vat-return-post-async-requests".formatted(this.envResourceNamePrefix);
        this.hmrcVatReturnGetAsyncRequestsTableName =
                "%s-hmrc-vat-return-get-async-requests".formatted(this.envResourceNamePrefix);
        this.hmrcVatObligationGetAsyncRequestsTableName =
                "%s-hmrc-vat-obligation-get-async-requests".formatted(this.envResourceNamePrefix);
        this.hmrcApiRequestsTableName = "%s-hmrc-api-requests".formatted(this.envResourceNamePrefix);
        this.passesTableName = "%s-passes".formatted(this.envResourceNamePrefix);
        this.bundleCapacityTableName = "%s-bundle-capacity".formatted(this.envResourceNamePrefix);
        this.activityBusName = "%s-activity-bus".formatted(this.envResourceNamePrefix);
        this.subscriptionsTableName = "%s-subscriptions".formatted(this.envResourceNamePrefix);
        this.distributionAccessLogGroupName = "distribution-%s-logs".formatted(this.envResourceNamePrefix);
        this.distributionAccessLogDeliveryHoldingSourceName =
                "%s-holding-dist-logs-src".formatted(this.envResourceNamePrefix);
        this.distributionAccessLogDeliveryOriginSourceName = "%s-orig-dist-l-src".formatted(props.deploymentName); // x
        this.distributionAccessLogDeliveryHoldingDestinationName =
                "%s-holding-logs-dest".formatted(this.envResourceNamePrefix);
        this.distributionAccessLogDeliveryOriginDestinationName = "%s-orig-l-dst".formatted(props.deploymentName); // x

        this.ew2SelfDestructLogGroupName =
                "/aws/lambda/%s-self-destruct-eu-west-2".formatted(this.envResourceNamePrefix);
        this.ue1SelfDestructLogGroupName =
                "/aws/lambda/%s-self-destruct-us-east-1".formatted(this.envResourceNamePrefix);
        this.apiAccessLogGroupName = "/aws/apigw/%s/access".formatted(this.envResourceNamePrefix);

        this.appResourceNamePrefix = "%s-app".formatted(props.deploymentName);
        this.authStackId = "%s-app-AuthStack".formatted(props.deploymentName);
        this.hmrcStackId = "%s-app-HmrcStack".formatted(props.deploymentName);
        this.accountStackId = "%s-app-AccountStack".formatted(props.deploymentName);
        this.billingStackId = "%s-app-BillingStack".formatted(props.deploymentName);
        this.apiStackId = "%s-app-ApiStack".formatted(props.deploymentName);
        this.opsStackId = "%s-app-OpsStack".formatted(props.deploymentName);
        this.selfDestructStackId = "%s-app-SelfDestructStack".formatted(props.deploymentName);

        this.edgeStackId = "%s-app-EdgeStack".formatted(props.deploymentName);
        this.publishStackId = "%s-app-PublishStack".formatted(props.deploymentName);

        this.trailName = "%s-trail".formatted(this.envResourceNamePrefix);
        this.holdingBucketName =
                convertDotSeparatedToDashSeparated("%s-holding-us-east-1".formatted(this.envResourceNamePrefix));
        this.originBucketName =
                convertDotSeparatedToDashSeparated("%s-origin-us-east-1".formatted(this.appResourceNamePrefix));
        this.originAccessLogBucketName = "%s-origin-access-logs".formatted(this.appResourceNamePrefix);

        var appLambdaHandlerPrefix = "app/functions";
        var appLambdaArnPrefix = "arn:aws:lambda:%s:%s:function:%s"
                .formatted(props.regionName, props.awsAccount, this.appResourceNamePrefix);

        this.cognitoTokenPostLambdaHttpMethod = HttpMethod.POST;
        this.cognitoTokenPostLambdaUrlPath = "/api/v1/cognito/token";
        this.cognitoTokenPostLambdaJwtAuthorizer = false;
        this.cognitoTokenPostLambdaCustomAuthorizer = false;
        var cognitoTokenPostLambdaHandlerName = "cognitoTokenPost.ingestHandler";
        var cognitoTokenPostLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(cognitoTokenPostLambdaHandlerName);
        this.cognitoTokenPostIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, cognitoTokenPostLambdaHandlerDashed);
        this.cognitoTokenPostIngestLambdaHandler =
                "%s/auth/%s".formatted(appLambdaHandlerPrefix, cognitoTokenPostLambdaHandlerName);
        this.cognitoTokenPostIngestLambdaArn =
                "%s-%s".formatted(appLambdaArnPrefix, cognitoTokenPostLambdaHandlerDashed);
        this.cognitoTokenPostIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.cognitoTokenPostIngestLambdaArn, this.provisionedConcurrencyAliasName);
        publishedApiLambdas.add(new PublishedLambda(
                this.cognitoTokenPostLambdaHttpMethod,
                this.cognitoTokenPostLambdaUrlPath,
                "Exchange Cognito authorization code for access token",
                "Exchanges an authorization code for a Cognito access token",
                "exchangeCognitoToken"));

        // Custom authorizer for HMRC VAT endpoints
        var customAuthorizerHandlerName = "customAuthorizer.ingestHandler";
        var customAuthorizerHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(customAuthorizerHandlerName);
        this.customAuthorizerIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, customAuthorizerHandlerDashed);
        this.customAuthorizerIngestLambdaHandler =
                "%s/auth/%s".formatted(appLambdaHandlerPrefix, customAuthorizerHandlerName);
        this.customAuthorizerIngestLambdaArn = "%s-%s".formatted(appLambdaArnPrefix, customAuthorizerHandlerDashed);
        this.customAuthorizerIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.customAuthorizerIngestLambdaArn, this.provisionedConcurrencyAliasName);

        this.bundleGetLambdaHttpMethod = HttpMethod.GET;
        this.bundleGetLambdaUrlPath = "/api/v1/bundle";
        this.bundleGetLambdaJwtAuthorizer = true;
        this.bundleGetLambdaCustomAuthorizer = false;
        var bundleGetLambdaHandlerName = "bundleGet.ingestHandler";
        var bundleGetLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(bundleGetLambdaHandlerName);
        this.bundleGetIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, bundleGetLambdaHandlerDashed);
        this.bundleGetIngestLambdaHandler =
                "%s/account/%s".formatted(appLambdaHandlerPrefix, bundleGetLambdaHandlerName);
        this.bundleGetIngestLambdaArn = "%s-%s".formatted(appLambdaArnPrefix, bundleGetLambdaHandlerDashed);
        this.bundleGetIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.bundleGetIngestLambdaArn, this.provisionedConcurrencyAliasName);
        publishedApiLambdas.add(new PublishedLambda(
                this.bundleGetLambdaHttpMethod,
                this.bundleGetLambdaUrlPath,
                "Get user bundles",
                "Retrieves all bundles for the authenticated user",
                "getBundles",
                List.of(new ApiParameter(
                        "x-wait-time-ms", "header", false, "Max time to wait for synchronous response (ms)"))));

        var bundlePostProps = LambdaNameProps.builder()
                .apiHttpMethod(HttpMethod.POST)
                .apiUrlPath("/api/v1/bundle")
                .handlerPath("account")
                .ingestHandlerName("bundlePost.ingestHandler")
                .workerHandlerName("bundlePost.workerHandler")
                .apiJwtAuthorizer(true)
                .apiCustomAuthorizer(false)
                .resourceNamePrefix(this.appResourceNamePrefix)
                .lambdaArnPrefix(appLambdaArnPrefix)
                .provisionedConcurrencyAliasName(this.provisionedConcurrencyAliasName)
                .build();
        this.bundlePost = new LambdaNames(bundlePostProps);
        // TODO: Remove and reference bundlePost directly where used
        this.bundlePostLambdaHttpMethod = this.bundlePost.apiHttpMethod;
        this.bundlePostLambdaUrlPath = this.bundlePost.apiUrlPath;
        this.bundlePostLambdaJwtAuthorizer = this.bundlePost.apiJwtAuthorizer;
        this.bundlePostLambdaCustomAuthorizer = this.bundlePost.apiCustomAuthorizer;
        this.bundlePostIngestLambdaFunctionName = this.bundlePost.ingestLambdaFunctionName;
        this.bundlePostIngestLambdaHandler = this.bundlePost.ingestLambdaHandler;
        this.bundlePostIngestLambdaArn = this.bundlePost.ingestLambdaArn;
        this.bundlePostIngestProvisionedConcurrencyLambdaAliasArn =
                this.bundlePost.ingestProvisionedConcurrencyLambdaAliasArn;
        this.bundlePostWorkerLambdaFunctionName = this.bundlePost.workerLambdaFunctionName;
        this.bundlePostWorkerLambdaHandler = this.bundlePost.workerLambdaHandler;
        this.bundlePostWorkerLambdaArn = this.bundlePost.workerLambdaArn;
        this.bundlePostWorkerProvisionedConcurrencyLambdaAliasArn =
                this.bundlePost.workerProvisionedConcurrencyLambdaAliasArn;
        this.bundlePostLambdaQueueName = "%s-queue".formatted(this.bundlePostIngestLambdaFunctionName);
        this.bundlePostLambdaDeadLetterQueueName = "%s-dlq".formatted(this.bundlePostIngestLambdaFunctionName);
        publishedApiLambdas.add(new PublishedLambda(
                this.bundlePostLambdaHttpMethod,
                this.bundlePostLambdaUrlPath,
                "Request new bundle",
                "Creates a new bundle request for the authenticated user",
                "requestBundle"));
        //        this.bundlePostLambdaHttpMethod = HttpMethod.POST;
        //        this.bundlePostLambdaUrlPath = "/api/v1/bundle";
        //        this.bundlePostLambdaJwtAuthorizer = true;
        //        this.bundlePostLambdaCustomAuthorizer = false;
        //        var bundlePostLambdaHandlerName = "bundlePost.ingestHandler";
        //        var bundlePostLambdaWorkerHandlerName = "bundlePost.workerHandler";
        //        var bundlePostLambdaHandlerDashed =
        //            ResourceNameUtils.convertCamelCaseToDashSeparated(bundlePostLambdaHandlerName);
        //        this.bundlePostIngestLambdaFunctionName =
        //            "%s-%s".formatted(this.appResourceNamePrefix, bundlePostLambdaHandlerDashed);
        //        this.bundlePostIngestLambdaHandler = "%s/account/%s".formatted(appLambdaHandlerPrefix,
        // bundlePostLambdaHandlerName);
        //        this.bundlePostIngestLambdaArn = "%s-%s".formatted(appLambdaArnPrefix, bundlePostLambdaHandlerDashed);
        //        this.bundlePostIngestDefaultAliasLambdaArn = "%s:%s".formatted(this.bundlePostIngestLambdaArn,
        // this.defaultAliasName);
        //        this.bundlePostWorkerLambdaFunctionName =
        // "%s-worker".formatted(this.bundlePostIngestLambdaFunctionName);
        //        this.bundlePostWorkerLambdaHandler =
        //            "%s/account/%s".formatted(appLambdaHandlerPrefix, bundlePostLambdaWorkerHandlerName);
        //        this.bundlePostWorkerLambdaArn = "%s-worker".formatted(this.bundlePostIngestLambdaArn);
        //        this.bundlePostWorkerDefaultAliasLambdaArn =
        //            "%s:%s".formatted(this.bundlePostWorkerLambdaArn, this.defaultAliasName);
        //        this.bundlePostLambdaQueueName = "%s-queue".formatted(this.bundlePostIngestLambdaFunctionName);
        //        this.bundlePostLambdaDeadLetterQueueName =
        // "%s-dlq".formatted(this.bundlePostIngestLambdaFunctionName);
        //        publishedApiLambdas.add(new PublishedLambda(
        //            this.bundlePostLambdaHttpMethod,
        //            this.bundlePostLambdaUrlPath,
        //            "Request new bundle",
        //            "Creates a new bundle request for the authenticated user",
        //            "requestBundle"));

        this.bundleDeleteLambdaHttpMethod = HttpMethod.DELETE;
        this.bundleDeleteLambdaUrlPath = "/api/v1/bundle";
        this.bundleDeleteLambdaJwtAuthorizer = true;
        this.bundleDeleteLambdaCustomAuthorizer = false;
        var bundleDeleteLambdaHandlerName = "bundleDelete.ingestHandler";
        var bundleDeleteLambdaWorkerHandlerName = "bundleDelete.workerHandler";
        var bundleDeleteLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(bundleDeleteLambdaHandlerName);
        this.bundleDeleteIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, bundleDeleteLambdaHandlerDashed);
        this.bundleDeleteIngestLambdaHandler =
                "%s/account/%s".formatted(appLambdaHandlerPrefix, bundleDeleteLambdaHandlerName);
        this.bundleDeleteIngestLambdaArn = "%s-%s".formatted(appLambdaArnPrefix, bundleDeleteLambdaHandlerDashed);
        this.bundleDeleteIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.bundleDeleteIngestLambdaArn, this.provisionedConcurrencyAliasName);
        this.bundleDeleteWorkerLambdaFunctionName = "%s-worker".formatted(this.bundleDeleteIngestLambdaFunctionName);
        this.bundleDeleteWorkerLambdaHandler =
                "%s/account/%s".formatted(appLambdaHandlerPrefix, bundleDeleteLambdaWorkerHandlerName);
        this.bundleDeleteWorkerLambdaArn = "%s-worker".formatted(this.bundleDeleteIngestLambdaArn);
        this.bundleDeleteWorkerProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.bundleDeleteWorkerLambdaArn, this.provisionedConcurrencyAliasName);
        this.bundleDeleteLambdaQueueName = "%s-queue".formatted(this.bundleDeleteIngestLambdaFunctionName);
        this.bundleDeleteLambdaDeadLetterQueueName = "%s-dlq".formatted(this.bundleDeleteIngestLambdaFunctionName);
        publishedApiLambdas.add(new PublishedLambda(
                this.bundleDeleteLambdaHttpMethod,
                this.bundleDeleteLambdaUrlPath,
                "Delete bundle",
                "Deletes a bundle for the authenticated user",
                "deleteBundle",
                List.of(
                        new ApiParameter("bundleId", "query", false, "The bundle id (or name) to delete"),
                        new ApiParameter("removeAll", "query", false, "When true, removes all bundles"))));
        publishedApiLambdas.add(new PublishedLambda(
                this.bundleDeleteLambdaHttpMethod,
                "/api/v1/bundle/{id}",
                "Delete bundle by id",
                "Deletes a bundle for the authenticated user using a path parameter",
                "deleteBundleById",
                List.of(new ApiParameter("id", "path", true, "The bundle id (or name) to delete"))));

        this.hmrcTokenPostLambdaHttpMethod = HttpMethod.POST;
        this.hmrcTokenPostLambdaUrlPath = "/api/v1/hmrc/token";
        this.hmrcTokenPostLambdaJwtAuthorizer = false;
        this.hmrcTokenPostLambdaCustomAuthorizer = false;
        var hmrcTokenPostLambdaHandlerName = "hmrcTokenPost.ingestHandler";
        var hmrcTokenPostLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(hmrcTokenPostLambdaHandlerName);
        this.hmrcTokenPostIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, hmrcTokenPostLambdaHandlerDashed);
        this.hmrcTokenPostIngestLambdaHandler =
                "%s/hmrc/%s".formatted(appLambdaHandlerPrefix, hmrcTokenPostLambdaHandlerName);
        this.hmrcTokenPostIngestLambdaArn = "%s-%s".formatted(appLambdaArnPrefix, hmrcTokenPostLambdaHandlerDashed);
        this.hmrcTokenPostIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.hmrcTokenPostIngestLambdaArn, this.provisionedConcurrencyAliasName);
        publishedApiLambdas.add(new PublishedLambda(
                this.hmrcTokenPostLambdaHttpMethod,
                this.hmrcTokenPostLambdaUrlPath,
                "Exchange HMRC authorization code for access token",
                "Exchanges an HMRC authorization code for an access token",
                "exchangeHmrcToken"));

        this.hmrcVatReturnPostLambdaHttpMethod = HttpMethod.POST;
        this.hmrcVatReturnPostLambdaUrlPath = "/api/v1/hmrc/vat/return";
        this.hmrcVatReturnPostLambdaJwtAuthorizer = false;
        this.hmrcVatReturnPostLambdaCustomAuthorizer = true;
        var hmrcVatReturnPostLambdaHandlerName = "hmrcVatReturnPost.ingestHandler";
        var hmrcVatReturnPostLambdaWorkerHandlerName = "hmrcVatReturnPost.workerHandler";
        var hmrcVatReturnPostLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(hmrcVatReturnPostLambdaHandlerName);
        this.hmrcVatReturnPostIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, hmrcVatReturnPostLambdaHandlerDashed);
        this.hmrcVatReturnPostIngestLambdaHandler =
                "%s/hmrc/%s".formatted(appLambdaHandlerPrefix, hmrcVatReturnPostLambdaHandlerName);
        this.hmrcVatReturnPostIngestLambdaArn =
                "%s-%s".formatted(appLambdaArnPrefix, hmrcVatReturnPostLambdaHandlerDashed);
        this.hmrcVatReturnPostIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.hmrcVatReturnPostIngestLambdaArn, this.provisionedConcurrencyAliasName);
        this.hmrcVatReturnPostWorkerLambdaFunctionName =
                "%s-worker".formatted(this.hmrcVatReturnPostIngestLambdaFunctionName);
        this.hmrcVatReturnPostWorkerLambdaHandler =
                "%s/hmrc/%s".formatted(appLambdaHandlerPrefix, hmrcVatReturnPostLambdaWorkerHandlerName);
        this.hmrcVatReturnPostWorkerLambdaArn = "%s-worker".formatted(this.hmrcVatReturnPostIngestLambdaArn);
        this.hmrcVatReturnPostWorkerProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.hmrcVatReturnPostWorkerLambdaArn, this.provisionedConcurrencyAliasName);
        this.hmrcVatReturnPostLambdaQueueName = "%s-queue".formatted(this.hmrcVatReturnPostIngestLambdaFunctionName);
        this.hmrcVatReturnPostLambdaDeadLetterQueueName =
                "%s-dlq".formatted(this.hmrcVatReturnPostIngestLambdaFunctionName);
        publishedApiLambdas.add(new PublishedLambda(
                this.hmrcVatReturnPostLambdaHttpMethod,
                this.hmrcVatReturnPostLambdaUrlPath,
                "Submit VAT return to HMRC",
                "Submits a VAT return to HMRC on behalf of the authenticated user",
                "submitVatReturn",
                List.of(
                        new ApiParameter("Gov-Test-Scenario", "header", false, "HMRC sandbox test scenario"),
                        new ApiParameter(
                                "runFraudPreventionHeaderValidation",
                                "query",
                                false,
                                "When true, validates HMRC Fraud Prevention Headers"))));

        this.hmrcVatObligationGetLambdaHttpMethod = HttpMethod.GET;
        this.hmrcVatObligationGetLambdaUrlPath = "/api/v1/hmrc/vat/obligation";
        this.hmrcVatObligationGetLambdaJwtAuthorizer = false;
        this.hmrcVatObligationGetLambdaCustomAuthorizer = true;
        var hmrcVatObligationGetLambdaHandlerName = "hmrcVatObligationGet.ingestHandler";
        var hmrcVatObligationGetLambdaWorkerHandlerName = "hmrcVatObligationGet.workerHandler";
        var hmrcVatObligationGetLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(hmrcVatObligationGetLambdaHandlerName);
        this.hmrcVatObligationGetIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, hmrcVatObligationGetLambdaHandlerDashed);
        this.hmrcVatObligationGetIngestLambdaHandler =
                "%s/hmrc/%s".formatted(appLambdaHandlerPrefix, hmrcVatObligationGetLambdaHandlerName);
        this.hmrcVatObligationGetIngestLambdaArn =
                "%s-%s".formatted(appLambdaArnPrefix, hmrcVatObligationGetLambdaHandlerDashed);
        this.hmrcVatObligationGetIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.hmrcVatObligationGetIngestLambdaArn, this.provisionedConcurrencyAliasName);
        this.hmrcVatObligationGetWorkerLambdaFunctionName =
                "%s-worker".formatted(this.hmrcVatObligationGetIngestLambdaFunctionName);
        this.hmrcVatObligationGetWorkerLambdaHandler =
                "%s/hmrc/%s".formatted(appLambdaHandlerPrefix, hmrcVatObligationGetLambdaWorkerHandlerName);
        this.hmrcVatObligationGetWorkerLambdaArn = "%s-worker".formatted(this.hmrcVatObligationGetIngestLambdaArn);
        this.hmrcVatObligationGetWorkerProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.hmrcVatObligationGetWorkerLambdaArn, this.provisionedConcurrencyAliasName);
        this.hmrcVatObligationGetLambdaQueueName =
                "%s-queue".formatted(this.hmrcVatObligationGetIngestLambdaFunctionName);
        this.hmrcVatObligationGetLambdaDeadLetterQueueName =
                "%s-dlq".formatted(this.hmrcVatObligationGetIngestLambdaFunctionName);
        publishedApiLambdas.add(new PublishedLambda(
                this.hmrcVatObligationGetLambdaHttpMethod,
                this.hmrcVatObligationGetLambdaUrlPath,
                "Get VAT obligations from HMRC",
                "Retrieves VAT obligations from HMRC for the authenticated user",
                "getVatObligations",
                List.of(
                        new ApiParameter("vrn", "query", true, "VAT registration number (9 digits)"),
                        new ApiParameter("from", "query", false, "From date in YYYY-MM-DD format"),
                        new ApiParameter("to", "query", false, "To date in YYYY-MM-DD format"),
                        new ApiParameter("status", "query", false, "Obligation status: O (Open) or F (Fulfilled)"),
                        new ApiParameter("Gov-Test-Scenario", "query", false, "HMRC sandbox test scenario"),
                        new ApiParameter(
                                "runFraudPreventionHeaderValidation",
                                "query",
                                false,
                                "When true, validates HMRC Fraud Prevention Headers"))));

        this.hmrcVatReturnGetLambdaHttpMethod = HttpMethod.GET;
        // Note: Uses query params (vrn, periodStart, periodEnd), not path parameter
        // Must match Express server route in app/functions/hmrc/hmrcVatReturnGet.js
        this.hmrcVatReturnGetLambdaUrlPath = "/api/v1/hmrc/vat/return";
        this.hmrcVatReturnGetLambdaJwtAuthorizer = false;
        this.hmrcVatReturnGetLambdaCustomAuthorizer = true;
        var hmrcVatReturnGetLambdaHandlerName = "hmrcVatReturnGet.ingestHandler";
        var hmrcVatReturnGetLambdaWorkerHandlerName = "hmrcVatReturnGet.workerHandler";
        var hmrcVatReturnGetLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(hmrcVatReturnGetLambdaHandlerName);
        this.hmrcVatReturnGetIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, hmrcVatReturnGetLambdaHandlerDashed);
        this.hmrcVatReturnGetIngestLambdaHandler =
                "%s/hmrc/%s".formatted(appLambdaHandlerPrefix, hmrcVatReturnGetLambdaHandlerName);
        this.hmrcVatReturnGetIngestLambdaArn =
                "%s-%s".formatted(appLambdaArnPrefix, hmrcVatReturnGetLambdaHandlerDashed);
        this.hmrcVatReturnGetIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.hmrcVatReturnGetIngestLambdaArn, this.provisionedConcurrencyAliasName);
        this.hmrcVatReturnGetWorkerLambdaFunctionName =
                "%s-worker".formatted(this.hmrcVatReturnGetIngestLambdaFunctionName);
        this.hmrcVatReturnGetWorkerLambdaHandler =
                "%s/hmrc/%s".formatted(appLambdaHandlerPrefix, hmrcVatReturnGetLambdaWorkerHandlerName);
        this.hmrcVatReturnGetWorkerLambdaArn = "%s-worker".formatted(this.hmrcVatReturnGetIngestLambdaArn);
        this.hmrcVatReturnGetWorkerProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.hmrcVatReturnGetWorkerLambdaArn, this.provisionedConcurrencyAliasName);
        this.hmrcVatReturnGetLambdaQueueName = "%s-queue".formatted(this.hmrcVatReturnGetIngestLambdaFunctionName);
        this.hmrcVatReturnGetLambdaDeadLetterQueueName =
                "%s-dlq".formatted(this.hmrcVatReturnGetIngestLambdaFunctionName);
        publishedApiLambdas.add(new PublishedLambda(
                this.hmrcVatReturnGetLambdaHttpMethod,
                this.hmrcVatReturnGetLambdaUrlPath,
                "Get submitted VAT returns from HMRC",
                "Retrieves previously submitted VAT returns from HMRC for the authenticated user",
                "getVatReturns",
                List.of(
                        new ApiParameter("periodKey", "path", true, "The VAT period key to retrieve"),
                        new ApiParameter("vrn", "query", true, "VAT registration number (9 digits)"),
                        new ApiParameter("Gov-Test-Scenario", "query", false, "HMRC sandbox test scenario"),
                        new ApiParameter(
                                "runFraudPreventionHeaderValidation",
                                "query",
                                false,
                                "When true, validates HMRC Fraud Prevention Headers"))));

        this.receiptGetLambdaHttpMethod = HttpMethod.GET;
        this.receiptGetLambdaUrlPath = "/api/v1/hmrc/receipt";
        this.receiptGetLambdaJwtAuthorizer = true;
        this.receiptGetLambdaCustomAuthorizer = false;
        this.receiptGetByNameLambdaUrlPath = "/api/v1/hmrc/receipt/{name}";
        var receiptGetLambdaHandlerName = "hmrcReceiptGet.ingestHandler";
        var receiptGetLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(receiptGetLambdaHandlerName);
        this.receiptGetIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, receiptGetLambdaHandlerDashed);
        this.receiptGetIngestLambdaHandler =
                "%s/hmrc/%s".formatted(appLambdaHandlerPrefix, receiptGetLambdaHandlerName);
        this.receiptGetIngestLambdaArn = "%s-%s".formatted(appLambdaArnPrefix, receiptGetLambdaHandlerDashed);
        this.receiptGetIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.receiptGetIngestLambdaArn, this.provisionedConcurrencyAliasName);
        publishedApiLambdas.add(new PublishedLambda(
                this.receiptGetLambdaHttpMethod,
                this.receiptGetLambdaUrlPath,
                "Retrieve stored receipts",
                "Retrieves previously stored receipts for the authenticated user",
                "getReceipts",
                List.of(
                        new ApiParameter("name", "query", false, "Receipt file name including .json"),
                        new ApiParameter("key", "query", false, "Full DynamoDB Item key"))));
        publishedApiLambdas.add(new PublishedLambda(
                this.receiptGetLambdaHttpMethod,
                this.receiptGetByNameLambdaUrlPath,
                "Retrieve a stored receipt by name",
                "Retrieves a specific stored receipt for the authenticated user by file name",
                "getReceiptByName",
                List.of(new ApiParameter("name", "path", true, "The receipt file name including .json"))));

        this.supportTicketPostLambdaHttpMethod = HttpMethod.POST;
        this.supportTicketPostLambdaUrlPath = "/api/v1/support/ticket";
        this.supportTicketPostLambdaJwtAuthorizer = false;
        this.supportTicketPostLambdaCustomAuthorizer = false;
        var supportTicketPostLambdaHandlerName = "supportTicketPost.ingestHandler";
        var supportTicketPostLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(supportTicketPostLambdaHandlerName);
        this.supportTicketPostIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, supportTicketPostLambdaHandlerDashed);
        this.supportTicketPostIngestLambdaHandler =
                "%s/support/%s".formatted(appLambdaHandlerPrefix, supportTicketPostLambdaHandlerName);
        this.supportTicketPostIngestLambdaArn =
                "%s-%s".formatted(appLambdaArnPrefix, supportTicketPostLambdaHandlerDashed);
        this.supportTicketPostIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.supportTicketPostIngestLambdaArn, this.provisionedConcurrencyAliasName);
        publishedApiLambdas.add(new PublishedLambda(
                this.supportTicketPostLambdaHttpMethod,
                this.supportTicketPostLambdaUrlPath,
                "Submit a support ticket",
                "Creates a GitHub issue for the authenticated user's support request",
                "submitSupportTicket"));

        // Interest POST Lambda (JWT auth - register waitlist interest)
        this.interestPostLambdaHttpMethod = HttpMethod.POST;
        this.interestPostLambdaUrlPath = "/api/v1/interest";
        this.interestPostLambdaJwtAuthorizer = true;
        this.interestPostLambdaCustomAuthorizer = false;
        var interestPostLambdaHandlerName = "interestPost.ingestHandler";
        var interestPostLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(interestPostLambdaHandlerName);
        this.interestPostIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, interestPostLambdaHandlerDashed);
        this.interestPostIngestLambdaHandler =
                "%s/account/%s".formatted(appLambdaHandlerPrefix, interestPostLambdaHandlerName);
        this.interestPostIngestLambdaArn = "%s-%s".formatted(appLambdaArnPrefix, interestPostLambdaHandlerDashed);
        this.interestPostIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.interestPostIngestLambdaArn, this.provisionedConcurrencyAliasName);
        publishedApiLambdas.add(new PublishedLambda(
                this.interestPostLambdaHttpMethod,
                this.interestPostLambdaUrlPath,
                "Register waitlist interest",
                "Publishes the authenticated user's email to an SNS topic for waitlist registration",
                "registerInterest"));

        // Pass GET Lambda (public, no auth)
        this.passGetLambdaHttpMethod = HttpMethod.GET;
        this.passGetLambdaUrlPath = "/api/v1/pass";
        this.passGetLambdaJwtAuthorizer = false;
        this.passGetLambdaCustomAuthorizer = false;
        var passGetLambdaHandlerName = "passGet.ingestHandler";
        var passGetLambdaHandlerDashed = ResourceNameUtils.convertCamelCaseToDashSeparated(passGetLambdaHandlerName);
        this.passGetIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, passGetLambdaHandlerDashed);
        this.passGetIngestLambdaHandler = "%s/account/%s".formatted(appLambdaHandlerPrefix, passGetLambdaHandlerName);
        this.passGetIngestLambdaArn = "%s-%s".formatted(appLambdaArnPrefix, passGetLambdaHandlerDashed);
        this.passGetIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.passGetIngestLambdaArn, this.provisionedConcurrencyAliasName);
        publishedApiLambdas.add(new PublishedLambda(
                this.passGetLambdaHttpMethod,
                this.passGetLambdaUrlPath,
                "Validate a pass code",
                "Checks if a pass code is valid and returns its details",
                "validatePass",
                List.of(new ApiParameter("code", "query", true, "The four-word pass code to validate"))));

        // Pass POST Lambda (JWT auth)
        this.passPostLambdaHttpMethod = HttpMethod.POST;
        this.passPostLambdaUrlPath = "/api/v1/pass";
        this.passPostLambdaJwtAuthorizer = true;
        this.passPostLambdaCustomAuthorizer = false;
        var passPostLambdaHandlerName = "passPost.ingestHandler";
        var passPostLambdaHandlerDashed = ResourceNameUtils.convertCamelCaseToDashSeparated(passPostLambdaHandlerName);
        this.passPostIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, passPostLambdaHandlerDashed);
        this.passPostIngestLambdaHandler = "%s/account/%s".formatted(appLambdaHandlerPrefix, passPostLambdaHandlerName);
        this.passPostIngestLambdaArn = "%s-%s".formatted(appLambdaArnPrefix, passPostLambdaHandlerDashed);
        this.passPostIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.passPostIngestLambdaArn, this.provisionedConcurrencyAliasName);
        publishedApiLambdas.add(new PublishedLambda(
                this.passPostLambdaHttpMethod,
                this.passPostLambdaUrlPath,
                "Redeem a pass code",
                "Redeems a pass code and grants the associated bundle to the authenticated user",
                "redeemPass"));

        // Pass Admin POST Lambda (JWT auth)
        this.passAdminPostLambdaHttpMethod = HttpMethod.POST;
        this.passAdminPostLambdaUrlPath = "/api/v1/pass/admin";
        this.passAdminPostLambdaJwtAuthorizer = false;
        this.passAdminPostLambdaCustomAuthorizer = false;
        var passAdminPostLambdaHandlerName = "passAdminPost.ingestHandler";
        var passAdminPostLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(passAdminPostLambdaHandlerName);
        this.passAdminPostIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, passAdminPostLambdaHandlerDashed);
        this.passAdminPostIngestLambdaHandler =
                "%s/account/%s".formatted(appLambdaHandlerPrefix, passAdminPostLambdaHandlerName);
        this.passAdminPostIngestLambdaArn = "%s-%s".formatted(appLambdaArnPrefix, passAdminPostLambdaHandlerDashed);
        this.passAdminPostIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.passAdminPostIngestLambdaArn, this.provisionedConcurrencyAliasName);
        publishedApiLambdas.add(new PublishedLambda(
                this.passAdminPostLambdaHttpMethod,
                this.passAdminPostLambdaUrlPath,
                "Generate a new pass",
                "Generates a new pass code for a specified bundle type (admin only)",
                "generatePass"));

        // Pass Generate POST Lambda (JWT auth - user pass generation using tokens)
        this.passGeneratePostLambdaHttpMethod = HttpMethod.POST;
        this.passGeneratePostLambdaUrlPath = "/api/v1/pass/generate";
        this.passGeneratePostLambdaJwtAuthorizer = true;
        this.passGeneratePostLambdaCustomAuthorizer = false;
        var passGeneratePostLambdaHandlerName = "passGeneratePost.ingestHandler";
        var passGeneratePostLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(passGeneratePostLambdaHandlerName);
        this.passGeneratePostIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, passGeneratePostLambdaHandlerDashed);
        this.passGeneratePostIngestLambdaHandler =
                "%s/account/%s".formatted(appLambdaHandlerPrefix, passGeneratePostLambdaHandlerName);
        this.passGeneratePostIngestLambdaArn =
                "%s-%s".formatted(appLambdaArnPrefix, passGeneratePostLambdaHandlerDashed);
        this.passGeneratePostIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.passGeneratePostIngestLambdaArn, this.provisionedConcurrencyAliasName);
        publishedApiLambdas.add(new PublishedLambda(
                this.passGeneratePostLambdaHttpMethod,
                this.passGeneratePostLambdaUrlPath,
                "Generate a pass using tokens",
                "Generates a new pass code using the authenticated user's token balance",
                "generatePassWithTokens"));

        // Pass My Passes GET Lambda (JWT auth - list user's generated passes)
        this.passMyPassesGetLambdaHttpMethod = HttpMethod.GET;
        this.passMyPassesGetLambdaUrlPath = "/api/v1/pass/my-passes";
        this.passMyPassesGetLambdaJwtAuthorizer = true;
        this.passMyPassesGetLambdaCustomAuthorizer = false;
        var passMyPassesGetLambdaHandlerName = "passMyPassesGet.ingestHandler";
        var passMyPassesGetLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(passMyPassesGetLambdaHandlerName);
        this.passMyPassesGetIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, passMyPassesGetLambdaHandlerDashed);
        this.passMyPassesGetIngestLambdaHandler =
                "%s/account/%s".formatted(appLambdaHandlerPrefix, passMyPassesGetLambdaHandlerName);
        this.passMyPassesGetIngestLambdaArn = "%s-%s".formatted(appLambdaArnPrefix, passMyPassesGetLambdaHandlerDashed);
        this.passMyPassesGetIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.passMyPassesGetIngestLambdaArn, this.provisionedConcurrencyAliasName);
        publishedApiLambdas.add(new PublishedLambda(
                this.passMyPassesGetLambdaHttpMethod,
                this.passMyPassesGetLambdaUrlPath,
                "List generated passes",
                "Lists all passes generated by the authenticated user",
                "listMyPasses",
                List.of(new ApiParameter(
                        "limit", "query", false, "Maximum number of passes to return (default 20, max 50)"))));

        // Bundle Capacity Reconciliation Lambda (scheduled, not API)
        var bundleCapacityReconcileLambdaHandlerName = "bundleCapacityReconcile.handler";
        var bundleCapacityReconcileLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(bundleCapacityReconcileLambdaHandlerName);
        this.bundleCapacityReconcileLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, bundleCapacityReconcileLambdaHandlerDashed);
        this.bundleCapacityReconcileLambdaHandler =
                "%s/account/%s".formatted(appLambdaHandlerPrefix, bundleCapacityReconcileLambdaHandlerName);
        this.bundleCapacityReconcileLambdaArn =
                "%s-%s".formatted(appLambdaArnPrefix, bundleCapacityReconcileLambdaHandlerDashed);
        this.bundleCapacityReconcileProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.bundleCapacityReconcileLambdaArn, this.provisionedConcurrencyAliasName);

        // Session Beacon POST Lambda (public, no auth)
        this.sessionBeaconPostLambdaHttpMethod = HttpMethod.POST;
        this.sessionBeaconPostLambdaUrlPath = "/api/session/beacon";
        this.sessionBeaconPostLambdaJwtAuthorizer = false;
        this.sessionBeaconPostLambdaCustomAuthorizer = false;
        var sessionBeaconPostLambdaHandlerName = "sessionBeaconPost.ingestHandler";
        var sessionBeaconPostLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(sessionBeaconPostLambdaHandlerName);
        this.sessionBeaconPostIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, sessionBeaconPostLambdaHandlerDashed);
        this.sessionBeaconPostIngestLambdaHandler =
                "%s/account/%s".formatted(appLambdaHandlerPrefix, sessionBeaconPostLambdaHandlerName);
        this.sessionBeaconPostIngestLambdaArn =
                "%s-%s".formatted(appLambdaArnPrefix, sessionBeaconPostLambdaHandlerDashed);
        this.sessionBeaconPostIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.sessionBeaconPostIngestLambdaArn, this.provisionedConcurrencyAliasName);
        publishedApiLambdas.add(new PublishedLambda(
                this.sessionBeaconPostLambdaHttpMethod,
                this.sessionBeaconPostLambdaUrlPath,
                "Session beacon",
                "Records a new browser session for activity monitoring",
                "sessionBeacon"));

        // Billing Checkout POST Lambda (JWT auth)
        this.billingCheckoutPostLambdaHttpMethod = HttpMethod.POST;
        this.billingCheckoutPostLambdaUrlPath = "/api/v1/billing/checkout-session";
        this.billingCheckoutPostLambdaJwtAuthorizer = true;
        this.billingCheckoutPostLambdaCustomAuthorizer = false;
        var billingCheckoutPostLambdaHandlerName = "billingCheckoutPost.ingestHandler";
        var billingCheckoutPostLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(billingCheckoutPostLambdaHandlerName);
        this.billingCheckoutPostIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, billingCheckoutPostLambdaHandlerDashed);
        this.billingCheckoutPostIngestLambdaHandler =
                "%s/billing/%s".formatted(appLambdaHandlerPrefix, billingCheckoutPostLambdaHandlerName);
        this.billingCheckoutPostIngestLambdaArn =
                "%s-%s".formatted(appLambdaArnPrefix, billingCheckoutPostLambdaHandlerDashed);
        this.billingCheckoutPostIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.billingCheckoutPostIngestLambdaArn, this.provisionedConcurrencyAliasName);
        publishedApiLambdas.add(new PublishedLambda(
                this.billingCheckoutPostLambdaHttpMethod,
                this.billingCheckoutPostLambdaUrlPath,
                "Create billing checkout session",
                "Creates a Stripe checkout session for subscription",
                "createCheckoutSession"));

        // Billing Portal GET Lambda (JWT auth)
        this.billingPortalGetLambdaHttpMethod = HttpMethod.GET;
        this.billingPortalGetLambdaUrlPath = "/api/v1/billing/portal";
        this.billingPortalGetLambdaJwtAuthorizer = true;
        this.billingPortalGetLambdaCustomAuthorizer = false;
        var billingPortalGetLambdaHandlerName = "billingPortalGet.ingestHandler";
        var billingPortalGetLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(billingPortalGetLambdaHandlerName);
        this.billingPortalGetIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, billingPortalGetLambdaHandlerDashed);
        this.billingPortalGetIngestLambdaHandler =
                "%s/billing/%s".formatted(appLambdaHandlerPrefix, billingPortalGetLambdaHandlerName);
        this.billingPortalGetIngestLambdaArn =
                "%s-%s".formatted(appLambdaArnPrefix, billingPortalGetLambdaHandlerDashed);
        this.billingPortalGetIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.billingPortalGetIngestLambdaArn, this.provisionedConcurrencyAliasName);
        publishedApiLambdas.add(new PublishedLambda(
                this.billingPortalGetLambdaHttpMethod,
                this.billingPortalGetLambdaUrlPath,
                "Get billing portal URL",
                "Creates a Stripe billing portal session URL",
                "getBillingPortal"));

        // Billing Recover POST Lambda (JWT auth)
        this.billingRecoverPostLambdaHttpMethod = HttpMethod.POST;
        this.billingRecoverPostLambdaUrlPath = "/api/v1/billing/recover";
        this.billingRecoverPostLambdaJwtAuthorizer = true;
        this.billingRecoverPostLambdaCustomAuthorizer = false;
        var billingRecoverPostLambdaHandlerName = "billingRecoverPost.ingestHandler";
        var billingRecoverPostLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(billingRecoverPostLambdaHandlerName);
        this.billingRecoverPostIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, billingRecoverPostLambdaHandlerDashed);
        this.billingRecoverPostIngestLambdaHandler =
                "%s/billing/%s".formatted(appLambdaHandlerPrefix, billingRecoverPostLambdaHandlerName);
        this.billingRecoverPostIngestLambdaArn =
                "%s-%s".formatted(appLambdaArnPrefix, billingRecoverPostLambdaHandlerDashed);
        this.billingRecoverPostIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.billingRecoverPostIngestLambdaArn, this.provisionedConcurrencyAliasName);
        publishedApiLambdas.add(new PublishedLambda(
                this.billingRecoverPostLambdaHttpMethod,
                this.billingRecoverPostLambdaUrlPath,
                "Recover billing subscription",
                "Recovers or reconciles a billing subscription",
                "recoverBilling"));

        // Billing Webhook POST Lambda (NO auth - Stripe signature verification)
        this.billingWebhookPostLambdaHttpMethod = HttpMethod.POST;
        this.billingWebhookPostLambdaUrlPath = "/api/v1/billing/webhook";
        this.billingWebhookPostLambdaJwtAuthorizer = false;
        this.billingWebhookPostLambdaCustomAuthorizer = false;
        var billingWebhookPostLambdaHandlerName = "billingWebhookPost.ingestHandler";
        var billingWebhookPostLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(billingWebhookPostLambdaHandlerName);
        this.billingWebhookPostIngestLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, billingWebhookPostLambdaHandlerDashed);
        this.billingWebhookPostIngestLambdaHandler =
                "%s/billing/%s".formatted(appLambdaHandlerPrefix, billingWebhookPostLambdaHandlerName);
        this.billingWebhookPostIngestLambdaArn =
                "%s-%s".formatted(appLambdaArnPrefix, billingWebhookPostLambdaHandlerDashed);
        this.billingWebhookPostIngestProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.billingWebhookPostIngestLambdaArn, this.provisionedConcurrencyAliasName);
        publishedApiLambdas.add(new PublishedLambda(
                this.billingWebhookPostLambdaHttpMethod,
                this.billingWebhookPostLambdaUrlPath,
                "Stripe webhook",
                "Receives Stripe webhook events for subscription lifecycle",
                "stripeWebhook"));

        // Telegram forwarder Lambda (EventBridge target, not API)
        var activityTelegramForwarderLambdaHandlerName = "activityTelegramForwarder.handler";
        var activityTelegramForwarderLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(activityTelegramForwarderLambdaHandlerName);
        this.activityTelegramForwarderLambdaFunctionName =
                "%s-%s".formatted(this.appResourceNamePrefix, activityTelegramForwarderLambdaHandlerDashed);
        this.activityTelegramForwarderLambdaHandler =
                "%s/ops/%s".formatted(appLambdaHandlerPrefix, activityTelegramForwarderLambdaHandlerName);
        this.activityTelegramForwarderLambdaArn =
                "%s-%s".formatted(appLambdaArnPrefix, activityTelegramForwarderLambdaHandlerDashed);
        this.activityTelegramForwarderProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.activityTelegramForwarderLambdaArn, this.provisionedConcurrencyAliasName);

        var appSelfDestructLambdaHandlerName = "selfDestruct.ingestHandler";
        var appSelfDestructLambdaHandlerDashed =
                ResourceNameUtils.convertCamelCaseToDashSeparated(appSelfDestructLambdaHandlerName);
        this.selfDestructLambdaFunctionName =
                "%s-app-%s".formatted(this.appResourceNamePrefix, appSelfDestructLambdaHandlerDashed);
        this.selfDestructLambdaHandler =
                "%s/infra/%s".formatted(appLambdaHandlerPrefix, appSelfDestructLambdaHandlerName);
        this.selfDestructLambdaArn = "%s-%s".formatted(appLambdaArnPrefix, appSelfDestructLambdaHandlerDashed);
        this.selfDestructProvisionedConcurrencyLambdaAliasArn =
                "%s:%s".formatted(this.selfDestructLambdaArn, this.provisionedConcurrencyAliasName);
    }
}
