#
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#

set(MAIN_TEST_DIR ${CMAKE_CURRENT_SOURCE_DIR})
set(TEST_SOLVERCH xpress)

add_subdirectory(divers)
add_subdirectory(connexite)
add_subdirectory(curatif)
add_subdirectory(effacements)
add_subdirectory(hvdc)
add_subdirectory(td)
add_subdirectory(groupes)
add_subdirectory(load_flow)
add_subdirectory(pertes)
add_subdirectory(variables_couplees)
add_subdirectory(seuils)
add_subdirectory(variantes_regroupees)
add_subdirectory(overload)
add_subdirectory(LODF_PTDF)
