/***********************************************************************
   FONCTION: Modification du probleme. On permet de changer le vecteur
             des couts ou bien celui du second membre.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"
 
# include "spx_fonctions.h"
# include "spx_define.h"

/*----------------------------------------------------------------------------*/

void SPX_ModifierLeVecteurCouts( PROBLEME_SPX * Spx , double * C_E , int NbVar_E ) 
{
int Var_E  ; int Var; double Cout; int * NumeroDesVariablesACoutNonNul; int il;
double * C; double * Csv; double * ScaleX; double ScaleLigneDesCouts;
int NombreDeVariablesDuProblemeSansCoupes; int * CorrespondanceVarEntreeVarSimplexe;

CorrespondanceVarEntreeVarSimplexe = Spx->CorrespondanceVarEntreeVarSimplexe;
NombreDeVariablesDuProblemeSansCoupes = Spx->NombreDeVariablesDuProblemeSansCoupes;
C = Spx->C ;
Csv = Spx->Csv;
ScaleX = Spx->ScaleX;
ScaleLigneDesCouts = Spx->ScaleLigneDesCouts;	
for ( Var_E = 0 ; Var_E < NbVar_E ; Var_E++ ) {
  Var = CorrespondanceVarEntreeVarSimplexe[Var_E];
  if ( Var < 0 || Var >= NombreDeVariablesDuProblemeSansCoupes ) continue;
    Cout = C_E[Var_E] * ScaleX[Var] * ScaleLigneDesCouts;		
    Csv[Var] = Cout;
    C  [Var] = Cout;
}

il = 0;
NumeroDesVariablesACoutNonNul = Spx->NumeroDesVariablesACoutNonNul;
for ( Var = 0 ; Var < Spx->NombreDeVariables ; Var++ ) {
  if ( C[Var] != 0 ) {
	  NumeroDesVariablesACoutNonNul[il] = Var;
		il++;
	}
}
Spx->NombreDeVariablesACoutNonNul = il;

return;
}

/*----------------------------------------------------------------------------*/
/* Modification du second membre apres scaling mais avant translation         */
/* Attention, cela suppose qu'il n'y ait pas de variables de type VARIABLE_FIXE
   dans le probleme d'entree car seul est conserve BAvantTranslationEtApresScaling */
void SPX_ModifierLeVecteurSecondMembre( PROBLEME_SPX * Spx , double * B_E , char * Sens_E, int NbCnt_E )
{
int Cnt_E; int Cnt; double Coeff;

/* Attention, ne marche pas s'il y a des variables fixes: il faut que les variables fixes soient 
	 exprimees de la facon suivante : xmin = xmax */

for ( Cnt_E = 0 ; Cnt_E < NbCnt_E ; Cnt_E++ ) {
  Cnt = Spx->CorrespondanceCntEntreeCntSimplexe[Cnt_E];	
  if ( Cnt < 0 || Cnt >= Spx->NombreDeContraintesDuProblemeSansCoupes ) continue;
  /* Spx->B[Cnt] est mis a jour plus tard en fonction du vecteur ci-dessous */
	Coeff = 1.;
	if ( Sens_E[Cnt_E] == '>' ) Coeff = -1.0;
  Spx->BAvantTranslationEtApresScaling[Cnt] = Coeff * B_E[Cnt_E]  * Spx->ScaleB[Cnt];		
}

return;
}
