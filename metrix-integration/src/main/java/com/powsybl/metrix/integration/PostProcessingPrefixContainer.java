package com.powsybl.metrix.integration;

import java.util.List;

public class PostProcessingPrefixContainer {
    public final String postProcessingType;
    public final String loadPrefix;
    public final String overloadPrefix;
    public final String overallOverloadPrefix;
    public final String maxThreatPrefix;

    public PostProcessingPrefixContainer(String postProcessingType, String loadPrefix, String overloadPrefix, String overallOverloadPrefix, String maxThreatPrefix) {
        this.postProcessingType = postProcessingType;
        this.loadPrefix = loadPrefix;
        this.overloadPrefix = overloadPrefix;
        this.overallOverloadPrefix = overallOverloadPrefix;
        this.maxThreatPrefix = maxThreatPrefix;
    }

    public List<String> postProcessingPrefixList() {
        return List.of(loadPrefix, overloadPrefix, overallOverloadPrefix);
    }
}
