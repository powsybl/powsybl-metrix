#
# Copyright (c) 2019, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#

MESSAGE("Reassembling Variant tests...")

set(TEST_DIR_NAME variantes_regroupees)
set(TEST_DIR ${MAIN_TEST_DIR}/${TEST_DIR_NAME})
set(EXPECTED_TEST_DIR ${MAIN_TEST_DIR}_reference/${TEST_DIR_NAME})

metrix_test("variantes_regroupees_complexe" 11) # no check here because of random and equivalent solutions
metrix_test("variantes_regroupees_simple" 8)
