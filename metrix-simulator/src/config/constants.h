//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#pragma once

#include <cmath>

// Traces PNE/SPX
#define TRACES_PNE NON_PNE   /*NON_PNE*/
#define PNE_PRESOLVE OUI_PNE /*OUI_PNE*/

namespace config
{
/**
 * @brief Namespace describing compile-time constants
 */
namespace constants
{
#ifdef M_PI
constexpr double pi = M_PI;
#else
constexpr double pi = 3.14159265358979323846;
#endif

constexpr int valdef = 99999;

constexpr unsigned int nb_max_termes_moyen_constraints = 30; // allocation initiale de la matrice de Contraintes =
                                                             // nb_max_termes_moyen_constraints * nb_Contraintes
constexpr double threshold_not_connex = 1.e-9;      // a partrir de cette valeur du denominateur du calcul des rho,
                                                    // on declare lincident comme incident qui fait perdre la connexite
constexpr unsigned int nb_active_constraints = 300; // taille d'allocation de la table des contraintes actives
constexpr unsigned int nb_max_constraints
    = 5000; // nombre max de contraintes ajoutees pour le simplexe au total lors de la resolution
constexpr unsigned int nb_max_contraints_by_iteration
    = 200; // Limitation du nombre de contraintes par itération (y compris parades)

constexpr unsigned int factor_max_size_dodu = 3;     // ce coeff *nbQuadripole = taille du vecteur de contrainte (DODU)
constexpr unsigned int nb_ouvrages_by_incident = 10; // nombre max d ouvrage par incident

constexpr float max_diff = 10000;
constexpr double acceptable_diff = 1.e-2; // seuil de detection d'une contrainte transit (optimiste)

constexpr double zero_power = 0.001;    // seuil a partir duquel la puissance perdue est considere nulle
constexpr double epsilon_coupe = 1.e-8; // seuil a partir duquel les coeff d'une coupe sont nuls (historiquement 1e-20)
constexpr double parameter_ktd = 0.1;   // partie du quadripole support correspondant au TD
constexpr double zero_cost = 1.e-8;     // valeur minimale pour laquelle le cout reduit est consideré nul
constexpr double pne_factor_inactive_constraint
    = 1.e+4; // pour pne, majorant qui multiplie a une variable entiere permet de desactiver une contrainte

constexpr bool limit_action_td = false;           // Limitation du rayon d'action des TDs
constexpr bool limit_action_hvdc = false;         // Limitation du rayon d'action des HVDCs
constexpr double threshold_influence_td = 0.01;   // 1% : Influence limite en % du transit max pour l'action d'un TD
constexpr double threshold_influence_hvdc = 0.01; // 1% : Influence limite en % du transit max pour l'action d'une HVDC
constexpr double epsilon = 1.e-3;        // epsilon used for floating comparaison (greater than numeric limits to avoid
                                         // creating too much constraints)
constexpr double epsilon_bilan = 1.;     // seuil de précision des bilans
constexpr double epsilon_threat = 1.e-3; // seuil de précision des menaces max

constexpr double threshold_test = 1.e-4; // seuil du controle de verification des transits
constexpr double threshold_heuristic_parade
    = 1.1; // On supprime les parades quand le transit après vaut 110% du transit avant
constexpr double constraints_precision = 1.e10; // précision des coefficients dans la matrice des contraintes

constexpr bool eval_parade = true; // Evalue la parade avant de l'insérer dans le pb (0: non, 1: oui)

constexpr double epsilon_constraint_eq
    = 1.e-7; // seuil a partir duquel les coeffs de 2 contraintes de parades sont considérés égaux

constexpr double cost_whole_variable = 0.;
constexpr double zero_cost_variable = 1.e-5; // below this threshold, cost for variabke is considered equal to 0
constexpr double cost_parade
    = 1.e-2; // légère pénalisation de la parade dans la fonction coût pour différencier les parades équivalentes
constexpr double singularisation_parade = 0.; // légère pénalisation du Tmax pour les parades

constexpr double display_margin_variation_threshold = 0.000005;

const std::string battery_type = "BATTERY";

} // namespace constants
} // namespace config