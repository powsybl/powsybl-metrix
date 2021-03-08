
/***********************************************************************

   FONCTION: A chaque iteration de point interiuer, calcul de delta S1
             et delta S2 (ce calcul est fait apres celui de delta U).
   
        
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pi_sys.h"

# include "pi_fonctions.h"
# include "pi_define.h"

/*-------------------------------------------------------------------------*/

void PI_Sos1s2( PROBLEME_PI * Pi )
{
int i; int NombreDeVariables; char * TypeDeVariable; double * DeltaS1;
double * DeltaS2; double * UnSurUkMoinsUmin ; double * UnSurUmaxMoinsUk;
double * S1; double * S2; double * DeltaU; double * DeltaUDeltaS1;
double * DeltaUDeltaS2; double Muk; char Type;

NombreDeVariables = Pi->NombreDeVariables;
TypeDeVariable    = Pi->TypeDeVariable;

DeltaS1 = Pi->DeltaS1;
DeltaS2 = Pi->DeltaS2;

UnSurUkMoinsUmin = Pi->UnSurUkMoinsUmin;
UnSurUmaxMoinsUk = Pi->UnSurUmaxMoinsUk;
S1 = Pi->S1;
S2 = Pi->S2;
DeltaU = Pi->DeltaU;
DeltaUDeltaS1 = Pi->DeltaUDeltaS1;
DeltaUDeltaS2 = Pi->DeltaUDeltaS2;

if ( Pi->NumeroDIteration <= 1 ) {
  for ( i = 0 ; i < NombreDeVariables ; i++ ) {
    DeltaS1[i] = 0.; 
    DeltaS2[i] = 0.; 	
	}
}
 
if ( Pi->TypeDIteration == AFFINE ) {
  for ( i = 0 ; i < NombreDeVariables ; i++ ) {
	  Type = TypeDeVariable[i];
    if ( Type == BORNEE ) {
      DeltaS1[i] = ( - UnSurUkMoinsUmin[i] * S1[i] * DeltaU[i] ) - S1[i];
      DeltaS2[i] = (   UnSurUmaxMoinsUk[i] * S2[i] * DeltaU[i] ) - S2[i];
    }
    else if ( Type == BORNEE_INFERIEUREMENT ) {
      DeltaS1[i] = ( - UnSurUkMoinsUmin[i] * S1[i] * DeltaU[i] ) - S1[i];
    }
    else if ( Type == BORNEE_SUPERIEUREMENT ) {
      DeltaS2[i] = ( UnSurUmaxMoinsUk[i] * S2[i] * DeltaU[i] ) - S2[i];
    }  
  }
}
else {
  Muk = Pi->Muk;
  for ( i = 0 ; i < NombreDeVariables ; i++ ) {	
	  Type = TypeDeVariable[i];
    if ( Type == BORNEE ) {
      DeltaS1[i] = UnSurUkMoinsUmin[i] * ( Muk - ( S1[i] * DeltaU[i] ) - 
                   DeltaUDeltaS1[i] ) - S1[i];    
      DeltaS2[i] = UnSurUmaxMoinsUk[i] * ( Muk + ( S2[i] * DeltaU[i] ) + 
                   DeltaUDeltaS2[i] ) - S2[i];
    }
    else if ( Type == BORNEE_INFERIEUREMENT ) {
      DeltaS1[i] = UnSurUkMoinsUmin[i] * ( Muk - ( S1[i] * DeltaU[i] ) - 
                   DeltaUDeltaS1[i] ) - S1[i];        
    }
    else if ( Type == BORNEE_SUPERIEUREMENT ) {
      DeltaS2[i] = UnSurUmaxMoinsUk[i] * ( Muk + ( S2[i] * DeltaU[i] ) + 
                   DeltaUDeltaS2[i] ) - S2[i]; 
    }  
  }
}

return;
}




