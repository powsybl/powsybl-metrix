package com.powsybl.metrix.integration.metrix;

import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.integration.MetrixDslData;
import com.powsybl.metrix.integration.MetrixParameters;
import com.powsybl.metrix.integration.io.MetrixConfigResult;
import com.powsybl.metrix.mapping.MappingParameters;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;

import javax.annotation.Nullable;

public class MetrixAnalysisResult {
    @Nullable
    public final MetrixDslData metrixDslData;
    public final TimeSeriesMappingConfig mappingConfig;
    public final Network network;
    public final MetrixParameters metrixParameters;
    public final MappingParameters mappingParameters;
    public final MetrixConfigResult metrixConfigResult;

    public MetrixAnalysisResult(@Nullable MetrixDslData metrixDslData, TimeSeriesMappingConfig mappingConfig, Network network,
                          MetrixParameters metrixParameters, MappingParameters mappingParameters, MetrixConfigResult metrixConfigResult) {
        this.metrixDslData = metrixDslData;
        this.mappingConfig = mappingConfig;
        this.network = network;
        this.metrixParameters = metrixParameters;
        this.mappingParameters = mappingParameters;
        this.metrixConfigResult = metrixConfigResult;
    }
}
