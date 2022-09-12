# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#

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
        -P ${CMAKE_SOURCE_DIR}/cmake/tnr.cmake
        WORKING_DIRECTORY ${WORKING_DIR})
    set_property(TEST ${FULL_TEST_NAME} PROPERTY ENVIRONMENT METRIX_ETC=${CMAKE_SOURCE_DIR}/etc)
    if(USE_XPRESS)
        set_property(TEST ${FULL_TEST_NAME} APPEND PROPERTY ENVIRONMENT LD_LIBRARY_PATH=${XPRESS_ROOT}/lib)
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
        -P ${CMAKE_SOURCE_DIR}/cmake/tnr.cmake
        WORKING_DIRECTORY ${WORKING_DIR})
    set_property(TEST ${FULL_TEST_NAME} PROPERTY ENVIRONMENT METRIX_ETC=${CMAKE_SOURCE_DIR}/etc)
    if(USE_XPRESS)
        set_property(TEST ${FULL_TEST_NAME} APPEND PROPERTY ENVIRONMENT LD_LIBRARY_PATH=${XPRESS_ROOT}/lib)
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
    if(USE_XPRESS)
        set_property(TEST ${FULL_TEST_NAME} APPEND PROPERTY ENVIRONMENT LD_LIBRARY_PATH=${XPRESS_ROOT}/lib)
    endif()
    message(AUTHOR_WARNING "No check on output files performed: outputs of test \"" ${FULL_TEST_NAME} "\" must be checked manually")
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
        -P ${CMAKE_SOURCE_DIR}/cmake/tnr.cmake
        WORKING_DIRECTORY ${WORKING_DIR})
    set_property(TEST ${FULL_TEST_NAME} PROPERTY ENVIRONMENT HADES_DIR=.)
    set_property(TEST ${FULL_TEST_NAME} APPEND PROPERTY ENVIRONMENT METRIX_ETC=${CMAKE_SOURCE_DIR}/etc)
    if(USE_XPRESS)
        set_property(TEST ${FULL_TEST_NAME} APPEND PROPERTY ENVIRONMENT LD_LIBRARY_PATH=${XPRESS_ROOT}/lib)
    endif()
endmacro()

add_subdirectory(sirius)
add_subdirectory(cbc)
if (USE_XPRESS)
    add_subdirectory(xpress)
    configure_file(${CMAKE_CURRENT_SOURCE_DIR}/CTestCustom.cmake ${CMAKE_BINARY_DIR}/tests/CTestCustom.cmake)
endif()
