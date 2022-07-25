/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration.dataGenerator;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.metrix.integration.*;
import com.powsybl.metrix.integration.io.MetrixDie;
import com.powsybl.metrix.mapping.TimeSeriesMapper;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MetrixInputData {

    private static final float UNDEFINED_VALUE = 99999f;
    private static final float CQADMITA_SWITCH_VAL = 1.e-5f;
    private static final float CQRESIST_SWITCH_VAL = 0f;

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixInputData.class);

    private final MetrixNetwork metrixNetwork;

    private final MetrixParameters parameters;

    private final MetrixDslData dslData;

    private final Map<String, Integer> ctyIndex = new HashMap<>();

    public enum ElementType {
        BRANCH(1),
        GENERATOR(2),
        HVDC(3);

        private final int type;

        ElementType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }

    public enum MonitoringType {
        NO(0),
        MONITORING(1),
        RESULT(2);

        private final int type;

        MonitoringType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }

    enum GeneratorAdjustmentMode {
        NONE(0),
        ADEQUACY_AND_REDISPATCHING(1),
        ADEQUACY_ONLY(2),
        REDISPATCHING_ONLY(3);

        private final int type;

        GeneratorAdjustmentMode(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }

    public MetrixInputData(MetrixNetwork metrixNetwork, MetrixDslData metrixDslData, MetrixParameters parameters) {
        this.metrixNetwork = Objects.requireNonNull(metrixNetwork);
        this.parameters = Objects.requireNonNull(parameters);
        this.dslData = metrixDslData;
        createMetrixInputs();
    }

    private int cgnbregi = 0;
    private int ecnbcons = 0;
    private int trnbgrou = 0;
    private int cqnbquad = 0;
    private int dtnbtrde = 0;
    private int dtnblrtd = 0;
    private int dtnburtd = 0;
    private int dcnblies = 0;
    private int dcnbdroo = 0;
    private int tnnbntot = 0;

    private int sectnbse = 0;

    private double sumPmax = 0.d;

    private static double toAdmittance(String id, final double x, final double uNom, final double nominalU) {
        double admittance = x * nominalU * nominalU / (uNom * uNom);
        if (admittance == 0) {
            admittance = CQADMITA_SWITCH_VAL;
            LOGGER.debug(String.format("x = 0 for branch <%s> -> replaced by x = %f", id, CQADMITA_SWITCH_VAL));
        }
        return admittance;
    }

    private int getMonitoringTypeBasecase(String id) {
        if (dslData != null) {
            return dslData.getBranchMonitoringN(id).getType();
        }
        return MonitoringType.NO.getType();
    }

    private int getMonitoringTypeOnContingency(String id) {
        if (dslData != null) {
            return dslData.getBranchMonitoringNk(id).getType();
        }
        return MonitoringType.NO.getType();
    }

    private MetrixHvdcRegulationType getRegulationType(HvdcLine line, boolean emulation) {

        MetrixHvdcControlType controlType = dslData == null ? MetrixHvdcControlType.FIXED : dslData.getHvdcControl(line.getId());

        MetrixHvdcRegulationType regulationType;
        if (controlType == MetrixHvdcControlType.FIXED) {
            regulationType = emulation ? MetrixHvdcRegulationType.FIXED_AC_EMULATION : MetrixHvdcRegulationType.FIXED_SETPOINT;
        } else {
            regulationType = emulation ? MetrixHvdcRegulationType.OPTIMIZED_AC_EMULATION : MetrixHvdcRegulationType.OPTIMIZED_SETPOINT;
        }

        return regulationType;
    }

    private GeneratorAdjustmentMode getGeneratorAdjustmentMode(String generatorId) {
        if (dslData == null) {
            return GeneratorAdjustmentMode.ADEQUACY_AND_REDISPATCHING;
        } else {
            Set<String> generatorsForAdequacy = dslData.getGeneratorsForAdequacy();
            Set<String> generatorsForRedispatching = dslData.getGeneratorsForRedispatching();

            if (generatorsForAdequacy.isEmpty() && generatorsForRedispatching.isEmpty()) {
                return GeneratorAdjustmentMode.ADEQUACY_AND_REDISPATCHING;
            } else {
                boolean adequacyMode = generatorsForAdequacy.contains(generatorId);
                boolean redispatchingMode = generatorsForRedispatching.contains(generatorId);
                if (adequacyMode && redispatchingMode) {
                    return GeneratorAdjustmentMode.ADEQUACY_AND_REDISPATCHING;
                } else if (adequacyMode) {
                    return GeneratorAdjustmentMode.ADEQUACY_ONLY;
                } else if (redispatchingMode) {
                    return GeneratorAdjustmentMode.REDISPATCHING_ONLY;
                } else {
                    return GeneratorAdjustmentMode.NONE;
                }
            }
        }
    }

    private void createMetrixInputs() {

        cgnbregi = metrixNetwork.getCountryList().size();

        trnbgrou = metrixNetwork.getGeneratorList().size();

        cqnbquad = metrixNetwork.getLineList().size() + metrixNetwork.getTwoWindingsTransformerList().size() + 3 * metrixNetwork.getThreeWindingsTransformerList().size() + metrixNetwork.getDanglingLineList().size() + metrixNetwork.getSwitchList().size();
        dtnbtrde = metrixNetwork.getPhaseTapChangerList().size();

        ecnbcons = metrixNetwork.getLoadList().size() + metrixNetwork.getDanglingLineList().size();

        dcnblies = metrixNetwork.getHvdcLineList().size();

        tnnbntot = metrixNetwork.getBusList().size();

        if (dslData != null) {
            sectnbse = dslData.getSectionList().size();
            dtnblrtd = dslData.getPtcLowerTapChangeList().size();
            dtnburtd = dslData.getPtcUpperTapChangeList().size();
        }
    }

    private void writeGeneral(MetrixDie die) {

        die.setInt("CGNBREGI", cgnbregi);
        die.setInt("TNNBNTOT", tnnbntot);
        die.setInt("CQNBQUAD", cqnbquad);
        die.setInt("ECNBCONS", ecnbcons);
        die.setInt("TRNBGROU", trnbgrou);
        die.setInt("DTNBTRDE", dtnbtrde);
        die.setInt("DCNBLIES", dcnblies);
        die.setInt("SECTNBSE", sectnbse);
    }

    private void writeOptions(MetrixDie die) {
        die.setInt("MODECALC", parameters.getComputationType().getType());
        die.setFloat("CGCPERTE", parameters.getLossFactor());
        die.setInt("UNOMINAL", parameters.getNominalU());
        parameters.isWithGridCost().ifPresent(value -> die.setBoolean("TRUTHBAR", value));
        parameters.isPreCurativeResults().ifPresent(value -> die.setBoolean("TESTITAM", value));
        if (parameters.isOutagesBreakingConnexity().orElse(parameters.isPropagateBranchTripping())) {
            die.setBoolean("INCNOCON", true);
        }
        parameters.isRemedialActionsBreakingConnexity().ifPresent(value -> die.setBoolean("PARNOCON", value));
        parameters.isAnalogousRemedialActionDetection().ifPresent(value -> die.setBoolean("PAREQUIV", value));
        parameters.isWithAdequacyResults().ifPresent(value -> die.setBoolean("EQUILRES", value));
        parameters.isWithRedispatchingResults().ifPresent(value -> die.setBoolean("REDISRES", value));
        if (parameters.isMarginalVariationsOnBranches()
                .orElse(!Objects.isNull(dslData) && !dslData.getContingencyDetailedMarginalVariationsList().isEmpty())) {
            die.setBoolean("VARMARES", true);
        }
        parameters.isMarginalVariationsOnHvdc().ifPresent(value -> die.setBoolean("LCCVMRES", value));
        parameters.isLossDetailPerCountry().ifPresent(value -> die.setBoolean("LOSSDETA", value));
        parameters.isOverloadResultsOnly().ifPresent(value -> die.setBoolean("OVRLDRES", value));
        parameters.getOptionalLossNbRelaunch().ifPresent(value -> die.setInt("RELPERTE", value));
        parameters.getOptionalLossThreshold().ifPresent(value -> die.setInt("SEUILPER", value));
        parameters.getOptionalPstCostPenality().ifPresent(value -> die.setFloat("TDPENALI", value));
        parameters.getOptionalHvdcCostPenality().ifPresent(value -> die.setFloat("HVDCPENA", value));
        parameters.getOptionalLossOfLoadCost().ifPresent(value -> die.setFloat("COUTDEFA", value));
        parameters.getOptionalCurativeLossOfLoadCost().ifPresent(value -> die.setFloat("COUENDCU", value));
        parameters.getOptionalCurativeLossOfGenerationCost().ifPresent(value -> die.setFloat("COUENECU", value));
        parameters.getOptionalContingenciesProbability().ifPresent(value -> die.setFloat("PROBAINC", value));
        parameters.getOptionalMaxSolverTime().ifPresent(value -> die.setInt("MAXSOLVE", value));
        parameters.getOptionalNbMaxIteration().ifPresent(value -> die.setInt("NBMAXMIT", value));
        parameters.getOptionalNbMaxCurativeAction().ifPresent(value -> die.setInt("NBMAXCUR", value));
        parameters.getOptionalGapVariableCost().ifPresent(value -> die.setInt("COUTECAR", value));
        parameters.getOptionalNbThreatResults().ifPresent(value -> die.setInt("NBTHREAT", value));
        parameters.getOptionalRedispatchingCostOffset().ifPresent(value -> die.setInt("REDISPOF", value));
        parameters.getOptionalAdequacyCostOffset().ifPresent(value -> die.setInt("ADEQUAOF", value));
        parameters.getOptionalCurativeRedispatchingLimit().ifPresent(value -> die.setInt("LIMCURGR", value));
        parameters.isShowAllTDandHVDCresults().ifPresent(value -> die.setBoolean("SHTDHVDC", value));
        parameters.isWithLostLoadDetailedResultsOnContingency().ifPresent(value -> die.setBoolean("LOSTLOAD", value));
        parameters.getOptionalNbMaxLostLoadDetailedResults().ifPresent(value -> die.setInt("LOSTCMAX", value));
    }

    private void writeBranch(String[] cqnomqua, float[] cqadmita, float[] cqresist, int[] qasurvdi, int[] qasurnmk, int[] tnnorqua, int[] tnnexqua,
                             int index, String branchId, double admittance, double r, int monitoringN, int monitoringNK, int node1, int node2) {
        cqnomqua[index - 1] = branchId;
        cqadmita[index - 1] = (float) (1 / admittance);
        cqresist[index - 1] = (float) r;
        qasurvdi[index - 1] = monitoringN;
        qasurnmk[index - 1] = monitoringNK;
        tnnorqua[index - 1] = node1;
        tnnexqua[index - 1] = node2;
    }

    private void writePhaseTapChanger(int[] dttrdequ, int[] dtmodreg, float[] dtvalinf, float[] dtvalsup, float[] dtvaldep, int[] lowtappo, int[] nbtaptd,
                                      int index, int branchId, int type, float alpha1, float alpha2, float alpha, int nbtap, int lowtap) {
        dttrdequ[index - 1] = branchId;
        dtmodreg[index - 1] = type;
        dtvalinf[index - 1] = Math.min(alpha1, alpha2);
        dtvalsup[index - 1] = Math.max(alpha1, alpha2);
        dtvaldep[index - 1] = alpha;
        lowtappo[index - 1] = lowtap;
        nbtaptd[index - 1] = nbtap;
    }

    private void writeBranches(boolean constantLossFactor, MetrixDie die) {

        // Branch
        String[] cqnomqua = new String[cqnbquad];
        float[] cqadmita = new float[cqnbquad];
        float[] cqresist = new float[cqnbquad];
        int[] qasurvdi = new int[cqnbquad];
        int[] qasurnmk = new int[cqnbquad];
        int[] tnnorqua = new int[cqnbquad];
        int[] tnnexqua = new int[cqnbquad];

        // PhaseTapChanger
        int[] dttrdequ = new int[dtnbtrde];
        int[] dtmodreg = new int[dtnbtrde];
        float[] dtvalsup = new float[dtnbtrde]; // PST max phasing value
        float[] dtvalinf = new float[dtnbtrde]; // PST min phasing value
        float[] dtvaldep = new float[dtnbtrde]; // PST current phasing value
        int[] dtlowtap = new int[dtnbtrde]; // PST min tap value
        int[] dtnbtaps = new int[dtnbtrde]; // PST number of taps
        List<Integer> dtlowran = new ArrayList<>(); // PST lowerTapRange [pst, lowerTapRange, ...]
        List<Integer> dtuppran = new ArrayList<>(); // PST upperTapRange [pst, upperTapRange, ...]
        List<Float> dttapdep = new ArrayList<>(); // PST phasing taps

        for (Line l : metrixNetwork.getLineList()) {
            double nominalVoltage1 = l.getTerminal1().getVoltageLevel().getNominalV();
            double nominalVoltage2 = l.getTerminal2().getVoltageLevel().getNominalV();
            double nominalVoltage = constantLossFactor ? Math.max(nominalVoltage1, nominalVoltage2) : nominalVoltage2;
            double r = (l.getR() * Math.pow(parameters.getNominalU(), 2)) / Math.pow(nominalVoltage, 2);
            double admittance = toAdmittance(l.getId(), l.getX(), nominalVoltage, parameters.getNominalU());
            int index = metrixNetwork.getIndex(l);
            int bus1Index = metrixNetwork.getIndex(l.getTerminal1().getBusBreakerView().getBus());
            int bus2Index = metrixNetwork.getIndex(l.getTerminal2().getBusBreakerView().getBus());
            writeBranch(cqnomqua, cqadmita, cqresist, qasurvdi, qasurnmk, tnnorqua, tnnexqua, index, l.getId(), admittance, r,
                    getMonitoringTypeBasecase(l.getId()), getMonitoringTypeOnContingency(l.getId()), bus1Index, bus2Index);
        }

        for (TwoWindingsTransformer twt : metrixNetwork.getTwoWindingsTransformerList()) {
            double nominalVoltage2 = twt.getTerminal2().getVoltageLevel().getNominalV();
            double x = twt.getX();
            double r = twt.getR();
            int index = metrixNetwork.getIndex(twt);

            if (twt.hasPhaseTapChanger()) {
                PhaseTapChanger ptc = twt.getPhaseTapChanger();
                int position = ptc.getTapPosition();
                x = x * (1 + ptc.getStep(position).getX() / 100);
                r = r * (1 + ptc.getStep(position).getR() / 100);
                if (constantLossFactor) {
                    float val = (float) (Math.pow(x, 2) + Math.pow(r, 2) - Math.pow(twt.getR(), 2));
                    if (val >= 0) {
                        x = (float) Math.sqrt(val);
                    }
                    LOGGER.debug(String.format("constantLossFactor -> twt <%s> x = <%f>", twt.getId(), x));
                }

                MetrixPtcControlType mode = MetrixPtcControlType.FIXED_ANGLE_CONTROL;
                if (dslData != null) {
                    mode = dslData.getPtcControl(twt.getId());

                    if (dslData.getPtcLowerTapChange(twt.getId()) != null) {
                        dtlowran.add(index);
                        dtlowran.add(dslData.getPtcLowerTapChange(twt.getId()));
                    }
                    if (dslData.getPtcUpperTapChange(twt.getId()) != null) {
                        dtuppran.add(index);
                        dtuppran.add(dslData.getPtcUpperTapChange(twt.getId()));
                    }
                }

                for (int pos = 0; pos < ptc.getStepCount(); pos++) {
                    dttapdep.add((float) ptc.getStep(pos).getAlpha());
                }

                writePhaseTapChanger(dttrdequ, dtmodreg, dtvalinf, dtvalsup, dtvaldep, dtlowtap, dtnbtaps,
                        metrixNetwork.getIndex(MetrixSubset.DEPHA, twt.getId()), index, mode.getType(),
                        (float) ptc.getStep(ptc.getLowTapPosition()).getAlpha(),
                        (float) ptc.getStep(ptc.getHighTapPosition()).getAlpha(),
                        (float) ptc.getStep(ptc.getTapPosition()).getAlpha(),
                        ptc.getStepCount(),
                        ptc.getLowTapPosition());
            }

            //Per-unitage
            double admittance = toAdmittance(twt.getId(), x, nominalVoltage2, parameters.getNominalU());
            r = (r * Math.pow(parameters.getNominalU(), 2)) / Math.pow(nominalVoltage2, 2);

            int bus1Index = metrixNetwork.getIndex(twt.getTerminal1().getBusBreakerView().getBus());
            int bus2Index = metrixNetwork.getIndex(twt.getTerminal2().getBusBreakerView().getBus());
            writeBranch(cqnomqua, cqadmita, cqresist, qasurvdi, qasurnmk, tnnorqua, tnnexqua, index, twt.getId(), admittance, r,
                    getMonitoringTypeBasecase(twt.getId()), getMonitoringTypeOnContingency(twt.getId()), bus1Index, bus2Index);
        }

        metrixNetwork.getThreeWindingsTransformerList().forEach(twt -> {
            throw new UnsupportedOperationException("TODO");
        });

        for (Switch sw : metrixNetwork.getSwitchList()) {
            int index = metrixNetwork.getIndex(sw);
            int bus1Index = metrixNetwork.getIndex(sw.getVoltageLevel().getBusBreakerView().getBus1(sw.getId()));
            int bus2Index = metrixNetwork.getIndex(sw.getVoltageLevel().getBusBreakerView().getBus2(sw.getId()));
            writeBranch(cqnomqua, cqadmita, cqresist, qasurvdi, qasurnmk, tnnorqua, tnnexqua, index, sw.getId(),
                    CQADMITA_SWITCH_VAL, CQRESIST_SWITCH_VAL, MonitoringType.NO.getType(), MonitoringType.NO.getType(), bus1Index, bus2Index);
        }

        // Branch
        die.setStringArray("CQNOMQUA", cqnomqua);
        die.setFloatArray("CQADMITA", cqadmita);
        die.setFloatArray("CQRESIST", cqresist);
        die.setIntArray("QASURVDI", qasurvdi);
        die.setIntArray("QASURNMK", qasurnmk);
        die.setIntArray("TNNORQUA", tnnorqua);
        die.setIntArray("TNNEXQUA", tnnexqua);

        // PhaseTapChanger
        die.setIntArray("DTTRDEQU", dttrdequ);
        die.setIntArray("DTMODREG", dtmodreg);
        die.setFloatArray("DTVALSUP", dtvalsup);
        die.setFloatArray("DTVALINF", dtvalinf);
        die.setFloatArray("DTVALDEP", dtvaldep);
        die.setIntArray("DTLOWTAP", dtlowtap);
        die.setIntArray("DTNBTAPS", dtnbtaps);
        die.setFloatArray("DTTAPDEP", ArrayUtils.toPrimitive(dttapdep.toArray(new Float[0]), 0.000F));
        if (!dtlowran.isEmpty()) {
            die.setIntArray("DTLOWRAN", dtlowran.stream().mapToInt(i -> i).toArray());
        }
        if (!dtuppran.isEmpty()) {
            die.setIntArray("DTUPPRAN", dtuppran.stream().mapToInt(i -> i).toArray());
        }

        // Disconnected branches
        if (!metrixNetwork.getDisconnectedElements().isEmpty()) {
            List<Integer> openbran = new ArrayList<>();
            for (Identifiable<?> disconnectedElement : metrixNetwork.getDisconnectedElements()) {
                try {
                    openbran.add(metrixNetwork.getIndex(disconnectedElement));
                } catch (IllegalStateException ise) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(String.format("Open branch '%s' is not in Main Connected Component", disconnectedElement.getId()));
                    }
                }
            }
            die.setInt("NBOPEBRA", openbran.size());
            die.setIntArray("OPENBRAN", openbran.stream().mapToInt(i -> i).toArray());
        }

    }

    private void writeTopology(MetrixDie die) {

        String[] cgnomreg = new String[cgnbregi];
        for (String country : metrixNetwork.getCountryList()) {
            int index = metrixNetwork.getCountryIndex(country);
            cgnomreg[index - 1] = replaceSpaces(country);
        }
        die.setStringArray("CGNOMREG", cgnomreg);

        int[] cpposreg = new int[tnnbntot];
        for (Bus bus : metrixNetwork.getBusList()) {
            int index = metrixNetwork.getIndex(bus);

            Substation substation = bus.getVoltageLevel().getSubstation()
                    .orElseThrow(() -> new IllegalStateException("No substation for bus [" + bus.getId() + "]"));
            cpposreg[index - 1] = metrixNetwork.getCountryIndex(metrixNetwork.getCountryCode(substation));
        }
        die.setIntArray("CPPOSREG", cpposreg);
    }

    private void writeLoad(int[] tnneucel, float[] esafiact, String[] tnnomnoe,
                           int index, int node, float p0, String id) {
        tnneucel[index] = node;
        esafiact[index] = p0;
        tnnomnoe[index] = id;
    }

    private void writeLoads(MetrixDie die) {

        int[] tnneucel = new int[ecnbcons];
        float[] esafiact = new float[ecnbcons];
        String[] tnnomnoe = new String[ecnbcons];

        int[] tnvapal1 = new int[ecnbcons];
        float[] tnvacou1 = new float[ecnbcons];

        Set<String> preventiveLoadsList;
        if (dslData != null) {
            preventiveLoadsList = dslData.getPreventiveLoadsList();
        } else {
            preventiveLoadsList = new HashSet<>();
        }

        int index = 0;
        for (Load load : metrixNetwork.getLoadList()) {
            int busIndex = metrixNetwork.getIndex(load.getTerminal().getBusBreakerView().getBus());
            String loadId = load.getId();
            writeLoad(tnneucel, esafiact, tnnomnoe, index, busIndex, (float) load.getP0(), loadId);

            if (!preventiveLoadsList.isEmpty() && preventiveLoadsList.contains(loadId)) {
                Integer preventivePercentage = dslData.getPreventiveLoadPercentage(loadId);
                tnvapal1[index] = preventivePercentage == null || preventivePercentage == 0 ? 0 : preventivePercentage;
                Float loadCost = dslData.getPreventiveLoadCost(loadId);
                tnvacou1[index] = loadCost == null ? UNDEFINED_VALUE : loadCost;
            }
            index++;
        }

        for (DanglingLine dl : metrixNetwork.getDanglingLineList()) {
            int busIndex = metrixNetwork.getIndex(dl.getTerminal().getBusBreakerView().getBus());
            writeLoad(tnneucel, esafiact, tnnomnoe, index, busIndex, (float) dl.getP0(), dl.getId());
            index++;
        }

        die.setStringArray("TNNOMNOE", tnnomnoe);
        die.setIntArray("TNNEUCEL", tnneucel);
        die.setFloatArray("ESAFIACT", esafiact);

        if (!preventiveLoadsList.isEmpty()) {
            die.setIntArray("TNVAPAL1", tnvapal1);
            die.setFloatArray("TNVACOU1", tnvacou1);
        }
    }

    private void writeGenerators(MetrixDie die) {

        String[] trnomgth = new String[trnbgrou];
        int[] tnneurgt = new int[trnbgrou];
        int[] spimpmod = new int[trnbgrou];
        float[] sppactgt = new float[trnbgrou];
        float[] trvalpmd = new float[trnbgrou];
        float[] trpuimin = new float[trnbgrou];

        List<String> trnomtyp = metrixNetwork.getGeneratorTypeList();
        int[] trtypgrp = new int[trnbgrou];

        for (Generator generator : metrixNetwork.getGeneratorList()) {
            int index = metrixNetwork.getIndex(generator);
            int busIndex = metrixNetwork.getIndex(generator.getTerminal().getBusBreakerView().getBus());
            trnomgth[index - 1] = generator.getId();
            tnneurgt[index - 1] = busIndex;
            trtypgrp[index - 1] = trnomtyp.indexOf(metrixNetwork.getGeneratorType(generator));
            GeneratorAdjustmentMode adjustmentMode = getGeneratorAdjustmentMode(generator.getId());
            spimpmod[index - 1] = adjustmentMode.getType();
            sppactgt[index - 1] = (float) generator.getTargetP();
            trvalpmd[index - 1] = (float) generator.getMaxP();
            trpuimin[index - 1] = (float) generator.getMinP();
            if (adjustmentMode == GeneratorAdjustmentMode.ADEQUACY_AND_REDISPATCHING ||
                    adjustmentMode == GeneratorAdjustmentMode.ADEQUACY_ONLY) {
                sumPmax += generator.getMaxP();
            }
        }

        die.setInt("TRNBTYPE", trnomtyp.size());
        die.setStringArray("TRNOMGTH", trnomgth);
        die.setStringArray("TRNOMTYP", trnomtyp.toArray(new String[0]));
        die.setIntArray("TNNEURGT", tnneurgt);
        die.setIntArray("TRTYPGRP", trtypgrp);
        die.setIntArray("SPIMPMOD", spimpmod);
        die.setFloatArray("SPPACTGT", sppactgt);
        die.setFloatArray("TRVALPMD", trvalpmd);
        die.setFloatArray("TRPUIMIN", trpuimin);
    }

    public static float getHvdcLineMax(HvdcLine hvdcLine) {
        return TimeSeriesMapper.getMax(hvdcLine);
    }

    public static float getHvdcLineMin(HvdcLine hvdcLine) {
        return TimeSeriesMapper.getMin(hvdcLine);
    }

    static HvdcAngleDroopActivePowerControl getActivePowerControl(HvdcLine hvdcLine) {
        return TimeSeriesMapper.getActivePowerControl(hvdcLine);
    }

    public static float getHvdcLineSetPoint(HvdcLine hvdcLine) {
        return TimeSeriesMapper.getHvdcLineSetPoint(hvdcLine);
    }

    private void writeHvdc(MetrixDie die) {

        String[] dcnomqua = new String[dcnblies];
        int[] dcnorqua = new int[dcnblies];
        int[] dcnexqua = new int[dcnblies];
        float[] dcminpui = new float[dcnblies];
        float[] dcmaxpui = new float[dcnblies];
        float[] dcimppui = new float[dcnblies];
        int[] dcregpui = new int[dcnblies];
        float[] dctensdc = new float[dcnblies];
        float[] dcresist = new float[dcnblies];
        float[] dcperst1 = new float[dcnblies];
        float[] dcperst2 = new float[dcnblies];
        float[] tmpDcdroopk = new float[dcnblies];

        for (HvdcLine l : metrixNetwork.getHvdcLineList()) {
            double nominalVoltage = l.getNominalV();
            int index = metrixNetwork.getIndex(l);
            int bus1Index = metrixNetwork.getIndex(l.getConverterStation1().getTerminal().getBusBreakerView().getBus());
            int bus2Index = metrixNetwork.getIndex(l.getConverterStation2().getTerminal().getBusBreakerView().getBus());
            dcnomqua[index - 1] = l.getId();
            dcnorqua[index - 1] = bus1Index;
            dcnexqua[index - 1] = bus2Index;

            dcminpui[index - 1] = getHvdcLineMin(l);
            dcmaxpui[index - 1] = getHvdcLineMax(l);

            boolean emulation = false;
            HvdcAngleDroopActivePowerControl activePowerControl = getActivePowerControl(l);
            if (activePowerControl != null) {
                emulation = true;
                tmpDcdroopk[dcnbdroo] = activePowerControl.getDroop();
                dcnbdroo++;
            }

            dcimppui[index - 1] = getHvdcLineSetPoint(l);
            dcregpui[index - 1] = getRegulationType(l, emulation).getType();
            dctensdc[index - 1] = (float) nominalVoltage;
            dcresist[index - 1] = (float) ((l.getR() * Math.pow(parameters.getNominalU(), 2)) / Math.pow(nominalVoltage, 2));
            dcperst1[index - 1] = l.getConverterStation1().getLossFactor();
            dcperst2[index - 1] = l.getConverterStation2().getLossFactor();
        }

        die.setInt("DCNDROOP", dcnbdroo);
        if (dcnbdroo > 0) {
            float[] dcdroopk = new float[dcnbdroo];
            System.arraycopy(tmpDcdroopk, 0, dcdroopk, 0, dcnbdroo);
            die.setFloatArray("DCDROOPK", dcdroopk);
        }
        die.setStringArray("DCNOMQUA", dcnomqua);
        die.setIntArray("DCNORQUA", dcnorqua);
        die.setIntArray("DCNEXQUA", dcnexqua);
        die.setFloatArray("DCMINPUI", dcminpui);
        die.setFloatArray("DCMAXPUI", dcmaxpui);
        die.setFloatArray("DCIMPPUI", dcimppui);
        die.setIntArray("DCREGPUI", dcregpui);
        die.setFloatArray("DCTENSDC", dctensdc);
        die.setFloatArray("DCRESIST", dcresist);
        die.setFloatArray("DCPERST1", dcperst1);
        die.setFloatArray("DCPERST2", dcperst2);
    }

    private void writeContingencies(MetrixDie die) {
        int index = 0;
        float maxGeneratorOutage = 0.f;
        List<Integer> dmptdefk = new ArrayList<>();
        List<String> dmnomdek = new ArrayList<>();
        List<Integer> dmdescrk = new ArrayList<>();

        for (Contingency contingency : metrixNetwork.getContingencyList()) {

            List<Integer> elementsToTrip = new ArrayList<>();

            float generatorPowerLost = 0.f;

            for (ContingencyElement element : contingency.getElements()) {
                try {
                    int type;
                    switch (element.getType()) {
                        case BRANCH:
                        case LINE:
                        case TWO_WINDINGS_TRANSFORMER:
                            type = ElementType.BRANCH.getType();
                            break;
                        case GENERATOR:
                            type = ElementType.GENERATOR.getType();
                            generatorPowerLost += metrixNetwork.getNetwork().getGenerator(element.getId()).getMaxP();
                            break;
                        case HVDC_LINE:
                            type = ElementType.HVDC.getType();
                            break;
                        default:
                            throw new PowsyblException("Unsupported contingency element '" + element.getId() + "' (type = " + element.getType() + ")");
                    }
                    int elementIndex = metrixNetwork.getIndex(element.getId()); // may throw an exception if element not found in MCC
                    elementsToTrip.add(type);
                    elementsToTrip.add(elementIndex);
                } catch (IllegalStateException ise) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(String.format("Error while processing contingency '%s', element '%s' not found.", contingency.getId(), element.getId()));
                        LOGGER.warn(ise.getMessage());
                    }
                }
            }

            if (!elementsToTrip.isEmpty()) {
                dmnomdek.add(contingency.getId());
                dmdescrk.add(elementsToTrip.size());
                dmptdefk.add(dmdescrk.size());
                dmdescrk.addAll(elementsToTrip);

                ctyIndex.put(contingency.getId(), index);

                index++;

                if (generatorPowerLost > maxGeneratorOutage) {
                    maxGeneratorOutage = generatorPowerLost;
                }
            }
        }

        die.setInt("DMNBDEFK", index);
        if (index > 0) {
            die.setIntArray("DMPTDEFK", dmptdefk.stream().mapToInt(i -> i).toArray());
            die.setStringArray("DMNOMDEK", dmnomdek.toArray(new String[0]));
            die.setIntArray("DMDESCRK", dmdescrk.stream().mapToInt(i -> i).toArray());

            if (maxGeneratorOutage > 0.) {
                // Power reserve for generator outage
                float[] trdemban = new float[trnbgrou];

                double prorata = maxGeneratorOutage / sumPmax;

                for (Generator generator : metrixNetwork.getGeneratorList()) {
                    index = metrixNetwork.getIndex(generator);
                    GeneratorAdjustmentMode adjustmentMode = getGeneratorAdjustmentMode(generator.getId());
                    float adjustmentValue = 0;
                    if (adjustmentMode == GeneratorAdjustmentMode.ADEQUACY_AND_REDISPATCHING ||
                            adjustmentMode == GeneratorAdjustmentMode.ADEQUACY_ONLY) {
                        adjustmentValue = Math.min((float) generator.getMaxP(),
                                BigDecimal.valueOf(prorata * generator.getMaxP()).setScale(1, RoundingMode.HALF_UP).floatValue());
                    }
                    trdemban[index - 1] = adjustmentValue;
                }
                die.setFloatArray("TRDEMBAN", trdemban);
            }
        }
    }

    private void writeSpecificContingencies(MetrixDie die) {
        if (dslData != null) {
            Set<Integer> dmspdefk = new TreeSet<>();

            dslData.getSpecificContingenciesList().forEach(cty -> {
                Integer indexCty = ctyIndex.get(cty);
                if (indexCty != null) {
                    dmspdefk.add(indexCty);
                }
            });

            if (!dmspdefk.isEmpty()) {
                die.setInt("NBDEFSPE", dmspdefk.size());
                die.setIntArray("PTDEFSPE", dmspdefk.stream().mapToInt(i -> i).toArray());
            }
        }
    }

    private void writeCurative(MetrixDie die) {
        if (dslData != null) {
            writeCurativePtc(die);
            writeCurativeHvdc(die);
            writeCurativeGenerators(die);
            writeCurativeLoads(die);
        }
    }

    private Map<Integer, String> getCurativeElementsIndexes(Set<String> elementIds, MetrixSubset subset) {
        Map<Integer, String> indexes = new TreeMap<>();
        for (String elementId : elementIds) {
            try {
                indexes.put(metrixNetwork.getIndex(subset, elementId), elementId);
            } catch (IllegalStateException ise) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(String.format("Curative element '%s' not found in Main Connected Component", elementId));
                }
            }
        }
        return indexes;
    }

    private void writeCurativePtc(MetrixDie die) {
        Map<Integer, String> curativePtcIndexes = getCurativeElementsIndexes(dslData.getPtcContingenciesList(),
                MetrixSubset.DEPHA);

        if (curativePtcIndexes.isEmpty()) {
            return;
        }

        int[] dtnbdefk = new int[dtnbtrde];
        List<Integer> dtptdefk = new ArrayList<>();
        curativePtcIndexes.forEach((index, pstId) -> {

            List<String> contingenciesList = dslData.getPtcContingencies(pstId);
            if (!contingenciesList.isEmpty()) {
                int nbCty = 0;
                Integer indexCty;
                for (String cty : contingenciesList) {
                    if ((indexCty = ctyIndex.get(cty)) != null) {
                        dtptdefk.add(indexCty);
                        nbCty++;
                    } else {
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn(String.format("Contingency '%s' not found for curative action of transformer '%s'", cty, pstId));
                        }
                    }
                }
                dtnbdefk[index - 1] = nbCty;
            }
        });

        if (!dtptdefk.isEmpty()) {
            die.setIntArray("DTNBDEFK", dtnbdefk);
            die.setIntArray("DTPTDEFK", dtptdefk.stream().mapToInt(i -> i).toArray());
        }
    }

    private void writeCurativeHvdc(MetrixDie die) {
        Map<Integer, String> curativeHvdcIndexes = getCurativeElementsIndexes(dslData.getHvdcContingenciesList(),
                MetrixSubset.HVDC);

        if (curativeHvdcIndexes.isEmpty()) {
            return;
        }

        int[] dcnbdefk = new int[dcnblies];
        List<Integer> dcptdefk = new ArrayList<>();

        curativeHvdcIndexes.forEach((index, hvdcId) -> {

            List<String> contingenciesList = dslData.getHvdcContingencies(hvdcId);
            if (!contingenciesList.isEmpty()) {
                int nbCty = 0;
                Integer indexCty;
                for (String cty : contingenciesList) {
                    if ((indexCty = ctyIndex.get(cty)) != null) {
                        dcptdefk.add(indexCty);
                        nbCty++;
                    } else {
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn(String.format("Contingency '%s' not found for curative action of hvdc line '%s'", cty, hvdcId));
                        }
                    }
                }
                dcnbdefk[index - 1] = nbCty;
            }
        });
        if (!dcptdefk.isEmpty()) {
            die.setIntArray("DCNBDEFK", dcnbdefk);
            die.setIntArray("DCPTDEFK", dcptdefk.stream().mapToInt(i -> i).toArray());
        }
    }

    private void writeCurativeGenerators(MetrixDie die) {
        Map<Integer, String> curativeGeneratorIndexes = getCurativeElementsIndexes(dslData.getGeneratorContingenciesList(),
                MetrixSubset.GROUPE);

        if (!curativeGeneratorIndexes.isEmpty()) {
            int[] grnbdefk = new int[trnbgrou];
            List<Integer> grptdefk = new ArrayList<>();
            AtomicInteger nbCurativeGenerators = new AtomicInteger(0);
            curativeGeneratorIndexes.forEach((index, generatorId) -> {

                List<String> contingenciesList = dslData.getGeneratorContingencies(generatorId);
                if (!contingenciesList.isEmpty()) {
                    int nbCty = 0;
                    Integer indexCty;
                    for (String cty : contingenciesList) {
                        if ((indexCty = ctyIndex.get(cty)) != null) {
                            grptdefk.add(indexCty);
                            nbCty++;
                        } else {
                            if (LOGGER.isWarnEnabled()) {
                                LOGGER.warn(String.format("Contingency '%s' not found for curative action of generator '%s'", cty, generatorId));
                            }
                        }
                    }
                    if (nbCty > 0) {
                        grnbdefk[index - 1] = nbCty;
                        nbCurativeGenerators.getAndIncrement();
                    }
                }
            });
            if (!grptdefk.isEmpty()) {
                die.setInt("GRNBCURA", nbCurativeGenerators.intValue());
                die.setIntArray("GRNBDEFK", grnbdefk);
                die.setIntArray("GRPTDEFK", grptdefk.stream().mapToInt(i -> i).toArray());
            }
        }
    }

    private void writeCurativeLoads(MetrixDie die) {
        Map<Integer, String> curativeLoadIds = getCurativeElementsIndexes(dslData.getCurativeLoadsList(),
                MetrixSubset.LOAD);

        if (!curativeLoadIds.isEmpty()) {
            int[] ldnbdefk = new int[ecnbcons];
            List<Integer> ldptdefk = new ArrayList<>();
            List<Integer> ldcurper = new ArrayList<>();

            curativeLoadIds.forEach((index, loadId) -> {

                List<String> contingenciesList = dslData.getLoadContingencies(loadId);
                if (!contingenciesList.isEmpty()) {
                    int nbCty = 0;
                    Integer indexCty;
                    for (String cty : contingenciesList) {
                        if ((indexCty = ctyIndex.get(cty)) != null) {
                            ldptdefk.add(indexCty);
                            nbCty++;
                        } else {
                            if (LOGGER.isWarnEnabled()) {
                                LOGGER.warn(String.format("Contingency '%s' not found for curative action of load '%s'", cty, loadId));
                            }
                        }
                    }
                    ldnbdefk[index - 1] = nbCty;
                    if (nbCty > 0) {
                        ldcurper.add(dslData.getCurativeLoadPercentage(loadId));
                    }
                }
            });

            if (!ldptdefk.isEmpty()) {
                die.setInt("NBLDCURA", ldcurper.size());
                die.setIntArray("LDNBDEFK", ldnbdefk);
                die.setIntArray("LDPTDEFK", ldptdefk.stream().mapToInt(i -> i).toArray());
                die.setIntArray("LDCURPER", ldcurper.stream().mapToInt(i -> i).toArray());
            }
        }
    }

    private void writeContingencyFlowResults(MetrixDie die) {
        if (dslData != null) {
            Set<String> idList = dslData.getContingencyFlowResultList();

            if (!idList.isEmpty()) {
                int nbdefres = 0;
                List<Integer> ptdefres = new ArrayList<>();
                Integer indexCty;
                for (String branchId : idList) {

                    Identifiable<?> identifiable = metrixNetwork.getIdentifiable(branchId);
                    if (identifiable == null ||
                            !metrixNetwork.isMapped(identifiable)) {
                        continue;
                    }

                    List<Integer> tmpList = new ArrayList<>();
                    for (String cty : dslData.getContingencyFlowResult(branchId)) {
                        if ((indexCty = ctyIndex.get(cty)) != null) {
                            tmpList.add(indexCty);
                        } else {
                            if (LOGGER.isWarnEnabled()) {
                                LOGGER.warn(String.format("Contingency '%s' not found for detailed flow results of branch '%s'", cty, branchId));
                            }
                        }
                    }

                    if (!tmpList.isEmpty()) {
                        nbdefres += tmpList.size() + 2;
                        ptdefres.add(metrixNetwork.getIndex(MetrixSubset.QUAD, branchId));
                        ptdefres.add(tmpList.size());
                        ptdefres.addAll(tmpList);
                    }
                }

                if (nbdefres > 0) {
                    die.setInt("NBDEFRES", nbdefres);
                    die.setIntArray("PTDEFRES", ptdefres.stream().mapToInt(i -> i).toArray());
                }
            }
        }
    }

    private void writeDetailedMarginalVariations(MetrixDie die) {
        if (dslData != null) {
            Set<String> idList = dslData.getContingencyDetailedMarginalVariationsList();

            if (!idList.isEmpty()) {
                int nbvarmar = 0;
                List<Integer> ptvarmar = new ArrayList<>();
                Integer indexCty;
                for (String branchId : idList) {

                    Identifiable<?> identifiable = metrixNetwork.getIdentifiable(branchId);
                    if (identifiable == null ||
                            !metrixNetwork.isMapped(identifiable)) {
                        continue;
                    }

                    List<Integer> tmpList = new ArrayList<>();
                    for (String cty : dslData.getContingencyDetailedMarginalVariations(branchId)) {
                        if ((indexCty = ctyIndex.get(cty)) != null) {
                            tmpList.add(indexCty);
                        } else {
                            if (LOGGER.isWarnEnabled()) {
                                LOGGER.warn(String.format("Contingency '%s' not found for detailed marginal variations of branch '%s'", cty, branchId));
                            }
                        }
                    }

                    if (!tmpList.isEmpty()) {
                        nbvarmar += tmpList.size() + 2;
                        ptvarmar.add(metrixNetwork.getIndex(MetrixSubset.QUAD, branchId));
                        ptvarmar.add(tmpList.size());
                        ptvarmar.addAll(tmpList);
                    }
                }

                if (nbvarmar > 0) {
                    die.setInt("NBVARMAR", nbvarmar);
                    die.setIntArray("PTVARMAR", ptvarmar.stream().mapToInt(i -> i).toArray());
                }
            }
        }
    }

    private void writeSections(MetrixDie die) {
        if (dslData != null && !dslData.getSectionList().isEmpty()) {

            int index = 0;
            String[] sectnoms = new String[sectnbse];
            float[] sectmaxn = new float[sectnbse];
            int[] sectnbqd = new int[sectnbse];

            List<Integer> secttype = new ArrayList<>();
            List<Integer> sectnumq = new ArrayList<>();
            List<Float> sectcoef = new ArrayList<>();

            for (MetrixSection section : dslData.getSectionList()) {
                sectnoms[index] = replaceSpaces(section.getId());
                sectmaxn[index] = section.getMaxFlowN();
                for (Map.Entry<String, Float> branch : section.getCoefFlowList().entrySet()) {
                    Identifiable<?> identifiable = metrixNetwork.getNetwork().getIdentifiable(branch.getKey());
                    if (identifiable != null) {
                        if (identifiable instanceof Line || identifiable instanceof TwoWindingsTransformer) {
                            secttype.add(ElementType.BRANCH.getType());
                        } else if (identifiable instanceof HvdcLine) {
                            secttype.add(ElementType.HVDC.getType());
                        } else {
                            if (LOGGER.isErrorEnabled()) {
                                LOGGER.error(String.format("Section '%s' : unsupported element type '%s'", section.getId(), identifiable.getClass().getName()));
                            }
                        }
                        sectnumq.add(metrixNetwork.getIndex(identifiable));
                        sectcoef.add(branch.getValue());
                        sectnbqd[index]++;
                    } else {
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn(String.format("Section '%s' : element not found in network '%s'", section.getId(), branch.getKey()));
                        }
                    }
                }
                index++;
            }

            die.setStringArray("SECTNOMS", sectnoms);
            die.setFloatArray("SECTMAXN", sectmaxn);
            die.setIntArray("SECTNBQD", sectnbqd);
            die.setIntArray("SECTTYPE", secttype.stream().mapToInt(i -> i).toArray());
            die.setIntArray("SECTNUMQ", sectnumq.stream().mapToInt(i -> i).toArray());
            die.setFloatArray("SECTCOEF", ArrayUtils.toPrimitive(sectcoef.toArray(new Float[0]), 0.0F));
        }
    }

    private void writeGeneratorsBindings(MetrixDie die) {
        if (dslData != null) {
            Collection<MetrixGeneratorsBinding> bindings = dslData.getGeneratorsBindingsValues();

            if (!bindings.isEmpty()) {

                List<String> gbindnom = new ArrayList<>();
                List<Integer> gbindref = new ArrayList<>();
                List<Integer> gbinddef = new ArrayList<>();

                for (MetrixGeneratorsBinding binding : bindings) {
                    List<Integer> idList = new ArrayList<>();
                    for (String generatorId : binding.getGeneratorsIds()) {
                        try {
                            idList.add(metrixNetwork.getIndex(MetrixSubset.GROUPE, generatorId));
                        } catch (IllegalStateException ise) {
                            if (LOGGER.isWarnEnabled()) {
                                LOGGER.warn(String.format("generator group '%s' : generator '%s' not found", binding.getName(), generatorId));
                            }
                        }
                    }

                    if (idList.size() > 1) {
                        gbindnom.add(binding.getName());
                        gbindref.add(binding.getReference().getType());
                        gbinddef.add(idList.size());
                        gbinddef.addAll(idList);
                    } else if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(String.format("generator group '%s' ignored because it contains too few elements (%d)", binding.getName(), idList.size()));
                    }

                }

                die.setInt("NBGBINDS", gbindnom.size());
                die.setStringArray("GBINDNOM", gbindnom.toArray(new String[0]));
                die.setIntArray("GBINDREF", gbindref.stream().mapToInt(i -> i).toArray());
                die.setIntArray("GBINDDEF", gbinddef.stream().mapToInt(i -> i).toArray());
            }
        }
    }

    private void writeLoadsBindings(MetrixDie die) {
        if (dslData != null) {
            Collection<MetrixLoadsBinding> bindings = dslData.getLoadsBindingsValues();

            if (!bindings.isEmpty()) {

                List<String> lbindnom = new ArrayList<>();
                List<Integer> lbinddef = new ArrayList<>();

                for (MetrixLoadsBinding binding : bindings) {
                    List<Integer> idList = new ArrayList<>();
                    for (String loadId : binding.getLoadsIds()) {
                        try {
                            idList.add(metrixNetwork.getIndex(MetrixSubset.LOAD, loadId));
                        } catch (IllegalStateException ise) {
                            if (LOGGER.isWarnEnabled()) {
                                LOGGER.warn(String.format("load group '%s' : load '%s' not found", binding.getName(), loadId));
                            }
                        }
                    }

                    if (idList.size() > 1) {
                        lbindnom.add(binding.getName());
                        lbinddef.add(idList.size());
                        lbinddef.addAll(idList);
                    } else if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(String.format("load group '%s' ignored because it contains to few elements (%d)", binding.getName(), idList.size()));
                    }

                }

                if (!lbindnom.isEmpty()) {
                    die.setInt("NBLBINDS", lbindnom.size());
                    die.setStringArray("LBINDNOM", lbindnom.toArray(new String[0]));
                    die.setIntArray("LBINDDEF", lbinddef.stream().mapToInt(i -> i).toArray());
                }
            }
        }
    }

    private void write(Path dir, boolean write, boolean writeJson, BufferedWriter writer, boolean constantLossFactor) throws IOException {
        MetrixDie die = new MetrixDie();
        writeGeneral(die);
        writeOptions(die);
        writeTopology(die);
        writeBranches(constantLossFactor, die);
        writeLoads(die);
        writeGenerators(die);
        writeHvdc(die);
        writeContingencies(die);
        writeSpecificContingencies(die);
        writeCurative(die);
        writeContingencyFlowResults(die);
        writeDetailedMarginalVariations(die);
        writeSections(die);
        writeGeneratorsBindings(die);
        writeLoadsBindings(die);

        //die.print(System.out);

        if (write && dir != null) {
            die.save(dir);
        }
        if (writeJson && dir != null) {
            die.saveToJson(dir.resolve("fort.json"));
        }
        if (writer != null) {
            die.saveToJson(writer);
        }
    }

    public void write(Path dir, boolean debug, boolean constantLossFactor) throws IOException {
        write(dir, true, debug, null, constantLossFactor);
    }

    public void writeJson(StringWriter writer) throws IOException {
        write(null, false, false, new BufferedWriter(writer), false);
    }

    /**
     * Estimation of the minimum number of result time-series for this metrix configuration
     */
    public int minResultNumberEstimate() {
        int nbTimeSeries = 0;
        if (dslData != null) {
            nbTimeSeries += dslData.getBranchMonitoringNList().size() + dslData.getSectionList().size();
            int nbBranchNk = dslData.getBranchMonitoringNkList().size();
            nbTimeSeries += 2 * nbBranchNk * parameters.getOptionalNbThreatResults().orElse(1);
            nbTimeSeries += 2 * nbBranchNk * parameters.isPreCurativeResults().map(b -> b ? 1 : 0).orElse(0);
            nbTimeSeries += dslData.getContingencyFlowResultList().stream().mapToInt(s -> dslData.getContingencyFlowResult(s).size()).sum();
        }
        return nbTimeSeries;

    }

    /**
     * Estimation of the maximum number of result time-series for this metrix configuration
     * (does not take topological remedial action into account)
     */
    public int maxResultNumberEstimate() {
        int nbTimeSeries = 0;
        if (dslData != null) {
            nbTimeSeries = minResultNumberEstimate();
            // PST preventive and curative
            nbTimeSeries += dslData.getPtcControlList().size();
            nbTimeSeries += dslData.getPtcContingenciesList().stream().mapToInt(s -> dslData.getPtcContingencies(s).size()).sum();
            // HVDC preventive and curative
            long nbOptimizedHvdc = dslData.getHvdcControlList().size();
            nbTimeSeries += nbOptimizedHvdc;
            nbTimeSeries += dslData.getHvdcContingenciesList().stream().mapToInt(s -> dslData.getHvdcContingencies(s).size()).sum();
            // Generator adequacy, preventive and curative results
            if (parameters.isWithAdequacyResults().orElse(false)) {
                nbTimeSeries += dslData.getGeneratorsForAdequacy().isEmpty() ? trnbgrou : dslData.getGeneratorsForAdequacy().size();
            }
            nbTimeSeries += dslData.getGeneratorsForRedispatching().size();
            nbTimeSeries += dslData.getGeneratorContingenciesList().stream().mapToInt(s -> dslData.getGeneratorContingencies(s).size()).sum();
            // Load curative only results
            nbTimeSeries += dslData.getCurativeLoadsList().stream().mapToInt(s -> dslData.getLoadContingencies(s).size()).sum();
            //Marginal variation results
            if (parameters.isMarginalVariationsOnBranches().orElse(!dslData.getContingencyDetailedMarginalVariationsList().isEmpty())) {
                nbTimeSeries += dslData.getBranchMonitoringNList().stream()
                        .filter(b -> dslData.getBranchMonitoringN(b) == MonitoringType.MONITORING)
                        .count();
                nbTimeSeries += dslData.getSectionList().size();
                nbTimeSeries += metrixNetwork.getContingencyList().size() * dslData.getBranchMonitoringNkList()
                        .stream()
                        .filter(b -> dslData.getBranchMonitoringNk(b) == MonitoringType.MONITORING)
                        .count();
                nbTimeSeries += 2 * dslData.getContingencyDetailedMarginalVariationsList()
                        .stream()
                        .mapToInt(s -> dslData.getContingencyDetailedMarginalVariations(s).size()).sum();
            }
            if (parameters.isMarginalVariationsOnHvdc().orElse(false)) {
                nbTimeSeries += nbOptimizedHvdc;
            }
        }
        return nbTimeSeries;
    }

    private String replaceSpaces(String txt) {
        return txt.replace(" ", "_");
    }

}
