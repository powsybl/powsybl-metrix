/***********************************************************************

   FONCTION: Detection des coupes de contraintes d'ordre violees.
                
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

# define TRACES 1
													
/*----------------------------------------------------------------------------*/

void PNE_DetectionDesContraintesDOrdreViolees( PROBLEME_PNE * Pne )
{
int c; CONTRAINTES_DORDRE * ContraintesDOrdre; int NbT; int * First; int * IndiceDeLaVariable;
double * Coefficient; double * Xmin; double * Xmax;
double * X; int * TypeDeBorne; double S; int il; int ilMax; double Sec; int Var;
double Seuil; double * Coeff; int * Indice; char * LaContrainteEstDansLePool;

# if RELATION_DORDRE_DANS_LE_PROBING == NON_PNE
  return;
# endif
# if METHODE_DE_PRISE_EN_COMPTE_DES_CONTRAINTES_DORDRE != CONTRAINTES_DORDRE_DANS_LES_COUPES 
  return;
# endif

return; /* En fait il ne faut pas faire comme. Il faut le prendre en compte au niveau du branching */

if ( Pne->ContraintesDOrdre == NULL ) return;

ContraintesDOrdre = Pne->ContraintesDOrdre;
First = ContraintesDOrdre->First;
IndiceDeLaVariable = ContraintesDOrdre->IndiceDeLaVariable;
Coefficient = ContraintesDOrdre->Coefficient;
LaContrainteEstDansLePool = ContraintesDOrdre->LaContrainteEstDansLePool;

TypeDeBorne = Pne->TypeDeBorneTrav;
X = Pne->UTrav;
Xmin = Pne->UminTrav;
Xmax = Pne->UmaxTrav;

Coeff = Pne->Coefficient_CG;
Indice = Pne->IndiceDeLaVariable_CG;

/* On utilise le seuil des cliques */
PNE_MiseAJourSeuilCoupes( Pne, COUPE_CONTRAINTE_DORDRE, &Seuil );

for ( c = 0 ; c < ContraintesDOrdre->NombreDeContraintes ; c++ ) {
  if ( LaContrainteEstDansLePool[c] == OUI_PNE ) continue;
  il = First[c];
	if ( il < 0 ) continue; /* On ne sait jamais ... */	
  ilMax = il + 2;
	S = 0;
	Sec = 0;
  while ( il < ilMax ) {
	  Var = IndiceDeLaVariable[il];
	  /*if ( TypeDeBorne[Var] == VARIABLE_FIXE || ) Sec -= Coefficient[il] * X[Var];*/
	  if ( TypeDeBorne[Var] == VARIABLE_FIXE || Xmin[Var] == Xmax[Var] ) {  
      S = Sec - 10;
      break;
		}		
		else S += Coefficient[il] * X[Var];		
	  il++;
	}
	
  if ( S < Sec + Seuil ) continue;

	# if TRACES == OUI_PNE
	  printf("****************************** Contrainte d'ordre %d violee de %e\n",c,S-Sec);
	# endif

  NbT = 0;
  il = First[c];	
  ilMax = il + 2;
  while ( il < ilMax ) {
	  printf(" %e (%d) + ",Coefficient[il],IndiceDeLaVariable[il]);
		Coeff[NbT] = Coefficient[il];
		Indice[NbT] = IndiceDeLaVariable[il];
		NbT++;		
	  il++;
	}
	printf("\n");
		
  PNE_EnrichirLeProblemeCourantAvecUneCoupe( Pne, 'K', NbT, Sec, fabs( S - Sec ), Coeff , Indice );
  Pne->CoupesCalculees[Pne->NombreDeCoupesCalculees-1]->IndexDansCoupesContraintesDOrdre = c; 
      		
}

return;
}

/*----------------------------------------------------------------------------*/

