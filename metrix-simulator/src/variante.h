//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//


#ifndef LOI_METRIX
#define LOI_METRIX


#include "reseau.h"

#include <memory>

//----------------
// Classe VARIANTE
//----------------

// Remarque :
// lors de l introduction de nouveaux type de variante
// il faudra mettre a jour l'optimisation du recalcul :
// A - des coeffs de report
// B - des coeffs d influencement
//


class Variante
{
public:
    int num_ = 0;

    std::set<std::shared_ptr<Groupe>> grpIndispo_; /*liste des groupes indisponibles*/


    Quadripole::SetQuadripoleSortedByName indispoLignes_; /* liste des lignes indisponibles et son comparateur*/


    std::map<std::shared_ptr<Groupe>, double> prodImpose_; /*production imposee : numero grp, valeur*/
    std::map<std::shared_ptr<Groupe>, double> prodMax_;    /*pmax dispo : numero grp, valeur*/
    std::map<std::shared_ptr<Groupe>, double> prodMin_;    /*pmin dispo : numero grp, valeur*/

    std::map<int, double> valeurEchange_; /*echange via conso : numero de region, valeur */
    std::map<int, double> varBilanProd_;  /* valeur de bilan d'une region a ajuster via la production */

    std::map<std::shared_ptr<Consommation>, double> valeurConso_; /*consommation variable : numero conso, valeur */
    std::map<std::shared_ptr<Consommation>, double> coutEfface_;  /*cout effacement conso : numero conso, cout */

    std::map<std::shared_ptr<Groupe>, double> grpHausseHR_; /*cout a la hausse HR: numero grp, valeur*/
    std::map<std::shared_ptr<Groupe>, double> grpBaisseHR_; /*cout a la baisse HR: numero grp, valeur*/
    std::map<std::shared_ptr<Groupe>, double> grpHausseAR_; /*cout a la hausse AR: numero grp, valeur*/
    std::map<std::shared_ptr<Groupe>, double> grpBaisseAR_; /*cout a la baisse AR: numero grp, valeur*/

    std::map<std::shared_ptr<LigneCC>, double> dcPuissMin_; /*puissance minimale de liaisons DC*/
    std::map<std::shared_ptr<LigneCC>, double> dcPuissMax_; /*puissance minimale de liaisons DC*/
    std::map<std::shared_ptr<LigneCC>, double> dcPuissImp_; /*consigne de puissance imposée de la liaison DC*/

    std::map<std::shared_ptr<TransformateurDephaseur>, double> dtValDep_; /*dephasage initial du TD*/

    std::map<std::shared_ptr<Incident>, double> probabinc_; /* probabilite de défaut */

    std::map<std::shared_ptr<ElementASurveiller>, double> quati00mn_; /* seuil max N (regime permanent) */
    std::map<std::shared_ptr<ElementASurveiller>, double> quati5mns_; /* seuil max N-1 (regime incident) */
    std::map<std::shared_ptr<ElementASurveiller>, double> quati20mn_; /* seuil max N-k (regime incident complexe) */
    std::map<std::shared_ptr<ElementASurveiller>, double> quatitamn_; /* seuil max avant curatif (ITAM) */
    std::map<std::shared_ptr<ElementASurveiller>, double>
        quatitamk_; /* seuil max avant curatif (regime incident complexe) */

    // Seuils Extremité vers Origine
    std::map<std::shared_ptr<ElementASurveiller>, double> quati00mnExOr_; /* seuil max N Ext -> Or */
    std::map<std::shared_ptr<ElementASurveiller>, double> quati5mnsExOr_; /* seuil max N-1 Ext -> Or */
    std::map<std::shared_ptr<ElementASurveiller>, double> quati20mnExOr_; /* seuil max N-k Ext -> Or */
    std::map<std::shared_ptr<ElementASurveiller>, double> quatitamnExOr_; /* seuil max avant curatif Ext -> Or */
    std::map<std::shared_ptr<ElementASurveiller>, double>
        quatitamkExOr_; /* seuil max avant curatif Ext -> Or (regime incident complexe) */
    
    // Liste des groupes renvoyées par le shuffle de calculecrirecontraintesdodu.cpp en G++9:
    //(utilisée pour les tests)
    std::vector<std::shared_ptr<Groupe>> randomGroups_;

    int nbGrpIndispo() const { return static_cast<int>(grpIndispo_.size()); }
    int nbProdImposee() const { return static_cast<int>(prodImpose_.size()); }
    int nbProdMax() const { return static_cast<int>(prodMax_.size()); }
    int nbProdMin() const { return static_cast<int>(prodMin_.size()); }
    int nbEchange() const { return static_cast<int>(valeurEchange_.size()); }
    int nbBilanProd() const { return static_cast<int>(varBilanProd_.size()); }
    int nbConsommation() const { return static_cast<int>(valeurConso_.size()); }
    int nbCoutEfface() const { return static_cast<int>(coutEfface_.size()); }
    int nbIndispoLignes() const { return static_cast<int>(indispoLignes_.size()); }
    int nbDcPuissMin() const { return static_cast<int>(dcPuissMin_.size()); }
    int nbDcPuissMax() const { return static_cast<int>(dcPuissMax_.size()); }
    int nbDcPuissImp() const { return static_cast<int>(dcPuissImp_.size()); }
    int nbDtValDep() const { return static_cast<int>(dtValDep_.size()); }
};

/**
 * @brief Comparator between quadripoles sets
 *
 * First it compares size, following the natural order. Then it uses the key comparator to compare
 * side by side the elements of each set
 */
struct CompareSet {
    using set = Quadripole::SetQuadripoleSortedByName;
    bool operator()(const set& lhs, const set& rhs) const
    {
        if (lhs.size() < rhs.size()) {
            return true;
        }

        if (lhs.size() > rhs.size()) {
            return false;
        }

        auto lit = lhs.begin();
        auto rit = rhs.begin();

        while (lit != lhs.end()) {
            if (lhs.key_comp()(*lit, *rit)) {
                return true;
            }
            if (rhs.key_comp()(*rit, *lit)) {
                return false;
            }
            ++lit;
            ++rit;
        }

        return false;
    }
};

#endif
