/***********************************************************************

   FONCTION: Probing sur les gub de type inegalite afin d'en faire des
	           egalites.
						 Soit la gub inegalite: Somme x_i <= 1
						 On test x_i = 0 pour tous les x_i en meme temps.
						 S'il n'y a pas de sollution alors on peut transformer
						 l'ineaglite en egalite.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"  
  
# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_define.h"
  
# include "bb_define.h"

# include "prs_define.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define VERBOSE_GUB_PROBING 0

# define PROBING_SUR_LES_GUB_NATIVES OUI_PNE /*OUI_PNE*/
# define EPS_GUB 1.e-6

/*----------------------------------------------------------------------------*/

void PNE_ProbingGubInegalites( PROBLEME_PNE * Pne )
{
int Cnt; int NombreDeContraintes; double * B; char * SensContrainte; int Var;
int * Mdeb; int * NbTerm; int il; int ilMax; char Gub; double * A; double b;
int * Nuvar; int * TypeDeBorne; int * TypeDeVariable; double * Umin; double * Umax;
double * U; PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve; int Nb; int NbFix;
int NbTransformations;

# if PROBING_SUR_LES_GUB_NATIVES == NON_PNE
  return;
# endif

ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;
if ( ProbingOuNodePresolve == NULL ) return;

NombreDeContraintes = Pne->NombreDeContraintesTrav;
B = Pne->BTrav;
SensContrainte = Pne->SensContrainteTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
A =  Pne->ATrav;
Nuvar = Pne->NuvarTrav;
U = Pne->UTrav;
Umin = Pne->UminTrav;
Umax = Pne->UmaxTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
TypeDeVariable = Pne->TypeDeVariableTrav;
NbTransformations = 0;

for ( Cnt = 0 ; Cnt < NombreDeContraintes; Cnt++ ) {
	if ( SensContrainte[Cnt] != '<' ) continue;
  il = Mdeb[Cnt];
  ilMax = il + NbTerm[Cnt];
	Gub = OUI_PNE;
	b = B[Cnt];
	Nb = 0;
	while ( il < ilMax) {
	  if ( A[il] == 0 ) goto NextIl;
	  if ( fabs( A[il] ) != 1 ) {
		  Gub = NON_PNE;
			break;
		}
		Var = Nuvar[il];		
		if ( Umin[Var] == Umax[Var] ) {
		  b -= A[il] * Umin[Var];
		  goto NextIl;
		}
		if ( TypeDeBorne[Var] == VARIABLE_FIXE ) {
		  b -= A[il] * U[Var];
		  goto NextIl;
		}
		if ( TypeDeVariable[Var] != ENTIER ) {
		  Gub = NON_PNE;
			break;
		}
		if ( A[il] < 0 ) b += 1;		
		Nb++;
		NextIl:
	  il++;
	}

	if ( b != 1 ) continue;
	
  if ( Gub == OUI_PNE && Nb > 0 ) {
		if ( b < EPS_GUB  ) {		
		  /* Pour l'instant on ne le fait pas car il faut tout remettre a jour comme
			   lorsqu'on fixe une varaible dans le probing */				 
			continue;
		  printf("mettre toutes les variables a 0:\n");
      il = Mdeb[Cnt];
      ilMax = il + NbTerm[Cnt];
	    while ( il < ilMax) {
	      if ( A[il] != 0 ) { 
		      Var = Nuvar[il];				
		      if ( TypeDeVariable[Var] == ENTIER ) {	
		        if ( Umin[Var] != Umax[Var] ) {
						  if ( A[il] > 0 ) { 
						    printf("   variable %d fixee a %e\n",Var,Umin[Var]);
                PNE_VariableProbingFixerUneVariableInstanciee( Pne, ProbingOuNodePresolve, Var, Umin[Var]);
	              if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;
							}
							else if ( A[il] < 0 ) {
						    printf("   variable %d fixee a %e\n",Var,Umax[Var]);
                PNE_VariableProbingFixerUneVariableInstanciee( Pne, ProbingOuNodePresolve, Var, Umax[Var]);
	              if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;						
							}
            }
				  }
			  }
	      il++;
	    }		 			
	  }
    else {
		  if ( Nb == 1 ) continue;
      /* On met tout a 0 et on teste la faisabilite */
      # if VERBOSE_GUB_PROBING == 1
			  printf("Instanciation de %d variables a 0\n",Nb);
			# endif
      PNE_VariableProbingReinitDonnees( Pne );
			ProbingOuNodePresolve->Faisabilite = OUI_PNE;
			NbFix = 0;
      il = Mdeb[Cnt];
      ilMax = il + NbTerm[Cnt];
	    while ( il < ilMax) {
	      if ( A[il] != 0 ) { 
		      Var = Nuvar[il];				
		      if ( TypeDeVariable[Var] == ENTIER && TypeDeBorne[Var] != VARIABLE_FIXE ) {	
		        if ( Umin[Var] != Umax[Var] ) {
	            ProbingOuNodePresolve->VariableInstanciee = Var;
              ProbingOuNodePresolve->Faisabilite = OUI_PNE;												
						  if ( A[il] > 0 ) {
							  NbFix++;
                ProbingOuNodePresolve->ValeurDeLaVariableInstanciee = Umin[Var];	
                PNE_VariableProbingPreparerInstanciation( Pne, Var, Umin[Var] );
                if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) {
                  # if VERBOSE_GUB_PROBING == 1
								    if ( NbFix < Nb ) printf("Infaisabilite avant la fin NbFix %d Nb %d\n",NbFix,Nb);
									# endif
								  break;
								}
							}
							else if ( A[il] < 0 ) {							
							  NbFix++;
                ProbingOuNodePresolve->ValeurDeLaVariableInstanciee = Umax[Var];	
                PNE_VariableProbingPreparerInstanciation( Pne, Var, Umax[Var] );
                if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) {
                  # if VERBOSE_GUB_PROBING == 1
								    if ( NbFix < Nb ) printf("Infaisabilite avant la fin NbFix %d Nb %d\n",NbFix,Nb);
									# endif
								  break;
								}
							}							
            }
				  }
			  }
	      il++;
	    }
			/* On controle la faisabilite */
      if ( Pne->ProbingOuNodePresolve->Faisabilite == OUI_PNE ) {
			  PNE_AnalyseListeDeContraintes( Pne );
			}			
	    if ( ProbingOuNodePresolve->Faisabilite == NON_PNE ) {
			  /* On peut transformer la gub en egalite */
        # if VERBOSE_GUB_PROBING == 1
				  printf("Transformation de la contrainte %d en egalite\n",Cnt);
				# endif
	      SensContrainte[Cnt] = '=';
				NbTransformations++;
				/* Il est aussi interessant d'ajouter les variables de l'egalite dans la liste des
				   variables a instancier dans le probing */
        il = Mdeb[Cnt];
        ilMax = il + NbTerm[Cnt];
	      while ( il < ilMax ) {
				  break;
	        if ( A[il] != 0 ) { 
		        Var = Nuvar[il];				
		        if ( TypeDeVariable[Var] == ENTIER && TypeDeBorne[Var] != VARIABLE_FIXE ) {	
		          if ( Umin[Var] != Umax[Var] ) {
							  ProbingOuNodePresolve->FlagVarAInstancier[Var] = 1;
							}
						}	
					}
					il++;
        }   					 					 
		  }			
      /* La liste des contraintes a analyser peut ne pas etre entierement a l'etat initial dans ce cas */
      PNE_VariableProbingRazContraintesAAnalyser( Pne, ProbingOuNodePresolve );	      			
      /* La liste des coupes de probing potentielles peut ne pas etre entierement a l'etat initial dans ce cas */
      PNE_VariableProbingRazCoupesDeProbingPotentielles( ProbingOuNodePresolve );									
		}
		
	}
}

Pne->ProbingOuNodePresolve->Faisabilite = OUI_PNE;

if ( Pne->AffichageDesTraces == OUI_PNE ) {
  if ( NbTransformations > 0 ) {
    printf("%d GUB inequalities changed to equalities\n",NbTransformations);
	}
}

return;
}
