/***********************************************************************

   FONCTION: Recherche de contraintes de sac a dos. On construit la contrainte
	           de depart en eliminant au maximum les variables continues
						 grace a des combinaisons de contraintes.

   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# include "bb_define.h"
# include "bb_fonctions.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define EPSILON_COEFF 1.e-9
# define EPSILON 1.e-7
# define RAPPORT_MAX 1.e+5 
# define RAPPORT_MIN 1.e-5 

# define SEUIL_1   0.999 
# define SEUIL_0   0.001 

# define NOMBRE_MAX_DE_VARIABLES_CONTINUES 1000
# define MAX_CHOIX 3
# define SEUIL_DECREMENT_MAX_CHOIX 100 /* Si on parcours plus de contraintes que ca alors MAX_CHOIX est decremente */

# define MAX_VAR_ENTIERES  1
# define MIN_VAR_CONTINUES 2

void PNE_KnapsackSupprimerLesVariablesContinues( PROBLEME_PNE * , char * , int * , int * , char * , double * , int * , int * , double * , int * , int * ,
																								 double * , int * , int * , char , int * , int * , int * );
																								 
void PNE_KnapsackCombinaisonDeContraintesAppliquerLeConflictGraph( PROBLEME_PNE * , int , double );

void PNE_AllocContraintesPourLaRechercheDesCoupesDeSacADos( PROBLEME_PNE * );

void PNE_ReAllocNombreDeContraintesPourLaRechercheDesCoupesDeSacADos( PROBLEME_PNE * );

void PNE_ReAllocTailleContraintesPourLaRechercheDesCoupesDeSacADos( PROBLEME_PNE * );

void PNE_CreerLesContraintesPourLaRechercheDesCoupesDeSacADos( PROBLEME_PNE * );

void PNE_DeterminerLaColineariteDesContraintesPourLaRechercheDesCoupesDeSacADos( PROBLEME_PNE * , char * , double * , double, int , char * , double * , int * );

void PNE_ChainageParColonnePourLaRechercheDesCoupesDeSacADos( int , int * , int * , int * , int * , int , int , int );

void PNE_ChainageParColonneDansLOrdreCroissantDuNombreDeTermes( PROBLEME_PNE * , int * , int * , int * , int * , char * );

void PNE_ContraintesPourLaRechercheDesCoupesDeSacADosCalculPGCD(int , double * , int * );

void PNE_KnapsackRechercheContraintePourAgregation( PROBLEME_PNE * , char * , int * , int * , char * , int , int , int * , double * , double , int * , int * , int * , char  );

void PNE_MajBlocContraintesPourLaRechercheDesCoupesDeSacADos( PROBLEME_PNE * , int * , int , int * , double  * , double );

void PNE_TransfertDeLaContrainteDansLeVecteurDeSynthese( PROBLEME_PNE * , char * , int , int * , double * , int , int * , double * , 
                                                         int * , double * , double * , int * , char * );
																												 
void PNE_RoundingEtPGCDDeLaContraintePourLaRechercheDesCoupesDeSacADos( int * ,  char * , int * , double * , double * );

void PNE_TestContraintePourKToujoursSatisfaite( int , double * , int * , double , double * , double * , char * );

void PNE_TransfererLaContrainteNativeDansLesVecteursDeTravail( int * , int * , double * , char * , double * , double * , int * , int * , int , double , int * ,
                                                               int * , double * , double * , int * , int * , int * , double * , double * , double * );


void PNE_CreerLesContraintesPourLaRechercheDesCoupesDeSacADosPourUnCritereDonne( PROBLEME_PNE * , int * , char , char * , double * , double * , double * , int * ,
																																								 int * , int * , int * , int * , int * , int , int , int , int * , int * , 
																																								 char * , double * , double * , double , int * , char * );																														 

void PNE_CreerLesContraintesPourLaRechercheDesCoupesDeSacADosPourPlusieursCritere( PROBLEME_PNE * , int * , char *  , double * , double * , char * , int * ,
																																									 int * , int * , int * , int * , int * , int * , int * );				 

void PNE_BlocPourKRechercheContraintePourAgregation( PROBLEME_PNE * , char * , char * , int , int , int * , double * , double , int * , int * , int * );			

void PNE_BlocPourKSupprimerDesVariablesBinaires( PROBLEME_PNE * , char * , char * , double * , int * , int * , double * , int * , int * , int * , int * , int * );

void PNE_AvecLeBlocKCreerUneContraintePourLaRechercheDesCoupesDeSacADos( PROBLEME_PNE * , int * , char * , double * , double * , double * , int * , int * , int * , int * ,                                                                   
                                                                         int , int , int , int * , int * , char * , double * , double * , double , int * );

void PNE_AvecLeBlocKCreerLesContraintesPourLaRechercheDesCoupesDeSacADos( PROBLEME_PNE * , int * , char * , double * , double * , char * ,  int * , int * ,
                                                                          int * , int * , int * , int * );

void PNE_ChainageParColonneDuBlocKPourLaRechercheDesCoupesDeSacADos( int , int * , int * , int * , int * , int , int , int );

void PNE_ParColonneDuBlocPourKDansLOrdreCroissantDuNombreDeTermes( PROBLEME_PNE * , int * , int * , int * );

void PNE_ContraintesNativesPourLaRechercheDesCoupesDeSacADos( PROBLEME_PNE * , int * , double * , double * , double * , int * , int , int , int * , int * ,
                                                              char * , double * , double * , double );					


/*----------------------------------------------------------------------------*/

void PNE_KnapsackRechercheContraintePourAgregation( PROBLEME_PNE * Pne, char * ContrainteUtilisable,
                                                    int * NombreDeVariablesContinuesDeLaContrainte, int * NombreDeVariablesBinairesDesContraintes, char * T,
																										int NbVarContinuesMax, int VariableASupprimer,
																										int * ContrainteAAgreger, double * CoeffContrainteAAgreger,
																										double CoeffDeLaVariableASupprimer, int * Cdeb, int * Csui, int * NumContrainte,
																										char Critere )																										
{
char * SensContrainte; int ic; int il; int ilMax; int * Mdeb; int MaxChoix; int Count; int MaxNbVarEntieres;
int * NbTerm; int * Nuvar; int Cnt; int Var; double * A; int * TypeDeVariable; int * TypeDeBorne; int NbVarEntieres;
double * Xmin; double * Xmax; int NbSelect; int MinNbVarContinues; int NbVarContinues; double Rapport; 
int Nb;

*ContrainteAAgreger = -1;
*CoeffContrainteAAgreger = 1;

/*if ( Pne->CNbTermTrav[VariableASupprimer] > 10000 )  return;*/ /* En attendant mieux ... */

SensContrainte = Pne->SensContrainteTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
Nuvar = Pne->NuvarTrav;
A = Pne->ATrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
Xmin = Pne->UminTravSv;
Xmax = Pne->UmaxTravSv;
# if BORNES_INF_AUXILIAIRES == OUI_PNE
  Xmin = Pne->XminAuxiliaire;
# endif

NbSelect = 0;
MinNbVarContinues = 1000000000;
MaxNbVarEntieres = -1;

MaxChoix = MAX_CHOIX;
Count = 0;

Nb = 0;
ic = Cdeb[VariableASupprimer];
while ( ic >= 0 ) {  
  if ( A[ic] == 0 ) goto NextIc;
  Cnt = NumContrainte[ic];	
  if ( NombreDeVariablesContinuesDeLaContrainte[Cnt] > NbVarContinuesMax ) return;

  if ( Critere == MAX_VAR_ENTIERES ) {
	  if ( NombreDeVariablesBinairesDesContraintes[Cnt] < MaxNbVarEntieres ) goto NextIc;
	}
	
	if ( ContrainteUtilisable[Cnt] == NON_PNE ) goto NextIc;
	Rapport = CoeffDeLaVariableASupprimer / A[ic];	
	if ( fabs( Rapport ) < RAPPORT_MIN || fabs( Rapport ) > RAPPORT_MAX  ) goto NextIc;
	if ( SensContrainte[Cnt] == '<' ) {
	  if ( Rapport > 0 ) goto NextIc; /* Car il faudrait utiliser un coefficent negatif pour l'agregation de la contrainte */			
	}	
		
	if ( MaxChoix > 0 ) {
	  Count++;
	  if ( Count > SEUIL_DECREMENT_MAX_CHOIX ) {
      Count = 0;
		  MaxChoix--;
	  }
	}

Nb++;
if ( Nb > 1000 ) break; /* En attendant mieux */
	
	/* On regarde si le support des variables continues de Cnt est contenu dans le support des
		 variables continues de la contrainte pour la knapscack */
	il = Mdeb[Cnt];
	ilMax = il + NbTerm[Cnt];	
	NbVarContinues = 0;	
  NbVarEntieres = 0;	
  while ( il < ilMax ) {
    Var = Nuvar[il];
		if ( A[il] == 0 ) goto NextIl;
		if ( TypeDeBorne[Var] == VARIABLE_FIXE ) goto NextIl;
		if ( Xmin[Var] == Xmax[Var] ) goto NextIl;
		if ( TypeDeVariable[Var] == ENTIER ) {
		  if ( T[Var] == 0 ) {
			  /* Car ni fixee ni Xmin = Xmax */
			  NbVarEntieres++;
			}		
			goto NextIl;
		}
		if ( T[Var] != 1 ) goto NextIc;
		NbVarContinues++;
    NextIl:
		il++;	
	}	
	/* Si on arrive ici alors la contrainte peut etre agregee */
  if ( Critere == MAX_VAR_ENTIERES ) {
	  if ( NbVarEntieres > MaxNbVarEntieres ) {
      *ContrainteAAgreger = Cnt;		
      *CoeffContrainteAAgreger = A[ic];		
		  MaxNbVarEntieres = NbVarEntieres;
		  NbSelect++;
		  if ( NbSelect >= MaxChoix ) break;
	  }
	}
	else if ( Critere == MIN_VAR_CONTINUES ) {	
	  if ( NbVarContinues < MinNbVarContinues ) {
      *ContrainteAAgreger = Cnt;		
      *CoeffContrainteAAgreger = A[ic];		
		  MinNbVarContinues = NbVarContinues;
		  NbSelect++;
	 	  if ( NbSelect >= MaxChoix ) break;
	  }
	}
  else {
    printf("KnapsackRechercheContraintePourAgregation Critere mal initialise -> %d\n",Critere);
		exit(0);
	}
	
	NextIc:
  ic = Csui[ic];
}

return;
}

/*----------------------------------------------------------------------------*/

void PNE_KnapsackSupprimerLesVariablesContinues( PROBLEME_PNE * Pne, char * ContrainteUtilisable,
                                                 int * NombreDeVariablesContinuesDesContraintes, int * NombreDeVariablesBinairesDesContraintes, char * T,
																								 double * V, int * NombreDeVariablesContinues, int * NumeroDeLaVariableContinue,
																								 double * I, int * NombreDeVariablesBinaires, int * NumeroDeLaVariableBinaire,
																								 double * SecondMembre, int * NombreDeContraintesAgregees,
																								 int * NumeroDesContraintesAgregees, char Critere, int * Cdeb, int * Csui, int * NumContrainte )																										
{
int i; int Var; double CoeffDeVar; int ContrainteAAgreger; double CoeffContrainteAAgreger;
double Alpha; int NombreCourantDeVariablesContinues; int il; int ilMax; int * Mdeb; int * NbTerm; int * Nuvar; 
double * A; int * TypeDeVariable; int * TypeDeBorne; double * Xmin; double * Xmax; double * X; double S; double * B;
int NombreCourantDeVariablesBinaires; int VariableASupprimer;

NombreCourantDeVariablesContinues = *NombreDeVariablesContinues;
NombreCourantDeVariablesBinaires = *NombreDeVariablesBinaires;

Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
Nuvar = Pne->NuvarTrav;
A = Pne->ATrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
X = Pne->UTrav;
Xmin = Pne->UminTravSv;
Xmax = Pne->UmaxTravSv;
# if BORNES_INF_AUXILIAIRES == OUI_PNE
  Xmin = Pne->XminAuxiliaire;
# endif
B = Pne->BTrav;

for ( i = 0 ; i < NombreCourantDeVariablesContinues ; i++ ) {
	
  VariableASupprimer = NumeroDeLaVariableContinue[i];

	if ( fabs( V[VariableASupprimer] ) < EPSILON_COEFF ) {
	  /* Car une annulation a pu se produire */
		T[VariableASupprimer] = 0;
		/* On inverse avec la derniere valeur */			
    NumeroDeLaVariableContinue[i] = NumeroDeLaVariableContinue[NombreCourantDeVariablesContinues - 1];		
    NombreCourantDeVariablesContinues--;			
    i--;
		continue;
	}
	
	CoeffDeVar = V[VariableASupprimer];	
  PNE_KnapsackRechercheContraintePourAgregation( Pne, ContrainteUtilisable, NombreDeVariablesContinuesDesContraintes, NombreDeVariablesBinairesDesContraintes, T,
																								 NombreCourantDeVariablesContinues, VariableASupprimer, &ContrainteAAgreger, &CoeffContrainteAAgreger, CoeffDeVar,
																								 Cdeb, Csui, NumContrainte, Critere );  	
	if ( ContrainteAAgreger < 0 ) continue;	

	Alpha = -CoeffDeVar / CoeffContrainteAAgreger;
  
	ContrainteUtilisable[ContrainteAAgreger] = NON_PNE;
  NumeroDesContraintesAgregees[*NombreDeContraintesAgregees] = ContrainteAAgreger;
  *NombreDeContraintesAgregees = *NombreDeContraintesAgregees + 1;
	S = B[ContrainteAAgreger];
  il = Mdeb[ContrainteAAgreger];
  ilMax = il + NbTerm[ContrainteAAgreger];			
  while ( il < ilMax ) {
		if ( A[il] == 0 ) goto NextIl;
    Var = Nuvar[il];
	  if ( TypeDeBorne[Var] == VARIABLE_FIXE ) { S -= A[il] * X[Var]; goto NextIl; }
	  if ( Xmin[Var] == Xmax[Var] ) { S -= A[il] * Xmin[Var]; goto NextIl; }		
	  if ( TypeDeVariable[Var] != ENTIER ) {
	    V[Var] += Alpha * A[il];	
		}
    else {
		  /* Var est entier */		
	    if ( T[Var] == 1 ) I[Var] += Alpha * A[il]; /* Les cas d'annulation seront traites plus tard */
      else {
		    /* On ajoute la variable */
			  T[Var] = 1;
			  I[Var] = Alpha * A[il];
        NumeroDeLaVariableBinaire[NombreCourantDeVariablesBinaires] = Var;
			  NombreCourantDeVariablesBinaires++;
		  }	
    }	
    NextIl:
	  il++;	    
	}
  *SecondMembre = *SecondMembre + ( Alpha * S );
	/* On supprime la variable continue */
	T[VariableASupprimer] = 0;
	I[VariableASupprimer] = 0;
	/* On inverse avec la derniere valeur */			
  NumeroDeLaVariableContinue[i] = NumeroDeLaVariableContinue[NombreCourantDeVariablesContinues - 1];		
  NombreCourantDeVariablesContinues--;			
  i--;	
}
*NombreDeVariablesContinues = NombreCourantDeVariablesContinues;
*NombreDeVariablesBinaires = NombreCourantDeVariablesBinaires;
return;
}

/*----------------------------------------------------------------------------*/

void PNE_BlocPourKRechercheContraintePourAgregation( PROBLEME_PNE * Pne, char * ContrainteUtilisable, char * T, int NbVarBinairesMax, 
                                                     int VariableASupprimer, int * ContrainteAAgreger, double * CoeffContrainteAAgreger,
																										 double CoeffDeLaVariableASupprimer, int * Cdeb, int * Csui, int * NumContrainte )																										
{
int ic; int il; int ilMax; int * Mdeb; int * NbTerm; int * Nuvar; int Cnt; int Var; double * A; int * TypeDeVariable; 
int * TypeDeBorne; double * Xmin; double * Xmax; double Rapport; int Nb;

*ContrainteAAgreger = -1;
*CoeffContrainteAAgreger = 1;

Mdeb = Pne->MdebPourK;
NbTerm = Pne->NbTermPourK;
Nuvar = Pne->NuvarPourK;
A = Pne->APourK;

TypeDeVariable = Pne->TypeDeVariableTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
Xmin = Pne->UminTravSv;
Xmax = Pne->UmaxTravSv;
# if BORNES_INF_AUXILIAIRES == OUI_PNE
  Xmin = Pne->XminAuxiliaire;
# endif

Nb = 0;
ic = Cdeb[VariableASupprimer];
while ( ic >= 0 ) {  
Nb++;
  if ( A[ic] == 0 ) goto NextIc;
Nb++;
if ( Nb > 1000 ) break;
  Cnt = NumContrainte[ic];	
  if ( NbTerm[Cnt] > NbVarBinairesMax ) break;
	if ( ContrainteUtilisable[Cnt] == NON_PNE ) goto NextIc;
	Rapport = CoeffDeLaVariableASupprimer / A[ic];	
	if ( fabs( Rapport ) < RAPPORT_MIN || fabs( Rapport ) > RAPPORT_MAX  ) goto NextIc;
	if ( Rapport > 0 ) goto NextIc; /* Car il faudrait utiliser un coefficent negatif pour l'agregation de la contrainte */						
	/* On regarde si le support des variables binaires de Cnt est contenu dans le support des
		 variables binaires de la contrainte pour la knapscack */
	il = Mdeb[Cnt];
	ilMax = il + NbTerm[Cnt];	
  while ( il < ilMax ) {
 		if ( A[il] != 0 ) {
      Var = Nuvar[il];
		  if ( TypeDeBorne[Var] == VARIABLE_FIXE ) goto NextIl;
		  if ( Xmin[Var] == Xmax[Var] ) goto NextIl;
		  if ( T[Var] == 0 ) goto NextIc;		
    }	
    NextIl:		
		il++;	
	}	
	/* Si on arrive ici alors la contrainte peut etre agregee */
  *ContrainteAAgreger = Cnt;		
  *CoeffContrainteAAgreger = A[ic];		
  break;	  	
	NextIc:
  ic = Csui[ic];
}

return;
}

/*----------------------------------------------------------------------------*/

void PNE_BlocPourKSupprimerDesVariablesBinaires( PROBLEME_PNE * Pne, char * ContrainteUtilisable, char * T,
																								 double * I, int * NombreDeVariablesBinaires, int * NumeroDeLaVariableBinaire,
																								 double * SecondMembre, int * NombreDeContraintesAgregees,
																								 int * NumeroDesContraintesAgregees, int * Cdeb, int * Csui, int * NumContrainte )																										
{
int i; int Var; double CoeffDeVar; int ContrainteAAgreger; double CoeffContrainteAAgreger;
double Alpha; int il; int ilMax; int * Mdeb; int * NbTerm; int * Nuvar; 
double * A; int * TypeDeVariable; int * TypeDeBorne; double * Xmin; double * Xmax; double * X; double S; double * B;
int NombreCourantDeVariablesBinaires; int VariableASupprimer;

NombreCourantDeVariablesBinaires = *NombreDeVariablesBinaires;

Mdeb = Pne->MdebPourK;
NbTerm = Pne->NbTermPourK;
B = Pne->BPourK;
Nuvar = Pne->NuvarPourK;
A = Pne->APourK;

TypeDeVariable = Pne->TypeDeVariableTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
X = Pne->UTrav;
Xmin = Pne->UminTravSv;
Xmax = Pne->UmaxTravSv;
# if BORNES_INF_AUXILIAIRES == OUI_PNE
  Xmin = Pne->XminAuxiliaire;
# endif

for ( i = 0 ; i < NombreCourantDeVariablesBinaires ; i++ ) {
	
  VariableASupprimer = NumeroDeLaVariableBinaire[i];

	if ( fabs( I[VariableASupprimer] ) < EPSILON_COEFF ) {
	  /* Car une annulation a pu se produire */
		T[VariableASupprimer] = 0;
		/* On inverse avec la derniere valeur */			
    NumeroDeLaVariableBinaire[i] = NumeroDeLaVariableBinaire[NombreCourantDeVariablesBinaires - 1];		
    NombreCourantDeVariablesBinaires--;			
    i--;
		continue;
	}
	
	CoeffDeVar = I[VariableASupprimer];	

  PNE_BlocPourKRechercheContraintePourAgregation( Pne, ContrainteUtilisable, T, NombreCourantDeVariablesBinaires, 
                                                  VariableASupprimer, &ContrainteAAgreger, &CoeffContrainteAAgreger,
																									CoeffDeVar, Cdeb, Csui, NumContrainte );				
	if ( ContrainteAAgreger < 0 ) continue;	

	Alpha = -CoeffDeVar / CoeffContrainteAAgreger;
  /* Par securite */
	if ( fabs( Alpha ) < RAPPORT_MIN || fabs( Alpha ) > RAPPORT_MAX  ) continue;
	  
	ContrainteUtilisable[ContrainteAAgreger] = NON_PNE;
  NumeroDesContraintesAgregees[*NombreDeContraintesAgregees] = ContrainteAAgreger;
  *NombreDeContraintesAgregees = *NombreDeContraintesAgregees + 1;
	S = B[ContrainteAAgreger];
  il = Mdeb[ContrainteAAgreger];
  ilMax = il + NbTerm[ContrainteAAgreger];			
  while ( il < ilMax ) {
    Var = Nuvar[il];
		if ( A[il] == 0 ) goto NextIl;
		/* Var est entier */		
	  if ( TypeDeBorne[Var] == VARIABLE_FIXE ) { S -= A[il] * X[Var]; goto NextIl; }
	  if ( Xmin[Var] == Xmax[Var] ) { S -= A[il] * Xmin[Var]; goto NextIl; }		
	  if ( T[Var] == 1 ) I[Var] += Alpha * A[il]; /* Les cas d'annulation seront traites plus tard */
    else {
		  /* On ajoute la variable */
      printf("Bug PNE_BlocPourKSupprimerDesVariablesBinaires il ne faut pas augmenter le nombre de variables binaires\n");
      exit(0);
		}		
    NextIl:
	  il++;	    
	}
  *SecondMembre = *SecondMembre + ( Alpha * S );
	/* On supprime la variable continue */
	T[VariableASupprimer] = 0;
	/* On inverse avec la derniere valeur */			
  NumeroDeLaVariableBinaire[i] = NumeroDeLaVariableBinaire[NombreCourantDeVariablesBinaires - 1];		
  NombreCourantDeVariablesBinaires--;			
  i--;	
}
*NombreDeVariablesBinaires = NombreCourantDeVariablesBinaires;
return;
}

/*----------------------------------------------------------------------------*/

void PNE_ReAllocNombreDeContraintesPourLaRechercheDesCoupesDeSacADos( PROBLEME_PNE * Pne )
{
int NbContraintesAlloueesPourK;
NbContraintesAlloueesPourK = Pne->NbContraintesAlloueesPourK;
NbContraintesAlloueesPourK += 1000;
Pne->MdebPourK = (int *) realloc( Pne->MdebPourK, NbContraintesAlloueesPourK  * sizeof( int ) );
Pne->NbTermPourK = (int *) realloc( Pne->NbTermPourK , NbContraintesAlloueesPourK  * sizeof( int ) );
Pne->BPourK = (double *) realloc( Pne->BPourK, NbContraintesAlloueesPourK  * sizeof( double ) );
if ( Pne->MdebPourK == NULL || Pne->NbTermPourK == NULL || Pne->BPourK == NULL ) {
  free( Pne->MdebPourK );
  free( Pne->NbTermPourK );
  free( Pne->BPourK );
  free( Pne->APourK );
  free( Pne->NuvarPourK );
  Pne->MdebPourK = NULL;
  Pne->NbTermPourK = NULL;
  Pne->BPourK = NULL;
  Pne->APourK = NULL;
  Pne->NuvarPourK = NULL;
  Pne->NbContraintesAlloueesPourK = 0;
  Pne->TailleAlloueesPourK = 0;
	Pne->NbContraintesPourK = 0;
  return;
}
Pne->NbContraintesAlloueesPourK = NbContraintesAlloueesPourK;
return;
}

/*----------------------------------------------------------------------------*/

void PNE_ReAllocTailleContraintesPourLaRechercheDesCoupesDeSacADos( PROBLEME_PNE * Pne )
{
int TailleAlloueesPourK;
TailleAlloueesPourK = Pne->TailleAlloueesPourK;
TailleAlloueesPourK += 10000;
Pne->APourK = (double *) realloc( Pne->APourK, TailleAlloueesPourK * sizeof( double ) );
Pne->NuvarPourK = (int *) realloc( Pne->NuvarPourK, TailleAlloueesPourK * sizeof( int ) );
if ( Pne->APourK == NULL || Pne->NuvarPourK == NULL ) {
  free( Pne->MdebPourK );
  free( Pne->NbTermPourK );
  free( Pne->BPourK );
  free( Pne->APourK );
  free( Pne->NuvarPourK );
  Pne->MdebPourK = NULL;
  Pne->NbTermPourK = NULL;
  Pne->BPourK = NULL;
  Pne->APourK = NULL;
  Pne->NuvarPourK = NULL;
  Pne->NbContraintesAlloueesPourK = 0;
  Pne->TailleAlloueesPourK = 0;
	Pne->NbContraintesPourK = 0;
	return;
}
Pne->TailleAlloueesPourK = TailleAlloueesPourK;
return;
}

/*----------------------------------------------------------------------------*/

void PNE_AllocContraintesPourLaRechercheDesCoupesDeSacADos( PROBLEME_PNE * Pne )
{
int NbContraintesAlloueesPourK; int TailleAlloueesPourK;

NbContraintesAlloueesPourK = 1000;
TailleAlloueesPourK = 10000;

free( Pne->MdebPourK );
free( Pne->NbTermPourK );
free( Pne->BPourK );
free( Pne->APourK );
free( Pne->NuvarPourK  );
Pne->MdebPourK = NULL;
Pne->NbTermPourK = NULL;
Pne->BPourK = NULL;
Pne->APourK = NULL;
Pne->NuvarPourK = NULL;
Pne->NbContraintesAlloueesPourK = 0;
Pne->TailleAlloueesPourK = 0;
Pne->NbContraintesPourK = 0;

Pne->MdebPourK = (int *) malloc( NbContraintesAlloueesPourK  * sizeof( int ) );
Pne->NbTermPourK = (int *) malloc( NbContraintesAlloueesPourK  * sizeof( int ) );
Pne->BPourK = (double *) malloc( NbContraintesAlloueesPourK  * sizeof( double ) );
Pne->APourK = (double *) malloc( TailleAlloueesPourK * sizeof( double ) );
Pne->NuvarPourK = (int *) malloc( TailleAlloueesPourK * sizeof( int ) );
if ( Pne->MdebPourK == NULL || Pne->NbTermPourK == NULL || Pne->BPourK == NULL ||
     Pne->APourK == NULL || Pne->NuvarPourK == NULL ) {
  free( Pne->MdebPourK );
  free( Pne->NbTermPourK );
  free( Pne->BPourK );
  free( Pne->APourK );
  free( Pne->NuvarPourK );
  Pne->MdebPourK = NULL;
  Pne->NbTermPourK = NULL;
  Pne->BPourK = NULL;
  Pne->APourK = NULL;
  Pne->NuvarPourK = NULL;
  Pne->NbContraintesAlloueesPourK = 0;
  Pne->TailleAlloueesPourK = 0;
	Pne->NbContraintesPourK = 0;
	return;
}
Pne->NbContraintesAlloueesPourK = NbContraintesAlloueesPourK;
Pne->TailleAlloueesPourK = TailleAlloueesPourK;
Pne->NbContraintesPourK = 0;
return;
}

/*----------------------------------------------------------------------------*/

void PNE_DeterminerLaColineariteDesContraintesPourLaRechercheDesCoupesDeSacADos( PROBLEME_PNE * Pne, char * T, double * V, double SecondMembre,
                                                                                 int NombreDeTermes, char * Colineaire, double * Coeff, int * Variable )
{
int * MdebPourK; int * NbTermPourK; double * BPourK; double * APourK; int * NuvarPourK;
int Cnt; int il; int ilMax; int Var; double Rapport; char PremierRapportTrouve; int i;

*Colineaire = NON_PNE;

for ( i = 0 ; i < NombreDeTermes ; i++ ) { T[Variable[i]] = 1; V[Variable[i]] = Coeff[i]; }		

MdebPourK = Pne->MdebPourK;
NbTermPourK = Pne->NbTermPourK;
BPourK = Pne->BPourK;
APourK = Pne->APourK;
NuvarPourK = Pne->NuvarPourK;
for ( Cnt = 0 ; Cnt < Pne->NbContraintesPourK ; Cnt++ ) {
  if ( NbTermPourK[Cnt] != NombreDeTermes ) continue;
  il = MdebPourK[Cnt];
	ilMax = il + NbTermPourK[Cnt];
	*Colineaire = OUI_PNE;
	Rapport = 1;
	PremierRapportTrouve = NON_PNE;
	while ( il < ilMax ) {
	  Var = NuvarPourK[il];
    if ( T[Var] == 0 ) {
		  *Colineaire = NON_PNE;
			break;
		}
		else {
      if ( PremierRapportTrouve == NON_PNE ){
		    Rapport = APourK[il] / V[Var];
				if ( Rapport < 0 ) {
		      *Colineaire = NON_PNE;
			    break;
				}
				PremierRapportTrouve = OUI_PNE;
			}
			if ( fabs( APourK[il] - ( Rapport * V[Var] ) ) > EPSILON ) {
		    *Colineaire = NON_PNE;
			  break;
			}
		}				
    il++;
	}
  /* Potentiellement colineaire, il reste a analyser le second membre */
	if ( fabs( BPourK[Cnt] - ( Rapport * SecondMembre ) ) > EPSILON ) {
    *Colineaire = NON_PNE;
	}	
	/* La contrainte est colineaire */
  if ( *Colineaire == OUI_PNE ) {
		goto FinColinearite;
	}
}
FinColinearite:
for ( i = 0 ; i < NombreDeTermes ; i++ ) { T[Variable[i]] = 0; V[Variable[i]] = 0; }		

return;
}

/*----------------------------------------------------------------------------*/

void PNE_ContraintesPourLaRechercheDesCoupesDeSacADosCalculPGCD(int NombreDeTermes, double * Coeff, int * Pgcd )
{
int i; int a; int b; int c;

/* Cas ou NombreDeTerme == 1 (correction du 20/12/2017) */
if ( NombreDeTermes == 1) {
	*Pgcd = (int) Coeff[0];
	return;
}

/* Calcul du GCD des coeffs */
if ( Coeff[0] > Coeff[1] ) { a = (int) Coeff[0]; b = (int) Coeff[1]; }
else                       { a = (int) Coeff[1]; b = (int) Coeff[0]; }

i = 1;
GCD:
if ( a == 0 && b == 0 ) c = 0;
else if ( b == 0 ) c = a;
else if ( a == 0 ) c = b;
else {
  c = a % b;
  while ( c != 0 ) { 
    a = b;
    b = c;
    c = a % b;
  }
  c = b;
}
i++;
if ( i < NombreDeTermes ) {
  if ( (int) Coeff[i] > c ) { a = (int) Coeff[i]; b = c; }
  else                      { a = c; b = (int) Coeff[i]; }
	goto GCD;
}

*Pgcd = c;
return;
}

/*----------------------------------------------------------------------------*/

void PNE_RoundingEtPGCDDeLaContraintePourLaRechercheDesCoupesDeSacADos( int * NbT, char * T, int * Variable, double  * Coeff, double * Sec )
{
char ToutEstAZero; int Pgcd; int i; double SecondMembre; int NombreDeTermes;

ToutEstAZero = OUI_PNE;
SecondMembre = *Sec;
NombreDeTermes = *NbT;
Pgcd = 0;

for ( i = 0 ; i < NombreDeTermes ; i++ ) {
  if ( Coeff[i] > 0 ) Coeff[i] = floor( Coeff[i] );
	else {
		/* Changement de variable x = 1 - x */
		SecondMembre -= Coeff[i];
		Coeff[i] *= -1;
		Coeff[i] = floor( Coeff[i] );
		T[Variable[i]] = 1;
	}
	if ( Coeff[i] != 0 ) {
    ToutEstAZero = NON_PNE;
  	if ( fabs( Coeff[i] ) == 1 ) Pgcd = 1;
  }
}
SecondMembre = floor( SecondMembre );

if ( ToutEstAZero == OUI_PNE ) goto FinGCD;

if ( Pgcd != 1 ) {
  PNE_ContraintesPourLaRechercheDesCoupesDeSacADosCalculPGCD( NombreDeTermes, Coeff, &Pgcd );
  if ( Pgcd == 0 ) goto FinGCD;
}

for ( i = 0 ; i < NombreDeTermes ; i++ ) { 
  Coeff[i] /= (double) Pgcd;	
}
SecondMembre = floor( SecondMembre / (double) Pgcd );
	
/*****************/
/*
{
  int i1;
  for ( i = 0 ; i < NombreDeTermes ; i++ ) { 
    if ( Coeff[i] > SecondMembre ) {		  
      printf("Variable d'index %d : %d doit etre mise a 0 ou 1  NombreDeTermes %d  Coeff %e Pgcd %d\n",i,Variable[i],NombreDeTermes,Coeff[i],Pgcd);
      for ( i1 = 0 ; i1 < NombreDeTermes ; i1++ ) { 
        printf(" %e ",Coeff[i1]);
      }
      printf("\n < %e\n",SecondMembre);
    }
  }	
}
*/
/*****************/

FinGCD:
/* On refait le changement de variable inverse */
for ( i = 0 ; i < NombreDeTermes ; i++ ) {
	if ( T[Variable[i]] == 1 ) {
	  SecondMembre -= Coeff[i];
	  Coeff[i] *= -1;
	  T[Variable[i]] = 0;
  }
	if ( Coeff[i] == 0 ) {
		Coeff[i] = Coeff[NombreDeTermes-1];
    Variable[i] = Variable[NombreDeTermes-1];
		i--; 
		NombreDeTermes--;
	}
}
if ( ToutEstAZero == OUI_PNE ) NombreDeTermes = 0;
*Sec = SecondMembre;
*NbT = NombreDeTermes;

return;
}

/*----------------------------------------------------------------------------*/

void PNE_MajBlocContraintesPourLaRechercheDesCoupesDeSacADos( PROBLEME_PNE * Pne, int * IndexLibre, int NombreDeTermes, int * Variable, double  * Coeff, double SecondMembre )
{
int il; int i; int TailleAlloueesPourK; double * APourK; int * NuvarPourK; 

APourK = Pne->APourK;
if ( APourK == NULL ) return;
NuvarPourK = Pne->NuvarPourK;
TailleAlloueesPourK = Pne->TailleAlloueesPourK;

if ( Pne->NbContraintesPourK >= Pne->NbContraintesAlloueesPourK ) {
  PNE_ReAllocNombreDeContraintesPourLaRechercheDesCoupesDeSacADos( Pne );
	if ( Pne->MdebPourK == NULL ) return;
}
		
il = *IndexLibre;
Pne->MdebPourK[Pne->NbContraintesPourK] = il;
Pne->NbTermPourK[Pne->NbContraintesPourK] = NombreDeTermes;
Pne->BPourK[Pne->NbContraintesPourK] = SecondMembre;
		
for ( i = 0 ; i < NombreDeTermes ; i++ ) {
  if ( il >= TailleAlloueesPourK ) {
    PNE_ReAllocTailleContraintesPourLaRechercheDesCoupesDeSacADos( Pne );
    TailleAlloueesPourK = Pne->TailleAlloueesPourK;
    APourK = Pne->APourK;
		if ( APourK == NULL ) return;
    NuvarPourK = Pne->NuvarPourK;
  }		
  APourK[il] = Coeff[i];
  NuvarPourK[il] = Variable[i];
	il++;
}
*IndexLibre = il;
Pne->NbContraintesPourK++;

return;
}

/*----------------------------------------------------------------------------*/

void PNE_TransfertDeLaContrainteDansLeVecteurDeSynthese( PROBLEME_PNE * Pne, char * T, int NombreDeVariablesBinaires, int * NumeroDeLaVariableBinaire, double * I,
                                                         int NombreDeVariablesContinues, int * NumeroDeLaVariableContinue, double * V, 
                                                         int * NbT, double * Sec, double * Coeff, int * Variable, char * TermeTresPetit )
{
int i; int NombreDeTermes; int Var; double SecondMembre; double * Xmin; double * Xmax; int * TypeDeBorne;

/* Remarque: sauf bug il ne peut y avoir de variable fixe dans les vecteurs transmis ni de variables telles que Xmin = Xmax */

TypeDeBorne = Pne->TypeDeBorneTrav;
Xmin = Pne->UminTravSv;
Xmax = Pne->UmaxTravSv;
# if BORNES_INF_AUXILIAIRES == OUI_PNE
  Xmin = Pne->XminAuxiliaire;
# endif

*NbT = 0;
SecondMembre = *Sec;
*TermeTresPetit = NON_PNE;	
/* Transfert des vecteurs dans le vecteur synthese */
NombreDeTermes = 0;	
for ( i = 0 ; i < NombreDeVariablesBinaires ; i++ ) {
	Var = NumeroDeLaVariableBinaire[i];
	/* Test epsilon au cas ou des annulations se seraient produites */
	if ( fabs( I[Var] ) > EPSILON_COEFF ) {		
    Coeff[NombreDeTermes] = I[Var];
		Variable[NombreDeTermes] = Var;
		NombreDeTermes++;
	}
  I[Var] = 0;
	T[Var] = 0;
}
	
for ( i = 0 ; i < NombreDeVariablesContinues ; i++ ) {
	Var = NumeroDeLaVariableContinue[i];
	/* Test epsilon au cas ou des annulations se seraient produites */
	if ( fabs( V[Var] ) > EPSILON_COEFF ) {
		/* Si le terme n'est pas trop petit on accepte de placer la variable sur borne sinon on invalide la contrainte */
		if ( fabs( V[Var] ) > EPSILON ) {
		  if ( V[Var] < 0.0 ) {			
		    /* Il faut monter la variable au max */						
			  if ( TypeDeBorne[Var] == VARIABLE_BORNEE_DES_DEUX_COTES || TypeDeBorne[Var] == VARIABLE_BORNEE_SUPERIEUREMENT ) {
			    SecondMembre -= V[Var] * Xmax[Var];
        }								
			  else NombreDeTermes = 0;
		  }
		  else {
		    /* Il faut baisser la variable au min */    				
			  if ( TypeDeBorne[Var] == VARIABLE_BORNEE_DES_DEUX_COTES || TypeDeBorne[Var] == VARIABLE_BORNEE_INFERIEUREMENT ) {
			    SecondMembre -= V[Var] * Xmin[Var];					
        }				
			  else NombreDeTermes = 0;
			}
    }
		else {
			/*printf("Invalidation car terme tres petit \n");*/
			*TermeTresPetit = OUI_PNE;
		}
	}
  V[Var] = 0;
	T[Var] = 0;		
}
*NbT = NombreDeTermes;
*Sec = SecondMembre;

return;
}

/*----------------------------------------------------------------------------*/

void PNE_TestContraintePourKToujoursSatisfaite( int NombreDeTermes, double * Coeff, int * Variable, double SecondMembre,
                                                double * Xmin, double * Xmax, char * ContrainteToujoursSatisfaite )
{
int i; double S; 
*ContrainteToujoursSatisfaite = NON_PNE;
S = 0;
for ( i = 0 ; i < NombreDeTermes ; i++ ) {
  if ( Coeff[i] > 0 ) S += Coeff[i] * Xmax[Variable[i]];
  else S += Coeff[i] * Xmin[Variable[i]];
}		
if ( S < SecondMembre + EPSILON ) *ContrainteToujoursSatisfaite = OUI_PNE;	
return;
}

/*----------------------------------------------------------------------------*/

void PNE_TransfererLaContrainteNativeDansLesVecteursDeTravail( int * NbVacCont, int * NbVarBin, double * Sec, char * T, double * I, double * V,
                                                               int * NumeroDeLaVariableBinaire, int * NumeroDeLaVariableContinue,
                                                               int Cnt, double Signe, int * Mdeb, int * NbTerm, double * B, double * A, int * Nuvar,
																															 int * TypeDeBorne, int * TypeDeVariable, double * X, double * Xmin, double * Xmax )
{
int il; int ilMax; int NombreDeVariablesContinues; int NombreDeVariablesBinaires; double SecondMembre;
int Var; 

NombreDeVariablesContinues = 0;
NombreDeVariablesBinaires = 0;
SecondMembre = Signe * B[Cnt];

il = Mdeb[Cnt]; 
ilMax =  il + NbTerm[Cnt];
while ( il < ilMax ) { 
	if ( A[il] == 0.0 ) goto NextIl;
  Var = Nuvar[il];
  if ( TypeDeBorne[Var] == VARIABLE_FIXE ) { SecondMembre -= Signe * A[il] * X[Var]; goto NextIl; }
	if ( Xmin[Var] == Xmax[Var] ) { SecondMembre -= Signe * A[il] * Xmin[Var]; goto NextIl; }
		
	if ( TypeDeVariable[Var] == ENTIER ) {					
    I[Var] = Signe * A[il];
		T[Var] = 1;
		NumeroDeLaVariableBinaire[NombreDeVariablesBinaires] = Var;
		NombreDeVariablesBinaires++;			
	}						
	else {
	  /* La variable n'est pas entiere */			
    V[Var] = Signe * A[il];
		T[Var] = 1;
		NumeroDeLaVariableContinue[NombreDeVariablesContinues] = Var;
    NombreDeVariablesContinues++;			   
	}
	NextIl:
  il++; 
}
*NbVacCont = NombreDeVariablesContinues;
*NbVarBin = NombreDeVariablesBinaires;
*Sec = SecondMembre;

return;
}

/*----------------------------------------------------------------------------*/

void PNE_ContraintesNativesPourLaRechercheDesCoupesDeSacADos( PROBLEME_PNE * Pne, int * IndexLibre,  
                                                              double * Xmin, double * Xmax, double * Coeff, int * Variable,
                                                              int NombreDeVariablesContinues, int NombreDeVariablesBinaires,
																													    int * NumeroDeLaVariableContinue, int * NumeroDeLaVariableBinaire, 
																													    char * T, double * I, double * V, double SecondMembre )																												 
{
int NombreDeTermes; char TermeTresPetit; char Colineaire;
char ContrainteToujoursSatisfaite;

if ( NombreDeVariablesBinaires <= 0 ) goto Fin;

PNE_TransfertDeLaContrainteDansLeVecteurDeSynthese(  Pne, T, NombreDeVariablesBinaires, NumeroDeLaVariableBinaire, I,
                                                     NombreDeVariablesContinues, NumeroDeLaVariableContinue, V, 
                                                     &NombreDeTermes, &SecondMembre, Coeff, Variable, &TermeTresPetit );

if ( TermeTresPetit == OUI_PNE ) NombreDeTermes = 0;
	
/* Stockage de la contrainte */		
if ( NombreDeTermes <= 0 ) goto Fin;
	
/* Test contrainte toujours satisfaite */
PNE_TestContraintePourKToujoursSatisfaite( NombreDeTermes, Coeff, Variable, SecondMembre, Xmin, Xmax, &ContrainteToujoursSatisfaite );
if ( ContrainteToujoursSatisfaite == OUI_PNE ) goto Fin;
			
/* Recherche de colinearite */
PNE_DeterminerLaColineariteDesContraintesPourLaRechercheDesCoupesDeSacADos( Pne, T, V, SecondMembre, NombreDeTermes, &Colineaire, Coeff, Variable );
if ( Colineaire == OUI_PNE ) goto Fin;
		
PNE_MajBlocContraintesPourLaRechercheDesCoupesDeSacADos( Pne, IndexLibre, NombreDeTermes, Variable, Coeff, SecondMembre );
if ( Pne->MdebPourK == NULL ) goto Fin;

/* Rounding et PGCD */
PNE_RoundingEtPGCDDeLaContraintePourLaRechercheDesCoupesDeSacADos( &NombreDeTermes, T, Variable, Coeff, &SecondMembre );	
if ( NombreDeTermes <= 0 ) goto Fin;

PNE_TestContraintePourKToujoursSatisfaite( NombreDeTermes, Coeff, Variable, SecondMembre, Xmin, Xmax, &ContrainteToujoursSatisfaite );
if ( ContrainteToujoursSatisfaite == OUI_PNE ) goto Fin;

/* Recherche de colinearite */
PNE_DeterminerLaColineariteDesContraintesPourLaRechercheDesCoupesDeSacADos( Pne, T, V, SecondMembre, NombreDeTermes, &Colineaire, Coeff, Variable );			
if ( Colineaire == OUI_PNE ) goto Fin;				

PNE_MajBlocContraintesPourLaRechercheDesCoupesDeSacADos( Pne, IndexLibre, NombreDeTermes, Variable, Coeff, SecondMembre );
if ( Pne->MdebPourK == NULL ) goto Fin;

Fin:
return;
}

/*----------------------------------------------------------------------------*/

void PNE_CreerLesContraintesPourLaRechercheDesCoupesDeSacADosPourUnCritereDonne( PROBLEME_PNE * Pne, int * IndexLibre, char Critere, char * ContrainteUtilisable,
                                                                                 double * Xmin, double * Xmax, double * Coeff, int * Variable,
																																								 int * Cdeb, int * Csui, int * NumContrainte,
                                                                                 int * NombreDeVariablesContinuesDesContraintes, int * NombreDeVariablesBinairesDesContraintes,
                                                                                 int Cnt, int NombreDeVariablesContinues, int NombreDeVariablesBinaires,
																																								 int * NumeroDeLaVariableContinue, int * NumeroDeLaVariableBinaire, 
																																								 char * T, double * I, double * V, double SecondMembre,
																																								 int * NumeroDesContraintesAgregees, char * ContraintesAgregees )
{
int NombreDeContraintesAgregees; int NombreDeTermes; char TermeTresPetit; int i; char Colineaire;
char ContrainteToujoursSatisfaite;

NombreDeContraintesAgregees = 0;
*ContraintesAgregees = NON_PNE;

if ( NombreDeVariablesContinues > 0 && NombreDeVariablesContinues <  NOMBRE_MAX_DE_VARIABLES_CONTINUES ) {

  ContrainteUtilisable[Cnt] = NON_PNE;
  NumeroDesContraintesAgregees[NombreDeContraintesAgregees] = Cnt;
  NombreDeContraintesAgregees++;		

  PNE_KnapsackSupprimerLesVariablesContinues( Pne, ContrainteUtilisable, NombreDeVariablesContinuesDesContraintes,
	                                            NombreDeVariablesBinairesDesContraintes, T,
																						  V, &NombreDeVariablesContinues, NumeroDeLaVariableContinue,
																						  I, &NombreDeVariablesBinaires,  NumeroDeLaVariableBinaire,
																							&SecondMembre, &NombreDeContraintesAgregees, NumeroDesContraintesAgregees, Critere,
																							Cdeb, Csui, NumContrainte );																										
																							
 if ( NombreDeContraintesAgregees > 1 ) *ContraintesAgregees = OUI_PNE;
 
}

PNE_TransfertDeLaContrainteDansLeVecteurDeSynthese(  Pne, T, NombreDeVariablesBinaires, NumeroDeLaVariableBinaire, I,
                                                     NombreDeVariablesContinues, NumeroDeLaVariableContinue, V, 
                                                     &NombreDeTermes, &SecondMembre, Coeff, Variable, &TermeTresPetit );

if ( NombreDeContraintesAgregees <= 1 ) 	NombreDeTermes = 0; /* Car deja stockee */


for ( i = 0 ; i < NombreDeContraintesAgregees ; i++ ) ContrainteUtilisable[NumeroDesContraintesAgregees[i]] = OUI_PNE;		
NombreDeContraintesAgregees = 0;	
	
if ( TermeTresPetit == OUI_PNE ) NombreDeTermes = 0;
	
/* Stockage de la contrainte */		
if ( NombreDeTermes <= 0 ) goto Fin;
	
/* Test contrainte toujours satisfaite */
PNE_TestContraintePourKToujoursSatisfaite( NombreDeTermes, Coeff, Variable, SecondMembre, Xmin, Xmax, &ContrainteToujoursSatisfaite );
if ( ContrainteToujoursSatisfaite == OUI_PNE ) goto Fin;
			
/* Recherche de colinearite */
PNE_DeterminerLaColineariteDesContraintesPourLaRechercheDesCoupesDeSacADos( Pne, T, V, SecondMembre, NombreDeTermes, &Colineaire, Coeff, Variable );		
if ( Colineaire == OUI_PNE ) goto Fin;
		
PNE_MajBlocContraintesPourLaRechercheDesCoupesDeSacADos( Pne, IndexLibre, NombreDeTermes, Variable, Coeff, SecondMembre );
if ( Pne->MdebPourK == NULL ) goto Fin;

/* Rounding et PGCD */
PNE_RoundingEtPGCDDeLaContraintePourLaRechercheDesCoupesDeSacADos( &NombreDeTermes, T, Variable, Coeff, &SecondMembre );	
if ( NombreDeTermes <= 0 ) goto Fin;

PNE_TestContraintePourKToujoursSatisfaite( NombreDeTermes, Coeff, Variable, SecondMembre, Xmin, Xmax, &ContrainteToujoursSatisfaite );
if ( ContrainteToujoursSatisfaite == OUI_PNE ) goto Fin;

/* Recherche de colinearite */
PNE_DeterminerLaColineariteDesContraintesPourLaRechercheDesCoupesDeSacADos( Pne, T, V, SecondMembre, NombreDeTermes, &Colineaire, Coeff, Variable );			
if ( Colineaire == OUI_PNE ) goto Fin;				

PNE_MajBlocContraintesPourLaRechercheDesCoupesDeSacADos( Pne, IndexLibre, NombreDeTermes, Variable, Coeff, SecondMembre );
if ( Pne->MdebPourK == NULL ) goto Fin;

Fin:
 		
return;
}

/*----------------------------------------------------------------------------*/

void PNE_CreerLesContraintesPourLaRechercheDesCoupesDeSacADosPourPlusieursCritere( PROBLEME_PNE * Pne, int * IndexLibre, char * ContrainteUtilisable ,
                                                                                   double * I, double * V, char * T, int * NumeroDeLaVariableBinaire,
																																									 int * NumeroDeLaVariableContinue, int * NombreDeVariablesContinuesDesContraintes,
                                                                                   int * NombreDeVariablesBinairesDesContraintes, int * NumeroDesContraintesAgregees,
																																							     int * Cdeb, int * Csui, int * NumContrainte )																																																																																	
{
int Cnt; int NombreDeContraintes; double SecondMembre; int * Mdeb; int * NbTerm; double * B; double * A; int * Nuvar;
int * TypeDeVariable; int * TypeDeBorne; double * X; double * Xmin; double * Xmax;  int NombreDeVariablesContinues;
int NombreDeVariablesBinaires; int NombreDeVariables; double * Coeff; int * Variable; char * SensContrainte; double Signe;
char Critere; char ContraintesAgregees;

NombreDeContraintes = Pne->NombreDeContraintesTrav;
NombreDeVariables = Pne->NombreDeVariablesTrav;

Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
SensContrainte = Pne->SensContrainteTrav;
B = Pne->BTrav;   
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
X = Pne->UTrav;
Xmin = Pne->UminTravSv;
Xmax = Pne->UmaxTravSv;
# if BORNES_INF_AUXILIAIRES == OUI_PNE
  Xmin = Pne->XminAuxiliaire;
# endif

Coeff = Pne->ValeurLocale;
Variable = Pne->IndiceLocal;

for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {

  if ( ContrainteUtilisable[Cnt] == NON_PNE ) continue;
			
	Signe = 1;
	RechercheKnapack:

	/* On commence archiver la contrainte native et sa derive obtenue apres rounding et PGCD */

  PNE_TransfererLaContrainteNativeDansLesVecteursDeTravail( &NombreDeVariablesContinues, &NombreDeVariablesBinaires, &SecondMembre, T, I, V,
                                                            NumeroDeLaVariableBinaire, NumeroDeLaVariableContinue, Cnt, Signe, Mdeb, NbTerm,
																														B, A, Nuvar, TypeDeBorne, TypeDeVariable, X, Xmin, Xmax );

  PNE_ContraintesNativesPourLaRechercheDesCoupesDeSacADos( Pne, IndexLibre, Xmin, Xmax, Coeff, Variable, NombreDeVariablesContinues, NombreDeVariablesBinaires,
																												 	 NumeroDeLaVariableContinue, NumeroDeLaVariableBinaire, T, I, V, SecondMembre );					

  Critere = MAX_VAR_ENTIERES;
  PNE_TransfererLaContrainteNativeDansLesVecteursDeTravail( &NombreDeVariablesContinues, &NombreDeVariablesBinaires, &SecondMembre, T, I, V,
                                                            NumeroDeLaVariableBinaire, NumeroDeLaVariableContinue, Cnt, Signe, Mdeb, NbTerm,
																														B, A, Nuvar, TypeDeBorne, TypeDeVariable, X, Xmin, Xmax );

  PNE_CreerLesContraintesPourLaRechercheDesCoupesDeSacADosPourUnCritereDonne( Pne, IndexLibre, Critere, ContrainteUtilisable, Xmin, Xmax, Coeff, Variable,
																																						  Cdeb, Csui, NumContrainte,
                                                                              NombreDeVariablesContinuesDesContraintes, NombreDeVariablesBinairesDesContraintes,
                                                                              Cnt, NombreDeVariablesContinues, NombreDeVariablesBinaires,
																																						  NumeroDeLaVariableContinue, NumeroDeLaVariableBinaire, 
																																						  T, I, V, SecondMembre,
																																							NumeroDesContraintesAgregees, &ContraintesAgregees );
																																								 
  if ( Pne->MdebPourK == NULL ) goto Fin;
	if ( ContraintesAgregees == NON_PNE ) goto EtapeSuivante;

	Critere = MIN_VAR_CONTINUES;
  PNE_TransfererLaContrainteNativeDansLesVecteursDeTravail( &NombreDeVariablesContinues, &NombreDeVariablesBinaires, &SecondMembre, T, I, V,
                                                            NumeroDeLaVariableBinaire, NumeroDeLaVariableContinue, Cnt, Signe, Mdeb, NbTerm,
																														B, A, Nuvar, TypeDeBorne, TypeDeVariable, X, Xmin, Xmax );

  PNE_CreerLesContraintesPourLaRechercheDesCoupesDeSacADosPourUnCritereDonne( Pne, IndexLibre, Critere, ContrainteUtilisable, Xmin, Xmax, Coeff, Variable,
																																						  Cdeb, Csui, NumContrainte,
                                                                              NombreDeVariablesContinuesDesContraintes, NombreDeVariablesBinairesDesContraintes,
                                                                              Cnt, NombreDeVariablesContinues, NombreDeVariablesBinaires,
																																						  NumeroDeLaVariableContinue, NumeroDeLaVariableBinaire, 
																																						  T, I, V, SecondMembre,
																																							NumeroDesContraintesAgregees, &ContraintesAgregees );
																																								 
  if ( Pne->MdebPourK == NULL ) goto Fin;
	 		
	EtapeSuivante:
	
  if ( Signe == 1 ) {
    if ( SensContrainte[Cnt] == '=' ) {
      Signe = -1;
      goto RechercheKnapack;
		}
	}
	
}

Fin:

/*printf("1- NbContraintesPourK %d\n",Pne->NbContraintesPourK);*/

return;
}

/*----------------------------------------------------------------------------*/

void PNE_ChainageParColonnePourLaRechercheDesCoupesDeSacADos( int Var, int * CdebAuxiliaire, int * CsuiAuxiliaire, int * NumContrainteAuxiliaire,
                                                              int * NombreDeVariablesContinuesDesContraintes, int CntNew, int NbVarCont, int icNew )
{
int Cnt; int ic; int ic1;
if ( CdebAuxiliaire[Var] < 0 ) {
  CdebAuxiliaire[Var] = icNew;
	CsuiAuxiliaire[icNew] = -1;
	NumContrainteAuxiliaire[icNew] = CntNew;
	return;
}

ic = CdebAuxiliaire[Var];
ic1 = -1;
while ( ic >= 0 ) {
  Cnt = NumContrainteAuxiliaire[ic];
  if ( NombreDeVariablesContinuesDesContraintes[Cnt] >= NbVarCont ) {
    /* On met CntNewAvant */
		if ( ic1 < 0 ) CdebAuxiliaire[Var] = icNew;		
		else CsuiAuxiliaire[ic1] = icNew;		
		CsuiAuxiliaire[icNew] = ic;
		NumContrainteAuxiliaire[icNew] = CntNew;
    return;		

  }
  ic1 = ic;
  ic = CsuiAuxiliaire[ic];
}
/* Si on arrive la c'est qu'il faut la classer en dernier */
CsuiAuxiliaire[ic1] = icNew;
CsuiAuxiliaire[icNew] = ic;
NumContrainteAuxiliaire[icNew] = CntNew;
return;
}

/*----------------------------------------------------------------------------*/
/* On ne le fait que pour les variables continues car sinon on n'en a pas besoin */
void PNE_ChainageParColonneDansLOrdreCroissantDuNombreDeTermes( PROBLEME_PNE * Pne, int * NombreDeVariablesContinuesDesContraintes,
                                                                int * CdebAuxiliaire, int * CsuiAuxiliaire, int * NumContrainteAuxiliaire,
																																char * ContrainteUtilisable )
{
int NombreDeContraintes; int Cnt; int il; int ilMax; int * TypeDeVariable; int * Mdeb; int * NbTerm;
double * A; int * Nuvar; int * TypeDeBorne; int NbVarCont; int NombreDeVariables; int Var; 
double * Xmin; double * Xmax;

NombreDeVariables = Pne->NombreDeVariablesTrav;
NombreDeContraintes = Pne->NombreDeContraintesTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;

Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;

Xmin = Pne->UminTravSv;
Xmax = Pne->UmaxTravSv;
# if BORNES_INF_AUXILIAIRES == OUI_PNE
  Xmin = Pne->XminAuxiliaire;
# endif

for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) CdebAuxiliaire[Var] = -1;

for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  if ( NombreDeVariablesContinuesDesContraintes[Cnt] <= 0 ) continue;
	if ( ContrainteUtilisable[Cnt] == NON_PNE ) continue;
	NbVarCont = NombreDeVariablesContinuesDesContraintes[Cnt];
  il    = Mdeb[Cnt];
  ilMax = il + NbTerm[Cnt];
  while ( il < ilMax ) {
	  if ( A[il] == 0 ) goto NextIl;
    Var = Nuvar[il];
    if ( Xmin[Var] == Xmax[Var] ) goto NextIl;
		if ( TypeDeVariable[Var] == ENTIER ) goto NextIl;		
    PNE_ChainageParColonnePourLaRechercheDesCoupesDeSacADos( Var, CdebAuxiliaire, CsuiAuxiliaire, NumContrainteAuxiliaire,
                                                             NombreDeVariablesContinuesDesContraintes, Cnt, NbVarCont, il );		
		NextIl:
    il++;
  }
}

return;
}

/*----------------------------------------------------------------------------*/

void PNE_AvecLeBlocKCreerUneContraintePourLaRechercheDesCoupesDeSacADos( PROBLEME_PNE * Pne, int * IndexLibre, char * ContrainteUtilisable,
                                                                         double * Xmin, double * Xmax, double * Coeff, int * Variable,
																																				 int * Cdeb, int * Csui, int * NumContrainte,                                                                   
                                                                         int Cnt, int NombreDeVariablesContinues, int NombreDeVariablesBinaires,
																																				 int * NumeroDeLaVariableContinue, int * NumeroDeLaVariableBinaire, 
																																				 char * T, double * I, double * V, double SecondMembre,
																																				 int * NumeroDesContraintesAgregees )
{
int NombreDeContraintesAgregees; int NombreDeTermes; char TermeTresPetit; int i; char Colineaire;
char ContrainteToujoursSatisfaite; int NbBinSv;

NbBinSv = NombreDeVariablesBinaires;

NombreDeContraintesAgregees = 0;

ContrainteUtilisable[Cnt] = NON_PNE;
NumeroDesContraintesAgregees[NombreDeContraintesAgregees] = Cnt;
NombreDeContraintesAgregees++;		

PNE_BlocPourKSupprimerDesVariablesBinaires( Pne, ContrainteUtilisable, T, I, &NombreDeVariablesBinaires, NumeroDeLaVariableBinaire,
																						&SecondMembre, &NombreDeContraintesAgregees, NumeroDesContraintesAgregees, Cdeb, Csui, NumContrainte );				
													
PNE_TransfertDeLaContrainteDansLeVecteurDeSynthese( Pne, T, NombreDeVariablesBinaires, NumeroDeLaVariableBinaire, I,
                                                    NombreDeVariablesContinues, NumeroDeLaVariableContinue, V, 
                                                    &NombreDeTermes, &SecondMembre, Coeff, Variable, &TermeTresPetit );
		
for ( i = 0 ; i < NombreDeContraintesAgregees ; i++ ) ContrainteUtilisable[NumeroDesContraintesAgregees[i]] = OUI_PNE;
		
NombreDeContraintesAgregees = 0;	
	
if ( NombreDeTermes == NbBinSv ) goto Fin;

if ( TermeTresPetit == OUI_PNE ) NombreDeTermes = 0;
	
/* Stockage de la contrainte */		
if ( NombreDeTermes <= 0 ) goto Fin;
	
/* Test contrainte toujours satisfaite */
PNE_TestContraintePourKToujoursSatisfaite( NombreDeTermes, Coeff, Variable, SecondMembre, Xmin, Xmax, &ContrainteToujoursSatisfaite );
if ( ContrainteToujoursSatisfaite == OUI_PNE ) goto Fin;
			
/* Recherche de colinearite */
PNE_DeterminerLaColineariteDesContraintesPourLaRechercheDesCoupesDeSacADos( Pne, T, V, SecondMembre, NombreDeTermes, &Colineaire, Coeff, Variable );		
if ( Colineaire == OUI_PNE ) goto Fin;

PNE_MajBlocContraintesPourLaRechercheDesCoupesDeSacADos( Pne, IndexLibre, NombreDeTermes, Variable, Coeff, SecondMembre );
if ( Pne->MdebPourK == NULL ) goto Fin;

/* Rounding et PGCD */
PNE_RoundingEtPGCDDeLaContraintePourLaRechercheDesCoupesDeSacADos( &NombreDeTermes, T, Variable, Coeff, &SecondMembre );	
if ( NombreDeTermes <= 0 ) goto Fin;

PNE_TestContraintePourKToujoursSatisfaite( NombreDeTermes, Coeff, Variable, SecondMembre, Xmin, Xmax, &ContrainteToujoursSatisfaite );
if ( ContrainteToujoursSatisfaite == OUI_PNE ) goto Fin;

/* Recherche de colinearite */
PNE_DeterminerLaColineariteDesContraintesPourLaRechercheDesCoupesDeSacADos( Pne, T, V, SecondMembre, NombreDeTermes, &Colineaire, Coeff, Variable );			
if ( Colineaire == OUI_PNE ) goto Fin;		
		
PNE_MajBlocContraintesPourLaRechercheDesCoupesDeSacADos( Pne, IndexLibre, NombreDeTermes, Variable, Coeff, SecondMembre );
if ( Pne->MdebPourK == NULL ) goto Fin;

Fin:
 		
return;
}

/*----------------------------------------------------------------------------*/

void PNE_AvecLeBlocKCreerLesContraintesPourLaRechercheDesCoupesDeSacADos( PROBLEME_PNE * Pne, int * IndexLibre, char * ContrainteUtilisable,
                                                                          double * I, double * V, char * T, 
                                                                          int * NumeroDeLaVariableBinaire, int * NumeroDeLaVariableContinue, 
                                                                          int * NumeroDesContraintesAgregees, int * Cdeb, int * Csui, int * NumContrainte )																																						
{
int Cnt; int NombreDeContraintes; double SecondMembre; int * TypeDeVariable; int * TypeDeBorne; double * X; 
double * Xmin; double * Xmax;  int NombreDeVariablesContinues; int NombreDeVariablesBinaires; int NombreDeVariables; 
double * Coeff; int * Variable; double Signe; 

NombreDeVariables = Pne->NombreDeVariablesTrav;

NombreDeContraintes = Pne->NbContraintesPourK;

TypeDeBorne = Pne->TypeDeBorneTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
X = Pne->UTrav;
Xmin = Pne->UminTravSv;
Xmax = Pne->UmaxTravSv;
# if BORNES_INF_AUXILIAIRES == OUI_PNE
  Xmin = Pne->XminAuxiliaire;
# endif

Coeff = Pne->ValeurLocale;
Variable = Pne->IndiceLocal;

Signe = 1;
for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {

  PNE_TransfererLaContrainteNativeDansLesVecteursDeTravail( &NombreDeVariablesContinues, &NombreDeVariablesBinaires, &SecondMembre, T, I, V,
                                                            NumeroDeLaVariableBinaire, NumeroDeLaVariableContinue, Cnt, Signe, 
                                                            Pne->MdebPourK, Pne->NbTermPourK, Pne->BPourK, Pne->APourK, Pne->NuvarPourK,
                                                            TypeDeBorne, TypeDeVariable, X, Xmin, Xmax );

  PNE_AvecLeBlocKCreerUneContraintePourLaRechercheDesCoupesDeSacADos( Pne, IndexLibre, ContrainteUtilisable, Xmin, Xmax, Coeff, Variable,
																																			Cdeb, Csui, NumContrainte, Cnt, NombreDeVariablesContinues, NombreDeVariablesBinaires,
																																			NumeroDeLaVariableContinue, NumeroDeLaVariableBinaire, T, I, V, SecondMembre,
																																			NumeroDesContraintesAgregees );
																																						 
  if ( Pne->MdebPourK == NULL ) goto Fin;
	
}

Fin:

/*printf("2- NbContraintesPourK %d\n",Pne->NbContraintesPourK);*/

return;
}

/*----------------------------------------------------------------------------*/

void PNE_ChainageParColonneDuBlocKPourLaRechercheDesCoupesDeSacADos( int Var, int * NbTerm, int * CdebAuxiliaire, int * CsuiAuxiliaire, 
                                                                     int * NumContrainteAuxiliaire, int CntNew, int NbVarDeCnt, int icNew )	
{
int Cnt; int ic; int ic1;
if ( CdebAuxiliaire[Var] < 0 ) {
  CdebAuxiliaire[Var] = icNew;
	CsuiAuxiliaire[icNew] = -1;
	NumContrainteAuxiliaire[icNew] = CntNew;
	return;
}

ic = CdebAuxiliaire[Var];
ic1 = -1;
while ( ic >= 0 ) {
  Cnt = NumContrainteAuxiliaire[ic];
  if ( NbTerm[Cnt] >= NbVarDeCnt ) {
    /* On met CntNewAvant */
		if ( ic1 < 0 ) CdebAuxiliaire[Var] = icNew;		
		else CsuiAuxiliaire[ic1] = icNew;		
		CsuiAuxiliaire[icNew] = ic;
		NumContrainteAuxiliaire[icNew] = CntNew;
    return;		

  }
  ic1 = ic;
  ic = CsuiAuxiliaire[ic];
}
/* Si on arrive la c'est qu'il faut la classer en dernier */
CsuiAuxiliaire[ic1] = icNew;
CsuiAuxiliaire[icNew] = ic;
NumContrainteAuxiliaire[icNew] = CntNew;
return;
}

/*----------------------------------------------------------------------------*/

void PNE_ParColonneDuBlocPourKDansLOrdreCroissantDuNombreDeTermes( PROBLEME_PNE * Pne, int * CdebAuxiliaire, int * CsuiAuxiliaire, 
                                                                   int * NumContrainteAuxiliaire )
{
int NombreDeContraintes; int Cnt; int il; int ilMax; int * Mdeb; int * NbTerm;
double * A; int * Nuvar; int NbVarDeCnt; int NombreDeVariables; int Var;
double * Xmin; double * Xmax;

NombreDeVariables = Pne->NombreDeVariablesTrav;
NombreDeContraintes = Pne->NombreDeContraintesTrav;

NombreDeContraintes = Pne->NbContraintesPourK;
Mdeb = Pne->MdebPourK;
NbTerm = Pne->NbTermPourK;
Nuvar = Pne->NuvarPourK;
A = Pne->APourK;

Xmin = Pne->UminTravSv;
Xmax = Pne->UmaxTravSv;
# if BORNES_INF_AUXILIAIRES == OUI_PNE
  Xmin = Pne->XminAuxiliaire;
# endif

for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) CdebAuxiliaire[Var] = -1;

for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  il = Mdeb[Cnt];
  NbVarDeCnt = NbTerm[Cnt];
  ilMax = il + NbVarDeCnt;
  while ( il < ilMax ) {
    if ( A[il] != 0 && Xmin[Nuvar[il]] != Xmax[Nuvar[il]] ) {
      PNE_ChainageParColonneDuBlocKPourLaRechercheDesCoupesDeSacADos( Nuvar[il], NbTerm, CdebAuxiliaire, CsuiAuxiliaire, NumContrainteAuxiliaire,
                                                                      Cnt, NbVarDeCnt, il );		
    }
    il++;
  }
}

return;
}

/*----------------------------------------------------------------------------*/

void PNE_CreerLesContraintesPourLaRechercheDesCoupesDeSacADos( PROBLEME_PNE * Pne )

{
int Cnt; int il; int ilMax; int NombreDeContraintes; int Var; int * NombreDeVariablesBinairesDesContraintes;
char * ContrainteActivable; int * Mdeb; int * NbTerm; double * B; double * A; int * Nuvar; int * TypeDeVariable;
int * TypeDeBorne; double * X; double * Xmin; double * Xmax; double * I; double * V; char * T;
int * NumeroDeLaVariableBinaire; int * NumeroDeLaVariableContinue; char * ContrainteUtilisable; 
int * NombreDeVariablesContinuesDesContraintes; int * NumeroDesContraintesAgregees; int NombreDeVariables; int Nb; 
char * SensContrainte; int NbEntiers; int IndexLibre; int NbTermesAlloues;
int * CdebAuxiliaire; int * CsuiAuxiliaire; int * NumContrainteAuxiliaire;

PNE_AllocContraintesPourLaRechercheDesCoupesDeSacADos( Pne );
if ( Pne->NbContraintesAlloueesPourK == 0 ) return;

NombreDeContraintes = Pne->NombreDeContraintesTrav;
NombreDeVariables = Pne->NombreDeVariablesTrav;
ContrainteActivable = Pne->ContrainteActivable;

Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
SensContrainte = Pne->SensContrainteTrav;
B = Pne->BTrav;   
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
X = Pne->UTrav;
Xmin = Pne->UminTravSv;
Xmax = Pne->UmaxTravSv;
# if BORNES_INF_AUXILIAIRES == OUI_PNE
  Xmin = Pne->XminAuxiliaire;
# endif

V = NULL;
I = NULL;
T = NULL;
NumeroDeLaVariableBinaire = NULL;
NumeroDeLaVariableContinue = NULL;
ContrainteUtilisable = NULL;
NumeroDesContraintesAgregees = NULL;
NombreDeVariablesContinuesDesContraintes = NULL;
NombreDeVariablesBinairesDesContraintes = NULL;

CdebAuxiliaire = NULL;
CsuiAuxiliaire = NULL;
NumContrainteAuxiliaire = NULL;

V = (double *) malloc( NombreDeVariables * sizeof( double ) );
if ( V == NULL ) goto Fin;
I = (double *) malloc( NombreDeVariables * sizeof( double ) );
if ( I == NULL ) goto Fin;
T = (char *) malloc( NombreDeVariables * sizeof( char ) );
if ( T == NULL ) goto Fin;

/* On n'a pas besoin de mettre V et I a 0 car c'est T qui repere les emplacements initialises */
for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) { V[Var] = 0; I[Var] = 0; T[Var] = 0; }

NumeroDeLaVariableBinaire = (int *) malloc( NombreDeVariables * sizeof( int ) );
if ( NumeroDeLaVariableBinaire == NULL ) goto Fin;
NumeroDeLaVariableContinue = (int *) malloc( NombreDeVariables * sizeof( int ) );
if ( NumeroDeLaVariableContinue == NULL ) goto Fin;

ContrainteUtilisable = (char *) malloc( NombreDeContraintes * sizeof( char ) );
if ( ContrainteUtilisable == NULL ) goto Fin;

NumeroDesContraintesAgregees = (int *) malloc( NombreDeContraintes * sizeof( int ) );
if ( NumeroDesContraintesAgregees == NULL ) goto Fin;

NombreDeVariablesContinuesDesContraintes = (int *) malloc( NombreDeContraintes * sizeof( int ) );
if ( NombreDeVariablesContinuesDesContraintes == NULL ) goto Fin;

NombreDeVariablesBinairesDesContraintes = (int *) malloc( NombreDeContraintes * sizeof( int ) );
if ( NombreDeVariablesBinairesDesContraintes == NULL ) goto Fin;

NbTermesAlloues = 0;
for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  il = Mdeb[Cnt]; 
  ilMax =  il + NbTerm[Cnt];
	if ( ilMax > NbTermesAlloues ) NbTermesAlloues = ilMax;	
  if ( ContrainteActivable != NULL ) ContrainteUtilisable[Cnt] = ContrainteActivable[Cnt];
	else ContrainteUtilisable[Cnt] = OUI_PNE;
	Nb = 0;
	NbEntiers = 0;
  while ( il < ilMax ) {
		if ( A[il] == 0.0 ) goto Next;
    Var = Nuvar[il];
		if ( TypeDeBorne[Var] == VARIABLE_FIXE ) goto Next;
		if ( Xmin[Var] == Xmax[Var] ) goto Next;
		if ( TypeDeVariable[Var] == ENTIER ) NbEntiers++; 
		else Nb++; /* Variable continue */
    Next:
	  il++;
	}
  NombreDeVariablesContinuesDesContraintes[Cnt] = Nb;
  NombreDeVariablesBinairesDesContraintes[Cnt] = NbEntiers;
}

CdebAuxiliaire = (int *) malloc( NombreDeVariables * sizeof( int ) );
if ( CdebAuxiliaire == NULL ) goto Fin;
CsuiAuxiliaire = (int *) malloc( NbTermesAlloues * sizeof( int ) );
if ( CsuiAuxiliaire == NULL ) goto Fin;
NumContrainteAuxiliaire = (int *) malloc( NbTermesAlloues * sizeof( int ) );
if ( NumContrainteAuxiliaire == NULL ) goto Fin;

PNE_ChainageParColonneDansLOrdreCroissantDuNombreDeTermes( Pne, NombreDeVariablesContinuesDesContraintes, CdebAuxiliaire, CsuiAuxiliaire,
                                                           NumContrainteAuxiliaire, ContrainteUtilisable );
																												 
IndexLibre = 0;																																							
PNE_CreerLesContraintesPourLaRechercheDesCoupesDeSacADosPourPlusieursCritere( Pne, &IndexLibre, ContrainteUtilisable, I, V, T, NumeroDeLaVariableBinaire,
																																						  NumeroDeLaVariableContinue, NombreDeVariablesContinuesDesContraintes,
                                                                              NombreDeVariablesBinairesDesContraintes, NumeroDesContraintesAgregees,
																																						  CdebAuxiliaire, CsuiAuxiliaire, NumContrainteAuxiliaire );																																							 

NombreDeContraintes = Pne->NbContraintesPourK;
Mdeb = Pne->MdebPourK;
NbTerm = Pne->NbTermPourK;
Nuvar = Pne->NuvarPourK;
A = Pne->APourK;

free( ContrainteUtilisable );
ContrainteUtilisable = NULL;
free( NumeroDesContraintesAgregees );
NumeroDesContraintesAgregees = NULL;
free( CsuiAuxiliaire );
CsuiAuxiliaire = NULL;
free( NumContrainteAuxiliaire );
NumContrainteAuxiliaire = NULL;

ContrainteUtilisable = (char *) malloc( NombreDeContraintes * sizeof( char ) );
if ( ContrainteUtilisable == NULL ) goto Fin;

NumeroDesContraintesAgregees = (int *) malloc( NombreDeContraintes * sizeof( int ) );
if ( NumeroDesContraintesAgregees == NULL ) goto Fin;

NbTermesAlloues = 0;
for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
	ContrainteUtilisable[Cnt] = OUI_PNE;
  il = Mdeb[Cnt]; 
  ilMax =  il + NbTerm[Cnt];
	if ( ilMax > NbTermesAlloues ) NbTermesAlloues = ilMax;	
}

CsuiAuxiliaire = (int *) malloc( NbTermesAlloues * sizeof( int ) );
if ( CsuiAuxiliaire == NULL ) goto Fin;
NumContrainteAuxiliaire = (int *) malloc( NbTermesAlloues * sizeof( int ) );
if ( NumContrainteAuxiliaire == NULL ) goto Fin;

PNE_ParColonneDuBlocPourKDansLOrdreCroissantDuNombreDeTermes( Pne, CdebAuxiliaire, CsuiAuxiliaire, NumContrainteAuxiliaire );

/* On essaie de combiner les contraintes du bloc pour les K */

PNE_AvecLeBlocKCreerLesContraintesPourLaRechercheDesCoupesDeSacADos( Pne, &IndexLibre, ContrainteUtilisable, I, V, T, NumeroDeLaVariableBinaire, 
                                                                     NumeroDeLaVariableContinue, NumeroDesContraintesAgregees, 
                                                                     CdebAuxiliaire, CsuiAuxiliaire, NumContrainteAuxiliaire );																																						


Fin: 
free( V );
free( I );
free( T );
free( NumeroDeLaVariableBinaire );
free( NumeroDeLaVariableContinue );
free( ContrainteUtilisable );
free( NumeroDesContraintesAgregees );
free( NombreDeVariablesContinuesDesContraintes );
free( NombreDeVariablesBinairesDesContraintes );
free( CdebAuxiliaire );
free( CsuiAuxiliaire );
free( NumContrainteAuxiliaire );

return;
}

/*----------------------------------------------------------------------------*/

void PNE_KnapsackAvecCombinaisonsDeContraintes( PROBLEME_PNE * Pne )
{
int Cnt; double S; char RendreLesCoeffsEntiers; char CouvertureTrouvee; int Var;
char UneVariableAEteFixee; int Faisabilite; int NombreDeContraintes; int * Mdeb; int * NbTerm;
double * B; double * A; int * Nuvar; int ilDeb; int il; int ilMax; double * Xmin; double * Xmax;
double Smin; double Smax; char Reboucler; double * X; int NbCntInutiles;

UneVariableAEteFixee = NON_PNE;

if ( Pne->CreerContraintesPourK == OUI_PNE || Pne->MdebPourK == NULL ) {
  PNE_CreerLesContraintesPourLaRechercheDesCoupesDeSacADos( Pne );
  Pne->CreerContraintesPourK = NON_PNE;
	if ( Pne->MdebPourK == NULL ||  Pne->NbContraintesPourK == 0 ) return;
  /* Controle des contraintes pour voir si on peut fixer des variables */
  X = Pne->UTrav;
  Xmin = Pne->UminTravSv;
  Xmax = Pne->UmaxTravSv;
  # if BORNES_INF_AUXILIAIRES == OUI_PNE
    Xmin = Pne->XminAuxiliaire;
  # endif

  NbCntInutiles = 0;

  NombreDeContraintes = Pne->NbContraintesPourK;
  Mdeb = Pne->MdebPourK;
  NbTerm = Pne->NbTermPourK;
  B = Pne->BPourK;
  A = Pne->APourK;
  Nuvar = Pne->NuvarPourK;
  FixationDeVariables:
  Reboucler = NON_PNE;
  for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
	  if ( NbTerm[Cnt] <= 0 ) continue;
    il = Mdeb[Cnt];
    ilMax = il + NbTerm[Cnt];
    ilDeb = il;
    Smin = 0;
    Smax = 0;
    while ( il < ilMax ) {
      Var = Nuvar[il];
      if ( Xmin[Var] == Xmax[Var] ) {
        B[Cnt] -= A[il] * Xmin[Var];
        A[il]= A[ilMax-1]; 
        Nuvar[il] = Nuvar[ilMax-1];
        NbTerm[Cnt]--;
        il--;
        ilMax--;
      }
      else {
        if ( A[il] > 0 ) { Smin += A[il] * Xmin[Var]; Smax += A[il] * Xmax[Var]; }
        else { Smin += A[il] * Xmax[Var]; Smax += A[il] * Xmin[Var]; }
      }
      il++;
    }
    if ( Smax <= B[Cnt] ) {
      /*printf("Contrainte pour K %d jamais violee\n",Cnt);*/
      NbTerm[Cnt] = 0;
      NbCntInutiles++;
      continue;
    }
    il = ilDeb;
    while ( il < ilMax ) {
      Var = Nuvar[il];
		  if ( A[il] > 0 ) {
        S = ( B[Cnt] - Smin + (A[il] * Xmin[Var]) ) / A[il];
			  if ( S < SEUIL_1 ) {
				  /*printf("On fixe la variable %d a 0 S = %e Coeff %e\n",Var,S,A[il]);*/
					UneVariableAEteFixee = OUI_PNE;
					PNE_KnapsackCombinaisonDeContraintesAppliquerLeConflictGraph( Pne, Var, 0.0 );
          Reboucler = OUI_PNE;	
          /* On enleve la variable de la contrainte */
          /*
          B[Cnt] -= A[il] * X[Var];
          A[il]= A[ilMax-1]; 
          Nuvar[il] = Nuvar[ilMax-1];
          NbTerm[Cnt]--;
          */
          break; /* Contrainte suivante */
				}
			}		
		  else {
        S = ( B[Cnt] - Smin + (A[il] * Xmax[Var]) ) / A[il];
			  if ( S > SEUIL_0 ) {
				  /*printf("On fixe la variable %d a 1 S = %e Coeff %e\n",Var,S,A[il]);*/								
					UneVariableAEteFixee = OUI_PNE;
					PNE_KnapsackCombinaisonDeContraintesAppliquerLeConflictGraph( Pne, Var, 1.0 );										
          Reboucler = OUI_PNE;	
          /* On enleve la variable de la contrainte */
          /*
          B[Cnt] -= A[il] * X[Var];
          A[il]= A[ilMax-1]; 
          Nuvar[il] = Nuvar[ilMax-1];
          NbTerm[Cnt]--;
          */
          break; /* Contrainte suivante */
				}
			}
      il++;
    }
  }
  if ( Reboucler == OUI_PNE ) goto FixationDeVariables;

  /*if ( NbCntInutiles > 0 ) printf("NbCntInutiles %d\n",NbCntInutiles);*/

  if ( UneVariableAEteFixee == OUI_PNE ) {
    /*Pne->CreerContraintesPourK = OUI_PNE;*/
    for ( Var = 0 ; Var < Pne->NombreDeVariablesTrav ; Var++ ) {
	    Pne->UminTrav[Var] = Pne->UminTravSv[Var];
	    Pne->UmaxTrav[Var] = Pne->UmaxTravSv[Var];
    }	
    PNE_InitBorneInfBorneSupDesVariables( Pne );
    PNE_CalculMinEtMaxDesContraintes( Pne, &Faisabilite );
    if ( Faisabilite == NON_PNE ) {	
		  /*printf("Pas de solution apres K sur combinaison de contraintes\n");*/ /* Pour l'instant on choisit de ne rien en faire */
    }	
    memcpy( (char *) Pne->ProbingOuNodePresolve->BminSv, (char *) Pne->ProbingOuNodePresolve->Bmin, Pne->NombreDeContraintesTrav * sizeof( double ) );
    memcpy( (char *) Pne->ProbingOuNodePresolve->BmaxSv, (char *) Pne->ProbingOuNodePresolve->Bmax, Pne->NombreDeContraintesTrav * sizeof( double ) );	
  }
	
}

NombreDeContraintes = Pne->NbContraintesPourK;
Mdeb = Pne->MdebPourK;
NbTerm = Pne->NbTermPourK;
B = Pne->BPourK;
A = Pne->APourK;
Nuvar = Pne->NuvarPourK;

for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  if ( NbTerm[Cnt] <= 0 ) continue;
  /* Recherche couverture de sac a dos */		
	if ( NbTerm[Cnt] >= MIN_TERMES_POUR_KNAPSACK ) {	
		RendreLesCoeffsEntiers = NON_PNE;
		CouvertureTrouvee = NON_PNE;		
		ilDeb = Mdeb[Cnt];	
    PNE_GreedyCoverKnapsack( Pne, 0, NbTerm[Cnt], &Nuvar[ilDeb], &A[ilDeb], B[Cnt], RendreLesCoeffsEntiers, &CouvertureTrouvee,
														 NON_PNE, 0.0, 0, NULL, NULL, NULL );				
    /*
		if ( CouvertureTrouvee == OUI_PNE ) {
			printf("   K trouvee sur combinaison !!!!!!!!!!!!!!\n");			
		}
		*/
	}
	
}

return;
}

/*----------------------------------------------------------------------------*/

void PNE_KnapsackCombinaisonDeContraintesAppliquerLeConflictGraph( PROBLEME_PNE * Pne, int Var, double ValeurDeVar )
{
int Edge; int Noeud; int Complement; int Nv; int Pivot; double * Xmax; double * Xmin;   
int * First; int * Adjacent; int * Next; double * X;

X = Pne->UTrav;
Xmin = Pne->UminTravSv;
Xmax = Pne->UmaxTravSv;

X[Var] = ValeurDeVar;
Xmin[Var] = ValeurDeVar;
Xmax[Var] = ValeurDeVar;
# if BORNES_INF_AUXILIAIRES == OUI_PNE
  Pne->XminAuxiliaire[Var] = ValeurDeVar;
# endif

if ( Pne->ConflictGraph == NULL ) return;
Pivot = Pne->ConflictGraph->Pivot;

if ( ValeurDeVar == 1.0 ) { Noeud = Var; Complement = Pivot + Var; }
else { Noeud = Pivot + Var; Complement = Var; }

First = Pne->ConflictGraph->First;
Adjacent = Pne->ConflictGraph->Adjacent;
Next = Pne->ConflictGraph->Next;

Edge = First[Noeud];
while ( Edge >= 0 ) {
  Nv = Adjacent[Edge];
	/* Attention a ne pas prendre le complement */
	if ( Nv == Complement ) goto NextEdge;
  if ( Nv < Pivot ) {
	  Var = Nv;
		/* On ne doit pas avoir U[Var] = 1.0 */
		if ( Xmin[Var] > 0.0001 ) return;  				
		if ( Xmin[Var] == Xmax[Var] ) goto NextEdge;
		/*printf("        fixation de var %d a 0 par graphe de conflit\n",Var);*/
		ValeurDeVar = 0.0;
    PNE_KnapsackCombinaisonDeContraintesAppliquerLeConflictGraph( Pne, Var, ValeurDeVar );
	}
  else {
    /* La valeur borne inf est interdite pour la variable */
		/* On doit donc fixer la variable a Umax et fixer les voisins de ce noeud */
	  Var = Nv - Pivot;
		/* On ne doit pas avoir U[Var] = 0.0 */
		if ( Xmax[Var] < 0.9999 ) return; 		
		if ( Xmin[Var] == Xmax[Var] ) goto NextEdge;
		/*printf("        fixation de var %d a 1 par graphe de conflit\n",Var);*/
		ValeurDeVar = 1.0;
    PNE_KnapsackCombinaisonDeContraintesAppliquerLeConflictGraph( Pne, Var, ValeurDeVar );
	}
	NextEdge:
  Edge = Next[Edge];
}

return;
}

/*----------------------------------------------------------------------------*/
