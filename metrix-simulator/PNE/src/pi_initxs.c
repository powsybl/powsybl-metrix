/***********************************************************************************

   FONCTION:    

                     
   AUTEUR: R. GONZALEZ

************************************************************************************/

# include "pi_sys.h"

# include "pi_fonctions.h"
# include "pi_define.h"

/*------------------------------------------------------------------------*/
/*                     Initialisation des variables                       */

void PI_InitXS( PROBLEME_PI * Pi )     
{
int Var; double Div;

for ( Var = 0 ; Var < Pi->NombreDeVariables ; Var++ ) {

  Pi->Qpar2[Var] = 2. * Pi->Q[Var];

  Pi->UkMoinsUmin[Var] = Pi->U[Var]    - Pi->Umin[Var];
  Pi->UmaxMoinsUk[Var] = Pi->Umax[Var] - Pi->U[Var]; 

  Div = Pi->UkMoinsUmin[Var];
  if ( Div < ZERO ) Div = ZERO;
  Pi->UnSurUkMoinsUmin[Var] = 1. / Div;
  Div = Pi->UmaxMoinsUk[Var];
  if ( Div < ZERO ) Div = ZERO;
  Pi->UnSurUmaxMoinsUk[Var] = 1. / Div;

  if ( Pi->TypeDeVariable[Var] == BORNEE_INFERIEUREMENT ) {
    Pi->UnSurUmaxMoinsUk[Var] = 0.;
    Pi->UmaxMoinsUk     [Var] = 0.;     
  }
  else if ( Pi->TypeDeVariable[Var] == BORNEE_SUPERIEUREMENT ) {
    Pi->UnSurUkMoinsUmin[Var] = 0.;
    Pi->UkMoinsUmin     [Var] = 0.; 
  }
  else if ( Pi->TypeDeVariable[Var] == NON_BORNEE ) {
    Pi->UnSurUkMoinsUmin[Var] = 0.;
    Pi->UnSurUmaxMoinsUk[Var] = 0.;
    Pi->UkMoinsUmin     [Var] = 0.; 
    Pi->UmaxMoinsUk     [Var] = 0.; 
  }
  
}

return;
}

