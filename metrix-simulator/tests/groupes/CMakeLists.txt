#
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#

MESSAGE("Generator and redispatching tests...")

set(TEST_DIR_NAME groupes)
set(TEST_DIR ${MAIN_TEST_DIR}/${TEST_DIR_NAME})
set(EXPECTED_TEST_DIR ${MAIN_TEST_DIR}_reference/${TEST_DIR_NAME})

metrix_test("config_adequacy_vs_redispatching" 3)
metrix_test("cost_offsets" 5)
metrix_test("high_cost_offsets" 5)
metrix_test("redispatching" 4)
metrix_test("defaut_groupes" 3)
metrix_test("curatif_groupes" 10)
metrix_test("curatif_groupe_2_defauts" 3)
metrix_test("curatif_groupes_et_parade" 7)
metrix_test("curatif_groupes_et_defaut_groupe" 3)
if(METRIX_RUN_ALL_TESTS)
metrix_test("pompage_hr_necessaire" 1)
endif()
metrix_test("limitation_curatif" 5)
metrix_test("noise_cost" 3)
