# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

MESSAGE("Load shedding and curtainment tests...")

set(TEST_DIR_NAME effacements)
set(TEST_DIR ${MAIN_TEST_DIR}/${TEST_DIR_NAME})
set(EXPECTED_TEST_DIR ${MAIN_TEST_DIR}_reference/${TEST_DIR_NAME})

metrix_test("delestage_HR" 4)
metrix_test("delestage_HR_cible" 4)
metrix_test("delestage_preventif" 4)
metrix_test("delestage_preventif_cible" 4)
metrix_test("effacement_curatif" 8)
metrix_test("effacement_batterie" 1)
metrix_test("delestage_charges_negatives" 2)
if(METRIX_RUN_ALL_TESTS)
metrix_test_no_check("curatif_sans_preventif" 4) # order of display
endif()