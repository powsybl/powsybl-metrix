//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#include <cmath>
#include <cstdio>
#include <iomanip>
#include <iostream>
#include <iterator>
#include <numeric>
#include <sstream>
#include <string>
#include <unordered_set>
#include <vector>

/* Prototypes des fonctions */
#include "calcul.h"
#include "config/configuration.h"
#include "config/constants.h"
#include "config/input_configuration.h"
#include "cte.h"
#include "err/IoDico.h"
#include "err/error.h"
#include "parametres.h"
#include "prototypes.h"
#include "reseau.h"
#include "status.h"
#include "variante.h"


using cte::c_fmt;


using std::fixed;
using std::ifstream;
using std::ios;
using std::max;
using std::min;
using std::ostringstream;
using std::setprecision;
using std::setw;
using std::string;
using std::vector;


std::mt19937 Reseau::random;

bool compareGroupeHausse(const std::shared_ptr<Groupe>& grp1, const std::shared_ptr<Groupe>& grp2)
{
    return (grp1->coutHausseHR_ < grp2->coutHausseHR_);
}

bool compareGroupeBaisse(const std::shared_ptr<Groupe>& grp1, const std::shared_ptr<Groupe>& grp2)
{
    return (grp1->coutBaisseHR_ < grp2->coutBaisseHR_);
}

bool compareMenaces(const Menace& menace1, const Menace& menace2)
{
    if (fabs(fabs(menace1.transit_) - fabs(menace2.transit_)) < config::constants::epsilon) {
        return menace1.defaut_->num_ > menace2.defaut_->num_;
    }
    return (fabs(menace1.transit_) < fabs(menace2.transit_));
}

//---------------------------------------------------------
// Methodes des differentes des classes du fichier Reseau.h
//---------------------------------------------------------

string GroupesCouples::toString() const
{
    std::stringstream out;
    vector<int> indices(elements_.size());
    std::transform(elements_.begin(),
                   elements_.end(),
                   std::back_inserter(indices),
                   [](const std::set<std::shared_ptr<Groupe>>::value_type& v) { return v->num_; });

    out << "\"" << nomRegroupement_ << "\" (ref=" << reference_ << ") ids=[";
    std::copy(indices.begin(), indices.end(), std::ostream_iterator<int>(out, ", "));
    out << "]";
    return out.str();
}

string ConsommationsCouplees::toString() const
{
    std::stringstream out;
    vector<int> indices(elements_.size());
    std::transform(elements_.begin(),
                   elements_.end(),
                   std::back_inserter(indices),
                   [](const std::set<std::shared_ptr<Consommation>>::value_type& v) { return v->num_; });

    out << "\"" << nomRegroupement_ << "\" ids=[";
    std::copy(indices.begin(), indices.end(), std::ostream_iterator<int>(out, ", "));
    out << "]";
    return out.str();
}

bool Groupe::estAjustable(bool adequacy) const
{
    if (prodAjust_ == Groupe::NON_HR_AR) {
        return false;
    }
    if (adequacy && prodAjust_ == Groupe::OUI_AR) {
        return false;
    }
    if (!adequacy && prodAjust_ == Groupe::OUI_HR) {
        return false;
    }
    return true;
}

void Reseau::update_with_configuration()
{
    const auto& configuration = config::configuration();

    coeff_pertes_ = configuration.coeffPertes();
    nbRegions_ = configuration.nbRegions();
    nbNoeuds_ = configuration.nbNodes();
    nbPostes_ = nbNoeuds_;     /* AAA ZZZ attention le nombre de postes doit etre lu separement*/
    nbNoeudsReel_ = nbNoeuds_; /* sans les noeud fictifs crees par les lcc et les td*/
    nbQuads_ = configuration.nbAcQuads();
    nbConsos_ = configuration.nbConsos();
    nbGroupes_ = configuration.nbGroups();
    nbTypesGroupes_ = configuration.nbGroupsTypes();
    nbGroupesCuratifs_ = configuration.nbCurativeGroups();
    nbConsosCuratifs_ = configuration.nbCurativeCharges();
    nbTd_ = configuration.nbTds();
    nbIncidents_ = configuration.nbIncidents();
    nbIncidentsHorsParades_ = nbIncidents_;
    nbCC_ = configuration.nbDcLinks();
    nbCCEmulAC_ = configuration.nbAcEmulatedDcLinks();
    nbSectSurv_ = configuration.nbWatchedSections();
    nbOpenBranches_ = configuration.nbOpenedBranchs();
    nbConsosCouplees_ = configuration.nbConsoCouplees();
    nbGroupesCouples_ = configuration.nbGroupsCouplees();
}

//--------------------
// Reseau::interfaceDIE
//--------------------
void Reseau::lireDonnees()
{
    const auto& config = config::configuration();

    update_with_configuration();

    // allocation des vecteurs de pointeur des objets modelisant le reseau
    //-------------------------------------------------------------------

    noeuds_.resize(nbNoeuds_, nullptr); /*vector des noeuds*/


    // Traitement des régions
    //----------------------
    regions_ = config.cgnomregDIE();
    for (auto& region : regions_) {
        rtrim(region);
    }

    // Traitement des noeuds
    //----------------------
    for (int i = 0; i < nbNoeuds_; ++i) {
        auto noeud = std::make_shared<Noeud>(i, config.cpposregDIE()[i] - 1);
        noeuds_[i] = noeud;
        noeud->typeNoeud_ = Noeud::NOEUD_REEL;
    }

    // Traitement des quadripoles
    //--------------------------
    string nomQuadripole;

    vector<std::shared_ptr<Quadripole>> quadsParIndice;
    quadsParIndice.reserve(nbQuads_);
    for (unsigned int i = 0; i < static_cast<unsigned int>(nbQuads_); ++i) {
        nomQuadripole = config.cqnomquaDIE()[i];
        rtrim(nomQuadripole);
        if (i >= config.tnnorquaDIE().size() || i >= config.tnnexquaDIE().size() || config.tnnorquaDIE()[i] == 0
            || config.tnnexquaDIE()[i] == 0 || config.tnnorquaDIE()[i] > nbNoeuds_
            || config.tnnexquaDIE()[i] > nbNoeuds_) {
            ostringstream errMsg;
            errMsg << err::ioDico().msg("ERRNoeudsQuadripole", nomQuadripole);
            throw ErrorI(errMsg.str());
        }

        int indNorqua = config.tnnorquaDIE()[i] - 1; // attention indice fortran ou C++
        int indNexqua = config.tnnexquaDIE()[i] - 1; // attention indice fortran ou C++

        auto quad = std::make_shared<Quadripole>(
            i, nomQuadripole, noeuds_[indNorqua], noeuds_[indNexqua], config.cqadmitaDIE()[i], config.cqresistDIE()[i]);
        quads_.insert(std::pair<string, std::shared_ptr<Quadripole>>(quad->nom_, quad));
        quadsParIndice.push_back(quad);

        // on cree un element a surveiller si la ligne est connectee des 2 cotes et que la surveillance est demandee
        if (config.qasurvdiDIE()[i] != ElementASurveiller::NON_SURVEILLE
            || config.qasurnmkDIE()[i] != ElementASurveiller::NON_SURVEILLE) {
            auto elemAS = std::make_shared<ElementASurveiller>(
                nomQuadripole,
                0,
                (ElementASurveiller::TypeSurveillance)config.qasurvdiDIE()[i],
                (ElementASurveiller::TypeSurveillance)config.qasurnmkDIE()[i],
                config::constants::valdef,
                config::constants::valdef,
                config::constants::valdef,
                config::constants::valdef,
                false);
            elemAS->quadsASurv_[quad] = 1.;
            quad->elemAS_ = elemAS;

            elementsASurveiller_.push_back(elemAS);
            quadsSurv_.insert(quad);

            if (elemAS->survMaxN_ == ElementASurveiller::SURVEILLE) {
                elementsASurveillerN_.insert(
                    std::pair<string, std::shared_ptr<ElementASurveiller>>(elemAS->nom_, elemAS));
                nbQuadSurvN_++;
            }

            if (elemAS->survMaxInc_ == ElementASurveiller::SURVEILLE) {
                elementsASurveillerNk_.insert(
                    std::pair<string, std::shared_ptr<ElementASurveiller>>(elemAS->nom_, elemAS));
                nbQuadSurvNk_++;
            } else if (elemAS->survMaxInc_ == ElementASurveiller::AVEC_RESULTAT) {
                elementsAvecResultatNk_.push_back(elemAS);
                nbQuadResultNk_++;
            }
        }

        // Pour les procédures de verif en debug on a besoin de calculer les coeffs de report de tous les quads
        if (config::inputConfiguration().checkConstraintLevel()
            != config::InputConfiguration::CheckConstraintLevel::NONE) {
            quadsSurv_.insert(quad);
        }
    }

    // Deconnexion des quadripoles ouverts
    for (auto ind : config.openbranDIE()) {
        const auto& quad = quadsParIndice[ind - 1];
        quad->etatExBase_ = false;
        quad->etatEx_ = false;
        quad->etatOrBase_ = false;
        quad->etatOr_ = false;
        LOG(debug) << metrix::log::verbose_config << "La branche '" << quad->nom_ << "' est deconnectee";
    }


    int cptPtIncident = 0;
    // traitement particulier des TD(s)
    //-------------------------------
    tdParIndice_.reserve(nbTd_);
    if (nbTd_ > 0) {
        // Ajout d'un nouveau quadripole et un nouveau noeud pour chaque TD
        int oldSizeNoeuds = static_cast<int>(noeuds_.size());
        int oldSizeQuads = static_cast<int>(quads_.size());
        nbQuads_ = nbQuads_ + nbTd_;
        nbNoeuds_ = nbNoeuds_ + nbTd_;
        noeuds_.resize(nbNoeuds_);

        for (unsigned int i = 0; i < static_cast<unsigned int>(nbTd_); ++i) {
            const auto& quadVrai = quadsParIndice[config.dttrdequDIE()[i] - 1];
            auto typeP = static_cast<TransformateurDephaseur::TypePilotageTD>(config.dtmodregDIE()[i]);

            if (typeP == TransformateurDephaseur::PILOTAGE_ANGLE_OPTIMISE
                || typeP == TransformateurDephaseur::PILOTAGE_PUISSANCE_OPTIMISE) {
                actionsPreventivesPossibles_ = true;
            }
            // Lecture des valeurs de déphasage des prises du TD
            vector<float> tapdepha;
            int dtlowtapi = -1;

            int nbtabsdie = 0;
            if (i < config.dtnbtapsDIE().size()
                && config.dtnbtapsDIE()[i] > 0) { // il y a des prises pour le TD dans les donnees d'entree
                nbtabsdie = config.dtnbtapsDIE()[i];
                unsigned int indexFirstTapDepha = std::accumulate(
                    config.dtnbtapsDIE().begin(), config.dtnbtapsDIE().begin() + i, 0);
                auto it_first = config.dttapdepDIE().begin() + indexFirstTapDepha;
                auto it_last = it_first + nbtabsdie;
                tapdepha = std::vector<float>(it_first, it_last);

                dtlowtapi = config.dtlowtapDIE()[i];
            }
            auto td = creerTD(quadVrai,
                              i,
                              oldSizeNoeuds + i,
                              oldSizeQuads + i,
                              config.dtvaldepDIE()[i],
                              config.dtvalinfDIE()[i],
                              config.dtvalsupDIE()[i],
                              typeP,
                              PREVENTIF_SEUL,
                              dtlowtapi,
                              nbtabsdie,
                              tapdepha,
                              -1,
                              -1);

            // Lecture des incidents a traiter en curatif:
            if (i < config.dtnbdefkDIE().size() && config.dtnbdefkDIE()[i] > 0) {
                nbTdCuratif_++;
                td->mode_ = CURATIF_POSSIBLE;

                for (int u = 0; u < config.dtnbdefkDIE()[i]; ++u) {
                    td->incidentsAtraiterCuratif_.insert(config.dtptdefkDIE()[cptPtIncident]);
                    cptPtIncident++;
                }
            }
        }

        // Positionnement des limites de prise preventive inf
        for (unsigned int ii = 0; ii < 2 * static_cast<unsigned int>(nbTd_) && ii < config.dtlowranDIE().size()
                                  && config.dtlowranDIE()[ii] != -1;
             ii += 2) {
            const auto& tmpQuad = quadsParIndice[config.dtlowranDIE()[ii] - 1];
            TransfoDephaseurs_.find(tmpQuad->nom_)->second->lowran_ = config.dtlowranDIE()[ii + 1];
        }

        // Positionnement des limites de prise preventive sup
        for (unsigned int ii = 0; ii < 2 * static_cast<unsigned int>(nbTd_) && ii < config.dtuppranDIE().size()
                                  && config.dtuppranDIE()[ii] != -1;
             ii += 2) {
            const auto& quad = quadsParIndice[config.dtuppranDIE()[ii] - 1];
            TransfoDephaseurs_.find(quad->nom_)->second->uppran_ = config.dtuppranDIE()[ii + 1];
        }
    }

    // Traitement des lignes a courant continu
    //---------------------------------------
    lccParIndice_.reserve(nbCC_);
    if (nbCC_ > 0) {
        cptPtIncident = 0;
        int cptEmulAC = 0;

        if (nbCCEmulAC_ > 0) {
            noeuds_.resize(nbNoeuds_ + nbCCEmulAC_);
        }

        for (unsigned int i = 0; i < static_cast<unsigned int>(nbCC_); ++i) {
            const auto& nor = noeuds_[config.dcnorquaDIE()[i] - 1];
            const auto& nex = noeuds_[config.dcnexquaDIE()[i] - 1];
            string nom = config.dcnomquaDIE()[i];
            rtrim(nom);
            auto ligneCC = std::make_shared<LigneCC>(i,
                                                     nom,
                                                     nor,
                                                     nex,
                                                     config.dcminpuiDIE()[i],
                                                     config.dcmaxpuiDIE()[i],
                                                     config.dcimppuiDIE()[i],
                                                     config.dcperst1DIE()[i],
                                                     config.dcperst2DIE()[i],
                                                     config.dcresistDIE()[i],
                                                     config.dctensdcDIE()[i]);

            nor->listeCC_.push_back(ligneCC);
            nex->listeCC_.push_back(ligneCC);

            ligneCC->type_ = (LigneCC::TypePilotageCC)config.dcregpuiDIE()[i];
            // ligneCC->type_ = LigneCC::PILOTAGE_PUISSANCE_IMPOSE;

            if (ligneCC->type_ == LigneCC::PILOTAGE_PUISSANCE_OPTIMISE) {
                actionsPreventivesPossibles_ = true;
            }

            if (ligneCC->puiMin_ > ligneCC->puiCons_ || ligneCC->puiMax_ < ligneCC->puiCons_) {
                ostringstream errMsg;
                errMsg << err::ioDico().msg("ERRHVDCPminPmaxImpose", nom);
                throw ErrorI(errMsg.str());
            }

            // Lecture des incidents a traiter en curatif:
            if (i < config.dcnbdefkDIE().size() && config.dcnbdefkDIE()[i] > 0) {
                if (ligneCC->type_ != LigneCC::PILOTAGE_PUISSANCE_OPTIMISE
                    && ligneCC->type_ != LigneCC::PILOTAGE_EMULATION_AC_OPTIMISE) {
                    LOG_ALL(warning) << err::ioDico().msg("WARNHVDCCurNonTraite", ligneCC->nom_);
                } else {
                    nbCCCuratif_++;
                    ligneCC->mode_ = CURATIF_POSSIBLE;

                    for (int u = 0; u < config.dcnbdefkDIE()[i]; ++u) {
                        ligneCC->incidentsAtraiterCuratif_.insert(config.dcptdefkDIE()[cptPtIncident]);
                    }
                    cptPtIncident++;
                }
            }
            LigneCCs_.insert(std::pair<string, std::shared_ptr<LigneCC>>(ligneCC->nom_, ligneCC));
            lccParIndice_.push_back(ligneCC);

            // Traitement des lignes CC en emulation AC
            if (ligneCC->isEmulationAC()) {
                actionsPreventivesPossibles_ = true;

                if (cptEmulAC >= nbCCEmulAC_) {
                    ostringstream errMsg;
                    errMsg << "Incoherence nb donnees DCDROOPK, ligne CC : " << nom;
                    throw ErrorI(errMsg.str());
                }

                // creation d'un quadripole fictif pour representer l'emulation AC
                string nomQuad = ligneCC->nom_ + "_AC";

                double coefK = config.dcdroopkDIE()[cptEmulAC];

                if (coefK == 0) {
                    ostringstream errMsg;
                    errMsg << "HVDC en emulation AC avec k a 0 : " << nom;
                    throw ErrorI(errMsg.str());
                }

                unsigned int uref = config::configuration().uRef();
                coefK *= 180.0 / config::constants::pi / uref / uref;

                auto quadACFictif = std::make_shared<Quadripole>(
                    nbQuads_, nomQuad, ligneCC->norqua_, ligneCC->nexqua_, coefK, 0.);

                quadACFictif->typeQuadripole_ = Quadripole::QUADRIPOLE_EMULATION_AC;
                quads_.insert(std::pair<string, std::shared_ptr<Quadripole>>(quadACFictif->nom_, quadACFictif));
                ligneCC->quadFictif_ = quadACFictif;
                nbQuads_++;

                // vecteur de prises vide
                vector<float> dephavide;

                auto td = creerTD(quadACFictif,
                                  nbTd_,
                                  nbNoeuds_,
                                  nbQuads_,
                                  0.,
                                  ligneCC->puiMin_ / coefK,
                                  ligneCC->puiMax_ / coefK,
                                  TransformateurDephaseur::PILOTAGE_ANGLE_OPTIMISE,
                                  CURATIF_POSSIBLE,
                                  0,
                                  0,
                                  dephavide,
                                  -1,
                                  -1);
                nbNoeuds_++;
                nbTd_++;
                nbTdCuratif_++;
                TDFictifs_.push_back(td);
                nbQuads_++;
                td->fictif_ = true;

                // Ajout de la surveillance sur le quad fictif
                auto elemAS = std::make_shared<ElementASurveiller>(ligneCC->nom_,
                                                                   0,
                                                                   ElementASurveiller::SURVEILLE,
                                                                   ElementASurveiller::SURVEILLE,
                                                                   ligneCC->puiMax_,
                                                                   ligneCC->puiMax_,
                                                                   ligneCC->puiMax_,
                                                                   ligneCC->puiMax_,
                                                                   false);

                elemAS->seuilsAssymetriques_ = true;
                double seuilMin = -ligneCC->puiMin_;
                elemAS->seuilMaxNExOr_ = seuilMin;
                elemAS->seuilMaxIncExOr_ = seuilMin;
                elemAS->seuilMaxIncComplexeExOr_ = seuilMin;
                elemAS->seuilMaxAvantCurExOr_ = seuilMin;
                elemAS->seuilMaxNExOr_ = seuilMin;
                elemAS->seuilMaxIncExOr_ = seuilMin;
                elemAS->seuilMaxIncComplexeExOr_ = seuilMin;
                elemAS->seuilMaxAvantCurExOr_ = seuilMin;

                elemAS->quadsASurv_[quadACFictif] = 1.;
                elemAS->hvdcASurv_[ligneCC] = 1.;
                quadACFictif->elemAS_ = elemAS;
                elementsASurveiller_.push_back(elemAS);
                quadsSurv_.insert(quadACFictif);
                elementsASurveillerN_.insert(
                    std::pair<string, std::shared_ptr<ElementASurveiller>>(elemAS->nom_, elemAS));
                nbQuadSurvN_++;
                elementsASurveillerNk_.insert(
                    std::pair<string, std::shared_ptr<ElementASurveiller>>(elemAS->nom_, elemAS));
                nbQuadSurvNk_++;

                // Ajout du TD fictif en curatif sur tous les incidents
                for (int j = 0; j < nbIncidents_; ++j) {
                    td->incidentsAtraiterCuratif_.insert(j);
                }

                cptEmulAC++;
            }
        }
    }

    // Traitement des Groupes
    //----------------------

    // Types des groupes
    for (unsigned int i = 0; i < static_cast<unsigned int>(nbTypesGroupes_); ++i) {
        string typeGroupe = config.trnomtypDIE()[i];
        rtrim(typeGroupe);
        typesGroupes_.push_back(typeGroupe);
    }

    string nomGroupe;
    nbVarGroupes_ = 0;
    double sumMaxGroupes = 0.0;
    DBandeRegGlobale_ = 0;
    cptPtIncident = 0;
    vector<std::shared_ptr<Groupe>> groupesParIndice;
    groupesParIndice.reserve(nbGroupes_);

    for (unsigned int i = 0; i < static_cast<unsigned int>(nbGroupes_); ++i) {
        nomGroupe = config.trnomgthDIE()[i];
        rtrim(nomGroupe);
        if (i >= config.tnneurgtDIE().size() || config.tnneurgtDIE()[i] == 0 || config.tnneurgtDIE()[i] > nbNoeuds_) {
            ostringstream errMsg;
            errMsg << err::ioDico().msg("ERRNoeudGroupe", nomGroupe, c_fmt("%d", config.tnneurgtDIE()[i]));
            throw ErrorI(errMsg.str());
        }

        if (i >= config.trvalpmdDIE().size() || i >= config.trpuiminDIE().size()
            || config.trvalpmdDIE()[i] < config.trpuiminDIE()[i]) {
            ostringstream errMsg;
            errMsg << err::ioDico().msg("ERRGroupePminPmax",
                                        nomGroupe,
                                        c_fmt("%d", config.trpuiminDIE()[i]),
                                        c_fmt("%d", config.tnneurgtDIE()[i]));
            throw ErrorI(errMsg.str());
        }

        int indNoeudRaccord = config.tnneurgtDIE()[i] - 1; // attention indice fortran ou C++

        auto ajustMode = static_cast<Groupe::ProdAjustable>(config.spimpmodDIE()[i]);
        if (config::configuration().computationType()
            == config::Configuration::ComputationType::OPF_WITHOUT_REDISPATCH) {
            if (ajustMode == Groupe::OUI_HR_AR) {
                ajustMode = Groupe::OUI_HR;
            } else if (ajustMode == Groupe::OUI_AR) {
                ajustMode = Groupe::NON_HR_AR;
            }
        }

        if (ajustMode == Groupe::OUI_HR_AR || ajustMode == Groupe::OUI_AR) {
            actionsPreventivesPossibles_ = true;
        }

        float trdembanDIE = 0.0;
        if (i < config.trdembanDIE().size()) {
            trdembanDIE = config.trdembanDIE()[i];
        }

        auto prod = std::make_shared<Groupe>(i,
                                             nomGroupe,
                                             noeuds_[indNoeudRaccord],
                                             config.trtypgrpDIE()[i],
                                             config.sppactgtDIE()[i],
                                             config.trpuiminDIE()[i],
                                             config.trvalpmdDIE()[i],
                                             trdembanDIE,
                                             ajustMode);


        groupes_.insert(std::pair<string, std::shared_ptr<Groupe>>(prod->nom_, prod));
        groupesParIndice.push_back(prod);
        // chaque groupe modifiable introduit trois variable P_hausse et P_baisse et P_fixe
        if (prod->prodAjust_ != Groupe::NON_HR_AR) {
            nbVarGroupes_ += 2;
        }

        if (prod->prodAjust_ == Groupe::OUI_HR_AR || prod->prodAjust_ == Groupe::OUI_HR) {
            groupesEOD_.push_back(prod);
            sumMaxGroupes += prod->puisMaxDispo_;
        }

        DBandeRegGlobale_ += trdembanDIE;

        // Lecture des incidents a traiter en curatif:
        if ((config::configuration().computationType() == config::Configuration::ComputationType::OPF
             || config::configuration().computationType() == config::Configuration::ComputationType::OPF_WITH_OVERLOAD)
            && i < config.grnbdefkDIE().size() && config.grnbdefkDIE()[i] > 0) {
            for (int u = 0; u < config.grnbdefkDIE()[i]; ++u) {
                prod->incidentsAtraiterCuratif_.insert(config.grptdefkDIE()[cptPtIncident]);
                cptPtIncident++;
            }
        }
    }
    prodMaxPossible_ = sumMaxGroupes;

    // Traitement des Consommations
    //----------------------------
    cptPtIncident = 0;
    int cptCuratif = 0;
    string nomConso;
    vector<std::shared_ptr<Consommation>> consommationsParIndice;
    consommationsParIndice.reserve(nbConsos_);
    for (unsigned int i = 0; i < static_cast<unsigned int>(nbConsos_); ++i) {
        nomConso = config.tnnomnoeDIE()[i];
        rtrim(nomConso);

        if (i >= config.tnneucelDIE().size() || config.tnneucelDIE()[i] == 0 || config.tnneucelDIE()[i] > nbNoeuds_) {
            ostringstream errMsg;
            errMsg << err::ioDico().msg("ERRNoeudConso", c_fmt("%d", i), c_fmt("%d", config.tnneucelDIE()[i]));
            throw ErrorI(errMsg.str());
        }

        int indNoeudRaccord = config.tnneucelDIE()[i] - 1;
        const auto& noeud = noeuds_[indNoeudRaccord];

        auto conso = std::make_shared<Consommation>(i, nomConso, noeud, config.esafiactDIE()[i]);
        consos_.insert(std::pair<string, std::shared_ptr<Consommation>>(conso->nom_, conso));
        consommationsParIndice.push_back(conso);

        int nvapalDIE = 100;
        if (i < config.tnvapalDIE().size()) {
            if (config.tnvapalDIE()[i] < 0) {
                ostringstream errMsg;
                errMsg << err::ioDico().msg("ERRPalierNoeudNegatif", nomConso, c_fmt("%d", config.tnvapalDIE()[i]));
                throw ErrorI(errMsg.str());
            }

            if (config.tnvapalDIE()[i] > 100) {
                ostringstream errMsg;
                errMsg << err::ioDico().msg("ERRPalierNoeudSup100", nomConso, c_fmt("%d", config.tnvapalDIE()[i]));
                throw ErrorI(errMsg.str());
            }

            nvapalDIE = config.tnvapalDIE()[i];
        }

        conso->seuil_ = static_cast<double>(nvapalDIE) / 100.0; // par exemple 1/3
        if (conso->seuil_ > 0) {
            conso->numVarConso_ = nbVarGroupes_ + nbVarConsos_;
            nbVarConsos_++;
        }

        conso->cout_ = (i >= config.tnvacouDIE().size() || config.tnvacouDIE()[i] == config::constants::valdef)
                           ? config::configuration().costFailure()
                           : config.tnvacouDIE()[i];

        // Lecture des incidents a traiter en curatif:
        if (config::configuration().computationType() == config::Configuration::ComputationType::OPF
            && i < config.ldnbdefkDIE().size() && config.ldnbdefkDIE()[i] > 0) {
            conso->pourcentEffacement_ = ((float)config.ldcurperDIE()[cptCuratif]) / 100.;

            for (int u = 0; u < config.ldnbdefkDIE()[i]; ++u) {
                conso->incidentsAtraiterCuratif_.insert(config.ldptdefkDIE()[cptPtIncident]);
                cptPtIncident++;
            }

            cptCuratif++;
        }
    }

    // Calcul de connexite
    //-------------------
    if (!connexite()) {
        // ConstructionChainage();
        //It is possible to display the subnetwork by calling the afficheSousReseau(node number,depth level) method
        // where node number is the center node of the subnetwork and depth level is the distance in nodes depth from that center.
        ostringstream errMsg;
        errMsg << err::ioDico().msg("ERRReseauNonConnexe");
        throw ErrorI(errMsg.str());
    }

    // Traitement des sections surveillees
    //-------------------------------------
    int cptTableDescr = 0; // iterateur sur les tables de description des sect. surv.
                           //( secttype, sectnumq et sectcoef)
    for (int i = 0; i < nbSectSurv_; ++i) {
        string nom = config.sectnomsDIE()[i];
        rtrim(nom);
        int num = 0;
        double seuilN = config.sectmaxnDIE()[i];

        auto elemAS = std::make_shared<ElementASurveiller>(nom,
                                                           num,
                                                           ElementASurveiller::SURVEILLE,
                                                           ElementASurveiller::NON_SURVEILLE,
                                                           seuilN,
                                                           config::constants::valdef,
                                                           config::constants::valdef,
                                                           config::constants::valdef,
                                                           true);

        elemAS->seuilsAssymetriques_ = true;

        for (int j = 0; j < config.sectnbqdDIE()[i]; ++j) {
            int type = config.secttypeDIE()[j + cptTableDescr];
            if (type == QUADRIPOLE) {
                const auto& quad = quadsParIndice[config.sectnumqDIE()[j + cptTableDescr] - 1];
                elemAS->quadsASurv_[quad] = config.sectcoefDIE()[j + cptTableDescr];
                quadsSurv_.insert(quad);
            } else if (type == HVDC) {
                const auto& lcc = lccParIndice_[config.sectnumqDIE()[j + cptTableDescr] - 1];
                elemAS->hvdcASurv_[lcc] = config.sectcoefDIE()[j + cptTableDescr];
                if (lcc->isEmulationAC()) {
                    elemAS->quadsASurv_[lcc->quadFictif_] = config.sectcoefDIE()[j + cptTableDescr];
                }
            } else {
                LOG_ALL(error) << " Objet dans section surveillee non traite : type " << type;
            }
        } // end for

        cptTableDescr += config.sectnbqdDIE()[i];
        elementsASurveiller_.push_back(elemAS);
        elementsASurveillerN_.insert(std::pair<string, std::shared_ptr<ElementASurveiller>>(elemAS->nom_, elemAS));
        nbQuadSurvN_++;
    }

    // Traitement des incidents
    //------------------------
    // les incidents sont classés dans l'ordre suivant
    // N-1 groupes systematique
    // N-1 lignes  systematique
    // N-k (groupes et ligne)
    // ATTENTION : cette partie du code est  a revoir lors de la lecture de :
    // DMNBDEFL ;DMTYPLIG; DMNUMLIG ;	DMNBDEFG; DMTYPGRO	;DMNUMGRO
    // en effet, la creation des incidents simples se fera directement sur les boucles :
    // (i) quadripoles (ii) groupes

    double pMaxTmp;
    double pMaxIncident = 0; // puissance max que l on peut perdre lors d un incident sur un grp
    int nbIncValides = 0;

    incidentsEtParades_.reserve(nbIncidents_);
    for (unsigned int i = 0; i < static_cast<unsigned int>(nbIncidents_); ++i) {
        auto icdt = std::make_shared<Incident>(Incident::INCONNU);
        icdt->num_ = i;
        icdt->nbLignes_ = 0;
        icdt->nbGroupes_ = 0;

        int ind = config.dmptdefkDIE()[i];
        int indFin = ind + config.dmdescrkDIE()[ind - 1];


        pMaxTmp = 0;
        while (ind < indFin) {
            int typeOuv = config.dmdescrkDIE()[ind];
            if (typeOuv == QUADRIPOLE) {
                // Quadripole: ligne, transfo; TD
                icdt->nbLignes_++;
                ind++;
                if (config.dmdescrkDIE()[ind] < 1) {
                    ostringstream errMsg;
                    errMsg << err::ioDico().msg("ERROuvrageInconnuIncident", c_fmt("%d", icdt->num_));
                    throw ErrorI(errMsg.str());
                }
                const auto& quadInc = quadsParIndice[config.dmdescrkDIE()[ind] - 1];
                icdt->listeQuads_.push_back(quadInc);
                quadsSurv_.insert(quadInc);
                ind++;
            } else if (typeOuv == GROUPE) {
                // Groupe
                icdt->nbGroupes_++;
                ind++;
                if (config.dmdescrkDIE()[ind] < 1) {
                    ostringstream errMsg;
                    errMsg << err::ioDico().msg("ERROuvrageInconnuIncident", c_fmt("%d", icdt->num_));
                    throw ErrorI(errMsg.str());
                }
                const auto& grp = groupesParIndice[config.dmdescrkDIE()[ind] - 1];
                icdt->listeGroupes_.push_back(grp);
                pMaxTmp += grp->puisMax_;

                ind++;
            } else if (typeOuv == HVDC) {
                // liaison Courant continu
                icdt->nbLccs_++;
                ind++;
                if (config.dmdescrkDIE()[ind] < 1) {
                    ostringstream errMsg;
                    errMsg << err::ioDico().msg("ERROuvrageInconnuIncident", c_fmt("%d", icdt->num_));
                    throw ErrorI(errMsg.str());
                }
                const auto& hvdc = lccParIndice_[config.dmdescrkDIE()[ind] - 1];
                icdt->listeLccs_.push_back(hvdc);
                if (hvdc->quadFictif_) {
                    icdt->nbLignes_++;
                    icdt->listeQuads_.push_back(hvdc->quadFictif_);
                }
                ind++;
            } else {
                ostringstream errMsg;
                errMsg << err::ioDico().msg("ERRTypeOuvrageInconnuIncident", c_fmt("%d", icdt->num_));
                throw ErrorI(errMsg.str());
            }
        }

        if (pMaxIncident < pMaxTmp) {
            pMaxIncident = pMaxTmp;
        }

        if (icdt->nbLignes_ == 0 && icdt->nbLccs_ == 0) {
            icdt->type_ = Incident::N_MOINS_K_GROUPE;
        } else if (icdt->nbGroupes_ == 0 && icdt->nbLignes_ == 1 && icdt->nbLccs_ == 0) {
            icdt->type_ = Incident::N_MOINS_1_LIGNE;
        } else {
            icdt->type_ = Incident::N_MOINS_K_GROUPE_LIGNE;
        }


        string nom = config.dmnomdekDIE()[i];
        rtrim(nom);
        if (!nom.empty()) {
            icdt->nom_.assign(nom);
        } else if (icdt->type_ == Incident::N_MOINS_1_LIGNE) {
            icdt->nom_ = icdt->listeQuads_[0]->nom_;
        } else if (icdt->nbGroupes_ == 0 && icdt->nbLignes_ == 0 && icdt->nbLccs_ == 1) {
            icdt->nom_ = icdt->listeLccs_[0]->nom_;
        } else {
            for (int l = 0; l < icdt->nbLignes_; ++l) {
                if (icdt->listeQuads_[l]->typeQuadripole_ == Quadripole::QUADRIPOLE_REEL) {
                    nom += "L_" + icdt->listeQuads_[l]->nom_ + ";";
                }
            }
            for (int l = 0; l < icdt->nbGroupes_; ++l) {
                nom += "G_";
                nom += icdt->listeGroupes_[l]->nom_;
                nom += ";";
            }
            for (int l = 0; l < icdt->nbLccs_; ++l) {
                nom += "H_";
                nom += icdt->listeLccs_[l]->nom_;
                nom += ";";
            }

            icdt->nom_ = nom;
        }
        incidents_.insert(std::pair<string, std::shared_ptr<Incident>>(icdt->nom_, icdt));
        incidentsEtParades_.push_back(icdt);

        if (icdt->nbLignes_ > 0 || icdt->nbLccs_ > 0) {
            // Test de connexite
            connexite(icdt, false, config::configuration().useIncRompantConnexite());

            if (!icdt->validite_) {
                icdt->validiteBase_ = false;
                LOG_ALL(info) << err::ioDico().msg(
                    "INFOIncidentRompantConnexite", "N-k", c_fmt("%d", icdt->num_), icdt->nom_);
            }
        }

        if (icdt->validite_) {
            nbIncValides++;
            LOG(debug) << metrix::log::verbose_config << " on simule l'incident " << icdt->nom_ << " num "
                       << icdt->num_;
        }
    }

    // Traitement des incidents avec resultats detailles
    if (config.nbDefres() > 0) {
        int indice = -1;
        int nbDef = -1;
        while (indice < static_cast<int>(config.nbDefres()) - 1) {
            ++indice;
            const auto& quad = quadsParIndice[config.ptdefresDIE()[indice] - 1];
            ++indice;
            nbDef = config.ptdefresDIE()[indice];
            std::set<std::shared_ptr<Incident>> listeIncidents;
            for (int i = 0; i < nbDef; ++i) {
                ++indice;
                const auto& icdt = incidentsEtParades_[config.ptdefresDIE()[indice]];
                LOG(debug) << metrix::log::verbose_config << "Quad : " << quad->nom_
                           << ", transit sur incident : " << icdt->nom_;
                if (icdt->validite_) {
                    listeIncidents.insert(icdt);
                    incidentsAvecTransits_.insert(icdt);
                }
            }
            transitsSurDefauts_.insert(
                std::pair<std::shared_ptr<Quadripole>, std::set<std::shared_ptr<Incident>>>(quad, listeIncidents));
        }
    }

    // Traitement des incidents complexes
    for (unsigned int i = 0; i < config.nbDefspe(); ++i) {
        const auto& icdt = incidentsEtParades_[config.ptdefspeDIE()[i]];
        icdt->incidentComplexe_ = true;
        LOG_ALL(info) << "Incident complexe : " << icdt->nom_ << " (num=" << icdt->num_ << ")";
    }

    // Traitement des variations marginales detaillees
    if (config.nbVarmar() > 0) {
        int indice = -1;
        int nbDef = -1;
        while (indice < static_cast<int>(config.nbVarmar()) - 1) {
            ++indice;
            const auto& quad = quadsParIndice[config.ptvarmarDIE()[indice] - 1];
            ++indice;
            nbDef = config.ptvarmarDIE()[indice];
            std::set<std::shared_ptr<Incident>> listeIncidents;
            for (int i = 0; i < nbDef; ++i) {
                ++indice;
                const auto& icdt = incidentsEtParades_[config.ptvarmarDIE()[indice]];
                LOG(debug) << "Quad : " << quad->nom_
                           << ", variations marginales detaillees sur incident : " << icdt->nom_;
                if (icdt->validite_) {
                    listeIncidents.insert(icdt);
                }
            }
            variationsMarginalesDetaillees_.insert(
                std::pair<string, std::set<std::shared_ptr<Incident>>>(quad->nom_, listeIncidents));
        }
    }


    int nbCur = 0;
    // Remplissage du vecteur des TD curatif
    for (int u = 0; nbCur < nbTdCuratif_ && u < nbTd_; ++u) {
        const auto& td = tdParIndice_[u];

        if (td->incidentsAtraiterCuratif_.empty()) {
            continue;
        }

        nbCur++;
        for (auto num : td->incidentsAtraiterCuratif_) {
            const auto& inc = incidentsEtParades_[num];

            if (!inc->validite_) {
                continue;
            }

            if (find(inc->listeQuads_.begin(), inc->listeQuads_.end(), td->quadVrai_) != inc->listeQuads_.end()) {
                LOG_ALL(warning) << err::ioDico().msg("WARNIncCurNonTraiteOuvInc", inc->nom_, td->quad_->nom_);
                continue;
            }

            auto elemC = std::make_shared<ElementCuratifTD>(td);

            inc->listeElemCur_.push_back(elemC);
            if (td->fictif_) {
                inc->tdFictifsElemCur_.insert(
                    std::pair<std::shared_ptr<Quadripole>, std::shared_ptr<ElementCuratifTD>>(td->quadVrai_, elemC));
            }
        }
    }

    // Remplissage du vecteur des HVDC curatifs
    nbCur = 0;
    for (int u = 0; nbCur < nbCCCuratif_ && u < nbCC_; ++u) {
        const auto& lcc = lccParIndice_[u];

        if (lcc->incidentsAtraiterCuratif_.empty()) {
            continue;
        }

        nbCur++;
        for (auto num : lcc->incidentsAtraiterCuratif_) {
            const auto& inc = incidentsEtParades_[num];

            if (!inc->validite_) {
                continue;
            }

            if (find(inc->listeLccs_.begin(), inc->listeLccs_.end(), lcc) != inc->listeLccs_.end()) {
                LOG_ALL(warning) << err::ioDico().msg("WARNIncCurNonTraiteOuvInc", inc->nom_, lcc->nom_);
                continue;
            }

            auto elemC = std::make_shared<ElementCuratifHVDC>(lcc);
            inc->listeElemCur_.push_back(elemC);
            inc->lccElemCur_.insert(
                std::pair<std::shared_ptr<LigneCC>, std::shared_ptr<ElementCuratifHVDC>>(lcc, elemC));
        }
    }

    // Remplissage du vecteur des groupes curatifs
    nbCur = 0;
    for (int u = 0; nbCur < nbGroupesCuratifs_ && u < nbGroupes_; ++u) {
        const auto& grp = groupesParIndice[u];
        if (grp->incidentsAtraiterCuratif_.empty()) {
            continue;
        }

        nbCur++;
        for (auto num : grp->incidentsAtraiterCuratif_) {
            const auto& inc = incidentsEtParades_[num];

            if (!inc->validite_) {
                continue;
            }

            if (find(inc->listeGroupes_.begin(), inc->listeGroupes_.end(), grp) != inc->listeGroupes_.end()) {
                LOG_ALL(warning) << err::ioDico().msg("WARNIncCurNonTraiteOuvInc", inc->nom_, grp->nom_);
                continue;
            }

            auto elemC = std::make_shared<ElementCuratifGroupe>(grp);
            inc->listeElemCur_.push_back(elemC);
        }
    }

    // Remplissage du vecteur des effacements curatifs
    nbCur = 0;
    for (int u = 0; nbCur < nbConsosCuratifs_ && u < nbConsos_; ++u) {
        const auto& conso = consommationsParIndice[u];

        if (conso->incidentsAtraiterCuratif_.empty()) {
            continue;
        }
        nbCur++;
        for (auto num : conso->incidentsAtraiterCuratif_) {
            const auto& inc = incidentsEtParades_[num];

            if (!inc->validite_) {
                continue;
            }

            auto elemC = std::make_shared<ElementCuratifConso>(conso);
            inc->listeElemCur_.push_back(elemC);
        }
    }

    for (const auto& elem : incidents_) {
        const auto& inc = elem.second;
        if (!inc->validite_) {
            continue;
        }

        if (!inc->listeElemCur_.empty()) {
            for (const auto& elemCur : inc->listeElemCur_) {
                string typeCur;
                string nomElem;
                switch (elemCur->typeElem_) {
                    case ElementCuratif::TD:
                        typeCur = "TD";
                        nomElem = std::dynamic_pointer_cast<ElementCuratifTD>(elemCur)->td_->quad_->nom_;
                        break;
                    case ElementCuratif::TD_FICTIF:
                        typeCur = "TD_FICTIF";
                        nomElem = std::dynamic_pointer_cast<ElementCuratifTD>(elemCur)->td_->quad_->nom_;
                        break;
                    case ElementCuratif::HVDC:
                        typeCur = "HVDC";
                        nomElem = std::dynamic_pointer_cast<ElementCuratifHVDC>(elemCur)->lcc_->nom_;
                        break;
                    case ElementCuratif::GROUPE:
                        typeCur = "GROUPE";
                        nomElem = std::dynamic_pointer_cast<ElementCuratifGroupe>(elemCur)->groupe_->nom_;
                        break;
                    case ElementCuratif::CONSO:
                        typeCur = "CONSO";
                        nomElem = std::dynamic_pointer_cast<ElementCuratifConso>(elemCur)->conso_->nom_;
                        break;
                    default: typeCur = "ERREUR"; nomElem = "NON TROUVE";
                }
                LOG(debug) << metrix::log::verbose_config << "curative action: inc " << inc->nom_ << " (" << inc->num_
                           << ")  traite en curatif par " << typeCur << "  " << nomElem << "(" << elemCur->num() << ")";
            }
        }
    }

    // Variables couplees
    int indexCourant = 0;
    int nbVar = -1;
    for (int u = 0; u < nbGroupesCouples_; ++u) {
        string nom = config.gbindnomDIE()[u];
        rtrim(nom);
        nbVar = config.gbinddefDIE()[indexCourant];
        indexCourant++;
        std::set<int> listeVariables(&(config.gbinddefDIE()[indexCourant]),
                                     &(config.gbinddefDIE()[indexCourant + nbVar]));
        GroupesCouples::SetGroupPtr elements;
        for (auto var : listeVariables) {
            const auto& grp = groupesParIndice[var - 1];
            if (!grp->estAjustable(false)) {
                ostringstream errMsg;
                errMsg << err::ioDico().msg("ERRGroupeLieNonModifiable", grp->nom_, nom);
                throw ErrorI(errMsg.str());
            }
            elements.insert(grp);
        }

        indexCourant += nbVar;
        auto coupleGroupes = std::make_shared<GroupesCouples>(
            nom, elements, (GroupesCouples::VariableReference)config.gbindrefDIE()[u]);
        groupesCouples_.push_back(coupleGroupes);
    }

    indexCourant = 0;
    for (int u = 0; u < nbConsosCouplees_; ++u) {
        string nom = config.lbindnomDIE()[u];
        rtrim(nom);
        nbVar = config.lbinddefDIE()[indexCourant];
        indexCourant++;
        std::set<int> listeVariables(&config.lbinddefDIE()[indexCourant], &config.lbinddefDIE()[indexCourant + nbVar]);
        std::set<std::shared_ptr<Consommation>> elements;
        for (auto var : listeVariables) {
            const auto& conso = consommationsParIndice[var - 1];
            if (conso->numVarConso_ == -1) {
                ostringstream errMsg;
                errMsg << err::ioDico().msg("ERRConsoLieeNonModifiable", conso->nom_, nom);
                throw ErrorI(errMsg.str());
            }
            elements.insert(conso);
        }

        indexCourant += nbVar;
        auto coupleConsos = std::make_shared<ConsommationsCouplees>(nom, elements);
        consosCouplees_.push_back(coupleConsos);
    }

    std::stringstream ss;
    ss << "Informations sur les variables couplees";
    for (const auto& elem : groupesCouples_) {
        ss << " " << elem->toString();
    }
    for (const auto& elem : consosCouplees_) {
        ss << " " << elem->toString();
    }
    LOG(debug) << metrix::log::verbose_config << ss.str();


    // Verification de la demi-bande de reglage suffit a tenir au plus gros incident groupe
    // Attention: condition trop restritive car les groupes que l on perd peuvent ne pas etre demarre
    // A voir ...

    if (pMaxIncident > 0) {
        defautsGroupesPresents_ = true;

        if (DBandeRegGlobale_ <= pMaxIncident - config::constants::epsilon) {
            ostringstream errMsg;
            errMsg << err::ioDico().msg(
                "ERRBandeReglInsuffisante", c_fmt("%.3f", DBandeRegGlobale_), c_fmt("%.3f", pMaxIncident));
            throw ErrorI(errMsg.str());
        }

        // abattre les groupe de "DBandeRegGlobale_*Pmax/sum(Pmax)"
        // Uniquement s'il y a des defauts groupes
        miseAjourPmax(0.0); // Pas d'indispo sur le cas de base
    }

    // Construction du nombre de variable
    //---------------------------------
    nbVarTd_ = 2 * nbTd_; //(x+ et x-)
    nbVarCc_ = 2 * nbCC_;

    ss.str("");
    ss << err::ioDico().msg("INFOGeneralReseau") << std::endl;
    ss << err::ioDico().msg("INFODemiBandeReglGlobale", c_fmt("%f", DBandeRegGlobale_)) << std::endl;
    if (!defautsGroupesPresents_) {
        ss << err::ioDico().msg("INFODemiBandeReglGlobaleIgnoree") << std::endl;
    }

    ss << err::ioDico().msg("INFOPmaxPerdueSurIncident", c_fmt("%10.0f", pMaxIncident)) << std::endl;
    ss << err::ioDico().msg("INFONbIncidents", c_fmt("%d", nbIncValides), c_fmt("%d", nbIncidents_)) << std::endl;
    ss << err::ioDico().msg("INFONbRegions", c_fmt("%d", nbRegions_)) << std::endl;
    ss << err::ioDico().msg("INFONbSommets", c_fmt("%d", nbNoeuds_)) << std::endl;
    ss << err::ioDico().msg("INFONbConsos", c_fmt("%d", nbConsos_), c_fmt("%d", nbConsosCuratifs_)) << std::endl;
    ss << err::ioDico().msg("INFONbQuads", c_fmt("%d", nbQuads_), c_fmt("%d", nbQuadSurvN_), c_fmt("%d", nbQuadSurvNk_))
       << std::endl;
    ss << err::ioDico().msg("INFONbGroupes", c_fmt("%d", nbGroupes_), c_fmt("%d", nbGroupesCuratifs_)) << std::endl;
    ss << err::ioDico().msg("INFONbTDs", c_fmt("%d", nbTd_ - nbCCEmulAC_), c_fmt("%d", nbTdCuratif_ - nbCCEmulAC_))
       << std::endl;
    ss << err::ioDico().msg("INFONbHVDC", c_fmt("%d", nbCC_), c_fmt("%d", nbCCCuratif_), c_fmt("%d", nbCCEmulAC_))
       << std::endl;
    ss << err::ioDico().msg("INFOCoeffPerte", c_fmt("%.2f", coeff_pertes_)) << std::endl;
    if (config::configuration().useItam()) {
        ss << err::ioDico().msg("INFOPriseEnCompteItam") << std::endl;
    }
    if (config::configuration().useCurative()) {
        ss << err::ioDico().msg("INFONbMaxActionCurative", c_fmt("%d", config::configuration().nbMaxActionCurative()))
           << std::endl;
    } else {
        ss << err::ioDico().msg("INFOPasLimiteActionCurative") << std::endl;
    }
    if (config::configuration().limitCurativeGrp() >= 0) {
        ss << err::ioDico().msg("INFOLimiteRedispatchingCuratif",
                                c_fmt("%d", config::configuration().limitCurativeGrp()))
           << std::endl;
    }
    if (config::configuration().maxRelancePertes() > 0) {
        ss << err::ioDico().msg("INFOMaxRelancePerte", c_fmt("%d", config::configuration().maxRelancePertes()))
           << std::endl;
        ss << err::ioDico().msg("INFOSeuilRelancePerte", c_fmt("%d", config::configuration().thresholdRelancePertes()))
           << std::endl;
    }
    if (nbGroupesCouples_ > 0) {
        ss << err::ioDico().msg("INFONbGroupesCouples", c_fmt("%d", nbGroupesCouples_)) << std::endl;
    }
    if (nbConsosCouplees_ > 0) {
        ss << err::ioDico().msg("INFONbConsosCouplees", c_fmt("%d", nbConsosCouplees_)) << std::endl;
    }
    ss << err::ioDico().msg("INFONbComposantesSynchrones", c_fmt("%d", numNoeudBilanParZone_.size())) << std::endl;
    // Infos sur la penalisations des TDs et HVDCs
    //--------------------------------------------
    if (config::configuration().usePenalisationTD()) {
        ss << err::ioDico().msg("INFOPenalisationTD", c_fmt("%g", config::configuration().costTd())) << std::endl;
    }
    if (config::configuration().usePenalisationHVDC()) {
        ss << err::ioDico().msg("INFOPenalisationHVDC", c_fmt("%g", config::configuration().costHvdc())) << std::endl;
    }
    if ((!config::configuration().usePenalisationTD()) && (!config::configuration().usePenalisationHVDC())) {
        ss << err::ioDico().msg("INFOPasDePenalisationTDHVDC") << std::endl;
    }

    // type de resultats
    // ------------------
    if (config::configuration().displayResultatsSurcharges()) {
        ss << err::ioDico().msg("INFOResultatsSurchargesUniquement") << std::endl;
    }

    LOG_ALL(info) << ss.str();
}


Noeud::Noeud(int num, int numRegion) : num_(num), numRegion_(numRegion) {}

Noeud::PositionConnexion Noeud::position(const std::shared_ptr<Connexion>& branche) const
{
    return num_ == branche->norqua_->num_ ? ORIGINE : EXTREMITE;
}

string Noeud::print() const
{
    string txt = c_fmt("%d", num_);
    if (nbConsos_ > 0) {
        txt += " [" + listeConsos_[0]->nom_;
        for (int i = 1; i < nbConsos_; i++) {
            txt += ", " + listeConsos_[i]->nom_;
        }
        txt += "]";
    }
    return txt;
}

double Noeud::consoNodale()
{
    double consoNodale = 0.;
    for (int i = 0; i < nbConsos_; ++i) {
        consoNodale += listeConsos_[i]->valeur_;
    }
    return consoNodale;
}


Connexion::Connexion(const std::shared_ptr<Noeud>& nor, const std::shared_ptr<Noeud>& nex) : norqua_(nor), nexqua_(nex)
{
    etatOr_ = etatOrBase_;
    etatEx_ = etatExBase_;
}

Quadripole::Quadripole(int num,
                       const string& nom,
                       const std::shared_ptr<Noeud>& norqua,
                       const std::shared_ptr<Noeud>& nexqua,
                       double y,
                       double r) :
    Connexion(norqua, nexqua),
    nom_(nom),
    num_(num),
    y_(y),
    r_(r),
    u2Yij_(config::configuration().uRef() * config::configuration().uRef() * y)
{
    reconnectable_ = tnnorqua() != tnnexqua();

    norqua_->listeQuads_.push_back(this);
    nexqua_->listeQuads_.push_back(this);

    if (fabs(y_) < 0.00001) {
        LOG_ALL(warning) << err::ioDico().msg("WARNAdmitFaible", nom_, c_fmt("%1.10f", u2Yij_));
    }
    if (u2Yij_ > 6000000) {
        LOG_ALL(warning) << err::ioDico().msg("WARNAdmitGrande", nom_, c_fmt("%10.10f", y_));
    }

    if (tnnorqua() == tnnexqua()) {
        // La ligne boucle sur elle même, on l'ouvre
        LOG_ALL(warning) << err::ioDico().msg("WARNQuadBoucle", nom_);
        etatOr_ = false;
        etatOrBase_ = false;
    }
}

ElementASurveiller::ElementASurveiller(const string& nom,
                                       int num,
                                       TypeSurveillance survN,
                                       TypeSurveillance survInc,
                                       double seuilN,
                                       double seuilInc,
                                       double seuilIncComplexe,
                                       double seuilAvantCuratif,
                                       bool isWatchedSection) :
    nom_(nom),
    num_(num),
    isWatchedSection(isWatchedSection),
    survMaxN_(survN),
    survMaxInc_(survInc),
    seuilMaxN_(seuilN),
    seuilMaxInc_(seuilInc),
    seuilMaxIncComplexe_(seuilIncComplexe),
    seuilMaxAvantCur_(seuilAvantCuratif),
    seuilMaxAvantCurIncComplexe_(seuilAvantCuratif)
{
}

double ElementASurveiller::seuilMax(const std::shared_ptr<Incident>& icdt) const
{
    const auto& config = config::configuration();
    if (!icdt) {
        return seuilMaxN_;
    }

    if (icdt->parade_) {
        if (icdt->incTraiteCur_->incidentComplexe_ && seuilMaxIncComplexe_ != config::constants::valdef) {
            return seuilMaxIncComplexe_;
        }

        return seuilMaxInc_;
    }

    if (icdt->paradesActivees_) {
        if (icdt->incidentComplexe_ && seuilMaxAvantCurIncComplexe_ != config::constants::valdef) {
            return seuilMaxAvantCurIncComplexe_;
        }

        return seuilMaxAvantCur_;
    }

    if (icdt->incidentComplexe_ && seuilMaxIncComplexe_ != config::constants::valdef) {
        return config.thresholdMaxITAM(seuilMaxIncComplexe_, seuilMaxAvantCurIncComplexe_);
    }

    return config.thresholdMaxITAM(seuilMaxInc_, seuilMaxAvantCur_);
}

double ElementASurveiller::seuilMin(const std::shared_ptr<Incident>& icdt) const
{
    const auto& config = config::configuration();
    auto checkThreshold = [&icdt, this](double threshold) {
        return (threshold != config::constants::valdef) ? -threshold : -seuilMax(icdt);
    };
    if (seuilsAssymetriques_) {
        if (!icdt) {
            return -seuilMaxNExOr_;
        }

        if (icdt->parade_) {
            if (icdt->incTraiteCur_->incidentComplexe_) {
                return checkThreshold(seuilMaxIncComplexeExOr_);
            }
            return checkThreshold(seuilMaxIncExOr_);
        }

        if (icdt->paradesActivees_) {
            if (icdt->incidentComplexe_) {
                return checkThreshold(seuilMaxAvantCurIncComplexeExOr_);
            }
            return checkThreshold(seuilMaxAvantCurExOr_);
        }

        if (icdt->incidentComplexe_) {
            double threshold = config.thresholdMaxITAM(seuilMaxIncComplexeExOr_, seuilMaxAvantCurIncComplexeExOr_);
            return checkThreshold(threshold);
        }

        double threshold = config.thresholdMaxITAM(seuilMaxIncExOr_, seuilMaxAvantCurExOr_);
        return checkThreshold(threshold);
    }

    return -seuilMax(icdt);
}

double ElementASurveiller::seuil(const std::shared_ptr<Incident>& icdt, double transit) const
{
    return transit >= 0 ? seuilMax(icdt) : seuilMin(icdt);
}

void ElementASurveiller::verificationSeuils() const
{
    if (seuilMaxN_ <= 0 && (survMaxN_ == ElementASurveiller::SURVEILLE)) {
        LOG_ALL(warning) << err::ioDico().msg("WARNLimiteCourantNegative", nom_, "N", c_fmt("%5.1f", seuilMaxN_));
    }
    if (survMaxInc_ == ElementASurveiller::SURVEILLE) {
        if (seuilMaxIncComplexe_ <= 0) {
            LOG_ALL(warning) << err::ioDico().msg(
                "WARNLimiteCourantNegative", nom_, "N-k", c_fmt("%5.1f", seuilMaxIncComplexe_));
        }
        if (seuilMaxInc_ <= 0) {
            LOG_ALL(warning) << err::ioDico().msg(
                "WARNLimiteCourantNegative", nom_, "N-1", c_fmt("%5.1f", seuilMaxInc_));
        }
        if (seuilMaxIncComplexe_ < seuilMaxN_ - config::constants::epsilon) {
            LOG_ALL(warning) << err::ioDico().msg("WARNLimiteCourantPlusContraignante",
                                                  nom_,
                                                  "N-k",
                                                  c_fmt("%4.1f", seuilMaxIncComplexe_),
                                                  "N",
                                                  c_fmt("%4.1f", seuilMaxN_));
        }
        if (seuilMaxInc_ < seuilMaxN_ - config::constants::epsilon) {
            LOG_ALL(warning) << err::ioDico().msg("WARNLimiteCourantPlusContraignante",
                                                  nom_,
                                                  "N-1",
                                                  c_fmt("%4.1f", seuilMaxInc_),
                                                  "N",
                                                  c_fmt("%4.1f", seuilMaxN_));
        }
        if (config::configuration().useItam()) {
            if (seuilMaxAvantCur_ < seuilMaxInc_ - config::constants::epsilon) {
                LOG_ALL(warning) << err::ioDico().msg("WARNLimiteCourantPlusContraignante",
                                                      nom_,
                                                      "avant curatif",
                                                      c_fmt("%4.1f", seuilMaxAvantCur_),
                                                      "N-1",
                                                      c_fmt("%4.1f", seuilMaxInc_));
            }
            if (seuilMaxAvantCur_ < seuilMaxIncComplexe_ - config::constants::epsilon) {
                LOG_ALL(warning) << err::ioDico().msg("WARNLimiteCourantPlusContraignante",
                                                      nom_,
                                                      "avant curatif",
                                                      c_fmt("%4.1f", seuilMaxAvantCur_),
                                                      "N-k",
                                                      c_fmt("%4.1f", seuilMaxIncComplexe_));
            }
        }
    }
}


Groupe::Groupe(int num,
               const string& nom,
               const std::shared_ptr<Noeud>& noeud,
               int type,
               float prodN,
               float puisMin,
               float puisMaxDispo,
               float demiBande,
               Groupe::ProdAjustable pimpmod) :
    nom_(nom),
    num_(num),
    numNoeud_{noeud->num_},
    noeud_(noeud),
    type_(type),
    prodAjust_(pimpmod),
    puisMinBase_(puisMin),
    puisMinAR_(puisMin),
    demiBande_(demiBande)
{
    noeud->listeGroupes_.push_back(this);
    noeud->nbGroupes_++;

    puisMin_ = (puisMinAR_ >= 0) ? 0 : puisMinAR_;

    if (puisMinBase_ > 0) {
        LOG(debug) << "Groupe " << nom_ << " : puisMin : " << puisMinBase_;
    }

    puisMaxDispoBase_ = puisMaxDispo;
    puisMaxDispo_ = puisMaxDispoBase_;
    puisMax_ = puisMaxDispoBase_;

    etat_ = etatBase_;
    prodPobj_ = prodN;
    prodPobjBase_ = prodPobj_;

    checkCoherencePminMaxObj();
}

int Groupe::checkCoherencePminMaxObj()
{
    if (!etat_) {
        prodPobj_ = 0.0;
    }

    if (puisMax_ < puisMinAR_) {
        LOG_ALL(error) << err::ioDico().msg("ERRGroupePminPmax", nom_, c_fmt("%f", puisMinAR_), c_fmt("%f", puisMax_));
        return METRIX_PROBLEME;
    }

    if (prodAjust_ != Groupe::NON_HR_AR || prodPobj_ != 0.0) { // cas des groupes dont la production est fixee
        if (prodPobj_ > puisMax_) {
            LOG_ALL(warning) << err::ioDico().msg(
                "WARNPobjInferieurPmax", nom_, c_fmt("%f", prodPobj_), c_fmt("%f", puisMax_));
            prodPobj_ = puisMax_; // si le groupe doit démarrer en dessus de Pmax il démarre é Pmax
        } else if (prodPobj_ < puisMin_) {
            LOG_ALL(warning) << err::ioDico().msg(
                "WARNPobjSuperieurPmin", nom_, c_fmt("%f", prodPobj_), c_fmt("%f", puisMin_));
            prodPobj_ = puisMin_; // si le groupe doit démarrer en dessous de Pmin il démarre é Pmin
        }
    }
    prod_ = prodPobj_;

    return METRIX_PAS_PROBLEME;
}


Consommation::Consommation(int num, const string& nom, const std::shared_ptr<Noeud>& noeud, float valeur) :
    num_(num),
    nom_(nom),
    noeud_(noeud),
    valeur_(valeur),
    valeurBase_(valeur)
{
    noeud_->listeConsos_.push_back(this);
    noeud->nbConsos_++;
}

TransformateurDephaseur::TransformateurDephaseur(int unsigned num,
                                                 const std::shared_ptr<Quadripole>& quadTd,
                                                 const std::shared_ptr<Quadripole>& quadVrai,
                                                 double pCons,
                                                 double pMin,
                                                 double pMax,
                                                 TypePilotageTD pilotage,
                                                 ModeCuratif mode,
                                                 int lowtap,
                                                 int nbtap,
                                                 const vector<float>& tapdepha,
                                                 int lowran,
                                                 int uppran) :
    num_(num),
    quad_(quadTd),
    quadVrai_(quadVrai),
    type_(pilotage),
    mode_(mode),
    lowtap_(lowtap),
    nbtap_(nbtap),
    tapdepha_(tapdepha),
    lowran_(lowran),
    uppran_(uppran)
{
    switch (pilotage) {
        case HORS_SERVICE:
            puiCons_ = 0.0;
            puiMin_ = 0.0;
            puiMax_ = 0.0;
            break;
        case PILOTAGE_ANGLE_OPTIMISE:
            puiCons_ = angle2Power(pCons);
            puiMin_ = angle2Power(pMin);
            puiMax_ = angle2Power(pMax);
            break;
        case PILOTAGE_ANGLE_IMPOSE:
            puiCons_ = angle2Power(pCons);
            puiMin_ = puiCons_;
            puiMax_ = puiCons_;
            break;
        case PILOTAGE_PUISSANCE_OPTIMISE:
            puiCons_ = pCons;
            puiMin_ = pMin;
            puiMax_ = pMax;
            break;
        case PILOTAGE_PUISSANCE_IMPOSE:
            puiCons_ = pCons;
            puiMin_ = puiCons_;
            puiMax_ = puiCons_;
            break;
        default:
            ostringstream errMsg;
            errMsg << err::ioDico().msg("ERRDonneeAnormaleTD", quadVrai_->nom_);
            throw ErrorI(errMsg.str());
    }

    puiConsBase_ = puiCons_;

    if (pilotage == TransformateurDephaseur::PILOTAGE_PUISSANCE_OPTIMISE
        || pilotage == TransformateurDephaseur::PILOTAGE_PUISSANCE_IMPOSE) {
        // En mode pilotage en puissance, le quad fictif est deconnecté
        quad_->etatOr_ = false;
        quad_->etatEx_ = false;
        quad_->etatOrBase_ = false;
        quad_->etatExBase_ = false;
    }

    if (puiCons_ < puiMin_ || puiCons_ > puiMax_) {
        ostringstream errMsg;
        errMsg << err::ioDico().msg("ERRPuissanceConsigneTD", quadVrai_->nom_);
        throw ErrorI(errMsg.str());
    }
}

double TransformateurDephaseur::angle2Power(double angle) const
{
    return angle * config::constants::pi / 180.0 * quad_->u2Yij_;
}

double TransformateurDephaseur::power2Angle(double power) const
{
    return power / quad_->u2Yij_ * 180.0 / config::constants::pi;
}


LigneCC::LigneCC(int unsigned num,
                 const string& nom,
                 const std::shared_ptr<Noeud>& nor,
                 const std::shared_ptr<Noeud>& nex,
                 double pMin,
                 double pMax,
                 double de0,
                 double coeffPerteOr,
                 double coeffPerteEx,
                 double r,
                 double vdc) :
    Connexion(nor, nex),
    num_(num),
    nom_(nom),
    puiMin_(pMin),
    puiMax_(pMax),
    puiCons_(de0),
    puiMinBase_(pMin),
    puiMaxBase_(pMax),
    puiConsBase_(de0),
    coeffPertesOr_(coeffPerteOr),
    coeffPertesEx_(coeffPerteEx),
    r_(r),
    vdc_(vdc)
{
}

bool LigneCC::isEmulationAC() const
{
    return type_ == PILOTAGE_EMULATION_AC || type_ == PILOTAGE_EMULATION_AC_OPTIMISE;
}

//--------------------
// Reseau::interfaceDIE
//--------------------

int Reseau::modifReseauTopo(const Quadripole::SetQuadripoleSortedByName& quads)
{
    // Indisponibilites de lignes (et verification de la connexite de la variante)
    //---------------------------------------------------------------------------------
    if (!quads.empty()) {
        for (auto& ligne : quads) {
            ligne->etatOr_ = false;
            ligne->etatEx_ = false;
        }
        calculerCoeffReport_ = true;

        // verification de la connexite du reseau + recalcul des noeuds bilan
        for (const auto& pair : numNoeudBilanParZone_) {
            noeuds_[pair.second]->bilan_ = false;
        }
        numNoeudBilanParZone_.clear();

        if (!connexite()) {
            LOG_ALL(error) << err::ioDico().msg("ERRIndispoLigneVarConnexite", "N/A");
            return METRIX_PROBLEME;
        }

        incidentsRompantConnexite_.clear();

        // verification de la validite des incidents
        for (const auto& inc : incidentsEtParades_) {
            if (!inc->validite_) {
                continue;
            }

            bool ruptureConnexite = config::configuration().useIncRompantConnexite();
            if (inc->parade_) {
                ruptureConnexite = inc->incTraiteCur_->pochePerdue_ != nullptr
                                   || config::configuration().useParRompantConnexite();
            }
            connexite(inc, false, ruptureConnexite);

            if (!inc->validite_) {
                LOG_ALL(error) << err::ioDico().msg("ERRIncidentIgnoreVarConnexite", c_fmt("%d", inc->num_));
            }

            // Adaptation de la jacobienne fait ensuite dans resolutionProbleme: modifJacobienne
            // factoriser la jacobienne
            // les coeff de reports dans coeffReportsInfluencement

        } // end loi d indispo ligne
    }
    return METRIX_PAS_PROBLEME;
}


int Reseau::modifReseau(const std::shared_ptr<Variante>& var)
{
    // I-Indispo groupes
    //------------------
    // II-1 modification de l'etat et la production objectif
    double prodEnMoins = 0.0;

    for (const auto& prod : var->grpIndispo_) {
        if (prod->prodAjust_ == Groupe::OUI_HR_AR || prod->prodAjust_ == Groupe::OUI_HR) {
            prodEnMoins += prod->puisMaxDispo_;
        }
        prod->etat_ = false;
    }


    // I-2 : Modif pmax et pmin
    //------------------------
    for (const auto& elem : var->prodMax_) {
        const auto& prod = elem.first;
        prod->puisMaxDispo_ = elem.second;
        prod->puisMax_ = elem.second;
        if (prod->prodAjust_ == Groupe::OUI_HR_AR || prod->prodAjust_ == Groupe::OUI_HR) {
            prodEnMoins += (prod->puisMaxDispoBase_ - prod->puisMaxDispo_);
        }
        LOG(debug) << "la prod max du groupe : " << prod->nom_ << " est modifee, nouvelle valeur : " << prod->puisMax_;
    }

    for (const auto& elem : var->prodMin_) {
        elem.first->puisMinAR_ = elem.second;
        // prod->puisMin_ = (prod->puisMinAR_ > 0)?0:prod->puisMinAR_; // deja dans le modifBilan
        LOG(debug) << "la prod min AR du groupe : " << elem.first->nom_
                   << " est modifee, nouvelle valeur : " << elem.second;
    }

    // I-3 : mise a jour des Pmax des groupes pour le reglage de frequence suite a l'indisponibilite des groupes de
    // la variante courante
    if ((var->nbGrpIndispo() > 0 || var->nbProdMax() > 0) && defautsGroupesPresents_) {
        miseAjourPmax(prodEnMoins);
    }

    // II-productions imposees
    //------------------------
    for (const auto& elem : var->prodImpose_) {
        elem.first->prodPobj_ = elem.second;

        // prod->checkCoherencePminMaxObj();
        LOG(debug) << "la prod impose : " << elem.first->nom_ << " est modifee, nouvelle valeur : " << elem.second;
    }


    // III-variation de la consommation
    //-------------------------------
    for (const auto& elem : var->valeurConso_) {
        elem.first->valeur_ = elem.second;

        LOG(debug) << "la conso : " << elem.first->nom_ << " est modifee, nouvelle valeur : " << elem.first->valeur_;
    }

    // IV-variation des couts a la hausse sans reseau
    //----------------------------------------------
    for (const auto& elem : var->grpHausseHR_) {
        elem.first->coutHausseHR_ = elem.second;

        LOG(debug) << "le cout a la hausse  : " << elem.first->nom_
                   << " est modife, nouvelle valeur est: " << elem.second;
    }

    // V-variation des couts a la baisse sans reseau
    //----------------------------------------------
    for (const auto& elem : var->grpBaisseHR_) {
        elem.first->coutBaisseHR_ = elem.second;

        LOG(debug) << "le cout a la baisse  : " << elem.first->nom_
                   << " est modife, nouvelle valeur est: " << elem.second;
    }
    // VII-variation des couts a la hausse avec reseau
    //----------------------------------------------
    for (const auto& elem : var->grpHausseAR_) {
        elem.first->coutHausseAR_ = elem.second;

        LOG(debug) << "le cout a la hausse AR : " << elem.first->nom_
                   << " est modife, nouvelle valeur est: " << elem.second;
    }
    // VIII-variation des couts a la baisse avec reseau
    //----------------------------------------------
    for (const auto& elem : var->grpBaisseAR_) {
        elem.first->coutBaisseAR_ = elem.second;

        LOG(debug) << "le cout a la baisse AR : " << elem.first->nom_
                   << " est modife, nouvelle valeur est: " << elem.second;
    }

    // IX - variation des couts d'effacement
    //----------------------------------------------
    for (const auto& elem : var->coutEfface_) {
        elem.first->coutEffacement_ = elem.second;

        LOG(debug) << "le cout d'effacement : " << elem.first->nom_
                   << " est modife, nouvelle valeur est: " << elem.first->coutEffacement_;
    }

    // X - bilan zonal en jouant sur la consommation
    //-------------------------------------------------------
    double bilanCourant = 0.0; // bilan actuel apres application de toutes les autres lois
    double bilanCible = 0.0;   // bilan visé par la variante
    double sumC = 0.0;         // somme des consommations

    for (const auto& elem : var->valeurEchange_) {
        bilanCible = elem.second;

        bilanCourant = 0.0;
        sumC = 0.0;

        LOG(debug) << " bilan d'echange sur region num : " << elem.first
                   << ", nouvelle valeur d echange : " << bilanCible;
        // calcul de echCourant
        for (auto grpIt = groupes_.cbegin(); grpIt != groupes_.end(); ++grpIt) {
            const auto& prod = grpIt->second;
            if (noeuds_[prod->numNoeud_]->numRegion_ != elem.first || !prod->etat_) {
                continue;
            }

            if (prod->prodAjust_ == Groupe::OUI_HR_AR || prod->prodAjust_ == Groupe::OUI_HR) {
                // cette loi suppose que bilan du pays = prod impose - conso
                // cela suppose donc qu il n y a pas de grp dispatchables
                // on rejete la variante s'il y a un grp dispatchable
                LOG_ALL(error) << err::ioDico().msg("ERRApplLoiBilanGrpNonImp", prod->nom_);
                return METRIX_PROBLEME;
            }
            bilanCourant += prod->prod_;
        }

        for (auto cIt = consos_.cbegin(); cIt != consos_.end(); ++cIt) {
            const auto& conso = cIt->second;

            if (conso->noeud_->numRegion_ != elem.first) {
                continue;
            }

            bilanCourant -= conso->valeur_;
            sumC += conso->valeur_;
        }

        if (bilanCourant - bilanCible == 0.0) {
            continue;
        }

        // mise a jour des consommations de la region var->numRegion_[k]
        for (auto cIt = consos_.cbegin(); cIt != consos_.end(); ++cIt) {
            const auto& conso = cIt->second;

            if (conso->noeud_->numRegion_ != elem.first) {
                continue;
            }

            conso->valeur_ += conso->valeur_ * (bilanCourant - bilanCible) / sumC;
        }
        // cout<< "region "<<var->numRegion_[k]<<"  export init "<<bilanCourant<< " cible "<<bilanCible<<" new
        // Conso"<<sumCtmp1<<" old conso "<<sumC<<endl;
    }

    // XI-Limitations Pmin et Pmax des HVDCs
    //--------------------------------------
    for (auto lccIt = var->dcPuissMin_.cbegin(); lccIt != var->dcPuissMin_.end(); ++lccIt) {
        const auto& lcc = lccIt->first;
        lcc->puiMin_ = lccIt->second;

        if (lcc->isEmulationAC()) {
            // Mise a jour du quad fictif surveille
            const auto& elemAS = lcc->quadFictif_->elemAS_;
            double minT = -lcc->puiMin_;
            elemAS->seuilMaxNExOr_ = minT;
            elemAS->seuilMaxIncExOr_ = minT;
            elemAS->seuilMaxIncComplexeExOr_ = minT;
            elemAS->seuilMaxAvantCurExOr_ = minT;
            elemAS->seuilMaxAvantCurIncComplexeExOr_ = minT;
        }
    }

    for (auto& elem : var->dcPuissMax_) {
        const auto& lcc = elem.first;
        lcc->puiMax_ = elem.second;

        if (lcc->isEmulationAC()) {
            // Mise à jour du quad fictif surveillé
            auto& elemAS = lcc->quadFictif_->elemAS_;
            elemAS->seuilMaxN_ = lcc->puiMax_;
            elemAS->seuilMaxInc_ = lcc->puiMax_;
            elemAS->seuilMaxIncComplexe_ = lcc->puiMax_;
            elemAS->seuilMaxAvantCur_ = lcc->puiMax_;
        }
    }

    // XII-Puissance de consigne des HVDCs
    //-----------------------------------
    for (const auto& elem : var->dcPuissImp_) {
        // Modification de la puissance de consigne de la LCC
        elem.first->puiCons_ = elem.second;
    }

    // Vérification PCons, Pmin et Pmax (si pilotage puissance imposée)
    // ----------------------------------------------------------------
    for (auto lIt = LigneCCs_.cbegin(); lIt != LigneCCs_.end(); ++lIt) {
        const auto& lcc = lIt->second;
        if ((lcc->puiCons_ < lcc->puiMin_) || (lcc->puiCons_ > lcc->puiMax_)) {
            LOG_ALL(error) << err::ioDico().msg("ERRHVDCPminPmaxImpose", lcc->nom_);
            return METRIX_PROBLEME;
        }
    }

    // XIII-Dephasage des TDs
    //------------------------
    for (const auto& elem : var->dtValDep_) {
        const auto& td = elem.first;
        if (td->type_ == TransformateurDephaseur::HORS_SERVICE) {
            LOG_ALL(warning) << err::ioDico().msg("WARNTdHs", td->quadVrai_->nom_);
            continue;
        }
        td->puiCons_ = elem.second;
    }

    // XIV-Seuils des quadripoles
    //----------------------------
    for (const auto& elem : var->quati00mn_) {
        // Modification du seuil N
        const auto& quad = elem.first;
        quad->seuilMaxN_ = elem.second;
    }
    for (const auto& elem : var->quati5mns_) {
        // Modification du seuil N-1
        const auto& quad = elem.first;
        quad->seuilMaxInc_ = elem.second;
    }
    for (const auto& elem : var->quati20mn_) {
        // Modification du seuil N-k
        const auto& quad = elem.first;
        quad->seuilMaxIncComplexe_ = elem.second;
    }
    for (auto& elem : var->quatitamn_) {
        // Modification du seuil ITAM
        auto& quad = elem.first;
        quad->seuilMaxAvantCur_ = elem.second;
    }
    for (const auto& elem : var->quatitamk_) {
        // Modification du seuil ITAM complexe
        const auto& quad = elem.first;
        quad->seuilMaxAvantCurIncComplexe_ = elem.second;
    }

    // Seuils Extremite -> Origine
    for (const auto& elem : var->quati00mnExOr_) {
        // Modification du seuil N
        const auto& quad = elem.first;
        quad->seuilMaxNExOr_ = elem.second;
    }
    for (const auto& elem : var->quati5mnsExOr_) {
        // Modification du seuil N-1
        const auto& quad = elem.first;
        quad->seuilMaxIncExOr_ = elem.second;
    }
    for (const auto& elem : var->quati20mnExOr_) {
        // Modification du seuil N-k
        const auto& quad = elem.first;
        quad->seuilMaxIncComplexeExOr_ = elem.second;
    }
    for (const auto& elem : var->quatitamnExOr_) {
        // Modification du seuil ITAM
        const auto& quad = elem.first;
        quad->seuilMaxAvantCurExOr_ = elem.second;
    }
    for (const auto& elem : var->quatitamkExOr_) {
        // Modification du seuil ITAM complexe
        const auto& quad = elem.first;
        quad->seuilMaxAvantCurIncComplexeExOr_ = elem.second;
    }

    // XV-Probabilite des incidents
    //----------------------------
    for (const auto& elem : var->probabinc_) {
        // Modification de la probabilite de défaut
        const auto& icdt = elem.first;
        icdt->probabilite_ = elem.second;
    }

    // A la fin : Application des bilans zonaux (ex-X)
    return modifBilans(var);
}


/**
 * Modification des bilans zonaux de la variante en jouant sur la production par merit order
 */
int Reseau::modifBilans(const std::shared_ptr<Variante>& var)
{
    // Modification des Pmin > 0 pour la phase HR et vérification Pmin, Pmax, Pobj
    bool ok = true;
    for (auto grpIt = groupes_.cbegin(); grpIt != groupes_.end(); ++grpIt) {
        const auto& grp = grpIt->second;
        grp->puisMin_ = grp->puisMinAR_ >= 0 ? 0 : grp->puisMinAR_;
        ok &= (grp->checkCoherencePminMaxObj() != METRIX_PROBLEME);
    }

    if (!ok) {
        LOG_ALL(error) << err::ioDico().msg("ERRGroupesPminPmax");
        return METRIX_PROBLEME;
    }

    double bilanCourant = 0.0; // bilan actuel apres application de toutes les autres lois
    double bilanCible = 0.0;   // bilan vis par la variante
    double sumC = 0.0;         // somme des consommations
    int numRegion = -1;
    vector<std::shared_ptr<Groupe>> groupesDeLaZone;
    std::map<int, vector<std::shared_ptr<Groupe>>>::const_iterator grpZone;

    for (const auto& elem : var->varBilanProd_) {
        numRegion = elem.first;
        bilanCible = elem.second;
        bilanCourant = 0.0;
        sumC = 0.0;

        if ((grpZone = varGroupesZones_.find(numRegion)) != varGroupesZones_.end()) {
            groupesDeLaZone = grpZone->second;
            // Calcul de la production initiale sur la zone
            for (auto grpIt = groupes_.cbegin(); grpIt != groupes_.end(); ++grpIt) {
                const auto& prod = grpIt->second;
                if (!prod->etat_ || prod->noeud_->numRegion_ != numRegion) {
                    continue;
                }
                bilanCourant += prod->prodPobj_;
            }
        } else {
            groupesDeLaZone.clear();

            // Calcul de la production initiale sur la zone
            for (auto grpIt = groupes_.cbegin(); grpIt != groupes_.end(); ++grpIt) {
                const auto& prod = grpIt->second;
                if (!prod->etat_ || prod->noeud_->numRegion_ != numRegion) {
                    continue;
                }

                bilanCourant += prod->prodPobj_;

                if (prod->prodAjust_ == Groupe::OUI_HR_AR || prod->prodAjust_ == Groupe::OUI_HR) {
                    // Groupe non impose qui participera a l'empilement au Merit Order
                    groupesDeLaZone.push_back(prod);
                }
            }

            // Bruitage des groupes
            std::shuffle(groupesDeLaZone.begin(), groupesDeLaZone.end(), random);
            varGroupesZones_[numRegion] = groupesDeLaZone;
        }

        // Calcul de la somme des consommations de la zone
        for (auto cIt = consos_.cbegin(); cIt != consos_.end(); ++cIt) {
            const auto& conso = cIt->second;

            if (conso->noeud_->numRegion_ != numRegion) {
                continue;
            }

            sumC += conso->valeur_;
        }

        LOG(debug) << "Loi de bilan sur region num : " << numRegion << ", bilan cible : " << bilanCible
                   << ", conso : " << sumC << ", prod initiale : " << bilanCourant;

        bilanCourant -= sumC;
        double deltaBilan = bilanCourant - bilanCible;
        double prodDisp = 0.0;

        if (deltaBilan > 0.0) {
            // Trop de production on baisse les groupes les moins chers (CBHR)
            std::sort(groupesDeLaZone.begin(), groupesDeLaZone.end(), compareGroupeBaisse);
            for (const auto& prod : groupesDeLaZone) {
                prodDisp = max(prod->prodPobj_ - prod->puisMin_, 0.0); // puisMin_ <= 0
                if (prodDisp <= deltaBilan) {
                    prod->prodPobj_ = prod->puisMin_;
                    deltaBilan -= prodDisp;
                } else {
                    prod->prodPobj_ -= deltaBilan;
                    deltaBilan = 0.0;
                    break;
                }
            }
        } else if (deltaBilan < 0.0) {
            // Pas assez de production on empile les groupes les moins chers (CEHR)
            std::sort(groupesDeLaZone.begin(), groupesDeLaZone.end(), compareGroupeHausse);
            for (const auto& prod : groupesDeLaZone) {
                prodDisp = min(prod->prodPobj_ - prod->puisMax_, 0.0);
                if (prodDisp >= deltaBilan) {
                    prod->prodPobj_ = prod->puisMax_;
                    deltaBilan -= prodDisp;
                } else {
                    prod->prodPobj_ -= deltaBilan;
                    deltaBilan = 0.0;
                    break;
                }
            }
        }

        if (fabs(deltaBilan) > config::constants::epsilon_bilan) {
            // Recherche du nom de la region
            string nomZone = regions_[numRegion];
            LOG_ALL(error) << err::ioDico().msg("ERRApplLoiBilanDelta", nomZone, c_fmt("%f", deltaBilan));
            return METRIX_PROBLEME;
        }


        double debugBilan = 0.0;

        for (auto grpIt = groupes_.cbegin(); grpIt != groupes_.end(); ++grpIt) {
            const auto& prod = grpIt->second;
            if (prod->etat_ && noeuds_[prod->numNoeud_]->numRegion_ == numRegion) {
                debugBilan += prod->prodPobj_;
            }
        }
        for (auto cIt = consos_.cbegin(); cIt != consos_.end(); ++cIt) {
            const auto& conso = cIt->second;
            if (conso->noeud_->numRegion_ == numRegion) {
                debugBilan -= conso->valeur_;
            }
        }
        LOG(debug) << "bilan final : " << debugBilan;
    }

    return METRIX_PAS_PROBLEME;
}

void Reseau::miseAjourPmax(double prodEnMoins)
{
    // Pmax est evalue en fonction de PmaxDispo et de la reserve de frequence
    // Pmax doit étre mis a jour des que dans une variante, on reduit la puissance disponible
    // prodEnMoins : correspond au volume perdu par l indisponibilite des groupes de la variante consideree par rapport
    // au cas de base
    for (auto grpIt = groupes_.cbegin(); grpIt != groupes_.end(); ++grpIt) {
        const auto& grp = grpIt->second;
        if (!grp->etat_ || grp->prodAjust_ == Groupe::NON_HR_AR || grp->prodAjust_ == Groupe::OUI_AR) {
            continue;
        }
        grp->puisMax_ = grp->puisMaxDispo_ * (1 - DBandeRegGlobale_ / (prodMaxPossible_ - prodEnMoins));
    }
}

/**
 * Modification de toutes les consommations positives du réseau pour prendre en compte un nouveau taux de pertes
 */
int Reseau::modifTauxDePertes(float ancienTx, float nouveauTx)
{
    float coeff = (100 + nouveauTx) / (100 + ancienTx);
    for (auto cIt = consos_.cbegin(); cIt != consos_.end(); ++cIt) {
        const auto& conso = cIt->second;
        if (conso->valeur_ > 0) {
            conso->valeur_ *= coeff;
        }
    }
    return METRIX_PAS_PROBLEME;
}

/**
 * Remise du réseau dans l'état de base aprés le calcul d'une variante
 * si "toutesConsos" est à true, toutes les consommations sont réinitialisées (et pas seulement celles modifiées par
 * la variante)
 */
int Reseau::resetReseau(const std::shared_ptr<Variante>& var, bool toutesConsos)
{
    if (var) {
        // I-indispo groupes
        //-----------------

        // I-1-mise a jour de l etat de connection des groupes
        for (const auto& prod : var->grpIndispo_) {
            prod->etat_ = prod->etatBase_;
            prod->prodPobj_ = prod->prodPobjBase_;
            prod->prod_ = prod->prodPobjBase_;
            LOG(debug) << "Le groupe num " << prod->num_ << " est remis a jour a son etat de base (modif reseau)";
        }

        // I-2-Productions min et max
        //-------------------------
        for (auto groupesIt = var->prodMax_.cbegin(); groupesIt != var->prodMax_.end(); groupesIt++) {
            const auto& prod = groupesIt->first;
            prod->puisMaxDispo_ = prod->puisMaxDispoBase_;
            prod->puisMax_ = prod->puisMaxDispoBase_;
        }
        for (auto groupesIt = var->prodMin_.cbegin(); groupesIt != var->prodMin_.end(); groupesIt++) {
            const auto& prod = groupesIt->first;
            prod->puisMinAR_ = prod->puisMinBase_;
        }

        // I-3-mise a jour de la Pmax
        if ((var->nbGrpIndispo() > 0 || var->nbProdMax() > 0) && defautsGroupesPresents_) {
            miseAjourPmax(0.);
        }

        // II-Productions imposees
        //-----------------------

        for (auto groupesIt = var->prodImpose_.cbegin(); groupesIt != var->prodImpose_.end(); ++groupesIt) {
            const auto& prod = groupesIt->first;
            prod->prodPobj_ = prod->prodPobjBase_;
            prod->prod_ = prod->prodPobjBase_;
        }

        // III-variation de la consommation
        //-------------------------------
        if (!toutesConsos) { // sinon on le fait plus bas
            for (auto consosIt = var->valeurConso_.cbegin(); consosIt != var->valeurConso_.end(); ++consosIt) {
                const auto& conso = consosIt->first;
                conso->valeur_ = conso->valeurBase_;
            }
        }

        // VI-variation des echanges
        //-------------------------
        for (auto it = var->valeurEchange_.cbegin(); it != var->valeurEchange_.end(); ++it) {
            for (auto cIt = consos_.cbegin(); cIt != consos_.end(); ++cIt) {
                const auto& conso = cIt->second;
                if (conso->noeud_->numRegion_ != it->first) {
                    continue;
                }
                conso->valeur_ = conso->valeurBase_;
            }
        }

        // Couts a la baisse des consommations (effacements)
        for (auto consosIt = var->coutEfface_.cbegin(); consosIt != var->coutEfface_.end(); ++consosIt) {
            const auto& conso = consosIt->first;
            conso->coutEffacement_ = conso->coutEffacementBase_;

            LOG(debug) << "le cout a la baisse AR : " << conso->nom_ << " est remis a jour a son etat de base";
        }

        // X-limitation des HVDC
        //----------------------
        for (auto lccIt = var->dcPuissMin_.cbegin(); lccIt != var->dcPuissMin_.end(); ++lccIt) {
            const auto& lcc = lccIt->first;
            lcc->puiMin_ = lcc->puiMinBase_;

            if (lcc->isEmulationAC()) {
                // Mise a jour du quad fictif surveille
                auto& quad = lcc->quadFictif_->elemAS_;
                quad->seuilMaxNExOr_ = lcc->puiMin_;
                quad->seuilMaxIncExOr_ = lcc->puiMin_;
                quad->seuilMaxIncComplexeExOr_ = lcc->puiMin_;
                quad->seuilMaxAvantCurExOr_ = lcc->puiMin_;
            }

            LOG(debug) << "la Pmin de la liaison HVDC : " << lcc->nom_ << " est remis a jour a son etat de base";
        }

        for (auto lccIt = var->dcPuissMax_.cbegin(); lccIt != var->dcPuissMax_.end(); ++lccIt) {
            const auto& lcc = lccIt->first;
            lcc->puiMax_ = lcc->puiMaxBase_;

            if (lcc->isEmulationAC()) {
                // Mise a jour du quad fictif surveille
                const auto& quad = lcc->quadFictif_->elemAS_;
                quad->seuilMaxN_ = lcc->puiMax_;
                quad->seuilMaxInc_ = lcc->puiMax_;
                quad->seuilMaxIncComplexe_ = lcc->puiMax_;
                quad->seuilMaxAvantCur_ = lcc->puiMax_;
            }

            LOG(debug) << "la Pmax de la liaison HVDC : " << lcc->nom_ << " est remis a jour a son etat de base";
        }

        // XI-puissance de consigne des HVDC
        //----------------------------------
        for (auto lccIt = var->dcPuissImp_.cbegin(); lccIt != var->dcPuissImp_.end(); ++lccIt) {
            // Reset puissance de consigne
            const auto& lcc = lccIt->first;
            lcc->puiCons_ = lcc->puiConsBase_;
            LOG(debug) << "la consigne de puissance de la liaison HVDC : " << lcc->nom_
                       << " est remis a jour a son etat de base";
        }

        // XII-dephasage des TDs
        //----------------------
        for (auto tdIt = var->dtValDep_.cbegin(); tdIt != var->dtValDep_.end(); ++tdIt) {
            // Reset dephasage
            const auto& td = tdIt->first;
            td->puiCons_ = td->puiConsBase_;

            LOG(debug) << "le dephasage du TD : " << td->quadVrai_->nom_ << " est remis a jour a son etat de base";
        }

        // XIV - Bilans zonaux
        //--------------------
        int numRegion = -1;
        for (auto it = var->varBilanProd_.cbegin(); it != var->varBilanProd_.end(); ++it) {
            numRegion = it->first;

            for (auto grpIt = groupes_.cbegin(); grpIt != groupes_.end(); ++grpIt) {
                const auto& prod = grpIt->second;

                if (prod->noeud_->numRegion_ == numRegion) {
                    prod->prod_ = prod->prodPobjBase_;
                    prod->prodPobj_ = prod->prodPobjBase_;
                    LOG(debug) << "la puissance du groupe : " << prod->nom_ << " est remise a jour a son etat de base";
                }
            }
        }

        // XV - Probabilite d'incidents
        //--------------------
        for (auto icIt = var->probabinc_.cbegin(); icIt != var->probabinc_.end(); ++icIt) {
            const auto& icdt = icIt->first;
            icdt->probabilite_ = icdt->probabiliteBase_;
        }
    }

    // Reset des consos nodales
    if (toutesConsos) {
        for (auto cIt = consos_.cbegin(); cIt != consos_.end(); ++cIt) {
            const auto& conso = cIt->second;
            conso->valeur_ = conso->valeurBase_;
        }
    }

    // Reset sur les numeros d'incidents contraignants et les activations de parade
    for (const auto& inc : incidentsEtParades_) {
        inc->incidentATraiterEncuratif_ = false;
        inc->numVarActivation_ = -1;
        inc->paradesActivees_ = false;
        inc->contraintes_.clear();
        for (const auto& elem : inc->listeElemCur_) {
            elem->reset();
        }
    }

    // Reset des transits en N et N-k HR des quadripéles surveillés
    for (const auto& elemAS : elementsASurveiller_) {
        // suppression des variables d'écart précédentes
        elemAS->ecarts_.clear();
        elemAS->menacesMax_.clear();
        elemAS->menaceMaxAvantParade_.transit_ = config::constants::valdef;
        elemAS->menaceMaxAvantParade_.defaut_ = nullptr;
    }
    varGroupesZones_.clear();
    return METRIX_PAS_PROBLEME;
}

int Reseau::resetReseauTopo(const Quadripole::SetQuadripoleSortedByName& quads)
{
    if (!quads.empty()) {
        // Application des indispos lignes
        //----------------------------------------------
        for (const auto& ligne : quads) {
            ligne->etatOr_ = ligne->etatOrBase_;
            ligne->etatEx_ = ligne->etatExBase_;
        }
        calculerCoeffReport_ = true;

        // on recalcule la connexite surtout dans le but de replacer les noeuds bilan
        std::map<int, int>::const_iterator itNb;
        for (itNb = numNoeudBilanParZone_.cbegin(); itNb != numNoeudBilanParZone_.cend(); ++itNb) {
            noeuds_[itNb->second]->bilan_ = false;
        }
        numNoeudBilanParZone_.clear();
        if (!connexite()) {
            LOG_ALL(error) << err::ioDico().msg("ERRReseauNonConnexe");
            return METRIX_PROBLEME;
        }

        // On remet la validite des incidents a jour
        for (const auto& inc : incidentsEtParades_) {
            if (inc->validite_ != inc->validiteBase_) {
                inc->validite_ = inc->validiteBase_;

                if (inc->pochePerdue_) {
                    // incident rompant la connexité déclaré invalide dans cette variante
                    connexite(inc, false, config::configuration().useIncRompantConnexite());
                }
            }
        }

        vector<std::shared_ptr<Incident>> oldIncRompantConnexite = incidentsRompantConnexite_;
        incidentsRompantConnexite_.clear();
        // recalcul de la poche pour les incidents rompant la connexité
        for (const auto& elem : oldIncRompantConnexite) {
            connexite(elem, false, config::configuration().useIncRompantConnexite());
        }
    }
    return METRIX_PAS_PROBLEME;
}


std::shared_ptr<TransformateurDephaseur> Reseau::creerTD(const std::shared_ptr<Quadripole>& quadVrai,
                                                         int numTd,
                                                         int numNoeudFictif,
                                                         int numQuadFictif,
                                                         double pCons,
                                                         double pMin,
                                                         double pMax,
                                                         TransformateurDephaseur::TypePilotageTD typeP,
                                                         ModeCuratif curatif,
                                                         int lowtap,
                                                         int nbtap,
                                                         const vector<float>& tapdepha,
                                                         int lowran,
                                                         int uppran)
{
    // a- creation du nouveau noeud
    auto noeudVrai = quadVrai->norqua_;
    // cout<<"New Noeud td : "<<nomNoeud<<endl;
    auto noeudFictif = std::make_shared<Noeud>(numNoeudFictif, noeudVrai->numRegion_);
    noeuds_[numNoeudFictif] = noeudFictif;

    // b- creation d'un nouveau quadripole support du TD
    string nomQuad = quadVrai->nom_ + "_Td";
    auto quad = std::make_shared<Quadripole>(
        numQuadFictif, nomQuad, noeudVrai, noeudFictif, quadVrai->y_ / config::constants::parameter_ktd, 0.);
    quad->typeQuadripole_ = Quadripole::QUADRIPOLE_FICTIF;
    quads_.insert(std::pair<string, std::shared_ptr<Quadripole>>(quad->nom_, quad));

    // Pour les procédures de verif en debug on a besoin de calculer les coeffs de report de tous les quads
    if (config::inputConfiguration().checkConstraintLevel() != config::InputConfiguration::CheckConstraintLevel::NONE) {
        quadsSurv_.insert(quad);
    }

    noeudFictif->listeQuads_.push_back(quadVrai.get());
    noeudVrai->listeQuads_.erase(find(noeudVrai->listeQuads_.begin(), noeudVrai->listeQuads_.end(), quadVrai.get()));

    auto td = std::make_shared<TransformateurDephaseur>(
        numTd, quad, quadVrai, pCons, pMin, pMax, typeP, curatif, lowtap, nbtap, tapdepha, lowran, uppran);
    TransfoDephaseurs_.insert(std::pair<string, std::shared_ptr<TransformateurDephaseur>>(quadVrai->nom_, td));
    tdParIndice_.push_back(td);

    // c- modification du noeud origine du quadripole quadVrais qui etait support du TD, et qui ne l est plus.
    // notons que l'etat de connection du quadripole quadVrais ne change pas
    quadVrai->norqua_ = noeudFictif;
    quadVrai->y_ = quadVrai->y_ / (1 - config::constants::parameter_ktd);
    quadVrai->u2Yij_ = quadVrai->u2Yij_ / (1 - config::constants::parameter_ktd);

    // d- les lients td--quadripoles--noeuds
    quad->td_ = td;

    quad->norqua_->listeTd_.push_back(td);
    quad->nexqua_->listeTd_.push_back(td);

    return td;
}

void ElementCuratif::reset()
{
    positionVarCurative_ = -1;
    positionVarEntiereCur_ = -1;
}

int TransformateurDephaseur::getClosestTapPosition(double angleFinal)
{
    int closestTapPosition = nbtap_ - 1;
    double dif1 = angleFinal - tapdepha_[0];
    double dif2;
    for (int j = 1; j < nbtap_; ++j) {
        dif2 = angleFinal - tapdepha_[j];
        if (dif1 * dif2 <= 1.e-6) {
            closestTapPosition = fabs(dif1) <= fabs(dif2) ? j - 1 : j;
            break;
        }
        dif1 = dif2;
    }
    LOG(debug) << metrix::log::verbose_config << "TD " << quadVrai_->nom_ << " dephasage : " << angleFinal
               << ", prise la plus proche " << closestTapPosition;
    return closestTapPosition;
}


double TransformateurDephaseur::getPuiMax()
{
    double puiMax = puiMax_;
    if (uppran_ != -1) {
        // convertir la puis cons en angle, puis déterminer sa prise courante
        double anglecourant = power2Angle(puiCons_);
        int priseCourante = getClosestTapPosition(anglecourant); // détermine la position dans le vecteur des prises
        int priseMax = min(priseCourante + uppran_, nbtap_ - 1);
        if (tapdepha_[priseMax] < anglecourant) { // cas ou les prises sont rangees en ordre decroissant
            priseMax = max(priseCourante - uppran_, 0);
        }
        puiMax = angle2Power(tapdepha_[priseMax]);
        LOG(debug) << metrix::log::verbose_config << "Pour le TD " << quadVrai_->nom_
                   << ": angle courant: " << anglecourant << " prise courante: " << priseCourante
                   << " prise max: " << priseMax;
    }

    LOG(debug) << metrix::log::verbose_config << "Pour le TD " << this->quadVrai_->nom_
               << ": la puiMax vaut:  " << puiMax;
    return puiMax;
}

double TransformateurDephaseur::getPuiMin()
{
    double puiMin = puiMin_;
    if (lowran_ != -1) {
        // convertir la puis cons en angle, puis déterminer sa prise courante
        double anglecourant = power2Angle(puiCons_);
        int priseCourante = getClosestTapPosition(anglecourant);
        int priseMin = max(priseCourante - lowran_, 0);
        if (tapdepha_[priseMin] > anglecourant) { // cas ou les prises sont rangees en ordre decroissant
            priseMin = min(priseCourante + lowran_, nbtap_ - 1);
        }
        puiMin = angle2Power(tapdepha_[priseMin]);
        LOG(debug) << metrix::log::verbose_config << "Pour le TD " << quadVrai_->nom_ << ": angle courant "
                   << anglecourant << " prise courante " << priseCourante << " prise min: " << priseMin;
    }
    LOG(debug) << metrix::log::verbose_config << "Pour le TD " << quadVrai_->nom_ << ": la puiMin vaut:  " << puiMin;
    return puiMin;
}

double Incident::getProb() const
{
    double prob = config::configuration().probaInc();
    if (parade_) {
        if (incTraiteCur_ != nullptr) {
            prob = incTraiteCur_->probabilite_;
        }
    } else {
        prob = probabilite_;
    }
    return (prob);
}

void Reseau::updateBase(const config::VariantConfiguration::VariantConfig& config)
{
    using config::VariantConfiguration;

    if (config.num != VariantConfiguration::variant_base) {
        LOG(error) << "Update base network with variant number different from -1";
        return;
    }

    for (const auto& group : config.unavailableGroups) {
        auto groupesIt = groupes_.find(group);
        if (groupesIt != groupes_.end()) {
            auto& grp = groupesIt->second;
            grp->etatBase_ = false;
            grp->etat_ = false;

            if (grp->prodAjust_ == Groupe::OUI_HR_AR || grp->prodAjust_ == Groupe::OUI_HR) {
                prodMaxPossible_ -= grp->puisMaxDispo_;
            }
        } else {
            LOG_ALL(warning) << err::ioDico().msg("ERRGroupeIntrouvable", group, c_fmt("%d", config.num));
        }
    }

    for (const auto& conso_cfg : config.consos) {
        const auto& str = std::get<VariantConfiguration::NAME>(conso_cfg);
        auto consosIt = consos_.find(str);
        if (consosIt != consos_.end()) {
            const auto& conso = consosIt->second;
            auto var_dbl = std::get<VariantConfiguration::VALUE>(conso_cfg);
            conso->valeurBase_ = var_dbl;
            conso->valeur_ = var_dbl;
        } else {
            LOG_ALL(warning) << err::ioDico().msg("ERRNoeudConsoIntrouvable", str, c_fmt("%d", config.num));
        }
    }

    for (const auto& group : config.groups) {
        const auto& str = std::get<VariantConfiguration::NAME>(group);
        auto groupesIt = groupes_.find(str);
        if (groupesIt != groupes_.end()) {
            auto var_dbl = std::get<VariantConfiguration::VALUE>(group);
            const auto& grp = groupesIt->second;
            grp->prodPobjBase_ = var_dbl;
            grp->prodPobj_ = var_dbl;
            grp->prod_ = var_dbl;
        } else {
            LOG_ALL(warning) << err::ioDico().msg("ERRGroupeIntrouvable", str, c_fmt("%d", config.num));
        }
    }

    for (const auto& group : config.pmaxGroups) {
        const auto& str = std::get<VariantConfiguration::NAME>(group);
        auto groupesIt = groupes_.find(str);
        if (groupesIt != groupes_.end()) {
            auto var_dbl = std::get<VariantConfiguration::VALUE>(group);
            const auto& grp = groupesIt->second;
            if (grp->prodAjust_ == Groupe::OUI_HR_AR || grp->prodAjust_ == Groupe::OUI_HR) {
                prodMaxPossible_ -= grp->puisMaxDispoBase_ - var_dbl;
            }
            grp->puisMaxDispoBase_ = var_dbl;
            grp->puisMaxDispo_ = var_dbl;
            grp->puisMax_ = var_dbl;
        } else {
            LOG_ALL(warning) << err::ioDico().msg("ERRGroupeIntrouvable", str, c_fmt("%d", config.num));
        }
    }

    for (const auto& group : config.pminGroups) {
        const auto& str = std::get<VariantConfiguration::NAME>(group);
        auto groupesIt = groupes_.find(str);
        if (groupesIt != groupes_.end()) {
            auto var_dbl = std::get<VariantConfiguration::VALUE>(group);
            const auto& grp = groupesIt->second;
            grp->puisMinBase_ = var_dbl;
            grp->puisMin_ = var_dbl;
            grp->puisMinAR_ = var_dbl;
        } else {
            LOG_ALL(warning) << err::ioDico().msg("ERRGroupeIntrouvable", str, c_fmt("%d", config.num));
        }
    }

    for (const auto& cost : config.costs) {
        for (const auto& group : cost.second) {
            const auto& str = std::get<VariantConfiguration::NAME>(group);
            auto groupesIt = groupes_.find(str);
            if (groupesIt != groupes_.end()) {
                const auto& grp = groupesIt->second;
                auto var_dbl = std::get<VariantConfiguration::VALUE>(group);

                switch (cost.first) {
                    case VariantConfiguration::VariantConfig::CostType::UP_HR: grp->coutHausseHR_ = var_dbl; break;
                    case VariantConfiguration::VariantConfig::CostType::DOWN_HR: grp->coutBaisseHR_ = var_dbl; break;
                    case VariantConfiguration::VariantConfig::CostType::UP_AR: grp->coutHausseAR_ = var_dbl; break;
                    case VariantConfiguration::VariantConfig::CostType::DOWN_AR: grp->coutBaisseAR_ = var_dbl; break;
                    default:
                        // impossible case
                        break;
                }
            } else {
                LOG_ALL(warning) << err::ioDico().msg("ERRGroupeIntrouvable", str, c_fmt("%d", config.num));
            }
        }
    }

    for (const auto& line : config.pmaxHvdc) {
        const auto& str = std::get<VariantConfiguration::NAME>(line);
        auto lccIt = LigneCCs_.find(str);
        if (lccIt != LigneCCs_.end()) {
            auto var_dbl = std::get<VariantConfiguration::VALUE>(line);
            const auto& lcc = lccIt->second;
            lcc->puiMaxBase_ = var_dbl;
            lcc->puiMax_ = var_dbl;

            if (lcc->isEmulationAC()) {
                // Mise à jour du quad fictif surveillé
                const auto& elemAS = lcc->quadFictif_->elemAS_;
                elemAS->seuilMaxN_ = lcc->puiMax_;
                elemAS->seuilMaxInc_ = lcc->puiMax_;
                elemAS->seuilMaxIncComplexe_ = lcc->puiMax_;
                elemAS->seuilMaxAvantCur_ = lcc->puiMax_;
            }
        }
    }

    for (const auto& line : config.pminHvdc) {
        const auto& str = std::get<VariantConfiguration::NAME>(line);
        auto lccIt = LigneCCs_.find(str);
        if (lccIt != LigneCCs_.end()) {
            auto var_dbl = std::get<VariantConfiguration::VALUE>(line);
            const auto& lcc = lccIt->second;
            lcc->puiMinBase_ = var_dbl;
            lcc->puiMin_ = var_dbl;
        }
    }

    for (const auto& line : config.powerHvdc) {
        const auto& str = std::get<VariantConfiguration::NAME>(line);
        auto lccIt = LigneCCs_.find(str);
        if (lccIt != LigneCCs_.end()) {
            auto var_dbl = std::get<VariantConfiguration::VALUE>(line);
            const auto& lcc = lccIt->second;
            lcc->puiConsBase_ = var_dbl;
            lcc->puiCons_ = var_dbl;
        }
    }

    for (const auto& tdPhasing : config.tdPhasing) {
        const auto& str = std::get<VariantConfiguration::NAME>(tdPhasing);
        auto tdIt = TransfoDephaseurs_.find(str);
        if (tdIt != TransfoDephaseurs_.end()) {
            auto var_int = std::get<VariantConfiguration::VALUE>(tdPhasing);
            const auto& td = tdIt->second;
            if (!td->tapdepha_.empty() && (var_int >= td->lowtap_) && (var_int < td->lowtap_ + td->nbtap_)) {
                td->puiConsBase_ = td->angle2Power(td->tapdepha_[var_int - td->lowtap_]);
            } else {
                LOG_ALL(warning) << err::ioDico().msg(
                    "ERRTDPriseIntrouvable", str, c_fmt("%d", var_int), c_fmt("%d", config.num));
            }
        }
    }

    for (const auto& threshold : config.tresholds) {
        for (const auto& quad_cfg : threshold.second) {
            const auto& str = std::get<VariantConfiguration::NAME>(quad_cfg);
            const auto& mapping = (threshold.first == VariantConfiguration::VariantConfig::Threshold::MAX_N)
                                      ? elementsASurveillerN_
                                      : elementsASurveillerNk_;
            auto quadSurvIt = mapping.find(str);
            if (quadSurvIt != mapping.end()) {
                const auto& quad = quadSurvIt->second;
                auto var_dbl = std::get<VariantConfiguration::VALUE>(quad_cfg);

                switch (threshold.first) {
                    case VariantConfiguration::VariantConfig::Threshold::MAX_N: quad->seuilMaxN_ = var_dbl; break;
                    case VariantConfiguration::VariantConfig::Threshold::MAX_INC: quad->seuilMaxInc_ = var_dbl; break;
                    case VariantConfiguration::VariantConfig::Threshold::MAX_INC_COMPLEX:
                        quad->seuilMaxIncComplexe_ = var_dbl;
                        break;
                    case VariantConfiguration::VariantConfig::Threshold::MAX_BEFORE_CUR:
                        quad->seuilMaxAvantCur_ = var_dbl;
                        break;
                    case VariantConfiguration::VariantConfig::Threshold::MAX_BEFORE_CUR_COMPLEX:
                        quad->seuilMaxAvantCurIncComplexe_ = var_dbl;
                        break;
                    case VariantConfiguration::VariantConfig::Threshold::MAX_NEXOR:
                        quad->seuilMaxNExOr_ = var_dbl;
                        quad->seuilsAssymetriques_ = true;
                        break;
                    case VariantConfiguration::VariantConfig::Threshold::MAX_INC_EXOR:
                        quad->seuilMaxIncExOr_ = var_dbl;
                        quad->seuilsAssymetriques_ = true;
                        break;
                    case VariantConfiguration::VariantConfig::Threshold::MAX_INC_COMPLEX_EXOR:
                        quad->seuilMaxIncComplexeExOr_ = var_dbl;
                        quad->seuilsAssymetriques_ = true;
                        break;
                    case VariantConfiguration::VariantConfig::Threshold::MAX_BEFORE_CUR_EXOR:
                        quad->seuilMaxAvantCurExOr_ = var_dbl;
                        quad->seuilsAssymetriques_ = true;
                        break;
                    case VariantConfiguration::VariantConfig::Threshold::MAX_BEFORE_CUR_COMPLEX_EXOR:
                        quad->seuilMaxAvantCurIncComplexeExOr_ = var_dbl;
                        quad->seuilsAssymetriques_ = true;
                        break;
                    default:
                        // impossible case
                        break;
                }
            }
        }
    }

    for (const auto& conso_cfg : config.deleteConsosCosts) {
        const auto& str = std::get<VariantConfiguration::NAME>(conso_cfg);
        auto consosIt = consos_.find(str);
        if (consosIt != consos_.end()) {
            auto var_dbl = std::get<VariantConfiguration::VALUE>(conso_cfg);
            const auto& conso = consosIt->second;
            conso->coutEffacementBase_ = var_dbl;
            conso->coutEffacement_ = var_dbl;
        } else {
            LOG_ALL(warning) << err::ioDico().msg("ERRNoeudConsoIntrouvable", str, c_fmt("%d", config.num));
        }
    }

    for (auto& incident : config.probas) {
        const auto& str = std::get<VariantConfiguration::NAME>(incident);
        auto icIt = incidents_.find(str);
        if (icIt != incidents_.end()) {
            auto var_dbl = std::get<VariantConfiguration::VALUE>(incident);
            const auto& icdt = icIt->second;
            icdt->probabilite_ = var_dbl;
            icdt->probabiliteBase_ = var_dbl;
        } else {
            LOG_ALL(warning) << err::ioDico().msg("ERRIncidentIntrouvable", str, c_fmt("%d", config.num));
        }
    }


    Quadripole::SetQuadripoleSortedByName indispoLignesBase;
    for (const auto& line : config.unavailableLines) {
            auto quadsIt = quads_.find(line);
            if (quadsIt != quads_.end()) {
                indispoLignesBase.insert(quadsIt->second);
                const auto& quad = quadsIt->second;
                quad->etatExBase_ = false;
                quad->etatOrBase_ = false;
                } else {
                LOG_ALL(warning) << err::ioDico().msg("ERRQuadIntrouvable", line, c_fmt("%d", config.num), "QUADIN");
            }

        }
        int status = modifReseauTopo(indispoLignesBase);

        if (status != METRIX_PAS_PROBLEME) {
            LOG_ALL(warning) << "probleme lors de la modification du reseau pour la variante de base";
        }

}

void Reseau::updateVariants(MapQuadinVar& mapping, const config::VariantConfiguration& config)
{
    const auto& input_config = config::inputConfiguration();
    unsigned int variant_index = 0;
    for (const auto& pair : config.variants()) {
        if (pair.first == config::VariantConfiguration::variant_base) {
            // ignore base variant: is processed in another function
            continue;
        }

        if (pair.first < static_cast<int>(input_config.firstVariant())) {
            // ignore variants before first variant
            continue;
        }

        if (variant_index >= input_config.nbVariant()) {
            return;
        }
        variant_index++;

        updateVariant(mapping, pair.second);
    }
}

void Reseau::updateVariant(MapQuadinVar& mapping, const config::VariantConfiguration::VariantConfig& config)
{
    using config::VariantConfiguration;

    auto variant = std::make_shared<Variante>();
    variant->num_ = config.num;

    for (const auto& group : config.unavailableGroups) {
        auto groupesIt = groupes_.find(group);
        if (groupesIt != groupes_.end()) {
            variant->grpIndispo_.insert(groupesIt->second);
        } else {
            LOG_ALL(warning) << err::ioDico().msg("ERRGroupeIntrouvable", group, c_fmt("%d", config.num));
        }
    }

    for (const auto& conso_cfg : config.consos) {
        const auto& str = std::get<VariantConfiguration::NAME>(conso_cfg);
        auto consosIt = consos_.find(str);
        if (consosIt != consos_.end()) {
            auto var_dbl = std::get<VariantConfiguration::VALUE>(conso_cfg);
            variant->valeurConso_.insert(std::pair<std::shared_ptr<Consommation>, double>(consosIt->second, var_dbl));
        } else {
            LOG_ALL(warning) << err::ioDico().msg("ERRNoeudConsoIntrouvable", str, c_fmt("%d", config.num));
        }
    }

    for (const auto& group : config.groups) {
        const auto& str = std::get<VariantConfiguration::NAME>(group);
        auto groupesIt = groupes_.find(str);
        if (groupesIt != groupes_.end()) {
            auto var_dbl = std::get<VariantConfiguration::VALUE>(group);
            variant->prodImpose_.insert(std::pair<std::shared_ptr<Groupe>, double>(groupesIt->second, var_dbl));
        } else {
            LOG_ALL(warning) << err::ioDico().msg("ERRGroupeIntrouvable", str, c_fmt("%d", config.num));
        }
    }

    for (const auto& group : config.pmaxGroups) {
        const auto& str = std::get<VariantConfiguration::NAME>(group);
        auto groupesIt = groupes_.find(str);
        if (groupesIt != groupes_.end()) {
            auto var_dbl = std::get<VariantConfiguration::VALUE>(group);
            variant->prodMax_.insert(std::pair<std::shared_ptr<Groupe>, double>(groupesIt->second, var_dbl));
        } else {
            LOG_ALL(warning) << err::ioDico().msg("ERRGroupeIntrouvable", str, c_fmt("%d", config.num));
        }
    }

    for (const auto& group : config.pminGroups) {
        const auto& str = std::get<VariantConfiguration::NAME>(group);
        auto groupesIt = groupes_.find(str);
        if (groupesIt != groupes_.end()) {
            auto var_dbl = std::get<VariantConfiguration::VALUE>(group);
            variant->prodMin_.insert(std::pair<std::shared_ptr<Groupe>, double>(groupesIt->second, var_dbl));
        } else {
            LOG_ALL(warning) << err::ioDico().msg("ERRGroupeIntrouvable", str, c_fmt("%d", config.num));
        }
    }

    for (const auto& cost : config.costs) {
        for (auto group : cost.second) {
            const auto& str = std::get<VariantConfiguration::NAME>(group);
            auto groupesIt = groupes_.find(str);
            if (groupesIt != groupes_.end()) {
                auto var_dbl = std::get<VariantConfiguration::VALUE>(group);

                switch (cost.first) {
                    case VariantConfiguration::VariantConfig::CostType::UP_HR:
                        variant->grpHausseHR_.insert(
                            std::pair<std::shared_ptr<Groupe>, double>(groupesIt->second, var_dbl));
                        break;
                    case VariantConfiguration::VariantConfig::CostType::DOWN_HR:
                        variant->grpBaisseHR_.insert(
                            std::pair<std::shared_ptr<Groupe>, double>(groupesIt->second, var_dbl));
                        break;
                    case VariantConfiguration::VariantConfig::CostType::UP_AR:
                        variant->grpHausseAR_.insert(
                            std::pair<std::shared_ptr<Groupe>, double>(groupesIt->second, var_dbl));
                        break;
                    case VariantConfiguration::VariantConfig::CostType::DOWN_AR:
                        variant->grpBaisseAR_.insert(
                            std::pair<std::shared_ptr<Groupe>, double>(groupesIt->second, var_dbl));
                        break;
                    default:
                        // impossible case
                        break;
                }
            } else {
                LOG_ALL(warning) << err::ioDico().msg("ERRGroupeIntrouvable", str, c_fmt("%d", config.num));
            }
        }
    }

    for (const auto& line : config.unavailableLines) {
        auto quadsIt = quads_.find(line);
        if (quadsIt != quads_.end()) {
            variant->indispoLignes_.insert(quadsIt->second);
        } else {
            LOG_ALL(warning) << err::ioDico().msg("ERRQuadIntrouvable", line, c_fmt("%d", config.num), "QUADIN");
        }
    }

    for (const auto& line : config.pmaxHvdc) {
        const auto& str = std::get<VariantConfiguration::NAME>(line);
        auto lccIt = LigneCCs_.find(str);
        if (lccIt != LigneCCs_.end()) {
            auto var_dbl = std::get<VariantConfiguration::VALUE>(line);
            variant->dcPuissMax_.insert(std::pair<std::shared_ptr<LigneCC>, double>(lccIt->second, var_dbl));
        }
    }

    for (const auto& line : config.pminHvdc) {
        const auto& str = std::get<VariantConfiguration::NAME>(line);
        auto lccIt = LigneCCs_.find(str);
        if (lccIt != LigneCCs_.end()) {
            auto var_dbl = std::get<VariantConfiguration::VALUE>(line);
            variant->dcPuissMin_.insert(std::pair<std::shared_ptr<LigneCC>, double>(lccIt->second, var_dbl));
        }
    }

    for (const auto& line : config.powerHvdc) {
        const auto& str = std::get<VariantConfiguration::NAME>(line);
        auto lccIt = LigneCCs_.find(str);
        if (lccIt != LigneCCs_.end()) {
            auto var_dbl = std::get<VariantConfiguration::VALUE>(line);
            variant->dcPuissImp_.insert(std::pair<std::shared_ptr<LigneCC>, double>(lccIt->second, var_dbl));
        }
    }

    for (const auto& tdPhasing : config.tdPhasing) {
        const auto& str = std::get<VariantConfiguration::NAME>(tdPhasing);
        auto tdIt = TransfoDephaseurs_.find(str);
        if (tdIt != TransfoDephaseurs_.end()) {
            auto var_int = std::get<VariantConfiguration::VALUE>(tdPhasing);
            const auto& td = tdIt->second;
            if (!td->tapdepha_.empty() && (var_int >= td->lowtap_) && (var_int < td->lowtap_ + td->nbtap_)) {
                auto power = td->angle2Power(td->tapdepha_[var_int - td->lowtap_]);
                variant->dtValDep_.insert(std::pair<std::shared_ptr<TransformateurDephaseur>, double>(td, power));
            } else {
                LOG_ALL(warning) << err::ioDico().msg(
                    "ERRTDPriseIntrouvable", str, c_fmt("%d", var_int), c_fmt("%d", config.num));
            }
        }
    }

    for (const auto& threshold : config.tresholds) {
        for (const auto& quad_cfg : threshold.second) {
            const auto& str = std::get<VariantConfiguration::NAME>(quad_cfg);
            const auto& map = (threshold.first == VariantConfiguration::VariantConfig::Threshold::MAX_N)
                                  ? elementsASurveillerN_
                                  : elementsASurveillerNk_;
            auto quadSurvIt = map.find(str);
            if (quadSurvIt != map.end()) {
                const auto& quad = quadSurvIt->second;
                auto var_dbl = std::get<VariantConfiguration::VALUE>(quad_cfg);

                switch (threshold.first) {
                    case VariantConfiguration::VariantConfig::Threshold::MAX_N:
                        variant->quati00mn_.insert(
                            std::pair<std::shared_ptr<ElementASurveiller>, double>(quad, var_dbl));
                        break;
                    case VariantConfiguration::VariantConfig::Threshold::MAX_INC:
                        variant->quati5mns_.insert(
                            std::pair<std::shared_ptr<ElementASurveiller>, double>(quad, var_dbl));
                        break;
                    case VariantConfiguration::VariantConfig::Threshold::MAX_INC_COMPLEX:
                        variant->quati20mn_.insert(
                            std::pair<std::shared_ptr<ElementASurveiller>, double>(quad, var_dbl));
                        break;
                    case VariantConfiguration::VariantConfig::Threshold::MAX_BEFORE_CUR:
                        variant->quatitamn_.insert(
                            std::pair<std::shared_ptr<ElementASurveiller>, double>(quad, var_dbl));
                        break;
                    case VariantConfiguration::VariantConfig::Threshold::MAX_BEFORE_CUR_COMPLEX:
                        variant->quatitamk_.insert(
                            std::pair<std::shared_ptr<ElementASurveiller>, double>(quad, var_dbl));
                        break;
                    case VariantConfiguration::VariantConfig::Threshold::MAX_NEXOR:
                        variant->quati00mnExOr_.insert(
                            std::pair<std::shared_ptr<ElementASurveiller>, double>(quad, var_dbl));
                        quad->seuilsAssymetriques_ = true;
                        break;
                    case VariantConfiguration::VariantConfig::Threshold::MAX_INC_EXOR:
                        variant->quati5mnsExOr_.insert(
                            std::pair<std::shared_ptr<ElementASurveiller>, double>(quad, var_dbl));
                        quad->seuilsAssymetriques_ = true;
                        break;
                    case VariantConfiguration::VariantConfig::Threshold::MAX_INC_COMPLEX_EXOR:
                        variant->quati20mnExOr_.insert(
                            std::pair<std::shared_ptr<ElementASurveiller>, double>(quad, var_dbl));
                        quad->seuilsAssymetriques_ = true;
                        break;
                    case VariantConfiguration::VariantConfig::Threshold::MAX_BEFORE_CUR_EXOR:
                        variant->quatitamnExOr_.insert(
                            std::pair<std::shared_ptr<ElementASurveiller>, double>(quad, var_dbl));
                        quad->seuilsAssymetriques_ = true;
                        break;
                    case VariantConfiguration::VariantConfig::Threshold::MAX_BEFORE_CUR_COMPLEX_EXOR:
                        variant->quatitamkExOr_.insert(
                            std::pair<std::shared_ptr<ElementASurveiller>, double>(quad, var_dbl));
                        quad->seuilsAssymetriques_ = true;
                        break;
                    default:
                        // impossible case
                        break;
                }
            }
        }
    }

    for (const auto& conso_cfg : config.deleteConsosCosts) {
        const auto& str = std::get<VariantConfiguration::NAME>(conso_cfg);
        auto consosIt = consos_.find(str);
        if (consosIt != consos_.end()) {
            auto var_dbl = std::get<VariantConfiguration::VALUE>(conso_cfg);
            variant->coutEfface_.insert(std::pair<std::shared_ptr<Consommation>, double>(consosIt->second, var_dbl));
        } else {
            LOG_ALL(warning) << err::ioDico().msg("ERRNoeudConsoIntrouvable", str, c_fmt("%d", config.num));
        }
    }

    for (const auto& balance : config.balancesConso) {
        const auto& str = std::get<VariantConfiguration::NAME>(balance);
        int region_num = findRegion(str);
        if (region_num == -1) {
            throw ErrorI(err::ioDico().msg("ERRRegionIntrouvable", str, c_fmt("%d", variant->num_)));
        }
        auto var_dbl = std::get<VariantConfiguration::VALUE>(balance);
        variant->valeurEchange_.insert(std::pair<int, double>(region_num, var_dbl));
    }

    for (const auto& balance : config.balancesProd) {
        const auto& str = std::get<VariantConfiguration::NAME>(balance);
        int region_num = findRegion(str);
        if (region_num == -1) {
            throw ErrorI(err::ioDico().msg("ERRRegionIntrouvable", str, c_fmt("%d", variant->num_)));
        }
        auto var_dbl = std::get<VariantConfiguration::VALUE>(balance);
        variant->varBilanProd_.insert(std::pair<int, double>(region_num, var_dbl));
    }

    for (const auto& incident : config.probas) {
        const auto& str = std::get<VariantConfiguration::NAME>(incident);
        auto icIt = incidents_.find(str);
        if (icIt != incidents_.end()) {
            auto var_dbl = std::get<VariantConfiguration::VALUE>(incident);
            variant->probabinc_.insert(std::pair<std::shared_ptr<Incident>, double>(icIt->second, var_dbl));
        } else {
            LOG_ALL(warning) << err::ioDico().msg("ERRIncidentIntrouvable", str, c_fmt("%d", config.num));
        }
    }

    for (const auto& group : config.randomGroups) {
        auto groupesIt = groupes_.find(group);
        if (groupesIt != groupes_.end()) {
            variant->randomGroups_.push_back(groupesIt->second);
        } else {
            throw ErrorI(err::ioDico().msg("ERRGrpRandomDifferentGrp", c_fmt("%d", variant->num_)));
        }
    }
    if (!config.randomGroups.empty()){
        for (const auto& group : groupes_){
            if (std::find(config.randomGroups.begin(), config.randomGroups.end(), group.first) == config.randomGroups.end()){
                throw ErrorI(err::ioDico().msg("ERRGrpRandomDifferentGrp", c_fmt("%d", variant->num_)));
            }
        }
    }
    
    mapping[variant->indispoLignes_].push_back(variant);
}

int Reseau::findRegion(const std::string& name)
{
    auto regionIt = std::find(regions_.begin(), regions_.end(), name);
    if (regionIt == regions_.end()) {
        return -1;
    }
    return static_cast<int>(regionIt - regions_.begin());
}

void Reseau::updateParades(const config::ParadesConfiguration& config)
{
    if (config.parades().empty()) {
        return;
    }

    for (const auto& parade_def : config.parades()) {
        auto mapIt = incidents_.find(parade_def.incident_name);
        if (mapIt == incidents_.end()) {
            LOG_ALL(warning) << err::ioDico().msg("ERRCurTopoIncidentInconnu", parade_def.incident_name);
            continue;
        }

        const auto& incident = mapIt->second;
        if (!incident->validite_) {
            continue;
        }

        // Creation de la parade
        auto parade = std::make_shared<Incident>(*incident);
        parade->parade_ = true;
        parade->incTraiteCur_ = incident;
        std::string nom;

        if (config::inputConfiguration().useAllOutputs()) {
            nom = "P_" + incident->nom_ + " / ";
        }

        if (!parade_def.constraints.empty()) {
            for (const auto& cont : parade_def.constraints) {
                auto mapCIt = elementsASurveillerNk_.find(cont);
                if (mapCIt != elementsASurveillerNk_.end()) {
                    parade->contraintesAutorisees_.insert(mapCIt->second);
                } else {
                    LOG_ALL(warning) << err::ioDico().msg("ERRCurTopoContrainteInconnue", cont);
                }
            }
        }

        bool ok = true;
        for (auto it = parade_def.couplings.begin(); it != parade_def.couplings.end(); ++it) {
            if (it != parade_def.couplings.begin()) {
                nom += " / ";
            }
            nom += *it;

            bool to_close = false;
            auto incident_name = *it;
            if (incident_name.front() == '+') {
                to_close = true;
                incident_name = incident_name.substr(1);
            }

            auto mapQIt = quads_.find(incident_name);
            if (mapQIt == quads_.end()) {
                LOG_ALL(error) << err::ioDico().msg("ERRCouplageInconnu", incident_name);
                ok = false;
                break;
            }

            const auto& quad = mapQIt->second;

            if (quad->norqua_->num_ == quad->nexqua_->num_) {
                LOG_ALL(error) << err::ioDico().msg("ERRCouplageBoucle", quad->nom_);
                ok = false;
                break;
            }

            if (std::find(incident->listeQuads_.begin(), incident->listeQuads_.end(), quad)
                != incident->listeQuads_.end()) {
                LOG_ALL(warning) << err::ioDico().msg("WarnElementDansIncident", quad->nom_, incident->nom_);
                continue; // ce couplage est dans l'incident
            }

            parade->type_ = Incident::N_MOINS_K_GROUPE_LIGNE;
            if (to_close) {
                if (std::find(parade->listeCouplagesFermes_.begin(), parade->listeCouplagesFermes_.end(), quad)
                    != parade->listeCouplagesFermes_.end()) {
                    LOG_ALL(warning) << err::ioDico().msg("WarnElementDejaDansParade", quad->nom_, incident->nom_);
                    continue; // ce couplage est deja dans la parade
                }
                parade->nbCouplagesFermes_++;
                parade->listeCouplagesFermes_.push_back(quad);
            } else {
                if (std::find(parade->listeQuads_.begin(), parade->listeQuads_.end(), quad)
                    != parade->listeQuads_.end()) {
                    LOG_ALL(warning) << err::ioDico().msg("WarnElementDejaDansParade", quad->nom_, incident->nom_);
                    continue; // ce couplage est deja dans la parade
                }
                parade->nbLignes_++;
                parade->listeQuads_.push_back(quad);
            }

            // ajout pour le calcul des coeff de report
            quadsSurv_.insert(quad);
        }

        if (!ok) {
            continue;
        }

        if (nom.empty()) {
            nom = "PARADE_NRF";
        }

        parade->nom_ = nom;

        if (!parade_def.couplings.empty()) {
            connexite(parade,
                      false,
                      config::configuration().useIncRompantConnexite()
                          || config::configuration().useParRompantConnexite());
            if (!parade->validite_) {
                LOG_ALL(info) << err::ioDico().msg(
                    "INFOIncidentRompantConnexite", "parade", incident->nom_, parade->nom_);
                continue;
            }
        }

        if (incident->parades_.empty()) {
            // C'est la premiere parade de l'incident, on ajoute la parade "Ne Rien Faire"
            ajouteParadeNeRienFaire(incident);
        }

        incident->parades_.push_back(parade); // ajout de la parade dans les parades de l'incident
        parade->num_ = static_cast<int>(incidentsEtParades_.size());
        incidentsEtParades_.push_back(parade); // ajout de la parade dans les incidents du reseau
        nbIncidents_++;

        std::stringstream ss;
        ss << " on a ajoute " << parade->nom_ << " (num " << parade->num_
           << "), nb actions = " << parade->nbLignes_ + parade->nbCouplagesFermes_ << ")";
        if (!parade->contraintesAutorisees_.empty()) {
            ss << ", nb restriction contraintes = " << parade->contraintesAutorisees_.size();
        }
        LOG(debug) << metrix::log::verbose_config << ss.str();
    }

    // Move curative means (TD/HVDC) from incident to parades
    for (const auto& elem : incidents_) {
        const auto& inc = elem.second;

        if (inc->parades_.empty() || inc->listeElemCur_.empty()) {
            continue;
        }

        const auto& premiereParade = inc->parades_[0];

        // Switch lists
        inc->listeElemCur_.swap(premiereParade->listeElemCur_);
        inc->lccElemCur_.swap(premiereParade->lccElemCur_);
        inc->tdFictifsElemCur_.swap(premiereParade->tdFictifsElemCur_);
        for (unsigned int j = 1; j < inc->parades_.size(); ++j) {
            inc->parades_[j]->listeElemCur_ = premiereParade->listeElemCur_;
            inc->parades_[j]->lccElemCur_ = premiereParade->lccElemCur_;
            inc->parades_[j]->tdFictifsElemCur_ = premiereParade->tdFictifsElemCur_;
        }

        if (premiereParade->tdFictifsElemCur_.empty()) {
            continue;
        }

        // duplicate fictive curative corresponding to AC emulation on parent incident
        for (auto it = premiereParade->tdFictifsElemCur_.cbegin(); it != premiereParade->tdFictifsElemCur_.end();
             ++it) {
            auto elemCur = std::make_shared<ElementCuratifTD>(it->second->td_);
            inc->listeElemCur_.push_back(elemCur);
            inc->tdFictifsElemCur_.insert(
                std::pair<std::shared_ptr<Quadripole>, std::shared_ptr<ElementCuratifTD>>(it->first, elemCur));
        }
    }

    LOG_ALL(info) << " Nb total d'incidents + parades : " << nbIncidents_;
    nbIncidentsBase_ = nbIncidents_;
}

std::shared_ptr<Incident> Reseau::ajouteParadeNeRienFaire(const std::shared_ptr<Incident>& incident)
{
    auto paradeNRF = std::make_shared<Incident>(*incident);
    paradeNRF->parade_ = true;
    paradeNRF->incTraiteCur_ = incident;
    paradeNRF->nom_ = incident->nom_ + "_NRF";

    incident->parades_.push_back(paradeNRF); // ajout de la parade dans les parades de l'inc
    paradeNRF->num_ = static_cast<int>(incidentsEtParades_.size());
    incidentsEtParades_.push_back(paradeNRF); // ajout de la parade dans les incidents du reseau
    nbIncidents_++;

    if (incident->pochePerdue_) {
        paradeNRF->pochePerdue_ = std::make_shared<PochePerdue>(*incident->pochePerdue_);
        incidentsRompantConnexite_.push_back(paradeNRF);
    }

    return paradeNRF;
}
