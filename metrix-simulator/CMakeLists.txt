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

# Include helper functions for creating config files
include(CMakePackageConfigHelpers)

# Project description
project(MetrixSimulator
    VERSION 7.4.0
    LANGUAGES C CXX)

# Explicitly activate policies
cmake_policy(SET CMP0074 NEW) # Use <package>_ROOT variables to find package
if(POLICY CMP0167)
  # The new version of this policy removes the FindBoost.cmake and uses the official Boost Cmake support
  # However it's only available after the version 1.70+ so, since we ask for version 1.66+, we need the old policy
  cmake_policy(SET CMP0167 OLD)
endif()

# Show build and source directories
message("-- Build directory : ${CMAKE_BINARY_DIR}")
message("-- Source directory : ${CMAKE_SOURCE_DIR}")

# Enable C++11 standards
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED true)

# Specify some parameters depending on if it's run on the compiler
if(MSVC)
  add_definitions(-D_WIN32_WINNT=0x0A00)
  add_compile_definitions("_CRT_SECURE_NO_WARNINGS")
  set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} /STACK:5000000 ")
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
  # In release, we prefer speed over size of the code
  set(CMAKE_CXX_FLAGS_RELEASE "/O2")
  set(CMAKE_C_FLAGS_RELEASE "/O2")
  set(Boost_USE_STATIC_RUNTIME ON)
else()
  if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
  endif()
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -pedantic -Werror -pthread")
  set(CMAKE_CXX_FLAGS_DEBUG "-g -O0")
  set(CMAKE_CXX_FLAGS_RELEASE "-O3")
  set(CMAKE_C_FLAGS_RELEASE "-O3")
endif(MSVC)

# Add custom cmake modules to the path
set(CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/cmake")

# Specify where the cmake files will be installed
set(INSTALL_CMAKE_DIR cmake CACHE PATH "Installation directory for cmake files")

# ==================================== Boost =================================================
set(Boost_USE_STATIC_LIBS   ON)
set(Boost_USE_MULTITHREADED ON)
find_package(Boost 1.66.0 REQUIRED COMPONENTS system program_options log filesystem)

# ================================ Sirius_solver =============================================
# Add an option to use or not shared library for sirius solver
option(USE_SIRIUS_SHARED "Use shared library for sirius solver" OFF)
if (USE_SIRIUS_SHARED)
  message("-- Use shared sirius_solver in ${sirius_solver_DIR}")
  set(SIRIUS_NAME sirius_solver)
else()
  message("-- Use static sirius_solver in ${sirius_solver_static_DIR}")
  set(SIRIUS_NAME sirius_solver_static)
endif()

# Define already existing sirius installation path
set(${SIRIUS_NAME}_ROOT ${CMAKE_CURRENT_BINARY_DIR}/external/sirius_solver CACHE PATH "Path where a Sirius installation already exists")

# Find the sirius_solver package
find_package(${SIRIUS_NAME} REQUIRED)

# ================================= SuiteSparse ==============================================
# Add suitesparse cmake directory to the path
set(SUITESPARSE_DIR ${CMAKE_CURRENT_BINARY_DIR}/external/suitesparse CACHE PATH "Path where an SuiteSparse installation already exists")
list(APPEND CMAKE_PREFIX_PATH "${SUITESPARSE_DIR}/lib/cmake")

# Find the required SuiteSparse packages
find_package(AMD REQUIRED)
find_package(COLAMD REQUIRED)
find_package(BTF REQUIRED)
find_package(KLU REQUIRED)

# Find the actual library name
find_library(SUITESPARSECONFIG_LIB
        NAMES suitesparseconfig SuiteSparse_config
        PATHS ${SUITESPARSE_DIR}/lib
        REQUIRED
)

# Create an "imported" targer if it does not already exists
if(NOT TARGET SuiteSparse::config)
    add_library(SuiteSparse::config UNKNOWN IMPORTED)
    set_target_properties(SuiteSparse::config PROPERTIES
            IMPORTED_LOCATION "${SUITESPARSECONFIG_LIB}"
            INTERFACE_INCLUDE_DIRECTORIES "${SUITESPARSE_DIR}/include"
    )
endif()

# ================================= The rest ==============================================
# Add the log directory to the build
add_subdirectory(log)

message(STATUS "METRIX")

# Define required headers
set(METRIX_HEADERS
        src/calcul.h
        src/err/error.h
        src/parametres.h
        src/reseau.h
        src/err/IoDico.h
        src/prototypes.h
        src/variante.h
        src/pne.h
        src/config/configuration.h
        src/config/input_configuration.h
        src/config/variant_configuration.h
        src/config/parades_configuration.h
        src/config/converter.h
        src/status.h
        src/config/constants.h
        src/config/version_def.h
        src/cte.h
        src/options/options.h
        src/config/version.h
        src/compute/solver.h
        src/margin_variations_compute.h
    )

# Define required source files
set(METRIX_SOURCES
        src/calculmacrofonctions.cpp
        src/err/error.cpp
        src/reseau.cpp
        src/calculrepports.cpp
        src/calculecrirecontraintesdodu.cpp
        src/connexite.cpp
        src/err/IoDico.cpp
        src/calculeEmpilementGroupes.cpp
        src/metrix.cpp
        src/metrix2assess.cpp
        src/config/configuration.cpp
        src/config/input_configuration.cpp
        src/config/variant_configuration.cpp
        src/config/parades_configuration.cpp
        src/config/converter.cpp
        src/config/version_def.cpp
        src/cte.cpp
        src/options/options.cpp
        src/compute/solver.cpp
        src/margin_variations_compute.cpp
    )

# Define binary executable file name
set(target "metrix-simulator")

# Generate a version.h file
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/cmake/version.h.in ${CMAKE_CURRENT_SOURCE_DIR}/src/config/version.h)

# Create the executable
add_executable(${target}
  ${METRIX_HEADERS} ${METRIX_SOURCES})

# Add the required directories for the executable
target_include_directories(${target}
    PRIVATE
    metrix::log
    src
    ${SIRIUS_NAME}
    ${Boost_INCLUDE_DIRS}
)

# Add the required libraries
target_link_libraries(${target}
    PRIVATE
    metrix::log
    ${SIRIUS_NAME}
    SuiteSparse::config
    SuiteSparse::AMD
    SuiteSparse::COLAMD
    SuiteSparse::BTF
    SuiteSparse::KLU
    ${Boost_LIBRARIES}
    $<$<BOOL:${MSVC}>:msvcrt.lib>
)

# Checks whether the shared library for Sirius is available and installs it
if (USE_SIRIUS_SHARED)
    # Export sirius library if using shared
    get_target_property(sirius_PATH_LIB_RELEASE sirius_solver IMPORTED_LOCATION_RELEASE)
    get_target_property(sirius_PATH_LIB_DEBUG sirius_solver IMPORTED_LOCATION_DEBUG)
    if (EXISTS ${sirius_PATH_LIB_RELEASE})
        set(sirius_PATH_LIB ${sirius_PATH_LIB_RELEASE})
    elseif(EXISTS ${sirius_PATH_LIB_DEBUG})
        set(sirius_PATH_LIB ${sirius_PATH_LIB_DEBUG})
    else()
        # shouldn't happen if find_package worked
        message(FATAL_ERROR "sirius_solver imported location for debug or release was not found: check external installation")
    endif()
    install(FILES ${sirius_PATH_LIB} DESTINATION lib)
endif()

# Option to enable code coverage (disabled by default)
set(CODE_COVERAGE FALSE CACHE BOOL "Enable code coverage")
if (CODE_COVERAGE)
    message(STATUS "CODE COVERAGE") # Output message for code coverage
	set(CMAKE_BUILD_TYPE "Debug") # Switch to Debug build type
    list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}") # Add current source to module path
    include(cmake/CodeCoverage) # Include custom code coverage script
    CODE_COVERAGE(NAME code-coverage
        OUTPUT_DIR coverage
        ROOT_DIR src
        EXCLUDE_DIRS tests external
        DEPENDENCIES metrix-simulator
    )
endif()

# Install SuiteSparse config files
install(FILES "${SUITESPARSECONFIG_LIB}" DESTINATION lib)

# Install the executable to bin directory
install(TARGETS ${target} EXPORT targets RUNTIME DESTINATION bin)
# Install the CMake configuration files
install(EXPORT targets
    FILE metrix-simulator-config.cmake
    DESTINATION ${INSTALL_CMAKE_DIR}
)
# Install 'etc' directory to the root of install
install(DIRECTORY ${PROJECT_SOURCE_DIR}/etc DESTINATION .)

# Option to run a reduced scope of tests
option(METRIX_RUN_ALL_TESTS "Run reduced scope of tests" ON)

# Enable CTest for running tests
enable_testing()

# Add the tests subdirectory, which contains the tests
add_subdirectory(tests)

# Generate a basic package version file for the installed package
write_basic_package_version_file(
    "${CMAKE_CURRENT_BINARY_DIR}/metrix-simulator-config.cmake"
    VERSION ${MetrixSimulator_VERSION}
    COMPATIBILITY SameMajorVersion
)

# Install the generated version file to the CMake install directory
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/metrix-simulator-config.cmake"
    DESTINATION ${INSTALL_CMAKE_DIR}
)
