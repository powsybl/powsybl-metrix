# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

cmake_minimum_required(VERSION 3.12 FATAL_ERROR)

include(CMakePackageConfigHelpers)

project(MetrixSimulator 
    VERSION 7.4.0
    LANGUAGES C CXX)

cmake_policy(SET CMP0074 NEW) # Use <package>_ROOT variables to find package

include(CMakeDependentOption)

message("Build directory : ${CMAKE_BINARY_DIR}")
message("Source directory : ${CMAKE_SOURCE_DIR}")

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED true)
if (${CMAKE_VERSION} VERSION_GREATER_EQUAL "3.12")
    cmake_policy(SET CMP0074 NEW) # Use <package>_ROOT variables for find package
endif()
cmake_policy(SET CMP0028 NEW) # Fatal error if "::" link is not imported / alias target

add_compile_definitions(
    USE_XPRESS=1 # Use express
    USE_SIRIUS=1 # Use sirius
)

set(CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/cmake")
set(INSTALL_CMAKE_DIR cmake CACHE PATH "Installation directory for cmake files")

if(MSVC)
  add_compile_definitions(WIN32)
  set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} /STACK:5000000 ")
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}")
  # In release, we prefer speed over size of the code
  set(CMAKE_CXX_FLAGS_RELEASE "/O2")
  set(CMAKE_C_FLAGS_RELEASE "/O2")
else()
  if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
  endif()
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -pedantic -Werror -pthread")
  set(CMAKE_CXX_FLAGS_DEBUG "-g -O0")
  set(CMAKE_CXX_FLAGS_RELEASE "-O3")
  set(CMAKE_C_FLAGS_RELEASE "-O3")
endif(MSVC)

if(NOT ortools_ROOT)
    message(WARNING "ortools_ROOT not defined: may not find package ortools")
else()
    message(STATUS "ortools_ROOT=${ortools_ROOT}")
endif()

set(Boost_USE_STATIC_LIBS   ON)
set(Boost_USE_MULTITHREADED ON)
find_package(Boost 1.66.0 REQUIRED COMPONENTS system program_options log filesystem)

# case policy of ortools root cannot be done (policy CMP0074)
if (${CMAKE_VERSION} VERSION_LESS "3.12")
    list(APPEND CMAKE_PREFIX_PATH "${ortools_ROOT}")
endif()

set(GFLAGS_USE_TARGET_NAMESPACE TRUE)
find_package(ortools CONFIG REQUIRED)
find_package(XPRESS REQUIRED)

option(USE_ORTOOLS "Toggle or-tools usage" OFF) # disable by default
cmake_dependent_option(USE_SIRIUS_SHARED "Use shared library for sirius solver" OFF "NOT USE_ORTOOLS" ON)

if (USE_SIRIUS_SHARED)
    set(SIRIUS_NAME sirius_solver)
else()
    set(SIRIUS_NAME sirius_solver_static)
endif()

set(${SIRIUS_NAME}_ROOT ${CMAKE_CURRENT_BINARY_DIR}/external/sirius_solver CACHE PATH "Path where a Sirius installation already exists")
find_package(${SIRIUS_NAME} REQUIRED)

# SUITESPARSE
set(SUITESPARSE_HOME ${CMAKE_CURRENT_BINARY_DIR}/external/suitesparse CACHE PATH "Path where an SuiteSparse installation already exists")
find_package(SuiteSparse REQUIRED)

add_subdirectory(log)

message(STATUS "METRIX")

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
        src/ortools/solver.h
    )

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
        src/ortools/solver.cpp
        src/margin_variations_compute.cpp
    )

set(target "metrix-simulator")

configure_file(${CMAKE_CURRENT_SOURCE_DIR}/cmake/version.h.in ${CMAKE_CURRENT_SOURCE_DIR}/src/config/version.h)

add_executable(${target} 
${METRIX_HEADERS} ${METRIX_SOURCES})

target_include_directories(${target}
    PRIVATE
    metrix::log
    ortools::ortools
    XPRESS::XPRESS
    sirius_solver
    src
    ${SIRIUS_NAME}
    SuiteSparse::SuiteSparse_KLU
    SuiteSparse::SuiteSparse_AMD
    SuiteSparse::SuiteSparse_COLAMD
    SuiteSparse::SuiteSparse_BTF
    SuiteSparse::SuiteSparse_Config
    ${Boost_INCLUDE_DIRS}
)
target_link_libraries(${target}
    PRIVATE
    metrix::log
    ortools::ortools
    XPRESS::XPRESS
    ${SIRIUS_NAME}
    SuiteSparse::SuiteSparse_KLU
    SuiteSparse::SuiteSparse_AMD
    SuiteSparse::SuiteSparse_COLAMD
    SuiteSparse::SuiteSparse_BTF
    SuiteSparse::SuiteSparse_Config
    ${Boost_LIBRARIES}
)

if(USE_ORTOOLS)
    target_compile_definitions(${target} PRIVATE USE_ORTOOLS)
endif()

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

set(CODE_COVERAGE FALSE CACHE BOOL "Enable code coverage")
if (CODE_COVERAGE)
    message(STATUS "CODE COVERAGE")
	set(CMAKE_BUILD_TYPE "Debug")
    list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}")
    include(cmake/CodeCoverage)
    CODE_COVERAGE(NAME code-coverage
        OUTPUT_DIR coverage
        ROOT_DIR src
        EXCLUDE_DIRS tests external
        DEPENDENCIES metrix-simulator
    )
endif()

install(TARGETS ${target} EXPORT targets RUNTIME DESTINATION bin)
install(EXPORT targets
    FILE metrix-simulator-config.cmake
    DESTINATION ${INSTALL_CMAKE_DIR}
)
install(DIRECTORY ${PROJECT_SOURCE_DIR}/etc DESTINATION .)

option(METRIX_RUN_ALL_TESTS "Run reduced scope of tests" ON)

enable_testing()
add_subdirectory(tests)


write_basic_package_version_file(
    "${CMAKE_CURRENT_BINARY_DIR}/metrix-simulator-config.cmake"
    VERSION ${MetrixSimulator_VERSION}
    COMPATIBILITY SameMajorVersion
)
install(FILES "${CMAKE_CURRENT_BINARY_DIR}/metrix-simulator-config.cmake"
    DESTINATION ${INSTALL_CMAKE_DIR}
)
