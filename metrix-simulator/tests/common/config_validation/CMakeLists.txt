#
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#

MESSAGE("Configuration validation tests...")

set(TEST_DIR_NAME config_validation)
set(TEST_DIR ${MAIN_TEST_DIR}/${TEST_DIR_NAME})

# Out-of-range solver choices must be rejected whatever the build flags
metrix_test_config_error("solverch_hors_plage" "lors de la lecture des param")
metrix_test_config_error("pcsolverch_hors_plage" "lors de la lecture des param")

if(NOT USE_ORTOOLS)
    # Without OR-Tools support, only SIRIUS (5) is allowed
    metrix_test_config_error("solverch_sans_ortools" "lors de la lecture des param")
endif()
if(NOT USE_XPRESS)
    # XPRESS (6) requires a binary compiled with USE_XPRESS
    metrix_test_config_error("solverch_xpress_non_compile" "lors de la lecture des param")
endif()
