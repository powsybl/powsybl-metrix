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
#include "config/input_configuration.h"
#include "cte.h"
#include "err/IoDico.h"
#include "err/error.h"
#include "pne.h"
#include "prototypes.h"
#include "reseau.h"
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

const double COEFF_MULT = 1e-3;

// Pour reduire la precision des coefficients utilises dans les calculs
double Calculer::round(double x, double prec)
{
    if (x > 0) {
        x = floor(x * prec + 0.5) / prec;
    } else if (x < 0) {
        x = ceil(x * prec - 0.5) / prec;
    }

    return x;
}

std::ostream& operator<<(std::ostream& str, const ElementCuratif::TypeElement a)
{
    switch (a) {
        case ElementCuratif::TD: return str << "TD";
        case ElementCuratif::TD_FICTIF: return str << "TD_FICTIF";
        case ElementCuratif::HVDC: return str << "HVDC";
        case ElementCuratif::GROUPE: return str << "GROUPE";
        case ElementCuratif::CONSO: return str << "CONSO";
        default: return str << "Unknown";
    }
}

bool estEnDepassement(const std::shared_ptr<ElementASurveiller>& elemSurv,
                      const std::shared_ptr<Incident>& icdt,
                      double transit)
{
    double seuil = fabs(elemSurv->seuil(icdt, transit));
    return (seuil != config::constants::valdef) && (fabs(transit) - seuil > config::constants::acceptable_diff);
}

bool estEnDepassementAvantManoeuvre(const std::shared_ptr<ElementASurveiller>& elemSurv, double transit)
{
    double seuil = elemSurv->seuilMaxAvantCur_;
    if (elemSurv->seuilsAssymetriques_) {
        seuil = transit >= 0 ? elemSurv->seuilMaxAvantCur_ : -elemSurv->seuilMaxAvantCurExOr_;
    }
    return (seuil != config::constants::valdef) && (fabs(transit) - seuil > config::constants::acceptable_diff);
}


int Calculer::allocationProblemeDodu()
{
    // ---------------------1/3-----------------------------------
    // nombre de Variables (dans lordre du vecteur des inconnues) =
    // 2*nombre de groupes +
    // nombre de noeuds ou il y a au moins une consos +
    //------------------------------------------------------------

    pbNombreDeVariables_ = res_.nbVarGroupes_ + res_.nbVarConsos_ + res_.nbVarTd_ + res_.nbVarCc_;
    // cout<<"-->pbNombreDeVariables_ : "<<pbNombreDeVariables_<<endl;
    if (pbNombreDeVariables_ == 0) {
        LOG_ALL(info) << err::ioDico().msg("ERRCalcVarnul");
        return METRIX_PROBLEME;
    }

    unsigned int taille = pbNombreDeVariables_;

    if (!problemeAlloue_) { // le premier cas traite
        pbTypeDeVariable_.resize(taille, REEL);
        pbTypeDeBorneDeLaVariable_.resize(taille, VARIABLE_BORNEE_DES_DEUX_COTES);
        pbX_.resize(taille, 0);
        pbXhR_.resize(taille, 0);
        pbXmin_.resize(taille, 0);
        pbXmax_.resize(taille, 0);
        pbCoutLineaire_.resize(taille, 0);
        pbCoutLineaireSansOffset_.resize(taille, 0);

        // pour le simplexe seulement
        pbPositionDeLaVariable_.resize(taille, HORS_BASE_A_ZERO);
        pbCoutsReduits_.resize(taille, 0.);
    } else { // pour les variantes
        pbTypeDeVariable_.assign(taille, REEL);
        pbTypeDeBorneDeLaVariable_.assign(taille, VARIABLE_BORNEE_DES_DEUX_COTES);
        pbX_.assign(taille, 0.0);
        pbXhR_.assign(taille, 0.0);
        pbXmin_.assign(taille, 0.0);
        pbXmax_.assign(taille, 0.0);
        pbCoutLineaire_.resize(taille, 0.);
        pbCoutLineaireSansOffset_.resize(taille, 0);

        pbPositionDeLaVariable_.assign(taille, HORS_BASE_A_ZERO);
        pbCoutsReduits_.assign(taille, 0.);

        // on supprime les variables curatives ajoutee
        typeEtat_.resize(taille);
        numSupportEtat_.resize(taille);
    }

    // ---------------------2/3---------------------------------------------------------------------------------
    // nombre de Variables (dans l ordre du vecteur des inconues) =
    // 2*nombre de groupes +
    // nombre de noeuds ou il y a au moins une consos
    // res_.nbVarGroupes_ + res_.nbVarConsos_ ;
    // ---------------------------------------------------------------------------------------------------------
    /*
    if (numMicroIteration_<=1){
        for(unsigned int i = 0; i < taille; ++i){
            pbTypeDeVariable_[i]          = REEL;
            pbCoutLineaire_[i]            = 0.0; initialisation, mise a jour ulterieurement.
            pbTypeDeBorneDeLaVariable_[i] = VARIABLE_BORNEE_DES_DEUX_COTES;
        }
    }
    */

    // ---------------------3/3-----------------------------------
    // pbNombreDeContraintes_ :une seule contrainte
    //------------------------------------------------------------

    videMatricesDesContraintes();

    if (!problemeAlloue_) {
        for (auto icdtIt = res_.incidentsEtParades_.cbegin(); icdtIt != res_.incidentsEtParades_.end(); ++icdtIt) {
            if ((*icdtIt)->validite_) {
                calculInitCoefs(*icdtIt);
            }
        }
    }
    if (problemeAlloue_) {
        lienParadeValorisation_.clear();
    }

    problemeAlloue_ = true;

    return METRIX_PAS_PROBLEME;
}


void Calculer::videMatricesDesContraintes()
{
    pbSens_.resize(0);
    pbSecondMembre_.resize(0);
    pbIndicesDebutDeLigne_.resize(0);
    pbNombreDeTermesDesLignes_.resize(0);
    pbContraintes_.resize(0);
    pbTypeContrainte_.resize(0);
    pbCoefficientsDeLaMatriceDesContraintes_.resize(0);
    pbIndicesColonnes_.resize(0);
    pbNombreDeContraintes_ = 0;
    nbElmdeMatrContraint_ = 0;
}


int Calculer::ecrireContraintesDeBordGroupesDodu()
{
    int i1 = -1;
    // traitement des groupes

    // melange des groupes: afin que l'on n'ait pas besoin de bruiter les couts
    std::vector<std::shared_ptr<Groupe>> grp_melanges;
    // Si la variante possède une liste de groupe GROURAND, elle va l'utiliser
    if (!varianteCourante_->randomGroups_.empty()){
        grp_melanges = varianteCourante_->randomGroups_;
    }
    else{
        //Sinon le code prend la liste de base et la mélange lui-même.
        grp_melanges.reserve(res_.nbGroupes_);
        for (auto grpIt = res_.groupes_.cbegin(); grpIt != res_.groupes_.end(); ++grpIt) {
            grp_melanges.push_back(grpIt->second);
        }
        std::shuffle(grp_melanges.begin(), grp_melanges.end(), Reseau::random);
    }

    for (int i = 0; i < res_.nbGroupes_; ++i) {
        const auto& grp = grp_melanges[i];

        if (grp->prodAjust_ == Groupe::NON_HR_AR) {
            continue;
        }

        i1++;
        grp->numVarGrp_ = 2 * i1;
        int numVar = grp->numVarGrp_;
        numSupportEtat_[numVar] = grp->numNoeud_;
        numSupportEtat_[numVar + 1] = grp->numNoeud_;

        if (grp->etat_ && (grp->prodAjust_ == Groupe::OUI_HR_AR || grp->prodAjust_ == Groupe::OUI_HR)) {
            if (grp->puisMin_ >= 0) {
                if (grp->prod_ < 0) {
                    LOG_ALL(error) << err::ioDico().msg("ERRPminSupPc", grp->nom_);
                    return METRIX_PROBLEME;
                }
                // P_min<=P_0
                if (grp->puisMin_ <= grp->prodPobj_) {
                    // A la hausse
                    pbXmin_[numVar] = 0.0;
                    pbXmax_[numVar] = grp->puisMax_ - grp->prodPobj_;
                    pbCoutLineaire_[numVar] = std::max(grp->coutHausseHR_, config::configuration().noiseCost())
                                              + config::configuration().adequacyCostOffset();
                    // A la baisse
                    pbXmin_[numVar + 1] = 0.0;
                    pbXmax_[numVar + 1] = grp->prodPobj_ - grp->puisMin_;
                    pbCoutLineaire_[numVar + 1] = std::max(grp->coutBaisseHR_, config::configuration().noiseCost())
                                                  + config::configuration().adequacyCostOffset();

                } else { // P_min>P_0
                    // A la hausse
                    pbXmin_[numVar] = grp->puisMin_ - grp->prodPobj_;
                    pbXmax_[numVar] = grp->puisMax_ - grp->prodPobj_;
                    pbCoutLineaire_[numVar] = std::max(grp->coutHausseHR_, config::configuration().noiseCost())
                                              + config::configuration().adequacyCostOffset();
                    // A la baisse
                    pbXmin_[numVar + 1] = 0.0;
                    pbXmax_[numVar + 1] = 0.0;
                    pbCoutLineaire_[numVar + 1] = config::configuration().noiseCost();
                }
            } else {
                // P_min<0
                // A la hausse
                pbXmin_[numVar] = 0.;
                pbXmax_[numVar] = grp->puisMax_ - grp->prodPobj_;
                pbCoutLineaire_[numVar] = std::max(grp->coutHausseHR_, config::configuration().noiseCost())
                                          + config::configuration().adequacyCostOffset();
                // A la baisse
                pbXmin_[numVar + 1] = 0.;
                pbXmax_[numVar + 1] = grp->prodPobj_ - grp->puisMin_;
                pbCoutLineaire_[numVar + 1] = std::max(grp->coutBaisseHR_, config::configuration().noiseCost())
                                              + config::configuration().adequacyCostOffset();
            }
        } else { // Groupe deconnecte ou fixe HR
            // A la hausse
            pbXmin_[numVar] = 0.;
            pbXmax_[numVar] = 0.;
            // A la baisse
            pbXmin_[numVar + 1] = 0.;
            pbXmax_[numVar + 1] = 0.;
            pbCoutLineaire_[numVar + 1] = config::configuration().noiseCost();
        }

        if (pbXmax_[numVar] - pbXmin_[numVar] < config::constants::epsilon) {
            pbTypeDeBorneDeLaVariable_[numVar] = VARIABLE_FIXE;
            pbX_[numVar] = pbXmin_[numVar];
        }

        if (pbXmax_[numVar + 1] - pbXmin_[numVar + 1] < config::constants::epsilon) {
            pbTypeDeBorneDeLaVariable_[numVar + 1] = VARIABLE_FIXE;
            pbX_[numVar + 1] = pbXmin_[numVar + 1];
        }
    }

    return METRIX_PAS_PROBLEME;
}

void Calculer::ajoutRedispatchCostOffsetConsos()
{
    const auto& config = config::configuration();
    for (auto cIt = res_.consos_.cbegin(); cIt != res_.consos_.end(); ++cIt) {
        const auto& conso = cIt->second;
        if (conso->numVarConso_ >= 0) {
            if (conso->valeur_ >= 0) {
                pbCoutLineaire_[conso->numVarConso_] = std::max(conso->cout_, config.noiseCost())
                                                       + config.redispatchCostOffset();
                pbCoutLineaireSansOffset_[conso->numVarConso_] = conso->cout_;
            } else {
                // consumption cost is negative so we use min instead of max to compare to cost noise
                pbCoutLineaire_[conso->numVarConso_] = -(std::min(conso->cout_, -config.noiseCost())
                                                         + config.redispatchCostOffset());
                pbCoutLineaireSansOffset_[conso->numVarConso_] = conso->cout_;
            }
        }
    }
}


int Calculer::ecrireContraintesDeBordConsosDodu()
{
    int numVar;
    double consoNodale = 0.;

    for (auto cIt = res_.consos_.cbegin(); cIt != res_.consos_.end(); ++cIt) {
        const auto& conso = cIt->second;
        numVar = conso->numVarConso_;
        if (numVar >= 0) { // La conso peut etre delestee en N

            consoNodale = conso->valeur_;

            if (consoNodale >= 0) {
                pbXmin_[numVar] = 0.0;
                pbXmax_[numVar] = conso->seuil_ * consoNodale;
                pbCoutLineaire_[numVar] = std::max(conso->cout_, config::configuration().noiseCost())
                                          + config::configuration().adequacyCostOffset();
            } else {
                pbXmin_[numVar] = conso->seuil_ * consoNodale;
                pbXmax_[numVar] = 0.0;
                pbCoutLineaire_[numVar] = -(std::max(conso->cout_, config::configuration().noiseCost())
                                            + config::configuration().adequacyCostOffset());
            }

            if (pbXmax_[numVar] - pbXmin_[numVar] < config::constants::epsilon) {
                pbTypeDeBorneDeLaVariable_[numVar] = VARIABLE_FIXE;
                pbX_[numVar] = 0.;
            }
        }
    }
    return METRIX_PAS_PROBLEME;
}

int Calculer::ecrireContraintesDeBordTransformateurDephaseur()
{
    int numVarTd;

    for (auto tdIt = res_.TransfoDephaseurs_.cbegin(); tdIt != res_.TransfoDephaseurs_.end(); ++tdIt) {
        const auto& td = tdIt->second;
        numVarTd = td->numVar_;

        double puiMax;
        double puiMin;
        if (td->type_ == TransformateurDephaseur::PILOTAGE_ANGLE_IMPOSE
            || td->type_ == TransformateurDephaseur::PILOTAGE_PUISSANCE_IMPOSE) {
            puiMax = td->puiCons_;
            puiMin = td->puiCons_;
        } else {
            // Calcul des nouveaux PuiMax et PuiMin en prenant en compte les limitations des prises lowran et uppran
            puiMax = td->getPuiMax();
            if (puiMax < td->puiMax_) {
                LOG_ALL(info) << "le TD " << td->quadVrai_->nom_ << " est limite a la hausse";
                LOG(debug) << metrix::log::verbose_constraints << "TD " << td->quadVrai_->nom_
                           << " : PuiMaxUppRan (prise courante + dtuppran) = " << puiMax << " <= "
                           << " PuiMax " << td->puiMax_;
            }
            puiMin = td->getPuiMin();
            if (td->puiMin_ < puiMin) {
                LOG_ALL(info) << "le TD " << td->quadVrai_->nom_ << " est limite a la baisse";
                LOG(debug) << metrix::log::verbose_constraints << "TD " << td->quadVrai_->nom_
                           << " : PuiMinLowRan (prise courante - dtlowran) = " << puiMin << " >= "
                           << " PuiMin " << td->puiMin_;
            }

            if (puiMax < puiMin) {
                LOG_ALL(warning) << "ERREUR : TD " << td->quadVrai_->nom_
                                 << "(Y=" << td->quadVrai_->y_ * config::constants::parameter_ktd << ") "
                                 << " Xmax = " << puiMax << ", Xmin = " << puiMin;
            }
        }

        // Ajout des variables x+ et x- pour penaliser le dephasage autour de la position initiale
        // x+
        pbX_[numVarTd] = 0.0;
        pbXmin_[numVarTd] = 0.0;
        pbXmax_[numVarTd] = max(puiMax - td->puiCons_, 0.);
        pbCoutLineaire_[numVarTd] = (config::configuration().usePenalisationTD() && !td->fictif_)
                                        ? config::configuration().costTd()
                                        : config::constants::zero_cost_variable;
        pbTypeDeBorneDeLaVariable_[numVarTd] = VARIABLE_BORNEE_DES_DEUX_COTES;

        if (pbXmax_[numVarTd] - pbXmin_[numVarTd] < config::constants::epsilon) {
            pbTypeDeBorneDeLaVariable_[numVarTd] = VARIABLE_FIXE;
        }

        // x-
        pbX_[numVarTd + 1] = 0.0;
        pbXmin_[numVarTd + 1] = 0.0;
        pbXmax_[numVarTd + 1] = max(td->puiCons_ - puiMin, 0.);
        pbCoutLineaire_[numVarTd + 1] = (config::configuration().usePenalisationTD() && !td->fictif_)
                                            ? config::configuration().costTd()
                                            : config::constants::zero_cost_variable;
        pbTypeDeBorneDeLaVariable_[numVarTd + 1] = VARIABLE_BORNEE_DES_DEUX_COTES;


        if (pbXmax_[numVarTd + 1] - pbXmin_[numVarTd + 1] < config::constants::epsilon) {
            pbTypeDeBorneDeLaVariable_[numVarTd + 1] = VARIABLE_FIXE;
        }
    }
    return METRIX_PAS_PROBLEME;
}


int Calculer::ecrireContraintesDeBordLignesCC()
{
    int numVarCc;

    for (auto lccIt = res_.LigneCCs_.cbegin(); lccIt != res_.LigneCCs_.end(); ++lccIt) {
        const auto& lcc = lccIt->second;
        numVarCc = lcc->numVar_;

        // Ajout des variables x+ et x- pour avoir la plus petite variation de transit possible autour de P0
        // x+
        pbX_[numVarCc] = 0.0;
        pbXmin_[numVarCc] = 0.0;
        pbCoutLineaire_[numVarCc] = (config::configuration().usePenalisationHVDC()) ? config::configuration().costHvdc()
                                                                                    : 0.;
        // x-
        pbX_[numVarCc + 1] = 0.0;
        pbXmin_[numVarCc + 1] = 0.0;
        pbCoutLineaire_[numVarCc + 1] = (config::configuration().usePenalisationHVDC())
                                            ? config::configuration().costHvdc()
                                            : 0.;

        if (lcc->type_ == LigneCC::PILOTAGE_PUISSANCE_IMPOSE || lcc->type_ == LigneCC::PILOTAGE_EMULATION_AC) {
            pbXmax_[numVarCc] = 0.;
            pbTypeDeBorneDeLaVariable_[numVarCc] = VARIABLE_FIXE;
            pbXmax_[numVarCc + 1] = 0.;
            pbTypeDeBorneDeLaVariable_[numVarCc + 1] = VARIABLE_FIXE;
        } else {
            pbXmax_[numVarCc] = max(lcc->puiMax_ - lcc->puiCons_, 0.);
            pbTypeDeBorneDeLaVariable_[numVarCc] = VARIABLE_BORNEE_DES_DEUX_COTES;
            pbXmax_[numVarCc + 1] = max(lcc->puiCons_ - lcc->puiMin_, 0.);
            pbTypeDeBorneDeLaVariable_[numVarCc + 1] = VARIABLE_BORNEE_DES_DEUX_COTES;
        }
    }
    return METRIX_PAS_PROBLEME;
}

int Calculer::ecrireContrainteBilanEnergetique(bool parZonesSynchr)
{
    // cout<<"ecrire bilan energetique"<<endl;
    // on ecrit une contrainte de bilan par zone synchrone (afin que les load flow ensuite se passent bien)
    // decrit en plein: les zeros sont decrits ( a revoir eventuellement)
    // A noter : on n'ecrit pas ces contraintes dans la phase Hors reseau car les HVDC font parties du reseau ...
    int nbGroupesN;
    int nbConsosN;
    int nbQuadN;
    int firstZone = parZonesSynchr ? 1 : 0;
    int nbZonesSynch = parZonesSynchr ? static_cast<int>(res_.numNoeudBilanParZone_.size()) : 1;

    int nbTermesNonNuls;
    double secondMembre;
    for (int b = firstZone; b < nbZonesSynch; ++b) {
        nbTermesNonNuls = 0;
        secondMembre = 0.;
        for (int i = 0; i < pbNombreDeVariables_; ++i) {
            if ((typeEtat_[i] == PROD_B) || (typeEtat_[i] == DEPH_H) || (typeEtat_[i] == DEPH_B)
                || (typeEtat_[i] == LIGNE_CC_B)) { // Les autres types de variables n'existent pas encore
                continue;
            }

            if (typeEtat_[i] == LIGNE_CC_H) {
                if (parZonesSynchr) { // donc pas dans la zone 0
                    const auto& lcc = res_.lccParIndice_[numSupportEtat_[i]];

                    int numCSOr = lcc->norqua_->numCompSynch_;
                    int numCSEx = lcc->nexqua_->numCompSynch_;

                    if (numCSOr == numCSEx) {
                        continue;
                    }

                    // Origine
                    if (numCSOr == b) {
                        secondMembre += lcc->puiCons_;

                        if (pbTypeDeBorneDeLaVariable_[i] == VARIABLE_FIXE
                            && pbTypeDeBorneDeLaVariable_[i + 1] == VARIABLE_FIXE) {
                            secondMembre += pbX_[i] - pbX_[i + 1];
                        } else {
                            pbIndicesColonnes_.push_back(i);
                            pbCoefficientsDeLaMatriceDesContraintes_.push_back(-1.0); // LIGNE_CC_H
                            pbIndicesColonnes_.push_back(i + 1);
                            pbCoefficientsDeLaMatriceDesContraintes_.push_back(1.0); // LIGNE_CC_B
                            nbTermesNonNuls += 2;
                        }
                    }
                    // extremite
                    else if (numCSEx == b) {
                        secondMembre -= lcc->puiCons_;

                        if (pbTypeDeBorneDeLaVariable_[i] == VARIABLE_FIXE
                            && pbTypeDeBorneDeLaVariable_[i + 1] == VARIABLE_FIXE) {
                            secondMembre -= pbX_[i] - pbX_[i + 1];
                        } else {
                            pbIndicesColonnes_.push_back(i);
                            pbCoefficientsDeLaMatriceDesContraintes_.push_back(1.0); // LIGNE_CC_H
                            pbIndicesColonnes_.push_back(i + 1);
                            pbCoefficientsDeLaMatriceDesContraintes_.push_back(-1.0); // LIGNE_CC_B
                            nbTermesNonNuls += 2;
                        }
                    }
                }
            } else {
                if (parZonesSynchr && res_.noeuds_[numSupportEtat_[i]]->numCompSynch_ != b) {
                    continue;
                }

                if (typeEtat_[i] == PROD_H) {
                    pbIndicesColonnes_.push_back(i);
                    pbCoefficientsDeLaMatriceDesContraintes_.push_back(1.0);
                    pbIndicesColonnes_.push_back(i + 1);
                    pbCoefficientsDeLaMatriceDesContraintes_.push_back(-1.0);
                    nbTermesNonNuls += 2;
                } else if (typeEtat_[i] == CONSO_D) {
                    if (pbTypeDeBorneDeLaVariable_[i] == VARIABLE_FIXE) {
                        secondMembre -= pbX_[i];
                    } else {
                        pbIndicesColonnes_.push_back(i);
                        pbCoefficientsDeLaMatriceDesContraintes_.push_back(1.0);
                        nbTermesNonNuls++;
                    }
                } else {
                    LOG_ALL(error) << err::ioDico().msg("ERRCalcInterne") << "Contrainte bilan energetique :"
                                   << "typeEtat_ " << typeEtat_[i] << " de la variable numero " << i
                                   << " ne peut pas etre traite";
                    return METRIX_PROBLEME;
                }
            }
        }

        for (int i = 0; i < res_.nbNoeuds_; ++i) {
            const auto& nod = res_.noeuds_[i];

            if (parZonesSynchr && nod->numCompSynch_ != b) {
                continue;
            }

            nbQuadN = nod->nbQuads();
            if (nbQuadN == 0 && nod->nbCC() == 0) {
                LOG_ALL(error) << err::ioDico().msg("ERRCalcInterne") << " attention le noeud " << nod->print()
                               << ", est isole";
                return METRIX_PROBLEME;
            }
            // cout<<"contrainte ne : " << i<<endl;
            // cout<<"------------------- " <<endl;

            nbGroupesN = nod->nbGroupes_;
            nbConsosN = nod->nbConsos_;

            if (nbGroupesN == 0 && nbConsosN == 0) {
                continue;
            }

            // parcours des productions imposees connectees au noeud
            for (int j = 0; j < nbGroupesN; ++j) {
                if (nod->listeGroupes_[j]->etat_) {
                    secondMembre -= nod->listeGroupes_[j]->prodPobj_;
                }
            }

            // Ajout de la conso nodale
            if (nbConsosN > 0) {
                secondMembre += nod->consoNodale();
            }
        }

        if (nbTermesNonNuls > 0) {
            pbSecondMembre_.push_back(secondMembre);
            pbSens_.push_back('=');
            pbIndicesDebutDeLigne_.push_back(nbElmdeMatrContraint_);
            pbTypeContrainte_.push_back(COUPE_BILAN);
            pbContraintes_.push_back(nullptr);
            pbNombreDeTermesDesLignes_.push_back(nbTermesNonNuls);
            nbElmdeMatrContraint_ += nbTermesNonNuls;
            pbNombreDeContraintes_++;
        }
    }

    return METRIX_PAS_PROBLEME;
}


double valeurVariableReference(GroupesCouples::VariableReference varRef, const std::shared_ptr<Groupe>& grp)
{
    std::stringstream ss("Type de variable de reference inconnu : ");
    ss << varRef;
    switch (varRef) {
        case GroupesCouples::VariableReference::PMAX: return grp->puisMax_;
        case GroupesCouples::VariableReference::PMIN: return grp->puisMin_;
        case GroupesCouples::VariableReference::POBJ: return grp->prod_;
        case GroupesCouples::VariableReference::PMAX_POBJ: return grp->puisMax_ - grp->prod_;
        default: throw ErrorI(ss.str());
    }
}

/*
 * Ecriture des contraintes de couplage des variables conso
 */
int Calculer::ajouterContraintesCouplagesConsos()
{
    // Pour chaque element i de la liste hormis l'element 0
    // D(i)/ref(i) = D(0)/ref(0)
    // soit ref(0)*D(i) - ref(i)*D(0) = 0

    for (const auto& binding : res_.consosCouplees_) {
        auto listeIt = binding->elements_.cbegin();
        const auto& consoRef = *listeIt;

        int nbContraintes = 0;
        int numVar;
        int numVarRef = consoRef->numVarConso_;
        double valeurVar;
        double valeurRef = consoRef->valeur_;

        if (valeurRef == 0) {
            LOG_ALL(warning) << err::ioDico().msg("ERRConsoNulle", consoRef->nom_, binding->nomRegroupement_);
        }

        for (listeIt++; listeIt != binding->elements_.cend(); ++listeIt) {
            auto& conso = *listeIt;

            // Ecriture contrainte
            pbTypeContrainte_.push_back(COUPE_AUTRE);
            pbIndicesDebutDeLigne_.push_back(nbElmdeMatrContraint_);
            pbSens_.push_back('=');
            pbSecondMembre_.push_back(0.);
            pbContraintes_.push_back(nullptr);
            numVar = conso->numVarConso_;
            valeurVar = conso->valeur_;

            if (valeurVar == 0) {
                LOG_ALL(warning) << err::ioDico().msg("ERRConsoNulle", conso->nom_, binding->nomRegroupement_);
            }

            pbIndicesColonnes_.push_back(numVar);
            pbCoefficientsDeLaMatriceDesContraintes_.push_back(valeurRef);
            pbIndicesColonnes_.push_back(numVarRef);
            pbCoefficientsDeLaMatriceDesContraintes_.push_back(-valeurVar);
            pbNombreDeTermesDesLignes_.push_back(2);
            nbElmdeMatrContraint_ += 2;
            nbContraintes++;
        }
        pbNombreDeContraintes_ += nbContraintes;
        LOG(debug) << "Ajout des " << nbContraintes
                   << " contraintes du couplage de variables consos : " << binding->nomRegroupement_;
    }
    return METRIX_PAS_PROBLEME;
}


bool margeSuffisante(const std::shared_ptr<Groupe>& grp)
{
    return (grp->puisMax_ - grp->prod_ > config::constants::epsilon
            && grp->prod_ - grp->puisMin_ > config::constants::epsilon);
}

/*
 * Ecriture des contraintes de couplage des variables groupe
 */
int Calculer::ajouterContraintesCouplagesGroupes()
{
    // Pour chaque element i de la liste hormis l'element 0
    // P(i)/ref(i) = P(0)/ref(0)
    // soit ref(0)*P(i) - ref(i)*P(0) = 0

    for (const auto& binding : res_.groupesCouples_) {
        auto listeIt = binding->elements_.begin();
        const auto& grpRef = *listeIt;

        int nbContraintes = 0;
        int numVar;
        int numVarRef = grpRef->numVarGrp_;
        double valeurVar;
        auto valeurRef = valeurVariableReference(binding->reference_, grpRef);

        if (valeurRef == 0) {
            LOG_ALL(error) << err::ioDico().msg("ERRVarRefNulle", grpRef->nom_, binding->nomRegroupement_);
            return METRIX_PROBLEME;
        }
        if (!margeSuffisante(grpRef)) {
            LOG_ALL(warning) << err::ioDico().msg("WarnMargePetiteGrp", grpRef->nom_, binding->nomRegroupement_);
        }

        for (listeIt++; listeIt != binding->elements_.end(); ++listeIt) {
            auto& grp = *listeIt;

            // Ecriture contrainte
            pbTypeContrainte_.push_back(COUPE_AUTRE);
            pbIndicesDebutDeLigne_.push_back(nbElmdeMatrContraint_);
            pbSens_.push_back('=');
            pbSecondMembre_.push_back(0.);
            pbContraintes_.push_back(nullptr);
            numVar = grp->numVarGrp_;
            valeurVar = valeurVariableReference(binding->reference_, grp);

            if (valeurVar == 0) {
                LOG_ALL(error) << err::ioDico().msg("ERRVarRefNulle", grp->nom_, binding->nomRegroupement_);
                return METRIX_PROBLEME;
            }
            if (!margeSuffisante(grp)) {
                LOG_ALL(warning) << err::ioDico().msg("WarnMargePetiteGrp", grp->nom_, binding->nomRegroupement_);
            }

            pbIndicesColonnes_.push_back(numVar);
            pbCoefficientsDeLaMatriceDesContraintes_.push_back(valeurRef);
            pbIndicesColonnes_.push_back(numVar + 1);
            pbCoefficientsDeLaMatriceDesContraintes_.push_back(-valeurRef);
            pbIndicesColonnes_.push_back(numVarRef);
            pbCoefficientsDeLaMatriceDesContraintes_.push_back(-valeurVar);
            pbIndicesColonnes_.push_back(numVarRef + 1);
            pbCoefficientsDeLaMatriceDesContraintes_.push_back(valeurVar);
            pbNombreDeTermesDesLignes_.push_back(4);
            nbElmdeMatrContraint_ += 4;
            nbContraintes++;
        }
        pbNombreDeContraintes_ += nbContraintes;
        LOG(debug) << "Ajout des " << nbContraintes
                   << " contraintes du couplage de variables groupes : " << binding->nomRegroupement_;
    }
    return METRIX_PAS_PROBLEME;
}

/* ajout de 2 contraintes pour limiter le vol de redispatching curatif e la hausse et a la baisse */
int Calculer::ajouterLimiteCuratifGroupe(const std::map<int, std::vector<int>>& mapZoneSyncGrp)
{
    for (const auto& zone : mapZoneSyncGrp) {
        const std::vector<int>& listeVarCurGrp = zone.second;
        int nbVarCurGrp = static_cast<int>(listeVarCurGrp.size());

        // redispatching B
        for (int i = 0; i < nbVarCurGrp; i++) {
            pbIndicesColonnes_.push_back(listeVarCurGrp[i] + 1);
            pbCoefficientsDeLaMatriceDesContraintes_.push_back(1.);
        }
        pbTypeContrainte_.push_back(COUPE_AUTRE);
        pbIndicesDebutDeLigne_.push_back(nbElmdeMatrContraint_);
        pbSens_.push_back('<');
        pbContraintes_.push_back(nullptr);
        pbNombreDeTermesDesLignes_.push_back(nbVarCurGrp);
        nbElmdeMatrContraint_ += nbVarCurGrp;
        pbSecondMembre_.push_back(config::configuration().limitCurativeGrp());
        pbNombreDeContraintes_++;
    }
    return METRIX_PAS_PROBLEME;
}

int Calculer::ecrireEquationBilanCuratif(
    const std::map<int, std::vector<int>>& mapZoneSyncGrp,
    const std::map<int, std::vector<int>>& mapZoneSyncConso,
    const std::map<int, std::vector<std::shared_ptr<ElementCuratifHVDC>>>& mapZoneSyncHvdc)
{
    std::set<int> zoneSync;
    for (const auto& sync : mapZoneSyncGrp) {
        zoneSync.insert(sync.first);
    }
    for (const auto& sync : mapZoneSyncConso) {
        zoneSync.insert(sync.first);
    }

    for (auto& zone : zoneSync) {
        int nbVarCurGrp = 0;
        int nbVarCurConso = 0;
        int nbVarCurLcc = 0;
        auto itZoneSync = mapZoneSyncGrp.find(zone);
        if (itZoneSync != mapZoneSyncGrp.cend()) {
            const std::vector<int>& listeVarCurGrp = itZoneSync->second;
            nbVarCurGrp = static_cast<int>(listeVarCurGrp.size());
            for (int i = 0; i < nbVarCurGrp; i++) {
                pbIndicesColonnes_.push_back(listeVarCurGrp[i]);
                pbCoefficientsDeLaMatriceDesContraintes_.push_back(1.);
                pbIndicesColonnes_.push_back(listeVarCurGrp[i] + 1);
                pbCoefficientsDeLaMatriceDesContraintes_.push_back(-1.);
            }
        }
        itZoneSync = mapZoneSyncConso.find(zone);
        if (itZoneSync != mapZoneSyncConso.cend()) {
            const std::vector<int>& listeVarCurConso = itZoneSync->second;
            nbVarCurConso = static_cast<int>(listeVarCurConso.size());
            for (int i = 0; i < nbVarCurConso; i++) {
                pbIndicesColonnes_.push_back(listeVarCurConso[i]);
                pbCoefficientsDeLaMatriceDesContraintes_.push_back(-1.);
                pbIndicesColonnes_.push_back(listeVarCurConso[i] + 1);
                pbCoefficientsDeLaMatriceDesContraintes_.push_back(1.);
            }
        }
        auto itZoneSyncLcc = mapZoneSyncHvdc.find(zone);
        if (itZoneSyncLcc != mapZoneSyncHvdc.cend()) {
            const auto& listeVarCurLcc = itZoneSyncLcc->second;
            nbVarCurLcc = static_cast<int>(listeVarCurLcc.size());
            for (int i = 0; i < nbVarCurLcc; i++) {
                auto& elemCurLcc = listeVarCurLcc[i];
                double coeff = 1.;
                if (elemCurLcc->lcc_->nexqua_->numCompSynch_ == itZoneSyncLcc->first) {
                    coeff = -1.;
                }
                pbIndicesColonnes_.push_back(elemCurLcc->positionVarCurative_);
                pbCoefficientsDeLaMatriceDesContraintes_.push_back(-coeff);
                pbIndicesColonnes_.push_back(elemCurLcc->positionVarCurative_ + 1);
                pbCoefficientsDeLaMatriceDesContraintes_.push_back(coeff);
            }
        }
        pbTypeContrainte_.push_back(COUPE_AUTRE);
        pbIndicesDebutDeLigne_.push_back(nbElmdeMatrContraint_);
        pbSens_.push_back('=');
        pbContraintes_.push_back(nullptr);
        int nbTermes = 2 * (nbVarCurGrp + nbVarCurConso + nbVarCurLcc);
        pbNombreDeTermesDesLignes_.push_back(nbTermes);
        nbElmdeMatrContraint_ += nbTermes;
        pbSecondMembre_.push_back(0.);
        pbNombreDeContraintes_++;
    }
    return METRIX_PAS_PROBLEME;
}


int Calculer::ecrireContraintesDodu()
{
    int status = METRIX_PAS_PROBLEME;

    // 2/5 contraintes de bords sur les groupes
    // a faire avant car on modifie les indices des variables de groupes dans la matrice
    status = ecrireContraintesDeBordGroupesDodu();
    if (status != METRIX_PAS_PROBLEME) {
        return METRIX_PROBLEME;
    }

    if (nbElmdeMatrContraint_ > pbCoefficientsDeLaMatriceDesContraintes_.size()) {
        LOG_ALL(error) << err::ioDico().msg("ERRCalcInterne")
                       << " debordement sur le veteur pbCoefficientsDeLaMatriceDesContraintes_";
        return METRIX_PROBLEME;
    }

    // 1/5 Bilan energetique
    // on ecrit la contrainte globale
    status = ecrireContrainteBilanEnergetique(false);
    if (status != METRIX_PAS_PROBLEME) {
        return METRIX_PROBLEME;
    }

    // puis on ecrit les contraintes pour les autres zones
    status = ecrireContrainteBilanEnergetique(true);
    if (status != METRIX_PAS_PROBLEME) {
        return METRIX_PROBLEME;
    }

    if (nbElmdeMatrContraint_ > pbCoefficientsDeLaMatriceDesContraintes_.size()) {
        LOG_ALL(error) << err::ioDico().msg("ERRCalcInterne")
                       << " debordement sur le veteur pbCoefficientsDeLaMatriceDesContraintes_";
        return METRIX_PROBLEME;
    }

    // 3/5 contraintes de bords sur les consos nodales
    status = ecrireContraintesDeBordConsosDodu();
    if (status != METRIX_PAS_PROBLEME) {
        return METRIX_PROBLEME;
    }

    if (nbElmdeMatrContraint_ > pbCoefficientsDeLaMatriceDesContraintes_.size()) {
        LOG_ALL(error) << err::ioDico().msg("ERRCalcInterne")
                       << " debordement sur le veteur pbCoefficientsDeLaMatriceDesContraintes_";
        return METRIX_PROBLEME;
    }
    // 4/5 contraintes de bords sur les transformateur dephaseurs
    status = ecrireContraintesDeBordTransformateurDephaseur();
    if (status != METRIX_PAS_PROBLEME) {
        return METRIX_PROBLEME;
    }

    // 5/5 contraintes de bords sur les ligne a courant continu
    status = ecrireContraintesDeBordLignesCC();
    if (status != METRIX_PAS_PROBLEME) {
        return METRIX_PROBLEME;
    }

    return METRIX_PAS_PROBLEME;
}

int Calculer::initJacobienne()
{
    // initialisation de la jacobienne
    jac_.NombreDeColonnes = res_.nbNoeuds_;
    jac_.ContexteDeLaFactorisation = LU_GENERAL;
    jac_.FaireScalingDeLaMatrice = NON_LU;
    jac_.UtiliserLesSuperLignes = NON_LU;

    jac_.LaMatriceEstSymetrique = NON_LU;
    jac_.LaMatriceEstSymetriqueEnStructure = NON_LU;
    jac_.FaireDuPivotageDiagonal = OUI_LU;
    jac_.SeuilPivotMarkowitzParDefaut = OUI_LU;

    jac_.UtiliserLesValeursDePivotNulParDefaut = OUI_LU;
    jac_.ValeurDuPivotMin = 1.e-5;
    jac_.ValeurDuPivotMinExtreme = 1.e-6;

    jac_.IndexDebutDesColonnes = &jacIndexDebutDesColonnes_[0];
    jac_.NbTermesDesColonnes = &jacNbTermesDesColonnes_[0];
    jac_.ValeurDesTermesDeLaMatrice = &jacValeurDesTermesDeLaMatrice_[0];
    jac_.IndicesDeLigne = &jacIndicesDeLigne_[0];

    return METRIX_PAS_PROBLEME;
}

int Calculer::miseAJourSecondMembre(std::vector<double>& secondMembre, std::vector<double>& secondMembreFixe)
{
    int nbGroupesN;
    int nbConsosN;
    int nbQuadN;
    int nbTdN;
    int nbCCN;

    for (int i = 0; i < res_.nbNoeuds_; ++i) {
        const auto& nod = res_.noeuds_[i];
        secondMembre[i] = 0.0;
        secondMembreFixe[i] = 0.0;

        if (nod->bilan_) {
            continue;
        } // end noeud bilan

        // dernier noeud correspond a la ref de phase
        nbQuadN = nod->nbQuads();
        nbGroupesN = nod->nbGroupes_;
        nbConsosN = nod->nbConsos_;
        nbTdN = nod->nbTd();
        nbCCN = nod->nbCC();

        // Ici ce n'est donc pas un noeud bilan
        if (nbQuadN + nbCCN == 0) {
            LOG_ALL(error) << err::ioDico().msg("ERRCalcInterne") << " attention le noeud " << nod->print()
                           << ", est isole";
            return METRIX_PROBLEME;
        }

        if (nbGroupesN == 0 && nbConsosN == 0 && nbTdN == 0 && nbCCN == 0) {
            continue;
        }

        // parcours des productions connectees au noeud
        for (const auto& grp : nod->listeGroupes_) {
            if (grp->etat_) {
                secondMembre[i] += grp->prod_;
                secondMembreFixe[i] += grp->prod_;

                if (grp->prodAjust_ != Groupe::NON_HR_AR) {
                    secondMembre[i] += pbX_[grp->numVarGrp_] - pbX_[grp->numVarGrp_ + 1];
                }
            }
        }

        // Ajout de la consommation nodale
        if (nbConsosN > 0) {
            for (const auto& conso : nod->listeConsos_) {
                int numVar = conso->numVarConso_;
                secondMembre[i] -= conso->valeur_;
                secondMembreFixe[i] -= conso->valeur_;
                if (numVar >= 0) {
                    secondMembre[i] += pbX_[numVar];
                }
            }
        }

        // parcours des transformateurs-dephaseurs connectees au noeud
        for (const auto& td : nod->listeTd_) {
            int varTd = td->numVar_;
            if (nod->position(td->quad_) == Noeud::ORIGINE) {
                secondMembreFixe[i] -= td->puiCons_;
                secondMembre[i] -= td->puiCons_ + pbX_[varTd] - pbX_[varTd + 1];
            } else {
                secondMembreFixe[i] += td->puiCons_;
                secondMembre[i] += td->puiCons_ + pbX_[varTd] - pbX_[varTd + 1];
            }
        }

        // parcours des ligne a CC connectees au noeud
        for (const auto& lcc : nod->listeCC_) {
            int varLcc = lcc->numVar_;
            if (nod->position(lcc) == Noeud::ORIGINE) {
                secondMembreFixe[i] -= lcc->puiCons_;
                secondMembre[i] -= lcc->puiCons_ + pbX_[varLcc] - pbX_[varLcc + 1];
            } else {
                secondMembreFixe[i] += lcc->puiCons_;
                secondMembre[i] += lcc->puiCons_ + pbX_[varLcc] - pbX_[varLcc + 1];
            }
        }
    }

    // Si on recalule le second membre on doit le faire egalement pour tous les incidents rompant la connexite
    for (const auto& inc : res_.incidentsRompantConnexite_) {
        if (miseAJourSecondMembrePochePerdue(inc, secondMembre, secondMembreFixe) != METRIX_PAS_PROBLEME) {
            return METRIX_PAS_SOLUTION;
        }
    }

    return METRIX_PAS_PROBLEME;
}

int Calculer::miseAJourSecondMembreSurIncident(const std::shared_ptr<Incident>& icdt,
                                               std::vector<double>& injectionsNodales,
                                               std::vector<double>& secondMembreFixe)
{
    // modification des injections suivant les incidents groupes
    if (icdt->nbGroupes_ > 0) {
        comput_ParticipationGrp(icdt);

        double puissancePerdueTotale = 0.0; // sur tous els groupes composant l incident
        for (const auto& grpe : icdt->listeGroupes_) {
            if (!grpe->etat_) {
                continue;
            }

            double puissancePerdue = grpe->prod_;
            if (grpe->prodAjust_ != Groupe::NON_HR_AR) {
                puissancePerdue += pbX_[grpe->numVarGrp_] - pbX_[grpe->numVarGrp_ + 1];
            }

            injectionsNodales[grpe->numNoeud_] -= puissancePerdue;
            puissancePerdueTotale += puissancePerdue;
        }

        double ParticipationGlobale = 0.;

        for (const auto& grp : res_.groupesEOD_) {
            double participation = grp->participation_ * puissancePerdueTotale;
            injectionsNodales[grp->noeud_->num_] += participation;
            ParticipationGlobale += participation;
        }

        if (fabs(ParticipationGlobale - puissancePerdueTotale) > config::constants::epsilon) {
            LOG_ALL(info) << " Participation globale " << ParticipationGlobale << "  puissance perdue "
                          << puissancePerdueTotale;
        }
    }

    // Modif de l injection si incident HVDC ...
    for (const auto& lcc : icdt->listeLccs_) {
        int numVar = lcc->numVar_;
        double injHvdc = lcc->puiCons_ + pbX_[numVar] - pbX_[numVar + 1];
        injectionsNodales[lcc->norqua_->num_] += injHvdc;
        injectionsNodales[lcc->nexqua_->num_] -= injHvdc;
    }

    // Modif de l'injection si curatif ...
    if (icdt->incidentATraiterEncuratif_) {
        for (const auto& elemC : icdt->listeElemCur_) {
            if (elemC->positionVarCurative_ == -1) {
                continue;
            }

            double varPuiss = 0;
            int numOr = -1;
            int numEx = -1;
            int numVarTd = -1;
            switch (elemC->typeElem_) {
                case ElementCuratif::TD: {
                    varPuiss = pbX_[elemC->positionVarCurative_] - pbX_[elemC->positionVarCurative_ + 1];
                    numOr = std::dynamic_pointer_cast<ElementCuratifTD>(elemC)->td_->quad_->norqua_->num_;
                    numEx = std::dynamic_pointer_cast<ElementCuratifTD>(elemC)->td_->quad_->nexqua_->num_;
                    break;
                }
                case ElementCuratif::TD_FICTIF: {
                    numVarTd = std::dynamic_pointer_cast<ElementCuratifTD>(elemC)->td_->numVar_;
                    varPuiss = pbX_[elemC->positionVarCurative_] - pbX_[elemC->positionVarCurative_ + 1]
                               - (pbX_[numVarTd] - pbX_[numVarTd + 1]);
                    numOr = std::dynamic_pointer_cast<ElementCuratifTD>(elemC)->td_->quad_->norqua_->num_;
                    numEx = std::dynamic_pointer_cast<ElementCuratifTD>(elemC)->td_->quad_->nexqua_->num_;
                    break;
                }
                case ElementCuratif::HVDC: {
                    varPuiss = pbX_[elemC->positionVarCurative_] - pbX_[elemC->positionVarCurative_ + 1];
                    numOr = std::dynamic_pointer_cast<ElementCuratifHVDC>(elemC)->lcc_->norqua_->num_;
                    numEx = std::dynamic_pointer_cast<ElementCuratifHVDC>(elemC)->lcc_->nexqua_->num_;
                    break;
                }
                case ElementCuratif::GROUPE: {
                    varPuiss = pbX_[elemC->positionVarCurative_] - pbX_[elemC->positionVarCurative_ + 1];
                    numEx = std::dynamic_pointer_cast<ElementCuratifGroupe>(elemC)->groupe_->numNoeud_;
                    break;
                }
                case ElementCuratif::CONSO: {
                    varPuiss = -pbX_[elemC->positionVarCurative_] + pbX_[elemC->positionVarCurative_ + 1];
                    numEx = std::dynamic_pointer_cast<ElementCuratifConso>(elemC)->conso_->noeud_->num_;
                    break;
                }
                default: LOG_ALL(warning) << " unsupported curative element of type " << elemC->typeElem_; break;
            } // end switch

            if (numOr != -1) {
                injectionsNodales[numOr] -= varPuiss;
            }
            injectionsNodales[numEx] += varPuiss;
        } // end for
    }     // end if

    if (icdt->pochePerdue_) {
        const auto& poche = icdt->pochePerdue_;

        if (poche->pocheAvecConsoProd_) {
            double prorataEOD = deltaEODPoche(poche);
            LOG(debug) << "Incident " << icdt->nom_ << " : Puissance perdue dans la poche = " << prorataEOD;

            const auto& noeudsPoche = poche->noeudsPoche_;
            if (fabs(prorataEOD) > config::constants::zero_power) {
                if (res_.prodMaxPossible_ - poche->prodMaxPoche_ < config::constants::zero_power) {
                    LOG_ALL(error) << err::ioDico().msg("ERRCompensationInsuffisante");
                    return METRIX_PROBLEME;
                }

                prorataEOD /= (res_.prodMaxPossible_ - poche->prodMaxPoche_);

                for (const auto& grp : res_.groupesEOD_) {
                    if (grp->etat_) {
                        const auto& node = grp->noeud_;
                        // Tous les groupes connectes participent a la compensation au prorata de leur pmax
                        injectionsNodales[node->num_] -= prorataEOD * grp->puisMaxDispo_;
                        secondMembreFixe[node->num_] -= prorataEOD * grp->puisMaxDispo_;
                    }
                }
            }

            for (const auto& node : noeudsPoche) {
                injectionsNodales[node->num_] = 0.;
                secondMembreFixe[node->num_] = 0.;
            }
        }
    }
    return METRIX_PAS_PROBLEME;
}

int Calculer::miseAJourSecondMembrePochePerdue(const std::shared_ptr<Incident>& icdt,
                                               const std::vector<double>& secondMembreN,
                                               const std::vector<double>& secondMembreFixeN)
{
    const auto& poche = icdt->pochePerdue_;

    if (!poche->pocheAvecConsoProd_) {
        return METRIX_PAS_PROBLEME;
    }

    MATRICE* jacFactorisee = nullptr;
    ListeQuadsIncident listeQuadsIncident = poche->incidentModifie_->listeQuadsIncident();

    if (listeQuadsIncident.quadsOuverts.empty() && listeQuadsIncident.quadsRefermes.empty()) {
        jacFactorisee = jacFactorisee_;
    } else {
        auto jacFactoriseeIt = jacIncidentsModifies_.find(listeQuadsIncident);
        if (jacFactoriseeIt == jacIncidentsModifies_.end()) {
            LOG(debug) << "Factorisation de la jacobienne pour " << icdt->nom_;
            modifJacobienneInc(poche->incidentModifie_, true);
            jacFactorisee = LU_Factorisation(&jac_);
            jacIncidentsModifies_.insert(std::pair<ListeQuadsIncident, MATRICE*>(listeQuadsIncident, jacFactorisee));
            modifJacobienneInc(poche->incidentModifie_, false);
        } else {
            jacFactorisee = jacFactoriseeIt->second;
        }
    }

    std::vector<double>& injectionsNodales = poche->phases_;
    std::vector<double>& secondMembreFixe = poche->secondMembreFixe_;

    secondMembreFixe = secondMembreFixeN;
    injectionsNodales = secondMembreN;

    if (miseAJourSecondMembreSurIncident(icdt, injectionsNodales, secondMembreFixe) != METRIX_PAS_PROBLEME) {
        return METRIX_PROBLEME;
    }

    // mise a zero de l'injection au noeud bilan
    std::map<int, int>::iterator itNb;
    for (itNb = res_.numNoeudBilanParZone_.begin(); itNb != res_.numNoeudBilanParZone_.end(); ++itNb) {
        injectionsNodales[itNb->second] = 0.;
    }

    int codeRet;
    LU_LuSolv(jacFactorisee, &injectionsNodales[0], &codeRet, nullptr, 0, 0.0);

    return codeRet;
}

double Calculer::deltaEODPoche(const std::shared_ptr<PochePerdue>& poche)
{
    double prodCoupee = 0.;
    double consoCoupee = 0.;
    if (poche->pocheAvecConsoProd_) {
        for (const auto& node : poche->noeudsPoche_) {
            for (const auto& conso : node->listeConsos_) {
                double cut = conso->valeur_;
                int numVar = conso->numVarConso_;
                if (numVar >= 0) {
                    cut -= pbX_[numVar];
                }
                consoCoupee += cut;
                poche->consumptionLosses_[conso] = cut;
            }

            for (const auto& tmpGrp : node->listeGroupes_) {
                if (tmpGrp->etat_) {
                    prodCoupee += tmpGrp->prod_;
                    if (tmpGrp->prodAjust_ != Groupe::NON_HR_AR) {
                        int numVar = tmpGrp->numVarGrp_;
                        prodCoupee += pbX_[numVar] - pbX_[numVar + 1];
                    }
                }
            }
        }
    }
    poche->prodPerdue_ = prodCoupee;
    poche->consoPerdue_ = consoCoupee;
    return consoCoupee - prodCoupee;
}

int Calculer::construireJacobienne()
{
    // ATTENTION : Construction de la jacobienne avec :
    // 1- elimination de la derniere injection
    // 2- remplacement de cette derniere par une phase.

    int nbQuadN;
    int indElmExistDeja = 0;
    int nbElmdeMatrContraint = 0;
    double elemNod = 0;
    bool existDeja;

    // construction de la matrice de contraintes d'egalites
    for (int i = 0; i < res_.nbNoeuds_; ++i) {
        // Attention : Pour l ecriture des contraintes il faut commencer par le traitement des quadripoles
        const auto& nod = res_.noeuds_[i];
        nbQuadN = nod->nbQuads();

        if (nbQuadN == 0 && nod->nbCC() == 0) {
            LOG_ALL(error) << err::ioDico().msg("ERRCalcInterne") << " attention le noeud " << nod->print()
                           << ", est isole";
            return METRIX_PROBLEME;
        }

        jacNbTermesDesColonnes_[i] = 0;
        jacIndexDebutDesColonnes_[i] = nbElmdeMatrContraint;

        // la derniere injection correspond au noeud bilan
        elemNod = 0;
        if (!nod->bilan_) {
            for (int j = 0; j < nbQuadN; ++j) {
                const auto& quad = nod->listeQuads_[j];
                const auto& nodO = quad->norqua_;
                const auto& nodE = quad->nexqua_;

                existDeja = false;

                // si le quadripole est connecte a l'origine et a l'extremite
                if (quad->connecte() || quad->reconnectable_) {
                    // Noeud voisin = noeuds extremite nodE
                    auto other_node = (nodO == nod) ? nodE : nodO;
                    // est que le noeud est deja traite ?
                    for (int k = jacIndexDebutDesColonnes_[i]; k < nbElmdeMatrContraint; ++k) {
                        if (jacIndicesDeLigne_[k] == static_cast<int>(other_node->num_)) {
                            existDeja = true;
                            indElmExistDeja = k;
                            break;
                        }
                    }

                    if (existDeja) {
                        jacValeurDesTermesDeLaMatrice_[indElmExistDeja]
                            += res_.noeuds_[jacIndicesDeLigne_[indElmExistDeja]]->bilan_ || !quad->connecte()
                                   ? 0.0
                                   : -quad->u2Yij_;
                    } else {
                        // nouvel element : noeud voisin de nod qui correspond au noeud nodE
                        jacIndicesDeLigne_[nbElmdeMatrContraint] = other_node->num_;
                        jacValeurDesTermesDeLaMatrice_[nbElmdeMatrContraint]
                            = res_.noeuds_[jacIndicesDeLigne_[nbElmdeMatrContraint]]->bilan_ || !quad->connecte()
                                  ? 0.0
                                  : -quad->u2Yij_;
                        nbElmdeMatrContraint++;
                        jacNbTermesDesColonnes_[i]++;
                    }
                    // cote du noeud en cours
                    elemNod += !quad->connecte() ? 0. : quad->u2Yij_;
                }
            }
        } else {
            LOG_ALL(info) << "Noeud bilan : " << nod->print();
            res_.afficheSousReseau(nod->num_, 2);
        }

        // element du noeud courant
        if (jacNbTermesDesColonnes_[i] != 0) {
            jacIndicesDeLigne_[nbElmdeMatrContraint] = nod->num_;
            jacValeurDesTermesDeLaMatrice_[nbElmdeMatrContraint] = elemNod;
            jacNbTermesDesColonnes_[i]++;
            nbElmdeMatrContraint++;
        } else {
            jacIndicesDeLigne_[nbElmdeMatrContraint] = i;
            jacValeurDesTermesDeLaMatrice_[nbElmdeMatrContraint] = 1;
            jacNbTermesDesColonnes_[i]++;
            nbElmdeMatrContraint++;
        }
    }

    return METRIX_PAS_PROBLEME;
}

int Calculer::modifJacobienneLigne(const std::shared_ptr<Quadripole>& ligne, bool applique)
{
    int u;

    double coeffApplication = applique ? 1.0 : -1.0;

    const auto& nor = ligne->norqua_;
    const auto& nex = ligne->nexqua_;

    // Pour la colonne noeud origine
    if (!nor->bilan_) {
        int umax = jacIndexDebutDesColonnes_[nor->num_] + jacNbTermesDesColonnes_[nor->num_];
        for (u = jacIndexDebutDesColonnes_[nor->num_]; u < umax; ++u) {
            if (!nex->bilan_ && jacIndicesDeLigne_[u] == static_cast<int>(nex->num_)) {
                jacValeurDesTermesDeLaMatrice_[u] += coeffApplication * ligne->u2Yij_;
            } else if (jacIndicesDeLigne_[u] == static_cast<int>(nor->num_)) {
                jacValeurDesTermesDeLaMatrice_[u] -= coeffApplication * ligne->u2Yij_;
            }
        }
    }

    // Pour la colonne noeud extremite
    if (!nex->bilan_) {
        int umax = jacIndexDebutDesColonnes_[nex->num_] + jacNbTermesDesColonnes_[nex->num_];
        for (u = jacIndexDebutDesColonnes_[nex->num_]; u < umax; ++u) {
            if (!nor->bilan_ && jacIndicesDeLigne_[u] == static_cast<int>(nor->num_)) {
                jacValeurDesTermesDeLaMatrice_[u] += coeffApplication * ligne->u2Yij_;
            } else if (jacIndicesDeLigne_[u] == static_cast<int>(nex->num_)) {
                jacValeurDesTermesDeLaMatrice_[u] -= coeffApplication * ligne->u2Yij_;
            }
        }
    }
    return METRIX_PAS_PROBLEME;
}

int Calculer::modifJacobienneInc(const std::shared_ptr<Incident>& icdt, bool applique)
{
    if (!icdt->validite_) {
        return 1;
    }

    // lignes (ou couplages) ouverts
    for (const auto& ligne : icdt->listeQuads_) {
        if (ligne->connecte()) {
            modifJacobienneLigne(ligne, applique);
        }
    }

    // lignes (ou couplages) fermes
    for (const auto& ligne : icdt->listeCouplagesFermes_) {
        if (!ligne->connecte()) {
            modifJacobienneLigne(ligne, !applique);
        }
    }

    return METRIX_PAS_PROBLEME;
}

int Calculer::modifJacobienneVar(const Quadripole::SetQuadripoleSortedByName& quads, bool applique)
{
    // Cette methode permet de modifier la jacobienne avec les indisponibilites de lignes d'une variante
    // le boolean applique est a vrai si on modifie la jacobienne
    // sinon on enleve les modifications de la jacobienne pour la variante donnee

    for (const auto& set : quads) {
        modifJacobienneLigne(set, applique);
    }

    return METRIX_PAS_PROBLEME;
}


// Nouvelle version avec detection des contraintes equivalentes
int Calculer::ecrireCoupeTransit(const double& maxTprev,
                                 const double& minTprev,
                                 const std::shared_ptr<Contrainte>& ctre)
{
    // Calcul des coefficients de la contrainte

    double tmpSecondMembre = 0.0;
    int coupe = 0;

    if (ctre->ctrSup_) {
        // borne sup
        //----------
        // on met ici un ecart acceptable: cela permet de detecter plus rapidement des incoherences du type avec un
        // seuil a 0 dans ce cas, on a direct: pas de solution. Et cela fait de la marge pour ne pas redetecter une
        // contrainte deja rentree.
        tmpSecondMembre = maxTprev - config::constants::acceptable_diff;
        for (int j = 0; j < pbNombreDeVariables_; ++j) {
            if (pbTypeDeBorneDeLaVariable_[j] == VARIABLE_FIXE) {
                tmpSecondMembre -= coefs_[j] * pbX_[j];
                coefs_[j] = 0.0;
            } else {
                // calcul du coeff
                if (fabs(coefs_[j]) < config::constants::epsilon_coupe) {
                    coefs_[j] = 0.0;
                    // tmpSecondMembre -= coefs_[j]*pbX_[j];
                    coupe++;
                } else {
                    coefs_[j] = round(coefs_[j], config::constants::constraints_precision);
                }
            }
        }
    } else {
        // borne inf
        //---------
        // on met ici un ecart acceptable: cela permet de detecter plus rapidement des incoherences du type avec un
        // seuil a 0 dans ce cas, on a direct: pas de solution. Et cela fait de la marge pour ne pas redetecter une
        // contrainte deja rentree.
        tmpSecondMembre = -minTprev - config::constants::acceptable_diff;
        for (int j = 0; j < pbNombreDeVariables_; ++j) {
            if (pbTypeDeBorneDeLaVariable_[j] == VARIABLE_FIXE) {
                tmpSecondMembre += coefs_[j] * pbX_[j];
                coefs_[j] = 0.0;
            } else {
                if (fabs(coefs_[j]) < config::constants::epsilon_coupe) {
                    coefs_[j] = 0.0;
                    // tmpSecondMembre += coefs_[j]*pbX_[j];
                    coupe++;
                } else {
                    coefs_[j] = -round(coefs_[j], config::constants::constraints_precision);
                }
            }
        }
    }

    tmpSecondMembre = round(tmpSecondMembre, config::constants::constraints_precision);

    const auto& icdt = ctre->icdt_;
    if (icdt && icdt->parade_) {
        tmpSecondMembre += config::constants::pne_factor_inactive_constraint;
    }

    std::shared_ptr<Incident> incidentPere;
    // Teste si une contrainte de parade est equivalente a une contrainte existante
    if (config::configuration().useParadesEquivalentes() && ctre->type_ == Contrainte::CONTRAINTE_PARADE) {
        bool contrainteEquivalente = false;
        incidentPere = icdt->incTraiteCur_;
        for (int cid : incidentPere->contraintes_) {
            if (cid == -1 || icdt->numVarActivation_ == pbContraintes_[cid]->icdt_->numVarActivation_) {
                continue;
            }

            contrainteEquivalente = (fabs(tmpSecondMembre - pbSecondMembre_[cid])
                                     < config::constants::epsilon_constraint_eq);
            int indice = pbIndicesDebutDeLigne_[cid];
            int nTermesMax = indice + pbNombreDeTermesDesLignes_[cid];
            for (int k = 0; contrainteEquivalente && k < pbNombreDeVariables_ && indice < nTermesMax; ++k) {
                if (pbTypeDeBorneDeLaVariable_[k] == VARIABLE_FIXE || coefs_[k] == 0) {
                    continue;
                }
                if (pbTypeDeVariable_[k] != ENTIER && typeEtat_[k] != ECART_T) {
                    if ((pbIndicesColonnes_[indice] != k)
                        || (fabs(coefs_[k] - pbCoefficientsDeLaMatriceDesContraintes_[indice])
                            > config::constants::epsilon_constraint_eq)) {
                        contrainteEquivalente = false;
                    }
                }
                indice++;
            }
            if (contrainteEquivalente) {
                LOG_ALL(info) << "Contraintes equivalentes : '" << icdt->nom_ << "' & '"
                              << pbContraintes_[cid]->icdt_->nom_ << "' (icdt : " << incidentPere->nom_ << ")";
                return METRIX_CONTRAINTE_IGNOREE;
            }
        }
    }

    if (icdt && icdt->parade_) {
        // ajouter le coefficient de la variable entiere (activation de la parade)
        if (icdt->numVarActivation_ == -1) {
            if (!incidentPere) {
                incidentPere = icdt->incTraiteCur_;
            }
            // cout<<" on ajoute la variable d'activation de la parade  "<< icdt->nom_<<endl;
            // prise en compte de la probabilite de l'incident si elle est définie

            double proba = incidentPere->getProb();

            icdt->numVarActivation_ = ajouterVariableEntiere(
                icdt->num_,
                config::constants::cost_parade * proba * static_cast<double>(incidentPere->contraintes_.size()));
            if (coefs_.size() < static_cast<size_t>(pbNombreDeVariables_)) {
                coefs_.resize(pbNombreDeVariables_, 0.);
            }

            if (!icdt->contraintesAutorisees_.empty()
                && icdt->contraintesAutorisees_.find(ctre->elemAS_) == icdt->contraintesAutorisees_.end()) {
                pbTypeDeVariable_[icdt->numVarActivation_] = REEL;
                pbXmax_[icdt->numVarActivation_] = 0;
                LOG(debug) << "Variable no " << icdt->numVarActivation_ << " de la parade '" << icdt->nom_
                           << "' desactivee.";
            }
        }

        // coefficient pour la variable entiere
        coefs_[icdt->numVarActivation_] = config::constants::pne_factor_inactive_constraint;
    }

    int nbTermesNonNuls = 0;
    // On ajoute la contrainte
    for (int j = 0; j < pbNombreDeVariables_; ++j) {
        if (coefs_[j] != 0.) {
            // indice des colonnes du coeff
            pbIndicesColonnes_.push_back(j);

            // valeur du coeff
            pbCoefficientsDeLaMatriceDesContraintes_.push_back(coefs_[j]);

            nbTermesNonNuls++;
        }
    }

    if (nbTermesNonNuls == 0) {
        // pas necessaire d'ajouter cette contrainte
        if (config::configuration().computationType() == config::Configuration::ComputationType::OPF_WITHOUT_REDISPATCH
            || config::configuration().computationType() == config::Configuration::ComputationType::OPF_WITH_OVERLOAD) {
            // Ajout d'une fausse variable d'ecart pour ne plus tester cet incident sur cet ouvrage
            ctre->elemAS_->ecarts_[ctre->numInc()] = -1;
            LOG(debug) << metrix::log::verbose_constraints << "Contrainte ignoree car pas d'action possible";
            return METRIX_CONTRAINTE_IGNOREE;
        }

        // en mode OPF on ne pourra rien faire !!
        return METRIX_PAS_SOLUTION;
    }

    // sinon on ajoute effectivement la contrainte

    if (icdt == nullptr) {
        pbTypeContrainte_.push_back(COUPE_SURETE_N);
    } else {
        if (icdt->type_ == Incident::N_MOINS_1_LIGNE) {
            pbTypeContrainte_.push_back(COUPE_SURETE_N_1_L);
        } else if (icdt->type_ == Incident::N_MOINS_K_GROUPE) {
            pbTypeContrainte_.push_back(COUPE_SURETE_N_1_G);
        } else if (icdt->type_ == Incident::N_MOINS_K_GROUPE_LIGNE) {
            pbTypeContrainte_.push_back(COUPE_SURETE_N_K);
        } else {
            pbTypeContrainte_.push_back(COUPE_AUTRE);
        }
    }

    ctre->num_ = static_cast<int>(pbContraintes_.size());
    if (incidentPere != nullptr) {
        incidentPere->contraintes_.push_back(ctre->num_);
    }

    // vecteur utilisees pour la mise a jour des contraintes et non fourni au solveur
    auto copieCtre = std::make_shared<Contrainte>(*ctre);
    pbContraintes_.push_back(copieCtre);

    pbSens_.push_back('<');
    pbNombreDeTermesDesLignes_.push_back(nbTermesNonNuls);
    pbIndicesDebutDeLigne_.push_back(nbElmdeMatrContraint_);

    pbSecondMembre_.push_back(tmpSecondMembre);

    nbElmdeMatrContraint_ += nbTermesNonNuls;

    if (ctre->type_ == Contrainte::CONTRAINTE_EMUL_AC_N || ctre->type_ == Contrainte::CONTRAINTE_EMULATION_AC) {
        // Ajout de la contrainte d'activation du dephasage : Tmax - T + M*delta < M
        LOG(debug) << metrix::log::verbose_constraints << "Ajout de la contrainte d'activation en emulation AC pour "
                   << ctre->elemAS_->nom_;
        if (ajoutContrainteActivation(pbNombreDeContraintes_, ctre->numVarActivation_) != METRIX_PAS_PROBLEME) {
            return METRIX_PROBLEME;
        }
    } else if (ctre->type_ == Contrainte::CONTRAINTE_ACTIVATION) {
        // Ajout d'une contrainte d'activation pour la parade : Tmax - T + M*delta < M

        if (ctre->numVarActivation_ == -1) {
            LOG_ALL(error) << "No integer activation variable";
            return METRIX_PROBLEME;
        }

        pbIndicesColonnes_.push_back(ctre->numVarActivation_);
        pbCoefficientsDeLaMatriceDesContraintes_.push_back(-config::constants::pne_factor_inactive_constraint);
        pbNombreDeTermesDesLignes_[pbNombreDeContraintes_]++;
        nbElmdeMatrContraint_++;
        pbSens_[pbNombreDeContraintes_] = '>';
        pbSecondMembre_[pbNombreDeContraintes_] -= config::constants::pne_factor_inactive_constraint;
    } else if (config::configuration().computationType()
                   == config::Configuration::ComputationType::OPF_WITHOUT_REDISPATCH
               || config::configuration().computationType()
                      == config::Configuration::ComputationType::OPF_WITH_OVERLOAD) { // Ajout de la variable d'ecart
        int numVar = ajoutVariableEcart(ctre);
        pbNombreDeTermesDesLignes_[pbNombreDeContraintes_]++;
        pbIndicesColonnes_.push_back(numVar);
        pbCoefficientsDeLaMatriceDesContraintes_.push_back(-1.);
        nbElmdeMatrContraint_++;
    }

    pbNombreDeContraintes_++;

    // Valorisation de la poche perdue
    if (icdt && icdt->parade_ && icdt->pochePerdue_ && icdt->pochePerdue_->pocheAvecConsoProd_) {
        if (lienParadeValorisation_.find(icdt) == lienParadeValorisation_.end()) {
            // Ajout de la variable et de la contrainte de valorisation
            lienParadeValorisation_[icdt] = ajoutContrainteValorisationPoche(icdt);
        }
    }

    return METRIX_PAS_PROBLEME;
}


int Calculer::ajoutContrainteActivation(int numContrainteInitiale, int numVarActivation)
{
    if (numVarActivation == -1) {
        LOG_ALL(error) << "No integer activation variable";
        return METRIX_PROBLEME;
    }

    pbTypeContrainte_.push_back(COUPE_AUTRE);
    pbIndicesDebutDeLigne_.push_back(nbElmdeMatrContraint_);
    pbSens_.push_back('>');
    pbContraintes_.push_back(nullptr);

    int debut = pbIndicesDebutDeLigne_[numContrainteInitiale];
    int nbTermesNonNuls = pbNombreDeTermesDesLignes_[numContrainteInitiale];
    for (int j = debut; j < debut + nbTermesNonNuls; ++j) {
        pbIndicesColonnes_.push_back(pbIndicesColonnes_[j]);
        // valeur du coeff
        pbCoefficientsDeLaMatriceDesContraintes_.push_back(pbCoefficientsDeLaMatriceDesContraintes_[j]);
    }

    // Variable d'activation
    pbIndicesColonnes_.push_back(numVarActivation);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(-config::constants::pne_factor_inactive_constraint);

    nbTermesNonNuls++;
    pbNombreDeTermesDesLignes_.push_back(nbTermesNonNuls);
    nbElmdeMatrContraint_ += nbTermesNonNuls;
    pbSecondMembre_.push_back(pbSecondMembre_[numContrainteInitiale]
                              - config::constants::pne_factor_inactive_constraint);
    pbNombreDeContraintes_++;

    return METRIX_PAS_PROBLEME;
}

int Calculer::ajouterContrainteChoixTopo(const std::vector<std::shared_ptr<Incident>>& paradesActivees)
{
    pbTypeContrainte_.push_back(COUPE_AUTRE);
    // La matrice du probleme est decrite:
    // 1: Par l indice de debut de la ligne dans les autres vecteur
    pbIndicesDebutDeLigne_.push_back(nbElmdeMatrContraint_);

    pbSens_.push_back('=');
    pbNombreDeTermesDesLignes_.push_back(0);

    pbContraintes_.push_back(nullptr);
    // cout<<pbNombreDeContraintes_<<" ajout d'une contrainte pour activation d'une seule parade ; var :";
    for (const auto& parade : paradesActivees) {
        int numVar = parade->numVarActivation_;

        // 2: le nombre de coeff sur cette ligne
        pbNombreDeTermesDesLignes_[pbNombreDeContraintes_]++;

        // 3 : les indices des colonnes pour chaque coeff
        pbIndicesColonnes_.push_back(numVar);

        // cout<<numVar<<" ; ";
        // 4: la valeur du coeff
        pbCoefficientsDeLaMatriceDesContraintes_.push_back(1);

        nbElmdeMatrContraint_++;
    }
    // une seule des variables doit etre activee a 0 <=> parade active
    pbSecondMembre_.push_back(1);
    // cout<<" secondMembre "<<cpt - 1<<endl;
    pbNombreDeContraintes_++;

    return METRIX_PAS_PROBLEME;
}

/*
 * Ajout de 2 contraintes pour borner les actions cumulee du preventif et du curatif
 */
void Calculer::ajouterContraintesBorneCuratif(int numVar, int numVarCur, double Pmin, double Pmax)
{
    // Pcur < Pmax
    pbTypeContrainte_.push_back(COUPE_AUTRE);
    pbIndicesDebutDeLigne_.push_back(nbElmdeMatrContraint_);
    pbSens_.push_back('<');
    pbContraintes_.push_back(nullptr);
    pbIndicesColonnes_.push_back(numVar);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(COEFF_MULT);
    pbIndicesColonnes_.push_back(numVarCur);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(COEFF_MULT);
    pbNombreDeTermesDesLignes_.push_back(2);
    nbElmdeMatrContraint_ += 2;
    pbSecondMembre_.push_back(Pmax * COEFF_MULT);
    pbNombreDeContraintes_++;

    // -Pcur < -Pmin
    pbTypeContrainte_.push_back(COUPE_AUTRE);
    pbIndicesDebutDeLigne_.push_back(nbElmdeMatrContraint_);
    pbSens_.push_back('<');
    pbContraintes_.push_back(nullptr);
    pbIndicesColonnes_.push_back(numVar + 1);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(COEFF_MULT);
    pbIndicesColonnes_.push_back(numVarCur + 1);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(COEFF_MULT);
    pbNombreDeTermesDesLignes_.push_back(2);
    nbElmdeMatrContraint_ += 2;
    pbSecondMembre_.push_back(-Pmin * COEFF_MULT);
    pbNombreDeContraintes_++;

    LOG(debug) << metrix::log::verbose_constraints << "Ajout des contraintes de bornes du TD/HVDC curatif  "
               << numSupportEtat_[numVar];
}

/*
 * Borne les variables curatives des groupes en fonction des variables preventives : Phausse, Pbaisse, P0
 */
void Calculer::ajouterContraintesBorneCuratifGroupe(int numVarGrp, int numVarCur, const std::shared_ptr<Groupe>& grp)
{
    // Pprev + Pcur+ < Pmax
    pbTypeContrainte_.push_back(COUPE_AUTRE);
    pbIndicesDebutDeLigne_.push_back(nbElmdeMatrContraint_);
    pbSens_.push_back('<');
    pbContraintes_.push_back(nullptr);
    pbIndicesColonnes_.push_back(numVarGrp);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(1.);
    pbIndicesColonnes_.push_back(numVarGrp + 1);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(-1.);
    pbIndicesColonnes_.push_back(numVarCur);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(1.);
    pbNombreDeTermesDesLignes_.push_back(3);
    nbElmdeMatrContraint_ += 3;
    pbSecondMembre_.push_back(grp->puisMax_ - grp->prod_);
    pbNombreDeContraintes_++;

    // Pprev - Pcur- > Pmin
    pbTypeContrainte_.push_back(COUPE_AUTRE);
    pbIndicesDebutDeLigne_.push_back(nbElmdeMatrContraint_);
    pbSens_.push_back('>');
    pbContraintes_.push_back(nullptr);
    pbIndicesColonnes_.push_back(numVarGrp);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(1.);
    pbIndicesColonnes_.push_back(numVarGrp + 1);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(-1.);
    pbIndicesColonnes_.push_back(numVarCur + 1);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(-1.);
    pbNombreDeTermesDesLignes_.push_back(3);
    nbElmdeMatrContraint_ += 3;
    pbSecondMembre_.push_back(std::min(0., grp->puisMin_ - grp->prod_));
    pbNombreDeContraintes_++;

    LOG(debug) << metrix::log::verbose_constraints << "Ajout des contraintes de bornes du groupe curatif  "
               << numVarGrp / 3;
}

/*
 * Borne les variables curatives des effacements en fonction des variables de delestage preventif
 */
void Calculer::ajouterContraintesBorneCuratifConso(int numVarPrev, int numVarCur, double consoNod, double pourcentEff)
{
    // Pcur < (conso - delestage) * %effacement
    pbTypeContrainte_.push_back(COUPE_AUTRE);
    pbIndicesDebutDeLigne_.push_back(nbElmdeMatrContraint_);
    pbSens_.push_back('<');
    pbContraintes_.push_back(nullptr);
    pbIndicesColonnes_.push_back(numVarPrev);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(pourcentEff);
    pbIndicesColonnes_.push_back(numVarCur);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(1.);
    pbIndicesColonnes_.push_back(numVarCur + 1);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(1.);
    pbNombreDeTermesDesLignes_.push_back(3);
    nbElmdeMatrContraint_ += 3;
    pbSecondMembre_.push_back(pourcentEff * consoNod);
    pbNombreDeContraintes_++;

    LOG(debug) << metrix::log::verbose_constraints << "Ajout de la contraintes de bornes de l'effacement curatif "
               << numVarCur << " (numVarPrev = " << numVarPrev << ")";
}

int Calculer::ajouterContrainteNbMaxActCur(const std::shared_ptr<Incident>& parade)
{
    pbTypeContrainte_.push_back(COUPE_AUTRE);
    // La matrice du probleme est decrite:
    // 1: Par l indice de debut de la ligne dans les autres vecteur
    pbIndicesDebutDeLigne_.push_back(nbElmdeMatrContraint_);
    pbSens_.push_back('<');
    pbNombreDeTermesDesLignes_.push_back(0);

    pbContraintes_.push_back(nullptr);

    // ajout du coeff pour l'activation de la parade
    int nbActions = parade->nbLignes_ + parade->nbCouplagesFermes_ - parade->incTraiteCur_->nbLignes_;
    pbNombreDeTermesDesLignes_[pbNombreDeContraintes_]++;
    pbIndicesColonnes_.push_back(parade->numVarActivation_);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(nbActions);
    nbElmdeMatrContraint_++;

    for (const auto& elemC : parade->listeElemCur_) {
        if (elemC->typeElem_ == ElementCuratif::TD_FICTIF || !elemC->estValide()) {
            continue;
        }

        // nombre de coeff sur cette ligne
        pbNombreDeTermesDesLignes_[pbNombreDeContraintes_]++;

        // indices des colonnes pour chaque coeff
        pbIndicesColonnes_.push_back(elemC->positionVarEntiereCur_);

        // valeur du coeff
        double coeff = elemC->typeElem_ == ElementCuratif::GROUPE || elemC->typeElem_ == ElementCuratif::CONSO ? .5
                                                                                                               : 1.;
        pbCoefficientsDeLaMatriceDesContraintes_.push_back(coeff);

        nbElmdeMatrContraint_++;
    }
    // une seule des variables doit etre activee a 1 <=> parade active
    pbSecondMembre_.push_back(config::configuration().nbMaxActionCurative());
    pbNombreDeContraintes_++;

    return METRIX_PAS_PROBLEME;
}

int Calculer::ajouterContrainteDeltaConsVarEntiere(const std::shared_ptr<ElementCuratif>& elemC)
{
    int numVarCur = elemC->positionVarCurative_;
    int numVarEntiere = elemC->positionVarEntiereCur_;

    // Ici, on ajoute 1 contrainte pour coincer la variation de consigne curative
    // si la variable entiere "activation du curatif " est a 0
    // a + b < delta*A (i.e. a + b - delta*A < 0)

    pbTypeContrainte_.push_back(COUPE_AUTRE);
    // La matrice du probleme est decrite:
    // 1: Par l indice de debut de la ligne dans les autres vecteur
    pbIndicesDebutDeLigne_.push_back(nbElmdeMatrContraint_);
    pbSens_.push_back('<');
    // 2: le nombre de coeff sur cette ligne
    pbNombreDeTermesDesLignes_.push_back(3);
    pbContraintes_.push_back(nullptr);

    pbIndicesColonnes_.push_back(numVarCur);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(1.e-3);

    pbIndicesColonnes_.push_back(numVarCur + 1);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(1.e-3);

    // variable entiere : activation de ce curatif ou pas
    pbIndicesColonnes_.push_back(numVarEntiere);
    double max = std::min(-pbXmax_[numVarCur] + pbXmin_[numVarCur], -pbXmax_[numVarCur + 1] + pbXmin_[numVarCur + 1]);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(max * 1e-3);
    nbElmdeMatrContraint_ += 3;

    pbSecondMembre_.push_back(0);
    pbNombreDeContraintes_++;

    LOG(debug) << metrix::log::verbose_constraints << "Ajout de la contrainte de desactivation d'un TD/HVDC curatif ";

    return METRIX_PAS_PROBLEME;
}

int Calculer::ajouterContrainteDeltaConsVarEntiere(const std::shared_ptr<TransformateurDephaseur>& td)
{
    int numVarEntiere = td->numVarEntiere_;
    int numVar = td->numVar_;

    // On ajoute 1 contrainte pour bloquer la variation du TD en N si la variable entiere d'activation vaut 0
    // a + b - delta*A < 0

    pbTypeContrainte_.push_back(COUPE_AUTRE);
    // indice de debut de la ligne dans les autres vecteur
    pbIndicesDebutDeLigne_.push_back(nbElmdeMatrContraint_);
    pbSens_.push_back('<');
    // nombre de coeff sur cette ligne
    pbNombreDeTermesDesLignes_.push_back(3);
    pbContraintes_.push_back(nullptr);

    // variables continues
    pbIndicesColonnes_.push_back(numVar);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(COEFF_MULT);

    pbIndicesColonnes_.push_back(numVar + 1);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(COEFF_MULT);

    // variable entiere : activation de ce curatif ou pas
    pbIndicesColonnes_.push_back(numVarEntiere);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(std::min(-td->puiMax_, td->puiMin_) * COEFF_MULT);
    nbElmdeMatrContraint_ += 3;

    pbSecondMembre_.push_back(0);
    pbNombreDeContraintes_++;

    LOG(debug) << metrix::log::verbose_constraints
               << "Ajout de la contrainte de desactivation d'un TD fictif en preventif";

    return METRIX_PAS_PROBLEME;
}


bool compareContraintes(const std::shared_ptr<Contrainte>& icdtQdt1, const std::shared_ptr<Contrainte>& icdtQdt2)
{
    if (icdtQdt1->type_ != icdtQdt2->type_) {
        return (icdtQdt1->type_ < icdtQdt2->type_);
    }
    double ecart1 = (icdtQdt1 != nullptr) ? icdtQdt1->ecart_ : 0.0;
    double ecart2 = (icdtQdt2 != nullptr) ? icdtQdt2->ecart_ : 0.0;
    return (ecart1 > ecart2);
}

void Calculer::traiterCuratif(const std::shared_ptr<ElementCuratif>& elemCur,
                              const std::shared_ptr<Incident>& icdtCourant,
                              int numOuvrSurv,
                              double& variationTdCuratif,
                              double& varTdSecondOrdre)
{
    if (!elemCur->estValide() || elemCur->positionVarCurative_ == -1) {
        return;
    }

    double varPuiss = 0.;

    // Coeffcient de report de 2 groupes correspondants au TD
    // variation de puissance
    int numVarCur = elemCur->positionVarCurative_;

    switch (elemCur->typeElem_) {
        case ElementCuratif::TD_FICTIF:
        case ElementCuratif::TD:
        case ElementCuratif::HVDC:
        case ElementCuratif::CONSO: varPuiss += -pbX_[numVarCur] + pbX_[numVarCur + 1]; break;
        case ElementCuratif::GROUPE: varPuiss += pbX_[numVarCur] - pbX_[numVarCur + 1]; break;
        default: LOG_ALL(error) << "Curative element of type " << elemCur->typeElem_ << " is unsupported"; return;
    }

    if (varPuiss != 0.) {
        const std::vector<double>& rho = elemCur->rho();
        variationTdCuratif += varPuiss * rho[numOuvrSurv];

        for (int i_q = 0; i_q < icdtCourant->nbLignes_; ++i_q) {
            const auto& quad2 = icdtCourant->listeQuads_[i_q];
            varTdSecondOrdre += rho[quad2->num_] * varPuiss * icdtCourant->rho_[i_q][numOuvrSurv];
        }
        for (int i_q = 0; i_q < icdtCourant->nbCouplagesFermes_; ++i_q) {
            const auto& quad2 = icdtCourant->listeCouplagesFermes_[i_q];
            varTdSecondOrdre += rho[quad2->num_] * varPuiss
                                * icdtCourant->rho_[icdtCourant->nbLignes_ + i_q][numOuvrSurv];
        }
    }
}

double Calculer::transitSurQuad(const std::shared_ptr<Quadripole>& quad,
                                std::shared_ptr<Incident> icdt,
                                const std::vector<double>& theta)
{
    auto incidentInitial = icdt;
    if (icdt && icdt->pochePerdue_) {
        if (icdt->pochePerdue_->pocheAvecConsoProd_) {
            return transitSurQuadIncidentNonConnexe(quad, icdt);
        }

        icdt = icdt->pochePerdue_->incidentModifie_;
    }

    double tranN = quad->u2Yij_ * (theta[quad->norqua_->num_] - theta[quad->nexqua_->num_]);

    if (!icdt) {
        return tranN;
    }


    // Traitement de l'incident
    double variationTranSurIncident = 0.;

    double puissance = 0.;
    int numQuadSurveille = quad->num_;

    for (int i_q = 0; i_q < icdt->nbLignes_; ++i_q) {
        const auto& quad2 = icdt->listeQuads_[i_q];
        // verification sur la presence du quad dans l'incident
        if (quad2->num_ == quad->num_) {
            return 0.;
        }
        puissance = quad2->u2Yij_ * (theta[quad2->norqua_->num_] - theta[quad2->nexqua_->num_]);
        variationTranSurIncident += puissance * icdt->rho_[i_q][numQuadSurveille];
    }

    for (int i_q = 0; i_q < icdt->nbCouplagesFermes_; ++i_q) {
        const auto& quad2 = icdt->listeCouplagesFermes_[i_q];
        // verification sur la presence du quad dans l'incident
        puissance = quad2->u2Yij_ * (theta[quad2->norqua_->num_] - theta[quad2->nexqua_->num_]);
        variationTranSurIncident += puissance * icdt->rho_[icdt->nbLignes_ + i_q][numQuadSurveille];
    }

    if (icdt->nbGroupes_ >= 1) {
        int nbQuadsInc = icdt->nbLignes_ + icdt->nbCouplagesFermes_;
        for (int i_p = 0; i_p < icdt->nbGroupes_; ++i_p) {
            const auto& grpe = icdt->listeGroupes_[i_p];
            if (!grpe->etat_) {
                continue;
            }

            double prodPerdue = grpe->prod_;
            if (grpe->prodAjust_ == Groupe::OUI_HR_AR || grpe->prodAjust_ == Groupe::OUI_AR) {
                int numVarGrp = grpe->numVarGrp_;
                prodPerdue += pbX_[numVarGrp] - pbX_[numVarGrp + 1];
            }

            // si incident ligne/groupe combine
            double rhoInc = 0.;
            for (int i_q = 0; i_q < icdt->nbLignes_; ++i_q) {
                const auto& quad2 = icdt->listeQuads_[i_q];
                rhoInc += icdt->rho_[nbQuadsInc + i_p][quad2->num_] * icdt->rho_[i_q][numQuadSurveille];
            }
            for (int i_q = 0; i_q < icdt->nbCouplagesFermes_; ++i_q) {
                const auto& quad2 = icdt->listeCouplagesFermes_[i_q];
                rhoInc += icdt->rho_[nbQuadsInc + i_p][quad2->num_]
                          * icdt->rho_[icdt->nbLignes_ + i_q][numQuadSurveille];
            }

            variationTranSurIncident -= prodPerdue * (icdt->rho_[nbQuadsInc + i_p][numQuadSurveille] + rhoInc);
        }
    }

    if (icdt->nbLccs_ >= 1) {
        for (int i_q = 0; i_q < icdt->nbLccs_; ++i_q) { // ATTENTION AU SIGNE -
            const auto& hvdc = icdt->listeLccs_[i_q];
            if (!hvdc->connecte()) {
                continue;
            }

            double rhoInc
                = icdt->rho_[icdt->nbLignes_ + icdt->nbCouplagesFermes_ + icdt->nbGroupes_ + i_q][numQuadSurveille];
            double prodPerdue = hvdc->puiCons_ + pbX_[hvdc->numVar_] - pbX_[hvdc->numVar_ + 1];

            variationTranSurIncident += prodPerdue * rhoInc;
            // Pour info, rhoInc contient la combinaison du report ligne/HVDC
        }
    }

    // Annulation de l'action des TD fictifs en N
    for (int i_p = 0; i_p < res_.nbCCEmulAC_; ++i_p) {
        double varPuiss = 0.;

        const auto& tdfictif = res_.TDFictifs_[i_p];
        if (pbX_[tdfictif->numVarEntiere_] > 0.5) {
            varPuiss = pbX_[tdfictif->numVar_] - pbX_[tdfictif->numVar_ + 1];

            if (varPuiss != 0.) {
                const std::vector<double>& rho = tdfictif->rho_;
                variationTranSurIncident += varPuiss * rho[numQuadSurveille];

                for (int i_q = 0; i_q < icdt->nbLignes_; ++i_q) {
                    const auto& quad2 = icdt->listeQuads_[i_q];
                    variationTranSurIncident += rho[quad2->num_] * varPuiss * icdt->rho_[i_q][numQuadSurveille];
                }
                for (int i_q = 0; i_q < icdt->nbCouplagesFermes_; ++i_q) {
                    const auto& quad2 = icdt->listeCouplagesFermes_[i_q];
                    variationTranSurIncident += rho[quad2->num_] * varPuiss
                                                * icdt->rho_[icdt->nbLignes_ + i_q][numQuadSurveille];
                }
            }
        }
    }

    double varTdSecondOrdre = 0.;
    double variationTdCuratif = 0.;

    if (incidentInitial->incidentATraiterEncuratif_) {
        for (const auto& elem : incidentInitial->listeElemCur_) {
            traiterCuratif(elem, icdt, numQuadSurveille, variationTdCuratif, varTdSecondOrdre);
        }
    }

    variationTranSurIncident += variationTdCuratif;
    variationTranSurIncident += varTdSecondOrdre;

    // transit sur incident
    return tranN + variationTranSurIncident;
}


/** Enregistre la menace correspondant a l'incident si elle fait partie des menaces max */
void enregistreMenaces(const std::shared_ptr<Incident>& icdt,
                       const std::shared_ptr<ElementASurveiller>& elemAS,
                       double transit)
{
    if (config::configuration().useItam() && !icdt->parade_) {
        // C'est une menace max apres incident et avant toute parade curative

        if (!config::inputConfiguration().useAllOutputs()
            && (!config::configuration().displayResultatsSurcharges()
                || estEnDepassementAvantManoeuvre(elemAS, transit))) {
            if (elemAS->menaceMaxAvantParade_.transit_ == config::constants::valdef
                || fabs(transit) - fabs(elemAS->menaceMaxAvantParade_.transit_) > config::constants::epsilon_threat) {
                elemAS->menaceMaxAvantParade_.transit_ = transit;
                elemAS->menaceMaxAvantParade_.defaut_ = icdt;
            }
        }
    }


    if (!icdt->paradesActivees_ && config::configuration().nbThreats() > 0) {
        // Classement des menaces max

        if (!config::inputConfiguration().useAllOutputs() && config::configuration().displayResultatsSurcharges()
            && !estEnDepassement(elemAS, icdt, transit)) {
            return;
        }

        if (elemAS->menacesMax_.size() < config::configuration().nbThreats()) {
            Menace menace;
            menace.defaut_ = icdt;
            menace.transit_ = transit;
            elemAS->menacesMax_.insert(menace);
        } else {
            auto it = elemAS->menacesMax_.begin();
            Menace menace = *it;
            if (fabs(transit) - fabs(menace.transit_) > config::constants::epsilon_threat) {
                elemAS->menacesMax_.erase(it);
                menace.defaut_ = icdt;
                menace.transit_ = transit;
                elemAS->menacesMax_.insert(menace);
            }
        }
    }
}


double Calculer::transitSurQuadIncidentNonConnexe(const std::shared_ptr<Quadripole>& quad,
                                                  const std::shared_ptr<Incident>& icdt) const
{
    const auto& quadsInc = icdt->listeQuads_;
    if (find(quadsInc.begin(), quadsInc.end(), quad) != quadsInc.end()) {
        return 0.;
    }

    const auto& poche = icdt->pochePerdue_;
    if (poche->noeudsPoche_.find(quad->norqua_) != poche->noeudsPoche_.end()
        || poche->noeudsPoche_.find(quad->nexqua_) != poche->noeudsPoche_.end()) {
        return 0.;
    }

    return quad->u2Yij_ * (poche->phases_[quad->norqua_->num_] - poche->phases_[quad->nexqua_->num_]);
}


int Calculer::detectionContraintes(const std::vector<double>& secondMembre /*phase*/, bool& existe_contrainte_active)
{
    double minT;
    double maxT;
    double tran;

    // initialisation du nombre de contraintes detectees
    nbCtr_ = 0;
    // identification des quadripoles en limite en N
    if (config::configuration().computationType()
        != config::Configuration::ComputationType::LOAD_FLOW) { // Pas necessaire qd on ne fait qu'un LF

        for (auto elemASIt = res_.elementsASurveillerN_.cbegin();
             elemASIt != res_.elementsASurveillerN_.cend() && nbCtr_ < icdtQdt_.size();
             ++elemASIt) {
            const auto& elemAS = elemASIt->second;
            elemAS->depassementEnN_ = 0.;

            if ((config::configuration().computationType()
                     == config::Configuration::ComputationType::OPF_WITHOUT_REDISPATCH
                 || config::configuration().computationType()
                        == config::Configuration::ComputationType::OPF_WITH_OVERLOAD)
                && elemAS->ecarts_.find(SITU_N) != elemAS->ecarts_.end()) {
                LOG(debug) << metrix::log::verbose_constraints << "Contrainte N deja entree dans le probleme pour "
                           << elemAS->nom_ << " (ecart = " << pbX_[elemAS->ecarts_.find(SITU_N)->second] << ")";
                int varEcart = elemAS->ecarts_.find(SITU_N)->second;
                if (varEcart != -1) {
                    elemAS->depassementEnN_ = fabs(pbX_[varEcart]);
                }
                continue; // on a deja une variable d'ecart pour cette contrainte
            }

            bool quadFictif = false;

            tran = 0.0;
            for (const auto& elem : elemAS->quadsASurv_) {
                const auto& quad = elem.first;
                double coeff = elem.second;

                if (!quad->connecte()) {
                    continue;
                }

                if (!elemAS->isWatchedSection) {
                    // In case of watched section, the TD shall not be controllable through constraints
                    quadFictif |= (quad->typeQuadripole_ == Quadripole::QUADRIPOLE_EMULATION_AC);
                }

                double transiQuad = transitSurQuad(quad, nullptr, secondMembre);
                tran += coeff * transiQuad;

            } // boucle sur les quadripoles de l'element a surveiller

            for (const auto& elem : elemAS->hvdcASurv_) {
                const auto& lcc = elem.first;
                double coeff = elem.second;

                if (!lcc->connecte()) {
                    continue;
                }

                tran += coeff * (lcc->puiCons_ + pbX_[lcc->numVar_] - pbX_[lcc->numVar_ + 1]);
            }

            // seuils max en N
            maxT = elemAS->seuilMax(nullptr);
            minT = elemAS->seuilMin(nullptr);

            if (tran > 0 && maxT != config::constants::valdef) {
                elemAS->depassementEnN_ = std::max(tran - maxT, 0.);
            } else if (tran < 0 && minT != -config::constants::valdef) {
                elemAS->depassementEnN_ = std::max(minT - tran, 0.);
            }

            if (elemAS->depassementEnN_ <= config::constants::acceptable_diff) {
                continue;
            }

            if ((config::configuration().computationType()
                     == config::Configuration::ComputationType::OPF_WITHOUT_REDISPATCH
                 || config::configuration().computationType()
                        == config::Configuration::ComputationType::OPF_WITH_OVERLOAD)
                && !quadFictif && !res_.actionsPreventivesPossibles_) { // on ne peut rien faire
                elemAS->ecarts_[SITU_N] = -1;
                LOG(debug) << metrix::log::verbose_constraints << "Contrainte ignoree en N pour " << elemAS->nom_
                           << " car pas d'action possible : (transit = " << tran << ")";
                continue;
            }

            existe_contrainte_active = true;
            if (!icdtQdt_[nbCtr_]) {
                icdtQdt_[nbCtr_] = std::make_shared<Contrainte>();
            }
            const auto& ctre = icdtQdt_[nbCtr_];
            ctre->elemAS_ = elemAS;
            ctre->icdt_ = nullptr;
            ctre->transit_ = tran;
            ctre->ecart_ = elemAS->depassementEnN_;
            ctre->ctrSup_ = (tran >= 0);
            ctre->maxT_ = maxT;
            ctre->minT_ = minT;
            ctre->type_ = quadFictif ? Contrainte::CONTRAINTE_EMUL_AC_N : Contrainte::CONTRAINTE_N;
            ctre->ecrireContrainte_ = true;
            ctre->numVarActivation_ = -1;
            nbCtr_++;

        } // End detection contraintes en N
    }
    // identification des quadripoles en limite en N-K
    double transNew = 0.;
    double ecart = 0.;
    sommeEcartsNk_ = 0.;

    bool premierIncidentValide = true;
    bool incidentAvecContrainte;

    for (auto icdtIt = res_.incidentsEtParades_.cbegin();
         icdtIt != res_.incidentsEtParades_.cend() && nbCtr_ < icdtQdt_.size();
         ++icdtIt) {
        const auto& icdt = *icdtIt;

        incidentAvecContrainte = false;

        if (!icdt->validite_) {
            continue;
        }

        // on saute les incidents/parades qui ne sont pas rentrees dans le pb ou pas activees par le solveur
        if (icdt->parade_) {
            if (icdt->numVarActivation_ == -1 || pbX_[icdt->numVarActivation_] < 0.5) {
                continue;
            }
            LOG(debug) << "Parade activee : " << icdt->nom_;
        }

        if (config::inputConfiguration().ignoreIncidentGroupAbsent()) {
            bool unGrpIndisponible = false;
            if (icdt->nbGroupes_ >= 1) {
                for (int i_p = 0; i_p < icdt->nbGroupes_; i_p++) {
                    if (!icdt->listeGroupes_[i_p]->etat_) {
                        unGrpIndisponible = true;
                        break;
                    }
                }
            }
            if (unGrpIndisponible) {
                continue;
            }
        }

        std::shared_ptr<Quadripole> quad;
        for (auto elemASIt = res_.elementsASurveillerNk_.cbegin();
             elemASIt != res_.elementsASurveillerNk_.cend() && nbCtr_ < icdtQdt_.size();
             ++elemASIt) {
            const auto& elemAS = elemASIt->second;

            if (premierIncidentValide) {
                // Reset du transit max sur incident
                elemAS->menacesMax_.clear();
                elemAS->menaceMaxAvantParade_.transit_ = config::constants::valdef;
                elemAS->menaceMaxAvantParade_.defaut_ = nullptr;
            }

            bool quadFictif = false;

            transNew = 0.0;
            for (const auto& elem : elemAS->quadsASurv_) {
                quad = elem.first;
                double coeff = elem.second;

                if (!quad->connecte()) {
                    if (icdt->nbCouplagesFermes_ == 0
                        || find(icdt->listeCouplagesFermes_.cbegin(), icdt->listeCouplagesFermes_.cend(), quad)
                               == icdt->listeCouplagesFermes_.cend()) {
                        continue; // on ne calcule pas les ouvrages ouverts sauf s'ils sont refermes par la parade
                    }
                }

                quadFictif |= (quad->typeQuadripole_ == Quadripole::QUADRIPOLE_EMULATION_AC);

                transNew += coeff * transitSurQuad(quad, icdt, secondMembre);

            } // boucle sur les quadripoles de l'element a surveiller

            for (const auto& elem : elemAS->hvdcASurv_) {
                const auto& lcc = elem.first;
                double coeff = elem.second;

                if (!lcc->connecte()
                    || (icdt->nbLccs_ > 0
                        && find(icdt->listeLccs_.begin(), icdt->listeLccs_.end(), lcc) != icdt->listeLccs_.end())) {
                    continue;
                }

                double puissHVDC = lcc->puiCons_ + pbX_[lcc->numVar_] - pbX_[lcc->numVar_ + 1];

                if (lcc->mode_ == CURATIF_POSSIBLE && icdt->incidentATraiterEncuratif_ && !icdt->lccElemCur_.empty()) {
                    auto itLcc = icdt->lccElemCur_.find(lcc);

                    if (itLcc != icdt->lccElemCur_.end()) {
                        const auto& elemC = itLcc->second;
                        if (elemC->positionVarCurative_ != -1) {
                            puissHVDC += pbX_[elemC->positionVarCurative_] - pbX_[elemC->positionVarCurative_ + 1];
                        }
                    }
                }
                transNew += coeff * puissHVDC;
            }

            // Enregistrement des menaces max
            enregistreMenaces(icdt, elemAS, transNew);

            if (icdt->paradesActivees_ && !config::configuration().useItam()) {
                continue;
            }

            // determination du seuil max
            maxT = elemAS->seuilMax(icdt);
            minT = elemAS->seuilMin(icdt);

            if (transNew > 0 && maxT != config::constants::valdef) {
                ecart = std::max(transNew - maxT, 0.);
            } else if (transNew < 0 && minT != -config::constants::valdef) {
                ecart = std::max(minT - transNew, 0.);
            } else {
                ecart = 0.;
            }

            if (!icdt->paradesActivees_ && elemAS->quadsASurv_.size() == 1 && elemAS->hvdcASurv_.empty()) {
                sommeEcartsNk_ += ecart;
            }

            if (config::configuration().computationType() == config::Configuration::ComputationType::LOAD_FLOW) {
                continue; // pas besoin d'aller plus loin en load-flow
            }

            if (config::configuration().computationType()
                    == config::Configuration::ComputationType::OPF_WITHOUT_REDISPATCH
                || config::configuration().computationType()
                       == config::Configuration::ComputationType::OPF_WITH_OVERLOAD) {
                if (elemAS->ecarts_.find(icdt->num_) != elemAS->ecarts_.end()) {
                    double valVarEcart = -1;
                    if (elemAS->ecarts_.find(icdt->num_)->second != -1) {
                        valVarEcart = pbX_[elemAS->ecarts_.find(icdt->num_)->second];
                    }
                    LOG(debug) << metrix::log::verbose_constraints << "Contrainte N-k '" << icdt->nom_
                               << "' deja dans le probleme pour " << elemAS->nom_ << " (var. ecart = " << valVarEcart
                               << ", ecart calc. = " << ecart << ")";
                    continue; // on a deje une variable d'ecart pour cette contrainte
                }

                if (!quadFictif && !res_.actionsPreventivesPossibles_ && icdt->listeElemCur_.empty()
                    && (!icdt->parade_ && icdt->parades_.empty())) { // on ne peut rien faire
                    LOG(debug) << metrix::log::verbose_constraints << "Contrainte ignoree pour " << elemAS->nom_
                               << " sur icdt " << icdt->nom_ << " car pas d'action possible : (transit = " << transNew
                               << ")";
                    elemAS->ecarts_[icdt->num_] = -1;
                    continue;
                }
            }

            // Y a-t-il une contrainte sur cet element a surveiller et sur cet incident ?
            if (ecart <= config::constants::acceptable_diff) {
                continue;
            }

            // gain de temps : permet de ne pas trop rentrer des contraintes redondantes
            // on n'ajoute pas les contraintes N-k qui seront resolues en N
            if (!quadFictif && elemAS->survMaxN_ == ElementASurveiller::SURVEILLE && ecart <= elemAS->depassementEnN_) {
                LOG(debug) << metrix::log::verbose_constraints << "Contrainte N-k pour " << elemAS->nom_ << " sur icdt "
                           << icdt->nom_ << " masquee par contrainte N";
                continue;
            }

            // On a une contrainte
            if (!icdtQdt_[nbCtr_]) {
                icdtQdt_[nbCtr_] = std::make_shared<Contrainte>();
            }
            const auto& ctre = icdtQdt_[nbCtr_];

            ctre->elemAS_ = elemAS;
            ctre->icdt_ = icdt;
            ctre->transit_ = transNew;
            ctre->ctrSup_ = transNew >= 0;
            ctre->maxT_ = maxT;
            ctre->minT_ = minT;
            ctre->type_ = quadFictif ? Contrainte::CONTRAINTE_EMULATION_AC : Contrainte::CONTRAINTE_N_MOINS_K;
            ctre->ecart_ = ecart;
            ctre->numVarActivation_ = -1;
            ctre->ecrireContrainte_ = true; // car a l'iteration precedente peut avoir ete mis a false

            nbCtr_++;
            existe_contrainte_active = true;
            incidentAvecContrainte = true;

            LOG(debug) << ctre->toString() << " Ecart : " << c_fmt("%.3f", ctre->ecart_);

        } // boucle sur les elements a surveiller

        premierIncidentValide = false;

        // Si un incident rompant la connnexite dont la poche est recuperable via une parade n'a pas provoque de
        // contrainte, on ajoute une contrainte fictive
        if (icdt->pocheRecuperableEncuratif_ && !incidentAvecContrainte && !icdt->paradesActivees_) {
            if (!icdtQdt_[nbCtr_]) {
                icdtQdt_[nbCtr_] = std::make_shared<Contrainte>();
            }
            const auto& ctre = icdtQdt_[nbCtr_];

            auto itQuadBegin = res_.elementsASurveillerNk_.begin();
            auto elemAS = itQuadBegin->second;
            do { // on cherche un quad qui ne soit pas dans l'incident
                elemAS = itQuadBegin->second;
                itQuadBegin++;
            } while (
                itQuadBegin != res_.elementsASurveillerNk_.end()
                && (elemAS->quadsASurv_.empty() || elemAS->seuilMax(icdt) == config::constants::valdef
                    || std::find(icdt->listeQuads_.begin(), icdt->listeQuads_.end(), elemAS->quadsASurv_.begin()->first)
                           != icdt->listeQuads_.end()));

            ctre->elemAS_ = elemAS;
            ctre->icdt_ = icdt;
            ctre->transit_ = config::constants::valdef;
            ctre->maxT_ = elemAS->seuilMax(icdt);
            ctre->minT_ = elemAS->seuilMin(icdt);
            ctre->type_ = Contrainte::CONTRAINTE_N_MOINS_K;
            ctre->ctrSup_ = true;
            ctre->ecart_ = config::constants::valdef;
            ctre->numVarActivation_ = -1;
            ctre->ecrireContrainte_ = true;

            if (quad) {
                LOG(debug) << metrix::log::verbose_constraints << "Ajout d'une contrainte fictive sur '" << quad->nom_
                           << "' pour recuperer une poche perdue de l'incident '" << icdt->nom_ << "'";
            }

            nbCtr_++;
            existe_contrainte_active = true;
        }
    } // boucle sur les incidents

    LOG_ALL(info) << err::ioDico().msg("INFOContraintesDetectees", c_fmt("%d", nbCtr_));

    // trier icdtQdt_
    std::sort(icdtQdt_.begin(), icdtQdt_.begin() + nbCtr_, compareContraintes);

    return METRIX_PAS_PROBLEME;
}

void Calculer::calculerFluxNk(const std::vector<double>& secondMembre)
{
    double transNew;

    for (const auto& icdt : res_.incidentsEtParades_) {
        if (!icdt->validite_) {
            continue;
        }

        // on saute les incidents/parades qui ne sont pas rentrees dans le pb ou pas activees par le solveur
        if (icdt->parade_ && (icdt->numVarActivation_ == -1 || pbX_[icdt->numVarActivation_] < 0.5)) {
            continue;
        }

        if (config::inputConfiguration().ignoreIncidentGroupAbsent()) {
            bool unGrpIndisponible = false;
            if (icdt->nbGroupes_ >= 1) {
                for (int i_p = 0; i_p < icdt->nbGroupes_; i_p++) {
                    if (!icdt->listeGroupes_[i_p]->etat_) {
                        unGrpIndisponible = true;
                        break;
                    }
                }
            }
            if (unGrpIndisponible) {
                continue;
            }
        }

        for (int j = 0; j < res_.nbQuadResultNk_; ++j) {
            const auto& elemAS = res_.elementsAvecResultatNk_[j];

            transNew = 0.0;
            for (const auto& elem : elemAS->quadsASurv_) {
                const auto& quad = elem.first;
                double coeff = elem.second;

                if (!quad->connecte()
                    && (icdt->nbCouplagesFermes_ == 0
                        || find(icdt->listeCouplagesFermes_.begin(), icdt->listeCouplagesFermes_.end(), quad)
                               == icdt->listeCouplagesFermes_.end())) {
                    continue; // on ne calcule pas les ouvrages ouverts sauf s'ils sont refermes par une parade
                }

                transNew += coeff * transitSurQuad(quad, icdt, secondMembre);

            } // boucle sur les quadripoles de l'element a surveiller

            for (const auto& elem : elemAS->hvdcASurv_) {
                const auto& lcc = elem.first;
                double coeff = elem.second;

                if (!lcc->connecte()
                    || (icdt->nbLccs_ > 0
                        && std::find(icdt->listeLccs_.begin(), icdt->listeLccs_.end(), lcc)
                               != icdt->listeLccs_.end())) {
                    continue;
                }

                double puissHVDC = lcc->puiCons_ + pbX_[lcc->numVar_] - pbX_[lcc->numVar_ + 1];

                if (lcc->mode_ == CURATIF_POSSIBLE && icdt->incidentATraiterEncuratif_ && !icdt->lccElemCur_.empty()) {
                    auto itLcc = icdt->lccElemCur_.find(lcc);

                    if (itLcc != icdt->lccElemCur_.end()) {
                        const auto& elemC = itLcc->second;
                        if (elemC->positionVarCurative_ != -1) {
                            puissHVDC += pbX_[elemC->positionVarCurative_] - pbX_[elemC->positionVarCurative_ + 1];
                        }
                    }
                }
                transNew += coeff * puissHVDC;
            }
            enregistreMenaces(icdt, elemAS, transNew);
        }
    }
}

void Calculer::choixContraintesAajouter()
{
    // heuristique pour eviter d'ajouter des contraintes quasi identiques :
    // comme une ligne peut etre surchargee de la meme maniere par X incidents (elle est peut etre meme chargee en N)
    // retourne le nombre de contrainte supprimees (pour ajuster la taille de la matrice)

    for (unsigned int i = 0; i < nbCtr_; ++i) {
        const auto& contrainte = icdtQdt_[i];
        contrainte->elemAS_->listeContraintes_.push_back(i);
    }

    for (unsigned int i = 0; i < nbCtr_; ++i) {
        const auto& contrainte = icdtQdt_[i];
        const auto& elemAS = contrainte->elemAS_;
        double tCalcIdt = fabs(contrainte->transit_);
        double tMaxIdt = contrainte->ctrSup_ ? contrainte->maxT_ : -contrainte->minT_;

        if (elemAS->listeContraintes_.size() == 1 || !contrainte->ecrireContrainte_) {
            continue;
        }

        for (auto index : elemAS->listeContraintes_) {
            // on ne teste que les incidents suivants qui sont donc moins contraignants
            if (index <= static_cast<int>(i)) {
                continue;
            }

            const auto& autreCont = icdtQdt_[index];
            double tCalcListe = fabs(autreCont->transit_);
            double tMaxListe = autreCont->ctrSup_ ? autreCont->maxT_ : autreCont->minT_;
            if ((contrainte->type_ != autreCont->type_)
                || (autreCont->numInc() != SITU_N
                    && res_.incidentsEtParades_[autreCont->numInc()]->listeElemCur_.empty()
                    && fabs((tCalcListe - tCalcIdt) / tCalcIdt) < 0.01 && fabs(tMaxListe - tMaxIdt) < 1)) {
                autreCont->ecrireContrainte_ = false;
                // nbCtr_ --;
                LOG(debug) << "on eclipse " << autreCont->toString() << " a cause de " << contrainte->toString();
            }
        }
    }

    // reset pour la prochaine micro iteration
    for (const auto& elem : res_.elementsASurveiller_) {
        elem->listeContraintes_.clear();
    }
}

void Calculer::addCurativeVariable(const std::shared_ptr<TransformateurDephaseur>& td, double proba, int numVarCur)
{
    // curatif TD
    typeEtat_.push_back(DEPH_CUR_H);
    pbXmin_.push_back(0.);
    pbXmax_.push_back(std::max(td->puiMax_ - td->puiMin_, 0.));
    typeEtat_.push_back(DEPH_CUR_B);
    pbXmin_.push_back(0.);
    pbXmax_.push_back(std::max(td->puiMax_ - td->puiMin_, 0.));
    // Penalisation du curatif
    if (config::configuration().usePenalisationTD()) {
        pbCoutLineaire_.push_back(config::configuration().costTd() * proba);
        pbCoutLineaire_.push_back(config::configuration().costTd() * proba);
        pbCoutLineaireSansOffset_.push_back(0.);
        pbCoutLineaireSansOffset_.push_back(0.);
    } else {
        pbCoutLineaire_.resize(pbNombreDeVariables_, 0.);
        pbCoutLineaireSansOffset_.resize(pbNombreDeVariables_, 0.);
    }

    ajouterContraintesBorneCuratif(td->numVar_, numVarCur, td->puiMin_ - td->puiCons_, td->puiMax_ - td->puiCons_);
}

void Calculer::addCurativeVariable(const std::shared_ptr<TransformateurDephaseur>& td_fictive)
{
    // Le curatif du TD fictif ne prend pas en compte la position preventive
    typeEtat_.push_back(DEPH_CUR_H);
    pbXmin_.push_back(0.);
    pbXmax_.push_back(td_fictive->puiMax_);
    typeEtat_.push_back(DEPH_CUR_B);
    pbXmin_.push_back(0.);
    pbXmax_.push_back(-td_fictive->puiMin_);
    pbCoutLineaire_.resize(pbNombreDeVariables_, config::constants::zero_cost_variable);
    pbCoutLineaireSansOffset_.resize(pbNombreDeVariables_, config::constants::zero_cost_variable);
}

void Calculer::addCurativeVariable(const std::shared_ptr<LigneCC>& lcc, double proba, int numVarCur)
{
    // curatif HVDC
    typeEtat_.push_back(HVDC_CUR_H);
    pbXmin_.push_back(0.);
    pbXmax_.push_back(std::max(lcc->puiMax_ - lcc->puiMin_, 0.));
    typeEtat_.push_back(HVDC_CUR_B);
    pbXmin_.push_back(0.);
    pbXmax_.push_back(std::max(lcc->puiMax_ - lcc->puiMin_, 0.));
    // Penalisation du curatif
    if (config::configuration().usePenalisationHVDC()) {
        pbCoutLineaire_.push_back(config::configuration().costHvdc() * proba);
        pbCoutLineaire_.push_back(config::configuration().costHvdc() * proba);
        pbCoutLineaireSansOffset_.push_back(0.);
        pbCoutLineaireSansOffset_.push_back(0.);
    } else {
        pbCoutLineaire_.resize(pbNombreDeVariables_, 0.);
        pbCoutLineaireSansOffset_.resize(pbNombreDeVariables_, 0.);
    }

    ajouterContraintesBorneCuratif(lcc->numVar_, numVarCur, lcc->puiMin_ - lcc->puiCons_, lcc->puiMax_ - lcc->puiCons_);
}

void Calculer::addCurativeVariable(const std::shared_ptr<Groupe>& grp, double proba, int numVarCur)
{
    // curatif Groupe
    typeEtat_.push_back(GRP_CUR_H);
    pbXmin_.push_back(0.);
    pbXmax_.push_back(std::max(grp->puisMax_ - grp->puisMin_, 0.));
    double value = std::max(grp->coutHausseAR_, config::configuration().noiseCost())
                   + config::configuration().redispatchCostOffset();
    pbCoutLineaire_.push_back(value * proba);
    pbCoutLineaireSansOffset_.push_back((grp->coutHausseAR_) * proba);
    typeEtat_.push_back(GRP_CUR_B);
    pbXmin_.push_back(0.);
    pbXmax_.push_back(std::max(grp->puisMax_ - grp->puisMin_, 0.));
    value = std::max(grp->coutBaisseAR_, config::configuration().noiseCost())
            + config::configuration().redispatchCostOffset();
    pbCoutLineaire_.push_back(value * proba);
    pbCoutLineaireSansOffset_.push_back((grp->coutBaisseAR_) * proba);
    ajouterContraintesBorneCuratifGroupe(grp->numVarGrp_, numVarCur, grp);
}

void Calculer::addCurativeVariable(const std::shared_ptr<Consommation>& conso, double proba, int numVarCur)
{
    // curatif Conso
    typeEtat_.push_back(CONSO_H);
    pbXmin_.push_back(0.);
    pbXmax_.push_back(0.);
    double value = std::max(conso->coutEffacement_, config::configuration().noiseCost())
                   + config::configuration().redispatchCostOffset();
    pbCoutLineaire_.push_back(value * proba);
    pbCoutLineaireSansOffset_.push_back((conso->coutEffacement_) * proba);
    typeEtat_.push_back(CONSO_B);
    pbXmin_.push_back(0.);
    value = std::max(conso->coutEffacement_, config::configuration().noiseCost())
            + config::configuration().redispatchCostOffset();
    pbCoutLineaire_.push_back(value * proba);
    pbCoutLineaireSansOffset_.push_back((conso->coutEffacement_) * proba);
    double effacementMax = conso->pourcentEffacement_ * conso->valeur_;
    if (effacementMax > config::constants::epsilon) {
        pbXmax_.push_back(effacementMax);
        if (conso->numVarConso_ >= 0) {
            ajouterContraintesBorneCuratifConso(
                conso->numVarConso_, numVarCur, conso->valeur_, conso->pourcentEffacement_);
        }
    } else {
        pbXmax_.push_back(0.);
    }
}

int Calculer::ajouterVariablesCuratives(const std::shared_ptr<ElementCuratif>& elem, double proba)
{
    int numVarCur = pbNombreDeVariables_;
    int numElem = elem->num();

    LOG(debug) << "Ajout des 2 variables curatives (" << numVarCur << "," << numVarCur + 1 << ") pour l'elt curatif "
               << numElem << " de type " << elem->typeElem_;

    pbNombreDeVariables_ += 2; // x+ et x-

    // Variable curative x+
    numSupportEtat_.push_back(numElem);
    pbPositionDeLaVariable_.push_back(HORS_BASE_SUR_BORNE_INF);
    pbTypeDeVariable_.push_back(REEL);
    pbTypeDeBorneDeLaVariable_.push_back(VARIABLE_BORNEE_DES_DEUX_COTES);
    pbX_.push_back(0.);

    // Variable curative x-
    numSupportEtat_.push_back(numElem);
    pbPositionDeLaVariable_.push_back(HORS_BASE_SUR_BORNE_INF);
    pbTypeDeVariable_.push_back(REEL);
    pbTypeDeBorneDeLaVariable_.push_back(VARIABLE_BORNEE_DES_DEUX_COTES);
    pbX_.push_back(0.);

    switch (elem->typeElem_) {
        case ElementCuratif::TD:
            addCurativeVariable(std::dynamic_pointer_cast<ElementCuratifTD>(elem)->td_, proba, numVarCur);
            break;
        case ElementCuratif::TD_FICTIF:
            addCurativeVariable(std::dynamic_pointer_cast<ElementCuratifTD>(elem)->td_);
            break;
        case ElementCuratif::HVDC:
            addCurativeVariable(std::dynamic_pointer_cast<ElementCuratifHVDC>(elem)->lcc_, proba, numVarCur);
            break;
        case ElementCuratif::GROUPE:
            addCurativeVariable(std::dynamic_pointer_cast<ElementCuratifGroupe>(elem)->groupe_, proba, numVarCur);
            break;
        case ElementCuratif::CONSO:
            addCurativeVariable(std::dynamic_pointer_cast<ElementCuratifConso>(elem)->conso_, proba, numVarCur);
            break;
        default:
            LOG_ALL(error) << "Curative element of type " << elem->typeElem_ << " is unsupported";
            return METRIX_PROBLEME;
    }


    if (pbXmax_[numVarCur] < config::constants::epsilon) {
        pbTypeDeBorneDeLaVariable_[numVarCur] = VARIABLE_FIXE;
    }
    if (pbXmax_[numVarCur + 1] < config::constants::epsilon) {
        pbTypeDeBorneDeLaVariable_[numVarCur + 1] = VARIABLE_FIXE;
    }

    return METRIX_PAS_PROBLEME;
}


int Calculer::ajouterVariableEntiere(int num, double cost)
{
    int indNewVar = pbNombreDeVariables_;
    LOG(debug) << "Ajout de la variable entiere " << indNewVar
               << " pour l'element curatif (TD, HVDC ou parade) : " << num;
    pbNombreDeVariables_++;

    numSupportEtat_.push_back(num);
    pbTypeDeBorneDeLaVariable_.push_back(VARIABLE_BORNEE_DES_DEUX_COTES);
    pbPositionDeLaVariable_.push_back(HORS_BASE_SUR_BORNE_SUP);
    pbTypeDeVariable_.push_back(ENTIER);

    typeEtat_.push_back(VAR_ENT);
    pbXmin_.push_back(0);
    pbXmax_.push_back(1);
    pbX_.push_back(1);

    // cout de la nouvelle variable
    pbCoutLineaire_.push_back(cost);
    pbCoutLineaireSansOffset_.push_back(0.);

    // Variable entiere : on change de solveur
    typeSolveur_ = UTILISATION_PNE_SOLVEUR;

    return indNewVar;
}


int Calculer::calculerCoeffEnN(const std::shared_ptr<Quadripole>& quad)
{
    int codeRetour = 1;
    std::vector<double>& b1 = quad->coeffN_;
    b1.resize(res_.nbNoeuds_, 0.0);

    // calcul des deux colonnes : quad->norqua_->num et quad->nexqua_->num_ de l'inverse
    b1[quad->norqua_->num_] = quad->u2Yij_;
    b1[quad->nexqua_->num_] = -quad->u2Yij_;
    LU_LuSolv(jacFactorisee_, &b1[0], &codeRetour, nullptr, 0, 0.0);

    // Attention il ne faut donc pas les prendre en compte
    // la sensibilite du transit aux ref de phase.
    std::map<int, int>::iterator itNb;
    for (itNb = res_.numNoeudBilanParZone_.begin(); itNb != res_.numNoeudBilanParZone_.end(); ++itNb) {
        b1[itNb->second] = 0.0;
    }
    return codeRetour;
}


int Calculer::coeffPourQuadEnN(const std::shared_ptr<Quadripole>& quad,
                               const std::vector<double>& secondMembreFixe,
                               double& sumcoeffFixe,
                               double coeff)
{
    if (quad->coeffN_.empty()) {
        LOG(debug) << "calcul des coefficients en N pour " << quad->nom_;
        calculerCoeffEnN(quad); // on ne recalcule les coefficient que si c'est necessaire
    }
    const std::vector<double>& b1 = quad->coeffN_;

    // somme de la prod imposee + la consommation
    for (unsigned int j = 0; j < b1.size(); ++j) {
        sumcoeffFixe += coeff * b1[j] * secondMembreFixe[j];
    }

    // calcul du vecteur coefs : Tmin <= coefs^T * x  <=Tmax
    int numSupportDeEtat = 0;

    int nbVarPrev = res_.nbVarGroupes_ + res_.nbVarConsos_ + res_.nbVarTd_ + res_.nbVarCc_;
    for (int j = 0; j < nbVarPrev; ++j) {
        numSupportDeEtat = numSupportEtat_[j];

        if (typeEtat_[j] == PROD_H) {
            coefs_[j] += coeff * b1[numSupportDeEtat];
        } else if (typeEtat_[j] == PROD_B) {
            coefs_[j] += coeff * (-b1[numSupportDeEtat]);
        } else if (typeEtat_[j] == CONSO_D) {
            coefs_[j] += coeff * b1[numSupportDeEtat];
        } else if (typeEtat_[j] == DEPH_H) {
            const auto& td = res_.tdParIndice_[numSupportDeEtat];
            double coeffInfl = coeff * (-b1[td->quad_->tnnorqua()] + b1[td->quad_->tnnexqua()]);
            if (config::constants::limit_action_td) {
                // limitation de l'influence du TD en deea d'un certain seuil
                if (max(fabs(coeffInfl * td->puiMax_), fabs(coeffInfl * td->puiMin_)) / quad->elemAS_->seuilMaxN_
                    > config::constants::threshold_influence_td) {
                    coefs_[j] += coeffInfl;
                } else {
                    coefs_[j] = 0.;
                    sumcoeffFixe += coeffInfl * pbX_[j];
                }
            } else {
                coefs_[j] += coeffInfl;
            }
        } else if (typeEtat_[j] == DEPH_B) {
            const auto& td = res_.tdParIndice_[numSupportDeEtat];
            double coeffInfl = coeff * (b1[td->quad_->tnnorqua()] - b1[td->quad_->tnnexqua()]);
            if (config::constants::limit_action_td) {
                // limitation de l'influence du TD en deea d'un certain seuil
                if (max(fabs(coeffInfl * td->puiMax_), fabs(coeffInfl * td->puiMin_)) / quad->elemAS_->seuilMaxN_
                    > config::constants::threshold_influence_td) {
                    coefs_[j] += coeffInfl;
                    LOG(debug) << "TD " << td->quadVrai_->nom_ << " agit en N sur le quadripole " << quad->nom_;
                } else {
                    coefs_[j] = 0.;
                    sumcoeffFixe += coeffInfl * pbX_[j];
                }
            } else {
                coefs_[j] += coeffInfl;
            }
        } else if (typeEtat_[j] == LIGNE_CC_H) {
            const auto& lcc = res_.lccParIndice_[numSupportDeEtat];
            double coeffInfl = coeff * (-b1[lcc->norqua_->num_] + b1[lcc->nexqua_->num_]);
            if (config::constants::limit_action_hvdc) {
                // limitation de l'influence des HVDC en deea d'un certain seuil
                if (max(fabs(coeffInfl * lcc->puiMax_), fabs(coeffInfl * lcc->puiMin_)) / quad->elemAS_->seuilMaxN_
                    > config::constants::threshold_influence_hvdc) {
                    coefs_[j] += coeffInfl;
                } else {
                    coefs_[j] = 0.;
                    sumcoeffFixe += coeffInfl * pbX_[j];
                }
            } else {
                coefs_[j] += coeffInfl;
            }
        } else if (typeEtat_[j] == LIGNE_CC_B) {
            const auto& lcc = res_.lccParIndice_[numSupportDeEtat];
            double coeffInfl = coeff * (b1[lcc->norqua_->num_] - b1[lcc->nexqua_->num_]);
            if (config::constants::limit_action_hvdc) {
                // limitation de l'influence des HVDC en deea d'un certain seuil
                if (max(fabs(coeffInfl * lcc->puiMax_), fabs(coeffInfl * lcc->puiMin_)) / quad->elemAS_->seuilMaxN_
                    > config::constants::threshold_influence_hvdc) {
                    coefs_[j] += coeffInfl;
                    LOG(debug) << "LCC " << lcc->nom_ << " agit en N sur le quadripole " << quad->nom_;
                } else {
                    coefs_[j] = 0.;
                    sumcoeffFixe += coeffInfl * pbX_[j];
                }
            } else {
                coefs_[j] += coeffInfl;
            }
        } else {
            LOG_ALL(error) << err::ioDico().msg("ERRCalcInterne") << " Contrainte N :"
                           << "typeEtat_ " << typeEtat_[j] << " de la variable numero " << j
                           << " ne peut pas etre traite";
            return METRIX_PROBLEME;
        }
    }
    return METRIX_PAS_PROBLEME;
}

// Calcule les coeffcients a mettre dans la matrice du simplexe pour ecrire une contrainte de transit
// sur quad et sur l'incident icdt
int Calculer::coeffPourQuadInc(const std::shared_ptr<Quadripole>& quad,
                               const std::shared_ptr<Contrainte>& ctre,
                               const std::vector<double>& secondMembreFixe,
                               double& sumcoeffFixe,
                               double coeff)
{
    LOG(debug) << "Traitement contrainte : " << ctre->toString();

    const auto& icdt = ctre->icdt_;

    if (!icdt) {
        // Traitement particulier pour les quads representant une HVDC en emulation AC
        if (ctre->type_ == Contrainte::CONTRAINTE_EMUL_AC_N) {
            const auto& td = res_.TransfoDephaseurs_[quad->nom_];
            ctre->numVarActivation_ = td->numVarEntiere_;
            pbTypeDeBorneDeLaVariable_[td->numVarEntiere_] = VARIABLE_BORNEE_DES_DEUX_COTES;
        }

        // Calcul des coefficients
        return coeffPourQuadEnN(quad, secondMembreFixe, sumcoeffFixe, coeff);
    }

    // Recherche si un element devrait agir en curatif
    if (!icdt->incidentATraiterEncuratif_ && !icdt->listeElemCur_.empty()) {
        std::map<int, std::vector<int>> variablesCurativesGrp;
        std::map<int, std::vector<int>> variablesCurativesConso;
        std::map<int, std::vector<std::shared_ptr<ElementCuratifHVDC>>> variablesCurativesHvdc;
        for (unsigned int u = 0; u < icdt->listeElemCur_.size(); ++u) {
            const auto& elemC = icdt->listeElemCur_[u];

            if (!elemC->estValide()) {
                continue;
            }

            icdt->incidentATraiterEncuratif_ = true;

            double proba = icdt->getProb();

            if (elemC->positionVarCurative_ == -1) {
                // ajout des variables curatives pour chaque element curatif
                elemC->positionVarCurative_ = pbNombreDeVariables_;

                ajouterVariablesCuratives(elemC, proba);

                if (elemC->typeElem_ == ElementCuratif::TD_FICTIF) {
                    // ajout d'une variable entiere contraindre l'action en emulation AC
                    elemC->positionVarEntiereCur_ = ajouterVariableEntiere(elemC->num(),
                                                                           config::constants::cost_whole_variable * u);
                    ajouterContrainteDeltaConsVarEntiere(elemC);
                    pbX_[elemC->positionVarEntiereCur_] = 0;
                    pbTypeDeBorneDeLaVariable_[elemC->positionVarEntiereCur_] = VARIABLE_FIXE;
                } else if (config::configuration().useCurative()) {
                    // ajout d'une variable entiere pour comptage des actions curative
                    elemC->positionVarEntiereCur_ = ajouterVariableEntiere(elemC->num(),
                                                                           config::constants::cost_whole_variable * u);
                    ajouterContrainteDeltaConsVarEntiere(elemC);
                }

                if (elemC->typeElem_ == ElementCuratif::GROUPE) {
                    variablesCurativesGrp[elemC->zoneSynchrone()].push_back(elemC->positionVarCurative_);
                } else if (elemC->typeElem_ == ElementCuratif::CONSO) {
                    variablesCurativesConso[elemC->zoneSynchrone()].push_back(elemC->positionVarCurative_);
                } else if (elemC->typeElem_ == ElementCuratif::HVDC) {
                    auto elemCurHvdc = std::dynamic_pointer_cast<ElementCuratifHVDC>(elemC);
                    const auto& hvdcCur = elemCurHvdc->lcc_;
                    if (hvdcCur->norqua_->numCompSynch_ != hvdcCur->nexqua_->numCompSynch_) {
                        variablesCurativesHvdc[hvdcCur->norqua_->numCompSynch_].push_back(elemCurHvdc);
                        variablesCurativesHvdc[hvdcCur->nexqua_->numCompSynch_].push_back(elemCurHvdc);
                    }
                }
            }
        }

        if (coefs_.size() < static_cast<size_t>(pbNombreDeVariables_)) {
            coefs_.resize(pbNombreDeVariables_, 0.);
        }

        if (!variablesCurativesGrp.empty() || !variablesCurativesConso.empty()) {
            ecrireEquationBilanCuratif(variablesCurativesGrp, variablesCurativesConso, variablesCurativesHvdc);
            LOG(debug) << "Ecriture des contraintes de bilan energetique curatif pour l'incident " << icdt->nom_;
        }

        if (config::configuration().limitCurativeGrp() >= 0 && !variablesCurativesGrp.empty()) {
            ajouterLimiteCuratifGroupe(variablesCurativesGrp);
            LOG(debug) << "Ecriture des contraintes de limitation en curatif pour l'incident " << icdt->nom_;
        }
    }

    // Association de l'element curatif pour les contraintes d'emulation AC
    if (ctre->type_ == Contrainte::CONTRAINTE_EMULATION_AC) {
        auto elemCurIt = icdt->tdFictifsElemCur_.find(quad);
        if (elemCurIt != icdt->tdFictifsElemCur_.end()) {
            const auto& elemC = elemCurIt->second;
            ctre->numVarActivation_ = elemC->positionVarEntiereCur_;
            pbTypeDeBorneDeLaVariable_[elemC->positionVarEntiereCur_] = VARIABLE_BORNEE_DES_DEUX_COTES;
        }
    }

    auto icdtCalcul = icdt;
    if (icdt->pochePerdue_) {
        if (icdt->pochePerdue_->pocheAvecConsoProd_) {
            // Cas particulier des incidents rompant la connexite quand on ne peut pas utiliser les coefficients de
            // report
            return coeffPourQuadIncRompantConnexite(quad, icdt, sumcoeffFixe, coeff);
        }
        icdtCalcul = icdt->pochePerdue_->incidentModifie_;
    }

    // calcul des sensibilites
    if (quad->coeffN_.empty()) {
        calculerCoeffEnN(quad); // on ne recalcule les coefficients que si c'est necessaire
        LOG(debug) << "calcul des coefficients en N pour " << quad->nom_;
    }
    const std::vector<double>& b1 = quad->coeffN_;

    int indQuad = quad->num_;
    for (unsigned int j1 = 0; j1 < b1.size(); ++j1) {
        sumcoeffFixe += coeff * b1[j1] * secondMembreFixe[j1];

        for (int i_q = 0; i_q < icdtCalcul->nbLignes_; ++i_q) {
            sumcoeffFixe += coeff * icdtCalcul->rho_[i_q][indQuad] * icdtCalcul->lambda_[i_q][j1]
                            * secondMembreFixe[j1];
        }

        for (int i_q = 0; i_q < icdtCalcul->nbCouplagesFermes_; ++i_q) {
            sumcoeffFixe += coeff * icdtCalcul->rho_[icdtCalcul->nbLignes_ + i_q][indQuad]
                            * icdtCalcul->lambda_[icdtCalcul->nbLignes_ + i_q][j1] * secondMembreFixe[j1];
        }
    }

    // Partie fixe des incidents groupe
    for (int jg = 0; jg < icdtCalcul->nbGroupes_; ++jg) {
        const auto& grp = icdtCalcul->listeGroupes_[jg];

        if (!grp->etat_) {
            continue;
        }

        double rho_incident = 0.;

        int indRhoGrp = icdtCalcul->nbLignes_ + icdtCalcul->nbCouplagesFermes_ + jg;

        for (int i_q = 0; i_q < icdtCalcul->nbLignes_; ++i_q) {
            rho_incident += icdtCalcul->rho_[indRhoGrp][icdtCalcul->listeQuads_[i_q]->num_]
                            * icdtCalcul->rho_[i_q][indQuad];
        }
        for (int i_q = 0; i_q < icdtCalcul->nbCouplagesFermes_; ++i_q) {
            rho_incident += icdtCalcul->rho_[indRhoGrp][icdtCalcul->listeCouplagesFermes_[i_q]->num_]
                            * icdtCalcul->rho_[icdtCalcul->nbLignes_ + i_q][indQuad];
        }
        sumcoeffFixe -= coeff * ((icdtCalcul->rho_[indRhoGrp][indQuad] + rho_incident) * grp->prod_);
    }

    // Partie fixe des incidents lcc
    for (int jl = 0; jl < icdtCalcul->nbLccs_; ++jl) {
        if (icdtCalcul->listeLccs_[jl]->connecte()) {
            sumcoeffFixe += icdtCalcul->rho_[icdtCalcul->nbLignes_ + icdtCalcul->nbCouplagesFermes_
                                             + icdtCalcul->nbGroupes_ + jl][indQuad]
                            * icdtCalcul->listeLccs_[jl]->puiCons_;
        }
    }

    // calcul du vecteur coefs : Tmin <= coefs^T * x  <=Tmax
    int numSupportDeEtat;
    double coefsIncident;

    int nbVarPrev = res_.nbVarGroupes_ + res_.nbVarConsos_ + res_.nbVarTd_ + res_.nbVarCc_;
    for (int j1 = 0; j1 < nbVarPrev; ++j1) {
        numSupportDeEtat = numSupportEtat_[j1];
        coefsIncident = 0.;

        // Pour les variables preventives
        // Incidents lignes
        for (int i_q = 0; i_q < icdtCalcul->nbLignes_; ++i_q) {
            if (typeEtat_[j1] == PROD_H || typeEtat_[j1] == PROD_B || typeEtat_[j1] == CONSO_D) {
                coefsIncident += icdtCalcul->rho_[i_q][indQuad] * icdtCalcul->lambda_[i_q][numSupportDeEtat];
            } else if (typeEtat_[j1] == DEPH_H || typeEtat_[j1] == DEPH_B) {
                coefsIncident += icdtCalcul->rho_[i_q][indQuad]
                                 * (-icdtCalcul->lambda_[i_q][res_.tdParIndice_[numSupportDeEtat]->quad_->tnnorqua()]
                                    + icdtCalcul->lambda_[i_q][res_.tdParIndice_[numSupportDeEtat]->quad_->tnnexqua()]);
            } else if (typeEtat_[j1] == LIGNE_CC_H || typeEtat_[j1] == LIGNE_CC_B) {
                coefsIncident += icdtCalcul->rho_[i_q][indQuad]
                                 * (-icdtCalcul->lambda_[i_q][res_.lccParIndice_[numSupportDeEtat]->norqua_->num_]
                                    + icdtCalcul->lambda_[i_q][res_.lccParIndice_[numSupportDeEtat]->nexqua_->num_]);
            }
        }
        for (int i_q = 0; i_q < icdtCalcul->nbCouplagesFermes_; ++i_q) {
            if (typeEtat_[j1] == PROD_H || typeEtat_[j1] == PROD_B || typeEtat_[j1] == CONSO_D) {
                coefsIncident += icdtCalcul->rho_[icdtCalcul->nbLignes_ + i_q][indQuad]
                                 * icdtCalcul->lambda_[icdtCalcul->nbLignes_ + i_q][numSupportDeEtat];
            } else if (typeEtat_[j1] == DEPH_H || typeEtat_[j1] == DEPH_B) {
                coefsIncident += icdtCalcul->rho_[icdtCalcul->nbLignes_ + i_q][indQuad]
                                 * (-icdtCalcul->lambda_[icdtCalcul->nbLignes_ + i_q]
                                                        [res_.tdParIndice_[numSupportDeEtat]->quad_->tnnorqua()]
                                    + icdtCalcul->lambda_[icdtCalcul->nbLignes_ + i_q]
                                                         [res_.tdParIndice_[numSupportDeEtat]->quad_->tnnexqua()]);
            } else if (typeEtat_[j1] == LIGNE_CC_H || typeEtat_[j1] == LIGNE_CC_B) {
                coefsIncident += icdtCalcul->rho_[icdtCalcul->nbLignes_ + i_q][indQuad]
                                 * (-icdtCalcul->lambda_[icdtCalcul->nbLignes_ + i_q]
                                                        [res_.lccParIndice_[numSupportDeEtat]->norqua_->num_]
                                    + icdtCalcul->lambda_[icdtCalcul->nbLignes_ + i_q]
                                                         [res_.lccParIndice_[numSupportDeEtat]->nexqua_->num_]);
            }
        }

        // Incidents groupes
        if (icdtCalcul->nbGroupes_ >= 1 && (typeEtat_[j1] == PROD_B || typeEtat_[j1] == PROD_H)) {
            for (int jg = 0; jg < icdtCalcul->nbGroupes_; ++jg) {
                if (!icdtCalcul->listeGroupes_[jg]->etat_) {
                    continue;
                }
                int numVarGroupe = icdtCalcul->listeGroupes_[jg]->numVarGrp_;
                if (j1 == numVarGroupe || j1 == numVarGroupe + 1) {
                    // on a une variable du groupe

                    // ici on cumule le coefficient de report ligne avec celui du groupe
                    double rho_incident = 0.;
                    for (int i_q = 0; i_q < icdtCalcul->nbLignes_; ++i_q) {
                        rho_incident += icdtCalcul->rho_[icdtCalcul->nbLignes_ + icdtCalcul->nbCouplagesFermes_ + jg]
                                                        [icdtCalcul->listeQuads_[i_q]->num_]
                                        * icdtCalcul->rho_[i_q][indQuad];
                    }
                    for (int i_q = 0; i_q < icdtCalcul->nbCouplagesFermes_; ++i_q) {
                        rho_incident += icdtCalcul->rho_[icdtCalcul->nbLignes_ + icdtCalcul->nbCouplagesFermes_ + jg]
                                                        [icdtCalcul->listeCouplagesFermes_[i_q]->num_]
                                        * icdtCalcul->rho_[icdtCalcul->nbLignes_ + i_q][indQuad];
                    }

                    coefsIncident
                        -= (icdtCalcul->rho_[icdtCalcul->nbLignes_ + icdtCalcul->nbCouplagesFermes_ + jg][indQuad]
                            + rho_incident);
                }
            } // end for inc grp
        }

        // Incidents lcc
        if (icdtCalcul->nbLccs_ >= 1 && (typeEtat_[j1] == LIGNE_CC_H || typeEtat_[j1] == LIGNE_CC_B)) {
            for (int jl = 0; jl < icdtCalcul->nbLccs_; ++jl) {
                if (icdtCalcul->listeLccs_[jl]->connecte()) {
                    if (numSupportDeEtat == static_cast<int>(icdtCalcul->listeLccs_[jl]->num_)) {
                        coefsIncident += icdtCalcul->rho_[icdtCalcul->nbLignes_ + icdtCalcul->nbCouplagesFermes_
                                                          + icdtCalcul->nbGroupes_ + jl][indQuad];
                    }
                }
            }
        }

        // Calcul des coefficient a mettre dans la matrice des contraintes
        if (typeEtat_[j1] == PROD_H) {
            coefs_[j1] += coeff * (b1[numSupportDeEtat] + coefsIncident);
        } else if (typeEtat_[j1] == PROD_B) {
            coefs_[j1] += coeff * -(b1[numSupportDeEtat] + coefsIncident);
        } else if (typeEtat_[j1] == CONSO_D) {
            coefs_[j1] += coeff * (b1[numSupportDeEtat] + coefsIncident);
        } else if (typeEtat_[j1] == DEPH_H) {
            const auto& td = res_.tdParIndice_[numSupportDeEtat];

            if (td->fictif_) {
                // dans ce cas le dephasage initial ne doit pas etre pris en compte
                coefs_[j1] = 0.;
                continue;
            }

            double coeffInfl = coeff * (b1[td->quad_->tnnexqua()] - b1[td->quad_->tnnorqua()] + coefsIncident);
            if (config::constants::limit_action_td) {
                // limitation de l'influence du TD en deca d'un certain seuil
                if (std::max(fabs(coeffInfl * td->puiMax_), fabs(coeffInfl * td->puiMin_)) / quad->elemAS_->seuilMaxInc_
                    > config::constants::threshold_influence_td) {
                    coefs_[j1] += coeffInfl;
                    LOG(debug) << "TD " << td->quadVrai_->nom_ << " agit sur le quadripole " << quad->nom_
                               << " pour inc " << icdt->nom_;
                } else {
                    coefs_[j1] = 0.;
                    sumcoeffFixe += coeffInfl * pbX_[j1];
                }
            } else {
                coefs_[j1] += coeffInfl;
            }
        } else if (typeEtat_[j1] == DEPH_B) {
            const auto& td = res_.tdParIndice_[numSupportDeEtat];

            if (td->fictif_) { // le dephasage initial ne doit pas etre pris en compte sur incident
                coefs_[j1] = 0.;
                continue;
            }

            double coeffInfl = -coeff * (b1[td->quad_->tnnexqua()] - b1[td->quad_->tnnorqua()] + coefsIncident);
            if (config::constants::limit_action_td) {
                // limitation de l'influence du TD en deea d'un certain seuil
                if (std::max(fabs(coeffInfl * td->puiMax_), fabs(coeffInfl * td->puiMin_)) / quad->elemAS_->seuilMaxInc_
                    > config::constants::threshold_influence_td) {
                    coefs_[j1] += coeffInfl;
                    LOG(debug) << "TD " << td->quadVrai_->nom_ << " agit sur le quadripole " << quad->nom_
                               << " pour inc " << icdt->nom_;
                } else {
                    coefs_[j1] = 0.;
                    sumcoeffFixe += coeffInfl * pbX_[j1];
                }
            } else {
                coefs_[j1] += coeffInfl;
            }
        } else if (typeEtat_[j1] == LIGNE_CC_H) {
            const auto& lcc = res_.lccParIndice_[numSupportDeEtat];
            double coeffInfl = coeff * (b1[lcc->nexqua_->num_] - b1[lcc->norqua_->num_] + coefsIncident);
            if (config::constants::limit_action_hvdc) {
                // limitation de l'influence des HVDC en deea d'un certain seuil
                if (std::max(fabs(coeffInfl * lcc->puiMax_), fabs(coeffInfl * lcc->puiMin_))
                        / quad->elemAS_->seuilMaxInc_
                    > config::constants::threshold_influence_hvdc) {
                    coefs_[j1] += coeffInfl;
                } else {
                    coefs_[j1] = 0.;
                    sumcoeffFixe += coeffInfl * pbX_[j1];
                }
            } else {
                coefs_[j1] += coeffInfl;
            }
        } else if (typeEtat_[j1] == LIGNE_CC_B) {
            const auto& lcc = res_.lccParIndice_[numSupportDeEtat];
            double coeffInfl = -coeff * (b1[lcc->nexqua_->num_] - b1[lcc->norqua_->num_] + coefsIncident);
            if (config::constants::limit_action_hvdc) {
                // limitation de l'influence des HVDC en deca d'un certain seuil
                if (std::max(fabs(coeffInfl * lcc->puiMax_), fabs(coeffInfl * lcc->puiMin_))
                        / quad->elemAS_->seuilMaxInc_
                    > config::constants::threshold_influence_hvdc) {
                    coefs_[j1] += coeffInfl;
                    LOG(debug) << "LCC " << lcc->nom_ << " agit sur le quadripole " << quad->nom_ << " pour inc "
                               << icdt->nom_;
                } else {
                    coefs_[j1] = 0.;
                    sumcoeffFixe += coeffInfl * pbX_[j1];
                }
            } else {
                coefs_[j1] += coeffInfl;
            }
        }
    } // Fin variables preventives

    // Actions des elements curatifs
    if (icdt->incidentATraiterEncuratif_) {
        for (const auto& elemC : icdt->listeElemCur_) {
            if (!elemC->estValide()) {
                continue;
            }

            coefsIncident = 0.0;

            const std::vector<double>& rho = elemC->rho();
            double rho_incident = 0.;
            for (int i_q = 0; i_q < icdtCalcul->nbLignes_; ++i_q) {
                rho_incident += icdtCalcul->rho_[i_q][indQuad] * rho[icdtCalcul->listeQuads_[i_q]->num_];
            }
            for (int i_q = 0; i_q < icdtCalcul->nbCouplagesFermes_; ++i_q) {
                rho_incident += icdtCalcul->rho_[icdtCalcul->nbLignes_ + i_q][indQuad]
                                * rho[icdtCalcul->listeCouplagesFermes_[i_q]->num_];
            }

            if (elemC->positionVarCurative_ == -1) {
                LOG_ALL(warning) << "No variable in curative for incident " << icdt->nom_;
            }

            coefsIncident -= (rho[indQuad] + rho_incident);

            int j1 = elemC->positionVarCurative_;

            if (typeEtat_[j1] == DEPH_CUR_H || typeEtat_[j1] == HVDC_CUR_H || typeEtat_[j1] == CONSO_H) {
                coefs_[j1] += coeff * coefsIncident;
                coefs_[j1 + 1] -= coeff * coefsIncident;
            } else if (typeEtat_[j1] == GRP_CUR_H) {
                coefs_[j1] -= coeff * coefsIncident;
                coefs_[j1 + 1] += coeff * coefsIncident;
            } else {
                LOG_ALL(error) << err::ioDico().msg("ERRCalcInterne") << "  typeEtat_ " << typeEtat_[j1]
                               << " de la variable numero " << j1 << " ne peut pas etre traite";
                return METRIX_PROBLEME;
            }
        }
    }

    return METRIX_PAS_PROBLEME;
}


int Calculer::coeffPourQuadIncRompantConnexite(const std::shared_ptr<Quadripole>& quad,
                                               const std::shared_ptr<Incident>& icdt,
                                               double& sumcoeffFixe,
                                               double coeff)
{
    const auto& poche = icdt->pochePerdue_;
    std::vector<double>& secondMembreFixe = poche->secondMembreFixe_;

    if (poche->coefficients_.find(quad) == poche->coefficients_.end()) {
        LOG(debug) << "Calcul des coefficients pour " << quad->nom_ << " sur l'incident " << icdt->nom_;
        int codeRetour;
        std::vector<double> coeffQuad(res_.nbNoeuds_, 0.0);

        // calcul des deux colonnes : quad->norqua_->num et quad->nexqua_->num_ de l'inverse
        coeffQuad[quad->norqua_->num_] = quad->u2Yij_;
        coeffQuad[quad->nexqua_->num_] = -quad->u2Yij_;
        MATRICE* jacFactorisee = nullptr;
        ListeQuadsIncident listeQuadsIncident = poche->incidentModifie_->listeQuadsIncident();
        if (listeQuadsIncident.quadsOuverts.empty() && listeQuadsIncident.quadsRefermes.empty()) {
            jacFactorisee = jacFactorisee_;
        } else {
            jacFactorisee = jacIncidentsModifies_.find(listeQuadsIncident)->second;
        }
        LU_LuSolv(jacFactorisee, &coeffQuad[0], &codeRetour, nullptr, 0, 0.0);

        // Attention il ne faut donc pas les prendre en compte
        // la sensibilite du transit aux ref de phase.
        std::map<int, int>::iterator itNb;
        for (itNb = res_.numNoeudBilanParZone_.begin(); itNb != res_.numNoeudBilanParZone_.end(); ++itNb) {
            coeffQuad[itNb->second] = 0.0;
        }

        poche->coefficients_[quad] = coeffQuad;
    }

    std::vector<double>& b1 = poche->coefficients_[quad];

    // somme de la prod imposee + la consommation
    for (unsigned int j = 0; j < b1.size(); ++j) {
        sumcoeffFixe += coeff * b1[j] * secondMembreFixe[j];
    }

    int numSupportDeEtat;

    const auto& noeudsPoche = poche->noeudsPoche_;

    int nbVarPrev = res_.nbVarGroupes_ + res_.nbVarConsos_ + res_.nbVarTd_ + res_.nbVarCc_;
    for (int j = 0; j < nbVarPrev; ++j) {
        numSupportDeEtat = numSupportEtat_[j];

        if (typeEtat_[j] == PROD_H || typeEtat_[j] == CONSO_D) {
            if (noeudsPoche.find(res_.noeuds_[numSupportDeEtat]) != noeudsPoche.end()) {
                continue; // cet element est dans la poche
            }

            coefs_[j] += coeff * b1[numSupportDeEtat];
        } else if (typeEtat_[j] == PROD_B) {
            if (noeudsPoche.find(res_.noeuds_[numSupportDeEtat]) != noeudsPoche.end()) {
                continue; // cet element est dans la poche
            }

            coefs_[j] += -coeff * b1[numSupportDeEtat];
        } else if (typeEtat_[j] == DEPH_H) {
            const auto& td = res_.tdParIndice_[numSupportDeEtat];

            if (noeudsPoche.find(td->quadVrai_->norqua_) != noeudsPoche.end()) {
                continue; // cet element est dans la poche
            }

            if (td->fictif_) {
                // dans ce cas le dephasage initial ne doit pas etre pris en compte
                coefs_[j] = 0.;
                continue;
            }

            double coeffInfl = coeff * (-b1[td->quad_->tnnorqua()] + b1[td->quad_->tnnexqua()]);
            if (config::constants::limit_action_td) {
                // limitation de l'influence du TD en deea d'un certain seuil
                if (max(fabs(coeffInfl * td->puiMax_), fabs(coeffInfl * td->puiMin_)) / quad->elemAS_->seuilMaxN_
                    > config::constants::threshold_influence_td) {
                    coefs_[j] += coeffInfl;
                } else {
                    coefs_[j] = 0.;
                    sumcoeffFixe += coeffInfl * pbX_[j];
                }
            } else {
                coefs_[j] += coeffInfl;
            }
        } else if (typeEtat_[j] == DEPH_B) {
            const auto& td = res_.tdParIndice_[numSupportDeEtat];

            if (noeudsPoche.find(td->quadVrai_->norqua_) != noeudsPoche.end()) {
                continue; // cet element est dans la poche
            }

            if (td->fictif_) {
                // dans ce cas le dephasage initial ne doit pas etre pris en compte
                coefs_[j] = 0.;
                continue;
            }

            double coeffInfl = coeff * (b1[td->quad_->tnnorqua()] - b1[td->quad_->tnnexqua()]);
            if (config::constants::limit_action_td) {
                // limitation de l'influence du TD en deca d'un certain seuil
                if (max(fabs(coeffInfl * td->puiMax_), fabs(coeffInfl * td->puiMin_)) / quad->elemAS_->seuilMaxN_
                    > config::constants::threshold_influence_td) {
                    coefs_[j] += coeffInfl;
                } else {
                    coefs_[j] = 0.;
                    sumcoeffFixe += coeffInfl * pbX_[j];
                }
            } else {
                coefs_[j] += coeffInfl;
            }
        } else if (typeEtat_[j] == LIGNE_CC_H) {
            const auto& lcc = res_.lccParIndice_[numSupportDeEtat];
            double coeffInfl = -coeff * (b1[lcc->norqua_->num_] - b1[lcc->nexqua_->num_]);
            if (config::constants::limit_action_hvdc) {
                // limitation de l'influence des HVDC en deea d'un certain seuil
                if (max(fabs(coeffInfl * lcc->puiMax_), fabs(coeffInfl * lcc->puiMin_)) / quad->elemAS_->seuilMaxN_
                    > config::constants::threshold_influence_hvdc) {
                    coefs_[j] += coeffInfl;
                } else {
                    coefs_[j] = 0.;
                    sumcoeffFixe += coeffInfl * pbX_[j];
                }
            } else {
                coefs_[j] += coeffInfl;
            }
        } else if (typeEtat_[j] == LIGNE_CC_B) {
            const auto& lcc = res_.lccParIndice_[numSupportDeEtat];
            double coeffInfl = coeff * (b1[lcc->norqua_->num_] - b1[lcc->nexqua_->num_]);
            if (config::constants::limit_action_hvdc) {
                // limitation de l'influence des HVDC en deea d'un certain seuil
                if (max(fabs(coeffInfl * lcc->puiMax_), fabs(coeffInfl * lcc->puiMin_)) / quad->elemAS_->seuilMaxN_
                    > config::constants::threshold_influence_hvdc) {
                    coefs_[j] += coeffInfl;
                    LOG(debug) << "LCC " << lcc->nom_ << " agit en N sur le quadripole " << quad->nom_;
                } else {
                    coefs_[j] = 0.;
                    sumcoeffFixe += coeffInfl * pbX_[j];
                }
            } else {
                coefs_[j] += coeffInfl;
            }
        } else {
            LOG_ALL(error) << err::ioDico().msg("ERRCalcInterne") << "Contrainte N-k :"
                           << "typeEtat_ " << typeEtat_[j] << " de la variable numero " << j
                           << " ne peut pas etre traite";
            return METRIX_PROBLEME;
        }
    }

    // Traitement du curatif
    if (icdt->incidentATraiterEncuratif_) {
        for (const auto& elemC : icdt->listeElemCur_) {
            if (!elemC->estValide()) {
                continue;
            }

            int j = elemC->positionVarCurative_;

            if (j == -1) {
                continue;
            }

            numSupportDeEtat = numSupportEtat_[j];

            if (typeEtat_[j] == GRP_CUR_H) {
                if (noeudsPoche.find(res_.noeuds_[numSupportDeEtat]) != noeudsPoche.end()) {
                    continue; // cet element est dans la poche
                }

                coefs_[j] += coeff * b1[numSupportDeEtat];
                coefs_[j + 1] -= coeff * b1[numSupportDeEtat];
            } else if (typeEtat_[j] == CONSO_H) {
                if (noeudsPoche.find(res_.noeuds_[numSupportDeEtat]) != noeudsPoche.end()) {
                    continue; // cet element est dans la poche
                }

                coefs_[j] += -coeff * b1[numSupportDeEtat];
                coefs_[j + 1] -= -coeff * b1[numSupportDeEtat];
            } else if (typeEtat_[j] == DEPH_CUR_H) {
                const auto& td = res_.tdParIndice_[numSupportDeEtat];
                if (noeudsPoche.find(td->quadVrai_->norqua_) != noeudsPoche.end()) {
                    continue; // cet element est dans la poche
                }

                double valCoeff = coeff * (-b1[td->quad_->tnnorqua()] + b1[td->quad_->tnnexqua()]);
                coefs_[j] += valCoeff;
                coefs_[j + 1] -= valCoeff;
            } else if (typeEtat_[j] == HVDC_CUR_H) {
                const auto& lcc = res_.lccParIndice_[numSupportDeEtat];
                if (noeudsPoche.find(lcc->norqua_) != noeudsPoche.end()) {
                    continue; // cet element est dans la poche
                }

                double valCoeff = -coeff * (b1[lcc->norqua_->num_] - b1[lcc->nexqua_->num_]);
                coefs_[j] += valCoeff;
                coefs_[j + 1] -= valCoeff;
            } else {
                LOG_ALL(error) << err::ioDico().msg("ERRCalcInterne") << "Contrainte N-k :"
                               << "typeEtat_ " << typeEtat_[j] << " de la variable numero " << j
                               << " ne peut pas etre traite";
                return METRIX_PROBLEME;
            }
        }
    }
    return METRIX_PAS_PROBLEME;
}

bool ouvragesEnContrainteDeconnectes(const std::shared_ptr<Contrainte>& contrainte,
                                     const std::shared_ptr<Incident>& parade)
{
    bool ouvragesDeconnectes = true;
    const auto& quadSurv = contrainte->elemAS_->quadsASurv_;

    for (const auto& elem : quadSurv) {
        const auto& quad = elem.first;
        if (quad->connecte()) {
            if (std::find(parade->listeQuads_.begin(), parade->listeQuads_.end(), quad) == parade->listeQuads_.end()) {
                ouvragesDeconnectes = false;
                break;
            }
        } else {
            if (std::find(parade->listeCouplagesFermes_.begin(), parade->listeCouplagesFermes_.end(), quad)
                != parade->listeCouplagesFermes_.end()) {
                ouvragesDeconnectes = false;
                break;
            }
        }
    }
    return ouvragesDeconnectes;
}


int Calculer::ajoutContrainteActivationParade(const std::shared_ptr<Incident>& parade,
                                              const std::shared_ptr<Contrainte>& contrainte,
                                              const std::shared_ptr<Incident>& icdtPere,
                                              const std::vector<double>& secondMembreFixe)
{
    // Ajout d'une contrainte d'activation de la parade
    if (!parade->contraintesAutorisees_.empty()
        && parade->contraintesAutorisees_.find(contrainte->elemAS_) != parade->contraintesAutorisees_.end()) {
        auto contrActivation = std::make_shared<Contrainte>(*contrainte);
        contrActivation->type_ = Contrainte::CONTRAINTE_ACTIVATION;
        contrActivation->numVarActivation_ = parade->numVarActivation_;
        contrActivation->icdt_ = icdtPere;

        if (ajoutContrainte(contrActivation, secondMembreFixe) == METRIX_PROBLEME) {
            return METRIX_PROBLEME;
        }

        // Liberation de la variable entiere
        pbXmax_[parade->numVarActivation_] = 1;
        pbTypeDeVariable_[parade->numVarActivation_] = ENTIER;
        LOG(debug) << "Ajout d'une contrainte d'activation pour la parade '" << parade->nom_ << "' sur '"
                   << contrainte->elemAS_->nom_ << "'";
    }
    return METRIX_PAS_PROBLEME;
}


int Calculer::ajoutContraintes(bool& existe_contrainte_active,
                               int& nbNewContreParVariante,
                               const std::vector<double>& secondMembreFixe,
                               const std::vector<double>& theta)
{
    if (!existe_contrainte_active) {
        LOG_ALL(info) << err::ioDico().msg("INFOPasDeContraintes");
        return METRIX_PAS_PROBLEME;
    }

    unsigned int nombreDeContraintesMicroIteration = nbCtr_;
    if (nombreDeContraintesMicroIteration < 1) {
        return METRIX_PROBLEME;
    }

    if (nombreDeContraintesMicroIteration > config::constants::nb_max_contraints_by_iteration) {
        if (config::configuration().computationType() == config::Configuration::ComputationType::OPF
            || config::configuration().computationType() == config::Configuration::ComputationType::OPF_WITH_OVERLOAD) {
            // En mode OPF, on supprime les contraintes redondantes
            if (numMicroIteration_ <= 5) {
                choixContraintesAajouter();
                nombreDeContraintesMicroIteration = config::constants::nb_max_contraints_by_iteration;
            }
        }
    }

    FILE* fr = nullptr;
    if (config::inputConfiguration().writeConstraintsFile()) {
        std::stringstream ss("contraintes_%d_%d.txt");
        ss << varianteCourante_->num_ << "_" << numMicroIteration_ << ".txt";
        fr = fopen(ss.str().c_str(), "w+");
    }
    int codeRet = 1;

    std::set<std::shared_ptr<Incident>> paradesActiveesDansMicroiteration;

    unsigned int cpt = 0;
    unsigned int cptAll = 0; // Compteurs du nombre de contraintes ajoutees dans la microiteration
    for (unsigned int i = 0; (i < nbCtr_) && (cpt < nombreDeContraintesMicroIteration); ++i) {
        const auto& contrainte = icdtQdt_[i];

        if (!contrainte->ecrireContrainte_) {
            continue;
        }

        const auto& icdt = contrainte->icdt_;

        // C'est la premiere contrainte d'un incident avec parades : ajout de toutes les parades dans le probleme
        if (icdt && !icdt->parades_.empty() && !icdt->paradesActivees_) {
            std::vector<std::shared_ptr<Incident>> paradesAjoutees;
            icdt->paradesActivees_ = true;
            paradesActiveesDansMicroiteration.insert(icdt);


            for (unsigned int u = 0; u < icdt->parades_.size(); ++u) {
                // Lors du 1er ajout de contraintes avec cet incident, (paradesActivees_ = false)
                // on ajoute toutes les parades possibles sur cet incident
                const auto& parade = icdt->parades_[u];

                if (!parade->validite_) {
                    continue;
                }

                if (ouvragesEnContrainteDeconnectes(contrainte, parade)) {
                    // La parade fait tomber tous les ouvrages de l'ElementASurveiller donc pas vraiment de contrainte
                    LOG(debug) << "Ouvrage en contrainte deconnecte par la parade : "
                               << contrainte->elemAS_->nom_ + " / " + parade->nom_;

                    if (config::inputConfiguration().writeConstraintsFile()) {
                        fprintf(
                            fr,
                            "Cur : N/A; Transit = %8.3f; MaxT = 99999; MinT = 99999; ElemAS = %s (%d); nomQicdt = %s "
                            "(%d)\n",
                            contrainte->transit_,
                            contrainte->elemAS_->nom_.c_str(),
                            contrainte->elemAS_->num_,
                            parade->nom_.c_str(),
                            parade->num_);
                    }
                    paradesAjoutees.push_back(parade);

                    // le poids de l'activation de la parade dépend de la probabilité de l'incident relatif à cette
                    // parade
                    double proba = icdt->getProb();

                    parade->numVarActivation_ = ajouterVariableEntiere(
                        parade->num_,
                        config::constants::cost_parade * proba * static_cast<double>(icdt->contraintes_.size()));

                    if (!parade->contraintesAutorisees_.empty()
                        && parade->contraintesAutorisees_.find(contrainte->elemAS_)
                               == parade->contraintesAutorisees_.end()) {
                        pbTypeDeVariable_[parade->numVarActivation_] = REEL;
                        pbXmax_[parade->numVarActivation_] = 0;
                        LOG(debug) << "Variable no " << parade->numVarActivation_ << " de la parade '" << parade->nom_
                                   << "' desactivee.";
                    }

                    icdt->contraintes_.push_back(-1);

                    // Ajout eventuel d'une contrainte d'activation de la parade
                    ajoutContrainteActivationParade(parade, contrainte, icdt, secondMembreFixe);

                    // Valorisation de la poche perdue
                    if (parade->pochePerdue_ && parade->pochePerdue_->pocheAvecConsoProd_) {
                        if (lienParadeValorisation_.find(parade) == lienParadeValorisation_.end()) {
                            // Ajout de la variable et de la contrainte de valorisation
                            lienParadeValorisation_[parade] = ajoutContrainteValorisationPoche(parade);
                        }
                    }

                    if (config::configuration().useCurative()) {
                        // si les actions curatives possibles sur cette parade sont nombreuses
                        // -> ajout d'une contrainte pour limiter a nbMaxActCur actions
                        int cptActCurPoss = parade->nbLignes_ + parade->nbCouplagesFermes_ - icdt->nbLignes_;
                        cptActCurPoss += parade->listeElemCur_.size() - icdt->tdFictifsElemCur_.size();

                        if (cptActCurPoss > 0
                            && static_cast<unsigned int>(cptActCurPoss)
                                   > config::configuration().nbMaxActionCurative()) {
                            LOG(debug) << "===> Limitation du nb d'actions pour " << parade->nom_;
                            ajouterContrainteNbMaxActCur(parade);
                        }
                    }
                    continue;
                }

                if (config::constants::eval_parade) {
                    // Heuristique verifiant l'efficacite de la parade a priori avant de l'entrer dans le pb
                    // -------------------------------------------------------------------------------------
                    if (icdt->parades_.size() > 2
                        && u > 0) { // on ne verifie que s'il y a plus de 2 parades pour cet incident

                        const auto& quad = contrainte->elemAS_->quadsASurv_.begin()
                                               ->first; // il n'y a qu'un quad car pas de sect. surv. en n-k
                        double transitApresParade = transitSurQuad(quad, parade, theta);
                        if (fabs(transitApresParade)
                            > config::constants::threshold_heuristic_parade * fabs(contrainte->transit_)) {
                            LOG(debug) << "La parade " << parade->nom_
                                       << " est ignoree. T_avant = " << contrainte->transit_
                                       << ", T_apres = " << transitApresParade;
                            continue;
                        }

                        LOG(debug) << "--> Parade " << parade->nom_ << " T_avant = " << contrainte->transit_
                                   << ", T_apres = " << transitApresParade;
                    }
                }

                // creation d'une contrainte pour la parade (pointant sur la parade et non l'incident pere)
                auto contrDeParade = std::make_shared<Contrainte>(
                    contrainte->elemAS_,
                    parade,
                    config::constants::valdef,
                    contrainte->maxT_ - config::constants::singularisation_parade * u,
                    contrainte->minT_ + config::constants::singularisation_parade * u,
                    contrainte->ecart_,
                    contrainte->type_ == Contrainte::CONTRAINTE_EMULATION_AC ? Contrainte::CONTRAINTE_EMULATION_AC
                                                                             : Contrainte::CONTRAINTE_PARADE,
                    contrainte->ctrSup_);

                codeRet = ajoutContrainte(contrDeParade, secondMembreFixe);
                if (codeRet == METRIX_PROBLEME) {
                    return METRIX_PROBLEME;
                }
                if (codeRet == METRIX_PAS_PROBLEME) {
                    // La contrainte a vraiment ete ajoutee

                    if (config::inputConfiguration().writeConstraintsFile()) {
                        fprintf(fr,
                                "Cur : %d; Transit = %8.3f; MaxT = %8.3f; MinT =%8.3f;  ElemAS = %s (%d); nomQicdt = "
                                "%s (%d)\n",
                                contrDeParade->num_,
                                contrainte->transit_,
                                contrDeParade->maxT_,
                                contrainte->minT_,
                                contrDeParade->elemAS_->nom_.c_str(),
                                contrDeParade->elemAS_->num_,
                                contrDeParade->nomInc().c_str(),
                                contrDeParade->numInc());
                    }
                    paradesAjoutees.push_back(parade);
                    cptAll++;
                    existe_contrainte_active = true;

                    if (config::configuration().useCurative()) {
                        // si les actions curatives possibles sur cette parade sont nombreuses
                        // -> ajout d'une contrainte pour limiter a nbMaxActCur actions
                        int cptActCurPoss = parade->nbLignes_ + parade->nbCouplagesFermes_ - icdt->nbLignes_;
                        cptActCurPoss += parade->listeElemCur_.size() - icdt->tdFictifsElemCur_.size();

                        if (cptActCurPoss > 0
                            && static_cast<unsigned int>(cptActCurPoss)
                                   > config::configuration().nbMaxActionCurative()) {
                            LOG(debug) << "===> Limitation du nb d'actions pour " << parade->nom_;
                            ajouterContrainteNbMaxActCur(parade);
                        }
                    }

                    // Ajout eventuel d'une contrainte d'activation de la parade
                    ajoutContrainteActivationParade(parade, contrainte, icdt, secondMembreFixe);
                }
            } // fin ajout des contraintes d'une parade

            // ajout d'une contrainte pour qu'une seule parade topo ne soit active en meme temps
            // il faut qu'il y en ait au moins une afin d'etre sur qu on se ramene au seuil N
            ajouterContrainteChoixTopo(paradesAjoutees);

            // Ajout de la contrainte sur seuil ITAM
            if (config::configuration().useItam()) {
                double transitAvantCur = contrainte->transit_;
                double ecart = 0;
                double maxT = contrainte->elemAS_->seuilMax(icdt);
                double minT = contrainte->elemAS_->seuilMin(icdt);

                if (transitAvantCur > 0 && maxT != config::constants::valdef) {
                    ecart = max(transitAvantCur - maxT, 0.);
                } else if (transitAvantCur < 0 && minT != -config::constants::valdef) {
                    ecart = max(minT - transitAvantCur, 0.);
                }

                if (ecart > config::constants::acceptable_diff) {
                    auto contrSeuilItam = std::make_shared<Contrainte>(*contrainte);
                    contrSeuilItam->maxT_ = maxT;
                    contrSeuilItam->minT_ = minT;
                    contrSeuilItam->ecart_ = ecart;

                    codeRet = ajoutContrainte(contrSeuilItam, secondMembreFixe);

                    if (codeRet == METRIX_PROBLEME) {
                        return METRIX_PROBLEME;
                    }
                    if (codeRet == METRIX_PAS_PROBLEME) {
                        // La contrainte a vraiment ete ajoutee

                        if (config::inputConfiguration().writeConstraintsFile()) {
                            fprintf(
                                fr,
                                "Cur : %d; Transit = %8.3f; MaxT = %8.3f; MinT =%8.3f;  ElemAS = %s (%d); nomQicdt = "
                                "%s (%d)\n",
                                contrSeuilItam->num_,
                                contrSeuilItam->transit_,
                                contrSeuilItam->maxT_,
                                contrSeuilItam->minT_,
                                contrSeuilItam->elemAS_->nom_.c_str(),
                                contrSeuilItam->elemAS_->num_,
                                contrSeuilItam->nomInc().c_str(),
                                contrSeuilItam->numInc());
                        }
                        cptAll++;
                        existe_contrainte_active = true;
                    }
                }
            }

        } // end traitement des parades d'un incidents

        else if (icdt && icdt->parade_) {
            // C'est une contrainte N-k liee a une parade, on ajoute egalement les contraintes des autres parades du
            // meme incident

            const auto& icdtPere = icdt->incTraiteCur_;

            for (unsigned int j = 0; j < icdtPere->parades_.size(); ++j) {
                const auto& parade = icdtPere->parades_[j];

                if ((parade->numVarActivation_ != -1)               // entree dans le pb
                    && (pbXmax_[parade->numVarActivation_] != 0)) { // non inhibee pour cause d'equivalence

                    if (ouvragesEnContrainteDeconnectes(contrainte, parade)) {
                        LOG(debug) << "Ouvrage en contrainte deconnecte par la parade : "
                                   << contrainte->elemAS_->nom_ + " / " + parade->nom_;
                        continue; // Cette parade fait tomber tous les ouvrages de l'ElementASurveiller donc pas de
                                  // contrainte
                    }

                    // creation d'une nouvelle contrainte tmp pour cette parade
                    auto contrDeParade = std::make_shared<Contrainte>(*contrainte);
                    contrDeParade->icdt_ = parade;
                    // seuils: Les seuils sont les memes
                    contrDeParade->maxT_ = contrainte->maxT_ - config::constants::singularisation_parade * j;
                    contrDeParade->minT_ = contrainte->minT_ + config::constants::singularisation_parade * j;
                    if (contrainte->icdt_ != parade) {
                        contrDeParade->transit_ = config::constants::valdef;
                    }

                    codeRet = ajoutContrainte(contrDeParade, secondMembreFixe);
                    if (codeRet == METRIX_PROBLEME) {
                        return METRIX_PROBLEME;
                    }
                    if (codeRet == METRIX_PAS_PROBLEME) {
                        cptAll++; // compteur par microiteration
                        existe_contrainte_active = true;

                        if (config::inputConfiguration().writeConstraintsFile()) {
                            string typeInc = contrainte->typeDeContrainteToString();

                            fprintf(
                                fr,
                                "%s : %d; Transit = %8.3f; MaxT = %8.3f; MinT = %8.3f;  ElemAS = %s (%d); nomQicdt = "
                                "%s (%d)\n",
                                typeInc.c_str(),
                                contrDeParade->num_,
                                contrDeParade->transit_,
                                contrDeParade->maxT_,
                                contrDeParade->minT_,
                                contrDeParade->elemAS_->nom_.c_str(),
                                contrDeParade->elemAS_->num_,
                                contrDeParade->nomInc().c_str(),
                                contrDeParade->numInc());
                        }
                    }
                }
            }
        }

        else if (icdt && paradesActiveesDansMicroiteration.find(icdt) != paradesActiveesDansMicroiteration.end()) {
            // C'est une autre contrainte N-k liee e un incident dont on vient d'activer les parades

            for (unsigned int j = 0; j < icdt->parades_.size(); ++j) {
                const auto& parade = icdt->parades_[j];
                if ((parade->numVarActivation_ != -1) // entree dans le pb
                    && (pbTypeDeBorneDeLaVariable_[parade->numVarActivation_]
                        != VARIABLE_FIXE)) { // non inhibee pour cause d'equivalence

                    std::map<Quadripole*, double>::const_iterator quadIt;

                    if (ouvragesEnContrainteDeconnectes(contrainte, parade)) {
                        LOG(debug) << "Ouvrage en contrainte deconnecte par la parade : "
                                   << contrainte->elemAS_->nom_ + " / " + parade->nom_;

                        // Ajout eventuel d'une contrainte d'activation de la parade
                        ajoutContrainteActivationParade(parade, contrainte, icdt, secondMembreFixe);

                        continue; // Cette parade fait tomber tous les ouvrages de l'ElementASurveiller donc pas de
                                  // contrainte
                    }

                    // creation d'une nouvelle contrainte tmp pour cette parade
                    auto contrDeParade = std::make_shared<Contrainte>(*contrainte);
                    contrDeParade->icdt_ = parade;
                    // seuils: Les seuils sont les memes
                    contrDeParade->maxT_ = contrainte->maxT_ - config::constants::singularisation_parade * j;
                    contrDeParade->minT_ = contrainte->minT_ + config::constants::singularisation_parade * j;
                    contrDeParade->transit_ = config::constants::valdef;

                    codeRet = ajoutContrainte(contrDeParade, secondMembreFixe);
                    if (codeRet == METRIX_PROBLEME) {
                        return METRIX_PROBLEME;
                    }
                    if (codeRet == METRIX_PAS_PROBLEME) {
                        cptAll++; // compteur par microiteration
                        existe_contrainte_active = true;

                        if (config::inputConfiguration().writeConstraintsFile()) {
                            string typeInc = contrainte->typeDeContrainteToString();

                            fprintf(
                                fr,
                                "%s : %d; Transit = %8.3f; MaxT = %8.3f; MinT = %8.3f;  ElemAS = %s (%d); nomQicdt = "
                                "%s (%d)\n",
                                typeInc.c_str(),
                                contrDeParade->num_,
                                contrDeParade->transit_,
                                contrDeParade->maxT_,
                                contrDeParade->minT_,
                                contrDeParade->elemAS_->nom_.c_str(),
                                contrDeParade->elemAS_->num_,
                                contrDeParade->nomInc().c_str(),
                                contrDeParade->numInc());
                        }
                    }

                    // Ajout eventuel d'une contrainte d'activation de la parade
                    ajoutContrainteActivationParade(parade, contrDeParade, icdt, secondMembreFixe);
                }
            }
        }

        else {
            // Ajout d'une contrainte de transit en N ou sur un incident hors parade

            codeRet = ajoutContrainte(contrainte, secondMembreFixe);

            if (codeRet == METRIX_PAS_PROBLEME) {
                // existe_contrainte_active = true;
                cptAll++; // compteur par microiteration
                existe_contrainte_active = true;
            } else if (codeRet == METRIX_CONTRAINTE_IGNOREE) {
                LOG(debug) << metrix::log::verbose_constraints << "Contrainte ignoree : " << contrainte->toString();
            } else {
                LOG_ALL(warning) << "Erreur ou pas de solution pour la contrainte : " << contrainte->toString();
                return codeRet;
            }

            if (config::inputConfiguration().writeConstraintsFile()) {
                string typeInc = contrainte->typeDeContrainteToString();
                string numContrainte = contrainte->num_ == -1 ? "IGNOREE" : c_fmt("%d", contrainte->num_);

                fprintf(fr,
                        "%s : %s; T(N) = %8.3f; MaxT = %8.3f; MinT = %8.3f; ElemAS = %s (%d); nomQicdt = %s (%d)\n",
                        typeInc.c_str(),
                        numContrainte.c_str(),
                        contrainte->transit_,
                        contrainte->maxT_,
                        contrainte->minT_,
                        contrainte->elemAS_->nom_.c_str(),
                        contrainte->elemAS_->num_,
                        contrainte->nomInc().c_str(),
                        contrainte->numInc());
            }
        }

        cpt++; // compteur par microiteration

    } // fin de la boucle sur les contraintes a ajouter dans le probleme SPX

    LOG_ALL(info) << err::ioDico().msg("INFOContraintesAjoutee", c_fmt("%d", cptAll));

    if (cptAll == 0) {
        existe_contrainte_active = false;
    }

    if (config::inputConfiguration().writeConstraintsFile()) {
        fclose(fr);
    }

    nbNewContreParVariante += cpt;

    return METRIX_PAS_PROBLEME;
}

int Calculer::ajoutContrainte(const std::shared_ptr<Contrainte>& ctre, const std::vector<double>& secondMembreFixe)
{
    if ((ctre->ctrSup_ && ctre->maxT_ == config::constants::valdef)
        || (!ctre->ctrSup_ && ctre->minT_ == -config::constants::valdef)) {
        LOG(debug) << metrix::log::verbose_constraints << "Contrainte ignoree car Tmax non defini";
        return METRIX_CONTRAINTE_IGNOREE;
    }
    // Ajoute une contrainte dans le probleme a optimiser envoye au simplexe
    // 1. remplissage du vecteur coeff
    // 2. ecriture de la contrainte ( ecrireCoupeTransit)
    double tmpSumCoeff = 0.;
    double maxTHorsPartieFixe = 0;
    double minTHorsPartieFixe = 0;

    const auto& elemAS = ctre->elemAS_;

    double sumcoeffFixe = 0.;

    coefs_.assign(pbNombreDeVariables_, 0.);

    int codeRet;

    for (const auto& elem : elemAS->quadsASurv_) {
        const auto& quad = elem.first;
        double coeff = elem.second; // 1 si ouvrage et <>1 si section surv.

        // calcul des coeffs a mettre dans la matrice du simplexe pour introduire la contrainte
        codeRet = coeffPourQuadInc(quad, ctre, secondMembreFixe, sumcoeffFixe, coeff);
        if (codeRet == METRIX_PROBLEME) {
            return METRIX_PROBLEME;
        }
    }

    for (const auto& elem : elemAS->hvdcASurv_) {
        const auto& lcc = elem.first;
        double coeff = elem.second;

        if (!lcc->connecte()) {
            continue;
        }

        sumcoeffFixe += coeff * lcc->puiCons_;

        coefs_[lcc->numVar_] += coeff;
        coefs_[lcc->numVar_ + 1] -= coeff;

        const auto& icdt = ctre->icdt_;
        if (icdt && lcc->mode_ == CURATIF_POSSIBLE && icdt->incidentATraiterEncuratif_ && !icdt->lccElemCur_.empty()) {
            auto itLcc = icdt->lccElemCur_.find(lcc);

            if (itLcc != icdt->lccElemCur_.end()) {
                const auto& elemC = itLcc->second;
                if (elemC->positionVarCurative_ != -1) {
                    coefs_[elemC->positionVarCurative_] += coeff;
                    coefs_[elemC->positionVarCurative_ + 1] -= coeff;
                }
            }
        }
    }

    // systeme de verification que les coefficients calcules correpondent
    // bien a la contrainte detectee ( cela cherche une eventuelle incoherence entre les phases detection et ecrire de
    // la contrainte)
    double transitDetecte = ctre->transit_;

    // Aide au debug des variables curatives

    for (int j1 = 0; j1 < pbNombreDeVariables_; ++j1) {
        tmpSumCoeff += pbX_[j1] * coefs_[j1];
    }

    // corriger les contraintes des injections par la partie fixe des seconds membres
    maxTHorsPartieFixe = ctre->maxT_ - sumcoeffFixe;
    minTHorsPartieFixe = ctre->minT_ - sumcoeffFixe;

    if (config::inputConfiguration().checkConstraintLevel()
            == config::InputConfiguration::CheckConstraintLevel::LOAD_FLOW
        && ctre->type_ != Contrainte::CONTRAINTE_PARADE) {
        check_bonneDetectionContrainte(ctre);
    }

    double transitCalcule = tmpSumCoeff + sumcoeffFixe;

    if (transitCalcule > 0 && !ctre->ctrSup_) {
        ctre->ctrSup_ = true;
        LOG(debug) << "Changement du sens de la contrainte Inf -> Sup (transit = " << transitCalcule << ")";
    } else if (transitCalcule < 0 && ctre->ctrSup_) {
        ctre->ctrSup_ = false;
        LOG(debug) << "Changement du sens de la contrainte Sup -> Inf (transit = " << transitCalcule << ")";
    }

    if (transitDetecte != config::constants::valdef
        && fabs(transitDetecte - transitCalcule) > config::constants::threshold_test) {
        LOG_ALL(error) << err::ioDico().msg("ERRCalcInterne") << " "
                       << err::ioDico().msg("ERRAjoutCtr", ctre->nomInc(), elemAS->nom_);
        if (config::inputConfiguration().writeConstraintsFile()) {
            LOG(debug) << ctre->numInc() << "    Attention (n-k) " << ctre->nomInc() << ": pour le Qdt " << elemAS->num_
                       << "  " << elemAS->nom_ << " Texacte = " << transitDetecte << " Tsensi = " << transitCalcule
                       << " (tmp_sumcoeff = " << tmpSumCoeff << ", sumcoeffFixe = " << sumcoeffFixe << ")";
        }
        return METRIX_PROBLEME;
    }

    return ecrireCoupeTransit(maxTHorsPartieFixe, minTHorsPartieFixe, ctre);
}

/** Ajout d'une variable d'ecart pour un element surveille sur un incident (0 = situ N) */
int Calculer::ajoutVariableEcart(const std::shared_ptr<Contrainte>& ctr)
{
    int indNewVar = pbNombreDeVariables_;
    LOG(debug) << "Ajout de la variable d'ecart " << indNewVar << " pour l'element e surveiller " << ctr->elemAS_->nom_
               << " sur l'incident " << ctr->nomInc();

    pbNombreDeVariables_++;
    typeEtat_.push_back(ECART_T);
    numSupportEtat_.push_back(ctr->elemAS_->num_);
    pbTypeDeBorneDeLaVariable_.push_back(VARIABLE_BORNEE_DES_DEUX_COTES);
    pbPositionDeLaVariable_.push_back(HORS_BASE_SUR_BORNE_SUP);
    pbTypeDeVariable_.push_back(REEL);
    pbXmin_.push_back(0.);
    pbXmax_.push_back(config::constants::max_diff);
    pbX_.push_back(0.);

    // cout de la nouvelle variable
    pbCoutLineaire_.push_back(config::configuration().costEcart());
    pbCoutLineaireSansOffset_.push_back(0.);

    // ne doit jamais arriver
    if ((ctr->elemAS_->ecarts_.find(ctr->numInc())) != ctr->elemAS_->ecarts_.end()) {
        LOG(debug) << "Erreur : variable d'ecart deje creee pour cet incident";
    }

    ctr->elemAS_->ecarts_[ctr->numInc()] = indNewVar;
    return indNewVar;
}

/** Ajout d'une variable pour valoriser la consommation (END) et la production (ENE) perdues dans la poche */
int Calculer::ajoutContrainteValorisationPoche(const std::shared_ptr<Incident>& parade)
{
    int indNewVar = pbNombreDeVariables_;

    LOG(debug) << "Ajout d'une variable  (" << indNewVar << ") pour valoriser END/ENE de la parade " << parade->nom_;

    pbNombreDeVariables_++;
    typeEtat_.push_back(END_ENE);
    numSupportEtat_.push_back(parade->num_);
    pbTypeDeBorneDeLaVariable_.push_back(VARIABLE_BORNEE_DES_DEUX_COTES);
    pbPositionDeLaVariable_.push_back(HORS_BASE_SUR_BORNE_INF);
    pbTypeDeVariable_.push_back(REEL);
    pbXmin_.push_back(0.);
    pbX_.push_back(0.);

    // cout de la nouvelle variable
    pbCoutLineaire_.push_back(1.);
    pbCoutLineaireSansOffset_.push_back(0.);

    // Ecriture de la contrainte
    // valo > somme_groupes_poche(p0 + p[x] - p[x+1])*ENE + somme_consos_poche(conso - p[x])*END - (1-delta)ValoMax
    pbTypeContrainte_.push_back(COUPE_AUTRE);
    pbIndicesDebutDeLigne_.push_back(nbElmdeMatrContraint_);
    pbSens_.push_back('<');
    pbContraintes_.push_back(nullptr);

    LOG(debug) << "Ajout de la contrainte de valorisation de poche perdue pour la parade : " << parade->nom_;

    double valoMax = 0.;
    double secondMembre = 0.;

    int nbTermes = 0;

    const auto& poche = parade->pochePerdue_;
    for (const auto& noeud : poche->noeudsPoche_) {
        for (const auto& grp : noeud->listeGroupes_) {
            if (grp->prod_ >= 0) {
                valoMax += grp->prod_ * config::configuration().costValoEne() * parade->getProb();
                secondMembre -= grp->prod_ * config::configuration().costValoEne() * parade->getProb();
            } else { // prod < 0, groupe en pompage
                valoMax -= grp->prod_ * config::configuration().costValoEne() * parade->getProb();
                secondMembre += grp->prod_ * config::configuration().costValoEne() * parade->getProb();
            }

            if (grp->prodAjust_ != Groupe::NON_HR_AR) {
                pbIndicesColonnes_.push_back(grp->numVarGrp_);
                pbCoefficientsDeLaMatriceDesContraintes_.push_back(config::configuration().costValoEne()
                                                                   * parade->getProb());
                pbIndicesColonnes_.push_back(grp->numVarGrp_ + 1);
                pbCoefficientsDeLaMatriceDesContraintes_.push_back(-config::configuration().costValoEne()
                                                                   * parade->getProb());
                nbTermes += 2;

                valoMax += grp->puisMaxDispo_ * config::configuration().costValoEne() * parade->getProb();
            }
        }

        if (noeud->nbConsos_ > 0) {
            for (int i = 0; i < noeud->nbConsos_; ++i) {
                const auto& conso = noeud->listeConsos_[i];
                if (conso->numVarConso_ != -1) {
                    pbIndicesColonnes_.push_back(conso->numVarConso_);
                    pbCoefficientsDeLaMatriceDesContraintes_.push_back(-config::configuration().costValoEnd()
                                                                       * parade->getProb());
                    nbTermes++;
                }
                valoMax += conso->valeur_ * config::configuration().costValoEnd() * parade->getProb();
                secondMembre -= conso->valeur_ * config::configuration().costValoEnd() * parade->getProb();
            }
        }
    }

    // Variable d'activation de la parade
    pbIndicesColonnes_.push_back(parade->numVarActivation_);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(valoMax);
    nbTermes++;

    // Variable de valorisation
    pbIndicesColonnes_.push_back(indNewVar);
    pbCoefficientsDeLaMatriceDesContraintes_.push_back(-1);
    nbTermes++;

    secondMembre += valoMax;
    pbSecondMembre_.push_back(secondMembre);

    pbXmax_.push_back(std::max(valoMax, 0.)); // securite, ne devrait pas arriver

    nbElmdeMatrContraint_ += nbTermes;
    pbNombreDeTermesDesLignes_.push_back(nbTermes);

    pbNombreDeContraintes_++;

    return indNewVar;
}

int Calculer::fixerProdSansReseau()
{
    // cette fonction se devise en deux parties (l'ordre est important):
    // a- mise a jour des contraintes de bord des groupes
    // b- mise a jour des contraintes, notamment la contrainte de bilan

    // Mise a jour des contraintes de bord des groupes
    // ***********************************************
    // Remarque importante :
    // la valeur des couts change entre iter1 et iter2
    // => la base de iter1 n'est plus realisable mais elle est inversible
    // il est donc toujours interessant de la fournir au probleme de iter2

    LOG_ALL(info) << err::ioDico().msg("INFOCoutsAvecReseau");

    for (const auto& elem : res_.groupes_) {
        const auto& grpe = elem.second;

        if (grpe->etat_ && grpe->prodAjust_ != Groupe::NON_HR_AR) {
            // Phase AR, on remet la puisMin e sa valeur
            grpe->puisMin_ = grpe->puisMinAR_;

            int numVar = grpe->numVarGrp_;

            double deltaProdHR = pbX_[numVar] - pbX_[numVar + 1];

            grpe->prod_ += deltaProdHR;

            pbSecondMembre_[0] -= deltaProdHR;
            if (grpe->noeud_->numCompSynch_ > 0) {
                pbSecondMembre_[grpe->noeud_->numCompSynch_] -= deltaProdHR;
            }

            if (grpe->prodAjust_ == Groupe::OUI_HR_AR || grpe->prodAjust_ == Groupe::OUI_AR) {
                const auto& config = config::configuration();
                pbX_[numVar] = 0.0;
                pbCoutLineaire_[numVar] = (config.computationType()
                                           == config::Configuration::ComputationType::OPF_WITHOUT_REDISPATCH)
                                              ? 0.
                                              : std::max(grpe->coutHausseAR_, config::configuration().noiseCost())
                                                    + config::configuration().redispatchCostOffset();
                pbX_[numVar + 1] = 0.0;
                pbCoutLineaire_[numVar + 1] = (config.computationType()
                                               == config::Configuration::ComputationType::OPF_WITHOUT_REDISPATCH)
                                                  ? 0.
                                                  : std::max(grpe->coutBaisseAR_, config::configuration().noiseCost())
                                                        + config::configuration().redispatchCostOffset();
                pbXmin_[numVar] = 0.0;
                pbXmax_[numVar] = (config.computationType()
                                   == config::Configuration::ComputationType::OPF_WITHOUT_REDISPATCH)
                                      ? 0.
                                      : grpe->puisMax_ - grpe->prod_;
                pbXmin_[numVar + 1] = 0.;

                double value = 0.0;
                if (grpe->prod_ >= grpe->puisMin_
                    && config.computationType() != config::Configuration::ComputationType::OPF_WITHOUT_REDISPATCH) {
                    value = grpe->prod_ - grpe->puisMin_;
                }
                pbXmax_[numVar + 1] = value;
            } else {
                pbCoutLineaire_[numVar] = config::configuration().noiseCost();
                pbCoutLineaire_[numVar + 1] = config::configuration().noiseCost();
                pbXmin_[numVar] = 0.;
                pbXmax_[numVar] = 0.;
                pbXmin_[numVar + 1] = 0.;
                pbXmax_[numVar + 1] = 0.;
            }

            // Remarque traitements effectues pour la resolution du probleme
            pbTypeDeBorneDeLaVariable_[numVar] = VARIABLE_BORNEE_DES_DEUX_COTES;
            pbTypeDeBorneDeLaVariable_[numVar + 1] = VARIABLE_BORNEE_DES_DEUX_COTES;

            if (pbXmax_[numVar] - pbXmin_[numVar] < config::constants::epsilon) {
                pbTypeDeBorneDeLaVariable_[numVar] = VARIABLE_FIXE;
                pbX_[numVar] = pbXmin_[numVar];
            }
            if (pbXmax_[numVar + 1] - pbXmin_[numVar + 1] < config::constants::epsilon) {
                pbTypeDeBorneDeLaVariable_[numVar + 1] = VARIABLE_FIXE;
                pbX_[numVar + 1] = pbXmin_[numVar + 1];
            }
        }
        // mise a jour des couts des groupes pour le redispatching
    }
    return METRIX_PAS_PROBLEME;
}

int Calculer::interdireDefaillance()
{
    LOG_ALL(info) << "Blocage des variables de defaillance";

    int finVarConsos = res_.nbVarGroupes_ + res_.nbVarConsos_;
    for (int varConso = res_.nbVarGroupes_; varConso < finVarConsos; ++varConso) {
        pbTypeDeBorneDeLaVariable_[varConso] = VARIABLE_FIXE;
        pbXmin_[varConso] = pbX_[varConso];
        pbXmax_[varConso] = pbX_[varConso];
        pbCoutLineaire_[varConso] = 0;
    }

    // on supprime la premiere equation de bilan energetique qui n'a plus que des variables fixes
    videMatricesDesContraintes();

    return METRIX_PAS_PROBLEME;
}

bool Calculer::check_calculThetaSurIncident(const std::shared_ptr<Incident>& icdt,
                                            std::vector<double>& injectionSNodales,
                                            std::vector<double>& secondMembreFixe)
{
    // adaptation des injections en fonction de l'incident :
    //  - groupes declenches et compensation
    //  - action curative de TD ou HVDC
    // puis resolution LU => en sortie, on a les theta

    if (icdt) {
        miseAJourSecondMembreSurIncident(icdt, injectionSNodales, secondMembreFixe);

        if (icdt->pochePerdue_) {
            modifJacobienneInc(icdt->pochePerdue_->incidentModifie_, true);
        } else {
            modifJacobienneInc(icdt, true);
        }

        LU_RefactorisationNonSymetrique(jacFactorisee_, &jac_);
    }

    // mise a zero de l injection aux noeuds bilan
    std::map<int, int>::iterator itNb;
    for (itNb = res_.numNoeudBilanParZone_.begin(); itNb != res_.numNoeudBilanParZone_.end(); ++itNb) {
        injectionSNodales[itNb->second] = 0.;
    }

    int codeRet = OUI_LU;

    LU_LuSolv(jacFactorisee_, &injectionSNodales[0], &codeRet, nullptr, 0, 0.0);

    // Retablissement
    if (icdt) {
        if (icdt->pochePerdue_) {
            modifJacobienneInc(icdt->pochePerdue_->incidentModifie_, false);
        } else {
            modifJacobienneInc(icdt, false);
        }
        LU_RefactorisationNonSymetrique(jacFactorisee_, &jac_);
    }

    return codeRet == OUI_LU;
}

void Calculer::compareLoadFlowReport()
{
    LOG(debug) << " on compare les resultats des coeff de report aux LF";
    /////////// pour le calcul des theta en N
    // injections nodales N qui seront utilisees pour la formule coeff de report
    std::vector<double> secondMembre(res_.nbNoeuds_, 0.0);
    // memset(&injectionSNodales[0],0,res_.nbNoeuds_*sizeof(double));

    // partie independante des variables pbX_ : sum(P0)-sum(conso)
    std::vector<double> secMembFixe(res_.nbNoeuds_, 0.0);
    // memset(&secMembFixe[0],0,res_.nbNoeuds_*sizeof(double));

    miseAJourSecondMembre(secondMembre, secMembFixe);

    int codeRet;
    std::vector<double> injectionsNodales = secondMembre;
    LU_LuSolv(jacFactorisee_, &injectionsNodales[0], &codeRet, nullptr, 0, 0.0);
    if (codeRet != 0) {
        LOG(error) << err::ioDico().msg("ERRCalcInterne")
                   << " probleme lors de la factorisation LU pour le calcul du second membre";
        return;
    }
    // on a donc dans injectionsNodales, les theta en N

    /////////// pour le calcul des theta sur defaut
    // injections nodales N qui seront utilisees pour la formule coeff de report
    std::vector<double> injectionsNodalesInc;
    // partie independante des variables pbX_ : sum(P0)-sum(conso)
    std::vector<double> secMembFixeInc;

    for (const auto& icdt : res_.incidentsEtParades_) {
        if (!icdt->validite_) {
            continue;
        }

        if (icdt->parade_ && (icdt->numVarActivation_ == -1 || pbX_[icdt->numVarActivation_] < 0.5)) {
            continue;
        }

        // remplir les injections nodales
        injectionsNodalesInc = secondMembre;
        secMembFixeInc = secMembFixe;
        // on a donc les theta sur incident dans injectionsNodalesInc

        LOG(debug) << " on compare pour " << icdt->nom_;

        for (const auto& elem : res_.quads_) {
            const auto& quad = elem.second;

            if (!quad->connecte()
                || std::find(icdt->listeQuads_.cbegin(), icdt->listeQuads_.cend(), quad) != icdt->listeQuads_.cend()) {
                continue;
            }

            double transitReport = transitSurQuad(quad, icdt, injectionsNodales);
            double transitLF = quad->u2Yij_
                               * (injectionsNodalesInc[quad->norqua_->num_]
                                  - injectionsNodalesInc[quad->nexqua_->num_]);

            if (fabs(transitReport - transitLF) > config::constants::threshold_test) {
                LOG(warning) << " ERREUR " << icdt->nom_ << " quad " << quad->nom_ << "  LF "
                             << c_fmt("%.6f", transitLF) << " report " << c_fmt("%.6f", transitReport);
            }
        }
    }
    jacFactorisee_ = LU_Factorisation(&jac_);
}

bool Calculer::check_bonneDetectionTouteContrainte(const std::shared_ptr<Incident>& icdt,
                                                   std::vector<double>& injectionSNodales,
                                                   std::vector<double>& secondMembreFixe)
{
    bool ok = true;
    miseAJourSecondMembre(injectionSNodales, secondMembreFixe);

    check_calculThetaSurIncident(icdt, injectionSNodales, secondMembreFixe);

    // Check le transit
    std::shared_ptr<Quadripole> quad = nullptr;
    for (const auto& elemAS : res_.elementsASurveiller_) {
        if (elemAS->survMaxInc_ != ElementASurveiller::SURVEILLE) {
            continue; // si ligne a surveiller
        }

        double transit = 0.;

        for (const auto& elem : elemAS->quadsASurv_) {
            quad = elem.first;
            double coeff = elem.second;

            // verification que le quad n'est pas consigne
            if (!quad->connecte()) {
                continue;
            }

            bool quadIncident = false;
            for (int q = 0; q < icdt->nbLignes_; ++q) {
                if (icdt->listeQuads_[q]->num_ == quad->num_) {
                    quadIncident = true;
                    break;
                }
            }
            if (quadIncident) {
                continue;
            }

            transit += coeff * quad->u2Yij_
                       * (injectionSNodales[quad->norqua_->num_] - injectionSNodales[quad->nexqua_->num_]);
        }

        for (const auto& elem : elemAS->hvdcASurv_) {
            const auto& lcc = elem.first;
            double coeff = elem.second;

            if (!lcc->connecte()) {
                continue;
            }

            double puissHVDC = lcc->puiCons_ + pbX_[lcc->numVar_] - pbX_[lcc->numVar_ + 1];

            if (icdt && lcc->mode_ == CURATIF_POSSIBLE && icdt->incidentATraiterEncuratif_
                && !icdt->lccElemCur_.empty()) {
                auto itLcc = icdt->lccElemCur_.find(lcc);

                if (itLcc != icdt->lccElemCur_.end()) {
                    const auto& elemC = itLcc->second;
                    if (elemC->positionVarCurative_ != -1) {
                        puissHVDC += pbX_[elemC->positionVarCurative_] - pbX_[elemC->positionVarCurative_ + 1];
                    }
                }
            }

            transit += coeff * puissHVDC;
        }

        double seuil = elemAS->seuil(icdt, transit);

        if (icdt->paradesActivees_
            && (!config::configuration().useItam() || seuil == config::constants::valdef
                || seuil == -config::constants::valdef)) {
            continue;
        }

        auto it = elemAS->ecarts_.find(icdt->num_);
        double ecart = 0.;
        if (it != elemAS->ecarts_.end()) {
            if (it->second == -1) {
                continue; // c'etait une contrainte ignoree
            }
            ecart = pbX_[it->second];
        } else {
            // etait-ce une contrainte masquee par une contrainte N
            it = elemAS->ecarts_.find(SITU_N);
            if (it != elemAS->ecarts_.end()) {
                ecart = pbX_[it->second];
            }
        }

        if (fabs(transit) - fabs(seuil) - ecart > config::constants::acceptable_diff) {
            ok = false;
            if (quad) {
                LOG(warning) << quad->nom_ << " (" << quad->num_ << ")  ; " << icdt->nom_ << " (" << icdt->num_
                             << ") il y a bien une contrainte: "
                             << "  lf " << transit << "  max " << seuil;
            }
        }
    } // end boucle elements A surveiller

    return ok;
}

bool Calculer::check_bonneDetectionContrainte(const std::shared_ptr<Contrainte>& cont)
{
    // Cette fonction permet de verifier que les flux en N et sur incidents sont bien calculee par la methode
    // de detection de contraintes qui utilise les coefficients de report
    // Elle modifie la jacobienne et le vecteur d'injection pour faire un load flow sur incident
    // Bien sur elle est tres couteuse en temps donc elle n'est e activer qu en debug

    const auto& inc = cont->icdt_;
    if (inc && inc->parade_ && (inc->numVarActivation_ == -1 || pbX_[inc->numVarActivation_] < 0.5)) {
        return true; // on ne peut pas tester un incident non active
    }

    double transitDetecte = cont->transit_;
    if (transitDetecte == config::constants::valdef) {
        return true; // on n'a pas l'information
    }

    std::vector<double> injectionSNodales(res_.nbNoeuds_, 0.);

    // partie independante des variables pbX_ : sum(P0)-sum(conso)
    std::vector<double> secondMembreFixe(res_.nbNoeuds_, 0.);

    miseAJourSecondMembre(injectionSNodales, secondMembreFixe);

    bool ok = true;
    const auto& elemAS = cont->elemAS_;

    double transit = 0.;

    check_calculThetaSurIncident(inc, injectionSNodales, secondMembreFixe);

    // Check le transit
    for (const auto& elem : elemAS->quadsASurv_) {
        const auto& quad = elem.first;
        double coeff = elem.second;

        transit += coeff * quad->u2Yij_
                   * (injectionSNodales[quad->norqua_->num_] - injectionSNodales[quad->nexqua_->num_]);
    }

    for (const auto& elem : elemAS->hvdcASurv_) {
        const auto& lcc = elem.first;
        double coeff = elem.second;

        if (!lcc->connecte()) {
            continue;
        }

        double puissHVDC = lcc->puiCons_ + pbX_[lcc->numVar_] - pbX_[lcc->numVar_ + 1];

        if (inc && lcc->mode_ == CURATIF_POSSIBLE && inc->incidentATraiterEncuratif_ && !inc->lccElemCur_.empty()) {
            auto itLcc = inc->lccElemCur_.find(lcc);

            if (itLcc != inc->lccElemCur_.end()) {
                const auto& elemC = itLcc->second;
                if (elemC->positionVarCurative_ != -1) {
                    puissHVDC += pbX_[elemC->positionVarCurative_] - pbX_[elemC->positionVarCurative_ + 1];
                }
            }
        }

        transit += coeff * puissHVDC;
    }

    if (fabs(transitDetecte - transit) > config::constants::acceptable_diff) {
        ok = false;
        LOG(warning) << "num " << cont->elemAS_->num_ << "  ;" << cont->elemAS_->nom_.c_str() << "  ; "
                     << cont->nomInc().c_str() << " ; num ;" << cont->numInc() << " erreur de transit  "
                     << transitDetecte << "  lf " << transit;
    } else {
        LOG(debug) << " ok " << cont->elemAS_->nom_ << "  transit " << transit << " ; " << cont->nomInc().c_str();
    }

    return ok;
}

void Calculer::printMatriceDesContraintes()
{
    std::stringstream ss;
    auto indiceDebut = pbIndicesColonnes_.begin();
    auto indiceFin = pbIndicesColonnes_.end();

    ss << "Matrice des contraintes" << std::endl;

    std::vector<int> variables;

    ss << "Var;";
    for (int i = 0; i < pbNombreDeVariables_; i++) {
        if (find(indiceDebut, indiceFin, i) == indiceFin) {
            continue; // variable non utilisee dans les contraintes
        }

        variables.push_back(i);
        ss << i << ";";
    }

    indiceDebut = variables.begin();
    indiceFin = variables.end();

    ss << ";B;";

    ss << "Type;";
    for (auto var : variables) {
        switch (typeEtat_[var]) {
            case PROD_H:
                ss << "PROD_H"
                   << ";";
                break;
            case PROD_B:
                ss << "PROD_B"
                   << ";";
                break;
            case CONSO_D:
                ss << "CONSO"
                   << ";";
                break;
            case ECART_T:
                ss << "ECART"
                   << ";";
                break;
            case LIGNE_CC_H:
                ss << "LCC_H"
                   << ";";
                break;
            case LIGNE_CC_B:
                ss << "LCC_B"
                   << ";";
                break;
            case HVDC_CUR_H:
                ss << "LLC_CUR_H"
                   << ";";
                break;
            case HVDC_CUR_B:
                ss << "LCCC_CUR_B"
                   << ";";
                break;
            case DEPH_H:
                ss << "PST_H"
                   << ";";
                break;
            case DEPH_B:
                ss << "PST_B"
                   << ";";
                break;
            case DEPH_CUR_H:
                ss << "PST_CUR_H"
                   << ";";
                break;
            case DEPH_CUR_B:
                ss << "PST_CUR_B"
                   << ";";
                break;
            case GRP_CUR_H:
                ss << "GRP_CUR_H"
                   << ";";
                break;
            case GRP_CUR_B:
                ss << "GRP_CUR_B"
                   << ";";
                break;
            case CONSO_H:
                ss << "EFF_H"
                   << ";";
                break;
            case CONSO_B:
                ss << "EFF_B"
                   << ";";
                break;
            case VAR_ENT:
                ss << "VAR_ENT"
                   << ";";
                break;
            case END_ENE:
                ss << "END_ENE"
                   << ";";
                break;
            default:
                ss << "UNDEFINED"
                   << ";";
        }
    }
    LOG(trace) << ss.str();
    ss.str("");

    ss << "Min;";
    for (auto var : variables) {
        ss << pbXmin_[var] << ";";
    }
    LOG(trace) << ss.str();
    ss.str("");

    ss << "Max;";
    for (auto var : variables) {
        ss << pbXmax_[var] << ";";
    }
    LOG(trace) << ss.str();
    ss.str("");

    for (int i = 0; i < pbNombreDeContraintes_; i++) {
        ss << i << ";";
        int firstCoeff = pbIndicesDebutDeLigne_[i];
        int lastCoeff = firstCoeff + pbNombreDeTermesDesLignes_[i];
        int pos = 0;
        for (int j = firstCoeff; j < lastCoeff; j++) {
            while (pos < pbIndicesColonnes_[j]) {
                if (find(indiceDebut, indiceFin, pos) != indiceFin) {
                    ss << ";";
                }
                pos++;
            }
            ss << pbCoefficientsDeLaMatriceDesContraintes_[j] << ";";
            pos++;
        }
        while (pos < pbNombreDeVariables_) {
            if (find(indiceDebut, indiceFin, pos) != indiceFin) {
                ss << ";";
            }
            pos++;
        }
        ss << pbSens_[i] << ";" << pbSecondMembre_[i];
        LOG(trace) << ss.str();
        ss.str("");
    }
}
