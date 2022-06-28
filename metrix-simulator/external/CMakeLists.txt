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

set(suitesparse_version  5.4.0)
set(suitesparse_name     SuiteSparse-${suitesparse_version}.tar.gz)
set(suitesparse_prefix_url http://faculty.cse.tamu.edu/davis/SuiteSparse)
set(suitesparse_url      ${suitesparse_prefix_url}/${suitesparse_name})
set(suitesparse_md5      4A6D4E74FC44C503F52996AE95CAD03A)

set(SUITESPARSE_HOME ${CMAKE_CURRENT_BINARY_DIR}/suitesparse)
find_package(SuiteSparse QUIET)
unset(SUITESPARSE_HOME)

GetPatchCommand(${CMAKE_CURRENT_SOURCE_DIR} suitesparse)
if(SuiteSparse_FOUND)
  message(STATUS "SuiteSparse Already found")
  add_custom_target(suitesparse)
else()
  ExternalProject_Add(suitesparse
    INSTALL_DIR         ${CMAKE_CURRENT_BINARY_DIR}/suitesparse
    DOWNLOAD_DIR        ${CMAKE_CURRENT_SOURCE_DIR}/suitesparse
    TMP_DIR             ${TMP_DIR}
    STAMP_DIR           ${DOWNLOAD_DIR}/suitesparse-stamp
    SOURCE_DIR          ${DOWNLOAD_DIR}/suitesparse
    BINARY_DIR          ${DOWNLOAD_DIR}/suitesparse-build
    URL                 ${suitesparse_url}
    URL_MD5             ${suitesparse_md5}
    PATCH_COMMAND       ${suitesparse_patch}

    CMAKE_CACHE_ARGS    -DCMAKE_C_COMPILER:STRING=${CMAKE_C_COMPILER}

    CMAKE_ARGS          -DCMAKE_INSTALL_PREFIX:PATH=<INSTALL_DIR>
                        -DCMAKE_BUILD_TYPE:STRING=${CMAKE_BUILD_TYPE}
  )
  ExternalProject_Get_Property(suitesparse install_dir)
  set(SUITESPARSE_HOME ${install_dir})
endif()

set(sirius_solver_url https://github.com/jdarlay/sirius-solver)
set(sirius_tag "metrix-integration-v0.1")
set(sirius_asset "ubuntu-20.04_sirius-solver.zip")

set(sirius_solver_ROOT ${CMAKE_CURRENT_BINARY_DIR}/sirius_solver)
find_package(sirius_solver QUIET)
unset(sirius_solver_ROOT)

if(sirius_solver_FOUND)
  message(STATUS "Sirius solver already found")
else()
  ExternalProject_Add(sirius_solver
    INSTALL_DIR         ${CMAKE_CURRENT_BINARY_DIR}/sirius_solver
    TMP_DIR             ${TMP_DIR}
    STAMP_DIR           ${DOWNLOAD_DIR}/sirius_solver-stamp
    SOURCE_DIR          ${CMAKE_CURRENT_BINARY_DIR}/sirius_solver
    BINARY_DIR          ${DOWNLOAD_DIR}/sirius_solver-build
    URL                 ${sirius_solver_url}/releases/download/${sirius_tag}/${sirius_asset}
    CONFIGURE_COMMAND   ""
    BUILD_COMMAND       ""
    INSTALL_COMMAND     ""
    )
  ExternalProject_Get_Property(sirius_solver install_dir)
  set(sirius_solver_ROOT ${install_dir})
  set(sirius_solver_static_ROOT ${install_dir})
endif()

set(ortools_url https://github.com/jdarlay/or-tools)
set(ortools_tag v9.2-ls_dev1.0.1)

option(USE_XPRESS "Use XPRESS in ortools" OFF)
if (USE_XPRESS)
  set(ortools_asset "ortools_cxx_ubuntu-20.04_shared_sirius-metrix_xprs.zip")
else()
  set(ortools_asset "ortools_cxx_ubuntu-20.04_shared_sirius-metrix.zip")
endif()

set(ortools_ROOT ${CMAKE_CURRENT_BINARY_DIR}/ortools)
find_package(ortools QUIET)
unset(ortools_ROOT)

if(ortools_FOUND)
  message(STATUS "Ortools already found")
else()
  ExternalProject_Add(ortools
    INSTALL_DIR         ${CMAKE_CURRENT_BINARY_DIR}/ortools
    TMP_DIR             ${TMP_DIR}
    STAMP_DIR           ${DOWNLOAD_DIR}/ortools-stamp
    SOURCE_DIR          ${CMAKE_CURRENT_BINARY_DIR}/ortools
    BINARY_DIR          ${DOWNLOAD_DIR}/ortools-build
    URL                 ${ortools_url}/releases/download/${ortools_tag}/${ortools_asset}
    CONFIGURE_COMMAND   ""
    BUILD_COMMAND       ""
    INSTALL_COMMAND     ""
    )
  ExternalProject_Get_Property(ortools install_dir)
  set(ortools_ROOT ${install_dir})
endif()
