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

project(MetrixSimulator 
    VERSION 1.0.0
    LANGUAGES C CXX)

cmake_policy(SET CMP0074 NEW) # Use <package>_ROOT variables to find package

message("Build directory : ${CMAKE_BINARY_DIR}")
message("Source directory : ${CMAKE_SOURCE_DIR}")

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED true)

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

add_subdirectory(LU)
add_subdirectory(PNE)
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
        src/margin_variations_compute.cpp
    )

set(Boost_USE_STATIC_LIBS   ON)
set(Boost_USE_MULTITHREADED ON)
find_package(Boost 1.66.0 REQUIRED COMPONENTS system program_options log filesystem)

set(target "metrix-simulator")

configure_file(${CMAKE_CURRENT_SOURCE_DIR}/cmake/version.h.in ${CMAKE_CURRENT_SOURCE_DIR}/src/config/version.h)

add_executable(${target} 
${METRIX_HEADERS} ${METRIX_SOURCES})

target_include_directories(${target}
    PRIVATE
    metrix::lu
    metrix::pne
    metrix::log
    src
    ${Boost_INCLUDE_DIRS}
)
target_link_libraries(${target}
    PRIVATE
    metrix::lu
    metrix::pne
    metrix::log
    
    ${Boost_LIBRARIES}
)

install(TARGETS ${target} RUNTIME DESTINATION bin)
install(DIRECTORY ${PROJECT_SOURCE_DIR}/etc DESTINATION .)

set(CODE_COVERAGE FALSE CACHE BOOL "Enable code coverage")
if (CODE_COVERAGE)
    message(STATUS "CODE COVERAGE")
	set(CMAKE_BUILD_TYPE "Debug")
    list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}")
    include(cmake/CodeCoverage)
    code_coverage(NAME code-coverage
        OUTPUT_DIR coverage
        EXCLUDE_DIRS LU PNE tests
        DEPENDENCIES metrix-simulator
    )
endif()

enable_testing()
add_subdirectory(tests)
