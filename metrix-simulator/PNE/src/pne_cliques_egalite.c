/***********************************************************************

   FONCTION: On essaie de transformer les cliques en contraintes d'egalite.
	        
                 
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

void PNE_TransformerCliquesEnEgalites( PROBLEME_PNE * Pne )
{
double * U; double * Umin; double * Umax; CLIQUES * Cliques; int c; int ilDeb;
int * First; int * NbElements; int il; int ilMax; int Pivot;  int Var; int * NoeudDeClique;
int * TypeDeBorne; char Ok; PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve;
int NbEgalites; int Nb; int NbFix;

return; /* Apres test il s'avere que si une clique est de type = elle est toujours verifiee par
           la suite et je ne sais pas pourquoi */
					 
if ( Pne->ConflictGraph == NULL ) return;
if ( Pne->Cliques == NULL ) return;

if ( Pne->ChainageTransposeeExploitable == NON_PNE ) PNE_ConstruireLeChainageDeLaTransposee( Pne );

ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;
if ( ProbingOuNodePresolve == NULL ) return;

U = Pne->UTrav;
Umin = Pne->UminTrav;
Umax = Pne->UmaxTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;

Cliques = Pne->Cliques;
First = Cliques->First;  
NbElements = Cliques->NbElements;
NoeudDeClique = Cliques->Noeud;
Pivot = Pne->ConflictGraph->Pivot;

memcpy( (char *) ProbingOuNodePresolve->BorneInfConnue, (char *) ProbingOuNodePresolve->BorneInfConnueSv, Pne->NombreDeVariablesTrav * sizeof( char ) );
memcpy( (char *) ProbingOuNodePresolve->BorneSupConnue, (char *) ProbingOuNodePresolve->BorneSupConnueSv, Pne->NombreDeVariablesTrav * sizeof( char ) );
memcpy( (char *) ProbingOuNodePresolve->ValeurDeBorneInf, (char *) ProbingOuNodePresolve->ValeurDeBorneInfSv, Pne->NombreDeVariablesTrav * sizeof( double ) );
memcpy( (char *) ProbingOuNodePresolve->ValeurDeBorneSup, (char *) ProbingOuNodePresolve->ValeurDeBorneSupSv, Pne->NombreDeVariablesTrav * sizeof( double ) );

memcpy( (char *) ProbingOuNodePresolve->Bmin, (char *) ProbingOuNodePresolve->BminSv, Pne->NombreDeContraintesTrav * sizeof( double ) );
memcpy( (char *) ProbingOuNodePresolve->Bmax, (char *) ProbingOuNodePresolve->BmaxSv, Pne->NombreDeContraintesTrav * sizeof( double ) );

ProbingOuNodePresolve->NbVariablesModifiees = 0;
ProbingOuNodePresolve->NbContraintesModifiees = 0;
memset( (char *) ProbingOuNodePresolve->VariableModifiee, NON_PNE, Pne->NombreDeVariablesTrav   * sizeof( char ) );
memset( (int *) ProbingOuNodePresolve->NbFoisContrainteModifiee, 0, Pne->NombreDeContraintesTrav * sizeof( int ) );

ProbingOuNodePresolve->NbCntCoupesDeProbing = 0;
memset( (char *) ProbingOuNodePresolve->FlagCntCoupesDeProbing, 0, Pne->NombreDeContraintesTrav * sizeof( char ) );

ProbingOuNodePresolve->NombreDeVariablesFixees = 0; 

NbEgalites = 0;

/* On initialise a 0 toutes les variables de la clique inferieures a Pivot
   et a 1 toutes les variables de la clique superieures ou egal a pivot.
	 S'il n'y a pas de solution admissibles alors la clique peut etre transformee
	 en contraintes d'egalite */
	 	 
for ( c = 0 ; c < Cliques->NombreDeCliques ; c++ ) {
	
  ilDeb = First[c];
	if ( ilDeb < 0 ) continue; /* On ne sait jamais ... */

	Ok = OUI_PNE;
	il = ilDeb;
  ilMax = il + NbElements[c];
	Ok = OUI_PNE;
	Nb = 0;
  while ( il < ilMax ) {
	  if ( NoeudDeClique[il] < Pivot ) Var = NoeudDeClique[il];
		else Var = NoeudDeClique[il] - Pivot;
	  if ( TypeDeBorne[Var] == VARIABLE_FIXE ) Ok = NON_PNE;
		if ( Umin[Var] == Umax[Var] ) Ok = NON_PNE;
		if ( Ok == NON_PNE ) break;
		Nb++;
		il++;
	}
	if ( Ok == NON_PNE ) {
	  continue;
	}
	
  PNE_VariableProbingReinitDonnees( Pne );
	
  ProbingOuNodePresolve->Faisabilite = OUI_PNE;		
	
	il = ilDeb;
  ilMax = il + NbElements[c];
	NbFix = 0;
  while ( il < ilMax ) {	
	  if ( NoeudDeClique[il] < Pivot ) {	    		
			/* On initialise a 0 */
			Var = NoeudDeClique[il]; 
	    ProbingOuNodePresolve->VariableInstanciee = Var;
      ProbingOuNodePresolve->ValeurDeLaVariableInstanciee = Umin[Var];
			NbFix++;
      PNE_VariableProbingPreparerInstanciation( Pne, Var, Umin[Var] );			
      if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE && NbFix != Nb ) printf("Infaisabilite avant la fin NbFix %d Nb %d\n",NbFix,Nb); 			
		}
		else {
			/* On initialise a 1 */
			Var = NoeudDeClique[il]-Pivot;
	    ProbingOuNodePresolve->VariableInstanciee = Var;
      ProbingOuNodePresolve->ValeurDeLaVariableInstanciee = Umax[Var];
			NbFix++;
      PNE_VariableProbingPreparerInstanciation( Pne, Var, Umax[Var] );
      if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE && NbFix != Nb ) printf("Infaisabilite avant la fin NbFix %d Nb %d\n",NbFix,Nb); 
		}		
	  il++;
	}	
	/* On controle la faisabilite */
  if ( Pne->ProbingOuNodePresolve->Faisabilite == OUI_PNE ) {
		PNE_AnalyseListeDeContraintes( Pne );		
	}
	if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) {
		/* On peut transformer la gub en egalite */
    Cliques->CliqueDeTypeEgalite[c] = OUI_PNE;		
		NbEgalites++;
		# if TRACES == 1
		  printf("Transformation de la clique %d en egalite:\n",c);
	    il = ilDeb;
      ilMax = il + NbElements[c];
      while ( il < ilMax ) {	
	      if ( NoeudDeClique[il] < Pivot ) {
				  Var = NoeudDeClique[il];
					printf(" + %d ",Var);	                     			
		    }
		    else {
			    Var = NoeudDeClique[il]-Pivot;
					printf(" + ( 1 - %d ) ",Var);	                     				      
		    }		
	      il++;
	    }
			printf(" = 1\n");
		# endif
	}
  /* La liste des contraintes a analyser peut ne pas etre entierement a l'etat initial dans ce cas */
  PNE_VariableProbingRazContraintesAAnalyser( Pne, ProbingOuNodePresolve );	      			
  /* La liste des coupes de probing potentielles peut ne pas etre entierement a l'etat initial dans ce cas */
  PNE_VariableProbingRazCoupesDeProbingPotentielles( ProbingOuNodePresolve );						
}

Pne->ProbingOuNodePresolve->Faisabilite = OUI_PNE;

/* Car utilise dans AmeliorerLesCoefficientsDesVariablesBinaires */

memcpy( (char *) ProbingOuNodePresolve->BorneInfConnue, (char *) ProbingOuNodePresolve->BorneInfConnueSv, Pne->NombreDeVariablesTrav * sizeof( char ) );
memcpy( (char *) ProbingOuNodePresolve->BorneSupConnue, (char *) ProbingOuNodePresolve->BorneSupConnueSv, Pne->NombreDeVariablesTrav * sizeof( char ) );
memcpy( (char *) ProbingOuNodePresolve->ValeurDeBorneInf, (char *) ProbingOuNodePresolve->ValeurDeBorneInfSv, Pne->NombreDeVariablesTrav * sizeof( double ) );
memcpy( (char *) ProbingOuNodePresolve->ValeurDeBorneSup, (char *) ProbingOuNodePresolve->ValeurDeBorneSupSv, Pne->NombreDeVariablesTrav * sizeof( double ) );

memcpy( (char *) ProbingOuNodePresolve->Bmin, (char *) ProbingOuNodePresolve->BminSv, Pne->NombreDeContraintesTrav * sizeof( double ) );
memcpy( (char *) ProbingOuNodePresolve->Bmax, (char *) ProbingOuNodePresolve->BmaxSv, Pne->NombreDeContraintesTrav * sizeof( double ) );

if ( Pne->AffichageDesTraces == OUI_PNE ) {
  if ( NbEgalites > 0 ) printf("%d cliques of %d are equalities\n",NbEgalites,Cliques->NombreDeCliques);
}

return;
}
