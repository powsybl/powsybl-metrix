/***********************************************************************

   FONCTION: Translater les bornes en sortie pour tout remettre     
             en ordre
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"

/*----------------------------------------------------------------------------*/

void SPX_SauvegardesBranchAndBoundAndCut( PROBLEME_SPX * Spx )
{
	int Cnt; int Var; /*int i;*/ 

/* Sauvegardes pour le strong branching */

for ( Var = 0 ; Var < Spx->NombreDeVariables ; Var++ ) {
  Spx->XSV                           [Var] = Spx->X[Var];
  Spx->PositionDeLaVariableSV        [Var] = Spx->PositionDeLaVariable[Var];
  Spx->CBarreSV                      [Var] = Spx->CBarre[Var];    
  Spx->InDualFrameworkSV             [Var] = Spx->InDualFramework[Var];  
  Spx->ContrainteDeLaVariableEnBaseSV[Var] = Spx->ContrainteDeLaVariableEnBase[Var];
}

for ( Cnt = 0 ; Cnt < Spx->NombreDeContraintes ; Cnt++ ) {   
  Spx->BBarreSV                      [Cnt] = Spx->BBarre[Cnt]; 
  Spx->DualPoidsSV                   [Cnt] = Spx->DualPoids[Cnt]; 
  Spx->VariableEnBaseDeLaContrainteSV[Cnt] = Spx->VariableEnBaseDeLaContrainte[Cnt];
  Spx->CdebBaseSV                    [Cnt] = Spx->CdebBase[Cnt];
  Spx->NbTermesDesColonnesDeLaBaseSV [Cnt] = Spx->NbTermesDesColonnesDeLaBase[Cnt]; 
}

return;
}



















