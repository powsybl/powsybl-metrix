/***********************************************************************

   FONCTION: Construction de la matrice complete pour les besoins
             du raffinement iteratif.
        
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pi_sys.h"

# include "pi_fonctions.h"
# include "pi_define.h"

# include "lu_constantes_externes.h"
# include "lu_definition_arguments.h"

# include "lu_define.h"
# include "lu_fonctions.h"

/*-------------------------------------------------------------------------*/
 
void PI_ReconstruireLaMatriceDuSystemeAResoudre( PROBLEME_PI * Pi )
{
int Var; int Cnt ; int Colonne; int il; int ilMax  ; int ilCourant; double X ; 
char Type; double * Alpha; int NombreDeVariables; int NombreDeContraintes;
double * RegulVar; double * RegulContrainte; double * Qpar2; double * UnSurUkMoinsUmin;
double * UnSurUmaxMoinsUk; double * S1; double * S2; char * TypeDeVariable; double * Alpha2;
int * Ideb; int * Nonu; int * Indl; int * Cdeb;int * CNbTerm; int * NumeroDeContrainte;
int * Mdeb; int * NbTerm; int * Indcol; double * Elm; double * ACol; double * A;

NombreDeVariables   = Pi->NombreDeVariables;
NombreDeContraintes = Pi->NombreDeContraintes;

RegulVar        = Pi->RegulVar;
RegulContrainte = Pi->RegulContrainte;

TypeDeVariable = Pi->TypeDeVariable;

Qpar2 = Pi->Qpar2;
UnSurUkMoinsUmin = Pi->UnSurUkMoinsUmin;
UnSurUmaxMoinsUk = Pi->UnSurUmaxMoinsUk;
S1 = Pi->S1;
S2 = Pi->S2;

Cdeb    = Pi->Cdeb;
CNbTerm = Pi->CNbTerm;
ACol    = Pi->ACol;
NumeroDeContrainte = Pi->NumeroDeContrainte;

Mdeb   = Pi->Mdeb;
NbTerm = Pi->NbTerm;
Indcol = Pi->Indcol;
A      = Pi->A;

Alpha  = Pi->Alpha;
Alpha2 = Pi->Alpha2;

/* RAZ des termes de regularisation */

Ideb = Pi->MatricePi->Ideb;
Nonu = Pi->MatricePi->Nonu;
Elm  = Pi->MatricePi->Elm;
Indl = Pi->MatricePi->Indl;

Pi->MatricePi->NombreDeColonnes = NombreDeVariables + NombreDeContraintes;

ilCourant = 0;
for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  Colonne = Var;
  Ideb[Colonne] = ilCourant;
  Nonu[Colonne] = 0;
  /* Terme diagonal ( hessien ) */
	Type = TypeDeVariable[Var];
  if ( Type == BORNEE ) {
    X = Qpar2[Var] + ( UnSurUkMoinsUmin[Var] * S1[Var] ) + ( UnSurUmaxMoinsUk[Var] * S2[Var] );
  }
  else if ( Type == BORNEE_INFERIEUREMENT ) {
    X = Qpar2[Var] + ( UnSurUkMoinsUmin[Var] * S1[Var] );
  }
  else if ( Type == BORNEE_SUPERIEUREMENT ) {
    X = Qpar2[Var] + ( UnSurUmaxMoinsUk[Var] * S2[Var] );
  }
  else {	
    X = Qpar2[Var]; 
  }	
  Elm [ilCourant] = ( X + RegulVar[Var] ) * Alpha2[Var];		
  Indl[ilCourant] = Var;
  Nonu[Colonne]++;  
  ilCourant++;  
  /* Partie matrice des contraintes, vision transposee */
  il    = Cdeb[Var];
  ilMax = il + CNbTerm[Var];
  while ( il < ilMax ) {
    Elm [ilCourant] = ACol[il] * Alpha[Var];				
    Indl[ilCourant] = NombreDeVariables + NumeroDeContrainte[il];
    Nonu[Colonne]++;
    ilCourant++;
    il++;
  }    
}

/* Matrice des contraintes */
for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  Colonne = NombreDeVariables + Cnt;
  Ideb[Colonne] = ilCourant;
  Nonu[Colonne] = 0;
  
  il = Mdeb[Cnt];
  ilMax = il + NbTerm[Cnt];
  while ( il < ilMax ) {
    Var = Indcol[il];
    Elm [ilCourant] = A[il] * Alpha[Var];				
    Indl[ilCourant] = Var;
    Nonu[Colonne]++;
    ilCourant++;
    il++;
  }
  /* Terme de regularisation si necessaire */
  Elm [ilCourant] = RegulContrainte[Cnt];
  Indl[ilCourant] = Colonne;
  Nonu[Colonne]++;
  ilCourant++;  
}

return;
}
