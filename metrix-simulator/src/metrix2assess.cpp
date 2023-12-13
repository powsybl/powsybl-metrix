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
#include "margin_variations_compute.h"
#include "parametres.h"
#include "pne.h"
#include "reseau.h"
#include "status.h"
#include "variante.h"

#include <cmath>
#include <cstdio>
#include <iostream>
#include <numeric>
#include <string>

using cte::c_fmt;


using std::string;
using std::vector;


static constexpr double EPSILON_SORTIES = 0.05; // seuil de precision pour les sorties
static const string PREC_FLOAT = "%.1f";        // Doit etre coherent avec EPSILON_SORTIE
static const string EMPTY_STRING;
static const string PREC_FLOAT_BIS = "%.4f";
static constexpr double EPSILON_SORTIES_BIS = 0.0001;

static void print_threats(FILE* file,
                          const Menace& threat_before,
                          const std::map<std::shared_ptr<Incident>, int>& constraints_incidents,
                          const std::string& name,
                          const std::set<Menace, bool (*)(const Menace&, const Menace&)>& threats)
{
    fprintf(file, "R3B ;;%s;", name.c_str());

    // Menace max avant parade
    if (threat_before.defaut_ != nullptr) {
        double value_transit = (fabs(threat_before.transit_) < EPSILON_SORTIES) ? 0.0 : threat_before.transit_;
        const auto& inc = threat_before.defaut_->parade_ ? threat_before.defaut_->incTraiteCur_ : threat_before.defaut_;
        fprintf(file, "%d;%.1f;", constraints_incidents.find(inc)->second, value_transit);
    } else {
        fprintf(file, ";;");
    }
    for (auto rit = threats.crbegin(); rit != threats.crend(); ++rit) {
        const auto& inc = rit->defaut_->parade_ ? rit->defaut_->incTraiteCur_ : rit->defaut_;
        double value_transit = (fabs(rit->transit_) < EPSILON_SORTIES) ? 0.0 : rit->transit_;
        fprintf(file, "%d;%.1f;", constraints_incidents.find(inc)->second, value_transit);
    }

    for (unsigned int j = 0; j < config::configuration().nbThreats() - threats.size(); ++j) {
        fprintf(file, ";;");
    }
    fprintf(file, "\n");
}

static std::string computeC2IncidentName(const std::shared_ptr<Incident>& inc)
{
    string nomInc = inc->nom_;
    if (inc->parade_) {
        nomInc = inc->incTraiteCur_->nom_;
    } else if (inc->paradesActivees_) {
        auto str = nomInc;
        nomInc = "BEFORE_CURATIVE_";
        nomInc += str;
    }

    return nomInc;
}

bool Calculer::findNumIncident(const std::map<std::shared_ptr<Incident>, int>& incidentsContraignants,
                               int indexConstraint,
                               int& numIncident) const
{
    if (pbTypeContrainte_[indexConstraint] == COUPE_SURETE_N) {
        numIncident = 0;
    } else {
        const auto& icdt = pbContraintes_[indexConstraint]->icdt_;
        auto itNumIncident = incidentsContraignants.end();
        if (icdt->parade_) {
            itNumIncident = incidentsContraignants.find(icdt->incTraiteCur_);
        } else {
            itNumIncident = incidentsContraignants.find(icdt);
        }
        if (itNumIncident != incidentsContraignants.end()) {
            numIncident = itNumIncident->second;
        } else {
            LOG_ALL(warning) << "Erreur incident inconnu";
            return false;
        }
    }

    return true;
}

void Calculer::printCutDetailed(FILE* fr)
{
    unsigned int nb_display = 0;
    fprintf(fr, "C2B ;NON CONNEXITE;INCIDENT;CONSO;CONSO COUPEE;\n");
    for (const auto& inc : res_.incidentsRompantConnexite_) {
        if (!inc->validite_ || !inc->pochePerdue_) {
            continue;
        }

        // on n'affiche que les parades activees
        if (inc->parade_ && (inc->numVarActivation_ == -1 || pbX_[inc->numVarActivation_] < 0.5)) {
            continue;
        }

        auto nomInc = computeC2IncidentName(inc);

        const auto& poche = inc->pochePerdue_;
        for (const auto& pair : poche->consumptionLosses_) {
            if (fabs(pair.second) < EPSILON_SORTIES) {
                continue;
            }

            if (nb_display >= config::configuration().lostLoadDetailedMax()) {
                LOG(warning) << err::ioDico().msg("WARNTooMuchCut",
                                                  c_fmt("%lu", config::configuration().showLostLoadDetailed()));
                return;
            }

            auto value_str = c_fmt(PREC_FLOAT.c_str(), pair.second);
            fprintf(fr, "C2B ;;%s;%s;%s;\n", nomInc.c_str(), pair.first->nom_.c_str(), value_str.c_str());
            nb_display++;
        }
    }
}

void Calculer::printCut(FILE* fr)
{
    fprintf(fr, "C2 ;NON CONNEXITE;INCIDENT;NB NOEUDS;PROD COUPEE;CONSO COUPEE;\n");
    for (const auto& inc : res_.incidentsRompantConnexite_) {
        if (!inc->validite_ || !inc->pochePerdue_) {
            continue;
        }

        // on n'affiche que les parades activees
        if (inc->parade_ && (inc->numVarActivation_ == -1 || pbX_[inc->numVarActivation_] < 0.5)) {
            continue;
        }

        auto nomInc = computeC2IncidentName(inc);

        const auto& poche = inc->pochePerdue_;

        if (fabs(poche->prodPerdue_) >= EPSILON_SORTIES || fabs(poche->consoPerdue_) >= EPSILON_SORTIES) {
            string prodPerdue = fabs(poche->prodPerdue_) >= EPSILON_SORTIES
                                    ? c_fmt(PREC_FLOAT.c_str(), poche->prodPerdue_)
                                    : EMPTY_STRING;
            string consoPerdue = fabs(poche->consoPerdue_) >= EPSILON_SORTIES
                                     ? c_fmt(PREC_FLOAT.c_str(), poche->consoPerdue_)
                                     : EMPTY_STRING;
            fprintf(fr,
                    "C2 ;;%s;%lu;%s;%s;\n",
                    nomInc.c_str(),
                    poche->noeudsPoche_.size(),
                    prodPerdue.c_str(),
                    consoPerdue.c_str());
        }
    }
}

int Calculer::metrix2Assess(const std::shared_ptr<Variante>& var, const vector<double>& theta, int status)
{
    FILE* fr;

    string nom1 = c_fmt("%s_s%d", config::inputConfiguration().filepathResults().c_str(), var->num_);
    fr = fopen(&nom1[0], "w+");
    if (fr == nullptr) {
        throw ErrorI(err::ioDico().msg("ERRPbOuvertureFic", nom1));
    }

    try {
        if (config::inputConfiguration().useAllOutputs()) {
            // ecriture : S1.
            //--------------
            fprintf(fr, "S1 ;INDISPONIBILITE; OUVRAGE.\n");
            if (var->num_ != -1) {
                for (const auto& grp : var->grpIndispo_) {
                    fprintf(fr, "S1 ;;2; %s;\n", grp->nom_.c_str());
                }
                for (const auto& quad : var->indispoLignes_) {
                    fprintf(fr, "S1 ;;1; %s;\n", quad->nom_.c_str());
                }
            }
        }

        // C5 : bilan initiaux par zone synchrone (avant equilibrage)
        // ----------------------------------------------------------
        // On les ecrit avant le status car il peuvent servir au debug
        fprintf(fr, "C5 ;ZONE SYNC;NUM ZONE;BILAN;\n");
        for (const auto& pair : res_.bilanParZone_) {
            double value = (fabs(pair.second) < EPSILON_SORTIES) ? 0.0 : pair.second;
            fprintf(fr, "C5 ;;%d;%.1f;\n", pair.first, value);
        }

        // ecriture : C2.
        //--------------
        printCut(fr);
        // writing : C2B
        // detailed consumption losses
        if (config::configuration().showLostLoadDetailed()) {
            printCutDetailed(fr);
        }

        // ecriture : C1. si probleme
        //-------------

        if (status != METRIX_PAS_PROBLEME) {
            fprintf(fr, "C1 ;COMPTE RENDU;CODE;\n");
            fprintf(fr, "C1 ;;%d;\n", status);
            fclose(fr);
            return METRIX_PAS_PROBLEME; // pour continuer les autres variantes
                                        // mais ne pas ecrire de resultats pour cette variante sans sol
        }


        // ecriture : C4.
        //--------------
        std::map<std::shared_ptr<Incident>, int> incidentsContraignants;

        fprintf(fr, "C4 ;INCIDENTS;NUMERO;TYPE;OUVRAGE;\n");

        // Incidents generant une menace max sur un ouvrage en N-k
        for (const auto& elemSurv : res_.elementsASurveiller_) {
            if (elemSurv->survMaxInc_ == ElementASurveiller::NON_SURVEILLE) {
                continue;
            }

            if (elemSurv->menaceMaxAvantParade_.defaut_ != nullptr
                && !elemSurv->menaceMaxAvantParade_.defaut_->parade_) {
                incidentsContraignants.insert(std::make_pair(elemSurv->menaceMaxAvantParade_.defaut_, 0));
            }

            for (auto rit = elemSurv->menacesMax_.crbegin(); rit != elemSurv->menacesMax_.crend(); ++rit) {
                if (!(*rit).defaut_->parade_) {
                    incidentsContraignants.insert(std::make_pair((*rit).defaut_, 0));
                }
            }
        }

        // Incidents avec transit
        for (const auto& inc : res_.incidentsAvecTransits_) {
            incidentsContraignants.insert(std::make_pair(inc, 0));
        }

        // Incidents generant des contraintes limitantes (i.e. avec des couts marginaux)
        if (config::configuration().useResVarMarLigne()
            && (config::configuration().computationType() == config::Configuration::ComputationType::OPF
                || config::configuration().computationType()
                       == config::Configuration::ComputationType::OPF_WITH_OVERLOAD)) {
            if (typeSolveur_ == UTILISATION_PNE_SOLVEUR) {
                // Relance avec SPX car PNE ne retourne pas la base de la solution optimale
                LOG_ALL(info) << "Relance sans variables entieres pour le calcul de la base";
                fixerVariablesEntieres();
                PneSolveur(UTILISATION_SIMPLEXE, var);
            }

            for (int i = 1; i < pbNombreDeContraintes_; ++i) {
                if (pbTypeContrainte_[i] == COUPE_AUTRE || pbTypeContrainte_[i] == COUPE_BILAN) {
                    continue;
                }

                const auto& inc = pbContraintes_[i]->icdt_;
                if (!inc) {
                    continue;
                }

                if (fabs(pbCoutsMarginauxDesContraintes_[i]) < config::constants::epsilon) {
                    continue;
                }

                //	inc = pbContraintes_[i]->icdt_;

                if (inc->parade_) {
                    incidentsContraignants.insert(std::make_pair(inc->incTraiteCur_, 0));
                } else {
                    incidentsContraignants.insert(std::make_pair(inc, 0));
                }
            }
        }

        // numerotation des incidents contraignants
        int numIncidentContraignant = 0;

        for (const auto& elem : res_.incidents_) {
            const auto& inc = elem.second;

            if (!inc->validite_) {
                continue;
            }

            // On ajoute aussi les incidents qui sont traites en curatif ou en parade
            // pour les tables R5B, R6B et R10
            if (inc->incidentATraiterEncuratif_ || inc->paradesActivees_) {
                incidentsContraignants[inc] = ++numIncidentContraignant;
            } else {
                if (incidentsContraignants.find(inc) == incidentsContraignants.end()) {
                    continue;
                }

                incidentsContraignants[inc] = ++numIncidentContraignant;
            }

            string typeInc = "COMBO";
            if (inc->type_ == Incident::N_MOINS_1_LIGNE) {
                typeInc = "N-1L";
            } else if (inc->type_ == Incident::N_MOINS_K_GROUPE) {
                typeInc = "N-KG";
            } else if (inc->type_ == Incident::N_MOINS_K_GROUPE_LIGNE && inc->nbLignes_ >= 2 && inc->nbGroupes_ == 0) {
                typeInc = "N-KL";
            } else if (inc->nbLccs_ > 0 && inc->nbLignes_ == 0 && inc->nbGroupes_ == 0) {
                typeInc = "N-HVDC";
            }

            fprintf(fr, "C4 ;;%5d;%s;%s;\n", numIncidentContraignant, typeInc.c_str(), inc->nom_.c_str());
        }

        // Ecriture R4 et R4B (variations marginales)
        // -------------------
        if (config::configuration().useResVarMarLigne()
            && (config::configuration().computationType() == config::Configuration::ComputationType::OPF
                || config::configuration().computationType()
                       == config::Configuration::ComputationType::OPF_WITH_OVERLOAD)
            && pbNombreDeContraintes_ > 0) {
            if (!calculVariationsMarginales(fr, incidentsContraignants)) {
                fclose(fr);
                return METRIX_PAS_PROBLEME; // pour continuer les autres variantes
                                            // mais ne pas ecrire de resultats pour cette variante sans sol
            }
        }

        // Calcul des pertes
        pertesTotales_ = 0;
        double tran = 0.;
        double pertesQuad = 0.;
        std::map<string, double> pertesParRegion;

        // pertes Quads
        for (const auto& elem : res_.quads_) {
            const auto& quad = elem.second;

            if (quad->connecte()) {
                if (quad->typeQuadripole_ == Quadripole::QUADRIPOLE_REEL) {
                    if (quad->td_ && quad->td_->fictif_) {
                        continue; // HVDC en emulation AC, traite plus bas
                    }

                    tran = quad->u2Yij_ * (theta[quad->norqua_->num_] - theta[quad->nexqua_->num_]);
                    unsigned int uref = config::configuration().uRef();
                    pertesQuad = (tran / uref) * (tran / uref) * quad->r_;

                    pertesTotales_ += pertesQuad;

                    if (config::configuration().useResPertesDetail()) {
                        pertesQuad *= 0.5;
                        pertesParRegion[res_.regions_[quad->norqua_->numRegion_]] += pertesQuad;
                        pertesParRegion[res_.regions_[quad->nexqua_->numRegion_]] += pertesQuad;
                    }
                }
            }
        }

        // pertes HVDC
        double puissHVDC = 0.;

        for (const auto& elem : res_.LigneCCs_) {
            const auto& hvdc = elem.second;
            puissHVDC = hvdc->puiCons_ + pbX_[hvdc->numVar_] - pbX_[hvdc->numVar_ + 1];

            if (hvdc->isEmulationAC()) {
                const auto& quad = hvdc->quadFictif_;
                puissHVDC += quad->u2Yij_ * (theta[quad->norqua_->num_] - theta[quad->nexqua_->num_]);
            }

            double coeffDepart = 0.;
            double coeffSortie = 0.;

            if (puissHVDC > 0) {
                coeffDepart = hvdc->coeffPertesOr_ / 100.;
                coeffSortie = hvdc->coeffPertesEx_ / 100.;
            } else {
                coeffDepart = hvdc->coeffPertesEx_ / 100.;
                coeffSortie = hvdc->coeffPertesOr_ / 100.;
                puissHVDC = fabs(puissHVDC);
            }

            // pertes convertisseur redresseur
            double p2 = (1 - coeffDepart) * puissHVDC;
            double pertesHVDC = coeffDepart * puissHVDC;

            // pertes cable
            double rOhm = hvdc->r_ * pow(hvdc->vdc_ / config::configuration().uRef(), 2);
            if (rOhm == 0.) {
                LOG_ALL(warning) << err::ioDico().msg("ERRPerteHVDC", hvdc->nom_);
                continue;
            }

            double delta = pow(hvdc->vdc_, 2) + 4 * p2 * rOhm;
            if (delta < 0) {
                LOG_ALL(warning) << err::ioDico().msg("ERRPerteHVDC", hvdc->nom_);
                continue;
            }

            double vdc2 = (hvdc->vdc_ + sqrt(delta)) / 2;
            double intensDc = (vdc2 - hvdc->vdc_) / rOhm; // en kA
            pertesHVDC += rOhm * pow(intensDc, 2);        // en MW

            // pertes convertisseur onduleur
            double pOnduleur = puissHVDC - pertesHVDC;
            pertesHVDC += coeffSortie * pOnduleur; // en MW

            pertesTotales_ += pertesHVDC;

            if (config::configuration().useResPertesDetail()) {
                pertesParRegion[hvdc->nom_] = pertesHVDC;
            }
        }


        double volDel = 0.;

        // ecriture : R1.
        //--------------
        if (!config::inputConfiguration().useAllOutputs()) {
            fprintf(fr, "R1 ;PAR CONSO;CONSO;VALEUR;DF HR;DF AR;\n");
        } else {
            fprintf(fr, "R1 ;PAR CONSO;CONSO;DEMANDE;DF HR;CDF HR;DF AR;CDF AR;\n");
        }

        for (const auto& elem : res_.consos_) {
            const auto& conso = elem.second;

            if (conso->numVarConso_ >= 0) {
                if (!config::inputConfiguration().useAllOutputs()) {
                    double valEquilibrage = pbXhR_[conso->numVarConso_];
                    double valRedispatching = pbX_[conso->numVarConso_];

                    if (fabs(valEquilibrage) < EPSILON_SORTIES_BIS && fabs(valRedispatching) < EPSILON_SORTIES_BIS) {
                        continue;
                    }

                    string resEquilibrage = EMPTY_STRING;
                    if (fabs(valEquilibrage) >= EPSILON_SORTIES_BIS) {
                        resEquilibrage = c_fmt(PREC_FLOAT_BIS.c_str(), valEquilibrage);
                    }
                    string resRedispatching = EMPTY_STRING;

                    if (fabs(valRedispatching) >= EPSILON_SORTIES) {
                        volDel += valRedispatching;
                        resRedispatching = c_fmt(PREC_FLOAT.c_str(), valRedispatching);
                    }

                    if (resEquilibrage != EMPTY_STRING || resRedispatching != EMPTY_STRING) {
                        fprintf(fr,
                                ("R1 ;;%s;" + PREC_FLOAT_BIS + ";%s;%s;\n").c_str(),
                                conso->nom_.c_str(),
                                conso->valeur_,
                                resEquilibrage.c_str(),
                                resRedispatching.c_str());
                    }
                } else {
                    fprintf(fr,
                            "R1 ;;%s;%7.3f;%7.3f;%7.3f;%7.3f;%7.3f;\n",
                            conso->nom_.c_str(),
                            conso->valeur_,
                            pbXhR_[conso->numVarConso_],
                            pbXhR_[conso->numVarConso_] * pbCoutLineaire_[conso->numVarConso_],
                            pbX_[conso->numVarConso_],
                            pbX_[conso->numVarConso_] * pbCoutLineaire_[conso->numVarConso_]);
                }
            } else if (config::inputConfiguration().useAllOutputs()) {
                fprintf(fr, "R1 ;;%s;%7.3f;%7.3f;%7.3f;%7.3f;%7.3f;\n", conso->nom_.c_str(), 0.0, 0.0, 0.0, 0.0, 0.0);
            }
        }

        // ecriture : R1B: Effacements curatifs
        //------------------------------------

        double volDelCur = 0;

        if (res_.nbConsosCuratifs_ > 0) {
            fprintf(fr, "R1B ;INCIDENT;CONSO;EFFACEMENT;\n");

            for (const auto& elem : res_.incidents_) {
                const auto& icdt = elem.second;

                if (!icdt->validite_) {
                    continue;
                }

                auto tmpListeElemCur = icdt->listeElemCur_;

                if (icdt->paradesActivees_) {
                    for (const auto& parade : icdt->parades_) {
                        if ((parade->numVarActivation_ != -1) && (pbX_[parade->numVarActivation_] > 0.5)) {
                            tmpListeElemCur = parade->listeElemCur_;
                            break;
                        }
                    }
                }

                for (const auto& elemC : tmpListeElemCur) {
                    if (elemC->typeElem_ != ElementCuratif::CONSO) {
                        continue;
                    }

                    int pos = elemC->positionVarEntiereCur_;
                    if ((pos == -1) || (pbX_[pos] > 0.5)) { // Curatif active sur parade (ou pas de parade associee)

                        pos = elemC->positionVarCurative_;
                        if (pos != -1) {
                            double val = pbX_[pos] - pbX_[pos + 1];

                            if (!config::inputConfiguration().useAllOutputs() && fabs(val) < EPSILON_SORTIES) {
                                continue;
                            }
                            volDelCur += val;
                            const auto& conso = std::dynamic_pointer_cast<ElementCuratifConso>(elemC)->conso_;
                            fprintf(fr,
                                    ("R1B ;%d;%s;" + PREC_FLOAT + ";\n").c_str(),
                                    incidentsContraignants.find(icdt)->second,
                                    conso->nom_.c_str(),
                                    val);
                        }
                    }
                } // end for elemCur
            }     // end for incident
        }         // End R1B

        // ecriture : R1C: Couplages de consos
        //---------------
        if (res_.nbConsosCouplees_ > 0) {
            double val;
            fprintf(fr, "R1C ;NOM REGROUPEMENT;DELTA_C;\n");
            for (const auto& binding : res_.consosCouplees_) {
                val = 0;
                for (const auto& list : binding->elements_) {
                    val -= pbX_[list->numVarConso_];
                }
                if (!config::inputConfiguration().useAllOutputs() && fabs(val) < EPSILON_SORTIES) {
                    continue;
                }
                fprintf(fr, ("R1C ;%s;" + PREC_FLOAT + ";\n").c_str(), binding->nomRegroupement_.c_str(), val);
            }
        } // End R1C

        // ecriture : R2. Productions groupe
        //---------------------------------

        vector<double> redispatchParTypeH(res_.nbTypesGroupes_, 0.);
        vector<double> redispatchParTypeB(res_.nbTypesGroupes_, 0.);

        if (!config::inputConfiguration().useAllOutputs()) {
            fprintf(fr, "R2 ;PAR GROUPE;GROUPE;PDISPO;DELTA_PIMP;DELTA_P_HR;DELTA_P_AR;\n");
        } else {
            fprintf(
                fr,
                "R2 ;PAR GROUPE;GROUPE;PDISPO;PIMP;PU HR;PU AR;CT HR;CT AR;CT ARP;CT GRT;CT GRTP;CT HAUSE AR;CT BAISSE "
                "AR;CT ORDRE;CT EMPIL HR;\n");
        }

        double deltaHR;
        double deltaAR;
        string sDeltaHR;
        string sDeltaAR;

        for (const auto& elem : res_.groupes_) {
            const auto& grp = elem.second;

            if (!grp->etat_) {
                continue;
            }

            if (grp->prodAjust_ == Groupe::NON_HR_AR) {
                if (!config::inputConfiguration().useAllOutputs()) {
                    continue;
                }

                fprintf(fr,
                        "R2 ;;%s;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;\n",
                        // fprintf(fr, " R2;%d;%s;%10.5f;%10.5f; %10.5f; %10.5; %10.5; %10.5; %10.5; %10.5;\n",
                        grp->nom_.c_str(),
                        grp->puisMaxDispo_,
                        grp->prod_,
                        grp->prod_,
                        grp->prod_,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        0.,
                        0.,
                        0.,
                        grp->coutHausseHR_);

            } else {
                int numVar = grp->numVarGrp_;

                deltaHR = pbXhR_[numVar] - pbXhR_[numVar + 1];
                deltaAR = pbX_[numVar] - pbX_[numVar + 1];

                if (grp->type_ != -1) {
                    if (deltaAR >= 0) {
                        redispatchParTypeH[grp->type_] += deltaAR;
                    } else {
                        redispatchParTypeB[grp->type_] -= deltaAR;
                    }
                }

                if (!config::inputConfiguration().useAllOutputs()) {
                    sDeltaHR = EMPTY_STRING;
                    sDeltaAR = EMPTY_STRING;
                    if (fabs(deltaHR) >= EPSILON_SORTIES_BIS) { // seuil resultat equilibrage
                        sDeltaHR = c_fmt(PREC_FLOAT_BIS.c_str(), deltaHR);
                    }
                    if (config::configuration().displayResultatsRedispatch()
                        && fabs(deltaAR) >= EPSILON_SORTIES) { // seuil resultat redispatching preventif
                        sDeltaAR = c_fmt(PREC_FLOAT.c_str(), deltaAR);
                    }

                    if (sDeltaHR == EMPTY_STRING && sDeltaAR == EMPTY_STRING) {
                        continue;
                    }

                    std::string str("R2 ;;%s;");
                    str += PREC_FLOAT_BIS;
                    str += ";";
                    str += PREC_FLOAT_BIS;
                    str += ";%s;%s;\n";
                    fprintf(fr,
                            str.c_str(),
                            grp->nom_.c_str(),
                            grp->puisMaxDispo_,
                            grp->prodPobj_,
                            sDeltaHR.c_str(),
                            sDeltaAR.c_str());
                } else {
                    double c_ar = 0.0;
                    double c_grt = 0.0;
                    double cout_hausse = 0.0;
                    double cout_baisse = 0.0;
                    double p_ar = grp->prod_ + deltaAR;
                    double p_hr = grp->prodPobj_ + deltaHR;
                    if (p_ar > p_hr) {
                        c_grt = (p_ar - p_hr) * grp->coutHausseAR_;
                    } else {
                        c_grt = (p_hr - p_ar) * grp->coutBaisseAR_;
                    }
                    cout_baisse = grp->coutBaisseAR_;
                    cout_hausse = grp->coutHausseAR_;
                    c_ar = 0.;

                    fprintf(fr,
                            "R2 ;;%s;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;\n",
                            grp->nom_.c_str(),
                            grp->puisMaxDispo_,
                            grp->prodPobj_,
                            deltaHR,
                            deltaAR,
                            grp->coutHausseHR_ * pbXhR_[numVar] + grp->coutBaisseHR_ * pbXhR_[numVar + 1],
                            c_ar,
                            0.0,
                            c_grt,
                            0.0,
                            cout_hausse,
                            cout_baisse,
                            grp->coutBaisseHR_,
                            grp->coutHausseHR_);
                }
            }
        }

        // ecriture : R2B: Curatif des groupes
        //--------------
        vector<double> maxRedispCurParTypeH(res_.nbTypesGroupes_, 0.);
        vector<double> maxRedispCurParTypeB(res_.nbTypesGroupes_, 0.);
        vector<double> redispCurParType(res_.nbTypesGroupes_, 0.);

        if (res_.nbGroupesCuratifs_ > 0) {
            fprintf(fr, "R2B ;INCIDENT;NOM GROUPE;DELTA_P;\n");

            for (const auto& elem : res_.incidents_) {
                const auto& icdt = elem.second;

                if (!icdt->validite_) {
                    continue;
                }

                auto tmpListeElemCur = icdt->listeElemCur_;

                if (icdt->paradesActivees_) {
                    for (const auto& parade : icdt->parades_) {
                        if ((parade->numVarActivation_ != -1) && (pbX_[parade->numVarActivation_] > 0.5)) {
                            tmpListeElemCur = parade->listeElemCur_;
                            break;
                        }
                    }
                }

                vector<double> tmpMaxRedispCurParTypeH(res_.nbTypesGroupes_, 0.);
                vector<double> tmpMaxRedispCurParTypeB(res_.nbTypesGroupes_, 0.);

                for (const auto& elemC : tmpListeElemCur) {
                    if (elemC->typeElem_ != ElementCuratif::GROUPE) {
                        continue;
                    }

                    int pos = elemC->positionVarEntiereCur_;
                    if ((pos == -1) || (pbX_[pos] > 0.5)) { // Curatif active sur parade (ou pas de parade associee)

                        pos = elemC->positionVarCurative_;
                        if (pos != -1) {
                            double val = config::constants::valdef;
                            const auto& grp = std::dynamic_pointer_cast<ElementCuratifGroupe>(elemC)->groupe_;
                            string s = grp->nom_;

                            val = pbX_[pos] - pbX_[pos + 1];

                            if (grp->type_ != -1) {
                                redispCurParType[grp->type_] += pbX_[pos] + pbX_[pos + 1];
                                if (val >= 0) {
                                    tmpMaxRedispCurParTypeH[grp->type_] += val;
                                } else {
                                    tmpMaxRedispCurParTypeB[grp->type_] += -val;
                                }
                            }

                            if (config::inputConfiguration().useAllOutputs()
                                || (config::configuration().displayResultatsRedispatch()
                                    && fabs(val) >= EPSILON_SORTIES)) {
                                fprintf(fr,
                                        ("R2B ;%d;%s;" + PREC_FLOAT + ";\n").c_str(),
                                        incidentsContraignants.find(icdt)->second,
                                        s.c_str(),
                                        val);
                            }
                        }
                    }

                    for (int i = 0; i < res_.nbTypesGroupes_; ++i) {
                        maxRedispCurParTypeH[i] = std::max(tmpMaxRedispCurParTypeH[i], maxRedispCurParTypeH[i]);
                        maxRedispCurParTypeB[i] = std::max(tmpMaxRedispCurParTypeB[i], maxRedispCurParTypeB[i]);
                    }

                } // end for elemCur
            }     // end for incident
        }         // End R2B

        // ecriture : R2C: Couplages de groupes
        //---------------
        if (res_.nbGroupesCouples_ > 0) {
            double val;
            int numVar;
            fprintf(fr, "R2C ;NOM REGROUPEMENT;DELTA_P;\n");
            for (const auto& binding : res_.groupesCouples_) {
                val = 0;
                for (const auto& elem : binding->elements_) {
                    numVar = elem->numVarGrp_;
                    val += pbX_[numVar] - pbX_[numVar + 1];
                }
                if (!config::inputConfiguration().useAllOutputs() && fabs(val) < EPSILON_SORTIES) {
                    continue;
                }
                fprintf(fr, ("R2C ;%s;" + PREC_FLOAT + ";\n").c_str(), binding->nomRegroupement_.c_str(), val);
            }
        } // End R2C


        // ecriture : R3.
        //--------------
        double sommeEcartsN = 0.;
        double transitN;
        fprintf(fr, "R3 ;PAR LIGNE;LIGNE;TRANSIT N;SEUIL N;SEUIL N-k;SEUIL ITAM;\n");
        // identification des quadripoles en limite en N
        for (const auto& elemSurv : res_.elementsASurveiller_) {
            if (elemSurv->survMaxN_ == ElementASurveiller::NON_SURVEILLE) {
                continue;
            }

            if (elemSurv->quadsASurv_.size() == 1 && elemSurv->hvdcASurv_.empty()) {
                const auto& quad = elemSurv->quadsASurv_.begin()->first;

                if (quad->typeQuadripole_ != Quadripole::QUADRIPOLE_REEL) {
                    continue;
                }

                if (!quad->connecte()) {
                    if (!config::inputConfiguration().useAllOutputs()) {
                        if (config::configuration().displayResultatsSurcharges()) {
                            continue;
                        }

                        fprintf(fr, "R3 ;;%s;0.0;\n", quad->nom_.c_str());
                    } else {
                        fprintf(fr,
                                "R3 ;;%s;%.1f;%.1f;%.1f;%.1f;\n",
                                quad->nom_.c_str(),
                                0.0,
                                elemSurv->seuilMaxN_,
                                elemSurv->seuilMaxInc_,
                                elemSurv->seuilMaxAvantCur_);
                    }
                } else {
                    transitN = quad->u2Yij_ * (theta[quad->norqua_->num_] - theta[quad->nexqua_->num_]);

                    double maxT = fabs(elemSurv->seuil(nullptr, transitN));
                    double ecart = 0;

                    if (maxT != config::constants::valdef) {
                        ecart = std::max(fabs(transitN) - maxT, 0.);
                        sommeEcartsN += ecart;
                    }

                    if (!config::inputConfiguration().useAllOutputs()) {
                        if (config::configuration().displayResultatsSurcharges() && ecart < EPSILON_SORTIES) {
                            continue;
                        }
                        if (fabs(transitN) < EPSILON_SORTIES) {
                            transitN = 0.0;
                        }

                        fprintf(fr, ("R3 ;;%s;" + PREC_FLOAT + ";\n").c_str(), quad->nom_.c_str(), transitN);
                    } else {
                        fprintf(fr,
                                "R3 ;;%s;%.1f;%.1f;%.1f;%.1f;\n",
                                quad->nom_.c_str(),
                                transitN,
                                elemSurv->seuilMaxN_,
                                elemSurv->seuilMaxInc_,
                                elemSurv->seuilMaxAvantCur_);
                    }
                }
            }
        }

        if (config::configuration().nbThreats() > 0) {
            // ecriture : R3B (Menaces max N-k HR)
            //------------------------------------
            Menace menace;
            fprintf(fr, "R3B ;PAR LIGNE;LIGNE;INCIDENT AM;MENACE MAX AM;");
            for (unsigned int i = 1; i <= config::configuration().nbThreats(); ++i) {
                fprintf(fr, "INCIDENT %u;TRANSIT N-k MAX %u;", i, i);
            }
            fprintf(fr, "\n");

            if (res_.nbIncidents_ > 0) {
                for (const auto& elemSurv : res_.elementsASurveiller_) {
                    if (elemSurv->survMaxInc_ == ElementASurveiller::NON_SURVEILLE) {
                        continue;
                    }

                    // Menace max avant parade
                    menace = elemSurv->menaceMaxAvantParade_;

                    if (!config::inputConfiguration().useAllOutputs()
                        && config::configuration().displayResultatsSurcharges() && menace.defaut_ == nullptr
                        && elemSurv->menacesMax_.empty()) {
                        continue;
                    }

                    print_threats(fr,
                                  elemSurv->menaceMaxAvantParade_,
                                  incidentsContraignants,
                                  elemSurv->nom_,
                                  elemSurv->menacesMax_);
                }
            }
        }

        // ecriture : R3C (transits sur incidents)
        //------------------------------------
        if (!res_.transitsSurDefauts_.empty()) {
            fprintf(fr, "R3C ;PAR LIGNE;LIGNE;INCIDENT;TRANSIT;\n");
            for (const auto& def : res_.transitsSurDefauts_) {
                const auto& quad = def.first;
                const auto& listeIcdt = def.second;
                for (const auto& icdt : listeIcdt) {
                    if (icdt->validite_) {
                        double transit = 0.;
                        if (quad->connecte()) {
                            if (icdt->paradesActivees_) {
                                // Recherche de la parade activee
                                for (const auto& parade : icdt->parades_) {
                                    if (parade->numVarActivation_ != -1 && pbX_[parade->numVarActivation_] > 0.5) {
                                        transit = transitSurQuad(quad, parade, theta);
                                        break;
                                    }
                                }
                            } else {
                                transit = transitSurQuad(quad, icdt, theta);
                            }
                        }
                        if (fabs(transit) < EPSILON_SORTIES) {
                            transit = 0.0;
                        }
                        fprintf(fr,
                                "R3C ;;%s;%d;%.1f\n",
                                quad->nom_.c_str(),
                                incidentsContraignants.find(icdt)->second,
                                transit);
                    }
                }
            }
        }

        // ecriture : R5.
        //--------------
        fprintf(fr, "R5 ;PAR TD;TD;CONSIGNE;PRISE; \n");
        for (const auto& elem : res_.TransfoDephaseurs_) {
            const auto& td = elem.second;

            if (!config::inputConfiguration().useAllOutputs()) {
                if (td->fictif_) {
                    continue;
                }

                if (!config::configuration().showAllAngleTDTransitHVDC() && // always display according to parameter
                    (td->fictif_ || td->type_ == TransformateurDephaseur::HORS_SERVICE
                     || td->type_ == TransformateurDephaseur::PILOTAGE_ANGLE_IMPOSE
                     || td->type_ == TransformateurDephaseur::PILOTAGE_PUISSANCE_IMPOSE || !td->quadVrai_->connecte()
                     || (pbX_[td->numVar_] < config::constants::epsilon
                         && pbX_[td->numVar_ + 1] < config::constants::epsilon))) {
                    continue;
                }
            }
            string s = td->quadVrai_->nom_;
            double val = td->puiCons_ + pbX_[td->numVar_] - pbX_[td->numVar_ + 1];
            // recuperation du numero de prise le plus proche
            int priseFinale;
            if (td->nbtap_ != 0) {
                priseFinale = td->getClosestTapPosition(td->power2Angle(val)) + td->lowtap_;
            } else {
                priseFinale = config::constants::valdef;
            }
            fprintf(fr, "R5 ;;%s;%.2f;%d; \n", s.c_str(), td->power2Angle(val), priseFinale);
        }
        // ecriture : R5B: Td en mode curatif
        //--------------
        fprintf(fr, "R5B ;INCIDENT;NOM TD; CONSIGNE;PRISE; \n");

        for (const auto& elem : res_.incidents_) {
            const auto& icdt = elem.second;

            if (!icdt->validite_) {
                continue;
            }

            auto tmpListeElemCur = icdt->listeElemCur_;

            if (icdt->paradesActivees_) {
                for (const auto& parade : icdt->parades_) {
                    if ((parade->numVarActivation_ != -1) && (pbX_[parade->numVarActivation_] > 0.5)) {
                        tmpListeElemCur = parade->listeElemCur_;
                        break;
                    }
                }
            }

            for (const auto& elemC : tmpListeElemCur) {
                if (elemC->typeElem_ != ElementCuratif::TD) {
                    if (!config::inputConfiguration().useAllOutputs()
                        || elemC->typeElem_ != ElementCuratif::TD_FICTIF) {
                        continue;
                    }
                }

                int pos = elemC->positionVarEntiereCur_;
                bool to_display = config::configuration().showAllAngleTDTransitHVDC();
                if ((pos == -1) || (pbX_[pos] > 0.5)) { // Curatif active sur parade (ou pas de parade associee)

                    pos = elemC->positionVarCurative_;
                    if (pos != -1) {
                        const auto& td = std::dynamic_pointer_cast<ElementCuratifTD>(elemC)->td_;
                        string s = td->quadVrai_->nom_;

                        if (!config::inputConfiguration().useAllOutputs()
                            && !config::configuration().showAllAngleTDTransitHVDC()
                            && fabs(pbX_[pos] + pbX_[pos + 1]) < 0.01) {
                            continue; // epsilon specifique pour les TD (10-2)
                        }

                        double val = config::constants::valdef;
                        if (td->fictif_) {
                            val = pbX_[pos] - pbX_[pos + 1];
                        } else {
                            val = td->puiCons_ + pbX_[td->numVar_] - pbX_[td->numVar_ + 1] + pbX_[pos] - pbX_[pos + 1];
                        }

                        int priseFinale = config::constants::valdef;
                        if (td->type_ == TransformateurDephaseur::PILOTAGE_ANGLE_OPTIMISE
                            || td->type_ == TransformateurDephaseur::PILOTAGE_ANGLE_IMPOSE) {
                            val = td->power2Angle(val);

                            if (td->nbtap_ > 0) {
                                priseFinale = td->getClosestTapPosition(val) + td->lowtap_;
                            }
                        }
                        fprintf(fr,
                                "R5B ;%d;%s;%.2f;%d;\n",
                                incidentsContraignants.find(icdt)->second,
                                s.c_str(),
                                val,
                                priseFinale);
                        to_display = false;
                    }
                }

                if (to_display) {
                    auto td = std::dynamic_pointer_cast<ElementCuratifTD>(elemC)->td_;
                    fprintf(fr,
                            "R5B ;%d;%s;%.2f;%d;\n",
                            incidentsContraignants.find(icdt)->second,
                            td->quadVrai_->nom_.c_str(),
                            static_cast<double>(config::constants::valdef),
                            config::constants::valdef);
                }
            } // end for
        }     // end for

        // ecriture : R6B: HVDC en mode curatif
        //--------------
        fprintf(fr, "R6B ;INCIDENT;NOM HVDC;CONSIGNE;VM_CUR;\n");
        std::map<int, double> vmHvdc;

        for (const auto& elem : res_.incidents_) {
            const auto& icdt = elem.second;

            if (!icdt->validite_) {
                continue;
            }

            auto tmpListeElemCur = icdt->listeElemCur_;

            if (icdt->paradesActivees_) {
                for (const auto& parade : icdt->parades_) {
                    if ((parade->numVarActivation_ != -1) && (pbX_[parade->numVarActivation_] > 0.5)) {
                        tmpListeElemCur = parade->listeElemCur_;
                        break;
                    }
                }
            }

            for (const auto& elemC : tmpListeElemCur) {
                if (elemC->typeElem_ != ElementCuratif::HVDC) {
                    continue;
                }

                int pos = elemC->positionVarEntiereCur_;
                bool to_display = config::configuration().showAllAngleTDTransitHVDC();
                if ((pos == -1) || (pbX_[pos] > 0.5)) { // Curatif active sur parade (ou pas de parade associee)

                    pos = elemC->positionVarCurative_;
                    if (pos != -1) {
                        double val = config::constants::valdef;
                        double varMarg = 0;
                        const auto& lcc = std::dynamic_pointer_cast<ElementCuratifHVDC>(elemC)->lcc_;
                        string s = lcc->nom_;

                        if (!config::inputConfiguration().useAllOutputs()
                            && !config::configuration().showAllAngleTDTransitHVDC()
                            && fabs(pbX_[pos] + pbX_[pos + 1]) < EPSILON_SORTIES) {
                            continue;
                        }

                        val = lcc->puiCons_ + pbX_[lcc->numVar_] - pbX_[lcc->numVar_ + 1] + pbX_[pos] - pbX_[pos + 1];
                        if (fabs(val) < EPSILON_SORTIES) {
                            val = 0.0;
                        }

                        if (pbPositionDeLaVariable_[pos] == HORS_BASE_SUR_BORNE_SUP) {
                            varMarg = fabs(pbCoutsReduits_[pos]);
                            auto itVm = vmHvdc.find(lcc->num_);
                            if (itVm != vmHvdc.end()) {
                                if (varMarg > itVm->second) {
                                    vmHvdc[lcc->num_] = varMarg;
                                }
                            } else {
                                vmHvdc[lcc->num_] = varMarg;
                            }
                        } else if (pbPositionDeLaVariable_[pos + 1] == HORS_BASE_SUR_BORNE_SUP) {
                            varMarg = fabs(pbCoutsReduits_[pos + 1]);
                            auto itVm = vmHvdc.find(lcc->num_);
                            if (itVm != vmHvdc.end()) {
                                if (varMarg > itVm->second) {
                                    vmHvdc[lcc->num_] = varMarg;
                                }
                            } else {
                                vmHvdc[lcc->num_] = varMarg;
                            }
                        }
                        if (fabs(varMarg) < EPSILON_SORTIES) {
                            varMarg = -0.0;
                        }
                        std::string str("R6B ;%d;%s;");
                        str += PREC_FLOAT;
                        str += ";";
                        str += PREC_FLOAT;
                        str += ";\n";
                        fprintf(fr, str.c_str(), incidentsContraignants.find(icdt)->second, s.c_str(), val, -varMarg);
                        to_display = false;
                    }
                }

                if (to_display) {
                    const auto& lcc = std::dynamic_pointer_cast<ElementCuratifHVDC>(elemC)->lcc_;
                    std::string str("R6B ;%d;%s;");
                    str += PREC_FLOAT;
                    str += ";";
                    str += PREC_FLOAT;
                    str += ";\n";
                    fprintf(fr,
                            str.c_str(),
                            incidentsContraignants.find(icdt)->second,
                            lcc->nom_.c_str(),
                            config::constants::valdef,
                            0.0);
                }
            } // end for elemCur
        }     // end for incident


        // ecriture : R6.
        //--------------
        if (!config::inputConfiguration().useAllOutputs()) {
            fprintf(fr, "R6 ; PAR LCC;NOM;TRANSIT;VM_GLOBALE;\n");
        } else {
            fprintf(fr, "R6 ; PAR LCC;NOM;TRANSIT;VM_PREV;VM_GLOBALE;TRANSIT HR;\n");
        }
        double val;
        double varMargPrev;
        double tmpVal;
        int numVar;
        string nom;

        for (const auto& elem : res_.LigneCCs_) {
            const auto& ligne = elem.second;

            if (!config::inputConfiguration().useAllOutputs() && !config::configuration().showAllAngleTDTransitHVDC()
                && (ligne->type_ == LigneCC::HORS_SERVICE || ligne->type_ == LigneCC::PILOTAGE_PUISSANCE_IMPOSE)) {
                continue;
            }

            nom = ligne->nom_;
            numVar = ligne->numVar_;
            val = ligne->puiCons_ + pbX_[numVar] - pbX_[numVar + 1];

            if (ligne->isEmulationAC()) {
                const auto& quad = ligne->quadFictif_;
                tmpVal = val
                         + quad->u2Yij_
                               * (theta[quad->norqua_->num_] - theta[quad->nexqua_->num_]); // transit k*delta_theta
                fprintf(fr,
                        ("R3 ;;%s;" + PREC_FLOAT + ";%.1f;%.1f;%.1f;\n").c_str(),
                        nom.c_str(),
                        tmpVal,
                        ligne->puiMin_,
                        ligne->puiMax_,
                        ligne->puiCons_);

                if (!config::inputConfiguration().useAllOutputs() && ligne->type_ == LigneCC::PILOTAGE_EMULATION_AC) {
                    continue;
                }
            }

            // Ici on a une liaison avec pilotage optimise
            if (!config::inputConfiguration().useAllOutputs() && !config::configuration().showAllAngleTDTransitHVDC()
                && pbX_[numVar] < EPSILON_SORTIES && pbX_[numVar + 1] < EPSILON_SORTIES) {
                continue;
            }

            if (config::inputConfiguration().useAllOutputs() || config::configuration().useResVarMarHvdc()) {
                varMargPrev = 0.;
                if (pbPositionDeLaVariable_[numVar] == HORS_BASE_SUR_BORNE_SUP) {
                    varMargPrev = fabs(pbCoutsReduits_[numVar]);
                } else if (pbPositionDeLaVariable_[numVar + 1] == HORS_BASE_SUR_BORNE_SUP) {
                    varMargPrev = fabs(pbCoutsReduits_[numVar + 1]);
                }
                double varMargGlob = varMargPrev;
                auto itVm = vmHvdc.find(ligne->num_);
                if (itVm != vmHvdc.end() && varMargGlob < itVm->second) {
                    varMargGlob = itVm->second;
                }
                fprintf(fr, ("R6 ;;%s;" + PREC_FLOAT + ";%.3f;\n").c_str(), nom.c_str(), val, -varMargGlob);
                if (config::inputConfiguration().useAllOutputs()) {
                    auto valHR = ligne->puiCons_ + pbXhR_[numVar] - pbXhR_[numVar + 1];
                    fprintf(fr, "R6 ;;%s;%.1f;%.3f;%.3f;%.1f;\n", nom.c_str(), val, -varMargPrev, -varMargGlob, valHR);
                }
            } else {
                fprintf(fr, ("R6 ;;%s;" + PREC_FLOAT + ";;\n").c_str(), nom.c_str(), val);
            }
        }

        // ecriture : R7 (Redispatching par filiere)
        //--------------
        fprintf(fr, "R7 ;PAR FILIERE;TYPE;VOL BAISSE;VOL HAUSSE;VOL CUR BAISSE;VOL CUR HAUSSE;\n");
        double volGrp = 0.;
        double volGrpCur = 0.;
        for (int i = 0; i < res_.nbTypesGroupes_; ++i) {
            if (redispatchParTypeB[i] < EPSILON_SORTIES && redispatchParTypeH[i] < EPSILON_SORTIES
                && maxRedispCurParTypeB[i] < EPSILON_SORTIES && maxRedispCurParTypeH[i] < EPSILON_SORTIES) {
                continue;
            }

            volGrp += redispatchParTypeB[i] + redispatchParTypeH[i];
            volGrpCur += redispCurParType[i];

            fprintf(fr,
                    "R7 ;;%s;%s;%s;%s;%s;\n",
                    res_.typesGroupes_[i].c_str(),
                    (redispatchParTypeB[i] < EPSILON_SORTIES ? EMPTY_STRING
                                                             : c_fmt(PREC_FLOAT.c_str(), -redispatchParTypeB[i]))
                        .c_str(),
                    (redispatchParTypeH[i] < EPSILON_SORTIES ? EMPTY_STRING
                                                             : c_fmt(PREC_FLOAT.c_str(), redispatchParTypeH[i]))
                        .c_str(),
                    (maxRedispCurParTypeB[i] < EPSILON_SORTIES ? EMPTY_STRING
                                                               : c_fmt(PREC_FLOAT.c_str(), -maxRedispCurParTypeB[i]))
                        .c_str(),
                    (maxRedispCurParTypeH[i] < EPSILON_SORTIES ? EMPTY_STRING
                                                               : c_fmt(PREC_FLOAT.c_str(), maxRedispCurParTypeH[i]))
                        .c_str());
        }

        // ecriture : R8.
        //--------------
        fprintf(fr, "R8 ;PERTES;VOLUME CALCULE;TAUX UTILISE;\n");
        fprintf(fr, ("R8 ;;" + PREC_FLOAT + ";%.2f;\n").c_str(), pertesTotales_, tauxPertes_);

        // ecriture R8B : detail des pertes par region
        //--------------
        if (config::configuration().useResPertesDetail()) {
            fprintf(fr, "R8B ;PERTES;REGION;VOLUME CALCULE;\n");

            for (const auto& region : pertesParRegion) {
                fprintf(fr, ("R8B ;;%s;" + PREC_FLOAT + ";\n").c_str(), region.first.c_str(), region.second);
            }
        }

        // ecriture : R9.
        //--------------
        fprintf(fr,
                "R9 ;FCT OBJECTIF;COUT GROUPES;COUT DELESTAGE;VOLUME ECARTS N-k;VOLUME ECARTS N;COUT GRP CUR;COUT "
                "CONSO CUR;\n");
        string coutGrpCur;
        string coutConsoCur;

        // on ote les offsets dans les couts restitues pour les groupes
        double coutGrp = fonction_objectif_G_ - (volGrp * config::configuration().redispatchCostOffset());
        double coutGrpC = fonction_objectif_G_cur_sans_offset_;
        double coutConso = fonction_objectif_D_ - (volDel * config::configuration().redispatchCostOffset());
        double coutConsoC = fonction_objectif_D_cur_sans_offset_;

        if (coutGrpC >= EPSILON_SORTIES) {
            coutGrpCur = c_fmt(PREC_FLOAT.c_str(), coutGrpC);
        }

        if (fonction_objectif_D_cur_ >= EPSILON_SORTIES) {
            coutConsoCur = c_fmt(PREC_FLOAT.c_str(), coutConsoC);
        }

        fprintf(fr,
                ("R9 ;;" + PREC_FLOAT + ";" + PREC_FLOAT + ";" + PREC_FLOAT + ";" + PREC_FLOAT + ";%s;%s;\n").c_str(),
                coutGrp,
                coutConso,
                sommeEcartsNk_,
                sommeEcartsN,
                coutGrpCur.c_str(),
                coutConsoCur.c_str());

        // ecriture : R10
        //---------------
        fprintf(fr, "R10;INCIDENT;NOM INCIDENT;NB ACTIONS;ACTION;\n");


        // Resultats des parades
        auto incEtParIt = res_.incidentsEtParades_.cbegin();
        advance(incEtParIt, res_.nbIncidentsHorsParades_);

        for (; incEtParIt != res_.incidentsEtParades_.end(); ++incEtParIt) {
            const auto& icdt = *incEtParIt;

            if ((!icdt->parade_) || (!icdt->validite_)) {
                continue;
            }

            // Information sur la parade activee
            if ((icdt->numVarActivation_ != -1) && (pbX_[icdt->numVarActivation_] > 0.5)) {
                const auto& icdtPere = icdt->incTraiteCur_;
                if (!config::inputConfiguration().useAllOutputs() && icdt == *icdtPere->parades_.begin()) {
                    continue; // il n'y a que la parade ne rien faire
                }
                fprintf(fr,
                        "R10;%d;%s;%d;%s;\n",
                        incidentsContraignants.find(icdtPere)->second,
                        icdtPere->nom_.c_str(),
                        icdt->nbLignes_ + icdt->nbCouplagesFermes_ - icdtPere->nbLignes_,
                        icdt->nom_.c_str());
            }
        }

        // ecriture : C1. si pas de probleme
        //-------------
        fprintf(fr, "C1 ;COMPTE RENDU;CODE;\n");
        fprintf(fr, "C1 ;;%d;\n", status);
    } catch (const std::exception& e) {
        LOG_ALL(error) << e.what();
        fclose(fr);
        return -METRIX_PROBLEME;
    } catch (...) {
        LOG_ALL(error) << "exception inconnue";
        fclose(fr);
        return -METRIX_PROBLEME;
    }
    fclose(fr);
    return METRIX_PAS_PROBLEME;
}

boost::optional<std::tuple<std::string, int, std::shared_ptr<Incident>>>
Calculer::getIncidentConstraint(const std::map<std::shared_ptr<Incident>, int>& incidentsContraignants, int i) const
{
    int numIncident;
    if (pbTypeContrainte_[i] == COUPE_AUTRE || pbTypeContrainte_[i] == COUPE_BILAN) {
        return boost::none; // Uniquement pour les contraintes de transit
    }

    if (fabs(pbCoutsMarginauxDesContraintes_[i]) < config::constants::epsilon) {
        return boost::none; // Seuil de restitution des resultats
    }

    const auto& ctre = pbContraintes_[i];
    std::shared_ptr<Incident> icdt;
    if (pbTypeContrainte_[i] == COUPE_SURETE_N) {
        numIncident = 0;
    } else {
        if (ctre->icdt_->parade_) {
            icdt = ctre->icdt_->incTraiteCur_;
        } else {
            icdt = ctre->icdt_;
        }

        auto itNumIncident = incidentsContraignants.find(icdt);

        if (itNumIncident != incidentsContraignants.end()) {
            numIncident = itNumIncident->second;
        } else {
            LOG_ALL(warning) << "Erreur incident inconnu : " << icdt->nom_;
            return boost::none;
        }
    }

    return boost::make_optional(std::make_tuple(ctre->elemAS_->nom_, numIncident, icdt));
}

bool Calculer::calculVariationsMarginales(FILE* fr,
                                          const std::map<std::shared_ptr<Incident>, int>& incidentsContraignants)
{
    CostDefMap cost;
    int ctrOuvr = 0;
    vector<string> nomOu; // nom de l'ouvrage

    // Tableau R4
    fprintf(fr, "R4 ;VAR. MARGINALES;LIGNE;INCIDENT;VMAR;\n");
    int numIncident;
    vector<int> AskedDetailedConstraints;
    std::vector<int> constraintsToDelail;
    std::shared_ptr<Incident> icdt;
    for (int i = 0; i < pbNombreDeContraintes_; ++i) {
        auto opt_inc = getIncidentConstraint(incidentsContraignants, i);
        if (!opt_inc.is_initialized()) {
            continue;
        }
        string nom;
        std::tie(nom, numIncident, icdt) = (*opt_inc);

        auto mapIt = res_.variationsMarginalesDetaillees_.find(nom);
        if (mapIt != res_.variationsMarginalesDetaillees_.end()) {
            if (pbTypeContrainte_[i] == COUPE_SURETE_N || mapIt->second.find(icdt) != mapIt->second.end()) {
                AskedDetailedConstraints.push_back(i);
                constraintsToDelail.push_back(i);
            }
        } else if (config::configuration().redispatchCostOffset() > 0) {
            // that means that we have to compute R4B array for ALL margin variations in order to compute correctly R4
            constraintsToDelail.push_back(i);
        }
    }

    if (!constraintsToDelail.empty()) { // Variations marginales detaillees
        // calcul des variations marginales
        // construction de la variation des variables en base due a 1 MW de plus sur les contraintes actives
        int nbOu = pbNombreDeContraintes_ - pbNbVarDeBaseComplementaires_;

        vector<int> typeOu; // 0 : grp, 1: conso, 2 : td, 3 : LCC
        typeOu.reserve(nbOu);
        nomOu.reserve(nbOu);
        vector<int> numVa; // numero de la variable dans le vecteur pbX.
        numVa.reserve(nbOu);
        vector<double> coeffCoupe(nbOu);
        coeffCoupe.reserve(nbOu);
        vector<int> numVarEnBaseDansB(pbNombreDeVariables_, -1);


        int k;

        // recherche des variables en base de type groupe
        for (const auto& elem : res_.groupes_) {
            const auto& grp = elem.second;

            int g1 = grp->numVarGrp_;
            if (g1 == -1) {
                continue;
            }

            if (pbPositionDeLaVariable_[g1] == EN_BASE || pbPositionDeLaVariable_[g1 + 1] == EN_BASE) {
                typeOu.push_back(0); // 0 : grp, 1: conso, 2 : td, 3 : LCC
                nomOu.push_back(grp->nom_);
                numVa.push_back(pbPositionDeLaVariable_[g1] == EN_BASE ? g1 : g1 + 1);
                numVarEnBaseDansB[numVa[ctrOuvr]] = ctrOuvr;
                ctrOuvr++;
            }
        }

        // recherche des variables en base de type delestage
        int nbDelestageEnBase = 0;
        int numVar;
        for (const auto& elem : res_.consos_) {
            const auto& conso = elem.second;
            numVar = conso->numVarConso_;
            if (numVar >= 0) {
                if (pbPositionDeLaVariable_[numVar] == EN_BASE) {
                    typeOu.push_back(1);          // 0 : grp, 1: conso, 2 : td, 3 : LCC
                    nomOu.push_back(conso->nom_); // nom de la conso
                    numVa.push_back(numVar);
                    numVarEnBaseDansB[numVa[ctrOuvr]] = ctrOuvr;
                    ctrOuvr++;
                    nbDelestageEnBase++;
                }
            }
        }
        // recherche des variables en base de type transformateur dephaseur
        int deb = res_.nbVarGroupes_ + res_.nbVarConsos_;
        for (int i = 0; i < res_.nbTd_; ++i) {
            numVar = deb + 2 * i;

            for (k = 0; k < 2; ++k) { // Verification des variables x, x+ et x-
                if (pbPositionDeLaVariable_[numVar + k] == EN_BASE) {
                    typeOu.push_back(2);                                    // 0 : grp, 1: conso, 2 : td, 3 : LCC
                    nomOu.push_back(res_.tdParIndice_[i]->quadVrai_->nom_); // nom du td
                    numVa.push_back(numVar + k);
                    numVarEnBaseDansB[numVa[ctrOuvr]] = ctrOuvr;
                    ctrOuvr++;
                }
            }
        }
        // recherche des variables en base de type ligne a courant continu
        deb = res_.nbVarGroupes_ + res_.nbVarConsos_ + res_.nbVarTd_;
        for (int i = 0; i < res_.nbCC_; ++i) {
            numVar = deb + 2 * i;

            for (k = 0; k < 1; ++k) { // Verification des variables x, x+ et x-
                if (pbPositionDeLaVariable_[numVar + k] == EN_BASE) {
                    typeOu.push_back(3);                          // 0 : grp, 1: conso, 2 : td, 3 : LCC
                    nomOu.push_back(res_.lccParIndice_[i]->nom_); // numero du quad-1
                    numVa.push_back(numVar + k);
                    numVarEnBaseDansB[numVa[ctrOuvr]] = ctrOuvr;
                    ctrOuvr++;
                }
            }
        }
        // recherche des variables en base de type TD et HVDC curatif, activation curatif ou ecart
        deb = res_.nbVarGroupes_ + res_.nbVarConsos_ + res_.nbVarTd_ + res_.nbVarCc_;
        for (int i = deb; i < pbNombreDeVariables_; ++i) {
            numVar = i;
            if (pbPositionDeLaVariable_[numVar] == EN_BASE) {
                typeOu.push_back(4); // 0 : grp, 1: conso, 2 : td, 3 : LCC; 4 : curatif
                std::stringstream ss("Cur_");
                ss << numSupportEtat_[numVar];
                nomOu.push_back(ss.str());
                numVa.push_back(numVar);
                numVarEnBaseDansB[numVa[ctrOuvr]] = ctrOuvr;
                ctrOuvr++;
            }
        }

        // Construction de la matrice B : base du probleme  resolu
        //-------------------------------------------------------
        std::shared_ptr<MarginVariationMatrix> marginVariationMatrix;
        try {
            marginVariationMatrix = std::make_shared<MarginVariationMatrix>(pbNombreDeContraintes_,
                                                                            pbNbVarDeBaseComplementaires_,
                                                                            pbNombreDeVariables_,
                                                                            pbIndicesDebutDeLigne_,
                                                                            pbIndicesColonnes_,
                                                                            pbPositionDeLaVariable_,
                                                                            pbNombreDeTermesDesLignes_,
                                                                            numVarEnBaseDansB,
                                                                            pbCoefficientsDeLaMatriceDesContraintes_,
                                                                            pbComplementDeLaBase_,
                                                                            pbSens_);
        } catch (MarginVariationMatrix::Exception& e) {
            LOG(error) << e.what();
            LOG_ALL(warning) << " Le nombre le contraintes (" << pbNombreDeContraintes_
                             << ") est different de la taille de la base : " << e.info;

            fprintf(fr, "C1 ;COMPTE RENDU;CODE;\n");
            fprintf(fr, "C1 ;;%d;\n", METRIX_PROBLEME);
            return false;
        }

        // ecriture : R4 (variations marginale)
        //-------------------------------------
        if (!computeCosts(
                constraintsToDelail, marginVariationMatrix, incidentsContraignants, numVa, typeOu, ctrOuvr, cost)) {
            LOG_ALL(error) << "probleme lors du calcul des variables marginales detaillees : Base * x = b";
            fprintf(fr, "C1 ;COMPTE RENDU;CODE;\n");
            fprintf(fr, "C1 ;;%d;\n", METRIX_PROBLEME);
            return false;
        }
    }

    for (int i = 0; i < pbNombreDeContraintes_; ++i) {
        auto opt_inc = getIncidentConstraint(incidentsContraignants, i);
        if (!opt_inc.is_initialized()) {
            continue;
        }
        string nom;
        std::tie(nom, numIncident, std::ignore) = (*opt_inc);
        double costVal = (config::configuration().redispatchCostOffset() > 0)
                             ? std::accumulate(cost[i].begin(),
                                               cost[i].end(),
                                               0.0,
                                               [](const double& sum, const CostDef& def) { return sum + def.cost; })
                             : pbCoutsMarginauxDesContraintes_[i];
        fprintf(fr, "R4 ;;%s;%d;%.3f;\n", nom.c_str(), numIncident, costVal);
    }
    if (!AskedDetailedConstraints.empty()) {
        fprintf(fr, "R4B ;VAR. MARGINALES;LIGNE;INCIDENT;VMAR TYPVAR;NOMVAR;VOL;COUT;\n");
        for (int i : AskedDetailedConstraints) {
            for (int j = 0; j < ctrOuvr; ++j) {
                const auto& cost_def = cost[i][j];
                if (cost_def.skipDisplay) {
                    continue; // On n'affiche que les variations marginales significatives
                }
                if (cost_def.type.empty()) {
                    continue;
                }

                fprintf(fr,
                        "R4B ;;%s;%d;%s;%s;%7.5f;%7.5f;\n",
                        pbContraintes_[i]->elemAS_->nom_.c_str(),
                        cost_def.numIncident,
                        cost_def.type.c_str(),
                        nomOu[j].c_str(),
                        cost_def.varMW,
                        cost_def.cost);
            }
        }
    }

    return true;
}

bool Calculer::computeCosts(const std::vector<int>& constraintsToDelail,
                            const std::shared_ptr<MarginVariationMatrix>& marginVariationMatrix,
                            const std::map<std::shared_ptr<Incident>, int>& incidentsContraignants,
                            const std::vector<int>& numVa,
                            const std::vector<int>& typeOu,
                            int ctrOuvr,
                            std::map<int, std::vector<CostDef>>& cost) const
{
    vector<double> b(pbNombreDeContraintes_, 0.0);
    int numIncident;
    for (int i : constraintsToDelail) {
        cost[i] = std::vector<CostDef>(ctrOuvr);

        if (!findNumIncident(incidentsContraignants, i, numIncident)) {
            continue;
        }
        int new_i = marginVariationMatrix->convertConstraintIndex(i);
        b[new_i] = 1;
        // Call to klu solve: klu_solve(symbolic, numeric, n, 1, b, commonParam)
        klu_common common = marginVariationMatrix->cParameters();
        klu_solve(marginVariationMatrix->sMatrix(),
                  marginVariationMatrix->nMatrix(),
                  marginVariationMatrix->nzz(),
                  1,
                  b.data(),
                  &common);

        for (int j = 0; j < ctrOuvr; ++j) {
            double varMW = 0.;
            std::string type;

            int numVar = numVa[j];
            double linearCost = pbCoutLineaire_[numVar] - config::configuration().redispatchCostOffset();
            if (typeOu[j] == 0) { // grp
                type = "G";
                if (typeEtat_[numVar] == PROD_H) {
                    varMW = b[j];
                } else if (typeEtat_[numVar] == PROD_B) {
                    varMW = -b[j];
                } else {
                    LOG_ALL(warning) << "probleme lors de la restitution des variations marginales "
                                     << typeEtat_[numVar];
                }
            } else if (typeOu[j] == 1) { // consommation
                type = "N";
                if (typeEtat_[numVar] == CONSO_D) {
                    varMW = b[j];
                } else {
                    LOG_ALL(warning) << "probleme lors de la restitution des variations marginales "
                                     << typeEtat_[numVar];
                }

            } else if (typeOu[j] == 4) { // curatif
                if (typeEtat_[numVar] == GRP_CUR_H || typeEtat_[numVar] == GRP_CUR_B || typeEtat_[numVar] == CONSO_B
                    || typeEtat_[numVar] == CONSO_H || typeEtat_[numVar] == DEPH_CUR_B
                    || typeEtat_[numVar] == DEPH_CUR_H || typeEtat_[numVar] == HVDC_CUR_B
                    || typeEtat_[numVar] == HVDC_CUR_H) {
                    LOG_ALL(info) << "variation marginale sur curatif : " << numVar;
                } else if (typeEtat_[numVar] == VAR_ENT) {
                    LOG_ALL(info) << "variation marginale sur variable entiere : " << numVar;
                } else {
                    LOG_ALL(info) << "variation marginale sur un element curatif non attendu : " << typeEtat_[numVar];
                }
            }

            cost[i][j] = CostDef{type,
                                 numIncident,
                                 varMW,
                                 b[j] * linearCost,
                                 (fabs(b[j]) < config::constants::display_margin_variation_threshold)};
        }

        b.assign(pbNombreDeContraintes_, 0.);
    }

    return true;
}
