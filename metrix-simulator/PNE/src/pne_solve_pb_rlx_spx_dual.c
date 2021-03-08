/***********************************************************************

   FONCTION: Resolution d'un probleme relaxe par le simplexe dual 
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_constantes_externes.h"
# include "spx_definition_arguments.h"
# include "spx_define.h"
# include "spx_fonctions.h"   

# include "bb_define.h"
# include "bb_fonctions.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define SEUIL_POUR_PRIORITE_DANS_SPX_AUX_VARIABLES_SORTANTES_ENTIERES  0.75 /* 75% soit 3/4 */

# define MARGE 1.e-6

/*----------------------------------------------------------------------------*/

void PNE_SolvePbRlxSpxDual( PROBLEME_PNE * Pne,
	             char     PremiereResolutionAuNoeudRacine,      /* Information en Entree */
	             double   CoutMax,			                        /* Information en Entree */
	             int      UtiliserCoutMax,                      /* Information en Entree: Oui ou non */
               int      BaseFournie,                          /* Information en Entree: Oui ou non */
               int    * PositionDeLaVariable,                 /* Information en Entree et Sortie */
               int    * NbVarDeBaseComplementaires,           /* Information en Entree et Sortie */
               int    * ComplementDeLaBase,                   /* Information en Entree et Sortie */
               int    * Faisabilite 
                          )
{
int BaseDeDepartFournie; int LibererMemoireALaFin; int Contexte; int i; double S; double Marge;
int ExistenceDUneSolution; int ChoixDeLAlgorithme; int NombreMaxDIterations;	 
time_t HeureDeCalendrierDebut; time_t HeureDeCalendrierCourant; double TempsEcoule; int Nb;
double * VariablesDualesDesContraintesTravEtDesCoupes; int * ContrainteSaturee;
PROBLEME_SIMPLEXE Probleme; BB * Bb; PROBLEME_SPX * Spx;

#if VERBOSE_PNE
  printf("----------------------------------------------------------\n");
  printf("Appel du simplexe dual. Nombre de variables %d contraintes %d\n",Pne->NombreDeVariablesTrav,Pne->NombreDeContraintesTrav);
  fflush(stdout);
#endif

*Faisabilite = OUI_PNE;	

if ( Pne->TailleAlloueeVariablesDualesDesContraintesTravEtDesCoupes < Pne->NombreDeContraintesTrav + Pne->Coupes.NombreDeContraintes ) {
  i = Pne->NombreDeContraintesTrav + Pne->Coupes.NombreDeContraintes;
	if ( Pne->VariablesDualesDesContraintesTravEtDesCoupes == NULL ) {
    Pne->VariablesDualesDesContraintesTravEtDesCoupes = (double *) malloc( i * sizeof( double ) ); 
	}
	else {
    Pne->VariablesDualesDesContraintesTravEtDesCoupes = (double *) realloc( Pne->VariablesDualesDesContraintesTravEtDesCoupes, i * sizeof( double ) ); 
	}
  if ( Pne->VariablesDualesDesContraintesTravEtDesCoupes == NULL ) {
    printf("PNE, memoire insuffisante dans le sous programme PNE_SolvePbRlxSpxDual  \n"); 
    Pne->AnomalieDetectee = OUI_PNE;
    longjmp( Pne->Env , Pne->AnomalieDetectee ); /* rq: le 2eme argument ne sera pas utilise */
  }
	Pne->TailleAlloueeVariablesDualesDesContraintesTravEtDesCoupes = i;
}

LibererMemoireALaFin = NON_SPX;

if ( PremiereResolutionAuNoeudRacine == OUI_PNE ) {
  if ( Pne->NombreDeVariablesEntieresTrav <= 0 ) {
    /* Si pas de variables entieres, alors simplexe seul */
    Contexte = SIMPLEXE_SEUL;
  }
  else {
    /* Premiere resolution du noeud racine dans le cas du branch and bound */
    Contexte = BRANCH_AND_BOUND_OU_CUT;	  
  }
}
else {
  /* Resolutions suivantes dans le cas du branch and bound */
  Contexte = BRANCH_AND_BOUND_OU_CUT_NOEUD;
}

BaseDeDepartFournie = BaseFournie;

ChoixDeLAlgorithme = SPX_DUAL;

if ( Contexte != BRANCH_AND_BOUND_OU_CUT_NOEUD ) {
  if ( Pne->ProblemeSpxDuSolveur != NULL ) {
    SPX_LibererProbleme( (PROBLEME_SPX *) Pne->ProblemeSpxDuSolveur );
    Pne->ProblemeSpxDuSolveur = NULL;
  }
}

/* Si on vient d'ajouter des coupes calculees c'est qu'il s'agit de la reoptimisation d'un noeud deja 
   resolu. Dans ce cas, on impose une limitation du nombre d'iterations. */
if ( Pne->NombreDeCoupesCalculees > 0 ) {
  NombreMaxDIterations = (int) (5. * Pne->NombreDeCoupesCalculees); 
  /*if ( NombreMaxDIterations < 1000 ) NombreMaxDIterations = 1000;*/

  if ( NombreMaxDIterations < 10000 ) NombreMaxDIterations = 10000;
	
}
else {
  /* On met un nombre negatif pour que la donnee ne soit pas prise en compte */
  NombreMaxDIterations = -10;
}
	 						
Probleme.Contexte = Contexte;
Probleme.NombreMaxDIterations = NombreMaxDIterations;
Probleme.DureeMaxDuCalcul     = -1.;
  
Probleme.CoutLineaire      = Pne->LTrav;
Probleme.X                 = Pne->UTrav;
Probleme.Xmin              = Pne->UminTrav;
Probleme.Xmax              = Pne->UmaxTrav;
Probleme.NombreDeVariables = Pne->NombreDeVariablesTrav;
Probleme.TypeDeVariable    = Pne->TypeDeBorneTrav;

Probleme.NombreDeContraintes                   = Pne->NombreDeContraintesTrav;
Probleme.IndicesDebutDeLigne                   = Pne->MdebTrav;
Probleme.NombreDeTermesDesLignes               = Pne->NbTermTrav;
Probleme.IndicesColonnes                       = Pne->NuvarTrav;
Probleme.CoefficientsDeLaMatriceDesContraintes = Pne->ATrav;
Probleme.Sens                                  = Pne->SensContrainteTrav;
Probleme.SecondMembre                          = Pne->BTrav;     

Probleme.ChoixDeLAlgorithme = ChoixDeLAlgorithme;    

Probleme.TypeDePricing               = PRICING_STEEPEST_EDGE /*PRICING_DANTZIG*/;
Probleme.FaireDuScaling              = OUI_SPX /*OUI_SPX*/;   
Probleme.StrategieAntiDegenerescence = AGRESSIF;
  
Probleme.BaseDeDepartFournie        = BaseDeDepartFournie;
Probleme.PositionDeLaVariable       = PositionDeLaVariable;
Probleme.NbVarDeBaseComplementaires = *NbVarDeBaseComplementaires;
Probleme.ComplementDeLaBase         = ComplementDeLaBase;
  
Probleme.LibererMemoireALaFin  = LibererMemoireALaFin;	

/* On peut abaisser Coutmax en fonction de la tolerance */
Marge = 0;

/*
if ( UtiliserCoutMax == OUI_SPX ) {
  Bb = (BB *) Pne->ProblemeBbDuSolveur;  
  if ( Bb != NULL ) Marge = Bb->ToleranceDOptimalite * 0.01 * fabs( Bb->CoutDeLaMeilleureSolutionEntiere ) * 0.1;
 }
*/

/* Modif 6/10/2016 pour etre en accord avec la partie branch and bound on supprime le coeff 0.1 dans le calcul de Marge
   et on fait CoutMax - Marge au lieu de CoutMax + Marge */
if ( UtiliserCoutMax == OUI_SPX ) {
  Bb = (BB *) Pne->ProblemeBbDuSolveur;  
  if ( Bb != NULL ) Marge = Bb->ToleranceDOptimalite * 0.01 * fabs( Bb->CoutDeLaMeilleureSolutionEntiere );	
  if ( Marge < 1.e-3 ) Marge = 1.e-3;
}

Probleme.CoutMax = CoutMax + Marge;
	 
Probleme.UtiliserCoutMax = UtiliserCoutMax;

Probleme.NombreDeContraintesCoupes        = Pne->Coupes.NombreDeContraintes;
Probleme.BCoupes                          = Pne->Coupes.B;
Probleme.PositionDeLaVariableDEcartCoupes = Pne->Coupes.PositionDeLaVariableDEcart;
Probleme.MdebCoupes                       = Pne->Coupes.Mdeb;
Probleme.NbTermCoupes                     = Pne->Coupes.NbTerm;
Probleme.NuvarCoupes                      = Pne->Coupes.Nuvar;
Probleme.ACoupes                          = Pne->Coupes.A;

Probleme.CoutsMarginauxDesContraintes = Pne->VariablesDualesDesContraintesTravEtDesCoupes;

Probleme.CoutsReduits = Pne->CoutsReduits;
  
Probleme.AffichageDesTraces = Pne->AffichageDesTraces;

if ( Pne->DureeDuPremierSimplexe <= 0. ) time( &HeureDeCalendrierDebut );

Pne->ProblemeSpxDuSolveur = SPX_Simplexe( &Probleme , Pne->ProblemeSpxDuSolveur );

/* On renseigne le Simplexe pour qu'il sache qui l'appelle */
Spx = NULL;
if ( Pne->ProblemeSpxDuSolveur != NULL ) {
  Spx = (PROBLEME_SPX *) Pne->ProblemeSpxDuSolveur;
  Spx->ProblemePneDeSpx = (void *) Pne;
}

if ( Pne->DureeDuPremierSimplexe <= 0. ) {
  time( &HeureDeCalendrierCourant );
  TempsEcoule = difftime( HeureDeCalendrierCourant , HeureDeCalendrierDebut );
  Pne->DureeDuPremierSimplexe = TempsEcoule;
}

ExistenceDUneSolution = Probleme.ExistenceDUneSolution;

if ( ExistenceDUneSolution == OUI_PNE ) {

  *Faisabilite = OUI_PNE;
  *NbVarDeBaseComplementaires = Probleme.NbVarDeBaseComplementaires;

  Bb = (BB *) Pne->ProblemeBbDuSolveur;
	if ( Bb != NULL ) {
    Bb->NombreDeSimplexes++;
    if ( Spx != NULL ) Bb->SommeDuNombreDIterations += Spx->Iteration;    
	}

	/* Init de l'indicateur ContrainteSaturee */
  /* Si la variable d'ecart est basique on considere que la contrainte n'est pas saturee */
	ContrainteSaturee = Pne->ContrainteSaturee;	
	/* Decompte du nombre de variables duales nulles */
	Nb = 0;
  VariablesDualesDesContraintesTravEtDesCoupes = Pne->VariablesDualesDesContraintesTravEtDesCoupes;
  for ( i = 0 ; i < Pne->NombreDeContraintesTrav ; i++ ) {
	  ContrainteSaturee[i] = OUI_PNE;
    if ( VariablesDualesDesContraintesTravEtDesCoupes[i] == 0.0 ) Nb++;
	}
	for ( i = 0 ; i < *NbVarDeBaseComplementaires ; i++ ) ContrainteSaturee[ComplementDeLaBase[i]] = NON_PNE;
	
	S = (float) Nb / (float) Pne->NombreDeContraintesTrav;
	if ( S > SEUIL_POUR_PRIORITE_DANS_SPX_AUX_VARIABLES_SORTANTES_ENTIERES ) {
	  Pne->PrioriteDansSpxAuxVariablesSortantesEntieres = OUI_PNE;
	}
	else Pne->PrioriteDansSpxAuxVariablesSortantesEntieres = NON_PNE;
}
else if ( ExistenceDUneSolution == NON_PNE ) { 
  *Faisabilite = NON_PNE;  
}
else if ( ExistenceDUneSolution == SPX_MATRICE_DE_BASE_SINGULIERE ) {	
  *Faisabilite = NON_PNE;
  /* Si la matrice de base est singuliere et qu'on avait mis des coupes, c'est que les 
     coupes sont merdiques. On pourrait virer seulement les coupes ajoutee a ce 
     probleme particulier. Par precaution, on prefere virer toutes les coupes du pool car 
     experimentalement on constate que bien d'autres coupes peuvent etre merdiques */ 
  if ( Pne->Coupes.NombreDeContraintes > 0 ) {
    /*printf("-> Nettoyage des coupes pour cause d'instabilites numeriques \n");*/
    Bb = (BB *) Pne->ProblemeBbDuSolveur;
    BB_LeverLeFlagPourEnleverToutesLesCoupes( Bb );     
  }
}
else if ( ExistenceDUneSolution == SPX_ERREUR_INTERNE ) {
  Pne->AnomalieDetectee = OUI_PNE;
  longjmp( Pne->Env , Pne->AnomalieDetectee ); /* rq: le 2eme argument ne sera pas utilise */
}
else {
  printf("Bug dans PNE_SolvePbRlxSpxDual, ExistenceDUneSolution mal renseigne\n"); 
  Pne->AnomalieDetectee = OUI_PNE;
  longjmp( Pne->Env , Pne->AnomalieDetectee ); /* rq: le 2eme argument ne sera pas utilise */
}


/* On essaie de fixer des variables en analysant les contraintes d'inegalite */
/*
char SigneCoeff; char SensCnt; double CoutVar; double Ai; int ic; int Var; int Cnt;
for ( i = 0 ; i < Pne->NombreDeVariablesNonFixes ; i++ ) {
  Var = Pne->NumeroDesVariablesNonFixes[i];
	if ( Pne->TypeDeVariableTrav[Var] != ENTIER ) continue; 
	CoutVar = Pne->LTrav[Var];
	SigneCoeff = '?';
  ic = Pne->CdebTrav[Var];
  while ( ic >= 0 ) {
    Ai = Pne->ATrav[ic];
    if ( Ai == 0.0 ) goto NextIc;		
    Cnt = Pne->NumContrainteTrav[ic];
    if ( Pne->ContrainteActivable[Cnt] == NON_PNE ) goto NextIc;		
	  SensCnt = Pne->SensContrainteTrav[Cnt];
    if ( SigneCoeff != 'X' ) {				 
      if ( SensCnt == '=' ) SigneCoeff = 'X';
			else {
		    if ( Ai > 0.0 ) {
          if ( SigneCoeff == '?' ) SigneCoeff = '+';
			    else if ( SigneCoeff == '-' ) SigneCoeff = 'X';				
			  }		  
		    else {
          if ( SigneCoeff == '?' ) SigneCoeff = '-';
			    else if ( SigneCoeff == '+' ) SigneCoeff = 'X';
			  }
		  }		
    }
		else break;		
	  NextIc:
    ic = Pne->CsuiTrav[ic];
	}	
	if ( SigneCoeff == 'X' ) continue;	
	if ( SigneCoeff == '+' ) {
	  if ( CoutVar >= 0.0 ) Pne->UTrav[Var] = Pne->UminTrav[Var];		  
	}	
	else if ( SigneCoeff == '-' ) {
	  if ( CoutVar <= 0.0 ) Pne->UTrav[Var] = Pne->UmaxTrav[Var];		
	}
	else if ( SigneCoeff == '?' ) {
	  if ( CoutVar >= 0.0 ) Pne->UTrav[Var] = Pne->UminTrav[Var];		  
		else Pne->UTrav[Var] = Pne->UmaxTrav[Var];		
  }				
}
*/
/* Fin du test */

return;
}

















