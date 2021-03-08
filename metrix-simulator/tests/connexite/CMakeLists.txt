# 
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# All rights reserved.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, you can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
# 

#
# Copyright (c) 2019, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#

MESSAGE("Connexity checks and issues tests...")

set(TEST_DIR_NAME connexite)
set(TEST_DIR ${MAIN_TEST_DIR}/${TEST_DIR_NAME})
set(EXPECTED_TEST_DIR ${MAIN_TEST_DIR}_reference/${TEST_DIR_NAME})

metrix_test("composante_non_connexe" 1)
metrix_test("connexite_base_variant" 1)
metrix_test("noeud_connexe_isole" 2)
metrix_test("incidents_piquage" 1)
metrix_test("incident_rompant_connexite" 4)
metrix_test("pb_inc_rompant_connex" 3)
metrix_test("incident_rompant_connexite_avec_preventif" 6)
metrix_test("incident_rompant_connexite_avec_curatif" 6)
metrix_test("incidents_rompant_connexite_avec_parades" 2)
metrix_test("incident_ne_rompt_plus_connexite" 2)
metrix_test("inc_connexite_et_quad_deconnecte" 2)
metrix_test("parade_cree_2_noeuds_isoles" 3)
metrix_test("reprise_partielle_poche_en_curatif" 7)
metrix_test("mapping_sectionneurs" 3)

