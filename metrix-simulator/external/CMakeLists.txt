#
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#


cmake_minimum_required(VERSION 3.9.6 FATAL_ERROR)

set(CMAKE_BUILD_TYPE "Release" CACHE STRING "Choose the type of build, options are: Debug Release RelWithDebInfo MinSizeRel (CMake defaults)")

if(POLICY CMP0074)
    cmake_policy(SET CMP0074 NEW)
endif()

# Add custom cmake modules to the path
set(CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../cmake")

project(Metrix-external CXX C) # language is required to identify compiler ID and version

set(DOWNLOAD_DIR ${CMAKE_CURRENT_BINARY_DIR}/src CACHE PATH "Directory where 3rd parties are downloaded.")
set(TMP_DIR ${CMAKE_CURRENT_BINARY_DIR}/tmp CACHE PATH "Directory where 3rd parties temporary files are created.")

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED true)

include(ExternalProject)
include(GetPatchCommand)
include(CloneUrl)

include(CMakeDependentOption)

# ============================================================================
# SuiteSparse
# ============================================================================

set(suitesparse_url https://github.com/DrTimothyAldenDavis/SuiteSparse.git)
set(suitesparse_tag_version v5.4.0)

if(DEFINED ENV{NNI} AND DEFINED ENV{NNI_PASSWORD})
    get_url(NAME suitesparse URL ${suitesparse_url} NNI $ENV{NNI} PASSWORD $ENV{NNI_PASSWORD})
    set(suitesparse_url ${SUITEPARSE_URL})
endif()

GetPatchCommand(${CMAKE_CURRENT_SOURCE_DIR} suitesparse)

if(SuiteSparse_FOUND)
    message(STATUS "SuiteSparse Already found")
    add_custom_target(suitesparse)
else()
    ExternalProject_Add(suitesparse
        INSTALL_DIR         ${CMAKE_CURRENT_BINARY_DIR}/suitesparse
        TMP_DIR             ${TMP_DIR}
        STAMP_DIR           ${DOWNLOAD_DIR}/suitesparse-stamp
        SOURCE_DIR          ${DOWNLOAD_DIR}/suitesparse
        BINARY_DIR          ${DOWNLOAD_DIR}/suitesparse-build
        GIT_REPOSITORY      ${suitesparse_url}
        GIT_TAG             refs/tags/${suitesparse_tag_version}
        PATCH_COMMAND       ${suitesparse_patch}
        CMAKE_CACHE_ARGS    -DCMAKE_C_COMPILER:STRING=${CMAKE_C_COMPILER}
        CMAKE_ARGS          -DCMAKE_INSTALL_PREFIX:PATH=<INSTALL_DIR>
                            -DCMAKE_BUILD_TYPE:STRING=${CMAKE_BUILD_TYPE}
                            -DCMAKE_POLICY_VERSION_MINIMUM:STRING=3.5
    )
    ExternalProject_Get_Property(suitesparse install_dir)
    set(SUITESPARSE_HOME ${install_dir})
endif()

# ============================================================================
# Sirius
# ============================================================================

set(sirius_solver_url https://github.com/rte-france/sirius-solver.git)

if(DEFINED ENV{NNI} AND DEFINED ENV{NNI_PASSWORD})
    get_url(NAME sirius URL ${sirius_solver_url} NNI $ENV{NNI} PASSWORD $ENV{NNI_PASSWORD})
    set(sirius_solver_url ${SIRIUS_URL})
endif()

set(sirius_solver_ROOT ${CMAKE_CURRENT_BINARY_DIR}/sirius_solver)
find_package(sirius_solver QUIET)
if(sirius_solver_FOUND)
    message(STATUS "Sirius solver already found")
else()
    ExternalProject_Add(sirius_solver
        INSTALL_DIR         ${CMAKE_CURRENT_BINARY_DIR}/sirius_solver
        TMP_DIR             ${TMP_DIR}
        STAMP_DIR           ${DOWNLOAD_DIR}/sirius_solver-stamp
        SOURCE_DIR          ${DOWNLOAD_DIR}/sirius_solver
        BINARY_DIR          ${DOWNLOAD_DIR}/sirius_solver-build
        GIT_REPOSITORY      ${sirius_solver_url}
        GIT_TAG             origin/metrix
        CMAKE_CACHE_ARGS    -DCMAKE_CXX_COMPILER:STRING=${CMAKE_CXX_COMPILER}
        CMAKE_ARGS          -DCMAKE_INSTALL_PREFIX:PATH=<INSTALL_DIR>
                            -DCMAKE_BUILD_TYPE:STRING=${CMAKE_BUILD_TYPE}
    )
    ExternalProject_Get_Property(sirius_solver install_dir)
    set(sirius_solver_ROOT ${install_dir})
    set(sirius_solver_static_ROOT ${install_dir})
endif()

# ============================================================================
# OR-Tools (conditional)
#
# Builds the OR-Tools RTE wrapper (rte-france/or-tools-rte).
#
# OR-Tools manages its own dependencies (abseil, protobuf, etc.)
# via BUILD_DEPS=ON.
# ============================================================================

option(USE_ORTOOLS "Build or-tools as external dependency" OFF)
cmake_dependent_option(USE_XPRESS "Build or-tools with Xpress backend (requires XPRESS_ROOT)" OFF "USE_ORTOOLS" OFF)

if(USE_ORTOOLS)
    set(ortools_rte_url https://github.com/rte-france/or-tools-rte.git)
    set(ortools_rte_tag "v9.13-rte1.3")

    if(DEFINED ENV{NNI} AND DEFINED ENV{NNI_PASSWORD})
        get_url(NAME ortools URL ${ortools_rte_url} NNI $ENV{NNI} PASSWORD $ENV{NNI_PASSWORD})
        set(ortools_rte_url ${ORTOOLS_URL})
    endif()

    set(ortools_ROOT ${CMAKE_CURRENT_BINARY_DIR}/ortools)
    find_package(ortools CONFIG QUIET)
    if(ortools_FOUND)
        message(STATUS "OR-Tools already found")
        add_custom_target(ortools)
    else()
        set(ORTOOLS_XPRESS_ARGS "")
        if(USE_XPRESS)
            if(NOT XPRESS_ROOT AND DEFINED ENV{XPRESS_ROOT})
                set(XPRESS_ROOT $ENV{XPRESS_ROOT})
            endif()
            if(NOT XPRESS_ROOT)
                message(FATAL_ERROR "USE_XPRESS=ON requires XPRESS_ROOT to be set (cmake variable or environment variable)")
            endif()
            list(APPEND ORTOOLS_XPRESS_ARGS
                -DUSE_XPRESS:BOOL=ON
                -DXPRESS_ROOT:PATH=${XPRESS_ROOT}
            )
            message(STATUS "OR-Tools will be built with Xpress backend (XPRESS_ROOT=${XPRESS_ROOT})")
        else()
            list(APPEND ORTOOLS_XPRESS_ARGS -DUSE_XPRESS:BOOL=OFF)
            message(STATUS "OR-Tools will be built without Xpress backend")
        endif()

        ExternalProject_Add(ortools
            INSTALL_DIR         ${CMAKE_CURRENT_BINARY_DIR}/ortools
            TMP_DIR             ${TMP_DIR}
            STAMP_DIR           ${DOWNLOAD_DIR}/ortools-stamp
            SOURCE_DIR          ${DOWNLOAD_DIR}/ortools
            BINARY_DIR          ${DOWNLOAD_DIR}/ortools-build
            GIT_REPOSITORY      ${ortools_rte_url}
            GIT_TAG             ${ortools_rte_tag}
            CMAKE_CACHE_ARGS    -DCMAKE_CXX_COMPILER:STRING=${CMAKE_CXX_COMPILER}
                                -DCMAKE_C_COMPILER:STRING=${CMAKE_C_COMPILER}
            CMAKE_ARGS          -DCMAKE_INSTALL_PREFIX:PATH=<INSTALL_DIR>
                                -DCMAKE_BUILD_TYPE:STRING=${CMAKE_BUILD_TYPE}
                                -DBUILD_DEPS:BOOL=ON
                                -DUSE_SIRIUS:BOOL=OFF
                                -Dortools_REPO:STRING=https://github.com/google/or-tools.git
                                -Dortools_REF:STRING=v9.13
                                ${ORTOOLS_XPRESS_ARGS}
        )
        ExternalProject_Get_Property(ortools install_dir)
        set(ortools_ROOT ${install_dir})
    endif()
endif()
