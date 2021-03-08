
/***********************************************************************************

   FONCTION: 

                   
   AUTEUR: R. GONZALEZ

************************************************************************************/

# include "pi_sys.h"

# include "pi_fonctions.h"
# include "pi_define.h"

/*------------------------------------------------------------------------*/
/*        Restitution des resultats dans la numerotation d'entree         */ 

void PI_RestitutionDesResultats( PROBLEME_PI * Pi,
                                 int     NombreDeVariables_E   , 
                                 double * U_E                   , 
                                 double * S1_E                  , 
                                 double * S2_E                  , 
				 int     NombreDeContraintes_E ,
                                 double * VariablesDualesDeContraintes )
{
int i; int Var_E; int Cnt_E;  

for ( i = 0 ; i < NombreDeVariables_E ; i++ ) {
  Var_E = Pi->CorrespondanceVarEntreeVarPi[i];
  if ( Var_E >= 0 ) {
    U_E[i]  = Pi->U[Var_E];
    /*
    S1_E[i] = Pi->S1[Var_E];
    S2_E[i] = Pi->S2[Var_E];
    */
  }
}


for ( i = 0 ; i <  NombreDeContraintes_E ; i++ ) {
  VariablesDualesDeContraintes[i] = 0;
  Cnt_E = Pi->CorrespondanceCntEntreeCntPi[i];
  if ( Cnt_E >= 0 ) {
     VariablesDualesDeContraintes[i] = Cnt_E;
  }				  
}

return;
}	       

