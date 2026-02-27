package com.powsybl.metrix.integration.postprocessing;

import com.powsybl.metrix.integration.MetrixDataName;
import com.powsybl.metrix.integration.MetrixDslData;
import com.powsybl.metrix.integration.MetrixVariable;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.DOCTRINE_COSTS_ARE_NOT_PROPERLY_CONFIGURED;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.checkAllConfigured;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.findIdsToProcess;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.getContingencyIdFromTsName;

public abstract class AbstractMetrixEquipmentPostProcessing {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMetrixEquipmentPostProcessing.class);

    protected final com.powsybl.metrix.integration.MetrixDslData dslData;
    protected final com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig mappingConfig;
    protected final Set<String> allTimeSeriesNames;
    protected final String nullableSchemaName;
    protected final PostProcessingEquipmentType equipmentType;

    protected final Map<String, NodeCalc> calculatedTimeSeries;

    // Map contingency id -> contingency probability
    protected final Map<String, NodeCalc> contingencyProbabilityById;

    // Map equipment -> preventive time series
    protected final Map<String, Set<String>> equipmentToPreventiveTs = new HashMap<>();

    // Map equipment -> curative time series
    protected final Map<String, Set<String>> equipmentToCurativeTs = new HashMap<>();

    protected final Map<String, NodeCalc> postProcessingTimeSeries = new HashMap<>();

    protected AbstractMetrixEquipmentPostProcessing(
        MetrixDslData dslData,
        TimeSeriesMappingConfig mappingConfig,
        Map<String, NodeCalc> contingencyProbabilityById,
        Set<String> allTimeSeriesNames,
        String nullableSchemaName,
        PostProcessingEquipmentType equipmentType) {

        this.dslData = dslData;
        this.mappingConfig = mappingConfig;
        this.allTimeSeriesNames = allTimeSeriesNames;
        this.nullableSchemaName = nullableSchemaName;
        this.equipmentType = equipmentType;
        this.calculatedTimeSeries = new HashMap<>(mappingConfig.getTimeSeriesNodes());
        this.contingencyProbabilityById = contingencyProbabilityById;
        initEquipmentToPreventiveTs();
        initEquipmentToCurativeTs();
    }

    private void initEquipmentToPreventiveTs() {
        String prefix = equipmentType.preventivePrefixContainer().getMetrixResultPrefix();

        // Filter equipment ids who have preventive time series results
        List<String> idsToProcess = findIdsToProcess(getPreventiveIds(), allTimeSeriesNames, prefix);

        for (String id : idsToProcess) {
            equipmentToPreventiveTs.computeIfAbsent(id, k -> new HashSet<>()).add(prefix + id);
        }
    }

    private void initEquipmentToCurativeTs() {
        String prefix = equipmentType.curativePrefixContainer().getMetrixResultPrefix();

        // Filter equipment ids who have curative time series results
        List<String> idsToProcess = findIdsToProcess(getCurativeIds(), allTimeSeriesNames, prefix, contingencyProbabilityById.keySet());

        for (String id : idsToProcess) {
            String elementIdPrefix = prefix + id;
            List<String> elementTimeSeriesNames = allTimeSeriesNames.stream().filter(s -> s.startsWith(elementIdPrefix)).toList();
            elementTimeSeriesNames.forEach(tsName -> equipmentToCurativeTs.computeIfAbsent(id, k -> new HashSet<>()).add(tsName));
        }
    }

    public final Map<String, NodeCalc> createPostProcessingTimeSeries() {
        // Preventive
        PostProcessingPrefixContainer preventivePrefixContainer = equipmentType.preventivePrefixContainer();
        process(equipmentToPreventiveTs.keySet(), PostProcessingType.PREVENTIVE, preventivePrefixContainer);

        // Curative
        PostProcessingPrefixContainer curativePrefixContainer = equipmentType.curativePrefixContainer();
        process(equipmentToCurativeTs.keySet(), PostProcessingType.CURATIVE, curativePrefixContainer);

        return postProcessingTimeSeries;
    }

    private NodeCalc getContingencyProbability(String contingencyId) {
        NodeCalc probability = contingencyProbabilityById.get(contingencyId);
        if (probability == null) {
            throw new IllegalStateException("Unknown contingency id: " + contingencyId);
        }
        return probability;
    }

    private ContingencyContext createContingencyContext(PostProcessingType postProcessingType, String tsName, String elementIdPrefix) {
        // Preventive
        if (postProcessingType == PostProcessingType.PREVENTIVE) {
            return new ContingencyPreventiveContext();
        }

        // Curative
        String contingencyId = getContingencyIdFromTsName(tsName, elementIdPrefix + "_");
        NodeCalc probability = getContingencyProbability(contingencyId);
        return new ContingencyCurativeContext(contingencyId, probability);
    }

    private void computeEquipment(PostProcessingPrefixContainer prefixContainer,
                                  String equipmentId,
                                  List<String> elementTimeSeriesNames,
                                  String elementIdPrefix,
                                  PostProcessingType postProcessingType,
                                  boolean allCostsConfigured) {

        for (String tsName : elementTimeSeriesNames) {
            ContingencyContext contingencyContext = createContingencyContext(postProcessingType, tsName, elementIdPrefix);
            compute(equipmentId, contingencyContext, new TimeSeriesNameNodeCalc(tsName), prefixContainer, allCostsConfigured);
        }
    }

    /**
     * Check if doctrine costs are properly defined, i.e., all equipments are configured for up and down costs.
     * If not, no costs time series will be created.
     * For each equipment having result (equipment time series) (MW)
     * - create time series (MW)
     * - create costs time series
     * - create global cost time series
     *
     * @param equipmentIds       list of equipment ids having results (redispatching for generators, load shedding for loads)
     * @param postProcessingType postprocessing type
     * @param prefixContainer    prefix of time series to create (preventive or curative)
     */
    private void process(Set<String> equipmentIds, PostProcessingType postProcessingType, PostProcessingPrefixContainer prefixContainer) {

        if (equipmentIds == null || equipmentIds.isEmpty()) {
            return;
        }

        boolean allCostsConfigured = checkAllConfigured(equipmentIds, getRequiredVariables(prefixContainer), mappingConfig.getEquipmentToTimeSeries());
        if (!allCostsConfigured) {
            LOGGER.warn(DOCTRINE_COSTS_ARE_NOT_PROPERLY_CONFIGURED, equipmentType.name());
        }

        for (String equipmentId : equipmentIds) {
            List<String> elementTimeSeriesNames = switch (postProcessingType) {
                case PREVENTIVE -> equipmentToPreventiveTs.get(equipmentId).stream().toList();
                case CURATIVE -> equipmentToCurativeTs.get(equipmentId).stream().toList();
            };
            computeEquipment(prefixContainer, equipmentId, elementTimeSeriesNames, prefixContainer.getMetrixResultPrefix() + equipmentId, postProcessingType, allCostsConfigured);
        }
    }

    protected String buildName(ContingencyContext contingencyContext,
                               String prefix,
                               String equipmentId) {
        String baseName = MetrixDataName.getNameWithSchema(prefix + "_" + equipmentId, nullableSchemaName);
        return baseName + contingencyContext.postfix();
    }

    protected abstract void compute(String equipmentId,
                                    ContingencyContext context,
                                    NodeCalc metrixResultTimeSeries,
                                    PostProcessingPrefixContainer prefixContainer,
                                    boolean allCostsConfigured);

    protected abstract Set<String> getPreventiveIds();

    protected abstract Set<String> getCurativeIds();

    protected abstract List<MetrixVariable> getRequiredVariables(PostProcessingPrefixContainer prefixContainer);
}
