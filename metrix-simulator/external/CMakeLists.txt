#
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#

# Minimal CMake required version
cmake_minimum_required(VERSION 3.14 FATAL_ERROR)

# Project description
project(Metrix-external
        DESCRIPTION "External libraries used by metrix-simulator"
        LANGUAGES CXX C) # language is required to identify compiler ID and version

# Activate new behaviour of the policy if necessary
if(POLICY CMP0074)
  cmake_policy(SET CMP0074 NEW)
endif()

# Type of build
set(CMAKE_BUILD_TYPE "Release" CACHE STRING "Choose the type of build, options are: Debug Release RelWithDebInfo MinSizeRel (CMake defaults)")

# Add custom cmake modules to the path
set(CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../cmake")

# Set a few directories
set(DOWNLOAD_DIR ${CMAKE_CURRENT_BINARY_DIR}/src CACHE PATH "Directory where 3rd parties are downloaded.")
set(TMP_DIR ${CMAKE_CURRENT_BINARY_DIR}/tmp CACHE PATH "Directory where 3rd parties temporary files are created.")

# Enable C++11 standards
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED true)

# Include extra modules
include(ExternalProject) # Used to download and build external projects
include(GetPatchCommand) # Used to get the name of a patch file
include(CloneUrl) # Used to adapt the URL to use login/password (method "get_url")

# ================================ SuiteSparse =============================================
# URL and version
set(suitesparse_url https://github.com/DrTimothyAldenDavis/SuiteSparse.git)
set(suitesparse_tag_version v7.10.1)

# Adapt the URL if needed with NNI (the login) and NNI_PASSWORD (the password)
if(DEFINED ENV{NNI} AND DEFINED ENV{NNI_PASSWORD})
  get_url(NAME suitesparse URL ${suitesparse_url} NNI $ENV{NNI} PASSWORD $ENV{NNI_PASSWORD})
  set(suitesparse_url ${SUITEPARSE_URL})
endif()

# Get the path file name as "suitesparse_patch"
GetPatchCommand(${CMAKE_CURRENT_SOURCE_DIR} suitesparse)

# Build SuiteSparse only if it is not already present
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
    LIST_SEPARATOR      ^^
    CMAKE_ARGS          -DCMAKE_INSTALL_PREFIX:PATH=<INSTALL_DIR>
                        -DCMAKE_BUILD_TYPE:STRING=${CMAKE_BUILD_TYPE}
                        -DSUITESPARSE_ENABLE_PROJECTS=suitesparse_config^^amd^^colamd^^btf^^klu
                        -DKLU_USE_CHOLMOD:BOOL=OFF
                        -DSUITESPARSE_REQUIRE_BLAS:BOOL=OFF
  )
  ExternalProject_Get_Property(suitesparse install_dir)
  set(SUITESPARSE_HOME ${install_dir})
endif()

# ================================ Sirius_solver =============================================
# URL
set(sirius_solver_url https://github.com/rte-france/sirius-solver.git)

# Adapt the URL if needed with NNI (the login) and NNI_PASSWORD (the password)
if(DEFINED ENV{NNI} AND DEFINED ENV{NNI_PASSWORD})
  get_url(NAME sirius URL ${sirius_solver_url} NNI $ENV{NNI} PASSWORD $ENV{NNI_PASSWORD})
  set(sirius_solver_url ${SIRIUS_URL})
endif()

# Build SuiteSparse only if it is not already present
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