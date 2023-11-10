package com.powsybl.metrix.integration;

import java.util.List;

public class PostProcessingPrefixContainer {
    public String postProcessingType;
    public String loadPrefix;
    public String overloadPrefix;
    public String overallOverloadPrefix;
    public String maxThreatPrefix;

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
