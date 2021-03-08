/***********************************************************************

   FONCTION: S'il existe des contraintes d'inegalite relaxees, on les
	           controle a chaque iteration. Si la contrainte est violee, on la
						 met dans la matrice et on ne l'enleve plus.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_define.h"
# include "spx_fonctions.h"

# include "pne_define.h"

# define TRACES 1

# if RELAXATION_CONTRAINTES == OUI_SPX

/*----------------------------------------------------------------------------*/

void SPX_ReinitialisationsRelaxationReactivation( PROBLEME_SPX * Spx )
{
/* On reconstruit le chainage de la transposee */
/*Spx->StockageParColonneSauvegarde = NON_SPX;*/
SPX_ChainageDeLaTransposee( Spx, COMPACT );

/* Refactoriser la base */
SPX_FactoriserLaBase( Spx );
Spx->CalculerBBarre = OUI_SPX; 
Spx->CalculerCBarre = OUI_SPX;

/* Initialisation des poids de la methode projected steepest edge */
SPX_InitDualPoids( Spx );

Spx->ContraintesRelaxeesOuReactivees = NON_SPX;

return;
}
/*----------------------------------------------------------------------------*/

void SPX_ControlerLesContraintesRelaxees( PROBLEME_SPX * Spx, char ToutRemettre )
{
int NombreDeContraintesRelaxees; int * NumerosDesContraintesRelaxees; int * MdebContraintesRelaxees ;
int * NbTermContraintesRelaxees; double * BContraintesRelaxees; double S; int Cnt;
int Var; char * PositionDeLaVariable; double * BBarre; int * ContrainteDeLaVariableEnBase;
double X; double * Xmax; int NbContraintesReactivees; char * StatutBorneSupCourante;
char Position; int i; int il; int ilMax; int iMx; int * Mdeb; int * NbTerm;
int * Indcol; double * A; double * B;

Spx->ContraintesRelaxeesOuReactivees = NON_SPX;

NombreDeContraintesRelaxees = Spx->NombreDeContraintesRelaxees;
NumerosDesContraintesRelaxees = Spx->NumerosDesContraintesRelaxees;
MdebContraintesRelaxees = Spx->MdebContraintesRelaxees;
NbTermContraintesRelaxees = Spx->NbTermContraintesRelaxees;
BContraintesRelaxees = Spx->BContraintesRelaxees;

Mdeb = Spx->Mdeb;
NbTerm = Spx->NbTerm;
Indcol = Spx->Indcol;    
A = Spx->A;
B = Spx->B;

PositionDeLaVariable = Spx->PositionDeLaVariable;
BBarre         = Spx->BBarre;
ContrainteDeLaVariableEnBase = Spx->ContrainteDeLaVariableEnBase;

Xmax = Spx->Xmax;
StatutBorneSupCourante = Spx->StatutBorneSupCourante;

NbContraintesReactivees = 0;

for ( i = 0 ; i < NombreDeContraintesRelaxees ; i++ ) {	
  Cnt = NumerosDesContraintesRelaxees[i];
	
	if ( ToutRemettre == OUI_SPX ) goto OnReactive;
	
  S = 0;
	il = MdebContraintesRelaxees[i];
	ilMax = il + NbTermContraintesRelaxees[i];
	ilMax --; /* Car la variable d'ecart est placee en dernier */
	while ( il < ilMax ) {
    Var = Indcol[il];
		Position = PositionDeLaVariable[Var];
    if ( Position == EN_BASE_LIBRE ) { 
      /* La variable est donc en base */
      X = BBarre[ContrainteDeLaVariableEnBase[Var]]; 
    }
    else if ( Position == HORS_BASE_SUR_BORNE_SUP ) X = Xmax[Var];
	  else {
	    /* La variable est HORS_BASE_A_ZERO ou HORS_BASE_SUR_BORNE_INF */
	    X = 0.0;
      if ( Position == HORS_BASE_SUR_BORNE_INF ) {
		    if ( StatutBorneSupCourante[Var] == BORNE_AUXILIAIRE_DE_VARIABLE_NON_BORNEE ) {
	        /* La variable est hors base sur borne inf mais qu'elle a une borne avec un StatutBorneSupCourante egal a
				     BORNE_AUXILIAIRE_DE_VARIABLE_NON_BORNEE alors elle est a -Xmax[Var] */
	        X = -Xmax[Var];				
	      }
		  }
		}
		S += A[il] * X;		
	  il++;
	}
	/* On reactive aussi les contraintes subeffectives */
	if ( S <= BContraintesRelaxees[i] ) continue;
  OnReactive:	
	/* On reactive la contrainte */
		
	NbContraintesReactivees++;
		
  NumerosDesContraintesRelaxees[NombreDeContraintesRelaxees] = Cnt;
	
  Mdeb[Cnt] = MdebContraintesRelaxees[i];
  NbTerm[Cnt] = NbTermContraintesRelaxees[i];
  B[Cnt] = BContraintesRelaxees[i];

	/* On supprime la contrainte reactivee de la liste des contraintes relaxees */

	iMx = NombreDeContraintesRelaxees - 1;
  NumerosDesContraintesRelaxees[i] = NumerosDesContraintesRelaxees[iMx];
  MdebContraintesRelaxees[i] = MdebContraintesRelaxees[iMx];
  NbTermContraintesRelaxees[i] = NbTermContraintesRelaxees[iMx];
  BContraintesRelaxees[i] = BContraintesRelaxees[iMx];
	NombreDeContraintesRelaxees--;
	i--;
	
}

if ( NbContraintesReactivees > 0 ) {
	
  Spx->NombreDeContraintesRelaxees = NombreDeContraintesRelaxees;
  Spx->ContraintesRelaxeesOuReactivees = OUI_SPX;
	
  # if TRACES == 1
    printf("Nombre de contraintes reactivee %d  NombreDeContraintesRelaxees %d iteration %d\n",
					 NbContraintesReactivees,NombreDeContraintesRelaxees,Spx->Iteration);
  # endif	

}

return;
}

/*----------------------------------------------------------------------------*/

# endif
