//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#ifndef CALCUL_METRIX
#define CALCUL_METRIX

#include "compute/isolver.h"
#include "err/error.h"
#include "margin_variations_compute.h"
#include "pne.h"
#include "prototypes.h"
#include "reseau.h"
#include "variante.h"

#include <boost/optional.hpp>

#include <cassert>
#include <iostream>
#include <map>
#include <memory>
#include <sstream>
#include <string>
#include <tuple>
#include <vector>


using std::ostringstream;
using std::string;
using std::vector;

//----------------------------------------------------------
// Contrainte : couple quad/incident
//----------------------------------------------------------

constexpr int SITU_N = -1;

class Contrainte
{
    friend std::ostream& operator<<(std::ostream& out, const Contrainte& C);

public:
    enum TypeDeContrainte {
        CONTRAINTE_EMUL_AC_N = 1,
        CONTRAINTE_EMULATION_AC = 2,
        CONTRAINTE_N = 3,
        CONTRAINTE_N_MOINS_K = 4,
        CONTRAINTE_PARADE = 5,
        CONTRAINTE_ACTIVATION = 6,
        CONTRAINTE_NON_DEF = -1
    };

    Contrainte(const Contrainte& copie) { *this = copie; }
    ~Contrainte() = default;

    Contrainte& operator=(const Contrainte& ctr)
    {
        if (this != &ctr) {
            elemAS_ = ctr.elemAS_;
            numVarActivation_ = ctr.numVarActivation_;
            icdt_ = ctr.icdt_;
            transit_ = ctr.transit_;
            maxT_ = ctr.maxT_;
            minT_ = ctr.minT_;
            ecart_ = ctr.ecart_;
            type_ = ctr.type_;
            ctrSup_ = ctr.ctrSup_;
            ecrireContrainte_ = ctr.ecrireContrainte_;
        }
        return *this;
    }

    int num_;                                    // indice de la contrainte dans la matrice
    std::shared_ptr<ElementASurveiller> elemAS_; // quadripole en contrainte
    int numVarActivation_;           // variable entiere d'activation (si contrainte sur une hvdc en emulation AC)
    std::shared_ptr<Incident> icdt_; // incident qui cree la contrainte
    double transit_;                 // transit calcule
    double maxT_;                    // borne sup de la contrainte
    double minT_;                    // borne inf de la contrainte (< 0)
    double ecart_;                   // "T-Tmax si ctrSup_" ou "Tmin-T" sinon
    TypeDeContrainte type_;          // type de contrainte
    bool ctrSup_;                    // vrai si T > Tmax
    bool ecrireContrainte_;

    // Quelques methodes
    string typeDeContrainteToString() const;
    string toString() const
    {
        return "(" + typeDeContrainteToString() + " " + nomInc() + " || " + elemAS_->nom_ + ")";
    };
    string nomInc() const { return icdt_ ? icdt_->nom_ : "N"; };
    int numInc() const { return icdt_ ? icdt_->num_ : SITU_N; };

    // constructeur
    Contrainte(const std::shared_ptr<ElementASurveiller>& qdt,
               const std::shared_ptr<Incident>& icdt,
               double T_N,
               double Tmax,
               double Tmin,
               double ecart,
               TypeDeContrainte type,
               bool ctrSup)
    {
        init(qdt, icdt, T_N, Tmax, Tmin, ecart, type, ctrSup);
    }

    // constructeur par defaut
    Contrainte() { init(nullptr, nullptr, 0.0, 0.0, 0.0, 0.0, Contrainte::CONTRAINTE_NON_DEF, false); }

private:
    void init(const std::shared_ptr<ElementASurveiller>& qdt,
              const std::shared_ptr<Incident>& icdt,
              double T_N,
              double Tmax,
              double Tmin,
              double ecart,
              TypeDeContrainte type,
              bool supTmax);
};

// Fonctions de comparaison
bool compareContraintes(const std::shared_ptr<Contrainte>& icdtQdt1, const std::shared_ptr<Contrainte>& icdtQdt2);
bool compareGroupeHausse(const std::shared_ptr<Groupe>& grp1, const std::shared_ptr<Groupe>& grp2);
bool compareGroupeBaisse(const std::shared_ptr<Groupe>& grp1, const std::shared_ptr<Groupe>& grp2);

//----------------------------------------------------------
// classe Calculer
//----------------------------------------------------------

class Calculer
{
public:
    enum TypeDeSolveur {
        UTILISATION_PNE_SOLVEUR = 1,
        UTILISATION_SIMPLEXE = 2,
        UTILISATION_PC_SIMPLEXE = 3
    };

    enum TypeCoupes {
        COUPE_SURETE_N = 1,
        COUPE_SURETE_N_1_L = 2,
        COUPE_SURETE_N_1_G = 3,
        COUPE_SURETE_N_K = 4,
        COUPE_BILAN = 5,
        COUPE_AUTRE = 6
    };

    enum TypeEtat {
        NONE = 0,
        PROD_H = 1,
        PROD_B = 2,
        CONSO_D = 3,
        DEPH_H = 4,
        DEPH_B = 5,
        LIGNE_CC_H = 6,
        LIGNE_CC_B = 7,
        HVDC_CUR_H = 10,
        HVDC_CUR_B = 11,
        DEPH_CUR_H = 12,
        DEPH_CUR_B = 13,
        GRP_CUR_H = 14,
        GRP_CUR_B = 15,
        CONSO_H = 16,
        CONSO_B = 17,
        ECART_T = 20,
        VAR_ENT = 30,
        END_ENE = 40
    };

    TypeDeSolveur typeSolveur_ = UTILISATION_SIMPLEXE;

    std::shared_ptr<Variante> varianteCourante_;

    int pbNombreDeVariables_ = 0;           // defenie dans pne ou spx
    int pbNombreDeContraintes_ = 0;         // defenie dans pne ou spx
    int pbExistenceDUneSolution_ = 0;       // defenie dans pne ou spx
    unsigned int nbElmdeMatrContraint_ = 0; // nombre d element de la matrice des contraintes
    vector<int> pbTypeDeVariable_;          // defenie dans pne ou spx
    vector<int> pbTypeDeBorneDeLaVariable_; // defenie dans pne ou spx
    vector<double> pbX_;                    // defenie dans pne ou spx
    vector<double> pbXhR_;                  // etat sans reseau
    vector<double> pbXmin_;                 // defenie dans pne ou spx
    vector<double> pbXmax_;                 // defenie dans pne ou spx
    vector<double> pbCoutLineaire_;         // defenie dans pne ou spx
    vector<double>
        pbCoutLineaireSansOffset_; // sert uniquement pour l'affichage des couts finaux curatifs production et conso. On
                                   // ne remplit donc le vecteur que sur les variables gen_cur et conso_cur
    vector<char> pbSens_;          // defini dans pne ou spx
    vector<double> pbSecondMembre_;                          // defini dans pne ou spx
    vector<int> pbIndicesDebutDeLigne_;                      // defini dans pne ou spx
    vector<int> pbNombreDeTermesDesLignes_;                  // defini dans pne ou spx
    vector<double> pbCoefficientsDeLaMatriceDesContraintes_; // defini dans pne ou spx
    vector<int> pbIndicesColonnes_;                          // defini dans pne ou spx

    vector<double> coefs_; // vecteur utilise pour le calcul des coefficients

    // specifique au simplexe
    std::shared_ptr<compute::ISolver> solver_pne_;
    std::shared_ptr<compute::ISolver> pc_solver_;
    std::shared_ptr<compute::ISolver> solver_simplex_;


    vector<int> pbPositionDeLaVariable_; // defini dans pne ou spx
    // vector<int> pbPositionDeLaVariableVariante0_   ; //defini dans pne ou spx
    int pbNbVarDeBaseComplementaires_ = 0;          // defini dans pne ou spx
    vector<int> pbComplementDeLaBase_;              // defini dans pne ou spx
    vector<double> pbCoutsMarginauxDesContraintes_; // defini dans pne ou spx
    vector<double> pbCoutsReduits_;                 // defini dans pne ou spx
    vector<TypeCoupes> pbTypeContrainte_;           // type de la coupe surete N : 1, surete N-1 Ligne : 2,
    // surete N-1 Groupe : 3, surete N-K : 4, autre : 5
    // pour ajouter des coupes
    int pbNombreDeContraintesCoupes_ = 0; // defenie dans pne ou spx
    int pbNbTermeACoupes_ = 0;            // c'est le nombre d element de la matrice des coupes

    vector<std::shared_ptr<Contrainte>> pbContraintes_; // pointeur sur le quadripole concerne par la coupe

    std::map<std::shared_ptr<Incident>, int> lienParadeValorisation_;

    // Chainage de la matrice jacobienne utilisee par la LU.
    //******************************************************/
    vector<int> jacIndexDebutDesColonnes_;
    vector<int> jacNbTermesDesColonnes_;
    vector<double> jacValeurDesTermesDeLaMatrice_;
    vector<int> jacIndicesDeLigne_;

    // Stock de resultat
    //******************************************************/

    // Recalcul des pertes
    double pertesTotales_ = 0.0;
    double consoTotale_ = 0;
    float tauxPertes_ = 0.0; /* taux de pertes utilise (peut avoir ete recalcule) */

    // Detail de la fonction objectif
    double fonction_objectif_G_ = 0.0;           // cout redispatching preventif
    double fonction_objectif_D_ = 0.0;           // cout delestage preventif
    double fonction_objectif_C_ = 0.0;           // cout d'activation du curatif 'gratuit' (TD, HVDC, parades)
    double fonction_objectif_E_ = 0.0;           // cout variables d'ecart
    double fonction_objectif_G_cur_ = 0.0;       // cout d'activation du curatif des groupes
    double fonction_objectif_D_cur_ = 0.0;       // cout d'activation du curatif des consos
    double fonction_objectif_G_cur_sans_offset_; // cout d'activation du curatif des groupes sans offset
    double fonction_objectif_D_cur_sans_offset_; // cout d'activation du curatif des consos sans offset

    double sommeEcartsNk_ = 0.0; // somme des surcharges

    // etat
    //****
    vector<TypeEtat> typeEtat_;
    vector<int> numSupportEtat_;
    Reseau& res_;
    MapQuadinVar& variantesOrdonnees_;
    MATRICE_A_FACTORISER jac_;
    MATRICE* jacFactorisee_ = nullptr;
    PROBLEME_SIMPLEXE pb_;
    PROBLEME_A_RESOUDRE pbPNE_;
    bool problemeAlloue_ = false;

    std::map<ListeQuadsIncident, MATRICE*> jacIncidentsModifies_;

    Calculer();
    ~Calculer();

    unsigned int numMicroIteration_ = 0; // iterations correspondant au contraintes ajoutees pour une variante (1,2,...)
    int resolutionProbleme();            // resolution de tous les problemes modifies par variante
    int metrix2Assess(const std::shared_ptr<Variante>& var, const vector<double>& theta, int status);
    void printCutDetailed(FILE* fr);
    void printCut(FILE* fr);
    static double cutDecimals(double x); //utilisé quand le round ne suffit pas, pour R3 et R3B
    int getClosestTapPosition(TransformateurDephaseur* td, double angleFinal);
    bool calculVariationsMarginales(FILE* fr, const std::map<std::shared_ptr<Incident>, int>& incidentsContraignants);
    static double round(double x, double prec); // utiliser pour arrondir les calculs
    Calculer(Reseau& res, MapQuadinVar& variantesOrdonnees);
    int PneSolveur(TypeDeSolveur typeSolveur, const std::shared_ptr<Variante>& varianteCourante);
    void comput_ParticipationGrp(const std::shared_ptr<Incident>& icdt) const;
    void fixerVariablesEntieres(); // Fixe les variables entieres pour lancement avec SPX

    // Modelisation DODU
    //*****************
    vector<std::shared_ptr<Contrainte>>
        icdtQdt_;            // vecteur decrivant la paire (icdt,quadripole) qui risque d induire une contrainte.
    unsigned int nbCtr_ = 0; // le nombre de contrainte enregistrees dans icdtQdt_
    int resolutionUnProblemeDodu(const std::shared_ptr<Variante>& varianteCourante); // resolution d'un probleme
    int allocationProblemeDodu();                                                    // programme dallocation
    int ecrireContraintesDodu(); // programme principal d'ecriture des cntraintes
    int ecrireContrainteBilanEnergetique(
        bool parZonesSynchr);                 // contraintes de bilan energetique global ou une par zone synchrone
    int ajouterContraintesCouplagesGroupes(); // Contraintes des variables couplees sur les groupes
    int ajouterContraintesCouplagesConsos();  // Contraintes des variables couplees sur les consos
    int ecrireContraintesDeBordGroupesDodu(); // contraintes de bord sur les groupes
    int ecrireContraintesDeBordConsosDodu();  // contraintes de bord sur les consommations nodales
    int ecrireContraintesDeBordTransformateurDephaseur(); // contraintes de bord sur les transformateurs dephaseur
    int ecrireContraintesDeBordLignesCC();                // contraintes de bord sur les ligne a courant continu
    int ecrireEquationBilanCuratif(const std::map<int, vector<int>>& mapZoneSyncGrp,
                                   const std::map<int, vector<int>>& mapZoneSyncConso,
                                   const std::map<int, vector<std::shared_ptr<ElementCuratifHVDC>>>&
                                       mapZoneSyncHvdc); // contrainte P=C si curatif de groupes
    int ajouterLimiteCuratifGroupe(
        const std::map<int, vector<int>>& mapZoneSyncGrp); // limitation du volume de redispatching curatif
    int construireJacobienne();
    int modifJacobienneLigne(
        const std::shared_ptr<Quadripole>& ligne,
        bool applique); // Modifier la jacobienne lorsqu'il y a des indispo sur une variante ( et operation inverse)
    int modifJacobienneInc(
        const std::shared_ptr<Incident>& icdt,
        bool applique); // Modifier la jacobienne lorsqu'il y a des indispo sur une variante ( et operation inverse)
    int modifJacobienneVar(
        const Quadripole::SetQuadripoleSortedByName& quads,
        bool applique); // Modifier la jacobienne lorsqu'il y a des indispo sur une variante ( et operation inverse)
    int initJacobienne();
    int miseAJourSecondMembre(vector<double>& secondMembre,
                              vector<double>& secondMembreFixe); // A theta = "secondMembre"
    int miseAJourSecondMembreSurIncident(const std::shared_ptr<Incident>& icdt,
                                         vector<double>& injectionsNodales,
                                         vector<double>& secondMembreFixe);
    int miseAJourSecondMembrePochePerdue(const std::shared_ptr<Incident>& icdt,
                                         const vector<double>& secondMembreN,
                                         const vector<double>& secondMembreFixeN);

    int ajouterVariablesCuratives(const std::shared_ptr<ElementCuratif>& elem, double proba);
    void ajouterContraintesBorneCuratif(int numVar, int numVarCur, double Pmin, double Pmax);
    void ajouterContraintesBorneCuratifGroupe(int numVarGrp, int numVarCur, const std::shared_ptr<Groupe>& grp);
    void ajouterContraintesBorneCuratifConso(int numVarPrev, int numVarCur, double consoNod, double pourcentEff);
    int ajoutContrainteActivation(int numContrainteInitiale, int numVarActivation);
    int ajoutContrainteActivationParade(const std::shared_ptr<Incident>& parade,
                                        const std::shared_ptr<Contrainte>& contrainte,
                                        const std::shared_ptr<Incident>& icdtPere,
                                        const vector<double>& secondMembreFixe);
    int ajoutContrainteValorisationPoche(const std::shared_ptr<Incident>& parade);

    int ajouterVariableEntiere(int numElemCur, double cost);
    void traiterCuratif(const std::shared_ptr<ElementCuratif>& elemC,
                        const std::shared_ptr<Incident>& icdtCourant,
                        int numOuvrSurv,
                        double& variationTdCuratif,
                        double& varTdSecondOrdre);

    int detectionContraintes(const vector<double>& secondMembre,
                             bool& existe_contrainte_active); // detection des contraintes en N et en N-k,
    void calculerFluxNk(const vector<double>& secondMembre);
    double transitSurQuad(const std::shared_ptr<Quadripole>& quad,
                          std::shared_ptr<Incident> icdt,
                          const vector<double>& theta);
    double transitSurQuadIncidentNonConnexe(const std::shared_ptr<Quadripole>& quad,
                                            const std::shared_ptr<Incident>& icdt) const;
    void choixContraintesAajouter();
    int ajoutContraintes(bool& existe_contrainte_active,
                         int& nbNewContreParVariante,
                         const vector<double>& secondMembreFixe,
                         const vector<double>& theta);
    int ajoutContrainte(const std::shared_ptr<Contrainte>& ctre, const vector<double>& secondMembreFixe);
    int coeffPourQuadEnN(
        const std::shared_ptr<Quadripole>& quad,
        const vector<double>& secondMembreFixe,
        double& sumcoeffFixe,
        double coeff); // calcule les coeff de la matrice sur simplexe pour exprimer le transit du quad en N
    int coeffPourQuadInc(const std::shared_ptr<Quadripole>& quad,
                         const std::shared_ptr<Contrainte>& ctre,
                         const vector<double>& secondMembreFixe,
                         double& sumcoeffFixe,
                         double coeff);
    int coeffPourQuadIncRompantConnexite(const std::shared_ptr<Quadripole>& quad,
                                         const std::shared_ptr<Incident>& icdt,
                                         double& sumcoeffFixe,
                                         double coeff);
    int ecrireCoupeTransit(const double& maxTprev, const double& minTprev, const std::shared_ptr<Contrainte>& ctre);
    int ajouterContrainteChoixTopo(const vector<std::shared_ptr<Incident>>& paradesActivees);
    int ajouterContrainteDeltaConsVarEntiere(const std::shared_ptr<ElementCuratif>& elemC);
    int ajouterContrainteDeltaConsVarEntiere(const std::shared_ptr<TransformateurDephaseur>& td);
    int ajouterContrainteNbMaxActCur(const std::shared_ptr<Incident>& parade);
    int ajoutVariableEcart(const std::shared_ptr<Contrainte>& ctr);
    double deltaEODPoche(const std::shared_ptr<PochePerdue>& poche);

    // dispatching economique
    int fixerProdSansReseau();
    int interdireDefaillance();
    void ajoutRedispatchCostOffsetConsos();

    // empilement economique des groupes
    int empilementEconomiqueDesGroupes(const std::shared_ptr<Variante>& varianteCourante);

    // Methodes de verification pour debug ou validation uniquement
    bool check_bonneDetectionContrainte(const std::shared_ptr<Contrainte>& cont);
    bool check_calculThetaSurIncident(const std::shared_ptr<Incident>& icdt,
                                      vector<double>& injectionSNodales,
                                      vector<double>& secondMembreFixe);
    bool check_bonneDetectionTouteContrainte(const std::shared_ptr<Incident>& icdt,
                                             vector<double>& injectionSNodales,
                                             vector<double>& secondMembreFixe);
    void compareLoadFlowReport();

    // Calcul des coefs de repport et des coefs d'influencement
    //********************************************************
    int calculReportInfluencement();
    int calculCoefsReport(std::shared_ptr<Incident> icdt);
    int calculCoefsInfluencement(const std::shared_ptr<Incident>& icdt);
    int calculInitCoefs(std::shared_ptr<Incident> icdt) const;
    int calculReportLcc(const std::shared_ptr<Incident>& inc) const;
    int calculReportLccs();
    int resetCoefs(std::shared_ptr<Incident> icdt) const;
    void resetCoeffQuadsN() const;
    int calculerCoeffEnN(const std::shared_ptr<Quadripole>& quad);
    int calculCoeffReportTD();
    int calculReportGroupesEtConsos();
    double hij(int lig, int col);
    int calculeInvB(const std::shared_ptr<Incident>& icdt,
                    vector<int>& BIndexDebutDesColonnes,
                    vector<int>& BNbTermesDesColonnes,
                    vector<double>& BValeurDesTermesDeLaMatrice,
                    vector<int>& BIndicesDeLigne,
                    int nbColones,
                    vector<vector<double>>& invB) const;
    int coeffsRepportPhases(const vector<std::shared_ptr<Quadripole>>& listeQuads,
                            int nbColonnes,
                            const vector<vector<double>>& invB_T,
                            vector<vector<double>>& g_mk) const;

    // Traces
    void printFctObj(bool silent);

private:
    struct CostDef {
        std::string type;
        int numIncident;
        double varMW;
        double cost;
        bool skipDisplay;
    };
    using CostDefMap = std::map<int, std::vector<CostDef>>;

private:
    static void afficherVariantesCle(const MapQuadinVar& variantesOrdonnees,
                                     const Quadripole::SetQuadripoleSortedByName& IndispoLignes);
    bool findNumIncident(const std::map<std::shared_ptr<Incident>, int>& incidentsContraignants,
                         int indexConstraint,
                         int& numIncident) const;
    bool computeCosts(const std::vector<int>& constraintsToDelail,
                      const std::shared_ptr<MarginVariationMatrix>& marginVariationMatrix,
                      const std::map<std::shared_ptr<Incident>, int>& incidentsContraignants,
                      const std::vector<int>& numVa,
                      const std::vector<int>& typeOu,
                      int ctrOuvr,
                      CostDefMap& cost) const;
    boost::optional<std::tuple<std::string, int, std::shared_ptr<Incident>>>
    getIncidentConstraint(const std::map<std::shared_ptr<Incident>, int>& incidentsContraignants, int i) const;
    void assessAndPrintPTDF(const string& PTDFfileName);
    void printLODF(const string& LODFfileName, bool writeLODFfile) const;

    void addCurativeVariable(const std::shared_ptr<TransformateurDephaseur>& td, double proba, int numVarCur);
    void addCurativeVariable(const std::shared_ptr<TransformateurDephaseur>& td_fictive);
    void addCurativeVariable(const std::shared_ptr<LigneCC>& lcc, double proba, int numVarCur);
    void addCurativeVariable(const std::shared_ptr<Groupe>& grp, double proba, int numVarCur);
    void addCurativeVariable(const std::shared_ptr<Consommation>& conso, double proba, int numVarCur);

private:
    void videMatricesDesContraintes();

    void printStats();
    void printMatriceDesContraintes();
};

#endif
