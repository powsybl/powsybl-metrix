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
#include "parametres.h"
#include "pne.h"
#include "prototypes.h"
#include "reseau.h"
#include "status.h"
#include "variante.h"

#include <algorithm>
#include <array>
#include <cmath>
#include <fstream> //attention
#include <iomanip>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>

using cte::c_fmt;


using std::fixed;
using std::ifstream;
using std::ios;
using std::istringstream;
using std::max;
using std::min;
using std::ostringstream;
using std::setprecision;
using std::setw;
using std::string;
using std::vector;

void Contrainte::init(const std::shared_ptr<ElementASurveiller>& qdt,
                      const std::shared_ptr<Incident>& icdt,
                      double T_N,
                      double Tmax,
                      double Tmin,
                      double ecart,
                      TypeDeContrainte type,
                      bool supTmax)
{
    num_ = -1;
    elemAS_ = qdt;
    icdt_ = icdt;
    transit_ = T_N;
    maxT_ = Tmax;
    minT_ = Tmin;
    ecart_ = ecart; // "T-Tmax" si supTmax ou "Tmin-T"
    type_ = type;
    ctrSup_ = supTmax;
    ecrireContrainte_ = true;
    numVarActivation_ = -1;
}


string Contrainte::typeDeContrainteToString() const
{
    string nom = "unknown";
    if (type_ == Contrainte::CONTRAINTE_N) {
        nom = "N";
    } else if (type_ == Contrainte::CONTRAINTE_N_MOINS_K) {
        nom = "N-k";
    } else if (type_ == Contrainte::CONTRAINTE_PARADE) {
        nom = "Parade";
    } else if (type_ == Contrainte::CONTRAINTE_EMUL_AC_N) {
        nom = "EmulAC_N";
    } else if (type_ == Contrainte::CONTRAINTE_EMULATION_AC) {
        nom = "EmulAC_Nk";
    } else if (type_ == Contrainte::CONTRAINTE_ACTIVATION) {
        nom = "Activation";
    }
    return nom;
}


Calculer::Calculer(Reseau& res, MapQuadinVar& variantesOrdonnees) : res_(res), variantesOrdonnees_(variantesOrdonnees)
{
    // Réglage des paramètres en fonction du mode de calcul
    std::stringstream ss;
    ss << "Mode de calcul : ";
    switch (config::configuration().computationType()) {
        case config::Configuration::ComputationType::LOAD_FLOW: ss << "LOAD-FLOW"; break;
        case config::Configuration::ComputationType::OPF_WITHOUT_REDISPATCH:
            ss << "OPF sans redispatching de groupes";
            break;
        case config::Configuration::ComputationType::OPF_WITH_OVERLOAD: ss << "OPF avec overload"; break;
        case config::Configuration::ComputationType::OPF:
            // Rien a faire, valeur par defaut
            ss << "OPF";
            break;
        default: throw ErrorI(err::ioDico().msg("ERRTypeCalculInconnu"));
    }
    LOG_ALL(info) << ss.str();

    jacIndexDebutDesColonnes_.resize(res_.nbNoeuds_, 0);
    jacNbTermesDesColonnes_.resize(res_.nbNoeuds_, 0);
    jacValeurDesTermesDeLaMatrice_.resize(config::constants::nb_max_termes_moyen_constraints * res_.nbNoeuds_, 0.0);
    jacIndicesDeLigne_.resize(config::constants::nb_max_termes_moyen_constraints * res_.nbNoeuds_, 0);

    typeEtat_.resize(res_.nbVarGroupes_ + res_.nbVarConsos_ + res_.nbVarTd_ + res_.nbVarCc_, NONE);
    numSupportEtat_.resize(res_.nbVarGroupes_ + res_.nbVarConsos_ + res_.nbVarTd_ + res_.nbVarCc_, 0);

    int i1 = -1;
    int numVar;
    for (const auto& elem : res_.groupes_) {
        const auto& grp = elem.second;

        if (grp->prodAjust_ == Groupe::NON_HR_AR) {
            continue;
        }

        i1++;
        grp->numVarGrp_ = 2 * i1;
        numVar = grp->numVarGrp_;
        typeEtat_[numVar] = PROD_H;
        typeEtat_[numVar + 1] = PROD_B;
        numSupportEtat_[numVar] = grp->numNoeud_;
        numSupportEtat_[numVar + 1] = grp->numNoeud_;
    }

    for (const auto& elem : res_.consos_) {
        const auto& conso = elem.second;

        if (conso->numVarConso_ >= 0) {
            typeEtat_[conso->numVarConso_] = CONSO_D;
            numSupportEtat_[conso->numVarConso_] = conso->noeud_->num_;
        }
    }

    for (const auto& elem : res_.TransfoDephaseurs_) {
        const auto& td = elem.second;
        numVar = res_.nbVarGroupes_ + res_.nbVarConsos_ + 2 * td->num_;
        td->numVar_ = numVar;

        typeEtat_[numVar] = DEPH_H;
        typeEtat_[numVar + 1] = DEPH_B;

        numSupportEtat_[numVar] = td->num_;
        numSupportEtat_[numVar + 1] = td->num_;
    }

    for (const auto& elem : res_.LigneCCs_) {
        const auto& lcc = elem.second;
        numVar = res_.nbVarGroupes_ + res_.nbVarConsos_ + res_.nbVarTd_ + 2 * lcc->num_;
        lcc->numVar_ = numVar;

        typeEtat_[numVar] = LIGNE_CC_H;
        typeEtat_[numVar + 1] = LIGNE_CC_B;

        numSupportEtat_[numVar] = lcc->num_;
        numSupportEtat_[numVar + 1] = lcc->num_;
    }

    icdtQdt_.resize(max(static_cast<int>(config::constants::factor_max_size_dodu) * res_.nbQuads_,
                        static_cast<int>(config::constants::nb_active_constraints)));
    nbCtr_ = 0;
}

int Calculer::resolutionProbleme()
{
    // 1- resolution du probleme de base.
    // 2- resolution des differentes variantes
    // 2-a modification du reseau pour la prise en compte des differentes variantes:
    // 		- modification production, conso, hvdc etc à chaque variante
    //		- modification de la topologie (indispo lignes) à chaque ensemble de variantes de même clé
    // 2-b remise du reseau a son etat initial:
    // 		- remise à l'état initial de la production, conso, hvdc etc pour chaque variante
    //		- remise à l'état initial de la topologie (indispo lignes) à chaque ensemble de variantes de même clé

    int status = METRIX_PAS_PROBLEME;
    unsigned int nbCalculs = 0;
    bool relance = false;
    tauxPertes_ = res_.coeff_pertes_;

    // MODELISATION DODU
    status = initJacobienne();
    if (status != METRIX_PAS_PROBLEME) {
        LOG_ALL(error) << "probleme dans initJacobienne";
        return METRIX_PROBLEME;
    }
    status = construireJacobienne();
    if (status != METRIX_PAS_PROBLEME) {
        LOG_ALL(error) << "probleme dans construireJacobienne";
        return METRIX_PROBLEME;
    }

    // Factoriser la jacobienne c'est la meme pour tout le probleme
    jacFactorisee_ = LU_Factorisation(&jac_);
    if (jac_.ProblemeDeFactorisation != NON_LU) {
        LOG_ALL(error) << "probleme lors de la factorisation: LU factorization code = " << jac_.ProblemeDeFactorisation;
        return METRIX_PROBLEME;
    }

    // Initialisation
    // Allocation et initialisation des tables
    status = allocationProblemeDodu();
    if (status != METRIX_PAS_PROBLEME) {
        LOG_ALL(error) << "Probleme lors de l'allocation des contraintes ou des variables";
        return METRIX_PROBLEME;
    }

    // 2-parcourir toutes les variantes
    //--------------------------------
    auto mapVarEnd = variantesOrdonnees_.end();
    for (auto mapVar = variantesOrdonnees_.begin(); mapVar != mapVarEnd; ++mapVar) {
        const Quadripole::SetQuadripoleSortedByName& quads = mapVar->first;

        LOG_RES() << "\n********************************************";
        LOG_ALL(info) << err::ioDico().msg("INFONouveauPaquetVariantes", c_fmt("%d", mapVar->second.size()));
        LOG_RES() << "********************************************";

        if (!mapVar->first.empty()) {
            // mise a jour de la topologie du reseau pour prendre en compte les modifications quadin
            status = res_.modifReseauTopo(quads);

            if (status != METRIX_PAS_PROBLEME) {
                LOG_ALL(warning) << "probleme lors de la modification du reseau pour:";
                afficherVariantesCle(variantesOrdonnees_, quads);
            }

            // Modification de la jacobienne pour prendre en compte les modifications topologiques quadin
            status = modifJacobienneVar(quads, true);
            if (status != METRIX_PAS_PROBLEME) {
                LOG_ALL(warning) << "probleme lors de la modification de la jacobienne pour:";
                afficherVariantesCle(variantesOrdonnees_, quads);
            }

            // Refactoriser la jacobienne
            LU_RefactorisationSymetrique(jacFactorisee_, &jac_);
            // jacFactorisee_ = LU_Factorisation(&jac_);
            if (jac_.ProblemeDeFactorisation != NON_LU) {
                LOG_ALL(error) << "probleme lors de la refactorisation symétrique: code = "
                               << jac_.ProblemeDeFactorisation;
                // ptet liberer la memoire et refactoriser from scratch...
                return METRIX_PROBLEME;
            }

            // Suppression des autres jacobiennes (pour les incidents rompant la connexité)
            for (const auto& pair : jacIncidentsModifies_) {
                LU_LibererMemoireLU(pair.second);
            }
            jacIncidentsModifies_.clear();
        }

        // LODFs (line outage distribution factors) and PTDFs (power transfer distrbution factors) assessment and print
        //*******************************************************
        // LODFs assessment and print:
        status = calculReportInfluencement();

        // PTDFs assessment and print:
        string PTDFfileName = "PTDF_matrix.csv";
        if (config::inputConfiguration().writePTDFfile()) {
            assessAndPrintPTDF(PTDFfileName);
        }

        auto varEnd = mapVar->second.end();
        for (auto var = mapVar->second.begin(); var != varEnd; ++var) {
            varianteCourante_ = (*var);

            LOG_RES() << "\n---------------------";
            LOG_RES() << "---------------------";
            LOG_ALL(info) << err::ioDico().msg("INFOVariante", c_fmt("%d", varianteCourante_->num_));
            LOG_RES() << "---------------------";
            LOG_RES() << "---------------------\n";

            // Update of the remaining elements of the network for each variant
            status = res_.modifReseau(varianteCourante_);

            if (status != METRIX_PAS_PROBLEME) {
                metrix2Assess(varianteCourante_, vector<double>(), METRIX_VARIANTE_IGNOREE);

                // We cancel the application of the variant
                if (res_.resetReseau(varianteCourante_, false) != METRIX_PAS_PROBLEME) {
                    return METRIX_PROBLEME;
                }
                continue;
            }

            nbCalculs = 0;
            tauxPertes_ = res_.coeff_pertes_;
            do {
                status = resolutionUnProblemeDodu(varianteCourante_);

                // Free the problem at the end of the resolution
                solver_.free();

                // No solution variant (but not due to internal problem)
                if (status == METRIX_PAS_SOLUTION || status == METRIX_NB_MAX_CONT_ATTEINT
                    || status == METRIX_NB_MICROIT) {
                    printFctObj(true);
                    metrix2Assess(varianteCourante_, vector<double>(), status);
                    break;
                }

                if (status == METRIX_PROBLEME) {
                    LOG_ALL(error) << "probleme dans resolutionUnProblemeDodu";
                    LU_LibererMemoireLU(jacFactorisee_);
                    return METRIX_PROBLEME;
                }

                relance = false;
                if (nbCalculs < config::configuration().maxRelancePertes() && status == METRIX_PAS_PROBLEME) {
                    double pertesEstimees = consoTotale_ / (100 + tauxPertes_) * tauxPertes_;
                    LOG_ALL(info) << err::ioDico().msg("INFOVolumePertes",
                                                       c_fmt("%.0f", pertesTotales_),
                                                       c_fmt("%.0f", pertesEstimees),
                                                       c_fmt("%.0f", pertesTotales_ - pertesEstimees));

                    if (fabs(round(pertesTotales_ - pertesEstimees, 1.))
                        > config::configuration().thresholdRelancePertes()) {
                        relance = true;
                        nbCalculs++;
                        auto txPertesCalcule = static_cast<float>(round(pertesTotales_ / consoTotale_ * 100, 100.));
                        res_.modifTauxDePertes(tauxPertes_, txPertesCalcule);
                        tauxPertes_ = txPertesCalcule;
                        LOG_ALL(info) << err::ioDico().msg("INFODeltaPertes",
                                                           c_fmt("%1.2f", txPertesCalcule),
                                                           c_fmt("%d", nbCalculs),
                                                           c_fmt("%d", config::configuration().maxRelancePertes()));
                        res_.modifBilans(varianteCourante_);
                        if (res_.resetReseau(nullptr, false) == METRIX_PROBLEME) {
                            LOG_ALL(error) << "probleme dans resetReseau";
                            return METRIX_PROBLEME;
                        }
                    }
                }
            } while (relance);

            // remise du reseau dans son etat initial (toute modification de type production ou conso mais pas les
            // lignes indisponibles),
            if (res_.resetReseau(varianteCourante_, (nbCalculs > 0)) != METRIX_PAS_PROBLEME) {
                LOG_ALL(error) << "probleme lors de la modification de reseau par la variante numero : "
                               << varianteCourante_->num_;
                LU_LibererMemoireLU(jacFactorisee_);
                return METRIX_PROBLEME;
            }
        } // fin variantes de même clé

        // remise des lignes indisponibles du reseau dans son etat initial pour l'ensemble des variantes de même clé
        // IndispoLignes
        if (!quads.empty()) {
            if (res_.resetReseauTopo(quads) != METRIX_PAS_PROBLEME) {
                LOG_ALL(error) << "probleme lors de la modification des indisponibilités de lignes du reseau pour: ";
                afficherVariantesCle(variantesOrdonnees_, quads);
                return METRIX_PROBLEME;
            }

            // 2-f On désapplique les modifications de la jacobienne pour l'ensemble des variantes de même clé
            // IndispoLignes
            if (modifJacobienneVar(quads, false) != METRIX_PAS_PROBLEME) { // false pour desappliquer la variante
                LOG_ALL(error) << "probleme lors de la remise en forme de la jacobienne pour:";
                afficherVariantesCle(variantesOrdonnees_, quads);
            }

            // Refactoriser la jacobienne
            LU_RefactorisationSymetrique(jacFactorisee_, &jac_);
            if (jac_.ProblemeDeFactorisation != NON_LU) {
                LOG_ALL(error) << "probleme lors de la refactorisation symétrique après traitements variantes: code  = "
                               << jac_.ProblemeDeFactorisation;
                // ptet liberer la memoire et refactoriser from scratch...
                return METRIX_PROBLEME;
            }
        }
    } // fin des variantes de l'ensemble de la map

    LU_LibererMemoireLU(jacFactorisee_);

    return METRIX_PAS_PROBLEME;
}


int Calculer::PneSolveur(TypeDeSolveur typeSolveur, const std::shared_ptr<Variante>& varianteCourante)
{
    if (config::inputConfiguration().printConstraintsMatrix()) {
        printMatriceDesContraintes();
    }

    // Solveur I : utilisation de PNE_SOLVEUR
    //--------------------------------------
    if (typeSolveur == UTILISATION_PNE_SOLVEUR) {
        LOG_ALL(info) << err::ioDico().msg("INFOAppelSolvLineairePNE");

        pbCoutsMarginauxDesContraintes_.resize(pbNombreDeContraintes_);

        // Options du solveur
        //------------------
        pbPNE_.AffichageDesTraces = TRACES_PNE;
        pbPNE_.SortirLesDonneesDuProbleme = config::inputConfiguration().exportMPSFile() ? OUI_PNE
                                                                                         : NON_PNE; // fichier mps
        pbPNE_.FaireDuPresolve
            = PNE_PRESOLVE; /* si NON : attention a ne pas mettre de colonne vide ou avec que des 0.*/
        pbPNE_.TempsDExecutionMaximum = config::configuration().timeMaxPne(); // (0 illimité, sinon en secondes)
        pbPNE_.NombreMaxDeSolutionsEntieres = -1;
        pbPNE_.ToleranceDOptimalite = 1.e-4; // En %

        // valeur des variables du problemes a resoudre
        //--------------------------------------------
        pbPNE_.NombreDeVariables = pbNombreDeVariables_;
        pbPNE_.TypeDeVariable = &pbTypeDeVariable_[0];
        pbPNE_.TypeDeBorneDeLaVariable = &pbTypeDeBorneDeLaVariable_[0];
        pbPNE_.X = &pbX_[0];
        pbPNE_.Xmax = &pbXmax_[0];
        pbPNE_.Xmin = &pbXmin_[0];
        pbPNE_.CoutLineaire = &pbCoutLineaire_[0];
        pbPNE_.NombreDeContraintes = pbNombreDeContraintes_;
        pbPNE_.SecondMembre = &pbSecondMembre_[0];
        pbPNE_.Sens = &pbSens_[0];
        pbPNE_.IndicesDebutDeLigne = &pbIndicesDebutDeLigne_[0];
        pbPNE_.NombreDeTermesDesLignes = &pbNombreDeTermesDesLignes_[0];
        pbPNE_.CoefficientsDeLaMatriceDesContraintes = &pbCoefficientsDeLaMatriceDesContraintes_[0];
        pbPNE_.IndicesColonnes = &pbIndicesColonnes_[0];
        pbPNE_.VariablesDualesDesContraintes = &pbCoutsMarginauxDesContraintes_[0];
        pbPNE_.AlgorithmeDeResolution = SIMPLEXE;

        // Resolution du probleme
        //----------------------
        solver_.solve(&pbPNE_);
        pbExistenceDUneSolution_ = pbPNE_.ExistenceDUneSolution;

        if (pbExistenceDUneSolution_ == SOLUTION_OPTIMALE_TROUVEE
            || pbExistenceDUneSolution_ == SOLUTION_OPTIMALE_TROUVEE_MAIS_QUELQUES_CONTRAINTES_SONT_VIOLEES
            || pbExistenceDUneSolution_ == ARRET_PAR_LIMITE_DE_TEMPS_AVEC_SOLUTION_ADMISSIBLE_DISPONIBLE) {
            LOG_ALL(info) << err::ioDico().msg(
                "INFOSolOptTrouve", c_fmt("%d", varianteCourante->num_), c_fmt("%d", pbExistenceDUneSolution_));
        } else if (pbExistenceDUneSolution_ == PROBLEME_INFAISABLE
                   || pbExistenceDUneSolution_ == PAS_DE_SOLUTION_TROUVEE) {
            LOG_ALL(info) << err::ioDico().msg("INFOPasDeSol", c_fmt("%d", varianteCourante->num_));
            return METRIX_PAS_SOLUTION;
        } else {
            return METRIX_PROBLEME;
        }

        // affichage de la fonction cout
        printFctObj(false);

        // Solveur II : utilisation du SIMPLEXE;
        //------------------------------------
    } else if (typeSolveur == UTILISATION_SIMPLEXE) {
        LOG_ALL(info) << err::ioDico().msg("INFOAppelSolvLineaireSPX");

        for (int i = 0; i < pbNombreDeVariables_; ++i) {
            if (pbXmin_[i] > pbXmax_[i] + config::constants::acceptable_diff) {
                LOG(debug) << i << " probleme Xmin (" << pbXmin_[i] << ") > xmax (" << pbXmax_[i] << ")";
            }
        }

        pbCoutsMarginauxDesContraintes_.resize(pbNombreDeContraintes_);
        pbComplementDeLaBase_.resize(pbNombreDeContraintes_);
        pbCoutsReduits_.resize(pbNombreDeVariables_);

        // definition des paramaitres de pb qui ne changent pas en fonction de la variante et de numMicroIteration_

        pb_.TypeDePricing = PRICING_STEEPEST_EDGE;  /*PRICING_DANTZIG()*/
        pb_.FaireDuScaling = OUI_SPX;               // Vaut OUI_SPX ou NON_SPX
        pb_.StrategieAntiDegenerescence = AGRESSIF; // Vaut AGRESSIF ou PEU_AGRESSIF
        pb_.NombreMaxDIterations = -1;              // si i < 0 , alors le simplexe prendre sa valeur par defaut
        pb_.DureeMaxDuCalcul = -1;                  // si i < 0 , alors le simplexe prendre sa valeur par defaut
        pb_.CoutLineaire = &pbCoutLineaire_[0];
        pb_.X = &pbX_[0];
        pb_.Xmin = &pbXmin_[0];
        pb_.Xmax = &pbXmax_[0];
        pb_.NombreDeVariables = pbNombreDeVariables_;
        pb_.TypeDeVariable = &pbTypeDeBorneDeLaVariable_[0]; //&pbTypeDeVariable_[0];
        pb_.NombreDeContraintes = pbNombreDeContraintes_;
        pb_.IndicesDebutDeLigne = &pbIndicesDebutDeLigne_[0];
        pb_.NombreDeTermesDesLignes = &pbNombreDeTermesDesLignes_[0];
        pb_.IndicesColonnes = &pbIndicesColonnes_[0];
        pb_.CoefficientsDeLaMatriceDesContraintes = &pbCoefficientsDeLaMatriceDesContraintes_[0];
        pb_.Sens = &pbSens_[0];
        pb_.SecondMembre = &pbSecondMembre_[0];
        pb_.ChoixDeLAlgorithme = SPX_DUAL;

        pb_.LibererMemoireALaFin = OUI_SPX /*OUI_SPX*/; // attention ca libere problemeSpx
        pb_.CoutsMarginauxDesContraintes
            = &pbCoutsMarginauxDesContraintes_[0]; // size = nb_contraintes + nb_coupes  ettention
        pb_.CoutsReduits = &pbCoutsReduits_[0];    // size = nb_variables
        pb_.AffichageDesTraces = TRACES_PNE /*NON_SPX*/;
        pb_.CoutMax = -1;
        pb_.UtiliserCoutMax = NON_SPX;
        // resoudre le probleme et definition des parametres de pb qui changent en fonction de variante et de
        // numMicroIteration_


        pb_.BCoupes = nullptr;                          /*&pbBCoupes_[0];*/
        pb_.PositionDeLaVariableDEcartCoupes = nullptr; /*&pbPositionDeLaVariableDEcartCoupes_[0];*/
        pb_.MdebCoupes = nullptr;                       /*&pbMdebCoupes_[0];*/
        pb_.NbTermCoupes = nullptr;                     /*&pbNbTermCoupes_[0];*/
        pb_.NuvarCoupes = nullptr;                      /*&pbNuvarCoupes_[0];*/
        pb_.ACoupes = nullptr;                          /*&pbACoupes_[0];*/

        pb_.Contexte = SIMPLEXE_SEUL;
        pb_.BaseDeDepartFournie = NON_SPX;                            // OUI_SPX ;//(pas de demarrage a chaud!!)
        pb_.NombreDeContraintesCoupes = pbNombreDeContraintesCoupes_; // nombre de coupes
        pb_.PositionDeLaVariable = &pbPositionDeLaVariable_[0];       // size = nb_variables (demarage a chaud!!)
        pb_.NbVarDeBaseComplementaires = pbNbVarDeBaseComplementaires_;
        pb_.ComplementDeLaBase = &pbComplementDeLaBase_[0]; // size = nb_contraintes (demarage a chaud!!)

        // Création du fichier MPS
        if (config::inputConfiguration().exportMPSFile()) {
            SPX_EcrireProblemeAuFormatMPS(pb_);
        }

        solver_.solve(&pb_);
        pbNbVarDeBaseComplementaires_ = pb_.NbVarDeBaseComplementaires;
        pbExistenceDUneSolution_ = pb_.ExistenceDUneSolution;

        if (pbExistenceDUneSolution_ == OUI_SPX) {
            LOG_ALL(info) << err::ioDico().msg(
                "INFOSolOptTrouve", c_fmt("%d", varianteCourante->num_), c_fmt("%d", pbExistenceDUneSolution_));
        } else {
            LOG_ALL(info) << err::ioDico().msg("INFOPasDeSol", c_fmt("%d", varianteCourante->num_));
            return METRIX_PAS_SOLUTION;
        }

        // Traces
        printFctObj(false);

    } else {
        LOG_ALL(error) << "Unknown solver type";
        return METRIX_PROBLEME;
    }
    return METRIX_PAS_PROBLEME;
}

int Calculer::resolutionUnProblemeDodu(const std::shared_ptr<Variante>& varianteCourante)
{
    // Ce programme :
    // 1. Allocation et initialisation des tables
    // 2. calcul des coeffs de reports, pour la variante i
    // 3. calcul sans reseau et fournir les Pobj au groupes, pour la variante i
    // 4. calcul avec reseau, pour la variante i
    // 4a.Resolution du probleme avec Simplex ou Branche-And-Bound.
    // 4b.Detection des contraintes.
    // 4c.Ecriture des contraintes.

    // initialisation/ reset d'une variante a l autre
    // la totalite du second monbre sum(P0)-sum(conso)+sum(Ph-Pb)+Delestage
    static vector<double> secondMembre(res_.nbNoeuds_);

    // partie independante des variables pbX_ : sum(P0)-sum(conso)
    static vector<double> secondMembreFixe(res_.nbNoeuds_);

    bool existe_contrainte_active = true;

    numMicroIteration_ = 0;
    nbElmdeMatrContraint_ = 0;
    pbNbVarDeBaseComplementaires_ = 0;
    pbNombreDeContraintes_ = 0;

    // allocation et initialisation des tables
    int status = METRIX_PAS_PROBLEME;
    status = allocationProblemeDodu();
    if (status != METRIX_PAS_PROBLEME) {
        LOG_ALL(error) << "Probleme lors de l'allocation des contraintes ou des variables";
        return METRIX_PROBLEME;
    }

    // reinitialisation de la production avant l'empilement economique
    //****************************************************************
    for (auto grpIt = res_.groupes_.cbegin(); grpIt != res_.groupes_.end(); ++grpIt) {
        const auto& grp = grpIt->second;
        if (grp->prodAjust_ == Groupe::OUI_HR_AR || grp->prodAjust_ == Groupe::OUI_HR) {
            grp->prod_ = grp->prodPobj_;
        }
    }

    // ecriture des contraintes
    //************************
    status = ecrireContraintesDodu();

    if (status != METRIX_PAS_PROBLEME) {
        LOG_ALL(error) << "Probleme lors de l'ecriture des contraintes";
        return METRIX_PROBLEME;
    }


    // Phase Hors reseau
    //*********************
    typeSolveur_ = UTILISATION_SIMPLEXE;
    status = empilementEconomiqueDesGroupes(varianteCourante);

    if (status == METRIX_PAS_SOLUTION) {
        return status;
    }

    if (status == METRIX_PROBLEME) {
        LOG_ALL(error) << err::ioDico().msg("ERRCalcInterne") << "Probleme lors de l'appel a Pne_solveur";
        return status;
    }

    if (config::inputConfiguration().compareLoadFlowReport()) {
        compareLoadFlowReport();
    }
    // sauvegarde de donnees HR
    for (unsigned int i = 0; i < pbX_.size(); ++i) {
        pbXhR_[i] = pbX_[i];
    }

    // initialisation de la prod AR si simulation du MA ou si pas de redispatching
    fixerProdSansReseau();

    if (config::configuration().adequacyCostOffset() != config::configuration().redispatchCostOffset()) {
        ajoutRedispatchCostOffsetConsos();
    }

    if (config::configuration().computationType() == config::Configuration::ComputationType::OPF_WITHOUT_REDISPATCH) {
        interdireDefaillance();
    } else if (config::configuration().computationType() == config::Configuration::ComputationType::OPF_WITH_OVERLOAD) {
        // Do not add coupling constraints
    } else {
        // ajout des contraintes de couplage de variables
        status += ajouterContraintesCouplagesGroupes();
        status += ajouterContraintesCouplagesConsos();
    }

    if (status != METRIX_PAS_PROBLEME) {
        LOG_ALL(error) << "Probleme lors de l'ecriture des contraintes de la phase avec reseau";
        return METRIX_PAS_SOLUTION;
    }

    if (res_.nbCCEmulAC_ > 0) {
        // Ajout des contraintes de blocage en N
        for (int i = 0; i < res_.nbCCEmulAC_; ++i) {
            const auto& td = res_.TDFictifs_[i];
            int numVarInt = ajouterVariableEntiere(td->num_, config::constants::cost_whole_variable * i);
            td->numVarEntiere_ = numVarInt;
            ajouterContrainteDeltaConsVarEntiere(td);
            pbX_[numVarInt] = 0;
            pbTypeDeBorneDeLaVariable_[numVarInt] = VARIABLE_FIXE;
        }

        // Obligatoire sinon pb si pas de contraintes détectées à l'étape suivantes
        pbCoutsMarginauxDesContraintes_.resize(pbNombreDeContraintes_);
        pbComplementDeLaBase_.resize(pbNombreDeContraintes_);
        pbCoutsReduits_.resize(pbNombreDeVariables_);
    }

    // phase Avec reseau
    //******************
    int nbmaxReseau = res_.nbQuadSurvNk_ * res_.nbIncidents_;
    int nbMaxContraintesAjoutees = max(static_cast<int>(config::constants::nb_max_constraints), nbmaxReseau);

    int nbNewContreParVariante = 0;
    unsigned int compteur = 0;
    while (existe_contrainte_active) {
        // I-tests d arret
        //***************

        if (nbMaxContraintesAjoutees <= nbNewContreParVariante) {
            LOG_ALL(error) << "trop de contraintes : " << nbNewContreParVariante
                           << "(max : " << nbMaxContraintesAjoutees << ")";
            return METRIX_NB_MAX_CONT_ATTEINT;
        }
        compteur++;
        if (compteur >= config::configuration().nbMaxNumberMicroIterations()) {
            LOG_ALL(error) << "nombre max d iterations est atteint : "
                           << config::configuration().nbMaxNumberMicroIterations();
            return METRIX_NB_MICROIT;
        }

        // II- Pne_solveur
        //***************

        numMicroIteration_++;
        LOG_ALL(info) << err::ioDico().msg("INFOMicroIt",
                                           c_fmt("%d", numMicroIteration_),
                                           c_fmt("%d", /*nbNewContreParVariante*/ pbNombreDeContraintes_));
        LOG_RES() << "-------------------- : \n";

        // Appel du solveur
        if (numMicroIteration_ != 1 && pbNombreDeContraintes_ > 0) {
            status = PneSolveur(typeSolveur_, varianteCourante);
        }

        if (status != METRIX_PAS_PROBLEME) {
            if (status != METRIX_PAS_SOLUTION) {
                LOG_ALL(error) << err::ioDico().msg("ERRCalcInterne") << "Probleme lors de l'appel a Pne_solveur";
            }
            return status;
        }

        // III- mise a jour du second membre
        //*********************************

        status = miseAJourSecondMembre(secondMembre, secondMembreFixe);

        if (status != METRIX_PAS_PROBLEME) {
            LOG_ALL(error) << "Probleme lors de la  mise a jour du Second Membre";
            return status;
        }

        // IV- resolution
        //**************
        int codeRet;

        LU_LuSolv(jacFactorisee_, &secondMembre[0], &codeRet, nullptr, 0, 0.0);
        if (codeRet != 0) {
            LOG_ALL(error) << err::ioDico().msg("ERRCalcInterne")
                           << " probleme lors de la factorisation LU pour le calcul du second membre";
            return METRIX_PROBLEME;
        }

        existe_contrainte_active = false;

        // V-detection de contraintes
        //**************************
        status = detectionContraintes(secondMembre, existe_contrainte_active);
        if (status != METRIX_PAS_PROBLEME) {
            LOG_ALL(error) << "probleme lors de la detection des contraintes";
            return METRIX_PROBLEME;
        }

        LOG_ALL(info) << "Nb termes dans la matrice : " << nbElmdeMatrContraint_
                      << " (taille de la matrice : " << pbCoefficientsDeLaMatriceDesContraintes_.size() << ")";

        if (config::configuration().computationType() == config::Configuration::ComputationType::LOAD_FLOW) {
            // On arrete ici les Load-Flow
            status = METRIX_PAS_PROBLEME;
            break;
        }

        // VII- ajouter les contraintes actives
        //***********************************
        status = ajoutContraintes(existe_contrainte_active, nbNewContreParVariante, secondMembreFixe, secondMembre);
        if (status != METRIX_PAS_PROBLEME) {
            LOG_ALL(error) << "probleme lors de l'ajout des contraintes";
            return status;
        }

        if (config::inputConfiguration().compareLoadFlowReport()) {
            compareLoadFlowReport();
        }
        if (config::inputConfiguration().checkConstraintLevel()
            == config::InputConfiguration::CheckConstraintLevel::EVERY_INCIDENT) {
            if (!existe_contrainte_active) {
                static vector<double> injectionSNodales(res_.nbNoeuds_, 0.);

                // partie independante des variables pbX_ : sum(P0)-sum(conso)
                static vector<double> secMembFixe(res_.nbNoeuds_, 0.);

                for (auto icdtIt = res_.incidentsEtParades_.cbegin(); icdtIt != res_.incidentsEtParades_.end();
                     ++icdtIt) {
                    const auto& icdt = *icdtIt;
                    if (!icdt->validite_) {
                        continue;
                    }

                    // on saute les parades non activées
                    if (icdt->parade_ && (icdt->numVarActivation_ == -1 || pbX_[icdt->numVarActivation_] < 0.5)) {
                        continue;
                    }

                    LOG(debug) << "constraints: Checking incident " << icdt->nom_;
                    check_bonneDetectionTouteContrainte(icdt, injectionSNodales, secMembFixe);
                }
            }
            jacFactorisee_ = LU_Factorisation(&jac_);
        }
    }

    printFctObj(true);
    calculerFluxNk(secondMembre);
    metrix2Assess(varianteCourante, secondMembre, status);

    return METRIX_PAS_PROBLEME;
}

int Calculer::calculReportInfluencement()
{
    // Assessment of the LODF and distribution factors

    // First remark : it is important to assess the factor in the following order :
    // distribution factors.
    // LODF (as they don't depend on the generation units and loads but they depend on topology).

    // Second remark: this algorithm has to be updated when the type of variants evolves.

    int status = METRIX_PAS_PROBLEME;

    bool modifInfluencement = res_.defautsGroupesPresents_;
    bool calculeReport = res_.calculerCoeffReport_;

    /*	 Assessment of the distribution factors and LODF"
         WARNING : we need to reassess the LODF of the variant modifies the topology
    */
    // The LODF of the HVDC on the quadripole of the N state network depend on topology
    // They have to be computed before the LODF for a given contingency as the use the HVDC LODF
    if (calculeReport) {
        time_t start, end;
        time(&start);

        calculReportLccs();

        for (const auto& icdt : res_.incidentsEtParades_) {
            if (!icdt->validite_) {
                continue;
            }

            if (icdt->pochePerdue_ && icdt->pochePerdue_->pocheAvecConsoProd_) {
                continue;
            }

            resetCoefs(icdt);

            if (modifInfluencement) {
                status = calculCoefsInfluencement(icdt);
                if (status != METRIX_PAS_PROBLEME) {
                    LOG_ALL(error) << "probleme dans lors du calcul des coefs d'influencement";
                    return METRIX_PROBLEME;
                }
            }

            status = calculCoefsReport(icdt);

            if (status != METRIX_PAS_PROBLEME) {
                LOG_ALL(error) << "probleme dans lors du calcul des coefs de report";
                return METRIX_PROBLEME;
            }
        }
        time(&end);
        if (config::inputConfiguration().writeLODFfile()) {
            LOG_ALL(info) << "Temps de calcul des coefficients de report : " << c_fmt("%.1f s.", difftime(end, start));
        }
        resetCoeffQuadsN();
        calculReportGroupesEtConsos();
        calculCoeffReportTD();
        res_.calculerCoeffReport_ = false;

        string LODFfileName = "LODF_matrix.csv";
        printLODF(LODFfileName, config::inputConfiguration().writeLODFfile());
    }
    
    return METRIX_PAS_PROBLEME;
}

void Calculer::printFctObj(bool silent)
{
    // affichage pour la solution courante (pbX_) de la fonction cout, volume de redispatching, delestage ...
    // En cout
    fonction_objectif_D_ = 0.;
    fonction_objectif_G_ = 0.;
    fonction_objectif_C_ = 0.;
    fonction_objectif_E_ = 0.;
    fonction_objectif_D_cur_ = 0.;
    fonction_objectif_G_cur_ = 0.;
    double fonction_objectif_V = 0.; // Valorisation des poches perdues par les parades

    fonction_objectif_G_cur_sans_offset_ = 0.;
    fonction_objectif_D_cur_sans_offset_ = 0.;

    // En volume MW
    double sumProdApresEmpilement = 0.0;
    double sumDelestage = 0.0;

    for (const auto& elem : res_.groupes_) {
        const auto& grp = elem.second;

        if (grp->etat_) {
            sumProdApresEmpilement += grp->prod_;
            if (grp->prodAjust_ == Groupe::OUI_HR || grp->prodAjust_ == Groupe::OUI_HR_AR
                || grp->prodAjust_ == Groupe::OUI_AR) {
                sumProdApresEmpilement += pbX_[grp->numVarGrp_] - pbX_[grp->numVarGrp_ + 1];
            }
        }
    }

    for (int i = 0; i < pbNombreDeVariables_; ++i) {
        if (pbTypeDeBorneDeLaVariable_[i] == VARIABLE_FIXE) {
            continue;
        }

        if (i < res_.nbVarGroupes_) {
            fonction_objectif_G_ += pbCoutLineaire_[i] * pbX_[i];
        } else if (i < res_.nbVarGroupes_ + res_.nbVarConsos_) {
            fonction_objectif_D_ += pbCoutLineaire_[i] * pbX_[i];
            sumDelestage += pbX_[i];
        } else {
            if (typeEtat_[i] == ECART_T) {
                fonction_objectif_E_ += pbCoutLineaire_[i] * pbX_[i];
            } else {
                if (typeEtat_[i] == GRP_CUR_H || typeEtat_[i] == GRP_CUR_B) {
                    fonction_objectif_G_cur_ += pbCoutLineaire_[i] * pbX_[i];
                    fonction_objectif_G_cur_sans_offset_ += pbCoutLineaireSansOffset_[i] * pbX_[i];

                } else if (typeEtat_[i] == CONSO_H || typeEtat_[i] == CONSO_B) {
                    fonction_objectif_D_cur_ += pbCoutLineaire_[i] * pbX_[i];
                    fonction_objectif_D_cur_sans_offset_ += pbCoutLineaireSansOffset_[i] * pbX_[i];
                } else if (typeEtat_[i] == END_ENE) {
                    fonction_objectif_V += pbCoutLineaire_[i] * pbX_[i];
                } else {
                    fonction_objectif_C_ += pbCoutLineaire_[i] * pbX_[i];
                }
            }
        }
    }

    consoTotale_ = sumProdApresEmpilement;

    fonction_objectif_C_ += fonction_objectif_G_cur_ + fonction_objectif_D_cur_;

    if (!silent) {
        std::stringstream ss;
        ss << err::ioDico().msg("INFOResMW") << std::endl;
        ss << err::ioDico().msg("INFOVolProd", c_fmt("%12.4f", sumProdApresEmpilement)) << std::endl;
        ss << err::ioDico().msg("INFOVolDelestage", c_fmt("%12.4f", sumDelestage)) << std::endl;

        ss << err::ioDico().msg("INFOResCout") << std::endl;
        ss << err::ioDico().msg("INFOCoutGrp", c_fmt("%12.4f", fonction_objectif_G_)) << std::endl;
        ss << err::ioDico().msg("INFOCoutDelestage", c_fmt("%12.4f", fonction_objectif_D_)) << std::endl;
        ss << err::ioDico().msg("INFOCoutPenalisation", c_fmt("%12.4f", fonction_objectif_C_)) << std::endl;
        ss << err::ioDico().msg("INFOCoutVariablesEcart", c_fmt("%12.4f", fonction_objectif_E_)) << std::endl;
        if (fabs(fonction_objectif_V) > config::constants::epsilon) {
            ss << err::ioDico().msg("INFOCoutVariablesValorisation", c_fmt("%12.4f", fonction_objectif_V)) << std::endl;
        }
        ss << err::ioDico().msg("INFOFctObj",
                                c_fmt("%12.4f",
                                      fonction_objectif_D_ + fonction_objectif_G_ + fonction_objectif_C_
                                          + fonction_objectif_E_ + fonction_objectif_V))
           << std::endl;
        LOG_ALL(info) << ss.str();
    }
}


void Calculer::fixerVariablesEntieres()
{
    for (int i = res_.nbVarGroupes_ + res_.nbVarConsos_ + res_.nbVarTd_ + res_.nbVarCc_; i < pbNombreDeVariables_;
         ++i) {
        if (pbTypeDeVariable_[i] == ENTIER) {
            pbTypeDeVariable_[i] = REEL;
            pbXmin_[i] = pbX_[i];
            pbXmax_[i] = pbX_[i];
        }
    }
}


void Calculer::printStats()
{
    // statistiques sur les termes de la matrice des contraintes
    std::array<int, 10> coeffs;
    coeffs.fill(0);
    int nbCoeff = 0;
    for (int i = 0; i < pbNombreDeContraintes_; i++) {
        nbCoeff += pbNombreDeTermesDesLignes_[i];
        LOG(trace) << "Contrainte " << i + 1 << " : " << pbNombreDeTermesDesLignes_[i]
                   << " termes (début indice : " << pbIndicesDebutDeLigne_[i] << ")";
    }

    for (int i = 0; i < nbCoeff; i++) {
        double tmpCoeff = fabs(pbCoefficientsDeLaMatriceDesContraintes_[i]);
        if (tmpCoeff > 1e-2) {
            coeffs[0]++;
        } else if (tmpCoeff > 1e-3) {
            coeffs[1]++;
        } else if (tmpCoeff > 1e-4) {
            coeffs[2]++;
        } else if (tmpCoeff > 1e-5) {
            coeffs[3]++;
        } else if (tmpCoeff > 1e-6) {
            coeffs[4]++;
        } else if (tmpCoeff > 1e-7) {
            coeffs[5]++;
        } else if (tmpCoeff > 1e-8) {
            coeffs[6]++;
        } else if (tmpCoeff > 1e-9) {
            coeffs[7]++;
        } else if (tmpCoeff > 1e-10) {
            coeffs[8]++;
        } else {
            coeffs[9]++;
        }
    }
    std::stringstream ss;
    ss << "Nombre de coefficients dans la matrice des contraintes : " << nbCoeff << std::endl;
    ss << "Nombre de termes > 10e-2 " << coeffs[0] << std::endl;
    ss << "Nombre de termes entre 10e-2 et 10e-3 " << coeffs[1] << std::endl;
    ss << "Nombre de termes entre 10e-3 et 10e-4 " << coeffs[2] << std::endl;
    ss << "Nombre de termes entre 10e-4 et 10e-5 " << coeffs[3] << std::endl;
    ss << "Nombre de termes entre 10e-5 et 10e-6 " << coeffs[4] << std::endl;
    ss << "Nombre de termes entre 10e-6 et 10e-7 " << coeffs[5] << std::endl;
    ss << "Nombre de termes entre 10e-7 et 10e-8 " << coeffs[6] << std::endl;
    ss << "Nombre de termes entre 10e-8 et 10e-9 " << coeffs[7] << std::endl;
    ss << "Nombre de termes entre 10e-9 et 10e-10 " << coeffs[8] << std::endl;
    ss << "Nombre de termes < 10e-10 " << coeffs[9] << std::endl;

    LOG(trace) << ss.str();
}

void Calculer::afficherVariantesCle(const MapQuadinVar& variantesOrdonnees,
                                    const Quadripole::SetQuadripoleSortedByName& IndispoLignes)
{
    const auto& VectVar = variantesOrdonnees.at(IndispoLignes);
    std::stringstream ss;
    ss << "Variantes:";
    for (const auto& var : VectVar) {
        ss << " " << var->num_ << ";";
    }
    LOG_ALL(info) << ss.str();
}

void Calculer::assessAndPrintPTDF(const string& PTDFfileName)
{
    // print PTDFs matrix
    // ------------------------
    FILE* file = fopen(PTDFfileName.c_str(), "w+");
    if (file == nullptr) {
        throw ErrorI(err::ioDico().msg("ERRPbOuvertureFic", PTDFfileName));
    }
    LOG(debug) << "PTDF matrix creation";
    vector<string> noms;
    noms.resize(res_.nbVarGroupes_ + res_.nbVarConsos_);
    for (auto& elem : res_.groupes_) {
        if (elem.second->numVarGrp_ != -1) {
            noms[elem.second->numVarGrp_] = elem.second->nom_;
        }
    }
    for (auto consoIt = res_.consos_.cbegin(); consoIt != res_.consos_.end(); ++consoIt) {
        if (consoIt->second->numVarConso_ != -1) {
            noms[consoIt->second->numVarConso_] = consoIt->second->nom_;
        }
    }

    fprintf(file, "BRANCH;");
    for (int i = 0; i < pbNombreDeVariables_; i++) {
        if (i < res_.nbVarGroupes_) {
            fprintf(file, "%s;", noms[i].c_str());
            i++;
        } else if (i < res_.nbVarGroupes_ + res_.nbVarConsos_) {
            fprintf(file, "%s;", noms[i].c_str());
        } else if (i < res_.nbVarGroupes_ + res_.nbVarConsos_ + res_.nbVarTd_) {
            // do nothing
        } else if (i < res_.nbVarGroupes_ + res_.nbVarConsos_ + res_.nbVarTd_ + res_.nbVarCc_) {
            fprintf(file, "%s;", res_.lccParIndice_[numSupportEtat_[i]]->nom_.c_str());
            i++;
        }
    }
    fprintf(file, "\n");

    resetCoeffQuadsN();
    Quadripole::SetQuadripoleSortedByName quadSet(res_.quadsSurv_.begin(), res_.quadsSurv_.end());
    for (const auto& quad : quadSet) {
        calculerCoeffEnN(quad);
        fprintf(file, "%s;", quad->nom_.c_str());
        for (int i = 0; i < pbNombreDeVariables_; i++) {
            if (i < res_.nbVarGroupes_) {
                fprintf(file, "%f;", quad->coeffN_[numSupportEtat_[i]]);
                i++;
            } else if (i < res_.nbVarGroupes_ + res_.nbVarConsos_) {
                fprintf(file, "%f;", quad->coeffN_[numSupportEtat_[i]]);
            } else if (i < res_.nbVarGroupes_ + res_.nbVarConsos_ + res_.nbVarTd_) {
                // do nothing
            } else if (i < res_.nbVarGroupes_ + res_.nbVarConsos_ + res_.nbVarTd_ + res_.nbVarCc_) {
                fprintf(file,
                        "%f;",
                        quad->coeffN_[res_.lccParIndice_[numSupportEtat_[i]]->nexqua_->num_]
                            - quad->coeffN_[res_.lccParIndice_[numSupportEtat_[i]]->norqua_->num_]);
                i++;
            }
        }
        fprintf(file, "\n");
    }
    fclose(file);
}

void Calculer::printLODF(const string& LODFfileName, bool writeLODFfile) const
{
    if (writeLODFfile) {
        // print Line Outage Distribution Factors matrix
        // ------------------------------
        FILE* file = fopen(LODFfileName.c_str(), "w+");
        if (file == nullptr) {
            throw ErrorI(err::ioDico().msg("ERRPbOuvertureFic", LODFfileName));
        }
        LOG_ALL(debug) << "LODF matrix creation";

        fprintf(file, "BRANCH;");
        vector<std::shared_ptr<Incident>> listeInc;
        for (auto& elem : res_.incidents_) {
            auto& inc = elem.second;

            if (!inc->validite_ || inc->nbCouplagesFermes_ + inc->nbGroupes_ > 0 || inc->pochePerdue_ != nullptr) {
                continue; // We only put the valid contingencies for lines and HVDC which don't break connectedness
            }

            listeInc.push_back(inc);

            if (inc->nbLignes_ + inc->nbLccs_ == 1) {
                fprintf(file, "%s;", elem.first.c_str());
            } else {
                for (int i = 0; i < inc->nbLignes_; i++) {
                    fprintf(file, "%s_%s;", elem.first.c_str(), inc->listeQuads_[i]->nom_.c_str());
                }
                for (int i = 0; i < inc->nbLccs_; i++) {
                    fprintf(file, "%s_%s;", elem.first.c_str(), inc->listeLccs_[i]->nom_.c_str());
                }
            }
        }
        fprintf(file, "\n");

        Quadripole::SetQuadripoleSortedByName quadSet(res_.quadsSurv_.begin(), res_.quadsSurv_.end());
        for (const auto& quad : quadSet) {
            int numQuad = quad->num_;
            fprintf(file, "%s;", quad->nom_.c_str());
            for (auto& inc : listeInc) {
                if (!inc->validite_ || inc->nbCouplagesFermes_ + inc->nbGroupes_ > 0) {
                    continue; // we only put the lines contingencies or HVDC
                }
                for (int i = 0; i < inc->nbLignes_; i++) {
                    fprintf(file, "%.4f;", inc->rho_[i][numQuad]);
                }
                for (int i = 0; i < inc->nbLccs_; i++) {
                    fprintf(file, "%.4f;", inc->rho_[inc->nbLignes_ + i][numQuad]);
                }
            }
            fprintf(file, "\n");
        }
        fclose(file);
    }
}
