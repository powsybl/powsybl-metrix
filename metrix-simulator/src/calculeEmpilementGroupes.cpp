//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#include "calcul.h"
#include "config/configuration.h"
#include "config/constants.h"
#include "cte.h"
#include "err/IoDico.h"
#include "err/error.h"
#include "pne.h"
#include "prototypes.h"
#include "status.h"
#include "variante.h"

#include <algorithm>
#include <cmath>
#include <iomanip>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>

using cte::c_fmt;


using std::fixed;
using std::ios;
using std::max;
using std::min;
using std::ostringstream;
using std::setprecision;
using std::setw;
using std::string;
using std::vector;

int Calculer::empilementEconomiqueDesGroupes(const std::shared_ptr<Variante>& varianteCourante)
{
    // Le programme est constitue de trois partie
    // (i)  calcul du second mombre (b), Sum (consommation), Sum (prod_Imposee), Sum (prod_Max)
    //      et voir s il existe un prod dont la pmin est diff de zero
    // (ii) s il y aun Pmin diff de zero ou (Sum (consommation) > Sum (prod_Imposee), Sum (prod_Max))
    //      alors en utilise Pnsolveur
    //(iii) sinon on utilise un empilement classique.

    // Hypothses : on suppose que toutes les variables de consommations sont initialisees a zero
    //             ceci est valable pour le premier passage

    double puissanceAdemarrer
        = 0.0; // second membre pour l'empilement economique = consommation - prod_Imposee -prodPobj
    double sumProdImposee = 0.0;

    double sumConsommations = 0.0;
    double sumProductionsMax = 0.0;
    double sumProductionsMin = 0.0;
    bool UnePminDifferenteDeZero = false; // si il existe un prode non impose dont la Pmin est diff de zero alors true.
    bool UnePobjDifferenteDeZero = false; // si il existe un prode non impose dont la Pobj est diff de zero alors true.
    vector<std::shared_ptr<Groupe>> groupesTries(res_.nbGroupes_);


    // I-calcul du second membre (b) + consommation prod_Imposee prod_Max
    //------------------------------------------------------------------
    for (size_t i = 0; i < res_.numNoeudBilanParZone_.size(); ++i) {
        res_.bilanParZone_[static_cast<int>(i)] = 0.;
    }


    for (const auto& grpPair : res_.groupes_) {
        auto& grp = grpPair.second;

        // initialisation de la table de pointeur de groupes a trier
        groupesTries.push_back(grp);

        // si le groupe n'est pas connecte au reseau
        if (!grp->etat_) {
            continue;
        }

        sumProdImposee += grp->prodPobj_;
        puissanceAdemarrer -= grp->prodPobj_;
        res_.bilanParZone_[grp->noeud_->numCompSynch_] += grp->prodPobj_;

        if (grp->prodAjust_ == Groupe::OUI_HR_AR || grp->prodAjust_ == Groupe::OUI_HR) {
            sumProductionsMax += grp->puisMax_ - grp->prodPobj_;
            sumProductionsMin += grp->prodPobj_ - grp->puisMin_;
            if (grp->puisMin_ != 0.0) {
                UnePminDifferenteDeZero = true;
            }
            if (grp->prodPobj_ != 0.0) {
                UnePobjDifferenteDeZero = true;
            }
        }
    }

    double coutDelestageMin = res_.nbConsos_ > 0 ? res_.consos_.cbegin()->second->cout_ : 0;

    for (int i = 0; i < res_.nbNoeuds_; ++i) {
        const auto& noeud = res_.noeuds_[i];

        for (int j = 0; j < noeud->nbConsos_; ++j) {
            const auto& conso = noeud->listeConsos_[j];
            coutDelestageMin = coutDelestageMin < conso->cout_ ? coutDelestageMin : conso->cout_;

            puissanceAdemarrer += conso->valeur_;
            sumConsommations += conso->valeur_;
            res_.bilanParZone_[noeud->numCompSynch_] -= conso->valeur_;
        }

        for (int j = 0; j < noeud->nbCC(); ++j) {
            const auto& lcc = noeud->listeCC_[j];
            res_.bilanParZone_[noeud->numCompSynch_] += (lcc->norqua_ == noeud) ? -lcc->puiCons_ : lcc->puiCons_;
        }
    }

    bool equilibrageNonNecessaire = fabs(puissanceAdemarrer) < config::constants::epsilon;

    std::stringstream ss;
    ss << err::ioDico().msg("INFOEmpilementEconomique") << std::endl;
    ss << err::ioDico().msg("INFOConsommationGlobale", c_fmt("%10.4f ", sumConsommations)) << std::endl;
    ss << err::ioDico().msg("INFOProdImposee", c_fmt("%10.4f ", sumProdImposee)) << std::endl;

    // cout<<" prod imposee negative "<<sumProdImposeeNegative <<endl<< std::endl;
    ss << err::ioDico().msg("INFOPuissanceAdemarrer", c_fmt("%10.4f ", puissanceAdemarrer)) << std::endl;
    ss << err::ioDico().msg("INFOPuissanceMaximaleDemarrableHorsPimp", c_fmt("%.4f ", sumProductionsMax)) << std::endl;
    ss << err::ioDico().msg("INFOPuissanceMaximaleArretableHorsPimp", c_fmt("%.4f ", -sumProductionsMin)) << std::endl;

    ss << err::ioDico().msg("INFOBilanParZoneSync") << std::endl;
    for (const auto& pair : res_.bilanParZone_) {
        int numNoeudBilan = res_.numNoeudBilanParZone_.find(pair.first)->second;

        ss << err::ioDico().msg("INFOBilanZoneSync",
                                c_fmt("%d", pair.first),
                                res_.noeuds_[numNoeudBilan]->print(),
                                c_fmt("%.4f", pair.second))
           << std::endl;

        equilibrageNonNecessaire &= fabs(pair.second) < config::constants::epsilon;
    }
    LOG_ALL(info) << ss.str();

    // II-tests d'incoherences
    //-----------------------
    if (equilibrageNonNecessaire) {
        // on doit quand meme ajuster la taille de ces vecteurs
        pbCoutsMarginauxDesContraintes_.resize(pbNombreDeContraintes_);
        pbComplementDeLaBase_.resize(pbNombreDeContraintes_);
        pbCoutsReduits_.resize(pbNombreDeVariables_);

        return METRIX_PAS_PROBLEME;
    }
    if (sumProdImposee - sumProductionsMin > sumConsommations) {
        LOG_ALL(error) << err::ioDico().msg("ERRPimpSupConso");
        return METRIX_PAS_SOLUTION;
    }

    if (sumProdImposee + sumProductionsMax < sumConsommations) {
        LOG_ALL(warning) << err::ioDico().msg("WARNPimpInfConso");
    }

    // III- Use PneSolveur
    //---------------------------------
    if (UnePminDifferenteDeZero) {
        LOG_ALL(info) << err::ioDico().msg("INFOSPXPminSupZero");
    } else if (UnePobjDifferenteDeZero) {
        LOG_ALL(info) << err::ioDico().msg("INFOSPXPObjSupZero");
    } else if (fabs(sumProdImposee + sumProductionsMax) < fabs(sumConsommations)) {
        LOG_ALL(info) << err::ioDico().msg("INFOSPXDelestageNecessaire");
    } else if (res_.numNoeudBilanParZone_.size() > 1) {
        LOG_ALL(info) << err::ioDico().msg("INFOSPXPlusieursZonesSynch");
    } else if (config::configuration().usePenalisationTD() || config::configuration().usePenalisationHVDC()) {
        LOG_ALL(info) << err::ioDico().msg("INFOSPXPenalisationTDHVDC");
    }

    // utilisation de PneSolveur
    int status = METRIX_PAS_PROBLEME;
    status = PneSolveur(UTILISATION_SIMPLEXE, varianteCourante);
    if (status == METRIX_PROBLEME) {
        LOG_ALL(error) << err::ioDico().msg("ERRAppelSpx");
        LU_LibererMemoireLU(jacFactorisee_);
        return status;
    }
    if (status == METRIX_PAS_SOLUTION) {
        LOG_ALL(error) << err::ioDico().msg("ERRPasDeSolEmpilEco");
        return status;
    }
    return METRIX_PAS_PROBLEME;
}
