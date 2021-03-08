/***********************************************************************

   FONCTION: Empilement des couts reduits des variables binaires hors base
	           pour en deduite des depassements potentiels de couts.
                
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

# define DEPASSEMENT 0.1 /*1.e-3*/

# define INCREMENT_NOMBRE_DE_COUPES 100
# define INCREMENT_TAILLE_COUPES    10000
# define MAX_ELEMENTS_COUPES 1000000
# define MAX_COUPES_GENEREES_A_CHAQUE_APPEL 1000 /* Mettre 5 si on l'appelle aussi a tous les reduced cost fixing */

# if UTILISER_LES_COUPES_DE_COUTS_REDUITS == OUI_PNE

void PNE_ClasserLesCoutsReduits( int , int * , int * , double * );

void PNE_GenererUneCoupeDeCoutsReduits( PROBLEME_PNE * , int , int , int * , int * , double * , double * , int * );

void PNE_ConstruireUneCoupeDeCoutsReduits( PROBLEME_PNE * , double * , int * , double , int );

int PNE_LaCoupeADejaEteGeneree( PROBLEME_PNE * , double * , int * , double , int );
														
/*----------------------------------------------------------------------------*/

void PNE_EmpilementDesCoutsReduitsDesVariablesBinairesNonFixees( PROBLEME_PNE * Pne, 
																				 double * CoutsReduits, double * Xmin, double * Xmax,
																				 int * PositionDeLaVariable, double Critere )
{
int NombreDeVariables; int NombreDeVariablesNonFixes; int * NumeroDesVariablesNonFixes;
int * TypeDeBorne; int * TypeDeVariable; int i; int Var; double SommeCoutsReduits;
int N; int First; int * Next; int VarDeb; int NbCoupesGenerees;

if ( Pne->CoupesDeCoutsReduits != NULL ) {
  if ( Pne->CoupesDeCoutsReduits->TailleCoupesDeCoutsReduitsAllouee > MAX_ELEMENTS_COUPES ) return;
}

NombreDeVariables = Pne->NombreDeVariablesTrav;
NombreDeVariablesNonFixes = Pne->NombreDeVariablesNonFixes;
NumeroDesVariablesNonFixes = Pne->NumeroDesVariablesNonFixes;

TypeDeBorne = Pne->TypeDeBorneTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;

Next = (int *) malloc( NombreDeVariables * sizeof( int ) );
if ( Next == NULL ) {
  return;
}
First = -1;

SommeCoutsReduits = 0;
for( i = 0 ; i < NombreDeVariablesNonFixes ; i++ ) {
  Var = NumeroDesVariablesNonFixes[i];	
  if ( TypeDeVariable[Var] != ENTIER ) continue;	
  if ( TypeDeBorne[Var] == VARIABLE_FIXE ) continue;	
  if ( Xmax[Var] == Xmin[Var] ) continue;
	if ( fabs( CoutsReduits[Var] ) < 1.e-8 ) continue;
	if ( PositionDeLaVariable[Var] != HORS_BASE_SUR_BORNE_INF && PositionDeLaVariable[Var] != HORS_BASE_SUR_BORNE_SUP ) continue;

  SommeCoutsReduits += fabs( CoutsReduits[Var] * ( Xmax[Var] - Xmin[Var] ) );
  PNE_ClasserLesCoutsReduits( Var, &First, Next, CoutsReduits );
	
}

# if TRACES == 1
  printf("SommeCoutsReduits %e\n",SommeCoutsReduits);
# endif

if ( Critere+SommeCoutsReduits < Pne->CoutOpt + DEPASSEMENT ) goto Fin;

NbCoupesGenerees = 0;
Var = First;
VarDeb = Var;
SommeCoutsReduits = 0;
N = 0;
while ( Var >= 0 ) {

  /*printf("CoutsReduits[%d] = %e\n",Var,fabs( CoutsReduits[Var] ));*/
	
  SommeCoutsReduits += fabs( CoutsReduits[Var] * ( Xmax[Var] - Xmin[Var] ) );
	N++;

	if ( Critere+SommeCoutsReduits > Pne->CoutOpt + DEPASSEMENT ) {
	  # if TRACES == 1
      printf("Generation CoutMax %e Pne->CoutOpt %e et N = %d\n",Critere+SommeCoutsReduits,Pne->CoutOpt,N);
		  printf("VarDeb %d VariableAEviter %d\n",VarDeb,Next[Var]);
		# endif
    PNE_GenererUneCoupeDeCoutsReduits( Pne, VarDeb, Next[Var], Next, PositionDeLaVariable, Xmin, Xmax, &NbCoupesGenerees );

		if ( NbCoupesGenerees >= MAX_COUPES_GENEREES_A_CHAQUE_APPEL ) break;
		
    if ( Pne->CoupesDeCoutsReduits != NULL ) {
      if ( Pne->CoupesDeCoutsReduits->TailleCoupesDeCoutsReduitsAllouee > MAX_ELEMENTS_COUPES ) break;
    }
		
		Var = Next[VarDeb];
    VarDeb = Var;
    SommeCoutsReduits = 0;
    N = 0;		
	}
	else {
    Var = Next[Var];
	}
}

Fin:

free( Next );

return;
}

/*----------------------------------------------------------------------------*/

void PNE_GenererUneCoupeDeCoutsReduits( PROBLEME_PNE * Pne, int PremiereVariable, int VariableAEviter,
                                        int * Next, int * PositionDeLaVariable, double * Xmin, double * Xmax,
																				int * NbCoupesGenerees )
{
double * Coeff; int * Indice; int NombreDeTermes; double SecondMembre; int Var; int NombreDeVariablesEntieres;
int * NumerosDesVariablesEntieres; int i; double * XminSv; double * XmaxSv; int * TypeDeVariable;
int * TypeDeBorne; double C; int j; char OnReboucle;

Coeff = Pne->Coefficient_CG;
Indice = Pne->IndiceDeLaVariable_CG;
NombreDeTermes = 0;

Var = PremiereVariable;
SecondMembre = 0.;
while ( Var != VariableAEviter ) {
  if ( PositionDeLaVariable[Var] == HORS_BASE_SUR_BORNE_INF ) {
    /* Interdiction de passer sur la borne sup */
    Coeff[NombreDeTermes] = 1;		
	}
	else if ( PositionDeLaVariable[Var] == HORS_BASE_SUR_BORNE_SUP ) {
    /* Interdiction de passer sur la borne inf */
    Coeff[NombreDeTermes] = -1;
    SecondMembre -= 1.;
	}
	else {
	  printf("BUG dans PNE_GenererUneCoupeDeCoutsReduits PositionDeLaVariable non reconnue\n");
	  return;
	}
  Indice[NombreDeTermes] = Var;
  NombreDeTermes++;
  Var = Next[Var];
}

/* Pour rendre la coupe valide dans tout l'arbre. Rq: les variables telles que Xmin = Xmax ne peuvent pas
   deja etre dans la coupe */
NombreDeVariablesEntieres = Pne->NombreDeVariablesEntieresTrav;
NumerosDesVariablesEntieres = Pne->NumerosDesVariablesEntieresTrav;
XminSv = Pne->UminTravSv;
XmaxSv = Pne->UmaxTravSv;
TypeDeBorne = Pne->TypeDeBorneTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
C = NombreDeTermes;

for ( i = 0 ; i < NombreDeVariablesEntieres ; i++ ) {
  Var = NumerosDesVariablesEntieres[i];
  if ( TypeDeVariable[Var] != ENTIER ) continue; /* Par securite */	
  if ( TypeDeBorne[Var] == VARIABLE_FIXE ) continue; /* Par securite */	
  if ( XmaxSv[Var] == XminSv[Var] ) continue;	
  if ( Xmax[Var] == Xmin[Var] ) {	
    if ( Xmin[Var] == XmaxSv[Var] ) {
      /* La variable a ete instanciee a 1 donc la coupe doit etre inhibee si la variable est instancie a 0 */
      Coeff[NombreDeTermes] = C;		
      Indice[NombreDeTermes] = Var;
			SecondMembre += C;
      NombreDeTermes++;			
		}
		else if ( Xmax[Var] == XminSv[Var] ) {
      /* La variable a ete instanciee a 0 donc la coupe doit etre inhibee si la variable est instancie a 1 */
      Coeff[NombreDeTermes] = -C;		
      Indice[NombreDeTermes] = Var;
      NombreDeTermes++;			
		}		
	}
}

SecondMembre += NombreDeTermes - 1;

/* On classe les elements dans l'ordre croissant des variables afin de tester plus facilement l'existence
   de coupes identiques */
OnReboucle = OUI_PNE;
while ( OnReboucle == OUI_PNE ) {
  OnReboucle = NON_PNE;
	for ( i = 0 ; i < NombreDeTermes - 1 ; i++ ) {
	  if ( Indice[i+1] < Indice[i] ) {
      OnReboucle = OUI_PNE;
      j = Indice[i];
			Indice[i] = Indice[i+1];
			Indice[i+1] = j;
			C = Coeff[i];
			Coeff[i] = Coeff[i+1];
			Coeff[i+1] = C;
		}
	}
}

/* Controler la presence d'une coupe identique */
if ( PNE_LaCoupeADejaEteGeneree( Pne, Coeff, Indice, SecondMembre, NombreDeTermes ) == OUI_PNE ) {
  return;
}

/* Archivage de la coupe */
# if TRACES == 1
  printf("NombreDeTermes de la coupe %d  SecondMembre %e\n",NombreDeTermes,SecondMembre);
# endif

PNE_ConstruireUneCoupeDeCoutsReduits( Pne, Coeff, Indice, SecondMembre, NombreDeTermes );
*NbCoupesGenerees = *NbCoupesGenerees + 1;

return;
}

/*----------------------------------------------------------------------------*/

int PNE_LaCoupeADejaEteGeneree( PROBLEME_PNE * Pne, double * Coeff, int * Indice, double SecMembre, int NombreDeTermes )
{
int c; COUPES_DE_COUTS_REDUITS * CoupesDeCoutsReduits; int i; int iMx; char CoupeIdentique;
int j; int * NbElements; double * SecondMembre; int * First; int * IndiceDeLaVariable;
double * Coefficient;

if ( Pne->CoupesDeCoutsReduits == NULL ) return( NON_PNE );

CoupesDeCoutsReduits = Pne->CoupesDeCoutsReduits;
NbElements = CoupesDeCoutsReduits->NbElements;
SecondMembre = CoupesDeCoutsReduits->SecondMembre;
First = CoupesDeCoutsReduits->First;
IndiceDeLaVariable = CoupesDeCoutsReduits->IndiceDeLaVariable;
Coefficient = CoupesDeCoutsReduits->Coefficient;

for ( c = 0 ; c < CoupesDeCoutsReduits->NombreDeCoupes ; c++ ) {
  if ( NbElements[c] != NombreDeTermes ) continue;
  if ( SecondMembre[c] != SecMembre ) continue;
  i = First[c];
	iMx = i + NbElements[c];
	j = 0;
	CoupeIdentique = OUI_PNE;
	while ( i < iMx ) {
    if ( IndiceDeLaVariable[i] != Indice[j] ) {
	    CoupeIdentique = NON_PNE;
			break;
		}
    if ( Coefficient[i] != Coeff[j] ) {
	    CoupeIdentique = NON_PNE;
			break;
		}
	  i++;
		j++;
	}
	if ( CoupeIdentique == OUI_PNE ) return( OUI_PNE );	
}

return( NON_PNE );
}

/*----------------------------------------------------------------------------*/

void PNE_ConstruireUneCoupeDeCoutsReduits( PROBLEME_PNE * Pne, double * Coeff, int * Indice, double SecondMembre, int NombreDeTermes )
{
int c; int Index; COUPES_DE_COUTS_REDUITS * CoupesDeCoutsReduits; int i; int j;

if ( Pne->CoupesDeCoutsReduits == NULL ) {
  Pne->CoupesDeCoutsReduits = (COUPES_DE_COUTS_REDUITS *) malloc( sizeof( COUPES_DE_COUTS_REDUITS ) );
	if ( Pne->CoupesDeCoutsReduits == NULL ) return;
  CoupesDeCoutsReduits = Pne->CoupesDeCoutsReduits;
	CoupesDeCoutsReduits->NombreDeCoupes = 0;
	CoupesDeCoutsReduits->NbCoupesDeCoutsReduitsAllouees = INCREMENT_NOMBRE_DE_COUPES;
	i = CoupesDeCoutsReduits->NbCoupesDeCoutsReduitsAllouees;
  CoupesDeCoutsReduits->First = (int *) malloc( i * sizeof( int ) );
  CoupesDeCoutsReduits->NbElements = (int *) malloc( i * sizeof( int ) );
  CoupesDeCoutsReduits->SecondMembre = (double *) malloc( i * sizeof( double ) );
  CoupesDeCoutsReduits->LaCoupeEstDansLePool = (char *) malloc( i * sizeof( char ) );
  if ( CoupesDeCoutsReduits->First == NULL || CoupesDeCoutsReduits->NbElements == NULL || CoupesDeCoutsReduits->SecondMembre == NULL ||
    CoupesDeCoutsReduits->LaCoupeEstDansLePool == NULL ) {
    free( CoupesDeCoutsReduits->First ); free( CoupesDeCoutsReduits->NbElements );
	  free( CoupesDeCoutsReduits->SecondMembre ); free( CoupesDeCoutsReduits->LaCoupeEstDansLePool );
    free( Pne->CoupesDeCoutsReduits );
		Pne->CoupesDeCoutsReduits = NULL;
		return;
  }
  CoupesDeCoutsReduits->TailleCoupesDeCoutsReduitsAllouee = INCREMENT_TAILLE_COUPES;
	i = CoupesDeCoutsReduits->TailleCoupesDeCoutsReduitsAllouee;
  CoupesDeCoutsReduits->Coefficient = (double *) malloc( i * sizeof( double ) );
  CoupesDeCoutsReduits->IndiceDeLaVariable = (int *) malloc( i * sizeof( int ) );
  if ( CoupesDeCoutsReduits->Coefficient == NULL || CoupesDeCoutsReduits->IndiceDeLaVariable == NULL ) {
    free( CoupesDeCoutsReduits->First ); free( CoupesDeCoutsReduits->NbElements );
	  free( CoupesDeCoutsReduits->SecondMembre ); free( CoupesDeCoutsReduits->LaCoupeEstDansLePool );
	  free( CoupesDeCoutsReduits->Coefficient ); free( CoupesDeCoutsReduits->IndiceDeLaVariable );		
    free( Pne->CoupesDeCoutsReduits );
		Pne->CoupesDeCoutsReduits = NULL;
		return;
  } 		
}

CoupesDeCoutsReduits = Pne->CoupesDeCoutsReduits;

c = CoupesDeCoutsReduits->NombreDeCoupes - 1;
if ( c >= 0 ) Index = CoupesDeCoutsReduits->First[c] + CoupesDeCoutsReduits->NbElements[c];
else Index = 0;
c++;

if ( c == CoupesDeCoutsReduits->NbCoupesDeCoutsReduitsAllouees ) {
  /* Il faut reallouer */	
	CoupesDeCoutsReduits->NbCoupesDeCoutsReduitsAllouees += INCREMENT_NOMBRE_DE_COUPES;
	i = CoupesDeCoutsReduits->NbCoupesDeCoutsReduitsAllouees;
  CoupesDeCoutsReduits->First = (int *) realloc( CoupesDeCoutsReduits->First, i * sizeof( int ) );
  CoupesDeCoutsReduits->NbElements = (int *) realloc( CoupesDeCoutsReduits->NbElements, i * sizeof( int ) );
  CoupesDeCoutsReduits->SecondMembre = (double *) realloc( CoupesDeCoutsReduits->SecondMembre, i * sizeof( double ) );
  CoupesDeCoutsReduits->LaCoupeEstDansLePool = (char *) realloc( CoupesDeCoutsReduits->LaCoupeEstDansLePool, i * sizeof( char ) );
  if ( CoupesDeCoutsReduits->First == NULL || CoupesDeCoutsReduits->NbElements == NULL || CoupesDeCoutsReduits->SecondMembre == NULL ||
    CoupesDeCoutsReduits->LaCoupeEstDansLePool == NULL ) {
    free( CoupesDeCoutsReduits->First ); free( CoupesDeCoutsReduits->NbElements );
	  free( CoupesDeCoutsReduits->SecondMembre ); free( CoupesDeCoutsReduits->LaCoupeEstDansLePool );
    free( Pne->CoupesDeCoutsReduits );
		Pne->CoupesDeCoutsReduits = NULL;
		return;
  }		
}

CoupesDeCoutsReduits->First[c] = Index;

for ( j= 0 ; j < NombreDeTermes ; j++ ) {
  if ( Index == CoupesDeCoutsReduits->TailleCoupesDeCoutsReduitsAllouee ) {	
    /* Il faut reallouer */
    CoupesDeCoutsReduits->TailleCoupesDeCoutsReduitsAllouee += INCREMENT_TAILLE_COUPES;
	  i = CoupesDeCoutsReduits->TailleCoupesDeCoutsReduitsAllouee;
    CoupesDeCoutsReduits->Coefficient = (double *) realloc( CoupesDeCoutsReduits->Coefficient, i * sizeof( double ) );
    CoupesDeCoutsReduits->IndiceDeLaVariable = (int *) realloc( CoupesDeCoutsReduits->IndiceDeLaVariable, i * sizeof( int ) );
    if ( CoupesDeCoutsReduits->Coefficient == NULL || CoupesDeCoutsReduits->IndiceDeLaVariable == NULL ) {
      free( CoupesDeCoutsReduits->First ); free( CoupesDeCoutsReduits->NbElements );
	    free( CoupesDeCoutsReduits->SecondMembre ); free( CoupesDeCoutsReduits->LaCoupeEstDansLePool );
	    free( CoupesDeCoutsReduits->Coefficient ); free( CoupesDeCoutsReduits->IndiceDeLaVariable );		
      free( Pne->CoupesDeCoutsReduits );
		  Pne->CoupesDeCoutsReduits = NULL;
		  return;
    } 				
  }		   
  CoupesDeCoutsReduits->Coefficient[Index] = Coeff[j];
  CoupesDeCoutsReduits->IndiceDeLaVariable[Index] = Indice[j];	
	Index++;		
}

CoupesDeCoutsReduits->NbElements[c] = NombreDeTermes;
CoupesDeCoutsReduits->SecondMembre[c] = SecondMembre;
CoupesDeCoutsReduits->LaCoupeEstDansLePool[c] = NON_PNE;
CoupesDeCoutsReduits->NombreDeCoupes++;

#if TRACES == 1
  printf("CoupesDeCoutsReduits->NombreDeCoupes %d\n",CoupesDeCoutsReduits->NombreDeCoupes);
# endif

return;
}

/*----------------------------------------------------------------------------*/

void PNE_ClasserLesCoutsReduits( int Var , int * First, int * Next, double * CoutReduit )
{
int ik; int ikPrec;

if ( *First == -1 ) { /* C'est la premiere variable */
  *First =  Var;
  Next[Var] = -1;
  return;
}

/* C'est pas la premiere variable: on lui cherche un emplacement */
ik = *First;
if ( fabs( CoutReduit[Var] ) > fabs( CoutReduit[ik] ) ) {
  *First = Var;
  Next[Var] = ik;
  return; 
}

/* C'est pas le meilleur */
ikPrec = ik;  
ik     = Next[ik];
while ( ik >= 0 ) {
  if ( fabs( CoutReduit[Var] ) > fabs( CoutReduit[ik] ) ) {		 
    /* Emplacement trouve */
    Next[ikPrec] = Var;
    Next[Var] = ik;
    return; 
  }
  ikPrec = ik;
  ik = Next[ik];
}

/* C'est la plus mauvaise: classement de la variable a la fin de la liste */
Next[ikPrec] =  Var;
Next[Var] = -1;

return;

}

/*----------------------------------------------------------------------------*/

# endif
