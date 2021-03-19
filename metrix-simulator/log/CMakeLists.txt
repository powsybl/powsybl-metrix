# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

message(STATUS "Log librairie")

set(Boost_USE_STATIC_LIBS   ON)
set(Boost_USE_MULTITHREADED ON)
find_package(Boost 1.66.0 REQUIRED COMPONENTS log system filesystem)

set(LOG_SOURCES
    src/logger.cpp
    )

add_library(log STATIC ${LOG_SOURCES})
target_include_directories(log
    PUBLIC
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
    $<INSTALL_INTERFACE:include>

    Boost::system
    Boost::log
    Boost::filesystem

    PRIVATE
    src
    )

target_link_libraries(log
    Boost::system
    Boost::log
    Boost::filesystem
)

add_library(metrix::log ALIAS log)
