/*
 * SPDX-License-Identifier: LicenseRef-PolyForm-Internal-Use-1.0.0
 * Copyright (C) 2006-2026 DIY Accounting Limited
 */

package co.uk.diyaccounting.root;

public class SubmitSharedNames {

    public final String holdingDomainName;
    public final String distributionAccessLogGroupName;
    public final String distributionAccessLogDeliveryHoldingSourceName;
    public final String distributionAccessLogDeliveryHoldingDestinationName;

    public static class SubmitSharedNamesProps {
        public String hostedZoneName;
        public String envName;
    }

    public SubmitSharedNames(SubmitSharedNamesProps props) {
        var envResourceNamePrefix = "%s-env".formatted(props.envName);
        this.holdingDomainName = "prod".equals(props.envName)
                ? "holding.%s".formatted(props.hostedZoneName)
                : "%s-holding.%s".formatted(props.envName, props.hostedZoneName);
        this.distributionAccessLogGroupName = "distribution-%s-logs".formatted(envResourceNamePrefix);
        this.distributionAccessLogDeliveryHoldingSourceName =
                "%s-holding-dist-logs-src".formatted(envResourceNamePrefix);
        this.distributionAccessLogDeliveryHoldingDestinationName =
                "%s-holding-logs-dest".formatted(envResourceNamePrefix);
    }
}
