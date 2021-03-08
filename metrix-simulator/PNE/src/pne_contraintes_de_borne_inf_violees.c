/***********************************************************************

   FONCTION: Detection des contraintes de borne inf violees.
	           Les contraintes de borne variable sont crees dans le node presolve
						 au noeud racine et dans le reduced cost fixing au noeud racine.
                 
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"  
  
# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_define.h"
# include "spx_fonctions.h"
 
# include "bb_define.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define MAX_COUPES_DE_BORNE_INF 1000

# define TRACES OUI_PNE

# if FAIRE_DES_COUPES_AVEC_LES_BORNES_INF_MODIFIEES == OUI_PNE

/*----------------------------------------------------------------------------*/

void PNE_DetectionDesContraintesDeBorneInfViolees( PROBLEME_PNE * Pne )
{
double E; int Nb; double * Coeff; int * Indice; double B; int NbT; double * X; int * TypeDeBorne;
/*char * LaContrainteDeBorneInfEstDansLePool;*/
int Var; double Seuil; int NombreDeVariables; double * UminAmeliorePourCoupes;

return; /* On utilisera ces bornes dans le node presolve simplifie */

/* On considere que c'est une coupe de borne variable pour eviter de compliquer */
PNE_MiseAJourSeuilCoupes( Pne, COUPE_DE_BORNE_VARIABLE, &Seuil );

NombreDeVariables = Pne->NombreDeVariablesTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
X = Pne->UTrav;
Coeff = Pne->Coefficient_CG;
Indice = Pne->IndiceDeLaVariable_CG;

UminAmeliorePourCoupes = Pne->UminAmeliorePourCoupes;

/* Attention: la partie simplexe considere que les variables intervenant dans les coupes ne
	 sont jamais de type fixe, c'est a dire qu'il y a toujours une correspondance des
	 les variables du simplexe. Il faut donc ne pas mettre ces coupes. */
	 
Nb = 0;

for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  if ( UminAmeliorePourCoupes[Var] == VALEUR_NON_INITIALISEE ) continue;
	if ( TypeDeBorne[Var] == VARIABLE_FIXE ) continue;
  if ( X[Var] < UminAmeliorePourCoupes[Var] -  Seuil ) {
    Coeff[0] = -1.;		 
	  Indice[0] = Var;
	  NbT = 1;
    B = -UminAmeliorePourCoupes[Var];
	  E = UminAmeliorePourCoupes[Var] - X[Var];

    if ( PNE_LaCoupeEstColineaire( Pne, Coeff, Indice, B, NbT ) == OUI_PNE ) continue;
		
    # if TRACES == OUI_PNE
		  printf("Ajout de la contrainte de borne inf. pour la variable %d: \n",Var);
			printf("valeur de la variable %e valeur de la borne inf %e depassement %e \n",X[Var],UminAmeliorePourCoupes[Var],E);
		# endif
		/* Pour l'instant on met le type I mais on prevoit de mettre O comme others */
		Nb++;
    PNE_EnrichirLeProblemeCourantAvecUneCoupe( Pne, 'I', NbT, B, E, Coeff, Indice );
    /*Pne->CoupesCalculees[Pne->NombreDeCoupesCalculees-1]->IndexDansContraintesDeBorneVariable = Var;*/
	}
}

return;
}

/*----------------------------------------------------------------------------*/

# endif
