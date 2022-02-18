//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#include "parametres.h"

#include <algorithm>
#include <cmath>
#include <cstdio>
#include <iomanip>
#include <iostream>
#include <set>
#include <sstream>
#include <string>
#include <vector>
/* Prototypes des fonctions */
#include "config/configuration.h"
#include "cte.h"
#include "err/IoDico.h"
#include "err/error.h"
#include "reseau.h"
#include "status.h"

using cte::c_fmt;


using std::fixed;
using std::ifstream;
using std::ios;
using std::map;
using std::max;
using std::min;
using std::ostringstream;
using std::set;
using std::setprecision;
using std::setw;
using std::string;
using std::vector;

//----------------------------------
// algorithme de calcul de connexite
//----------------------------------
int Reseau::fusionner(int nZone1, int nZone2, map<int, int>& mapZones)
{
    /* fusionner les deux zones*/
    if (nZone1 == -1 || nZone2 == -1) {
        LOG_ALL(warning) << "probleme lors de la fusion";
    }

    auto iZone1 = mapZones.find(nZone1);
    auto iZone2 = mapZones.find(nZone2);

    if (iZone1 != mapZones.end()) {
        nZone1 = mapZone(nZone1, mapZones);
    }
    if (iZone2 != mapZones.end()) {
        nZone2 = mapZone(nZone2, mapZones);
    }

    if (nZone1 != nZone2) {
        if (nZone1 < nZone2) {
            mapZones[nZone2] = nZone1;
        } else if (nZone1 > nZone2) {
            mapZones[nZone1] = nZone2;
        }
    }
    return METRIX_PAS_PROBLEME;
}

int Reseau::inclure(int nNoeud, int nZone, vector<int>& numZones)
{
    /* inclure le noeud dans la zone nZone */
    if (nZone < 0) {
        LOG_ALL(warning) << "probleme pour inclure le noeud : " << noeuds_[nNoeud]->print();
    }
    numZones[nNoeud] = nZone;

    return METRIX_PAS_PROBLEME;
}

int Reseau::creer(int nNoeud1, int nNoeud2, vector<int>& numZones, int nZoneCourant)
{
    /* creer un nouvelle zone est inclure les deux noeuds dans cette zone*/
    int nZoneNew = nZoneCourant + 1;
    numZones[nNoeud1] = nZoneNew;
    numZones[nNoeud2] = nZoneNew;
    return nZoneNew;
}

int Reseau::traiterConnexion(
    int nNoeudOrg, int nNoeudExt, vector<int>& numZones, map<int, int>& mapZones, int numZoneCourant)
{
    int nZoneOrg = numZones[nNoeudOrg];
    int nZoneExt = numZones[nNoeudExt];

    if (nZoneOrg > 0 && nZoneExt > 0) {
        if (nZoneOrg == nZoneExt) {
            return numZoneCourant;
        }
        fusionner(nZoneOrg, nZoneExt, mapZones);
    } else if (nZoneOrg < 0 && nZoneExt > 0) {
        inclure(nNoeudOrg, nZoneExt, numZones);
    } else if (nZoneOrg > 0 && nZoneExt < 0) {
        inclure(nNoeudExt, nZoneOrg, numZones);
    } else {
        numZoneCourant = creer(nNoeudOrg, nNoeudExt, numZones, numZoneCourant);
    }

    return numZoneCourant;
}

/** Fonction recursive qui recherche le plus petit identifiant d'une serie de zone connexes */
int Reseau::mapZone(int zone, map<int, int>& mapZones)
{
    map<int, int>::iterator zoneIt;
    if ((zoneIt = mapZones.find(zone)) == mapZones.end()) {
        return zone;
    }

    int newZone = mapZone(zoneIt->second, mapZones);
    mapZones[zone] = newZone;
    // cout<< "Zone " << zone << ", id = " << newZone <<endl;
    return newZone;
}

std::tuple<bool, bool, std::vector<int>> Reseau::checkConnexite(bool choisiNoeudsBilan)
{
    vector<int> numZones(nbNoeuds_, -1);
    map<int, int> mapZones;
    int nNoeudOrg;
    int nNoeudExt;
    int numZoneCourant = 0;

    for (const auto& quad_pair : quads_) {
        const auto& quad = quad_pair.second;

        if (!quad->connecte()) {
            continue;
        }

        nNoeudOrg = quad->tnnorqua();
        nNoeudExt = quad->tnnexqua();
        numZoneCourant = traiterConnexion(nNoeudOrg, nNoeudExt, numZones, mapZones, numZoneCourant);
    }

    for (int n = 0; n < nbNoeuds_; ++n) {
        numZones[n] = mapZone(numZones[n], mapZones);
    }

    map<int, int> lienNumConnexiteEtNumeroZones;
    map<int, int>::const_iterator itNB;
    int nbZonesSynch = 0;
    if (choisiNoeudsBilan) {
        numNoeudBilanParZone_[nbZonesSynch] = 0;
        noeuds_[0]->bilan_ = true;
        lienNumConnexiteEtNumeroZones[numZones[0]] = nbZonesSynch;
    }

    if (nbCC_ > 0) {
        if (choisiNoeudsBilan) {
            for (int n = 1; n < nbNoeuds_; ++n) {
                int numZoneSynch = -1;
                if ((itNB = lienNumConnexiteEtNumeroZones.find(numZones[n])) == lienNumConnexiteEtNumeroZones.end()) {
                    lienNumConnexiteEtNumeroZones[numZones[n]] = ++nbZonesSynch;
                    numNoeudBilanParZone_[nbZonesSynch] = n;
                    noeuds_[n]->bilan_ = true;
                    numZoneSynch = nbZonesSynch;
                } else {
                    numZoneSynch = itNB->second;
                }

                noeuds_[n]->numCompSynch_ = numZoneSynch;
            }
        }

        // est-ce les HVDC permettent la connexite
        std::shared_ptr<LigneCC> lcc;

        for (const auto& lccPair : LigneCCs_) {
            lcc = lccPair.second;

            if (!lcc->connecte()) {
                continue;
            }

            nNoeudOrg = lcc->norqua_->num_;
            nNoeudExt = lcc->nexqua_->num_;

            if (numZones[nNoeudOrg] != numZones[nNoeudExt]) {
                if (lcc->mode_ == CURATIF_POSSIBLE) {
                    lcc->mode_ = PREVENTIF_SEUL;
                    LOG_ALL(warning) << err::ioDico().msg("WARNHVDCCurNonTraiteEntreCompSyn", lcc->nom_);
                }
                if (lcc->isEmulationAC()) {
                    LOG_ALL(error) << err::ioDico().msg("ERRHVDCEmulACEntreCompSyn", lcc->nom_);
                    return std::make_tuple(false, false, std::vector<int>{});
                }
            }
            numZoneCourant = traiterConnexion(nNoeudOrg, nNoeudExt, numZones, mapZones, numZoneCourant);
        }

        for (int n = 0; n < nbNoeuds_; ++n) {
            numZones[n] = mapZone(numZones[n], mapZones);
        }
    }

    // test de la connexite
    for (int n = 1; n < nbNoeuds_; ++n) {
        if (numZones[n] != numZones[0]) {
            return std::make_tuple(false, true, numZones);
        }
    }

    return std::make_tuple(true, true, numZones);
}

void Reseau::correctConnexite(bool was_connexe,
                              const std::shared_ptr<Incident>& icdt,
                              const std::vector<int>& numZones,
                              bool detailsNonConnexite)
{
    if (was_connexe) {
        if (icdt && icdt->parade_) {
            const auto& icdtPere = icdt->incTraiteCur_;
            if (icdtPere->pochePerdue_) {
                // The remedial action doesn't break connectedness
                LOG(debug) << "La poche de l'incident '" << icdtPere->nom_ << "' est recuperable par la parade : '"
                           << icdt->nom_ << "'";
                icdtPere->pocheRecuperableEncuratif_ = true;
            }
        }
    } else {
        if (!detailsNonConnexite) {
            icdt->validite_ = false;
        } else {
            // 1. We assess the number of nodes in each connected zone
            map<int, int> nombreDeNoeudsParZones;
            map<int, int>::const_iterator itN;
            for (int n = 0; n < nbNoeuds_; ++n) {
                itN = nombreDeNoeudsParZones.find(numZones[n]);
                if (itN == nombreDeNoeudsParZones.end()) {
                    nombreDeNoeudsParZones[numZones[n]] = 1;
                } else {
                    nombreDeNoeudsParZones[numZones[n]]++;
                }
            }

            // 2. Assessment of the main connected component :
            // we get the connected zone that contains the greater number of nodes and we get its connected zone number
            // and number of nodes
            int numZonePrincipale = -1;
            int nombreDeNoeuds = 0; // de la composante principale
            for (itN = nombreDeNoeudsParZones.cbegin(); itN != nombreDeNoeudsParZones.cend(); ++itN) {
                if (itN->second > nombreDeNoeuds) {
                    numZonePrincipale = itN->first;
                    nombreDeNoeuds = itN->second;
                }
            }


            // 3. Treatment of nodes that don't belong to the main connected zone:
            // either we print the nodes that don't belong to the main component in N case
            // either we store the nodes out of the main component and their component number for the given contingency
            // (N-k)
            map<std::shared_ptr<Noeud>, int> listeNoeuds;
            for (int n = 0; n < nbNoeuds_; ++n) {
                if (numZones[n] != numZonePrincipale) {
                    if (icdt) {
                        listeNoeuds[noeuds_[n]] = numZones[n];
                    }
                    if (!icdt) {
                        LOG_ALL(info) << err::ioDico().msg(
                            "INFONoeudHorsCCPrincipale", noeuds_[n]->print(), c_fmt("%d", numZones[n]));
                    }
                }
            }

            // 4. Management of contingencies and remedial actions of the zones that aren't in the main connected zone:
            // 4.a. we create the lost load object from the list of nodes that are out of main connected zone and that
            // were previously calculated for the given contingency
            // 4.b. Then if the contingency or the remedial action is still valid after the lost load creation,
            // we add it to the contingency list and remedial actions that break connectedness
            // 4.c Then:
            // - if it is a remedial action, it becomes invalid if it worsen the connnectedness
            // else we consider the contingency as recoverable by the remedial action
            // - if it is a contingency we print the nodes of the lost load.
            if (icdt) {
                icdt->pochePerdue_ = std::make_shared<PochePerdue>(icdt, listeNoeuds);

                if (icdt->validite_) { // it could have become invalid when the lost load was created

                    if (icdt->parade_) {
                        const auto& icdtPere = icdt->incTraiteCur_;

                        if (icdtPere->pochePerdue_) {
                            // The parent contingency breaks the connectedness, the RA shouldn't worsen it
                            if (icdt->pochePerdue_->noeudsPoche_.size() < icdtPere->pochePerdue_->noeudsPoche_.size()) {
                                LOG(debug) << "La poche de l'incident '" << icdtPere->nom_
                                           << "' est partiellement recuperable par la parade : '" << icdt->nom_ << "'";
                                icdtPere->pocheRecuperableEncuratif_ = true;
                            } else if (icdt->pochePerdue_->noeudsPoche_ != icdtPere->pochePerdue_->noeudsPoche_) {
                                icdt->validite_ = false;
                                return;
                            }
                        } else if (!config::configuration().useParRompantConnexite()) {
                            // The parent contingency doesn't break connectedness and the RA shouldn't too.
                            icdt->validite_ = false;
                            return;
                        }
                        LOG_ALL(info) << err::ioDico().msg("INFOParade", icdt->nom_, icdtPere->nom_)
                                      << icdt->pochePerdue_->print().c_str();
                    } else {
                        // This is not a remedial action
                        LOG_ALL(info) << err::ioDico().msg("INFOIncident", icdt->nom_)
                                      << icdt->pochePerdue_->print().c_str();
                    }
                    incidentsRompantConnexite_.push_back(icdt);
                }
            }
        }
    }
}

bool Reseau::connexite() { return connexite(nullptr, true, true); }

bool Reseau::connexite(const std::shared_ptr<Incident>& icdt, bool choisiNoeudsBilan, bool detailsNonConnexite)
{
    /*connectedness */
    bool resultat = true;
    bool status = true; // OK
    int n;

    std::shared_ptr<Quadripole> quad;
    map<std::shared_ptr<Quadripole>, bool> etatsInitiaux;

    if (icdt) {
        if (icdt->pochePerdue_) { /* We delete a potential previous lost load  */
            icdt->pochePerdue_.reset();
            icdt->pochePerdue_ = nullptr;
            icdt->pocheRecuperableEncuratif_ = false;
        }

        for (n = 0; n < icdt->nbLignes_; ++n) {
            quad = icdt->listeQuads_[n];
            etatsInitiaux[quad] = quad->connecte();
            quad->etatOr_ = false;
            quad->etatEx_ = false;
        }
        for (n = 0; n < icdt->nbCouplagesFermes_; ++n) {
            quad = icdt->listeCouplagesFermes_[n];
            etatsInitiaux[quad] = quad->connecte();
            quad->etatOr_ = true;
            quad->etatEx_ = true;
        }
        for (n = 0; n < icdt->nbLccs_; ++n) {
            icdt->listeLccs_[n]->etatOr_ = false;
            icdt->listeLccs_[n]->etatEx_ = false;
        }
    }

    std::vector<int> numZones;
    std::tie(resultat, status, numZones) = checkConnexite(choisiNoeudsBilan);
    if (!status) {
        return false;
    }

    // Network state restoration
    std::shared_ptr<LigneCC> tmpHvdcInc;
    bool etatInit;
    if (icdt) {
        for (n = 0; n < icdt->nbLignes_; ++n) {
            quad = icdt->listeQuads_[n];
            etatInit = etatsInitiaux[quad];
            quad->etatOr_ = etatInit;
            quad->etatEx_ = etatInit;
        }
        for (n = 0; n < icdt->nbCouplagesFermes_; ++n) {
            quad = icdt->listeCouplagesFermes_[n];
            etatInit = etatsInitiaux[quad];
            quad->etatOr_ = etatInit;
            quad->etatEx_ = etatInit;
        }
        for (n = 0; n < icdt->nbLccs_; ++n) {
            tmpHvdcInc = icdt->listeLccs_[n];
            tmpHvdcInc->etatOr_ = tmpHvdcInc->etatOrBase_;
            tmpHvdcInc->etatEx_ = tmpHvdcInc->etatExBase_;
        }
    }

    correctConnexite(resultat, icdt, numZones, detailsNonConnexite);

    return resultat;
}


/** Print the subnetwork starting from the node given by the depth */
void Reseau::afficheSousReseau(unsigned int numNoeud, unsigned int prof)
{
    for (int i = 0; i < nbNoeuds_; i++) {
        if (noeuds_[i]->num_ == numNoeud) {
            std::stringstream ss;
            afficheSousReseau(noeuds_[i]->num_, -1, prof, 0, ss);
            LOG_ALL(info) << ss.str();
            return;
        }
    }
    LOG_ALL(info) << "Noeud non trouve : " << numNoeud;
}

void Reseau::afficheSousReseau(int numNoeud, int numPere, unsigned int prof, int nbIndent, std::stringstream& ss)
{
    const auto& noeud = noeuds_[numNoeud];
    string tmpString = noeud->print() + "(" + c_fmt("%d", noeud->nbQuads()) + ")";
    ss << tmpString;
    if (prof == 0) {
        ss << '\n';
        return;
    }
    bool first = true;
    nbIndent += tmpString.size();
    set<int> listeNoeuds;
    listeNoeuds.insert(numPere);
    for (int i = 0; i < noeud->nbQuads(); i++) {
        auto& quad = noeud->listeQuads_[i];
        int numFils = quad->tnnexqua() == static_cast<size_t>(numNoeud) ? static_cast<int>(quad->tnnorqua())
                                                                        : static_cast<int>(quad->tnnexqua());
        if (listeNoeuds.find(numFils) == listeNoeuds.end()) {
            listeNoeuds.insert(numFils);
            if (!first) {
                for (int j = 0; j < nbIndent; j++) {
                    ss << " ";
                }
            }
            ss << " <-> ";
            afficheSousReseau(numFils, numNoeud, noeuds_[numFils]->nbQuads() == 1 ? 0 : prof - 1, nbIndent + 5, ss);
            first = false;
        }
    }
    for (int i = 0; i < noeud->nbCC(); i++) {
        const auto& lcc = noeud->listeCC_[i];
        int numFils;
        if (noeud->position(lcc) == Noeud::ORIGINE) {
            numFils = noeud->listeCC_[i]->nexqua_->num_;
        } else {
            numFils = noeud->listeCC_[i]->norqua_->num_;
        }
        if (listeNoeuds.find(numFils) == listeNoeuds.end()) {
            listeNoeuds.insert(numFils);
            if (!first) {
                for (int j = 0; j < nbIndent; j++) {
                    ss << " ";
                }
            }
            ss << " |=| ";
            afficheSousReseau(numFils, numNoeud, noeuds_[numFils]->nbQuads() == 0 ? 0 : prof - 1, nbIndent + 5, ss);
            first = false;
        }
    }
}

bool compareNoeuds(const std::shared_ptr<Noeud>& noeud1, const std::shared_ptr<Noeud>& noeud2)
{
    return noeud1->num_ < noeud2->num_;
}

PochePerdue::PochePerdue(const std::shared_ptr<Incident>& icdt, map<std::shared_ptr<Noeud>, int>& listeNoeuds)
{
    if (icdt->nbGroupes_ > 0) {
        LOG_ALL(error) << err::ioDico().msg("ERRIncidentGroupeRompantConnexite", icdt->nom_, c_fmt("%d", icdt->num_));
        icdt->validite_ = false;
        icdt->validiteBase_ = false;
        return;
    }
    if (icdt->nbLccs_ > 0) {
        LOG_ALL(error) << err::ioDico().msg("ERRIncidentLccRompantConnexite", icdt->nom_, c_fmt("%d", icdt->num_));
        icdt->validite_ = false;
        icdt->validiteBase_ = false;
        return;
    }

    int maxNumZone = 0;
    for (const auto& node : listeNoeuds) {
        auto tmpNoeud = node.first;
        noeudsPoche_.insert(tmpNoeud);

        if (node.second > maxNumZone) {
            maxNumZone = node.second;
        }

        if (tmpNoeud->nbConsos_ > 0 || tmpNoeud->nbGroupes_ > 0) {
            pocheAvecConsoProd_ = true;
        }

        for (int i = 0; i < tmpNoeud->nbGroupes_; ++i) {
            auto tmpGroupe = tmpNoeud->listeGroupes_[i];
            if (tmpGroupe->etat_
                && (tmpGroupe->prodAjust_ == Groupe::OUI_HR_AR || tmpGroupe->prodAjust_ == Groupe::OUI_HR)) {
                prodMaxPoche_ += tmpGroupe->puisMaxDispo_;
            }
        }
    }

    incidentModifie_ = std::make_shared<Incident>(*icdt);

    auto zoneEnd = noeudsPoche_.end();
    set<int> zonesConservees;

    for (const auto& quadIcdt : icdt->listeQuads_) {
        if (!quadIcdt->connecte()) {
            continue;
        }

        auto zoneOr = noeudsPoche_.find(quadIcdt->norqua_);
        auto zoneExt = noeudsPoche_.find(quadIcdt->nexqua_);
        if (zoneOr != zoneEnd && zoneExt != zoneEnd) {
            // C'est une ligne interne de la poche, mieux vaut la conserver
            LOG(debug) << "On conserve la ligne " << quadIcdt->nom_;
            incidentModifie_->nbLignes_--;
            std::remove(incidentModifie_->listeQuads_.begin(), incidentModifie_->listeQuads_.end(), quadIcdt);
        } else if (zoneOr == zoneEnd && zoneExt == zoneEnd) {
            // C'est une ligne hors de la poche
            LOG(debug) << "On laisse la ligne '" << quadIcdt->nom_ << "' dans l'incident ";
        } else {
            // C'est un quad qui relie la poche au reste du reseau
            int numZoneOr = listeNoeuds[quadIcdt->norqua_];
            if (numZoneOr == -1) { // -1 signifie noeud isolé, on lui affecte une zone
                numZoneOr = ++maxNumZone;
                listeNoeuds[quadIcdt->norqua_] = numZoneOr;
            }
            int numZoneExt = listeNoeuds[quadIcdt->nexqua_];
            if (numZoneExt == -1) {
                numZoneExt = ++maxNumZone;
                listeNoeuds[quadIcdt->nexqua_] = numZoneExt;
            }

            if (zonesConservees.find(numZoneOr) == zonesConservees.end()
                || zonesConservees.find(numZoneExt) == zonesConservees.end()) {
                LOG(debug) << "On conserve la ligne " << quadIcdt->nom_;

                zonesConservees.insert(numZoneOr);
                zonesConservees.insert(numZoneExt);

                incidentModifie_->nbLignes_--;
                std::remove(incidentModifie_->listeQuads_.begin(), incidentModifie_->listeQuads_.end(), quadIcdt);
            } else {
                LOG(debug) << "On laisse la ligne '" << quadIcdt->nom_ << "' dans l'incident ";
            }
        }
    }
    incidentModifie_->listeQuads_.resize(incidentModifie_->nbLignes_);
}

PochePerdue::PochePerdue(const PochePerdue& poche) :
    noeudsPoche_(poche.noeudsPoche_),
    prodMaxPoche_(poche.prodMaxPoche_),
    pocheAvecConsoProd_(poche.pocheAvecConsoProd_),
    incidentModifie_(std::make_shared<Incident>(*poche.incidentModifie_))
{
}

PochePerdue& PochePerdue::operator=(const PochePerdue& other)
{
    noeudsPoche_ = other.noeudsPoche_;
    prodMaxPoche_ = other.prodMaxPoche_;
    prodPerdue_ = 0.;
    consoPerdue_ = 0.;
    consumptionLosses_.clear();
    pocheAvecConsoProd_ = other.pocheAvecConsoProd_;
    incidentModifie_ = std::make_shared<Incident>(*other.incidentModifie_);

    return *this;
}


string PochePerdue::print() const
{
    string txt = err::ioDico().msg("INFOPochePerdue");
    for (auto& node : noeudsPoche_) {
        txt += c_fmt(" '%s'", node->print().c_str());
    }
    return txt;
}
