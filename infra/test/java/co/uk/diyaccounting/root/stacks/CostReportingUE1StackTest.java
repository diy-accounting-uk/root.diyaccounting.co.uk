/*
 * SPDX-License-Identifier: AGPL-3.0-only
 * Copyright (C) 2025-2026 DIY Accounting Ltd
 */

package co.uk.diyaccounting.root.stacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.uk.diyaccounting.root.stacks.CostReportingUE1Stack.LinkedAccountBudget;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.assertions.Match;
import software.amazon.awscdk.assertions.Template;

class CostReportingUE1StackTest {

    private static final List<LinkedAccountBudget> LINKED_ACCOUNTS = List.of(
            new LinkedAccountBudget("887764105431", "management", 25),
            new LinkedAccountBudget("283165661847", "gateway", 5),
            new LinkedAccountBudget("064390746177", "spreadsheets", 15),
            new LinkedAccountBudget("367191799875", "submit-ci", 30),
            new LinkedAccountBudget("972912397388", "submit-prod", 120),
            new LinkedAccountBudget("914216784828", "submit-backup", 5));

    private static CostReportingUE1Stack synthCostReportingUE1Stack() {
        App app = new App();
        return new CostReportingUE1Stack(
                app,
                "TestCostReportingUE1Stack",
                CostReportingUE1Stack.CostReportingUE1StackProps.builder()
                        .env(Environment.builder()
                                .account("887764105431")
                                .region("us-east-1")
                                .build())
                        .exportBucketName("diy-accounting-cost-reports-887764105431")
                        .exportBucketRegion("eu-west-2")
                        .linkedAccounts(LINKED_ACCOUNTS)
                        .organisationBudgetLimitUsd(200)
                        .alertEmail("admin@diyaccounting.co.uk")
                        .build());
    }

    @Test
    void exportTargetsTheEuWest2BucketAsParquetWithResourceIds() {
        Template template = Template.fromStack(synthCostReportingUE1Stack());

        var tableConfig = Match.objectLike(Map.of("TIME_GRANULARITY", "DAILY", "INCLUDE_RESOURCES", "TRUE"));
        var dataQuery = Match.objectLike(
                Map.of("TableConfigurations", Match.objectLike(Map.of("COST_AND_USAGE_REPORT", tableConfig))));
        var s3OutputConfig = Match.objectLike(Map.of("Format", "PARQUET", "Overwrite", "OVERWRITE_REPORT"));
        var s3Destination = Match.objectLike(Map.of(
                "S3Bucket", "diy-accounting-cost-reports-887764105431",
                "S3Region", "eu-west-2",
                "S3OutputConfigurations", s3OutputConfig));
        var destinationConfigurations = Match.objectLike(Map.of("S3Destination", s3Destination));
        var exportProperty = Match.objectLike(
                Map.of("DataQuery", dataQuery, "DestinationConfigurations", destinationConfigurations));

        template.hasResourceProperties(
                "AWS::BCMDataExports::Export", Match.objectLike(Map.of("Export", exportProperty)));
    }

    @Test
    void costCategoryMapsEveryLinkedAccountAndDefaultsUnmatchedSpendToShared() {
        Template template = Template.fromStack(synthCostReportingUE1Stack());

        var costCategories =
                template.findResources("AWS::CE::CostCategory", Map.of("Properties", Map.of("Name", "Workload")));
        assertEquals(1, costCategories.size());

        @SuppressWarnings("unchecked")
        var properties =
                (Map<String, Object>) costCategories.values().iterator().next().get("Properties");
        assertEquals("Shared", properties.get("DefaultValue"));
        String rules = String.valueOf(properties.get("Rules"));
        for (var account : LINKED_ACCOUNTS) {
            assertTrue(rules.contains(account.accountId()), "rules missing account " + account.accountId());
            assertTrue(rules.contains(account.workloadName()), "rules missing name " + account.workloadName());
        }
    }

    @Test
    void sevenBudgetsCoverSixLinkedAccountsPlusTheOrganisationTotal() {
        Template template = Template.fromStack(synthCostReportingUE1Stack());

        assertEquals(7, template.findResources("AWS::Budgets::Budget").size());

        for (var account : LINKED_ACCOUNTS) {
            var budgetLimit = Match.objectLike(Map.of("Amount", account.monthlyLimitUsd(), "Unit", "USD"));
            var costFilters = Match.objectLike(Map.of("LinkedAccount", List.of(account.accountId())));
            var budget = Match.objectLike(Map.of("BudgetLimit", budgetLimit, "CostFilters", costFilters));
            template.hasResourceProperties("AWS::Budgets::Budget", Match.objectLike(Map.of("Budget", budget)));
        }
    }

    @Test
    void everyBudgetAlertsAt85PercentActualAnd100PercentForecast() {
        Template template = Template.fromStack(synthCostReportingUE1Stack());

        var emailSubscriber =
                Match.objectLike(Map.of("SubscriptionType", "EMAIL", "Address", "admin@diyaccounting.co.uk"));
        var actualAt85Percent = Match.objectLike(Map.of(
                "Notification",
                Match.objectLike(Map.of("NotificationType", "ACTUAL", "Threshold", 85, "ThresholdType", "PERCENTAGE")),
                "Subscribers",
                Match.arrayWith(List.of(emailSubscriber))));
        var forecastedAt100Percent = Match.objectLike(Map.of(
                "Notification",
                Match.objectLike(
                        Map.of("NotificationType", "FORECASTED", "Threshold", 100, "ThresholdType", "PERCENTAGE"))));
        var expectedNotifications = List.of(actualAt85Percent, forecastedAt100Percent);

        template.hasResourceProperties(
                "AWS::Budgets::Budget",
                Match.objectLike(Map.of("NotificationsWithSubscribers", Match.arrayWith(expectedNotifications))));
    }

    @Test
    void organisationBudgetCarriesNoLinkedAccountFilter() {
        Template template = Template.fromStack(synthCostReportingUE1Stack());

        var orgBudgets = template.findResources(
                "AWS::Budgets::Budget",
                Map.of("Properties", Map.of("Budget", Map.of("BudgetLimit", Map.of("Amount", 200, "Unit", "USD")))));
        assertEquals(1, orgBudgets.size());

        @SuppressWarnings("unchecked")
        var budget = (Map<String, Object>)
                ((Map<String, Object>) orgBudgets.values().iterator().next().get("Properties")).get("Budget");
        assertTrue(!budget.containsKey("CostFilters"), "organisation budget should carry no cost filter");
    }

    @Test
    void anomalyMonitorAndSubscriptionMatchTheDesign() {
        Template template = Template.fromStack(synthCostReportingUE1Stack());

        template.hasResourceProperties(
                "AWS::CE::AnomalyMonitor",
                Match.objectLike(Map.of("MonitorType", "DIMENSIONAL", "MonitorDimension", "SERVICE")));

        template.hasResourceProperties(
                "AWS::CE::AnomalySubscription",
                Match.objectLike(Map.of(
                        "Frequency", "DAILY",
                        "Subscribers",
                                Match.arrayWith(List.of(Match.objectLike(
                                        Map.of("Type", "EMAIL", "Address", "admin@diyaccounting.co.uk")))),
                        "ThresholdExpression",
                                Match.stringLikeRegexp(
                                        "(?=.*ANOMALY_TOTAL_IMPACT_ABSOLUTE)(?=.*GREATER_THAN_OR_EQUAL)(?=.*15).*"))));
    }
}
