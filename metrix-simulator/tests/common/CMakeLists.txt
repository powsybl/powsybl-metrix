#
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#

# Solver-independent test suite: every test here fails during configuration or
# input validation, or runs a pure load-flow, and therefore never reaches a
# solver call. Tests that do reach a solve belong to the sirius and xpress
# suites instead.

set(MAIN_TEST_DIR ${CMAKE_CURRENT_SOURCE_DIR})
set(TEST_SOLVERCH common)

add_subdirectory(config_validation)
add_subdirectory(connexite)
add_subdirectory(effacements)
add_subdirectory(groupes)
add_subdirectory(pertes)
