/***********************************************************************

   FONCTION: On essaie de faire des substitutions de variables en
	           exploitant le grpahe de conflits.
                
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

# define TRACES 0
   
# define SIMULATION   1
# define REMPLACEMENT 2
# define SUBSTITUTION_POSSIBLE   1
# define SUBSTITUTION_IMPOSSIBLE 2

# define CONTROLER_LE_CONDITIONNEMENT NON_PNE

/*----------------------------------------------------------------------------*/

void PNE_ExploiterLeGrapheDeConflitsPourLesSubtitutionsDeVariables( PROBLEME_PNE * Pne )
{
int Edge; int Noeud; int Complement; int Nv; int Pivot; int * First; int * Adjacent;
int * Next; int NbNoeudsAExaminer; int NvRecherche; int EdgeComplement; double ATilde; double BTilde;
int VarSubstituee; int VarRestante; char FaireSubstitution; int NbSubstitutions; int Mode;
int CodeRet; int * TypeDeBorne; double * L; int Var; double PlusPetitCout; double PlusGrandCout;

# if EXPLOITER_GRAPHE_POUR_SUBSTITUTIONS_DE_VARIABLES == NON_PNE
  /*
  printf(" return dans PNE_ExploiterLeGrapheDeConflitsPourLesSubtitutionsDeVariables\n");
  */
  return;
# endif

if ( Pne->ConflictGraph == NULL ) return;
Pivot = Pne->ConflictGraph->Pivot;
First = Pne->ConflictGraph->First;
Adjacent = Pne->ConflictGraph->Adjacent;
Next = Pne->ConflictGraph->Next;
NbNoeudsAExaminer = Pivot;
NbSubstitutions = 0;

if ( Pne->ChainageTransposeeExploitable == NON_PNE ) PNE_ConstruireLeChainageDeLaTransposee( Pne );

PlusPetitCout = LINFINI_PNE;
PlusGrandCout = -LINFINI_PNE;
TypeDeBorne = Pne->TypeDeBorneTrav;
L = Pne->LTrav;
for ( Var = 0 ; Var < Pne->NombreDeVariablesTrav ; Var++ ) {
  if ( L[Var] == 0.0 ) continue;
  if ( TypeDeBorne[Var] == VARIABLE_FIXE ) continue;
	if ( fabs( L[Var] ) < PlusPetitCout ) PlusPetitCout = fabs( L[Var] );
	else if ( fabs( L[Var] ) > PlusGrandCout ) PlusGrandCout =  fabs( L[Var] );
}

for ( Noeud = 0 ; Noeud < NbNoeudsAExaminer ; Noeud++ ) {  
  Complement = Noeud + Pivot;
  Edge = First[Noeud];
  while ( Edge >= 0 ) {
	  FaireSubstitution = NON_PNE;
    Nv = Adjacent[Edge];
	  /* Attention a ne pas prendre le complement */
	  if ( Nv == Complement ) goto NextEdge;
    if ( Nv < Pivot ) {
		  NvRecherche = Nv + Pivot;
		  /* On regarde les voisins de Complement */
      EdgeComplement = First[Complement];
      while ( EdgeComplement >= 0 ) {
        if ( Adjacent[EdgeComplement] == NvRecherche ) {
				  /* Substitution x = 1-y */
					ATilde = 1;
					BTilde = 1;
          VarSubstituee = NvRecherche - Pivot;					
          VarRestante = Noeud;
					FaireSubstitution = OUI_PNE;
					break;
				}		    
        EdgeComplement = Next[EdgeComplement];
			}	 
	  }
    else {
		  NvRecherche = Nv - Pivot;
		  /* On regarde les voisins de Complement */
      EdgeComplement = First[Complement];
      while ( EdgeComplement >= 0 ) {
        if ( Adjacent[EdgeComplement] == NvRecherche ) {
				  /* Substitution x = y */
					ATilde = -1;
					BTilde = 0;
          VarSubstituee = NvRecherche;
          VarRestante = Noeud;
					FaireSubstitution = OUI_PNE;				
					break;
				}		    
        EdgeComplement = Next[EdgeComplement];
			}	  
	  }
	  NextEdge:
		
    if ( FaireSubstitution == OUI_PNE ) {		
		  Mode = SIMULATION;
		  CodeRet = PNE_GrapheDeConflitsSubtituerUneVariable( Pne, Mode, VarSubstituee, VarRestante, ATilde, BTilde, PlusPetitCout, PlusGrandCout );
			if ( CodeRet == SUBSTITUTION_IMPOSSIBLE ) FaireSubstitution = NON_PNE;
		}
		
    if ( FaireSubstitution == OUI_PNE ) {

		  # if TRACES == 1
        if ( BTilde != 0.0 ) printf("Substitution possible x = 1 - y variable restante %d variable substituee %d\n",VarRestante,VarSubstituee);				
        else printf("Substitution possible x = y variable restante %d variable substituee %d\n",VarRestante,VarSubstituee);				
		  # endif
			
		  Mode = REMPLACEMENT;
		  CodeRet = PNE_GrapheDeConflitsSubtituerUneVariable( Pne, Mode, VarSubstituee, VarRestante, ATilde, BTilde, PlusPetitCout, PlusGrandCout );
			
			NbSubstitutions++;
					
			/* On redemarre au debut du noeud car le chainage a ete modifie */
      Edge = First[Noeud];			
		}
    else Edge = Next[Edge];		
	}
}

if ( Pne->AffichageDesTraces == OUI_PNE ) {
  if ( NbSubstitutions > 0 ) printf("%d substitutions using the conflict graph\n",NbSubstitutions);
}

/*if ( NbSubstitutions > 0 ) Pne->ChainageTransposeeExploitable = NON_PNE;*/ /* Inutile ici */

return;
}

/*************************************************************************/

int PNE_GrapheDeConflitsSubtituerUneVariable( PROBLEME_PNE * Pne, int Mode, int VarSubstituee,
                                              int VarRestante, double ATilde, double BTilde,
																							double PlusPetitCout, double PlusGrandCout )
{
int CodeRet;  

if ( Pne->NbVariablesSubstituees >= Pne->NombreDeVariablesTrav - 1 ) return( SUBSTITUTION_IMPOSSIBLE );
if ( Pne->NombreDOperationsDePresolve >= Pne->TailleTypeDOperationDePresolve ) {
  /*printf("NombreDOperationsDePresolve %d TailleTypeDOperationDePresolve %d\n",Pne->NombreDOperationsDePresolve,Pne->TailleTypeDOperationDePresolve);*/
  return( SUBSTITUTION_IMPOSSIBLE );
}

# if CONTROLER_LE_CONDITIONNEMENT == NON_PNE
  if ( Mode == SIMULATION ) {
    return( SUBSTITUTION_POSSIBLE ); /* On ne teste pas les nouveaux coefficients */
  }
# endif

if ( Mode == REMPLACEMENT ) {
  Pne->TypeDOperationDePresolve[Pne->NombreDOperationsDePresolve] = SUBSITUTION_DE_VARIABLE;
  Pne->IndexDansLeTypeDOperationDePresolve[Pne->NombreDOperationsDePresolve] = Pne->NbVariablesSubstituees;
  Pne->NombreDOperationsDePresolve++;	/* Verifier si utile */
	
  Pne->IndiceDebutVecteurDeSubstitution [Pne->NbVariablesSubstituees] = Pne->IndexLibreVecteurDeSubstitution;
  Pne->NumeroDesVariablesSubstituees    [Pne->NbVariablesSubstituees] = VarSubstituee;
  Pne->CoutDesVariablesSubstituees      [Pne->NbVariablesSubstituees] = Pne->LTrav[VarSubstituee];	
  Pne->ContrainteDeLaSubstitution       [Pne->NbVariablesSubstituees] = -1;	
  Pne->ValeurDeLaConstanteDeSubstitution[Pne->NbVariablesSubstituees] = BTilde; 
  Pne->NbTermesVecteurDeSubstitution    [Pne->NbVariablesSubstituees] = 1; 
  Pne->CoeffDeSubstitution           [Pne->IndexLibreVecteurDeSubstitution] = -ATilde;  
  Pne->NumeroDeVariableDeSubstitution[Pne->IndexLibreVecteurDeSubstitution] = VarRestante ;  
  Pne->IndexLibreVecteurDeSubstitution++;
  Pne->NbVariablesSubstituees++;
}  

/* On teste ou on effectue la subsitituion dans la matrice des contraintes */
CodeRet = PNE_SubstituerUneVariableDansLaMatrice( Pne, Mode, VarSubstituee, VarRestante, ATilde, BTilde, SIMULATION, REMPLACEMENT,
																						      SUBSTITUTION_POSSIBLE, SUBSTITUTION_IMPOSSIBLE );
if ( Mode == SIMULATION ) {
  if ( CodeRet == SUBSTITUTION_IMPOSSIBLE ) return( CodeRet );
}

/* Substitution dans les coupes de probing */
/* Peut etre ameliore en exploitant un chainage par colonne */
CodeRet = PNE_SubstituerUneVariableDansCoupesDeProbing ( Pne, Mode, VarSubstituee, VarRestante, ATilde, BTilde,SIMULATION, REMPLACEMENT,
																						             SUBSTITUTION_POSSIBLE, SUBSTITUTION_IMPOSSIBLE );																												 
if ( Mode != SIMULATION ) {

  /* Modification du graphe de conflits */
  PNE_SubstituerUneVariableDansGrapheDeConflits( Pne, VarSubstituee );																				
  
	/* Prise en compte des contraintes de bornes variables */
  PNE_BornesVariableSubstituerUneVariable( Pne, VarSubstituee, VarRestante, BTilde );
	
	/* Prise en compte des cliques */
  PNE_SubstituerUneVariableDansCliques( Pne, VarSubstituee, VarRestante, BTilde );
						
}

/* Substitution de la variable: modification des couts */
/* Doit etre place a la fin car modifie le type de variable et le type de borne */
CodeRet = PNE_SubtituerUneVariableUneVariableEntiere( Pne, Mode, VarSubstituee, VarRestante, ATilde, BTilde, PlusPetitCout,  PlusGrandCout,
                                                      SIMULATION, REMPLACEMENT, SUBSTITUTION_POSSIBLE, SUBSTITUTION_IMPOSSIBLE );				
if ( Mode == SIMULATION ) {
  if ( CodeRet == SUBSTITUTION_IMPOSSIBLE ) return( CodeRet );
}

return( SUBSTITUTION_POSSIBLE );

}

/*************************************************************************/


