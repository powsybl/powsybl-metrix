/***********************************************************************

   FONCTION: Creation des coupes de probing. Une coupe est cree lorsque
	           l'instanciation d'une variable rend une contrainte toujours
						 satisfaite.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"  
  
# include "pne_fonctions.h"
# include "pne_define.h"
  
# include "spx_define.h"
  
# include "bb_define.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define TRACES 0

# define ZERO 1.e-10

# define CONTRAINTE_NATIVE 1
# define COUPE_DE_PROBING 2

# define SIZE_ALLOC_COUPES 100 /* Nombre de coupes de probing allouees */
# define SIZE_ALLOC_TERMES_COUPES (SIZE_ALLOC_COUPES*25)

# define UTILISER_LA_MARGE_EN_FIN_DE_CONTRAINTE OUI_PNE /*OUI_PNE*/
# if UTILISER_LA_MARGE_EN_FIN_DE_CONTRAINTE == OUI_PNE
  # if MARGE_EN_FIN_DE_CONTRAINTE == 0
    # define UTILISER_LA_MARGE_EN_FIN_DE_CONTRAINTE NON_PNE
  # endif
# endif

# define MODIFIER_DIRECTEMENT_LA_MATRICE OUI_PNE /* OUI_PNE */  

# if MODIFIER_DIRECTEMENT_LA_MATRICE == NON_PNE
  # undef UTILISER_LA_MARGE_EN_FIN_DE_CONTRAINTE
  # define UTILISER_LA_MARGE_EN_FIN_DE_CONTRAINTE NON_PNE
# endif

# define NB_MAX_MODIF_DUNE_COUPE_DE_PROBING 50 /*50*/

void PNE_CreerUneCoupeDeProbingSiLaVariableEntiereEstHorsContrainte( PROBLEME_PNE * , PROBING_OU_NODE_PRESOLVE * , int );
void PNE_AllocCoupesDeProbing( PROBLEME_PNE * );
void PNE_AugmenterNombreDeCoupesDeProbing( PROBLEME_PNE * );
void PNE_AugmenterLaTailleDesCoupesDeProbing( PROBLEME_PNE * );
void PNE_CoupesDeProbingCalculerMinMaxDUneContrainte( PROBLEME_PNE * , int , double * , double * , double * , double * , char * );
void PNE_CoupesDeProbingAjusterLaCorrection( PROBLEME_PNE * , double , int , double * , int , double * , double * , double * , double * , char * );
																			 
/*----------------------------------------------------------------------------*/

void PNE_AllocCoupesDeProbing( PROBLEME_PNE * Pne )
{
COUPES_DE_PROBING * CoupesDeProbing;

CoupesDeProbing = (COUPES_DE_PROBING *) malloc( sizeof( COUPES_DE_PROBING ) );
if ( CoupesDeProbing == NULL ) return;

CoupesDeProbing->SecondMembre = (double *) malloc( SIZE_ALLOC_COUPES * sizeof( double ) );
if ( CoupesDeProbing->SecondMembre == NULL ) {
  return;
}
CoupesDeProbing->NombreDeModificationsDeLaCoupeDeProbing = (int *) malloc( SIZE_ALLOC_COUPES * sizeof( int ) );
if ( CoupesDeProbing->NombreDeModificationsDeLaCoupeDeProbing == NULL ) {
  free( CoupesDeProbing->SecondMembre );
  return;
}
CoupesDeProbing->LaCoupDeProbingEstDansLePool = (char *) malloc( SIZE_ALLOC_COUPES * sizeof( char ) );
if ( CoupesDeProbing->LaCoupDeProbingEstDansLePool == NULL ) {
  free( CoupesDeProbing->SecondMembre );
  free( CoupesDeProbing->NombreDeModificationsDeLaCoupeDeProbing );
	return;
}
CoupesDeProbing->First = (int *) malloc( SIZE_ALLOC_COUPES * sizeof( int ) );
if ( CoupesDeProbing->First == NULL ) {
  free( CoupesDeProbing->SecondMembre );	
  free( CoupesDeProbing->NombreDeModificationsDeLaCoupeDeProbing );
	free( CoupesDeProbing->LaCoupDeProbingEstDansLePool ); 
  return;
}
CoupesDeProbing->NbElements = (int *) malloc( SIZE_ALLOC_COUPES * sizeof( int ) );
if ( CoupesDeProbing->NbElements == NULL ) {
  free( CoupesDeProbing->SecondMembre );	
  free( CoupesDeProbing->NombreDeModificationsDeLaCoupeDeProbing );
	free( CoupesDeProbing->LaCoupDeProbingEstDansLePool ); 
	free( CoupesDeProbing->First ); 
  return;
}
CoupesDeProbing->NombreDeCoupesDeProbingAlloue = SIZE_ALLOC_COUPES;

CoupesDeProbing->Colonne = (int *) malloc( SIZE_ALLOC_TERMES_COUPES * sizeof( int ) );
if ( CoupesDeProbing->Colonne == NULL ) {
  free( CoupesDeProbing->SecondMembre );	
  free( CoupesDeProbing->NombreDeModificationsDeLaCoupeDeProbing );
	free( CoupesDeProbing->LaCoupDeProbingEstDansLePool ); 
	free( CoupesDeProbing->First ); 
	free( CoupesDeProbing->NbElements ); 
  return;
}
CoupesDeProbing->Coefficient = (double *) malloc( SIZE_ALLOC_TERMES_COUPES * sizeof( double ) );
if ( CoupesDeProbing->Coefficient == NULL ) {
  free( CoupesDeProbing->SecondMembre );	
  free( CoupesDeProbing->NombreDeModificationsDeLaCoupeDeProbing );
	free( CoupesDeProbing->LaCoupDeProbingEstDansLePool ); 
	free( CoupesDeProbing->First ); 
	free( CoupesDeProbing->NbElements ); 
	free( CoupesDeProbing->Colonne ); 
  return;
}
CoupesDeProbing->TailleCoupesDeProbingAllouee = SIZE_ALLOC_TERMES_COUPES;

CoupesDeProbing->IndexLibre = 0;
CoupesDeProbing->NombreDeCoupesDeProbing = 0;
CoupesDeProbing->Full = NON_PNE;
Pne->CoupesDeProbing = CoupesDeProbing;

return;
}

/*----------------------------------------------------------------------------*/

void PNE_AugmenterNombreDeCoupesDeProbing( PROBLEME_PNE * Pne )
{
COUPES_DE_PROBING * CoupesDeProbing; int Size; double * SecondMembre; char * LaCoupDeProbingEstDansLePool;
int * First; int * NbElements; int * NombreDeModificationsDeLaCoupeDeProbing;

CoupesDeProbing = Pne->CoupesDeProbing;
Size = CoupesDeProbing->NombreDeCoupesDeProbingAlloue + SIZE_ALLOC_COUPES;

SecondMembre = (double *) realloc( CoupesDeProbing->SecondMembre, Size * sizeof( double ) );
if ( SecondMembre == NULL ) {
	CoupesDeProbing->Full = OUI_PNE;
  return;
}
NombreDeModificationsDeLaCoupeDeProbing = (int *) realloc( CoupesDeProbing->NombreDeModificationsDeLaCoupeDeProbing, Size * sizeof( int ) );
if ( NombreDeModificationsDeLaCoupeDeProbing == NULL ) {
  free( SecondMembre );
	CoupesDeProbing->Full = OUI_PNE;
  return;
}
LaCoupDeProbingEstDansLePool = (char *) realloc( CoupesDeProbing->LaCoupDeProbingEstDansLePool, Size * sizeof( char ) );
if ( LaCoupDeProbingEstDansLePool == NULL ) {
  free( SecondMembre );
  free( NombreDeModificationsDeLaCoupeDeProbing );
	CoupesDeProbing->Full = OUI_PNE;
	return;
}
First = (int *) realloc( CoupesDeProbing->First, Size * sizeof( int ) );
if ( First == NULL ) {
  free( SecondMembre );	
  free( NombreDeModificationsDeLaCoupeDeProbing );	
	free( LaCoupDeProbingEstDansLePool ); 
	CoupesDeProbing->Full = OUI_PNE;
  return;
}
NbElements = (int *) realloc( CoupesDeProbing->NbElements, Size * sizeof( int ) );
if ( NbElements == NULL ) {
  free( SecondMembre );	
  free( NombreDeModificationsDeLaCoupeDeProbing );	
	free( LaCoupDeProbingEstDansLePool ); 
	free( First ); 
	CoupesDeProbing->Full = OUI_PNE;
  return;
}
CoupesDeProbing->NombreDeCoupesDeProbingAlloue = Size;
CoupesDeProbing->SecondMembre = SecondMembre;
CoupesDeProbing->NombreDeModificationsDeLaCoupeDeProbing = NombreDeModificationsDeLaCoupeDeProbing;
CoupesDeProbing->LaCoupDeProbingEstDansLePool = LaCoupDeProbingEstDansLePool;
CoupesDeProbing->First = First;
CoupesDeProbing->NbElements = NbElements;

return;
}

/*----------------------------------------------------------------------------*/

void PNE_AugmenterLaTailleDesCoupesDeProbing( PROBLEME_PNE * Pne )
{
COUPES_DE_PROBING * CoupesDeProbing; int Size; int * Colonne; double * Coefficient;

CoupesDeProbing = Pne->CoupesDeProbing;
Size = CoupesDeProbing->TailleCoupesDeProbingAllouee + SIZE_ALLOC_TERMES_COUPES;

Colonne = (int *) realloc( CoupesDeProbing->Colonne, Size * sizeof( int ) );
if ( Colonne == NULL ) {
	CoupesDeProbing->Full = OUI_PNE;
  return;
}
Coefficient = (double *) realloc( CoupesDeProbing->Coefficient, Size * sizeof( double ) );
if ( Coefficient == NULL ) {
	free( Colonne ); 
	CoupesDeProbing->Full = OUI_PNE;
  return;
}
CoupesDeProbing->TailleCoupesDeProbingAllouee = Size;
CoupesDeProbing->Colonne = Colonne;
CoupesDeProbing->Coefficient = Coefficient;

return;
}

/*----------------------------------------------------------------------------*/

void PNE_CreerUneCoupeDeProbing( PROBLEME_PNE * Pne, int Var1, double CoeffDeVar1, int ContrainteSource,
                                 char TypeContrainteSource, int ContrainteNative, double ValeurDeVar1 )
{
COUPES_DE_PROBING * CoupesDeProbing; int il; int NombreDeCoupesDeProbing; double NouveauCoeff;
int Nb; int * First; int * NbElements; int * Colonne; int * Mdeb; int * NbTerm; int * Nuvar; 
double * Coefficient; double * A; int il1; int ilMax; int ilVar1; int NbTrm; double DeltaB;
double Sec; char ResultatExploitable;

CoupesDeProbing = Pne->CoupesDeProbing;
if ( CoupesDeProbing == NULL ) {
  PNE_AllocCoupesDeProbing( Pne );
  CoupesDeProbing = Pne->CoupesDeProbing;
  if ( CoupesDeProbing == NULL ) return; /* Saturation memoire */
}

NombreDeCoupesDeProbing = CoupesDeProbing->NombreDeCoupesDeProbing;
First = CoupesDeProbing->First;
NbElements = CoupesDeProbing->NbElements;
Coefficient = CoupesDeProbing->Coefficient;
Colonne = CoupesDeProbing->Colonne;

/* Place suffisante */
if ( NombreDeCoupesDeProbing >= CoupesDeProbing->NombreDeCoupesDeProbingAlloue ) {
  /* On augmente la taille */
	PNE_AugmenterNombreDeCoupesDeProbing( Pne );
	if ( Pne->CoupesDeProbing == NULL ) return;
	if ( CoupesDeProbing->Full == OUI_PNE ) return;
  First = CoupesDeProbing->First;
  NbElements = CoupesDeProbing->NbElements;	
}

if ( TypeContrainteSource == CONTRAINTE_NATIVE ) NbTerm = Pne->NbTermTrav;
else if ( TypeContrainteSource == COUPE_DE_PROBING ) NbTerm = CoupesDeProbing->NbElements; 
else return;

NbTrm = NbTerm[ContrainteSource];
/* Place suffisante */
il1 = CoupesDeProbing->IndexLibre;
while ( il1 + NbTrm + 1 >= CoupesDeProbing->TailleCoupesDeProbingAllouee ) {
  /* On augmente la taille */
  PNE_AugmenterLaTailleDesCoupesDeProbing( Pne );	
	if ( Pne->CoupesDeProbing == NULL ) return;	 
	if ( CoupesDeProbing->Full == OUI_PNE ) return;
  Coefficient = CoupesDeProbing->Coefficient;
  Colonne = CoupesDeProbing->Colonne;	
}

if ( TypeContrainteSource == CONTRAINTE_NATIVE ) {
  Mdeb = Pne->MdebTrav;
  Nuvar = Pne->NuvarTrav;
  A = Pne->ATrav;
	Sec = Pne->BTrav[ContrainteSource];
}
else {
  Mdeb = CoupesDeProbing->First; 
  Nuvar = CoupesDeProbing->Colonne; 
  A = CoupesDeProbing->Coefficient;
	Sec = CoupesDeProbing->SecondMembre[ContrainteSource];
}

First[NombreDeCoupesDeProbing] = il1;
Nb = 0;
il = Mdeb[ContrainteSource];
ilMax = il + NbTerm[ContrainteSource];
ilVar1 = -1;
while ( il < ilMax ) {
  if ( A[il] != 0 ) {
	  if ( Nuvar[il] == Var1 ) {
		  ilVar1 = il1;
		}
    Coefficient[il1] = A[il];
    Colonne[il1] = Nuvar[il];
	  il1++;
		Nb++;
	}
  il++;
}
NbElements[NombreDeCoupesDeProbing] = Nb;

CoupesDeProbing->SecondMembre[NombreDeCoupesDeProbing] = Sec;

if ( ilVar1 >= 0 ) {
  /* On modifie le coefficient de la variable */	
  if ( ValeurDeVar1 == 1 ) {
    /* La variable Var1 a ete instanciee a 1 dans le probing */
    NouveauCoeff = Coefficient[ilVar1] + CoeffDeVar1;		
    if ( fabs( NouveauCoeff ) < ZERO ) NouveauCoeff = 0;	 

    if ( NouveauCoeff != 0.0 ) {    	
      PNE_CoupesDeProbingAjusterLaCorrection( Pne, ValeurDeVar1, ilVar1, Coefficient, NombreDeCoupesDeProbing, CoupesDeProbing->SecondMembre,
																				  		&NouveauCoeff, &CoeffDeVar1, &DeltaB, &ResultatExploitable );																						 
      if ( ResultatExploitable == NON_PNE ) return;
		}
				
    /* NouveauCoeff ne peut pas etre nul a cause du test ci-dessus */
    Coefficient[ilVar1] = NouveauCoeff; 
  }
  else {
    /* La variable Var1 a ete instanciee a 0 dans le probing */
		NouveauCoeff = Coefficient[ilVar1] - CoeffDeVar1;		
		DeltaB = CoeffDeVar1;
    if ( fabs( NouveauCoeff ) < ZERO ) {
	    NouveauCoeff = 0;
	    CoeffDeVar1 = Coefficient[ilVar1];
      DeltaB = CoeffDeVar1;			
	  }
		
    if ( NouveauCoeff != 0.0 ) {    			
      PNE_CoupesDeProbingAjusterLaCorrection( Pne, ValeurDeVar1, ilVar1, Coefficient, NombreDeCoupesDeProbing, CoupesDeProbing->SecondMembre,
																						  &NouveauCoeff, &CoeffDeVar1, &DeltaB, &ResultatExploitable );						
      if ( ResultatExploitable == NON_PNE ) return;
		}
		
    /* NouveauCoeff ne peut pas etre nul a cause du test ci-dessus */
    Coefficient[ilVar1] = NouveauCoeff;
    CoupesDeProbing->SecondMembre[NombreDeCoupesDeProbing] -= DeltaB;

  }
}
else {
  /* On ajoute la variable */
  Colonne[il1] = Var1;
  if ( ValeurDeVar1 == 1 ) {
	/* La variable Var1 a ete instanciee a 1 dans le probing */	
    NouveauCoeff = CoeffDeVar1;
    if ( fabs( NouveauCoeff ) < ZERO ) return; /* Inutile d'ajouter un terme nul */
		
    PNE_CoupesDeProbingAjusterLaCorrection( Pne, ValeurDeVar1, ilVar1, Coefficient, NombreDeCoupesDeProbing, CoupesDeProbing->SecondMembre,
																					  &NouveauCoeff, &CoeffDeVar1, &DeltaB, &ResultatExploitable );																						 
    if ( ResultatExploitable == NON_PNE ) return;
		
	  Coefficient[il1] = NouveauCoeff; 
  }
  else {
    /* La variable Var1 a ete instanciee a 0 dans le probing */
    NouveauCoeff = -CoeffDeVar1;
		DeltaB = CoeffDeVar1;	
    if ( fabs( NouveauCoeff ) < ZERO ) return; /* Inutile d'ajouter un terme nul */	 

    PNE_CoupesDeProbingAjusterLaCorrection( Pne, ValeurDeVar1, ilVar1, Coefficient, NombreDeCoupesDeProbing, CoupesDeProbing->SecondMembre,
																						&NouveauCoeff, &CoeffDeVar1, &DeltaB, &ResultatExploitable );																						 
    if ( ResultatExploitable == NON_PNE ) return;
		
    Coefficient[il1] = NouveauCoeff;
    CoupesDeProbing->SecondMembre[NombreDeCoupesDeProbing] -= DeltaB;		
  }
  NbElements[NombreDeCoupesDeProbing]++;
  il1++;
}

# if TRACES == 1
  printf("Coupe de probing: %d\n",NombreDeCoupesDeProbing);
  il = First[NombreDeCoupesDeProbing];
  ilMax = il + NbElements[NombreDeCoupesDeProbing];
	while ( il < ilMax ) {
    if ( Pne->TypeDeVariableTrav[Colonne[il]] == ENTIER ) printf("%e (%d I) ",Coefficient[il],Colonne[il]);
		else printf("%e (%d R) ",Coefficient[il],Colonne[il]);		
    il++;
	}
	printf("  SecondMembre %e\n",CoupesDeProbing->SecondMembre[NombreDeCoupesDeProbing]);
# endif

/* Si la contrainte source est deja une coupe de probing on invalide l'ancienne coupe de probing */
if ( TypeContrainteSource == COUPE_DE_PROBING ) {
  /* Si la contrainte source est deja une coupe de probing on invalide l'ancienne coupe de probing */
  First[ContrainteSource] = -1;
	CoupesDeProbing->NombreDeModificationsDeLaCoupeDeProbing[NombreDeCoupesDeProbing] = CoupesDeProbing->NombreDeModificationsDeLaCoupeDeProbing[ContrainteSource] + 1;	
}
else CoupesDeProbing->NombreDeModificationsDeLaCoupeDeProbing[NombreDeCoupesDeProbing] = 0;

Pne->ProbingOuNodePresolve->NumeroDeCoupeDeProbing[ContrainteNative] = NombreDeCoupesDeProbing;

CoupesDeProbing->IndexLibre = il1;
CoupesDeProbing->LaCoupDeProbingEstDansLePool[NombreDeCoupesDeProbing] = NON_PNE;
CoupesDeProbing->NombreDeCoupesDeProbing++;

return;
}

/*----------------------------------------------------------------------------------------------------*/

void PNE_CoupesDeProbingAjusterLaCorrection( PROBLEME_PNE * Pne, double ValeurDeLaVariableInstanciee, int ilVarBin, double * A,
                                             int Cnt, double * B, double * NouveauCoeff, double * CorrectionCoefficient, double * DeltaB,
																						 char * ResultatExploitable )
{
double a; char Traces; double Seuil; double EcartAbsolu; double EcartRelatif; 
*ResultatExploitable = OUI_PNE;

a = 0;
if ( ilVarBin >= 0 ) a = A[ilVarBin];

Traces = NON_PNE;

/* CorrectionCoefficient doit toujours etre positif */

Seuil = Pne->PlusPetitTerme;
if ( fabs( *NouveauCoeff ) < Seuil ) {
  if ( Traces == OUI_PNE ) { 
    printf("NouveauCoeff %e hors limites ValeurDeLaVariableInstanciee %e PlusPetitTerme %e B[%d] = %e CorrectionCoefficient %e\n",
			      *NouveauCoeff,ValeurDeLaVariableInstanciee,Seuil,Cnt,B[Cnt],*CorrectionCoefficient);
    printf("                A[ilVarBin] = %e\n",a);
	}
  if ( a > 0 ) {
    /* On ramene a PlusPetitTerme */
    if ( ValeurDeLaVariableInstanciee == 1 ) *CorrectionCoefficient = -a + Seuil;	
		else *CorrectionCoefficient = a - Seuil;   		
	}
	else {
    /* On ramene a -PlusPetitTerme */
    if ( ValeurDeLaVariableInstanciee == 1 ) *CorrectionCoefficient = -a - Seuil;	
	  else *CorrectionCoefficient = a + Seuil;		
	}
	if ( *CorrectionCoefficient < 0 ) { if ( Traces == OUI_PNE ) printf("ResultatExploitable = NON_PNE\n"); *ResultatExploitable = NON_PNE; goto Fin; }
	
	if ( ValeurDeLaVariableInstanciee == 1 ) {		
		*NouveauCoeff = a + *CorrectionCoefficient;						
		if ( Traces == OUI_PNE ) printf("                NouveauCoeff = %e\n",*NouveauCoeff);		
	}
	else {
		*NouveauCoeff = a - *CorrectionCoefficient;	
    *DeltaB = *CorrectionCoefficient;
	  if ( Traces == OUI_PNE ) printf("                NouveauCoeff = %e  nouveau B = %e\n",*NouveauCoeff,B[Cnt]-*DeltaB);		
	}
	
}

Seuil = Pne->PlusGrandTerme;
if ( fabs( *NouveauCoeff ) > Seuil ) {
  if ( Traces == OUI_PNE ) { 
	  printf("NouveauCoeff %e hors limites ValeurDeLaVariableInstanciee %e PlusGrandTerme %e B[%d] = %e CorrectionCoefficient %e\n",
			      *NouveauCoeff,ValeurDeLaVariableInstanciee,Seuil,Cnt,B[Cnt],*CorrectionCoefficient);
    printf("                A[ilVarBin] = %e\n",a);     
	}
  if ( *NouveauCoeff > 0 ) {
    /* On ramene a PlusGrandTerme */	
    if ( ValeurDeLaVariableInstanciee == 1 ) {
      if ( a < 0 ) {		
        /* S'il y a changement de signe du coeff on attenue la valeur absolue du nouveau coeff */
		    *CorrectionCoefficient = -a + (0.5 * (Pne->PlusGrandTerme + Pne->PlusPetitTerme)); 				
      }
			else *CorrectionCoefficient = -a + Seuil;			
		}
	  else {
      if ( a < 0 ) {		
        /* S'il y a changement de signe du coeff on attenue la valeur absolue du nouveau coeff */
		    *CorrectionCoefficient = a - (0.5 * (Pne->PlusGrandTerme + Pne->PlusPetitTerme));
      }
			else *CorrectionCoefficient = a - Seuil;			
		}
  }
  else {
    /* On ramene a -PlusGrandTerme */	
    if ( ValeurDeLaVariableInstanciee == 1 ) {
      if ( a > 0 ) {
        /* S'il y a changement de signe du coeff on attenue la valeur absolue du nouveau coeff */
	      *CorrectionCoefficient = -a - (0.5 * (Pne->PlusGrandTerme + Pne->PlusPetitTerme));
      }
			else *CorrectionCoefficient = -a - Seuil;
		}
	  else {
      if ( a > 0 ) {
        /* S'il y a changement de signe du coeff on attenue la valeur absolue du nouveau coeff */
	      *CorrectionCoefficient = a + (0.5 * (Pne->PlusGrandTerme + Pne->PlusPetitTerme));
      }
      else *CorrectionCoefficient = a + Seuil;     	
		}
  }
	
 	if ( *CorrectionCoefficient < 0 ) { if ( Traces == OUI_PNE ) printf("ResultatExploitable = NON_PNE\n"); *ResultatExploitable = NON_PNE; goto Fin; }
	
	if ( ValeurDeLaVariableInstanciee == 1 ) {	
		*NouveauCoeff =  a + *CorrectionCoefficient;				
		if ( Traces == OUI_PNE ) printf("                NouveauCoeff = %e\n",*NouveauCoeff);		
	}
	else {	
		*NouveauCoeff = a - *CorrectionCoefficient;	
    *DeltaB = *CorrectionCoefficient;
		if ( Traces == OUI_PNE ) printf("                NouveauCoeff = %e  nouveau B = %e\n",*NouveauCoeff,B[Cnt]-*DeltaB);   		
	}
	
}
Fin:
if ( *ResultatExploitable == OUI_PNE ) {
  if ( *NouveauCoeff != 0 && a != 0 && *NouveauCoeff * a < 0 ) {
    if ( fabs( *NouveauCoeff ) > fabs( a ) ) {
		  if ( Traces == OUI_PNE ) printf("Refus car NouveauCoeff = %e et ancien coeff = %e PlusPetitTerme = %e PlusGrandTerme = %e\n",*NouveauCoeff,a,Pne->PlusPetitTerme,Pne->PlusGrandTerme);
		  *ResultatExploitable = NON_PNE;
		}
	}
  if ( *ResultatExploitable == OUI_PNE && a != 0 ) {
    EcartAbsolu = fabs( a - *NouveauCoeff );
	  if ( EcartAbsolu < ECART_ABSOLU_MIN ) *ResultatExploitable = NON_PNE;			
    EcartRelatif = fabs( EcartAbsolu / a );
		if ( EcartRelatif < ECART_RELATIF_MIN ) *ResultatExploitable = NON_PNE;
		if ( Traces == OUI_PNE ) printf("ResultatExploitable = NON_PNE\n");
  }	
}
return;
}

/*----------------------------------------------------------------------------------------------------*/
/* Si des contraintes son devenues non activables on cree les coupes de probing correspondantes */
void PNE_CreerLesCoupesDeProbing( PROBLEME_PNE * Pne, PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve )
{
int NombreDeContraintes; double * Bmax; double * B; double CorrectionCoefficient; int Cnt; double * BmaxSv;
char * BorneInfConnueSv; double * ValeurDeBorneInfSv; double * ValeurDeBorneSupSv; int il; int Var; 
int NbCntCoupesDeProbing; int ilMax; double SmaxSv; double Sec; double DeltaB; char ResultatExploitable;
double Smax; char BrnInfConnue; char * BorneInfConnue; double * ValeurDeBorneInf;
double * ValeurDeBorneSup; int i; int * NumCntCoupesDeProbing; char * FlagCntCoupesDeProbing;
int * Mdeb; int * NbTerm; int * Nuvar; double * A; double * Bmin; double * BminSv;
char LaVariableEstDansLaContrainte; int ilVarBin; double NouveauCoeff;
int icDeb; int VarCol; int icPrec; int Found; int icSui; int ic; double a;
int * Cdeb; int * Csui; int * NumContrainte; int * CNbTerm;

# if UTILISER_LA_MARGE_EN_FIN_DE_CONTRAINTE == OUI_PNE
  char OnAjouteUnTerme;
# endif
	
# if UTILISER_LES_COUPES_DE_PROBING == NON_PNE
  return;
# endif

if ( ProbingOuNodePresolve->VariableInstanciee < 0 ) return;

NombreDeContraintes = Pne->NombreDeContraintesTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
Nuvar = Pne->NuvarTrav;
A = Pne->ATrav;
B = Pne->BTrav;

Bmin = ProbingOuNodePresolve->Bmin;
BminSv = ProbingOuNodePresolve->BminSv;

Bmax = ProbingOuNodePresolve->Bmax;
BmaxSv = ProbingOuNodePresolve->BmaxSv;

BorneInfConnueSv = ProbingOuNodePresolve->BorneInfConnueSv;
ValeurDeBorneInfSv = ProbingOuNodePresolve->ValeurDeBorneInfSv;
ValeurDeBorneSupSv = ProbingOuNodePresolve->ValeurDeBorneSupSv;
BorneInfConnue = ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = ProbingOuNodePresolve->ValeurDeBorneInf;
ValeurDeBorneSup = ProbingOuNodePresolve->ValeurDeBorneSup;

NumCntCoupesDeProbing = ProbingOuNodePresolve->NumCntCoupesDeProbing;
FlagCntCoupesDeProbing = ProbingOuNodePresolve->FlagCntCoupesDeProbing;

NbCntCoupesDeProbing = ProbingOuNodePresolve->NbCntCoupesDeProbing;
ProbingOuNodePresolve->NbCntCoupesDeProbing = 0;

for ( i = 0 ; i < NbCntCoupesDeProbing ; i++ ) {

  Cnt = NumCntCoupesDeProbing[i];
	FlagCntCoupesDeProbing[Cnt] = 0;
	
  SmaxSv = BmaxSv[Cnt];
  Smax = Bmax[Cnt];	
	Sec = B[Cnt];
	
	if ( SmaxSv <= Sec + 1.e-7 ) continue;
	
	if ( Smax > Sec ) continue;

	/* Si la variable instanciee fait partie de la contrainte, on met tout de suite le coefficient a jour
	   puis on recalcule BminSv et BmaxSv */
  LaVariableEstDansLaContrainte = NON_PNE;

  il = Mdeb[Cnt];
  ilMax = il + NbTerm[Cnt];	
  # if MODIFIER_DIRECTEMENT_LA_MATRICE == OUI_PNE	
	  ilVarBin = -1;
	  while ( il < ilMax ) {
	    if ( Nuvar[il] == ProbingOuNodePresolve->VariableInstanciee ) {
		    ilVarBin = il;
			  LaVariableEstDansLaContrainte = OUI_PNE;
		  	break;
		  }
		  il++;
	  }		
	# endif
	
  # if UTILISER_LA_MARGE_EN_FIN_DE_CONTRAINTE == OUI_PNE
	  OnAjouteUnTerme = NON_PNE;
	# endif  
	
	if ( LaVariableEstDansLaContrainte == NON_PNE ) {
    # if UTILISER_LA_MARGE_EN_FIN_DE_CONTRAINTE == OUI_PNE
		  /* Attention il faut imperativement que les contraintes soient rangees dans l'ordre */
		  if ( Cnt < NombreDeContraintes - 1 ) {
		    if ( ilMax < Mdeb[Cnt+1] ) {				
				  /* Il reste de la place */
				  ilVarBin = ilMax;
					A[ilVarBin] = 0.0;
	        OnAjouteUnTerme = OUI_PNE;
				}
				else {				  
					/*printf("1- Plus de place dans la contrainte native %d ilMax %d Mdeb[Cnt+1] %d NbTerm[Cnt] %d\n",Cnt,ilMax,Mdeb[Cnt+1],NbTerm[Cnt]);*/									  
					goto CreationCoupeDeProbing;
				}
		  }
		  else {			
		    /* C'est la derniere contrainte */
		    if ( ilMax < Pne->TailleAlloueePourLaMatriceDesContraintes - 1 ) {				
				  printf("Ajout d'un terme dans la derniere contrainte\n");					
          /* Attention, dans ce cas on peut aller beaucoup plus loin que MARGE_EN_FIN_DE_CONTRAINTE */
				  ilVarBin = ilMax;
					A[ilVarBin] = 0.0;
	        OnAjouteUnTerme = OUI_PNE;
				}
				else {
				  printf("2- Plus de place dans la contrainte native\n");
				  goto CreationCoupeDeProbing;
				}
			}			
		  goto AjoutDUnTermeDansLaContrainte;					 
	  # endif
		
    /* On prepare une coupe de probing */
    # if UTILISER_LA_MARGE_EN_FIN_DE_CONTRAINTE == OUI_PNE
      CreationCoupeDeProbing:
		# endif
		
    PNE_CreerUneCoupeDeProbingSiLaVariableEntiereEstHorsContrainte( Pne, ProbingOuNodePresolve, Cnt );  
    continue;
  }
	
	/* Recalcul de Smax car le calcul qui est fait pendant le probing risque peu cumuler des erreur d'arrondi */
  # if UTILISER_LA_MARGE_EN_FIN_DE_CONTRAINTE == OUI_PNE
    AjoutDUnTermeDansLaContrainte:
	# endif	
  Smax = 0;
  il = Mdeb[Cnt];
	ilMax = il + NbTerm[Cnt];
	while ( il < ilMax ) {
    Var = Nuvar[il];		
    BrnInfConnue = BorneInfConnue[Var];
    if ( BorneInfConnue[Var] == FIXE_AU_DEPART || BorneInfConnue[Var] == FIXATION_SUR_BORNE_INF ||
	       BorneInfConnue[Var] == FIXATION_SUR_BORNE_SUP || BorneInfConnue[Var] == FIXATION_A_UNE_VALEUR ) Smax += A[il] * ValeurDeBorneInf[Var];							    
		else {
	    if ( A[il] > 0.0 ) Smax += A[il] * ValeurDeBorneSup[Var];			
	    else Smax += A[il] * ValeurDeBorneInf[Var];
		}			
	  il++;
	}
	if ( fabs( Bmax[Cnt] - Smax ) > 1.e-9 ) continue; 
	if ( Smax >= Sec - 0.1 ) continue; /* L'instanciation ne relaxe pas assez la contrainte */

  CorrectionCoefficient = Sec - Smax;			
  DeltaB = 0.0;
	
  /* On modifie le coefficient de la variable */	
  if ( Pne->ProbingOuNodePresolve->ValeurDeLaVariableInstanciee == 1 ) {
    /* La variable a ete instanciee a 1 dans le probing */		
    # if UTILISER_LA_MARGE_EN_FIN_DE_CONTRAINTE == OUI_PNE
	    if ( OnAjouteUnTerme == NON_PNE ) {			
        NouveauCoeff = A[ilVarBin] + CorrectionCoefficient;								
      }
	    else {
			  NouveauCoeff = CorrectionCoefficient;
			}
    # else
		  NouveauCoeff = A[ilVarBin] + CorrectionCoefficient;						
		# endif							   					
	}
  else {
    /* La variable Var1 a ete instanciee a 0 dans le probing */		
    # if UTILISER_LA_MARGE_EN_FIN_DE_CONTRAINTE == OUI_PNE
	    if ( OnAjouteUnTerme == NON_PNE ) {
        NouveauCoeff = A[ilVarBin] - CorrectionCoefficient;				
      }
	    else {
			  NouveauCoeff = -CorrectionCoefficient;								
			}
    # else
		  NouveauCoeff = A[ilVarBin] - CorrectionCoefficient;						
		# endif		
    DeltaB = CorrectionCoefficient;						 
  }

  if ( fabs( NouveauCoeff ) < ZERO ) {	
	  /*printf("NouveauCoeff inferieur a %e : %e -> 0  A[ilVarBin] %e\n",ZERO,NouveauCoeff,A[ilVarBin]);*/		
	  NouveauCoeff = 0;
    if ( Pne->ProbingOuNodePresolve->ValeurDeLaVariableInstanciee == 0 ) {
      # if UTILISER_LA_MARGE_EN_FIN_DE_CONTRAINTE == OUI_PNE		
	      if ( OnAjouteUnTerme == NON_PNE ) CorrectionCoefficient = A[ilVarBin];
        else CorrectionCoefficient = 0;
			# else
	      CorrectionCoefficient = A[ilVarBin];
			# endif
      DeltaB = CorrectionCoefficient;			
    }		
	}
	
  if ( NouveauCoeff != 0.0 ) {

    /* Modif 9/9/2016 */
    /*
		if ( fabs( NouveauCoeff ) < 0.1 ) {
      if ( fabs( NouveauCoeff ) < fabs( A[ilVarBin] ) ) continue;
	  }
		*/
	
    PNE_CoupesDeProbingAjusterLaCorrection( Pne, Pne->ProbingOuNodePresolve->ValeurDeLaVariableInstanciee, ilVarBin, A,
                                            Cnt, B, &NouveauCoeff, &CorrectionCoefficient, &DeltaB, &ResultatExploitable );																						
    if ( ResultatExploitable == NON_PNE ) continue;			
  }

Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
NumContrainte = Pne->NumContrainteTrav;
CNbTerm = Pne->CNbTermTrav;

  A[ilVarBin] = NouveauCoeff;
  # if UTILISER_LA_MARGE_EN_FIN_DE_CONTRAINTE == OUI_PNE
	  if ( OnAjouteUnTerme == OUI_PNE ) {
			Nuvar[ilVarBin] = ProbingOuNodePresolve->VariableInstanciee;
		  NbTerm[Cnt]++;
			
      /*Pne->ChainageTransposeeExploitable = NON_PNE;*/
			
			VarCol = Nuvar[ilVarBin];
			icDeb = Cdeb[VarCol];
			Cdeb[VarCol] = ilVarBin;
			Csui[ilVarBin] = icDeb;
      NumContrainte[ilVarBin] = Cnt;			
      CNbTerm[VarCol]++;			
			
		}
  # endif
		
  B[Cnt] -= DeltaB;		

  if ( NouveauCoeff == 0.0 ) {
    /* On enleve le terme */
    il = Mdeb[Cnt] + NbTerm[Cnt] - 1;
		a = A[ilVarBin];
    A[ilVarBin] = A[il];   
    Nuvar[ilVarBin] = Nuvar[il];    
    A[il] = 0;
    Nuvar[il] = 0;    
    NbTerm[Cnt]--;
    if ( NbTerm[Cnt] == 1 && Pne->TypeDeVariableTrav[Nuvar[Mdeb[Cnt]]] == ENTIER ) {    
      /* On flag la variable pour qu'elle soit testee */
      ProbingOuNodePresolve->FlagVarAInstancier[Nuvar[Mdeb[Cnt]]] = 1;	     
    }		

		 Pne->ChainageTransposeeExploitable = NON_PNE;
		 
# if ABZERTAAAA == 1 /* Inihibition car je ne sais pas pourquoi mais il y a des JDD ou c'est plus lent */

    /* On modifie le chainage par colonne */
		
    /* On enleve le terme ilVarBin du chainage par colonne de ProbingOuNodePresolve->VariableInstanciee */
		VarCol = ProbingOuNodePresolve->VariableInstanciee;
		Found = 0;
		icPrec = -1;
    ic = Cdeb[VarCol];
		while ( ic >= 0 ) {
      if ( ic == ilVarBin ) {
			  Found = 1;
			  break;
			}
			icPrec = ic;
			ic = Csui[ic];
		}
		if ( Found == 0 && a != 0.0 ) {
		  printf("1-a- BEUGUUEUEUE dans PNE_CreerLesCoupesDeProbing !!!!!\n");
			exit(0);
		}
		if ( Found == 1 ) {
		  if ( icPrec >= 0 ) Csui[icPrec] = Csui[ic];		
		  else {
			  if ( ic < 0 ) {
		      printf("1-b- BEUGUUEUEUE dans PNE_CreerLesCoupesDeProbing !!!!!\n");
			    exit(0);
				}
			  Cdeb[VarCol] = Csui[ic];		
		  }
      CNbTerm[VarCol]--;
		}
		
    /* On enleve le terme ilVarBin du chainage par colonne de Nuvar[ilVarBin] */		
		VarCol = Nuvar[ilVarBin];
		a = A[ilVarBin];
		Found = 0;
		icPrec = -1;
    ic = Cdeb[VarCol];
		while ( ic >= 0 ) {
      if ( ic == il ) {
			  Found = 1;
			  break;
			}
			icPrec = ic;
			ic = Csui[ic];
		}
		if ( Found == 0 && a != 0.0 ) {
		  printf("2- BEUGUUEUEUE dans PNE_CreerLesCoupesDeProbing !!!!!\n");
			exit(0);
		}		
		if ( Found == 1 ) {
      icSui = Csui[ic];
		  if ( icPrec >= 0 ) Csui[icPrec] = ilVarBin;		
		  else Cdeb[VarCol] = ilVarBin;		
		  Csui[ilVarBin] = icSui;			
      NumContrainte[ilVarBin] = Cnt;
		}
		else {		  
			icDeb = Cdeb[VarCol];
			Cdeb[VarCol] = ilVarBin;
			Csui[ilVarBin] = icDeb;
      NumContrainte[ilVarBin] = Cnt;			
      CNbTerm[VarCol]++;			
		}
		
# endif
		
  }

	/* On recalcule BminSv et BmaxSv */
  
  PNE_CoupesDeProbingCalculerMinMaxDUneContrainte( Pne, Cnt, BminSv, BmaxSv, ValeurDeBorneInfSv,
                                                   ValeurDeBorneSupSv, BorneInfConnueSv );
   
  if ( ProbingOuNodePresolve->NbFoisContrainteModifiee[Cnt] == 0 ) {
    ProbingOuNodePresolve->NumeroDeContrainteModifiee[ProbingOuNodePresolve->NbContraintesModifiees] = Cnt;
	  ProbingOuNodePresolve->NbContraintesModifiees++;
  }

}

if ( Pne->ChainageTransposeeExploitable == NON_PNE ) {
  PNE_ConstruireLeChainageDeLaTransposee( Pne );
}

/*
if ( Pne->CoupesDeProbing != NULL ) printf("CoupesDeProbing->NombreDeCoupesDeProbing %d\n",Pne->CoupesDeProbing->NombreDeCoupesDeProbing);
*/

return;
}

/***************************************************************************************/

void PNE_CreerUneCoupeDeProbingSiLaVariableEntiereEstHorsContrainte( PROBLEME_PNE * Pne, PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve,
                                                                     int Cnt )
{
int NombreDeContraintes; double * Bmax; double * B; double CorrectionCoefficient; double * BmaxSv;
char * BorneInfConnueSv; double NouveauSmax; double * ValeurDeBorneInfSv; double * ValeurDeBorneSupSv;
int il; int Var; int ilMax; double SmaxSv; double Sec; COUPES_DE_PROBING * CoupesDeProbing; int * NumeroDeCoupeDeProbing; 
double Smax; char * BorneInfConnue; double * ValeurDeBorneInf; double * Coefficient; int NumCoupe;
int * First; int * NbElements; int * Colonne; double * SecondMembre; int NumContrainteSource;
double * ValeurDeBorneSup; int * NumCntCoupesDeProbing; char * FlagCntCoupesDeProbing;
int * Mdeb; int * NbTerm; int * Nuvar; double * A; double * Bmin; double * BminSv; char TypeContrainteSource;

/*
printf("************************* Attention PNE_CreerUneCoupeDeProbingSiLaVariableEntiereEstHorsContrainte desactive\n");
return;
*/

CoupesDeProbing = Pne->CoupesDeProbing;

NombreDeContraintes = Pne->NombreDeContraintesTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
Nuvar = Pne->NuvarTrav;
A = Pne->ATrav;
B = Pne->BTrav;

Bmin = ProbingOuNodePresolve->Bmin;
BminSv = ProbingOuNodePresolve->BminSv;
NumeroDeCoupeDeProbing = ProbingOuNodePresolve->NumeroDeCoupeDeProbing;

Bmax = ProbingOuNodePresolve->Bmax;
BmaxSv = ProbingOuNodePresolve->BmaxSv;

BorneInfConnueSv = ProbingOuNodePresolve->BorneInfConnueSv;
ValeurDeBorneInfSv = ProbingOuNodePresolve->ValeurDeBorneInfSv;
ValeurDeBorneSupSv = ProbingOuNodePresolve->ValeurDeBorneSupSv;
BorneInfConnue = ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = ProbingOuNodePresolve->ValeurDeBorneInf;
ValeurDeBorneSup = ProbingOuNodePresolve->ValeurDeBorneSup;

NumCntCoupesDeProbing = ProbingOuNodePresolve->NumCntCoupesDeProbing;
FlagCntCoupesDeProbing = ProbingOuNodePresolve->FlagCntCoupesDeProbing;

SmaxSv = BmaxSv[Cnt];
Smax = Bmax[Cnt];	
Sec = B[Cnt];	
TypeContrainteSource = CONTRAINTE_NATIVE;
NumContrainteSource = Cnt;

/* Si la contrainte est deja reliee a une coupe de probing on regarde si elle est violee puis relaxee */
if ( NumeroDeCoupeDeProbing[Cnt] >= 0 && CoupesDeProbing != NULL ) {
  /* Car a pu etre augmente */		
  First = CoupesDeProbing->First;
  NbElements = CoupesDeProbing->NbElements;
  SecondMembre = CoupesDeProbing->SecondMembre;
  Colonne = CoupesDeProbing->Colonne;
  Coefficient = CoupesDeProbing->Coefficient;	
	NumCoupe = NumeroDeCoupeDeProbing[Cnt];		
  if ( CoupesDeProbing->NombreDeModificationsDeLaCoupeDeProbing[NumCoupe] >= NB_MAX_MODIF_DUNE_COUPE_DE_PROBING ) {
	  /*printf("Coupe de probing modifiee trop de fois\n");*/
	  return;
	}	
	TypeContrainteSource = COUPE_DE_PROBING;
	NumContrainteSource = NumCoupe;
  il = First[NumCoupe];
	if ( il < 0 ) goto TestSeuil;
	ilMax = il + NbElements[NumCoupe];
	SmaxSv = 0;
  Smax = 0;
	while ( il < ilMax ) {
    Var = Colonne[il];		
    if ( BorneInfConnueSv[Var] == FIXE_AU_DEPART ) SmaxSv += Coefficient[il] * ValeurDeBorneInfSv[Var];							    
		else {
	    if ( Coefficient[il] > 0.0 ) SmaxSv += Coefficient[il] * ValeurDeBorneSupSv[Var];			
	    else SmaxSv += Coefficient[il] * ValeurDeBorneInfSv[Var];
		}
    if ( BorneInfConnue[Var] == FIXE_AU_DEPART || BorneInfConnue[Var] == FIXATION_SUR_BORNE_INF ||
	       BorneInfConnue[Var] == FIXATION_SUR_BORNE_SUP ||BorneInfConnue[Var] == FIXATION_A_UNE_VALEUR ) {
	    Smax += Coefficient[il] * ValeurDeBorneInf[Var];						
	  }
		else {
	    if ( Coefficient[il] > 0.0 ) Smax += Coefficient[il] * ValeurDeBorneSup[Var];			
	    else Smax += Coefficient[il] * ValeurDeBorneInf[Var];
		}			
		il++;
	}
	Sec = SecondMembre[NumCoupe];
	/* Si la coupe de probing n'est plus violee, on la supprime */
	if ( SmaxSv <= Sec ) {
    First[NumCoupe] = -1;
    NumeroDeCoupeDeProbing[Cnt] = -1;
    SmaxSv = BmaxSv[Cnt];
	}	
}
TestSeuil:
if ( SmaxSv <= Sec ) return;	
if ( Smax >= Sec ) return;

/* Recalcul de Smax car le calcul qui est fait pendant le probing risque peu cumuler des erreur d'arrondi */	
if ( CoupesDeProbing != NULL ) {
  if ( NumeroDeCoupeDeProbing[Cnt] < 0 ) {	
    NouveauSmax = 0;
    il = Mdeb[Cnt];
	  ilMax = il + NbTerm[Cnt];
	  while ( il < ilMax ) {
      Var = Nuvar[il];		
      if ( BorneInfConnue[Var] == FIXE_AU_DEPART || BorneInfConnue[Var] == FIXATION_SUR_BORNE_INF ||
	         BorneInfConnue[Var] == FIXATION_SUR_BORNE_SUP || BorneInfConnue[Var] == FIXATION_A_UNE_VALEUR ) NouveauSmax += A[il] * ValeurDeBorneInf[Var];							    
		  else {
	      if ( A[il] > 0.0 ) NouveauSmax += A[il] * ValeurDeBorneSup[Var];			
	      else NouveauSmax += A[il] * ValeurDeBorneInf[Var];
		  }			
	    il++;
	  }
		if ( fabs( NouveauSmax - Smax ) > 1.e-10 /*1.e-12*/ ) {
		  /* Trop d'ecart */
		  return;
		}
	  Smax = NouveauSmax;
	}
}

/* Si le coeff est entier on prend une marge nulle */
CorrectionCoefficient = Sec - Smax;

if ( fabs(CorrectionCoefficient - floor( CorrectionCoefficient )) <= 1.e-10 ) CorrectionCoefficient = floor( CorrectionCoefficient );
else if ( fabs(ceil( CorrectionCoefficient ) - CorrectionCoefficient) <= 1.e-10 ) CorrectionCoefficient = ceil( CorrectionCoefficient );
 
PNE_CreerUneCoupeDeProbing( Pne, ProbingOuNodePresolve->VariableInstanciee, CorrectionCoefficient, NumContrainteSource,
	                          TypeContrainteSource, Cnt, Pne->ProbingOuNodePresolve->ValeurDeLaVariableInstanciee );	  
  
return;
}
/***************************************************************************************/
/* Attention ca ne marche que pour les valeurs de sauvegarde car le test sur BorneInfConnue ne porte 
   que sur la valeur FIXE_AU_DEPART */
void PNE_CoupesDeProbingCalculerMinMaxDUneContrainte( PROBLEME_PNE * Pne, int Cnt, double * Bmin, double * Bmax,
                                                      double * Xmin, double * Xmax, char * BorneInfConnue )
{
int il; int Var; int ilMax; double Smax; int * Mdeb; int * NbTerm; int * Nuvar; double * A; 
double Smin; 
 	
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
Nuvar = Pne->NuvarTrav;
A = Pne->ATrav;
  
Smin = 0;
Smax = 0;
il = Mdeb[Cnt];  
ilMax = il + NbTerm[Cnt];
while ( il < ilMax ) {
  Var = Nuvar[il];
  if ( BorneInfConnue[Var] == FIXE_AU_DEPART ) {
		Smin += A[il] * Xmin[Var];
		Smax += A[il] * Xmin[Var];
  }
	else {
	  if ( A[il] > 0.0 ) {
			Smin += A[il] * Xmin[Var];
			Smax += A[il] * Xmax[Var];
		}
	  else {			
			Smin += A[il] * Xmax[Var];
			Smax += A[il] * Xmin[Var];
		}
	}			
	il++;
}
  
Bmin[Cnt] = Smin;
Bmax[Cnt] = Smax;
  
return;
}



