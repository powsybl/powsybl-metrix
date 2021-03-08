#
# Copyright (c) 2019, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#

MESSAGE("Remedial action tests...")

set(TEST_DIR_NAME curatif)
set(TEST_DIR ${MAIN_TEST_DIR}/${TEST_DIR_NAME})
set(EXPECTED_TEST_DIR ${MAIN_TEST_DIR}_reference/${TEST_DIR_NAME})

metrix_test("contraintes_equivalentes" 1)
metrix_test("parades" 10)
metrix_test("parades_et_TD" 10)
metrix_test("limitation_nb_actions" 4)
metrix_test("parade_ouverture_couplage" 6)
metrix_test("parade_ouverture_ligne_contrainte" 4)
metrix_test("fermeture_ligne" 2)
metrix_test("fermeture_ligne_et_TD_en_curatif" 3)
metrix_test("parade_rompant_connexite" 5)
metrix_test("contrainte_cree_par_parade" 2)
metrix_test("meilleure_parade_va_dans_le_mauvais_sens" 2)
metrix_test("parades_pas_vraiment_equivalentes" 2)
metrix_test("parade_inverse_transit" 2)
metrix_test("restriction_parade_sur_contrainte" 5)
metrix_test("parade_ouverture_interrupteur" 3)
metrix_test("ouverture_groupe_et_conso" 3)
metrix_test("ouverture_groupe_et_conso_avec_proba" 3)
metrix_test_no_check("couplage_boucle_sur_meme_noeud" 1) # no check here because of equivalent solutions and random
