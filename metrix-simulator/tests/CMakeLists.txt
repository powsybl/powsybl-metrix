#
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#

if(USE_XPRESS)
    # OR-Tools never links libxprs: at the first Xpress solve it dlopen()s
    # "$XPRESSDIR/lib/libxprs.so" (xprs.dll under %XPRESSDIR%\bin on Windows),
    # then falls back to the canonical install location (/opt/xpressmp). See
    # ortools/xpress/environment.cc. The variable that loader honours is
    # XPRESSDIR; LD_LIBRARY_PATH plays no part in locating libxprs.so.
    # The Xpress location is taken from XPRESS_ROOT (CMake variable or
    # environment) or, failing that, from the XPRESSDIR a licensed workstation
    # usually exports (xpvars.sh), and handed to every test as XPRESSDIR.
    if(NOT XPRESS_ROOT AND DEFINED ENV{XPRESS_ROOT})
        set(XPRESS_ROOT $ENV{XPRESS_ROOT})
    endif()
    if(NOT XPRESS_ROOT AND DEFINED ENV{XPRESSDIR})
        set(XPRESS_ROOT $ENV{XPRESSDIR})
    endif()
    if(XPRESS_ROOT)
        # LD_LIBRARY_PATH is still prepended (never overwritten) for the runtime
        # dependencies of libxprs.so itself, as xpvars.sh does. The caller's
        # value is only appended when non-empty: a trailing ":" would add an
        # empty element, which the dynamic loader reads as the current directory.
        set(METRIX_TEST_LD_LIBRARY_PATH "${XPRESS_ROOT}/lib")
        if(NOT "$ENV{LD_LIBRARY_PATH}" STREQUAL "")
            string(APPEND METRIX_TEST_LD_LIBRARY_PATH ":$ENV{LD_LIBRARY_PATH}")
        endif()
        set(METRIX_TEST_XPRESS_ENV
            "XPRESSDIR=${XPRESS_ROOT}"
            "LD_LIBRARY_PATH=${METRIX_TEST_LD_LIBRARY_PATH}")
    else()
        set(METRIX_TEST_XPRESS_ENV "")
        message(WARNING "USE_XPRESS is ON but neither XPRESS_ROOT nor XPRESSDIR is set: OR-Tools will only look for libxprs.so under /opt/xpressmp")
    endif()
endif()

macro(metrix_test TEST_NAME NB_TESTS)
    set(FULL_TEST_NAME ${TEST_SOLVERCH}_${TEST_NAME})
    set(WORKING_DIR ${TEST_DIR}/${TEST_NAME})
    set(EXPECTED_DIR ${EXPECTED_TEST_DIR}/${TEST_NAME})
    file(GLOB TEST_OUTPUT_FILES ${WORKING_DIR}/contraintes_*.txt ${WORKING_DIR}/metrixOut.txt ${WORKING_DIR}/out_* ${WORKING_DIR}/metrix*.log)
    list(LENGTH TEST_OUTPUT_FILES RES_LEN)
    if(RES_LEN GREATER 0)
        file(REMOVE ${TEST_OUTPUT_FILES})
    endif()
    add_test(NAME ${FULL_TEST_NAME} COMMAND ${CMAKE_COMMAND}
        -DEXE=$<TARGET_FILE:metrix-simulator>
        -DWORKING_DIR=${WORKING_DIR}
        -DEXPECTED_DIR=${EXPECTED_DIR}
        -DNB_TESTS=${NB_TESTS}
        -DALL_RESULTS=1
        -DWITH_LODF_PTDF=0
        -DALL_OUTPUTS=0
        -P ${CMAKE_SOURCE_DIR}/cmake/tnr.cmake
        WORKING_DIRECTORY ${WORKING_DIR})
    set_property(TEST ${FULL_TEST_NAME} PROPERTY ENVIRONMENT METRIX_ETC=${CMAKE_SOURCE_DIR}/etc)
    if(METRIX_TEST_XPRESS_ENV)
        set_property(TEST ${FULL_TEST_NAME} APPEND PROPERTY ENVIRONMENT ${METRIX_TEST_XPRESS_ENV})
    endif()
endmacro()

macro(metrix_test_all_outputs TEST_NAME NB_TESTS)
    set(FULL_TEST_NAME ${TEST_SOLVERCH}_${TEST_NAME})
    set(WORKING_DIR ${TEST_DIR}/${TEST_NAME})
    set(EXPECTED_DIR ${EXPECTED_TEST_DIR}/${TEST_NAME})
    file(GLOB TEST_OUTPUT_FILES ${WORKING_DIR}/contraintes_*.txt ${WORKING_DIR}/metrixOut.txt ${WORKING_DIR}/out_* ${WORKING_DIR}/metrix*.log)
    list(LENGTH TEST_OUTPUT_FILES RES_LEN)
    if(RES_LEN GREATER 0)
        file(REMOVE ${TEST_OUTPUT_FILES})
    endif()
    add_test(NAME ${FULL_TEST_NAME} COMMAND  ${CMAKE_COMMAND}
        -DEXE=$<TARGET_FILE:metrix-simulator>
        -DWORKING_DIR=${WORKING_DIR}
        -DEXPECTED_DIR=${EXPECTED_DIR}
        -DNB_TESTS=${NB_TESTS}
        -DALL_RESULTS=1
        -DWITH_LODF_PTDF=0
        -DALL_OUTPUTS=1
        -P ${CMAKE_SOURCE_DIR}/cmake/tnr.cmake
        WORKING_DIRECTORY ${WORKING_DIR})
    set_property(TEST ${FULL_TEST_NAME} PROPERTY ENVIRONMENT METRIX_ETC=${CMAKE_SOURCE_DIR}/etc)
    if(METRIX_TEST_XPRESS_ENV)
        set_property(TEST ${FULL_TEST_NAME} APPEND PROPERTY ENVIRONMENT ${METRIX_TEST_XPRESS_ENV})
    endif()
endmacro()

macro(metrix_test_check_only_results TEST_NAME NB_TESTS)
    set(FULL_TEST_NAME ${TEST_SOLVERCH}_${TEST_NAME})
    set(WORKING_DIR ${TEST_DIR}/${TEST_NAME})
    set(EXPECTED_DIR ${EXPECTED_TEST_DIR}/${TEST_NAME})
    file(GLOB TEST_OUTPUT_FILES ${WORKING_DIR}/contraintes_*.txt ${WORKING_DIR}/metrixOut.txt ${WORKING_DIR}/out_* ${WORKING_DIR}/metrix*.log)
    list(LENGTH TEST_OUTPUT_FILES RES_LEN)
    if(RES_LEN GREATER 0)
        file(REMOVE ${TEST_OUTPUT_FILES})
    endif()
    add_test(NAME ${FULL_TEST_NAME} COMMAND  ${CMAKE_COMMAND}
        -DEXE=$<TARGET_FILE:metrix-simulator>
        -DWORKING_DIR=${WORKING_DIR}
        -DEXPECTED_DIR=${EXPECTED_DIR}
        -DNB_TESTS=${NB_TESTS}
        -DALL_RESULTS=0
        -DWITH_LODF_PTDF=0
        -DALL_OUTPUTS=0
        -P ${CMAKE_SOURCE_DIR}/cmake/tnr.cmake
        WORKING_DIRECTORY ${WORKING_DIR})
    set_property(TEST ${FULL_TEST_NAME} PROPERTY ENVIRONMENT METRIX_ETC=${CMAKE_SOURCE_DIR}/etc)
    if(METRIX_TEST_XPRESS_ENV)
        set_property(TEST ${FULL_TEST_NAME} APPEND PROPERTY ENVIRONMENT ${METRIX_TEST_XPRESS_ENV})
    endif()
    message(AUTHOR_WARNING "Only checks on output results files performed: metrixOut.txt result file of test \"" ${FULL_TEST_NAME} "\" must be checked manually")
endmacro()

# This macro defines the test only by launching the metrix computation, without checking the outputs
# Used in case the order in the output file is not garanteed between two runs at the same configuration
macro(metrix_test_no_check TEST_NAME NB_TESTS)
    set(FULL_TEST_NAME ${TEST_SOLVERCH}_${TEST_NAME})
    set(WORKING_DIR ${TEST_DIR}/${TEST_NAME})
    file(GLOB TEST_OUTPUT_FILES ${WORKING_DIR}/contraintes_*.txt ${WORKING_DIR}/metrixOut.txt ${WORKING_DIR}/out_* ${WORKING_DIR}/metrix*.log)
    list(LENGTH TEST_OUTPUT_FILES RES_LEN)
    if(RES_LEN GREATER 0)
        file(REMOVE ${TEST_OUTPUT_FILES})
    endif()
    add_test(NAME ${FULL_TEST_NAME} COMMAND  metrix-simulator metrixOut.txt VariantSet.csv out 0 ${NB_TESTS} WORKING_DIRECTORY ${WORKING_DIR})
    set_property(TEST ${FULL_TEST_NAME} PROPERTY ENVIRONMENT HADES_DIR=.)
    set_property(TEST ${FULL_TEST_NAME} APPEND PROPERTY ENVIRONMENT METRIX_ETC=${CMAKE_SOURCE_DIR}/etc)
    if(METRIX_TEST_XPRESS_ENV)
        set_property(TEST ${FULL_TEST_NAME} APPEND PROPERTY ENVIRONMENT ${METRIX_TEST_XPRESS_ENV})
    endif()
    message(AUTHOR_WARNING "No check on output files performed: outputs of test \"" ${FULL_TEST_NAME} "\" must be checked manually")
endmacro()

# This macro defines a test that must fail while validating the fort.json
# configuration. The expected error message is checked on the process output:
# a configuration error is thrown before the logger is configured, so main()
# prints it on stderr (see the outer catch in metrix.cpp). With
# PASS_REGULAR_EXPRESSION set, the test passes if and only if the message is
# emitted, whatever the exit code.
macro(metrix_test_config_error TEST_NAME EXPECTED_MESSAGE)
    set(FULL_TEST_NAME ${TEST_SOLVERCH}_${TEST_NAME})
    set(WORKING_DIR ${TEST_DIR}/${TEST_NAME})
    file(GLOB TEST_OUTPUT_FILES ${WORKING_DIR}/contraintes_*.txt ${WORKING_DIR}/metrixOut.txt ${WORKING_DIR}/out_* ${WORKING_DIR}/metrix*.log)
    list(LENGTH TEST_OUTPUT_FILES RES_LEN)
    if(RES_LEN GREATER 0)
        file(REMOVE ${TEST_OUTPUT_FILES})
    endif()
    add_test(NAME ${FULL_TEST_NAME} COMMAND metrix-simulator metrixOut.txt VariantSet.csv out 0 1 WORKING_DIRECTORY ${WORKING_DIR})
    set_property(TEST ${FULL_TEST_NAME} PROPERTY PASS_REGULAR_EXPRESSION ${EXPECTED_MESSAGE})
    set_property(TEST ${FULL_TEST_NAME} APPEND PROPERTY ENVIRONMENT METRIX_ETC=${CMAKE_SOURCE_DIR}/etc)
    if(METRIX_TEST_XPRESS_ENV)
        set_property(TEST ${FULL_TEST_NAME} APPEND PROPERTY ENVIRONMENT ${METRIX_TEST_XPRESS_ENV})
    endif()
endmacro()

# This macro defines a test that is expected to fail (non-zero return code),
# e.g. when the input configuration is intentionally invalid and must be rejected.
macro(metrix_test_expect_failure TEST_NAME NB_TESTS)
    set(FULL_TEST_NAME ${TEST_SOLVERCH}_${TEST_NAME})
    set(WORKING_DIR ${TEST_DIR}/${TEST_NAME})
    file(GLOB TEST_OUTPUT_FILES ${WORKING_DIR}/contraintes_*.txt ${WORKING_DIR}/metrixOut.txt ${WORKING_DIR}/out_* ${WORKING_DIR}/metrix*.log)
    list(LENGTH TEST_OUTPUT_FILES RES_LEN)
    if(RES_LEN GREATER 0)
        file(REMOVE ${TEST_OUTPUT_FILES})
    endif()
    add_test(NAME ${FULL_TEST_NAME} COMMAND metrix-simulator metrixOut.txt VariantSet.csv out 0 ${NB_TESTS} WORKING_DIRECTORY ${WORKING_DIR})
    set_property(TEST ${FULL_TEST_NAME} PROPERTY ENVIRONMENT HADES_DIR=.)
    set_property(TEST ${FULL_TEST_NAME} APPEND PROPERTY ENVIRONMENT METRIX_ETC=${CMAKE_SOURCE_DIR}/etc)
    set_property(TEST ${FULL_TEST_NAME} PROPERTY WILL_FAIL TRUE)
    if(METRIX_TEST_XPRESS_ENV)
        set_property(TEST ${FULL_TEST_NAME} APPEND PROPERTY ENVIRONMENT ${METRIX_TEST_XPRESS_ENV})
    endif()
endmacro()

macro(metrix_test_with_lodf_pdtf TEST_NAME NB_TESTS)
    set(FULL_TEST_NAME ${TEST_SOLVERCH}_${TEST_NAME})
    set(WORKING_DIR ${TEST_DIR}/${TEST_NAME})
    set(EXPECTED_DIR ${EXPECTED_TEST_DIR}/${TEST_NAME})
    file(GLOB TEST_OUTPUT_FILES ${WORKING_DIR}/contraintes_*.txt ${WORKING_DIR}/metrixOut.txt ${WORKING_DIR}/out_* ${WORKING_DIR}/metrix*.log)
    list(LENGTH TEST_OUTPUT_FILES RES_LEN)
    if(RES_LEN GREATER 0)
        file(REMOVE ${TEST_OUTPUT_FILES})
    endif()
    add_test(NAME ${FULL_TEST_NAME} COMMAND  ${CMAKE_COMMAND}
        -DEXE=$<TARGET_FILE:metrix-simulator>
        -DWORKING_DIR=${WORKING_DIR}
        -DEXPECTED_DIR=${EXPECTED_DIR}
        -DNB_TESTS=${NB_TESTS}
        -DALL_RESULTS=1
        -DWITH_LODF_PTDF=1
        -DALL_OUTPUTS=0
        -P ${CMAKE_SOURCE_DIR}/cmake/tnr.cmake
        WORKING_DIRECTORY ${WORKING_DIR})
    set_property(TEST ${FULL_TEST_NAME} PROPERTY ENVIRONMENT HADES_DIR=.)
    set_property(TEST ${FULL_TEST_NAME} APPEND PROPERTY ENVIRONMENT METRIX_ETC=${CMAKE_SOURCE_DIR}/etc)
    if(METRIX_TEST_XPRESS_ENV)
        set_property(TEST ${FULL_TEST_NAME} APPEND PROPERTY ENVIRONMENT ${METRIX_TEST_XPRESS_ENV})
    endif()
endmacro()

# Asserts that two committed reference files are byte-identical. This encodes
# an invariant that must survive reference regeneration: the regular TNR tests
# only compare each run against its own reference, and are therefore blind to
# a behavior change that gets baked into regenerated references.
macro(metrix_test_identical_references TEST_NAME FILE_A FILE_B)
    set(FULL_TEST_NAME ${TEST_SOLVERCH}_${TEST_NAME})
    add_test(NAME ${FULL_TEST_NAME}
        COMMAND ${CMAKE_COMMAND} -E compare_files ${FILE_A} ${FILE_B})
endmacro()

# Solver-independent tests: configuration or input validation failures and
# pure load-flow runs that never reach a solver call. They run once, whatever
# the solver backends compiled in.
add_subdirectory(common)
add_subdirectory(sirius)
# The Xpress test suite is opt-in: FICO Xpress is a licensed product, so these
# tests must never run in automated environments (CI). Licensed developers can
# enable them locally with -D METRIX_RUN_XPRESS_TESTS=ON (requires USE_XPRESS=ON).
if (USE_XPRESS AND METRIX_RUN_XPRESS_TESTS)
    add_subdirectory(xpress)
elseif (METRIX_RUN_XPRESS_TESTS)
    message(WARNING "METRIX_RUN_XPRESS_TESTS is ON but USE_XPRESS is OFF: the Xpress test suite will not be added")
endif()
