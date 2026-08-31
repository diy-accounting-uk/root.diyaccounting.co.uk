/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright (C) 2025-2026 DIY Accounting Ltd
 */

package co.uk.diyaccounting.root.stacks;

import static co.uk.diyaccounting.root.utils.Kind.infof;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Tags;
import software.amazon.awscdk.services.bcmdataexports.CfnExport;
import software.amazon.awscdk.services.budgets.CfnBudget;
import software.amazon.awscdk.services.ce.CfnAnomalyMonitor;
import software.amazon.awscdk.services.ce.CfnAnomalySubscription;
import software.amazon.awscdk.services.ce.CfnCostCategory;
import software.constructs.Construct;

/**
 * Org-wide billing control-plane resources: the CUR 2.0 Data Export, seven budgets, one cost
 * anomaly monitor and subscription, and the Workload cost category. All live in the management
 * account, us-east-1 — the only region BCM Data Exports, Budgets and Cost Explorer serve
 * CloudFormation resource requests from, regardless of where the data itself is stored.
 *
 * <p>The export's destination bucket, Glue database and Athena workgroup live in {@link
 * CostReportingStack} (eu-west-2) instead. They are joined only by the bucket name, passed in as
 * a plain string, so this stack never needs a cross-region CDK reference to the other one.
 */
public class CostReportingUE1Stack extends Stack {

    public final CfnExport export;
    public final CfnCostCategory costCategory;
    public final List<CfnBudget> budgets;
    public final CfnAnomalyMonitor anomalyMonitor;
    public final CfnAnomalySubscription anomalySubscription;

    /**
     * BCM Data Exports rejects {@code SELECT *} ("ValidationException: SELECT * is not
     * supported"), so every column has to be named. This is the full COST_AND_USAGE_REPORT
     * schema for the table configuration below (TIME_GRANULARITY=DAILY, INCLUDE_RESOURCES=TRUE,
     * INCLUDE_MANUAL_DISCOUNT_COMPATIBILITY=FALSE, INCLUDE_SPLIT_COST_ALLOCATION_DATA=FALSE), read
     * from {@code aws bcm-data-exports get-table --table-name COST_AND_USAGE_REPORT
     * --table-properties '{"TIME_GRANULARITY":"DAILY","INCLUDE_RESOURCES":"TRUE",
     * "INCLUDE_MANUAL_DISCOUNT_COMPATIBILITY":"FALSE","INCLUDE_SPLIT_COST_ALLOCATION_DATA":"FALSE"}'}.
     */
    private static final List<String> COST_AND_USAGE_REPORT_COLUMNS = List.of(
            "identity_line_item_id",
            "identity_time_interval",
            "bill_invoice_id",
            "bill_invoicing_entity",
            "bill_billing_entity",
            "bill_bill_type",
            "bill_payer_account_id",
            "bill_payer_account_name",
            "bill_billing_period_start_date",
            "bill_billing_period_end_date",
            "line_item_usage_account_id",
            "line_item_usage_account_name",
            "line_item_line_item_type",
            "line_item_usage_start_date",
            "line_item_usage_end_date",
            "line_item_product_code",
            "line_item_usage_type",
            "line_item_operation",
            "line_item_availability_zone",
            "line_item_resource_id",
            "line_item_usage_amount",
            "line_item_normalization_factor",
            "line_item_normalized_usage_amount",
            "line_item_currency_code",
            "line_item_unblended_rate",
            "line_item_unblended_cost",
            "line_item_user_identifier",
            "line_item_blended_rate",
            "line_item_blended_cost",
            "line_item_line_item_description",
            "line_item_tax_type",
            "line_item_net_unblended_rate",
            "line_item_net_unblended_cost",
            "line_item_legal_entity",
            "product_servicecode",
            "product_operation",
            "product_usagetype",
            "product_sku",
            "product_product_family",
            "product_comment",
            "product_fee_code",
            "product_fee_description",
            "product_location",
            "product_location_type",
            "product_region_code",
            "product_from_location",
            "product_from_location_type",
            "product_from_region_code",
            "product_to_location",
            "product_to_location_type",
            "product_to_region_code",
            "product_instance_family",
            "product_instance_type",
            "product_instancesku",
            "product_pricing_unit",
            "product",
            "pricing_lease_contract_length",
            "pricing_offering_class",
            "pricing_purchase_option",
            "pricing_rate_code",
            "pricing_rate_id",
            "pricing_currency",
            "pricing_public_on_demand_rate",
            "pricing_public_on_demand_cost",
            "pricing_term",
            "pricing_unit",
            "reservation_availability_zone",
            "reservation_start_time",
            "reservation_end_time",
            "reservation_modification_status",
            "reservation_amortized_upfront_cost_for_usage",
            "reservation_amortized_upfront_fee_for_billing_period",
            "reservation_unused_amortized_upfront_fee_for_billing_period",
            "reservation_recurring_fee_for_usage",
            "reservation_unused_recurring_fee",
            "reservation_effective_cost",
            "reservation_upfront_value",
            "reservation_net_amortized_upfront_cost_for_usage",
            "reservation_net_amortized_upfront_fee_for_billing_period",
            "reservation_net_unused_amortized_upfront_fee_for_billing_period",
            "reservation_net_recurring_fee_for_usage",
            "reservation_net_unused_recurring_fee",
            "reservation_net_effective_cost",
            "reservation_net_upfront_value",
            "reservation_normalized_units_per_reservation",
            "reservation_number_of_reservations",
            "reservation_reservation_a_r_n",
            "reservation_subscription_id",
            "reservation_total_reserved_normalized_units",
            "reservation_total_reserved_units",
            "reservation_units_per_reservation",
            "reservation_unused_normalized_unit_quantity",
            "reservation_unused_quantity",
            "savings_plan_total_commitment_to_date",
            "savings_plan_savings_plan_a_r_n",
            "savings_plan_savings_plan_rate",
            "savings_plan_used_commitment",
            "savings_plan_savings_plan_effective_cost",
            "savings_plan_amortized_upfront_commitment_for_billing_period",
            "savings_plan_recurring_commitment_for_billing_period",
            "savings_plan_net_savings_plan_effective_cost",
            "savings_plan_net_amortized_upfront_commitment_for_billing_period",
            "savings_plan_net_recurring_commitment_for_billing_period",
            "savings_plan_start_time",
            "savings_plan_end_time",
            "savings_plan_instance_type_family",
            "savings_plan_offering_type",
            "savings_plan_payment_option",
            "savings_plan_purchase_term",
            "savings_plan_region",
            "discount_bundled_discount",
            "discount_total_discount",
            "discount",
            "resource_tags",
            "tags",
            "cost_category");

    /** One of the six linked accounts, its Workload cost-category name and its monthly budget. */
    public record LinkedAccountBudget(String accountId, String workloadName, double monthlyLimitUsd) {}

    @Value.Immutable
    public interface CostReportingUE1StackProps extends StackProps {
        @Override
        Environment getEnv();

        String exportBucketName();

        String exportBucketRegion();

        List<LinkedAccountBudget> linkedAccounts();

        double organisationBudgetLimitUsd();

        String alertEmail();

        static ImmutableCostReportingUE1StackProps.Builder builder() {
            return ImmutableCostReportingUE1StackProps.builder();
        }
    }

    public CostReportingUE1Stack(final Construct scope, final String id, final CostReportingUE1StackProps props) {
        super(scope, id, StackProps.builder().env(props.getEnv()).build());

        Tags.of(this).add("Application", "@diy-accounting-uk/root.diyaccounting.co.uk/cost-reporting");
        Tags.of(this).add("CostCenter", "@diy-accounting-uk/root.diyaccounting.co.uk");
        Tags.of(this).add("Owner", "@diy-accounting-uk/root.diyaccounting.co.uk");
        Tags.of(this).add("Stack", "CostReportingUE1Stack");
        Tags.of(this).add("ManagedBy", "aws-cdk");
        Tags.of(this).add("BillingPurpose", "cost-instrumentation");

        this.export = CfnExport.Builder.create(this, "Export")
                .export(CfnExport.ExportProperty.builder()
                        .name("diy-accounting-cur2")
                        .description("Org-wide CUR 2.0 for all linked accounts")
                        .dataQuery(CfnExport.DataQueryProperty.builder()
                                .queryStatement("SELECT " + String.join(", ", COST_AND_USAGE_REPORT_COLUMNS)
                                        + " FROM COST_AND_USAGE_REPORT")
                                .tableConfigurations(Map.of(
                                        "COST_AND_USAGE_REPORT",
                                        Map.of(
                                                "TIME_GRANULARITY", "DAILY",
                                                "INCLUDE_RESOURCES", "TRUE",
                                                "INCLUDE_MANUAL_DISCOUNT_COMPATIBILITY", "FALSE",
                                                "INCLUDE_SPLIT_COST_ALLOCATION_DATA", "FALSE")))
                                .build())
                        .destinationConfigurations(CfnExport.DestinationConfigurationsProperty.builder()
                                .s3Destination(CfnExport.S3DestinationProperty.builder()
                                        .s3Bucket(props.exportBucketName())
                                        .s3Prefix("cur2")
                                        .s3Region(props.exportBucketRegion())
                                        .s3OutputConfigurations(CfnExport.S3OutputConfigurationsProperty.builder()
                                                .outputType("CUSTOM")
                                                .format("PARQUET")
                                                .compression("PARQUET")
                                                .overwrite("OVERWRITE_REPORT")
                                                .build())
                                        .build())
                                .build())
                        .refreshCadence(CfnExport.RefreshCadenceProperty.builder()
                                .frequency("SYNCHRONOUS")
                                .build())
                        .build())
                .build();
        infof(
                "Created CUR 2.0 export targeting s3://%s/cur2 in %s",
                props.exportBucketName(), props.exportBucketRegion());

        this.costCategory = CfnCostCategory.Builder.create(this, "WorkloadCostCategory")
                .name("Workload")
                .ruleVersion("CostCategoryExpression.v1")
                .defaultValue("Shared")
                .rules(buildWorkloadRules(props.linkedAccounts()))
                .build();

        this.budgets = new ArrayList<>();
        for (var account : props.linkedAccounts()) {
            this.budgets.add(createBudget(
                    account.workloadName() + "-Budget",
                    account.workloadName() + "-monthly-budget",
                    account.monthlyLimitUsd(),
                    Map.of("LinkedAccount", List.of(account.accountId())),
                    props.alertEmail()));
        }
        this.budgets.add(createBudget(
                "OrganisationBudget",
                "organisation-monthly-budget",
                props.organisationBudgetLimitUsd(),
                null,
                props.alertEmail()));

        this.anomalyMonitor = CfnAnomalyMonitor.Builder.create(this, "AnomalyMonitor")
                .monitorName("org-service-spend")
                .monitorType("DIMENSIONAL")
                .monitorDimension("SERVICE")
                .build();

        this.anomalySubscription = CfnAnomalySubscription.Builder.create(this, "AnomalySubscription")
                .subscriptionName("org-daily-anomalies")
                .frequency("DAILY")
                .monitorArnList(List.of(this.anomalyMonitor.getAttrMonitorArn()))
                .subscribers(List.of(CfnAnomalySubscription.SubscriberProperty.builder()
                        .type("EMAIL")
                        .address(props.alertEmail())
                        .build()))
                .thresholdExpression(
                        """
                        {"Dimensions":{"Key":"ANOMALY_TOTAL_IMPACT_ABSOLUTE","MatchOptions":["GREATER_THAN_OR_EQUAL"],"Values":["15"]}}\
                        """)
                .build();

        infof("CostReportingUE1Stack %s created", this.getNode().getId());
    }

    /** One rule per linked account, mapping its account ID to its Workload category name. */
    private static String buildWorkloadRules(List<LinkedAccountBudget> linkedAccounts) {
        StringBuilder rules = new StringBuilder("[");
        for (int i = 0; i < linkedAccounts.size(); i++) {
            var account = linkedAccounts.get(i);
            if (i > 0) rules.append(",");
            rules.append(
                    """
                    {"Value":"%s","Rule":{"Dimensions":{"Key":"LINKED_ACCOUNT","Values":["%s"]}},"Type":"REGULAR"}\
                    """
                            .formatted(account.workloadName(), account.accountId()));
        }
        rules.append("]");
        return rules.toString();
    }

    /**
     * A monthly COST budget alerting at 85% of actual spend and again when the forecast reaches
     * 100%, both by email. costFilters is null for the org-wide budget, which carries no filter.
     */
    private CfnBudget createBudget(
            String constructId,
            String budgetName,
            double monthlyLimitUsd,
            Map<String, List<String>> costFilters,
            String alertEmail) {
        var subscribers = List.of(CfnBudget.SubscriberProperty.builder()
                .subscriptionType("EMAIL")
                .address(alertEmail)
                .build());

        var builder = CfnBudget.Builder.create(this, constructId)
                .budget(CfnBudget.BudgetDataProperty.builder()
                        .budgetName(budgetName)
                        .budgetType("COST")
                        .timeUnit("MONTHLY")
                        .budgetLimit(CfnBudget.SpendProperty.builder()
                                .amount(monthlyLimitUsd)
                                .unit("USD")
                                .build())
                        .costFilters(costFilters)
                        .build())
                .notificationsWithSubscribers(List.of(
                        CfnBudget.NotificationWithSubscribersProperty.builder()
                                .notification(CfnBudget.NotificationProperty.builder()
                                        .notificationType("ACTUAL")
                                        .comparisonOperator("GREATER_THAN")
                                        .threshold(85)
                                        .thresholdType("PERCENTAGE")
                                        .build())
                                .subscribers(subscribers)
                                .build(),
                        CfnBudget.NotificationWithSubscribersProperty.builder()
                                .notification(CfnBudget.NotificationProperty.builder()
                                        .notificationType("FORECASTED")
                                        .comparisonOperator("GREATER_THAN")
                                        .threshold(100)
                                        .thresholdType("PERCENTAGE")
                                        .build())
                                .subscribers(subscribers)
                                .build()));
        return builder.build();
    }
}
