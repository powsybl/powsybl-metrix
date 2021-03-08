/***********************************************************************

   FONCTION: S'il existe des contraintes d'inegalite, on relaxe celles qui
	           ne sont pas saturees. Attention, uniquement au noeud racine.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_define.h"
# include "spx_fonctions.h"

# include "pne_define.h"

# define TRACES 1

# define NOMBRE_MAX_DE_RELAXATIONS 2  

# if RELAXATION_CONTRAINTES == OUI_SPX 

void SPX_AllocReallocContraintesRelaxees( PROBLEME_SPX * , char * );

/*----------------------------------------------------------------------------*/

void SPX_AllocReallocContraintesRelaxees( PROBLEME_SPX * Spx, char * Success )
{
int NombreDeContraintesRelaxeesAllouees; int i; int * NumerosDesContraintesRelaxees;
int * MdebContraintesRelaxees; int * NbTermContraintesRelaxees; double * BContraintesRelaxees;
int SizeAlloc;

*Success = OUI_SPX;

if ( Spx->NombreDeContraintesRelaxees == 0 ) {
  if ( Spx->NombreDeRelaxationsDeLaContrainte != NULL ) {
	  /* Mode branch and bound on realloue le vecteur */
		free( Spx->NombreDeRelaxationsDeLaContrainte );
		Spx->NombreDeRelaxationsDeLaContrainte = NULL;
  } 
  if ( Spx->NombreDeRelaxationsDeLaContrainte == NULL ) {
    Spx->NombreDeRelaxationsDeLaContrainte = (int *) malloc( Spx->NombreDeContraintes * sizeof( int ) );
    if ( Spx->NombreDeRelaxationsDeLaContrainte == NULL ) {
      *Success = NON_SPX;		
	    return;			 
	  }
  }
	memset( (char *) Spx->NombreDeRelaxationsDeLaContrainte, 0, Spx->NombreDeContraintes * sizeof( int ) );
}

if ( Spx->NombreDeContraintesRelaxees < Spx->NombreDeContraintesRelaxeesAllouees ) return;

SizeAlloc = (int) ceil( 0.5 * Spx->NombreDeContraintes );

if ( Spx->NombreDeContraintesRelaxeesAllouees == 0 ) {
  NombreDeContraintesRelaxeesAllouees = SizeAlloc;
}
else {
  NombreDeContraintesRelaxeesAllouees = Spx->NombreDeContraintesRelaxeesAllouees + SizeAlloc;
}

NumerosDesContraintesRelaxees = (int *) malloc( NombreDeContraintesRelaxeesAllouees * sizeof( int ) );
MdebContraintesRelaxees = (int *) malloc( NombreDeContraintesRelaxeesAllouees * sizeof( int ) );
NbTermContraintesRelaxees = (int *) malloc( NombreDeContraintesRelaxeesAllouees * sizeof( int ) );
BContraintesRelaxees = (double *) malloc( NombreDeContraintesRelaxeesAllouees * sizeof( double ) );
if ( NumerosDesContraintesRelaxees == NULL || MdebContraintesRelaxees == NULL ||
     NbTermContraintesRelaxees == NULL || BContraintesRelaxees == NULL ) {
  free( NumerosDesContraintesRelaxees ); free( MdebContraintesRelaxees );
  free( NbTermContraintesRelaxees ); free( BContraintesRelaxees );
  *Success = NON_SPX;
	return;			 
}

for ( i = 0 ; i < Spx->NombreDeContraintesRelaxees ; i++ ) {
  NumerosDesContraintesRelaxees[i] = Spx->NumerosDesContraintesRelaxees[i];
  MdebContraintesRelaxees[i] = Spx->MdebContraintesRelaxees[i];
  NbTermContraintesRelaxees[i] = Spx->NbTermContraintesRelaxees[i];
  BContraintesRelaxees[i] = Spx->BContraintesRelaxees[i];
}

free( Spx->NumerosDesContraintesRelaxees ); free( Spx->MdebContraintesRelaxees );
free( Spx->NbTermContraintesRelaxees ); free( Spx->BContraintesRelaxees );

Spx->NombreDeContraintesRelaxeesAllouees = NombreDeContraintesRelaxeesAllouees;
Spx->NumerosDesContraintesRelaxees = NumerosDesContraintesRelaxees;
Spx->MdebContraintesRelaxees = MdebContraintesRelaxees;
Spx->NbTermContraintesRelaxees = NbTermContraintesRelaxees;
Spx->BContraintesRelaxees = BContraintesRelaxees;

return;
}

/*----------------------------------------------------------------------------*/

void SPX_RelaxerDesContraintes( PROBLEME_SPX * Spx )
{
char * PositionDeLaVariable; int Var; char * OrigineDeLaVariable; int * Cdeb;
int * NumeroDeContrainte; int Cnt; int * NbTerm; int * Mdeb; int * Indcol;
double * A; double * B; int NombreDeVariables; int * MdebContraintesRelaxees;
int * NbTermContraintesRelaxees; double * BContraintesRelaxees; int * NumerosDesContraintesRelaxees;
int NombreDeContraintesRelaxeesAllouees; char Success; char ContraintesRelaxees;
int * NombreDeRelaxationsDeLaContrainte; double * BBarre; int * ContrainteDeLaVariableEnBase;

Spx->ContraintesRelaxeesOuReactivees = NON_SPX;

if ( Spx->FaireDeLaRelaxationDeContrainte == NON_PNE ) return;

NombreDeVariables = Spx->NombreDeVariables;
PositionDeLaVariable = Spx->PositionDeLaVariable;
OrigineDeLaVariable = Spx->OrigineDeLaVariable;
Cdeb = Spx->Cdeb;
NumeroDeContrainte = Spx->NumeroDeContrainte;

Mdeb = Spx->Mdeb;
NbTerm = Spx->NbTerm;   
Indcol = Spx->Indcol;
A = Spx->A;
B = Spx->B;

NombreDeContraintesRelaxeesAllouees = Spx->NombreDeContraintesRelaxeesAllouees;
NombreDeRelaxationsDeLaContrainte = Spx->NombreDeRelaxationsDeLaContrainte;
NumerosDesContraintesRelaxees = Spx->NumerosDesContraintesRelaxees;
MdebContraintesRelaxees = Spx->MdebContraintesRelaxees;
NbTermContraintesRelaxees = Spx->NbTermContraintesRelaxees;
BContraintesRelaxees = Spx->BContraintesRelaxees;

BBarre = Spx->BBarre;
ContrainteDeLaVariableEnBase = Spx->ContrainteDeLaVariableEnBase;

ContraintesRelaxees = NON_PNE;	
for ( Var = Spx->NombreDeVariablesNatives ; Var < Spx->NombreDeVariables ; Var++ ) {

  if ( OrigineDeLaVariable[Var] == BASIQUE_ARTIFICIELLE ) {	
		continue;
	}
  if ( PositionDeLaVariable[Var] != EN_BASE_LIBRE ) continue;
	
	Cnt = NumeroDeContrainte[Cdeb[Var]];
	if ( NbTerm[Cnt] == 1 ) continue; /* La contrainte a deja ete relaxee */

	if ( BBarre[ContrainteDeLaVariableEnBase[Var]] < 0.0 ) continue;
	
	/* On annule tous les termes de la contrainte mais on conserve la variable d'ecart afin
		 de ne pas avoir a changer ni le nombre de variables ni le nombre de contraintes */
	if ( Spx->NombreDeContraintesRelaxees >= NombreDeContraintesRelaxeesAllouees || Spx->NombreDeContraintesRelaxees == 0 ) {
    SPX_AllocReallocContraintesRelaxees( Spx, &Success );
    if ( Success == NON_SPX ) break;
    NombreDeContraintesRelaxeesAllouees = Spx->NombreDeContraintesRelaxeesAllouees;
    NombreDeRelaxationsDeLaContrainte = Spx->NombreDeRelaxationsDeLaContrainte;
    NumerosDesContraintesRelaxees = Spx->NumerosDesContraintesRelaxees;
    MdebContraintesRelaxees = Spx->MdebContraintesRelaxees;
    NbTermContraintesRelaxees = Spx->NbTermContraintesRelaxees;
    BContraintesRelaxees = Spx->BContraintesRelaxees;
	}

	/* Si la contrainte a deja ete relaxee trop de fois on arrete de le faire */

	if ( NombreDeRelaxationsDeLaContrainte[Cnt] > NOMBRE_MAX_DE_RELAXATIONS ) continue;
  NombreDeRelaxationsDeLaContrainte[Cnt]++;
	
  NumerosDesContraintesRelaxees[Spx->NombreDeContraintesRelaxees] = Cnt;
  MdebContraintesRelaxees[Spx->NombreDeContraintesRelaxees] = Mdeb[Cnt];
  NbTermContraintesRelaxees[Spx->NombreDeContraintesRelaxees] = NbTerm[Cnt];
  BContraintesRelaxees[Spx->NombreDeContraintesRelaxees] = B[Cnt];
  Spx->NombreDeContraintesRelaxees++;
			 
	Mdeb[Cnt] = Mdeb[Cnt] + NbTerm[Cnt] - 1;
	NbTerm[Cnt] = 1;	
	B[Cnt] = 0;
	
  ContraintesRelaxees = OUI_PNE;	
		
}

if ( ContraintesRelaxees == NON_PNE ) return;

Spx->ContraintesRelaxeesOuReactivees = OUI_SPX;

# if TRACES == 1
  printf("Spx->NombreDeContraintesRelaxees %d  Iteration %d\n",Spx->NombreDeContraintesRelaxees, Spx->Iteration);
# endif

return;
}

/*----------------------------------------------------------------------------*/

# endif
