package com.powsybl.metrix.integration.postprocessing;

public enum PostProcessingEquipmentType {
    GENERATOR {
        @Override
        public PostProcessingPrefixContainer preventivePrefixContainer() {
            return MetrixGeneratorPostProcessingTimeSeries.PREVENTIVE_PREFIX_CONTAINER;
        }

        @Override
        public PostProcessingPrefixContainer curativePrefixContainer() {
            return MetrixGeneratorPostProcessingTimeSeries.CURATIVE_PREFIX_CONTAINER;
        }
    },

    LOAD {
        @Override
        public PostProcessingPrefixContainer preventivePrefixContainer() {
            return MetrixLoadPostProcessingTimeSeries.PREVENTIVE_PREFIX_CONTAINER;
        }

        @Override
        public PostProcessingPrefixContainer curativePrefixContainer() {
            return MetrixLoadPostProcessingTimeSeries.CURATIVE_PREFIX_CONTAINER;
        }
    };

    public abstract PostProcessingPrefixContainer preventivePrefixContainer();

    public abstract PostProcessingPrefixContainer curativePrefixContainer();
}
