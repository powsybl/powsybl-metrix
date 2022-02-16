package com.powsybl.metrix.mapping.timeseries;

public enum ScalingDownLimitViolation {
    BASE_CASE_MINP_BY_TARGETP,
    MAPPED_MINP_BY_TARGETP,
    MAXP_BY_TARGETP,
    MAXP_BY_ACTIVEPOWER,
    CS1TOCS2_BY_ACTIVEPOWER,
    MINP_BY_TARGETP,
    MINP_BY_ACTIVEPOWER,
    CS2TOCS1_BY_ACTIVEPOWER
}
