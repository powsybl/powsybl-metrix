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
#include "config/constants.h"
#include "cte.h"
#include "err/IoDico.h"
#include "err/error.h"
#include "pne.h"
#include "prototypes.h"
#include "status.h"
#include "variante.h"

#include <cmath>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>

using cte::c_fmt;


using std::max;
using std::min;
using std::ostringstream;
using std::string;
using std::vector;


//---------------------------------------------------------
// Methodes des differentes des classes du fichier Reseau.h
//---------------------------------------------------------
// REMARQUE : pour le detail des formules que nous utilisons,
//           consultez "MODERN POWER SYSTEMS CONTROL AND OPERATION (Atif S.Debs)"
int Calculer::calculInitCoefs(std::shared_ptr<Incident> icdt) const
{
    if (icdt->pochePerdue_) {
        if (icdt->pochePerdue_->pocheAvecConsoProd_) {
            return METRIX_PAS_PROBLEME; // les coefficients sont calculés spécifiquement
        }
        icdt = icdt->pochePerdue_->incidentModifie_;
    }

    icdt->rho_.resize(icdt->nbGroupes_ + icdt->nbLignes_ + icdt->nbCouplagesFermes_ + icdt->nbLccs_);
    // sensibilite aux variables
    icdt->lambda_.resize(icdt->nbLignes_ + icdt->nbCouplagesFermes_);

    for (size_t i = 0; i < icdt->rho_.size(); ++i) {
        icdt->rho_[i].resize(res_.nbQuads_, 0.0);
        if (i < icdt->lambda_.size()) {
            icdt->lambda_[i].resize(res_.nbNoeuds_, 0.0);
        }
    }
    return METRIX_PAS_PROBLEME;
}

int Calculer::resetCoefs(std::shared_ptr<Incident> icdt) const
{
    if (icdt->pochePerdue_) {
        icdt = icdt->pochePerdue_->incidentModifie_;
    }

    for (size_t i = 0; i < icdt->rho_.size(); ++i) {
        icdt->rho_[i].assign(res_.nbQuads_, 0.0);
        if (i < icdt->lambda_.size()) {
            icdt->lambda_[i].assign(res_.nbNoeuds_, 0.0);
        }
    }
    return METRIX_PAS_PROBLEME;
}

int Calculer::calculCoefsReport(std::shared_ptr<Incident> icdt)
{
    int codeRet;

    if (icdt->type_ == Incident::N_MOINS_K_GROUPE) {
        //"le N-K groupe n est pas traite dans cette fonction"
        return METRIX_PAS_PROBLEME;
    }

    if (icdt->type_ == Incident::N_MOINS_1_LIGNE
        || (icdt->type_ == Incident::N_MOINS_K_GROUPE_LIGNE && icdt->nbLignes_ == 1 && icdt->nbCouplagesFermes_ == 0)) {
        vector<double> g_mk(res_.nbNoeuds_, 0);
        // initialisation de la table des coefs de report du defaut
        if (icdt->rho_.empty()) {
            calculInitCoefs(icdt);
        }

        if (icdt->nbLignes_ == 1) {
            // calcul du vecteur g_mk= H^(-1) e_mk, m et k sont les numeros des noeuds
            // origine et extremite du quadripole qui declenche
            auto& qdt = icdt->listeQuads_[0];
            if (!qdt->connecte()) {
                LOG_ALL(warning) << err::ioDico().msg(
                    "WARNIncidentOuvrageDejaOuvert", c_fmt("%d", icdt->num_), qdt->nom_);
                icdt->validite_ = false;
                return METRIX_PAS_PROBLEME;
            }

            int m = qdt->tnnorqua();
            int k = qdt->tnnexqua();
            /* debut correction track-id #1568 : mise a 0 des noeuds bilan */
            // g_mk[m] = 1.0 ;
            // g_mk[k] = -1.0;
            if (!res_.noeuds_[m]->bilan_) {
                g_mk[m] = 1.0;
            }
            if (!res_.noeuds_[k]->bilan_) {
                g_mk[k] = -1.0;
            }
            /* fin correction */

            LU_LuSolv(jacFactorisee_, &g_mk[0], &codeRet, nullptr, 0, 0.0); // hij
            if (codeRet != 0) {
                LOG_ALL(error) << err::ioDico().msg("ERRCalcInterne") << " probleme lors de la resolution du systeme";
                return METRIX_PROBLEME;
            }

            double denominateur = (1.0 - qdt->u2Yij_ * (g_mk[m] - g_mk[k]));
            if (fabs(denominateur) <= config::constants::threshold_not_connex) {
                LOG_ALL(info) << err::ioDico().msg(
                                     "INFOIncidentRompantConnexite", "N-1", c_fmt("%d", icdt->num_), icdt->nom_)
                              << "  non connexe a cause du denominateur " << qdt->u2Yij_ << "  g_mk[m] " << g_mk[m]
                              << "  " << g_mk[k] << " denominateur " << denominateur << "  quad " << qdt->y_;
                icdt->validite_ = false;
                return METRIX_PAS_PROBLEME;
            }

            for (auto& quad : res_.quadsSurv_) {
                int ii = quad->tnnorqua();
                int jj = quad->tnnexqua();
                icdt->rho_[0][quad->num_] = quad->u2Yij_ /*-hij(ii,jj)*/ * (g_mk[ii] - g_mk[jj]) / denominateur;
            }

            icdt->rho_[0][qdt->num_] = 0; // mise a zero de l influence du quadripole sur lui-meme

            // derivee, ou sensibilite, de l incident par rapport aux injections nodales
            icdt->lambda_[0][qdt->norqua_->num_] = qdt->u2Yij_;
            icdt->lambda_[0][qdt->nexqua_->num_] = -qdt->u2Yij_;
            LU_LuSolv(jacFactorisee_, &icdt->lambda_[0][0], &codeRet, nullptr, 0, 0.0);

            // mise a zero des sensibilite aux noeuds bilan
            // cf. note Description generale Metrix pour justification
            for (auto& elem : res_.numNoeudBilanParZone_) {
                icdt->lambda_[0][elem.second] = 0;
            }
        }
    } else {
        // Remarque : cette partie calcule les coeffs de report de chaque incident
        //           de type N-K ligne sur les autres quadripoles
        //           g_mk[indice colonne : 1...icdt->nbLignes_][indice ligne : 1...res_.nbNoeuds_]
        //           invB[indice colonne : 1...icdt->nbLignes_][indice ligne : 1...icdt->nbLignes_]
        //           nous manipulerons B^T et non B

        auto& incidentInitial = icdt;
        if (icdt->pochePerdue_) {
            icdt = icdt->pochePerdue_->incidentModifie_;
        }

        int nbLignes = icdt->nbLignes_;
        int nbCouplages = icdt->nbCouplagesFermes_;
        int nbLignesAAnalyser = nbLignes + nbCouplages;

        if (nbLignesAAnalyser > 0) {
            int m;
            int k;
            int j;
            int i;
            int status;
            vector<std::shared_ptr<Quadripole>> tmpQuadInc(icdt->listeQuads_);

            if (nbCouplages > 0) {
                tmpQuadInc.reserve(nbLignesAAnalyser);
                copy(icdt->listeCouplagesFermes_.begin(), icdt->listeCouplagesFermes_.end(), back_inserter(tmpQuadInc));
            }

            vector<vector<double>> g_mk(nbLignesAAnalyser);
            vector<vector<double>> invB_T(nbLignesAAnalyser); // nous manipulerons B^T et non B

            // calcul de g
            for (i = 0; i < nbLignesAAnalyser; ++i) {
                g_mk[i].resize(res_.nbNoeuds_, 0);
                invB_T[i].resize(nbLignesAAnalyser, 0);
            }

            for (i = 0; i < nbLignes; ++i) {
                auto& qdt = icdt->listeQuads_[i];
                if (!qdt->connecte()) {
                    continue;
                }
                m = qdt->tnnorqua();
                k = qdt->tnnexqua();
                // correction track-id #1568
                // g_mk[i][m] = 1 ;
                // g_mk[i][k] = -1;
                if (!res_.noeuds_[m]->bilan_) {
                    g_mk[i][m] = 1.0;
                }
                if (!res_.noeuds_[k]->bilan_) {
                    g_mk[i][k] = -1.0;
                }
                LU_LuSolv(jacFactorisee_, &g_mk[i][0], &codeRet, nullptr, 0, 0.0);
            }

            // pour les couplages a fermer
            for (i = 0; i < nbCouplages; ++i) {
                auto& qdt = icdt->listeCouplagesFermes_[i];
                if (qdt->connecte()) {
                    continue;
                }
                m = qdt->tnnorqua();
                k = qdt->tnnexqua();
                j = nbLignes + i;
                if (!res_.noeuds_[m]->bilan_) {
                    g_mk[j][m] = -1.0;
                }
                if (!res_.noeuds_[k]->bilan_) {
                    g_mk[j][k] = 1.0;
                }
                LU_LuSolv(jacFactorisee_, &g_mk[j][0], &codeRet, nullptr, 0, 0.0);
            }

            // construction de la matrice B = (I+D^T A^(-1) C)^(-1)
            vector<int> BIndexDebutDesColonnes(nbLignesAAnalyser);
            vector<int> BNbTermesDesColonnes(nbLignesAAnalyser);
            vector<double> BValeurDesTermesDeLaMatrice(nbLignesAAnalyser * nbLignesAAnalyser);
            vector<int> BIndicesDeLigne(nbLignesAAnalyser * nbLignesAAnalyser);

            for (i = 0; i < nbLignesAAnalyser; ++i) {
                BIndexDebutDesColonnes[i] = nbLignesAAnalyser * i;
                BNbTermesDesColonnes[i] = nbLignesAAnalyser;
            }

            int indElm = -1;
            for (j = 0; j < nbLignesAAnalyser; ++j) { // parcours de la colonne
                auto& qdt = tmpQuadInc[j];

                for (i = 0; i < nbLignesAAnalyser; ++i) { // parcours de la ligne
                    indElm++;
                    auto& qdt2 = tmpQuadInc[i];

                    m = qdt2->tnnorqua();
                    k = qdt2->tnnexqua();

                    BIndicesDeLigne[indElm] = i;
                    BValeurDesTermesDeLaMatrice[indElm] = (g_mk[j][m] - g_mk[j][k]) * (-qdt->u2Yij_);

                    if (i == j) {
                        BValeurDesTermesDeLaMatrice[indElm] += 1.0 /*/qdt->u2Yij_*/;
                    }
                }
            }

            status = calculeInvB(icdt,
                                 BIndexDebutDesColonnes,
                                 BNbTermesDesColonnes,
                                 BValeurDesTermesDeLaMatrice,
                                 BIndicesDeLigne,
                                 nbLignesAAnalyser,
                                 invB_T);

            if (status != METRIX_PAS_PROBLEME) {
                LOG_ALL(error) << err::ioDico().msg("ERRCalcInterne") << " probleme dans : calculeInvB";
                return METRIX_PROBLEME;
            }

            if (!icdt->validite_) {
                incidentInitial->validite_ = false;
                return METRIX_PAS_PROBLEME;
            }

            // delta theta = coefsPhases * [delta theta_1;...; delta theta_k]
            // la variable coefsPhases est sauvgardee dans g_mk.
            status = coeffsRepportPhases(tmpQuadInc, nbLignesAAnalyser, invB_T, g_mk);
            if (status != METRIX_PAS_PROBLEME) {
                LOG_ALL(error) << err::ioDico().msg("ERRCalcInterne") << "probleme dans : coeffsRepportPhases";
                return METRIX_PROBLEME;
            }

            if (icdt->rho_.empty()) {
                calculInitCoefs(icdt);
            }

            // calcul de icdt->rho_[nbLignesAAnalyser][res_.nbQuads_]
            // delta Tij = rho * [delta T_1;...; delta T_nbLignesAAnalyser]
            double U2Y_ij;

            for (i = 0; i < nbLignesAAnalyser; ++i) {
                auto& qdt = tmpQuadInc[i];
                U2Y_ij = qdt->u2Yij_;
                // Remarque : je ne teste  pas si  U2Y_ij > Y_TRES_PETIT car c est deja fait plus haut

                for (auto& qdt2 : res_.quadsSurv_) {
                    icdt->rho_[i][qdt2->num_] = qdt2->u2Yij_
                                                * (g_mk[i][qdt2->norqua_->num_] - g_mk[i][qdt2->nexqua_->num_])
                                                / U2Y_ij;
                }

                // mise a zero de l influence des quadripoles du meme incident
                for (j = 0; j < nbLignes; ++j) {
                    icdt->rho_[i][icdt->listeQuads_[j]->num_] = 0.0;
                }

                // derivee, ou sensibilite, de l incident par rapport aux injections nodales
                icdt->lambda_[i][qdt->norqua_->num_] = qdt->u2Yij_;
                icdt->lambda_[i][qdt->nexqua_->num_] = -qdt->u2Yij_;
                LU_LuSolv(jacFactorisee_, &icdt->lambda_[i][0], &codeRet, nullptr, 0, 0.0);

                // mise a zero des sensibilite aux noeuds bilan
                // cf. note Description generale Metrix pour justification
                for (auto& elem : res_.numNoeudBilanParZone_) {
                    icdt->lambda_[i][elem.second] = 0;
                }
            }
        }
    }
    if (icdt->nbLccs_ > 0) {
        calculReportLcc(icdt);
    }

    return METRIX_PAS_PROBLEME;
}


int Calculer::calculReportLccs()
{
    // calcul pour chaque quad de sa sensibilite a la HVDC (a reseau complet)
    int codeRet = METRIX_PAS_PROBLEME;
    vector<double> theta(res_.nbNoeuds_, 0.0);

    for (auto& elem : res_.LigneCCs_) {
        auto& lcc = elem.second;

        if (!lcc->connecte()) {
            continue;
        }

        lcc->rho_.resize(res_.nbQuads_, 0.0);

        if (!lcc->norqua_->bilan_) {
            theta[lcc->norqua_->num_] = 1.0;
        }
        if (!lcc->nexqua_->bilan_) {
            theta[lcc->nexqua_->num_] = -1.0;
        }

        LU_LuSolv(jacFactorisee_, &theta[0], &codeRet, nullptr, 0, 0.0);

        // calcul des coefficients de report de la HVDC sur chacune des lignes
        for (auto& quad : res_.quadsSurv_) {
            lcc->rho_[quad->num_] = quad->u2Yij_ * (theta[quad->tnnorqua()] - theta[quad->tnnexqua()]);
        }

        // remise a zero
        theta.assign(res_.nbNoeuds_, 0.0);
    }

    return codeRet;
}

int Calculer::calculReportLcc(const std::shared_ptr<Incident>& inc) const
{
    // Calcul de sensibilite de toutes les lignes à la HVDC sur des incidents donnees
    int debut = inc->nbLignes_ + inc->nbCouplagesFermes_ + inc->nbGroupes_;

    for (int u = 0; u < inc->nbLccs_; ++u) {
        auto& lcc = inc->listeLccs_[u];

        if (!lcc->connecte()) {
            continue;
        }

        for (auto& quad : res_.quadsSurv_) {
            int i = quad->num_;
            inc->rho_[debut + u][i] = lcc->rho_[i];

            // le coeffcient de report de la HVDC pour l'incident tient compte de l'aggration avec les coeffcients de
            // defauts lignes si necessaire
            for (int l = 0; l < inc->nbLignes_; ++l) {
                double rhoSecondOrdre = inc->rho_[l][i] * lcc->rho_[inc->listeQuads_[l]->num_];
                inc->rho_[debut + u][i] += rhoSecondOrdre;
            }
            for (int l = 0; l < inc->nbCouplagesFermes_; ++l) {
                double rhoSecondOrdre = inc->rho_[inc->nbLignes_ + l][i]
                                        * lcc->rho_[inc->listeCouplagesFermes_[l]->num_];
                inc->rho_[debut + u][i] += rhoSecondOrdre;
            }
        }
    }
    return METRIX_PAS_PROBLEME;
}

int Calculer::coeffsRepportPhases(const vector<std::shared_ptr<Quadripole>>& listeQuads,
                                  int nbColonnes,
                                  const vector<vector<double>>& invB_T,
                                  vector<vector<double>>& g_mk) const
{
    // Remarque : g_mk[indice colonne : 1...icdt->nbLignes_][indice ligne : 1...res_.nbNoeuds_]
    //           invB[indice colonne : 1...icdt->nbLignes_][indice ligne : 1...icdt->nbLignes_]
    //           coefsPhases[indice colonne : 1...icdt->nbLignes_][indice ligne : 1...res_.nbNoeuds_]

    vector<vector<double>> coefsPhases(nbColonnes);

    for (int i = 0; i < nbColonnes; ++i) {
        coefsPhases[i].resize(res_.nbNoeuds_, 0);
    }

    for (int j = 0; j < nbColonnes; ++j) {
        for (int l = 0; l < res_.nbNoeuds_; ++l) {
            for (int i = 0; i < nbColonnes; ++i) {
                coefsPhases[j][l] += g_mk[i][l] * invB_T[j][i] * (-listeQuads[i]->u2Yij_);
            }
        }
    }

    for (int i = 0; i < res_.nbNoeuds_; ++i) {
        for (int j = 0; j < nbColonnes; ++j) {
            g_mk[j][i] = -coefsPhases[j][i];
        }
    }

    return METRIX_PAS_PROBLEME;
}

double Calculer::hij(int lig, int col)
{
    int deblig = jacIndexDebutDesColonnes_[lig];
    int finlig = jacNbTermesDesColonnes_[lig] + jacIndexDebutDesColonnes_[lig];
    for (int i = deblig; i < finlig; ++i) {
        if (jacIndicesDeLigne_[i] == col) {
            return jacValeurDesTermesDeLaMatrice_[i];
        }
    }
    return 0;
}
int Calculer::calculeInvB(const std::shared_ptr<Incident>& icdt,
                          vector<int>& BIndexDebutDesColonnes,
                          vector<int>& BNbTermesDesColonnes,
                          vector<double>& BValeurDesTermesDeLaMatrice,
                          vector<int>& BIndicesDeLigne,
                          int nbColones,
                          vector<vector<double>>& invB) const
{
    MATRICE_A_FACTORISER B;
    MATRICE* Bptr;

    B.NombreDeColonnes = nbColones;
    B.UtiliserLesSuperLignes = NON_LU;
    B.ContexteDeLaFactorisation = LU_GENERAL;
    B.FaireScalingDeLaMatrice = NON_LU;
    B.UtiliserLesValeursDePivotNulParDefaut = OUI_LU;
    B.LaMatriceEstSymetrique = NON_LU;
    B.LaMatriceEstSymetriqueEnStructure = NON_LU;
    B.FaireDuPivotageDiagonal = OUI_LU;
    B.SeuilPivotMarkowitzParDefaut = OUI_LU;
    B.IndexDebutDesColonnes = &BIndexDebutDesColonnes[0];
    B.NbTermesDesColonnes = &BNbTermesDesColonnes[0];
    B.ValeurDesTermesDeLaMatrice = &BValeurDesTermesDeLaMatrice[0];
    B.IndicesDeLigne = &BIndicesDeLigne[0];
    Bptr = LU_Factorisation(&B);
    if (B.ProblemeDeFactorisation != NON_LU) {
        icdt->validite_ = false;
        LOG_ALL(error) << err::ioDico().msg("WARNIncidentRompantConnexiteLU", icdt->nom_);
        return METRIX_PAS_PROBLEME;
    }

    // ATTENTION A verifier ProblemeDeFactorisation;  Le code retour ( NON_LU si tout s'est bien passe )
    int codeRet = OUI_LU;
    for (int i = 0; i < nbColones; ++i) {
        invB[i][i] = 1;
        LU_LuSolv(Bptr, &invB[i][0], &codeRet, nullptr, 0, 0.0);
        if (codeRet != NON_LU) {
            return METRIX_PROBLEME;
        }
    }
    LU_LibererMemoireLU(Bptr);
    return METRIX_PAS_PROBLEME;
}

void Calculer::comput_ParticipationGrp(const std::shared_ptr<Incident>& icdt) const
{
    // true si groupe qui participe a la compensation (1- connecte 2- Pmax diff Pmin 3- Pas N-k, non impose)
    //     sinon false
    vector<bool> Atraiter(res_.nbGroupes_, true);

    // sum2 : somme max des puissances perdues suite a l'incident N-k groupe
    double sumBandGrpNmoinsK = 0;
    for (int j = 0; j < icdt->nbGroupes_; ++j) {
        auto& grpe = icdt->listeGroupes_[j];
        if (grpe->prodAjust_ == Groupe::OUI_HR_AR || grpe->prodAjust_ == Groupe::OUI_HR) {
            sumBandGrpNmoinsK += icdt->listeGroupes_[j]->puisMaxDispo_ - icdt->listeGroupes_[j]->puisMax_;
        }
        Atraiter[icdt->listeGroupes_[j]->num_] = false;
    }

    double ceffMiseAJourDemiBand = 0;
    ceffMiseAJourDemiBand = (res_.DBandeRegGlobale_ - sumBandGrpNmoinsK) /* / res_.DBandeRegGlobale_*/;

    for (auto& grpe : res_.groupesEOD_) {
        grpe->participation_ = 0.;

        if (grpe->puisMax_ == grpe->puisMin_ || !grpe->etat_) {
            Atraiter[grpe->num_] = false;
        }

        if (Atraiter[grpe->num_]) { // ne sont pas traites (i) les gpes perdues lors de l incident, (ii) imposees et
                                    // (iii) deconnecte.
            grpe->participation_ = (grpe->puisMaxDispo_ - grpe->puisMax_) / ceffMiseAJourDemiBand;
        }
    }
}

int Calculer::calculCoefsInfluencement(const std::shared_ptr<Incident>& icdt)
{
    // calculer les coeff. d'influencement des groupes sur les lignes
    // = l'impact sur les flux de chaque ligne de la perte de 1 MW sur le groupe en incident
    //   et sa compensation par les autres groupes
    // Ce Calcul se fait en plusieurs etapes :
    // 1- calculer la reserve maxmimale disponible lors de l'incident : sum demiBande_
    // 2- distribuer la capacitee de compenser la perte de puissance au prorata de demiBande_

    if (icdt->type_ == Incident::N_MOINS_1_LIGNE) {
        return METRIX_PAS_PROBLEME;
    }

    if (icdt->nbGroupes_ == 0) {
        return METRIX_PAS_PROBLEME;
    }

    if (icdt->pochePerdue_) {
        LOG_ALL(error) << err::ioDico().msg("ERRIncidentGrpRompantConnexite");
        return METRIX_PROBLEME;
    }

    // sum2 : somme max des puissances perdues suite a l'incident N-k groupe
    double sum2 = 0.0;

    for (int j = 0; j < icdt->nbGroupes_; ++j) {
        sum2 += icdt->listeGroupes_[j]->puisMax_;
    }

    if (sum2 > res_.DBandeRegGlobale_) {
        LOG_ALL(error) << err::ioDico().msg("ERRInciGrpDemiBandeRegl",
                                            c_fmt("%d", icdt->num_),
                                            c_fmt("%s", icdt->listeGroupes_[0]->nom_.c_str()))
                       << err::ioDico().msg(
                              "ERRIDemiBandeReglInsuf", c_fmt("%10.1f", sum2), c_fmt("%10.1f", res_.DBandeRegGlobale_));
        return METRIX_PROBLEME;
    }

    if (fabs(sum2) < config::constants::zero_power) {
        LOG_ALL(warning) << err::ioDico().msg("WARNIncidentGrpPasDePuissance", c_fmt("%d", icdt->num_));
        icdt->rho_[icdt->nbLignes_].assign(res_.nbQuads_, 0.0);
        return METRIX_PAS_PROBLEME;
    }

    comput_ParticipationGrp(icdt);

    vector<double> inj(res_.nbNoeuds_, 0.0);
    vector<double> theta;
    // traitement des gpes perdues lors de l incident
    // coeffUnitaire permet d equilibrer la compensation lors du calcul de icdt->rho_

    for (auto grpIt = res_.groupes_.cbegin(); grpIt != res_.groupes_.end(); ++grpIt) {
        auto& grpe = grpIt->second;
        if (!grpe->noeud_->bilan_) { // Le dernier noeud correspond au noeud bilan
            inj[grpe->noeud_->num_] -= grpe->participation_;
        }
    }


    int codeRet;
    for (int j = 0; j < icdt->nbGroupes_; ++j) {
        auto& grpe = icdt->listeGroupes_[j];

        if (!grpe->etat_) {
            for (int i = 0; i < res_.nbQuads_; ++i) {
                icdt->rho_[icdt->nbLignes_ + j][i] = 0;
            }
            continue;
        }

        if (!grpe->noeud_->bilan_) { // Le dernier noeud correspond au noeud bilan
            inj[grpe->noeud_->num_] += 1.0;
        }

        // for (int k = 0; k < res_.nbNoeuds_; ++k) theta[k] = inj[k];
        theta = inj;

        LU_LuSolv(jacFactorisee_, &theta[0], &codeRet, nullptr, 0, 0.0);

        for (auto& quad : res_.quadsSurv_) {
            icdt->rho_[icdt->nbLignes_ + j][quad->num_] = quad->u2Yij_
                                                          * (theta[quad->tnnorqua()] - theta[quad->tnnexqua()]);
        }

        if (!grpe->noeud_->bilan_) {
            inj[grpe->noeud_->num_] += -1.0;
        }
    }

    return METRIX_PAS_PROBLEME;
}


int Calculer::calculCoeffReportTD()
{
    int codeRet = METRIX_PAS_PROBLEME;

    if (res_.nbTdCuratif_ + res_.nbCCEmulAC_ > 0) {
        // Calcul des coeff de report pour les TD
        vector<double> thetaTD(res_.nbNoeuds_, 0.0);

        for (auto& elem : res_.TransfoDephaseurs_) {
            auto& td = elem.second;

            if (td->mode_ != CURATIF_POSSIBLE) {
                continue;
            }

            td->rho_.resize(res_.nbQuads_, 0.0);

            if (!td->quad_->norqua_->bilan_) {
                thetaTD[td->quad_->norqua_->num_] = 1.0;
            }
            if (!td->quad_->nexqua_->bilan_) {
                thetaTD[td->quad_->nexqua_->num_] = -1.0;
            }

            LU_LuSolv(jacFactorisee_, &thetaTD[0], &codeRet, nullptr, 0, 0.0);

            for (auto& quad : res_.quadsSurv_) {
                td->rho_[quad->num_] = quad->u2Yij_ * (thetaTD[quad->tnnorqua()] - thetaTD[quad->tnnexqua()]);
            }

            // remise a zero
            thetaTD.assign(res_.nbNoeuds_, 0.0);
        }
    }
    return METRIX_PAS_PROBLEME;
}

int Calculer::calculReportGroupesEtConsos()
{
    // calcul pour chaque quad de sa sensibilite a une variation d'EOD sur le noeud (a reseau complet)
    int codeRet = METRIX_PAS_PROBLEME;

    // permet de savoir si on a déjà calculé les coefficient pour ce noeud
    vector<bool> noeudCalcules;
    vector<double> theta;
    int numNoeud;
    int cpt = 0;
    if (res_.nbGroupesCuratifs_ > 0 || res_.nbConsosCuratifs_ > 0) {
        noeudCalcules.resize(res_.nbNoeuds_, false);
    }

    for (auto grpIt = res_.groupes_.cbegin(); grpIt != res_.groupes_.end() && cpt < res_.nbGroupesCuratifs_; ++grpIt) {
        auto& grp = grpIt->second;

        if (grp->incidentsAtraiterCuratif_.empty()) {
            continue;
        }
        cpt++;

        numNoeud = grp->numNoeud_;
        if (noeudCalcules[numNoeud]) {
            continue;
        }
        noeudCalcules[numNoeud] = true;

        grp->noeud_->rho_.resize(res_.nbQuads_, 0.0);
        theta.assign(res_.nbNoeuds_, 0.0);

        if (!grp->noeud_->bilan_) {
            theta[numNoeud] = 1.0;
        }

        LU_LuSolv(jacFactorisee_, &theta[0], &codeRet, nullptr, 0, 0.0);

        if (codeRet != METRIX_PAS_PROBLEME) {
            return codeRet;
        }

        // calcul des coefficients de report du groupe sur chacune des lignes
        for (auto& quad : res_.quadsSurv_) {
            grp->noeud_->rho_[quad->num_] = quad->u2Yij_ * (theta[quad->tnnorqua()] - theta[quad->tnnexqua()]);
        }
    }

    cpt = 0;
    for (auto& elem : res_.consos_) {
        auto& conso = elem.second;

        if (conso->incidentsAtraiterCuratif_.empty()) {
            continue;
        }
        cpt++;

        numNoeud = conso->noeud_->num_;
        if (noeudCalcules[numNoeud]) {
            continue;
        }
        noeudCalcules[numNoeud] = true;

        conso->noeud_->rho_.resize(res_.nbQuads_, 0.0);
        theta.assign(res_.nbNoeuds_, 0.0);

        if (!conso->noeud_->bilan_) {
            theta[numNoeud] = 1.0;
        }

        LU_LuSolv(jacFactorisee_, &theta[0], &codeRet, nullptr, 0, 0.0);

        // calcul des coefficients de report du groupe sur chacune des lignes
        for (auto& quad : res_.quadsSurv_) {
            conso->noeud_->rho_[quad->num_] = quad->u2Yij_ * (theta[quad->tnnorqua()] - theta[quad->tnnexqua()]);
        }
    }
    return codeRet;
}


void Calculer::resetCoeffQuadsN() const
{
    for (auto& elem : res_.quadsSurv_) {
        elem->coeffN_.resize(0);
    }
}
