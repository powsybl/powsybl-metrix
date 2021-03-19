# Copyright (C) 2021, RTE (http://www.rte-france.com)
# See AUTHORS.txt
# SPDX-License-Identifier: Apache-2.0

message(STATUS "LU Solver")

set(LU_SOURCES
    src/lu_allouer_lu.c 
    src/lu_refactorisation.c  
    src/lu_selection_pivot_diagonal.c 
    src/lu_calculs_markowitz.c 
    src/lu_refactorisation_de_la_matrice.c 
    src/lu_selection_pivot_matrice_pleine.c 
    src/lu_classement_lignes_colonnes.c 
    src/lu_reorganisation_chainages.c 
    src/lu_super_lignes_calculs_hashcode.c 
    src/lu_construire_probleme.c 
    src/lu_resolution.c 
    src/lu_super_lignes_creations_et_fusions.c 
    src/lu_elimination_une_ligne.c 
    src/lu_resolution_hyper_creux.c 
    src/lu_super_lignes_declenchement.c 
    src/lu_elimination_une_ligne_cas_symetrique.c 
    src/lu_scaling.c 
    src/lu_super_lignes_detection.c 
    src/lu_elimination_une_ligne_matrice_pleine.c 
    src/lu_scan_ligne.c 
    src/lu_super_lignes_merge_toutes_super_lignes.c 
    src/lu_elimination_une_ligne_turbo.c 
    src/lu_scan_ligne_cas_symetrique.c 
    src/lu_super_lignes_modifications.c 
    src/lu_factorisation.c 
    src/lu_scan_ligne_matrice_pleine.c 
    src/lu_super_lignes_reallocs.c 
    src/lu_factorisation_calculs.c 
    src/lu_scan_super_ligne.c 
    src/lu_switch_markowitz.c 
    src/lu_inconnues_indeterminees.c 
    src/lu_scan_super_ligne_cas_symetrique.c 
    src/lu_update.c 
    src/lu_init_tableaux_null.c 
    src/lu_selection_pivot.c 
    src/memstats.c 
    src/memstats.h 
    src/lu_memoire.h 
)

add_library(lu STATIC ${LU_SOURCES})
target_include_directories(lu 
    PUBLIC
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
    $<INSTALL_INTERFACE:include>
    PRIVATE
    src
)

add_library(metrix::lu ALIAS lu)
