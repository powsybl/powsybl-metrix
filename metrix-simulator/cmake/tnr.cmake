# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

function(check_file file expected_file)
    configure_file(${file} ${file} NEWLINE_STYLE LF) # required for windows ctest
    execute_process( COMMAND ${CMAKE_COMMAND} -E compare_files ${file} ${expected_file}
        RESULT_VARIABLE compare_result)
    if(compare_result)
        MESSAGE(FATAL_ERROR "File " ${file} " is different from expected file " ${expected_file})
    endif()
endfunction()

function(check_files files_ expected_files_)
    list(LENGTH files_ length_files)
    list(LENGTH expected_files_ length_expected_files)
    if(NOT ${length_files} EQUAL ${length_expected_files})
        MESSAGE(FATAL_ERROR "Number of output files " ${length_files} " different from expected " ${length_expected_files})
    endif()
    message(STATUS "Number of output files to compare: " ${length_files})
    if(${length_expected_files} EQUAL 0)
        # No files to compare
        return()
    endif()
    math(EXPR len "${length_files} - 1")
    foreach(idx RANGE ${len})
        message(STATUS "Processing output file index: " ${idx})
        list(GET files_ ${idx} out)
        list(GET expected_files_ ${idx} expected_out)
        check_file(${out} ${expected_out})
    endforeach()
endfunction()

execute_process(COMMAND ${EXE} metrixOut.txt VariantSet.csv out 0 ${NB_TESTS}
    RESULT_VARIABLE cmd_result)
if(cmd_result)
    message(FATAL_ERROR "Error running: ${EXE} returns " ${cmd_result})
endif()
file(GLOB test_output_files ${WORKING_DIR}/out_*)
file(GLOB expected_output_files ${EXPECTED_DIR}/out_*)
check_files("${test_output_files}" "${expected_output_files}")

if(ALL_RESULTS)
    check_file(${WORKING_DIR}/metrixOut.txt ${EXPECTED_DIR}/metrixOut.txt)
endif()
