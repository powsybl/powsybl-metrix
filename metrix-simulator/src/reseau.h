//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#ifndef RESEAU_METRIX
#define RESEAU_METRIX

#include "config/configuration.h"
#include "config/constants.h"
#include "config/parades_configuration.h"
#include "config/variant_configuration.h"
#include "parametres.h"
#include "pne.h"

#include <map>
#include <memory>
#include <random>
#include <set>
#include <string>
#include <vector>

class Connexion;
class Noeud;
class Quadripole;
class Groupe;
class Consommation;
class Variante;
class Incident;
class TransformateurDephaseur;
class LigneCC;
class ElementASurveiller;
class PochePerdue;
class ElementCuratifHVDC;
class ElementCuratifTD;


enum ModeCuratif { PREVENTIF_SEUL = 0, CURATIF_POSSIBLE = 1 };

class ElementCuratif
{
public:
    enum TypeElement { /* types d'element curatif */
                       TD = 1,
                       HVDC = 2,
                       TD_FICTIF = 3,
                       GROUPE = 4,
                       CONSO = 5
    };

    explicit ElementCuratif(/*int num, */ TypeElement type) :
        // num_(num),
        typeElem_(type)
    {
    }

    // unsigned int num_ ;  // numero du TD ou de la HVDC
    TypeElement typeElem_;

    // donnees variables d'une variante a l'autre
    int positionVarCurative_ = -1;   // numero de la variable curative du TD (meme position que les incidents)
                                     // vaut 0 si la parade est active; 1 sinon
    int positionVarEntiereCur_ = -1; // pointe dans la place dans le vect. variable, la variable entiere qui active ou
                                     // desactive l'action

    void reset();
    virtual bool estValide() = 0;
    virtual int numVarPrev() = 0;
    virtual int num() = 0;
    virtual int zoneSynchrone() = 0;
    virtual const std::vector<double>& rho() = 0;

    ElementCuratif();
    virtual ~ElementCuratif() = default;

private:
    ElementCuratif(const ElementCuratif&);            // Constructeur de copie
    ElementCuratif& operator=(const ElementCuratif&); // Operateur d'affectation
};

class ConsommationsCouplees
{
public:
    std::string nomRegroupement_;
    std::set<std::shared_ptr<Consommation>> elements_;

    ConsommationsCouplees(const std::string& nom, const std::set<std::shared_ptr<Consommation>>& ids) :
        nomRegroupement_(nom),
        elements_(ids)
    {
    }

    std::string toString() const;
};


//--------------------
// classe Noeuds
//--------------------

class Noeud
{
public:
    enum PositionConnexion { ORIGINE = 1, EXTREMITE = 2 };

    enum TypeNoeud { NOEUD_REEL = 1, NOEUD_FICTIF = 2 };

    int unsigned num_;                          /* num des noeuds */
    TypeNoeud typeNoeud_ = Noeud::NOEUD_FICTIF; /* type du noeud*/
    int nbGroupes_ = 0;                         /* Nombre des groupes, fixe pour toute les variantes */
    int nbConsos_ = 0;                          /* Nombre des consommations, fixe pour toutes les variantes*/
    bool bilan_ = false;                        // est ce que le noeud est un noeud bilan
    int numCompSynch_ = 0;

    std::vector<Quadripole*> listeQuads_; /* les quadripoles voisins, fixe pour toutes les variantes*/
    int numRegion_;                       /* numero de la region */
    std::vector<Groupe*> listeGroupes_;   /* liste des groupes, fixe pour toutes les variantes */
    std::vector<Consommation*>
        listeConsos_; /* liste des consommations, fixe pour toutes les variantes
                         UTOPIQUE : on ne traite qu'une conso par noeud conformement au format Eurostag*/

    std::vector<std::shared_ptr<TransformateurDephaseur>>
        listeTd_;                                   /* liste des TD connectees a ce noeud, nullptr par defaut*/
    std::vector<std::shared_ptr<LigneCC>> listeCC_; /* liste des lignes a courant continu*/

    std::vector<double> rho_; // coefficient de report pour une modification EOD en curatif

    PositionConnexion
    position(const std::shared_ptr<Connexion>& branche) const; /* si noeud ORIGINE alors ORIGINE sinon EXTREMITE*/

    int nbQuads() const { return static_cast<int>(listeQuads_.size()); };
    int nbTd() const { return static_cast<int>(listeTd_.size()); };
    int nbCC() const { return static_cast<int>(listeCC_.size()); };

    double consoNodale();

    std::string print() const;

    Noeud(int num, int numRegion);
    ~Noeud() = default;

    Noeud(const Noeud&) = delete;            // Constructeur de copie
    Noeud& operator=(const Noeud&) = delete; // Operateur d'affectation
};


class Connexion
{
public:
    Connexion(const std::shared_ptr<Noeud>& nor, const std::shared_ptr<Noeud>& nex);
    ~Connexion() = default;

    Connexion(const Connexion&) = delete;            // Constructeur de copie
    Connexion& operator=(const Connexion&) = delete; // Operateur d'affectation

    bool etatOr_ = true;            /* etat de connection courant origine */
    bool etatEx_ = true;            /* etat de connection courant extremite */
    bool etatOrBase_ = true;        /* etat de connection Variante0 origine */
    bool etatExBase_ = true;        /* etat de connection Variante0 extremite */
    std::shared_ptr<Noeud> norqua_; /* ptr du Noeud origine du dipole, fixe pour toutes les variantes */
    std::shared_ptr<Noeud> nexqua_; /* ptr du Noeud extremite du dipole, fixe pour toutes les variantes */


    bool connecte() const { return etatOr_ && etatEx_; }
};

// Structure permettant de representer un transit lie a un defaut
struct Menace {
    std::shared_ptr<Incident> defaut_ = nullptr;
    double transit_ = 0.0;
};


//---------------------
// classe Quadripole
//---------------------

class Quadripole : public Connexion
{
public:
    enum TypeQuadripole { QUADRIPOLE_REEL = 1, QUADRIPOLE_FICTIF = 2, QUADRIPOLE_EMULATION_AC = 3 };
    struct CompareName {
        bool operator()(const std::shared_ptr<Quadripole>& a, const std::shared_ptr<Quadripole>& b) const
        {
            return a->nom_ < b->nom_;
        }
    };

    using SetQuadripoleSortedByName = std::set<std::shared_ptr<Quadripole>, CompareName>;

    std::string nom_;  /* Nom du dipole */
    int unsigned num_; /* num du dipole */
    TypeQuadripole typeQuadripole_ = Quadripole::QUADRIPOLE_REEL;

    std::shared_ptr<TransformateurDephaseur> td_; /* ptr vers le td sinon nullptr*/

    double y_;     /* Admittance du dipole */
    double r_;     /* Resistance du dipole */
    double u2Yij_; /* U*U*Yij */

    bool reconnectable_; /* indique si un quadripole ouvert peut-etre referme sans creer de boucle */

    std::shared_ptr<ElementASurveiller> elemAS_; /* pointeur vers l'element e surveiller */

    std::vector<double>
        coeffN_; /* Coefficients d'influencement en N, calcules au besoin, mais reinitialises si changement topo */

    Quadripole(int num,
               const std::string& nom,
               const std::shared_ptr<Noeud>& norqua,
               const std::shared_ptr<Noeud>& nexqua,
               double y,
               double r);
    Quadripole(const Quadripole&) = delete;            // Constructeur de copie
    Quadripole& operator=(const Quadripole&) = delete; // Operateur d'affectation

    int unsigned tnnorqua() const
    {
        return norqua_->num_;
    }; /* numero Noeud origine du dipole, fixe pour toutes les variantes */
    int unsigned tnnexqua() const
    {
        return nexqua_->num_;
    }; /* numero Noeud extremite du dipole, fixe pour toutes les variantes */
};

bool compareMenaces(const Menace& menace1, const Menace& menace2);

class ElementASurveiller
{
public:
    enum TypeSurveillance { /*  00*/
                            NON_SURVEILLE = 0,
                            SURVEILLE = 1,
                            AVEC_RESULTAT = 2
    };

    std::string nom_;  /* Nom de la section */
    int unsigned num_; /* num de la section */

    bool isWatchedSection; // watched element is part of watched section

    std::map<std::shared_ptr<Quadripole>, double> quadsASurv_; // liste des quads qui forment la section surveillee (qui
                                                               // peut etre reduite a une ligne a surveiller) et coeff
    std::map<std::shared_ptr<LigneCC>, double> hvdcASurv_;     // liste des HVDC qui forment la section surveillee

    TypeSurveillance survMaxN_;   // booleen qui indique si on surveille le transit sur N ou pas
    TypeSurveillance survMaxInc_; // ---- sur incident N-1 Ligne
    // bool survMin_;   // indique s'il faut aussi regarder  -seuilMax <  Transit

    double seuilMaxN_;                   // seuil max pour situ N
    double seuilMaxInc_;                 // seuil pour incident
    double seuilMaxIncComplexe_;         // seuil pour incident specifique
    double seuilMaxAvantCur_;            // seuil avant manoeuvre (ITAM)
    double seuilMaxAvantCurIncComplexe_; // seuil avant manoeuvre (ITAM) pour un incident specifique

    // Seuils Extremite -> Origine
    bool seuilsAssymetriques_ = false;
    double seuilMaxNExOr_ = config::constants::valdef;   // seuil max pour situ N
    double seuilMaxIncExOr_ = config::constants::valdef; // -- pour incident N-1 ligne
    double seuilMaxIncComplexeExOr_ = config::constants::valdef;
    double seuilMaxAvantCurExOr_ = config::constants::valdef; // seuil avant manoeuvre (ITAM)
    double seuilMaxAvantCurIncComplexeExOr_ = config::constants::valdef;

    double depassementEnN_ = 0.0; // depassement de seuil en N
    std::set<std::shared_ptr<Incident>> incidentsAvecTransit_;

    std::set<Menace, bool (*)(const Menace&, const Menace&)> menacesMax_
        = std::set<Menace, bool (*)(const Menace&, const Menace&)>(&compareMenaces);
    Menace menaceMaxAvantParade_;

    std::vector<int>
        listeContraintes_;      // utilise pour la classification des contraintes pour priorisation a mettre dans le pb
    std::map<int, int> ecarts_; // variables d'ecart sur incident <numInc,numVar> (situ N = -1)

    ElementASurveiller(const std::string& nom,
                       int num,
                       TypeSurveillance survN,
                       TypeSurveillance survInc,
                       double seuilN,
                       double seuilInc,
                       double seuilIncComplexe,
                       double seuilAvantCuratif,
                       bool isWatchedSection);
    ElementASurveiller(const ElementASurveiller&) = delete;            // Constructeur de copie
    ElementASurveiller& operator=(const ElementASurveiller&) = delete; // Operateur d'affectation

    void verificationSeuils() const;
    double seuilMax(const std::shared_ptr<Incident>& icdt) const;
    double seuilMin(const std::shared_ptr<Incident>& icdt) const;
    double seuil(const std::shared_ptr<Incident>& icdt, double transit) const;
};

//----------------
// classe Groupe
//----------------

class Groupe
{
    // Remarques :
    // 1 - chaque groupe dispose de deux variables P_hausse et P_baisse.
public:
    enum ProdAjustable { /* Indique si la production du groupe peut etre ajustee par METRIX */
                         NON_HR_AR = 0,
                         OUI_HR_AR = 1,
                         OUI_HR = 2,
                         OUI_AR = 3
    };

    std::string nom_;              /* Noms des groupes thermiques */
    int unsigned num_;             /* num du groupe*/
    int unsigned numNoeud_;        /* Sommets de raccordement des groupes thermiques */
    std::shared_ptr<Noeud> noeud_; /* noeud de connection*/
    bool etat_ = true;             /* etat de connection courant  */
    bool etatBase_ = true;         /* etat de connection cas de base */

    int type_; /* type de groupe */

    ProdAjustable prodAjust_; /* si production du groupe peut etre modifiee */
    double prod_;             /* Production active des groupes sans reseau*/
    double prodPobj_;         /* Production avant empilement economique sans reseau*/
    double prodPobjBase_;     /* Production avant empilement economique sans reseau (etat de base)*/
    double puisMin_;          /* Puissance min du groupe thermique */
    double puisMax_; /* Puissance max du groupe thermique (abattement pour le reglage de frequence pris en compte)*/
    double puisMaxDispo_; /* Puissance max disponible dans la variante */

    double puisMinBase_;      /* Puissance min du groupe thermique dans le cas de base */
    double puisMinAR_;        /* Puissance min du groupe thermique AR (HR = 0) */
    double puisMaxDispoBase_; /* Puissance max disponible du groupe thermique dans le cas de base */
    double demiBande_;        /* demi bande de reglage en reglage secondaire */

    // les couts
    double coutHausseHR_ = 0.0; /* cout Hors reseau*/
    double coutBaisseHR_ = 0.0; /* cout Hors reseau*/
    double coutHausseAR_ = 0.0; /* cout Avec reseau*/
    double coutBaisseAR_ = 0.0; /* cout Avec reseau*/

    double participation_ = 0.0; /* Participation du grp au reglage de frequence*/
    int numVarGrp_ = -1;         /* numero de la variable correspondant au groupe (numVarGrp_, numVarGrp_+1)*/
    // pour info numVar est different de num car les groupes sont retries pour que le demarrage HR soit arbitraire

    // Curatif
    std::set<int> incidentsAtraiterCuratif_; // incidents a traiter en curatif

    Groupe(int num,
           const std::string& nom,
           const std::shared_ptr<Noeud>& noeud,
           int type,
           float prodN,
           float puisMin,
           float puisMaxDispo,
           float demiBande,
           Groupe::ProdAjustable pimpmod);
    ~Groupe() = default;
    Groupe(const Groupe&) = delete;            // Constructeur de copie
    Groupe& operator=(const Groupe&) = delete; // Operateur d'affectation

    int checkCoherencePminMaxObj();
    bool estAjustable(bool adequacy) const;
};

class GroupesCouples
{
public:
    struct CompareGroupPtr {
        bool operator()(const std::shared_ptr<Groupe>& lhs, const std::shared_ptr<Groupe>& rhs) const
        {
            // lexicographic order on names
            return lhs->nom_ < rhs->nom_;
        }
    };
    using SetGroupPtr = std::set<std::shared_ptr<Groupe>, CompareGroupPtr>;
    enum VariableReference { PMAX = 0, PMIN = 1, POBJ = 2, PMAX_POBJ = 3 };

    std::string nomRegroupement_;
    VariableReference reference_;
    SetGroupPtr elements_;

    GroupesCouples(const std::string& nom, const SetGroupPtr& ids, VariableReference ref) :
        nomRegroupement_(nom),
        reference_(ref),
        elements_(ids)
    {
    }

    std::string toString() const;
};

//-----------------------
// classe Consommation
//-----------------------

class Consommation
{
public:
    unsigned int num_;      /* num de la conso */
    const std::string nom_; /* nom de la conso */
    int numVarConso_ = -1;  /* un numero est affecte si il existe une conso modifiable */

    std::shared_ptr<Noeud> noeud_; /* noeud de raccordement */
    double valeur_;                /* Valeur de la conso dans la variante */
    double valeurBase_;            /* Valeur de la conso dans le cas de base */

    double seuil_ = 0.0; /* pourcentage max de delestage preventif % */
    double cout_ = 0.0;  /* cout du delestage preventif */

    std::set<int> incidentsAtraiterCuratif_;                // indices des incidents a traiter en curatif
    double coutEffacement_ = config::constants::valdef;     /* Cout de l'effacement curatif dans la variante */
    double coutEffacementBase_ = config::constants::valdef; /* Cout de l'effacement curatif en base */
    double pourcentEffacement_ = 0.0;                       /* Poucentage d'effacement curatif de la conso */

    Consommation(int num, const std::string& nom, const std::shared_ptr<Noeud>& noeud, float valeur);
    ~Consommation() = default;

    Consommation(const Consommation&) = delete;            // Constructeur de copie
    Consommation& operator=(const Consommation&) = delete; // Operateur d'affectation
};


//-----------------------------------
// classe Transformateur Dephaseur
//-----------------------------------

class TransformateurDephaseur
{ // un transformateur dephaseur est coupe en 2 quad avec creation d'un fictif au milieu
  // quad et quadVrai_ (quadVrai_ a le nom d'origine du TD)
  // QuadVrai est un simple quadripole
  // Quad porte le TD qui est modelise par un depahsage supplmentaire
public:
    enum TypePilotageTD { /* types de TD */
                          HORS_SERVICE = 0,
                          PILOTAGE_ANGLE_OPTIMISE = 1,
                          PILOTAGE_ANGLE_IMPOSE = 2,
                          PILOTAGE_PUISSANCE_OPTIMISE = 3,
                          PILOTAGE_PUISSANCE_IMPOSE = 4
    };

    int unsigned num_;                     /* num du TD*/
    std::shared_ptr<Quadripole> quad_;     /* nom du quadripole fictif portant le dephasage*/
    std::shared_ptr<Quadripole> quadVrai_; /* nom du simple quadripole*/
    bool fictif_ = false;                  /* TD fictif representant une HVDC en emulation AC */
    double puiMin_;                        /* puissance minimale */
    double puiMax_;                        /* puissance maximale */
    double puiConsBase_;                   /* valeur correspondant au dephasage initial */
    double puiCons_;                       /* puissance apparente correspondant au dephasage */
    TypePilotageTD type_;
    ModeCuratif mode_;
    int lowtap_;                             /* numéro de la première prise (sert pour l'affichage) */
    int nbtap_;                              /* nombre de prises du td */
    std::vector<float> tapdepha_;            /* vecteur des prises de déphasage du TD  */
    int lowran_;                             /* borne minimale de variation de prises du td */
    int uppran_;                             /* borne maximale de variation de prises du td */
    std::set<int> incidentsAtraiterCuratif_; // indices des incidents a traiter en curatif
    int numVar_ = -1;                        // numero de la position de sa consigne en N dans le vecteur variable
    int numVarEntiere_ = -1;                 // numero de la variable entiere d'activation (td fictif)

    TransformateurDephaseur(int unsigned num,
                            const std::shared_ptr<Quadripole>& quadTd,
                            const std::shared_ptr<Quadripole>& quadVrai,
                            double pCons,
                            double pMin,
                            double pMax,
                            TypePilotageTD pilotage,
                            ModeCuratif mode,
                            int lowtap,
                            int nbtap,
                            const std::vector<float>& tapdepha,
                            int lowran = -1,
                            int uppran = -1);
    ~TransformateurDephaseur() = default;
    TransformateurDephaseur(const TransformateurDephaseur&) = delete;            // Constructeur de copie
    TransformateurDephaseur& operator=(const TransformateurDephaseur&) = delete; // Operateur d'affectation

    // donnees variables d'une variante a l'autre
    std::vector<double> rho_; // coefficients de report pour le changement de consigne du TD en curatif

    // Convertit un angle de dephasage (en degre) en puissance apparente
    double angle2Power(double angle) const;

    // Convertir la puissance apparente en angle (degre)
    double power2Angle(double power) const;

    // Methodes réalisant des opérations sur les prises de TD
    int getClosestTapPosition(double angleFinal);
    double getPuiMax();
    double getPuiMin();
};

//---------------------------------
// classe Ligne a  courant continu
//---------------------------------
class LigneCC : public Connexion
{
public:
    enum TypePilotageCC { /* types de ligne a CC */
                          HORS_SERVICE = 0,
                          PILOTAGE_PUISSANCE_OPTIMISE = 1,
                          PILOTAGE_PUISSANCE_IMPOSE = 2,
                          PILOTAGE_EMULATION_AC_OPTIMISE = 3,
                          PILOTAGE_EMULATION_AC = 4

    };
    int unsigned num_; /* num de la ligne a courant continu*/
    std::string nom_;  /* Nom de la liaison */

    double puiMin_;                      /* puissance minimale (pour faire des variantes)*/
    double puiMax_;                      /* puissance maximale (pour faire des variantes)*/
    double puiCons_;                     /* puissance de consigne (pour faire des variantes)*/
    double puiMinBase_;                  /* puissance minimale du cas de base */
    double puiMaxBase_;                  /* puissance maximale du cas de base */
    double puiConsBase_;                 /* puissance de consigne du cas de base */
    TypePilotageCC type_ = HORS_SERVICE; /* type de ligne a CC */
    ModeCuratif mode_ = PREVENTIF_SEUL;  // La HVDC peut elle agir en curatif

    // info pour calculer les pertes a posteriori de la liaison HVDC
    double coeffPertesOr_; // %pertes dans la station  Or
    double coeffPertesEx_; // %pertes dans la station  Ex
    double r_;             // resistance du cable DC
    double vdc_;           // tension du cable DC

    int numVar_ = -1;                        // numero de la position de sa consigne en N dans le vecteur variable
    std::set<int> incidentsAtraiterCuratif_; // indices des incidents a traiter en curatif

    std::shared_ptr<Quadripole> quadFictif_; // modelisation de l'emulation AC

    // donnees variables d'une variante a l'autre
    std::vector<double> rho_; // coefficient de report pour le changement de consigne de la HVDC

    bool isEmulationAC() const;

    LigneCC(int unsigned num,
            const std::string& nom,
            const std::shared_ptr<Noeud>& nor,
            const std::shared_ptr<Noeud>& nex,
            double pMin,
            double pMax,
            double de0,
            double coeffPerteOr,
            double coeffPerteEx,
            double r,
            double vdc);
    ~LigneCC() = default;

    LigneCC(const LigneCC&) = delete;            // Constructeur de copie
    LigneCC& operator=(const LigneCC&) = delete; // Operateur d'affectation
};


//-------------------
// classe Reseau
//-------------------

struct CompareSet;
using MapQuadinVar
    = std::map<Quadripole::SetQuadripoleSortedByName, std::vector<std::shared_ptr<Variante>>, CompareSet>;

class Reseau
{
    enum TypeOuvrage {  // pour les incidents et les sections surveillees
        QUADRIPOLE = 1, // ligne, transfo ou ou TD
        GROUPE = 2,
        HVDC = 3
    };

public:
    Reseau() { random.seed(1); };

    void updateBase(const config::VariantConfiguration::VariantConfig& config);
    void updateVariants(MapQuadinVar& mapping, const config::VariantConfiguration& config);
    void updateVariant(MapQuadinVar& mapping, const config::VariantConfiguration::VariantConfig& config);
    void updateParades(const config::ParadesConfiguration& config);

    std::vector<std::string> regions_;           /* noms des regions */
    std::vector<std::shared_ptr<Noeud>> noeuds_; /* indices des noeuds*/
    std::set<std::shared_ptr<Quadripole>>
        quadsSurv_; /* ensemble des quadripoles surveilles (pour le calcul des coefficient de report) */

    std::map<std::string, std::shared_ptr<Quadripole>>
        quads_; /* quadripoles du reseau (y compris couplages et TD) pour recherche par nom */
    std::map<std::string, std::shared_ptr<Groupe>> groupes_;      /* groupes du reseau */
    std::map<std::string, std::shared_ptr<Consommation>> consos_; /* consos du reseau */
    std::map<std::string, std::shared_ptr<TransformateurDephaseur>>
        TransfoDephaseurs_;                                    /* Transfos dephaseurs du reseau */
    std::map<std::string, std::shared_ptr<LigneCC>> LigneCCs_; /* Liaisons e courant continu */

    std::vector<std::shared_ptr<TransformateurDephaseur>> tdParIndice_; /* Indices des transformateurs dephaseurs */
    std::vector<std::shared_ptr<LigneCC>> lccParIndice_;                /* Indices des lignes CC */

    std::map<std::string, std::shared_ptr<Incident>> incidents_; /* incidents (hors parades) */
    std::vector<std::shared_ptr<Incident>> incidentsEtParades_;  /* incidents suivi des parades topologiques */
    std::vector<std::shared_ptr<Incident>>
        incidentsRompantConnexite_;                             /* std::vector des d'incidents rompant la connexite*/
    std::set<std::shared_ptr<Incident>> incidentsAvecTransits_; /* std::set des incidents avec resultats detailles*/
    std::vector<std::shared_ptr<TransformateurDephaseur>>
        TDFictifs_;                           /* std::vector des TD fictifs (modelisant des HVDC en emulation AC) */
    std::map<int, int> numNoeudBilanParZone_; /* liste des numeros de zones synchrones de 0 a nbZones-1 et le numero du
                                            noeud bilan (entre 0 et nbNoeud-1) */
    std::map<int, double> bilanParZone_;      /* valeurs de bilans initiaux */

    std::vector<std::shared_ptr<ElementASurveiller>>
        elementsASurveiller_; // ensemble des quads, sections surveilles... tout confondu
    std::map<std::string, std::shared_ptr<ElementASurveiller>>
        elementsASurveillerN_; // ensemble des quads, sections surveilles... pour lesquels
                               // il faut surveiller le transit en N
    std::map<std::string, std::shared_ptr<ElementASurveiller>>
        elementsASurveillerNk_; // ensemble des quads, sections surveilles... pour lesquels
                                // il faut surveiller le transit en N-k
    std::vector<std::shared_ptr<ElementASurveiller>>
        elementsAvecResultatNk_; // ensemble des quads, sections surveilles... pour lesquels on
                                 // veut juste le transit en N-k

    int nbGroupesCouples_ = 0;
    std::vector<std::shared_ptr<GroupesCouples>> groupesCouples_;
    int nbConsosCouplees_ = 0;
    std::vector<std::shared_ptr<ConsommationsCouplees>> consosCouplees_;

    std::vector<std::shared_ptr<Groupe>> groupesEOD_;

    /* Dimensions du probleme */
    int nbRegions_ = 0;          /* Nombre de regions */
    int nbNoeuds_ = 0;           /* Nombre total de sommets (noeuds) */
    int nbNoeudsReel_ = 0;       /* Nombre total de sommets reel (noeuds) sans les noeuds cree par les td*/
    int nbConsos_ = 0;           /* Nombre de consommations elementaires */
    int nbConsosCuratifs_ = 0;   /* Nombre de consommations pouvant agir en curatif */
    int nbQuads_ = 0;            /* Nombre de quadripoles elementaires */
    int nbGroupes_ = 0;          /* Nombre de groupes*/
    int nbTypesGroupes_ = 0;     /* Nombre de types de groupes */
    int nbGroupesNonFictif_ = 0; /* Nombre de groupes non fictif*/
    int nbGroupesCuratifs_ = 0;  /* Nombre de groupes pouvant agir en curatif */
    int nbTd_ = 0;               /* Nombre total de transfos dephaseurs */
    int nbTdCuratif_ = 0;        /* Nombre total de transfos dephaseurs curatif */
    int nbOpenBranches_ = 0;     /* Nombre de liaisons ouvertes dans le reseau initial */

    bool actionsPreventivesPossibles_ = false; /* Indique si des actions preventives sont possibles */

    int nbVarConsos_ = 0;            /* Nombre de variables consommations (delestage) */
    int nbVarGroupes_ = 0;           /* Nombre de variables groupes (P+, P-)*/
    int nbVarTd_ = 0;                /* Nombre de variables TD (x+, x-) */
    int nbVarCc_ = 0;                /* Nombre de variables HVDC (x+, x-) */
    int nbCC_ = 0;                   /* Nombre de ligne a courant continu*/
    int nbCCEmulAC_ = 0;             /* Nombre de ligne a courant continu en pilotage emulation AC */
    int nbCCCuratif_ = 0;            /* Nombre total de ligne a courant en continu curatif*/
    int nbPostes_ = 0;               /* Nombre de poste*/
    int nbQuadSurvN_ = 0;            /* Nombre total de quadripoles a surveiller (en N) */
    int nbQuadSurvNk_ = 0;           /* Nombre total de quadripoles a surveiller (en N-k) */
    int nbQuadResultNk_ = 0;         /* Nombre total de quadripoles avec resultat (en N-k) */
    int nbIncidents_ = 0;            /* Nombre d incidents*/
    int nbIncidentsHorsParades_ = 0; /* Nombre d incidents sans compter les parades */
    int nbIncidentsBase_ = 0;        /* Nombre d incidents et parades (hors parades auto)*/
    int nbSectSurv_ = 0;             // nombre de sections surveillees
    bool defautsGroupesPresents_
        = false; /* Indique si des defauts groupes sont presents : si oui, on utlisera la demibande de reglage*/
    float coeff_pertes_ = 0.0;      /* coeff de pertes utilise par ASSESS : conso = conso (1+coeff_pertes_/100) */
    double DBandeRegGlobale_ = 0.0; /* Demi bande de reglage Globale*/

    double prodMaxPossible_ = 0.0;
    bool calculerCoeffReport_ = true; // indique s il faut calculer ou recalculer les coefficients de report

    std::vector<std::string> typesGroupes_; // nom des types de groupes

    std::map<int, std::vector<std::shared_ptr<Groupe>>>
        varGroupesZones_; /* groupes melanges d'une zone (conserve si loi ECHANGP et relance sur tx de pertes) */

    std::map<std::shared_ptr<Quadripole>, std::set<std::shared_ptr<Incident>>> transitsSurDefauts_;
    std::map<std::string, std::set<std::shared_ptr<Incident>>> variationsMarginalesDetaillees_;

    /*lecture des donnees*/
    void lireDonnees();
    std::shared_ptr<Incident> ajouteParadeNeRienFaire(const std::shared_ptr<Incident>& incident);

    void update_with_configuration();

    int modifReseau(const std::shared_ptr<Variante>& var); /*modification du reseau (production, conso, etc) afin de
                                                    prendre en compte les variantes*/
    int modifReseauTopo(const Quadripole::SetQuadripoleSortedByName& quads); /*modification de la topologie du reseau
                                                                           afin de prendre en compte les variantes*/
    int modifBilans(const std::shared_ptr<Variante>& var); /*modification des bilans regionaux de la variante en jouant
                                                           sur les productions*/
    int modifTauxDePertes(float ancienTx,
                          float nouveauTx); /* modification du reseau pour utiliser un autre taux de pertes */
    int resetReseau(const std::shared_ptr<Variante>& var,
                    bool toutesConsos); /*base variant restoration of the network (all modifications except unavailable
                                           quadripoles)*/
    int resetReseauTopo(const Quadripole::SetQuadripoleSortedByName&
                            quads); /*base variant topology restoration of the network (for all unavailable lines)*/
    void miseAjourPmax(double prodEnMoins);

    // Methods for connectedness analysis
    bool connexite(const std::shared_ptr<Incident>& icdt,
                   bool choisiNoeudsBilan,
                   bool detailsNonConnexite); /* connedtedness calculation for a given contingency or remedial action*/
    bool connexite();                         /* connedtedness calculation for the network */

    int fusionner(int nZone1, int nZone2, std::map<int, int>& mapZones); /* merge two zones */
    int inclure(int nNoeud, int nZone, std::vector<int>& numZones);      /* include a node in a zone */
    static int creer(int nNoeud1, int nNoeud2, std::vector<int>& numZones, int nZoneCourant); /* create a new zone */
    int mapZone(int zone, std::map<int, int>& mapZones); /* find the smallest id of connected zones */
    int traiterConnexion(int nNoeudOrg,
                         int nNoeudExt,
                         std::vector<int>& numZones,
                         std::map<int, int>& mapZones,
                         int numZoneCourant); /* basic treatment of a two nodes connected quadripole */

    void afficheSousReseau(unsigned int numNoeud, unsigned int prof); /* For debug */
    void afficheSousReseau(int numNoeud, int numPere, unsigned int prof, int nbIndent, std::stringstream& ss);

    // For reproductible random between platforms
    static std::mt19937 random;

private:
    std::shared_ptr<TransformateurDephaseur> creerTD(const std::shared_ptr<Quadripole>& quadVrai,
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
                                                     const std::vector<float>& tapdepha,
                                                     int lowran,
                                                     int uppran);

    int findRegion(const std::string& name);
    std::tuple<bool, bool, std::vector<int>> checkConnexite(bool choisiNoeudsBilan);
    void
    correctConnexite(bool was_connexe,
                     const std::shared_ptr<Incident>& icdt,
                     const std::vector<int>& numZones,
                     bool detailsNonConnexite); /*allows to determine the treatment to be done for the contingencies and
                                                   remedial actions responisble for a connectedness break */
};


//------------------
// classe Incident
//------------------

struct ListeQuadsIncident {
    bool initialise;
    std::vector<std::shared_ptr<Quadripole>> quadsOuverts;
    std::vector<std::shared_ptr<Quadripole>> quadsRefermes;

    bool operator<(const ListeQuadsIncident& l) const
    {
        return std::tie(quadsOuverts, quadsRefermes) < std::tie(l.quadsOuverts, l.quadsRefermes);
    }
};

class Incident
{
    // NB : les parametres rho_ et lambda_ ne dependent que de la topologie et des caracteristiques des ouvrages.


public:
    enum TypeIncident { /* types d incident */
                        N_MOINS_1_LIGNE = 1,
                        N_MOINS_K_GROUPE = 2,
                        N_MOINS_K_GROUPE_LIGNE = 3,
                        INCONNU = 4
    };

    explicit Incident(TypeIncident type) : type_(type) { init(); };

    Incident(const Incident& copie) :
        type_(copie.type_),
        nbLignes_(copie.nbLignes_),
        nbGroupes_(copie.nbGroupes_),
        nbLccs_(copie.nbLccs_),
        incidentComplexe_(copie.incidentComplexe_),
        listeQuads_(copie.listeQuads_),
        listeGroupes_(copie.listeGroupes_),
        listeLccs_(copie.listeLccs_),
        nbCouplagesFermes_(copie.nbCouplagesFermes_),
        listeCouplagesFermes_(copie.listeCouplagesFermes_),
        probabilite_(copie.probabilite_),
        probabiliteBase_(copie.probabiliteBase_)
    {
        init();
    }

    Incident& operator=(const Incident& other)
    {
        type_ = other.type_;
        nbLignes_ = other.nbLignes_;
        nbGroupes_ = other.nbGroupes_;
        nbLccs_ = other.nbLccs_;
        incidentComplexe_ = other.incidentComplexe_;
        listeQuads_ = other.listeQuads_;
        listeGroupes_ = other.listeGroupes_;
        listeLccs_ = other.listeLccs_;
        nbCouplagesFermes_ = other.nbCouplagesFermes_;
        listeCouplagesFermes_ = other.listeCouplagesFermes_;
        probabilite_ = other.probabilite_;
        probabiliteBase_ = other.probabiliteBase_;

        init();
        return *this;
    }

    Incident& operator=(Incident&&) = default;
    Incident(Incident&&) = default;

    ~Incident() = default;

    TypeIncident type_; /* type d incident */
    int nbLignes_ = 0;  /* nombre de lignes a etudier  */
    int nbGroupes_ = 0; /* nombre de groupes a etudier */
    int nbLccs_ = 0;    /* nombre de groupes a etudier */
    int unsigned num_;  /* numero de l'incident */
    bool validite_;     /* indicateur de validite de l'incident, ex : "false" si perte de connexite */
    bool validiteBase_; /* indicateur de validite de l'incident en base, pour les variantes contennant des indispo
                           lignes */
    bool parade_;       /* ce n'est pas un vrai incident mais une parade */
    bool incidentComplexe_ = false;                       /* utilisation du seuil des incidents complexes */
    std::vector<std::shared_ptr<Quadripole>> listeQuads_; /* liste des quadripoles */
    std::vector<std::shared_ptr<Groupe>> listeGroupes_;   /* liste des groupes */
    std::vector<std::shared_ptr<LigneCC>> listeLccs_;     /* liste des liaisons A courant continu */
    std::vector<std::vector<double>> rho_;                /* coeff de repport et coeff d'influencement
                                                     rho = [ [] coeffs de repport des lignes
                                                             ...
                                                             [] coeffs de repport des couplages a fermer
                                                             ...
                                                             [] coeffs d'influencement des groupes
                                                             ...
                                                             [] coeffs d'influencement des LCC
                                                           ]*/
    std::vector<std::vector<double>> lambda_;             /* derivees, ou sensibilites, de l incident
                                                    par rapport aux injections nodales */

    std::vector<std::shared_ptr<ElementCuratif>>
        listeElemCur_; // liste des elements pouvant agir en curatif sur cet incident
    std::map<std::shared_ptr<LigneCC>, std::shared_ptr<ElementCuratifHVDC>>
        lccElemCur_; // pour rechercher les hvdc curatifs sur l'incident
    std::map<std::shared_ptr<Quadripole>, std::shared_ptr<ElementCuratifTD>>
        tdFictifsElemCur_; // pour rechercher les td fictif de l'incident

    // Donnees pour les fermetures de couplages (uniquement pour une parade) :
    int nbCouplagesFermes_ = 0;
    std::vector<std::shared_ptr<Quadripole>> listeCouplagesFermes_; /*liste des couplages a fermer dans la parade*/

    // donnees variables d'une variante a l autre
    bool incidentATraiterEncuratif_;
    bool pocheRecuperableEncuratif_;
    std::string nom_;
    double probabilite_
        = config::configuration()
              .probaInc(); // probabilite d'un incident (peut être définie à une valeur différente selon la variante).
    double probabiliteBase_
        = config::configuration().probaInc(); // probabilite de base de l'incident (sert pour les variantes)
    double getProb() const; // fonction renvoi probabilite d'un incident ou d'une parade. Si parade, prob est celle de
                            // l'incident de la parade

    // gestion des parades topo
    std::vector<std::shared_ptr<Incident>> parades_; // les parades possibles pour cet incident
    std::shared_ptr<Incident>
        paradeAuto_; // une parade ajoutee automatiquement par METRIX (a supprimer e la fin de la variante)
    std::vector<int> contraintes_; // identifiants des contraintes entrees dans le probleme suite e cet incident (pour
                                   // detecter les equivalences)
    bool paradesActivees_; // indique si les parades de cet incident ont deje ete activees (pour ne pas les reactiver)
    int numVarActivation_; // pour les inc. de type parade, num ds le vect. variable de la var entiere ( activ ou aps de
                           // la parade)
    std::shared_ptr<Incident> incTraiteCur_; // si parade, inc que la parade cherche a resoudre
    std::set<std::shared_ptr<ElementASurveiller>>
        contraintesAutorisees_; // liste des contraintes pour lesquelles la parade peut etre activee

    std::shared_ptr<PochePerdue> pochePerdue_; // representation des noeuds deconnectes si l'incident rompt la connexite
    ListeQuadsIncident listeQuadsIncident()
    {
        if (!listeQuadsIncident_.initialise) {
            listeQuadsIncident_.quadsOuverts = listeQuads_;
            listeQuadsIncident_.quadsRefermes = listeCouplagesFermes_;
            listeQuadsIncident_.initialise = true;
        }
        return listeQuadsIncident_;
    }

private:
    ListeQuadsIncident listeQuadsIncident_;


    void init()
    {
        nom_ = "";
        num_ = 0;
        validite_ = true;
        validiteBase_ = true;
        incidentATraiterEncuratif_ = false;
        pocheRecuperableEncuratif_ = false;
        parade_ = false;
        paradesActivees_ = false;
        paradeAuto_ = nullptr;
        numVarActivation_ = -1;
        incTraiteCur_ = nullptr;
        pochePerdue_ = nullptr;
        listeQuadsIncident_.initialise = false;
    }
};


class ElementCuratifTD : public ElementCuratif
{
public:
    std::shared_ptr<TransformateurDephaseur> td_;

    explicit ElementCuratifTD(const std::shared_ptr<TransformateurDephaseur>& td) :
        ElementCuratif(td->fictif_ ? TD_FICTIF : TD),
        td_(td)
    {
    }

    bool estValide() final { return td_->quadVrai_->connecte(); }

    int numVarPrev() final { return td_->numVar_; }

    int num() final { return td_->num_; }

    int zoneSynchrone() final { return td_->quadVrai_->norqua_->numCompSynch_; }

    const std::vector<double>& rho() final { return td_->rho_; }
};

class ElementCuratifHVDC : public ElementCuratif
{
public:
    std::shared_ptr<LigneCC> lcc_;

    explicit ElementCuratifHVDC(const std::shared_ptr<LigneCC>& lcc) : ElementCuratif(HVDC), lcc_(lcc) {}

    bool estValide() final { return lcc_->connecte(); }

    int numVarPrev() final { return lcc_->numVar_; }

    int num() final { return lcc_->num_; }

    int zoneSynchrone() final { return -1; } // not available for HVDC

    const std::vector<double>& rho() final { return lcc_->rho_; }
};

class ElementCuratifGroupe : public ElementCuratif
{
public:
    std::shared_ptr<Groupe> groupe_;

    explicit ElementCuratifGroupe(const std::shared_ptr<Groupe>& grp) : ElementCuratif(GROUPE), groupe_(grp) {}

    bool estValide() final { return groupe_->etat_; }

    int numVarPrev() final { return groupe_->numVarGrp_; }

    int num() final { return groupe_->numNoeud_; }

    int zoneSynchrone() final { return groupe_->noeud_->numCompSynch_; }

    const std::vector<double>& rho() final { return groupe_->noeud_->rho_; }
};

class ElementCuratifConso : public ElementCuratif
{
public:
    std::shared_ptr<Consommation> conso_;
    double max_ = 1.0; // valeur max de l'effacement (en %)

    explicit ElementCuratifConso(const std::shared_ptr<Consommation>& conso) : ElementCuratif(CONSO), conso_(conso) {}

    bool estValide() final { return conso_->valeur_ > 0; }

    int numVarPrev() final { return conso_->numVarConso_; }

    int num() final { return conso_->noeud_->num_; }

    int zoneSynchrone() final { return conso_->noeud_->numCompSynch_; }

    const std::vector<double>& rho() final { return conso_->noeud_->rho_; }
};

// Fonction pour ordonner les noeuds
bool compareNoeuds(const std::shared_ptr<Noeud>& noeud1, const std::shared_ptr<Noeud>& noeud2);

class PochePerdue
{
public:
    using NodeSet
        = std::set<std::shared_ptr<Noeud>, bool (*)(const std::shared_ptr<Noeud>&, const std::shared_ptr<Noeud>&)>;
    struct ConsumptionSort {
        bool operator()(const Consommation* lhs, const Consommation* rhs) const { return lhs->nom_ < rhs->nom_; }
    };

public:
    PochePerdue(const std::shared_ptr<Incident>& icdt, std::map<std::shared_ptr<Noeud>, int>& listeNoeuds);
    PochePerdue& operator=(const PochePerdue& other);
    PochePerdue(const PochePerdue& poche);
    PochePerdue(PochePerdue&&) = default;
    PochePerdue& operator=(PochePerdue&&) = default;
    ~PochePerdue() = default;

    NodeSet noeudsPoche_ = NodeSet(&compareNoeuds);
    double prodMaxPoche_ = 0.0;
    double prodPerdue_ = 0.0;
    double consoPerdue_ = 0.0;
    std::map<const Consommation*, double, ConsumptionSort> consumptionLosses_;
    bool pocheAvecConsoProd_ = false;

    std::vector<double> phases_;
    std::vector<double> secondMembreFixe_;
    std::map<std::shared_ptr<Quadripole>, std::vector<double>> coefficients_;
    std::shared_ptr<Incident> incidentModifie_;

    std::string print() const;
};

#endif
