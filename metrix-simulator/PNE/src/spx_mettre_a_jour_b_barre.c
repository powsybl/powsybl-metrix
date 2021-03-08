/***********************************************************************

   FONCTION: Mise de BBarre = B^{-1} * b 

                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"  

# include "spx_fonctions.h"
# include "spx_define.h"

# include "lu_define.h"
# include "lu_fonctions.h"

/*----------------------------------------------------------------------------*/
/* On tente un calcul en hyper creux s'il y a des chances de succes */
void SPX_CalculerBsHyperCreux( PROBLEME_SPX * Spx, char * ResoudreLeSysteme, char * T,
                               int * IndexTermesNonNuls, int * NbTNonNuls )
{
int il; int ilMax; int i; int Var; double Xmx; char Flag; double * Bs;
int * BoundFlip; double * Xmax; int * NumeroDeContrainte;
double * ACol; int * Cdeb; int * CNbTerm; int NbTermesNonNuls; int k;

NbTermesNonNuls = 0;	
Bs        = Spx->Bs;
BoundFlip = Spx->BoundFlip;
Xmax      = Spx->Xmax;

Cdeb               = Spx->Cdeb;
CNbTerm            = Spx->CNbTerm;
NumeroDeContrainte = Spx->NumeroDeContrainte;
ACol               = Spx->ACol;

Flag = NON_SPX;
for ( i = 0 ; i < Spx->NbBoundFlip; i++ ) { 
  Var = BoundFlip[i];
	if ( BoundFlip[i] > 0 ) {
	  Var = BoundFlip[i] - 1;
		Xmx = Xmax[Var];	
	}
	else {
	  Var = -BoundFlip[i] - 1;
		Xmx = -Xmax[Var];	
	}
  if ( Xmx == 0.0 ) continue;
  Flag  = OUI_SPX;
  il    = Cdeb[Var];
  ilMax = il + CNbTerm[Var];
  while ( il < ilMax ) {
	  k = NumeroDeContrainte[il];
    Bs[k]+= Xmx * ACol[il];		  
		if ( T[k] == 0 ) {
      T[k] = 1;
			IndexTermesNonNuls[NbTermesNonNuls] = k;
			NbTermesNonNuls++;				
		}			
    il++;
  }
}

*ResoudreLeSysteme = Flag;
*NbTNonNuls        = NbTermesNonNuls;
return;
}

/*----------------------------------------------------------------------------*/
void SPX_CalculerBsStandard( PROBLEME_SPX * Spx , char * ResoudreLeSysteme )
{

int il; int ilMax; int i; int Var; double Xmx; char Flag; double * Bs;
int * BoundFlip; double * Xmax; int * NumeroDeContrainte;
double * ACol; int * Cdeb; int * CNbTerm; 

Bs         = Spx->Bs;
BoundFlip  = Spx->BoundFlip;
Xmax       = Spx->Xmax;

Cdeb               = Spx->Cdeb;
CNbTerm            = Spx->CNbTerm;
NumeroDeContrainte = Spx->NumeroDeContrainte;
ACol               = Spx->ACol;

Flag = NON_SPX;
for ( i = 0 ; i < Spx->NbBoundFlip; i++ ) { 
  Var = BoundFlip[i];
	if ( BoundFlip[i] > 0 ) {
	  Var = BoundFlip[i] - 1;
		Xmx = Xmax[Var];	
	}
	else {
	  Var = -BoundFlip[i] - 1;
		Xmx = -Xmax[Var];	
	}
  if ( Xmx == 0.0 ) continue;
  Flag  = OUI_SPX;
  il    = Cdeb[Var];
  ilMax = il + CNbTerm[Var];
  while ( il < ilMax ) {
    Bs[NumeroDeContrainte[il]]+= Xmx * ACol[il];		
    il++;
  }
}
	
*ResoudreLeSysteme = Flag; 

return;
}

/*----------------------------------------------------------------------------*/
/*                    Cas de la mise a jour de BBarre                         */

void SPX_MettreAJourBBarre( PROBLEME_SPX * Spx )
{
int Cnt; int i; char Save; char SecondMembreCreux; double * Bs; double * BBarre;
double * ABarreS; int * CntDeABarreSNonNuls; double DeltaXSurLaVariableHorsBase;
char ResoudreLeSysteme; char * T; int * IndexTermesNonNuls;
int NbTermesNonNuls; char TypeDEntree; char TypeDeSortie; int CntBase;
int NbBBarreModifies; int * IndexDeBBarreModifies; char ResolutionEnHyperCreux;
char StockageDeBs; char HyperCreuxInitial;
  
Bs = Spx->Bs;

/* Bs est toujours a 0 des qu'on a fini de s'en servir */
/*memset( (char *) Bs , 0 , Spx->NombreDeContraintes * sizeof( double ) );*/
/*for ( i = 0 ; i < Spx->NombreDeContraintes ; i++ ) Bs[i] = 0.0;*/

T = Spx->T;
IndexTermesNonNuls = (int *) Spx->ErBMoinsUn;

if ( Spx->CalculABarreSEnHyperCreux == OUI_SPX ) ResolutionEnHyperCreux = OUI_SPX;
else ResolutionEnHyperCreux = NON_SPX;  

if ( Spx->TypeDeCreuxDeLaBase == BASE_HYPER_CREUSE && ResolutionEnHyperCreux == OUI_SPX ) {
  SPX_CalculerBsHyperCreux( Spx , &ResoudreLeSysteme, T, IndexTermesNonNuls, &NbTermesNonNuls );
	StockageDeBs = ADRESSAGE_INDIRECT_SPX;	
}
else {
  SPX_CalculerBsStandard( Spx , &ResoudreLeSysteme );
	StockageDeBs = VECTEUR_SPX;
	ResolutionEnHyperCreux = NON_SPX;
}
HyperCreuxInitial = ResolutionEnHyperCreux;

ABarreS = Spx->ABarreS;
BBarre  = Spx->BBarre;

if ( ResoudreLeSysteme == OUI_SPX ) {
  Save = NON_LU;
  SecondMembreCreux = OUI_LU;

	TypeDEntree  = ADRESSAGE_INDIRECT_LU;
	TypeDeSortie = COMPACT_LU;      
	
	if ( ResolutionEnHyperCreux == OUI_SPX ) {	
    for ( i = 0 ; i < NbTermesNonNuls ; i++ ) T[IndexTermesNonNuls[i]] = 0;	 		
	  if ( NbTermesNonNuls >= ceil(  TAUX_DE_REMPLISSAGE_POUR_VECTEUR_HYPER_CREUX * Spx->NombreDeContraintes ) ) {
		  ResolutionEnHyperCreux = NON_SPX;
	    StockageDeBs = VECTEUR_SPX;				
		}	 		
	}
	
  SPX_ResoudreBYegalA( Spx, TypeDEntree, Bs, IndexTermesNonNuls, &NbTermesNonNuls, &TypeDeSortie,
                       ResolutionEnHyperCreux, Save, SecondMembreCreux );
											 
  if ( ResolutionEnHyperCreux == OUI_SPX ) {
    if ( TypeDeSortie != COMPACT_LU ) {		
      StockageDeBs = VECTEUR_SPX;
    }		
  }
														 
}

/* Attention en realite StockageDeBs est soit COMPACT_LU ou VECTEUR_SPX */
/* Et on remarque que COMPACT_LU est ici equivalent a ADRESSAGE_INDIRECT_SPX */

DeltaXSurLaVariableHorsBase = Spx->DeltaXSurLaVariableHorsBase;
NbBBarreModifies      = 0;
IndexDeBBarreModifies = (int *) Spx->V;

if ( Spx->TypeDeStockageDeABarreS == COMPACT_SPX ) {
  CntDeABarreSNonNuls = Spx->CntDeABarreSNonNuls;	
  for ( i = 0 ; i < Spx->NbABarreSNonNuls ; i++ ) {
	  Cnt = CntDeABarreSNonNuls[i];
		BBarre[Cnt]-= DeltaXSurLaVariableHorsBase * ABarreS[i];
		T[Cnt] = 1;
		IndexDeBBarreModifies[NbBBarreModifies] = Cnt;
		NbBBarreModifies++;
	}
	if ( StockageDeBs == ADRESSAGE_INDIRECT_SPX ) {	
    for ( i = 0 ; i < NbTermesNonNuls ; i++ ) {
		  Cnt = IndexTermesNonNuls[i];
			BBarre[Cnt]-= Bs[i];
			
			Bs[i] = 0;
			
			if ( T[Cnt] == 0 ) {			
			  T[Cnt] = 1;
		    IndexDeBBarreModifies[NbBBarreModifies] = Cnt;
		    NbBBarreModifies++;			
			}			
		}		
	}
	else {
    for ( Cnt = 0 ; Cnt < Spx->NombreDeContraintes ; Cnt++ ) {
		  if ( Bs[Cnt] != 0.0 ) {
			  BBarre[Cnt]-= Bs[Cnt];

				Bs[Cnt] = 0;
				
			  if ( T[Cnt] == 0 ) {							
				  T[Cnt] = 1;
		      IndexDeBBarreModifies[NbBBarreModifies] = Cnt;
		      NbBBarreModifies++;
				}
			}
		}
	}
}
else {
	if ( StockageDeBs == ADRESSAGE_INDIRECT_SPX ) {
    for ( Cnt = 0 ; Cnt < Spx->NombreDeContraintes ; Cnt++ ) {
		  if ( ABarreS[Cnt] != 0.0 ) {
		    BBarre[Cnt]-= DeltaXSurLaVariableHorsBase * ABarreS[Cnt];
			  T[Cnt] = 1;
		    IndexDeBBarreModifies[NbBBarreModifies] = Cnt;
		    NbBBarreModifies++;				
			}
		}
    for ( i = 0 ; i < NbTermesNonNuls ; i++ ) {
		  Cnt = IndexTermesNonNuls[i];
			BBarre[Cnt]-= Bs[i];

			Bs[i] = 0;
			
			if ( T[Cnt] == 0 ) {							
				T[Cnt] = 1;
		    IndexDeBBarreModifies[NbBBarreModifies] = Cnt;
		    NbBBarreModifies++;
			}		
		}				
  }
	else {
    for ( Cnt = 0 ; Cnt < Spx->NombreDeContraintes ; Cnt++ ) {
	    if ( Bs[Cnt] != 0.0 || ABarreS[Cnt] != 0.0 ) {
        BBarre[Cnt]-= Bs[Cnt] + ( DeltaXSurLaVariableHorsBase * ABarreS[Cnt] );

				Bs[Cnt] = 0;
				
		    T[Cnt] = 1;
		    IndexDeBBarreModifies[NbBBarreModifies] = Cnt;
		    NbBBarreModifies++;				
		  }
    }
	}
}

/* Cas particulier des variables echangees */
if ( Spx->ChangementDeBase == OUI_SPX ) {
  CntBase = Spx->ContrainteDeLaVariableEnBase[Spx->VariableSortante];
  if ( Spx->PositionDeLaVariable[Spx->VariableEntrante] == HORS_BASE_SUR_BORNE_INF || 
       Spx->PositionDeLaVariable[Spx->VariableEntrante] == HORS_BASE_A_ZERO ) {			 
    BBarre[CntBase] = DeltaXSurLaVariableHorsBase;
		if ( T[CntBase] == 0 ) {							
			T[CntBase] = 1;
		  IndexDeBBarreModifies[NbBBarreModifies] = CntBase;
		  NbBBarreModifies++;
		}				
  }
  else {	
    BBarre[CntBase] = DeltaXSurLaVariableHorsBase + Spx->Xmax[Spx->VariableEntrante];		
		if ( T[CntBase] == 0 ) {							
			T[CntBase] = 1;
		  IndexDeBBarreModifies[NbBBarreModifies] = CntBase;
		  NbBBarreModifies++;
		}				
  }
}

Spx->BBarreAEteCalculeParMiseAJour = OUI_SPX;

SPX_MajDesVariablesEnBaseAControler( Spx, NbBBarreModifies, IndexDeBBarreModifies, T );

return;
}   

