/***********************************************************************

   FONCTION: On ordonne les stockage des matrices dans l'ordre croissant
	           des indices.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"
 
# include "spx_fonctions.h"

int SPX_PartitionOrdreCroissant( double * , int * , int , int );
void SPX_ClasserVecteurDansOrdreCroissant( double * , int * , int , int );

/*----------------------------------------------------------------------------*/

int SPX_PartitionOrdreCroissant( double * A, int * NumColonneOuNumLigne, int Debut, int Fin )
{
int Compt; int Pivot; int i; double a; int v;
Compt = Debut;
Pivot = NumColonneOuNumLigne[Debut];
/* Ordre croissant */
for ( i = Debut + 1 ; i <= Fin ; i++) {		
  if ( NumColonneOuNumLigne[i] < Pivot) {
    Compt++;		
    a = A[Compt];
    A[Compt] = A[i];
		A[i] = a;
		v = NumColonneOuNumLigne[Compt];
		NumColonneOuNumLigne[Compt] = NumColonneOuNumLigne[i];
		NumColonneOuNumLigne[i] = v;
  }
}	
a = A[Compt];
A[Compt] = A[Debut];
A[Debut] = a;
v = NumColonneOuNumLigne[Compt];
NumColonneOuNumLigne[Compt] = NumColonneOuNumLigne[Debut];
NumColonneOuNumLigne[Debut] = v;
return(Compt);
}

/*----------------------------------------------------------------------------*/

void SPX_ClasserVecteurDansOrdreCroissant( double * A, int * NumColonneOuNumLigne, int Debut, int Fin )
{
int Pivot;
if ( Debut < Fin ) {
  Pivot = SPX_PartitionOrdreCroissant( A, NumColonneOuNumLigne, Debut, Fin );
  SPX_ClasserVecteurDansOrdreCroissant( A, NumColonneOuNumLigne, Debut, Pivot-1 );
  SPX_ClasserVecteurDansOrdreCroissant( A, NumColonneOuNumLigne, Pivot+1, Fin );
}
return;
}

/*----------------------------------------------------------------------------*/

void SPX_OrdonnerMatriceDesContraintes( int NombreDeVecteurs, int * IndexDebut, int * NbTermes,
                                        int * NumColonneOuNumLigne, double * A )
{
int Vecteur; int Debut; int Fin; 
for ( Vecteur = 0 ; Vecteur < NombreDeVecteurs ; Vecteur++ ) { 
	if ( NbTermes[Vecteur] <= 0 ) continue;
	Debut = IndexDebut[Vecteur];
	if ( Debut < 0 ) continue;
	Fin = Debut + NbTermes[Vecteur] - 1;
  SPX_ClasserVecteurDansOrdreCroissant( A, NumColonneOuNumLigne, Debut, Fin );	
}

return;
}

