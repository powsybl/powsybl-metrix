/***********************************************************************
   FONCTION: Gestion des listes de variables hors base et des bornes
	           sur les variables en base.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"
  
/*----------------------------------------------------------------------------*/

void SPX_InitialiserLeTableauDesVariablesHorsBase( PROBLEME_SPX * Spx ) 
{
int Var; char * PositionDeLaVariable; 
int * NumerosDesVariablesHorsBase   ; int NombreDeVariablesHorsBase;
int * IndexDeLaVariableDansLesVariablesHorsBase; int NombreDeVariables;
# ifdef ENLEVER_LES_VARIABLES_XMIN_EGAL_XMAX_DE_LA_LISTE_HORS_BASE
  char * InDualFramework; double * Xmin; double * Xmax;
	char LeSteepestEdgeEstInitilise;
# endif

NombreDeVariables = Spx->NombreDeVariables;
PositionDeLaVariable = Spx->PositionDeLaVariable;

NumerosDesVariablesHorsBase = Spx->NumerosDesVariablesHorsBase;
IndexDeLaVariableDansLesVariablesHorsBase = Spx->IndexDeLaVariableDansLesVariablesHorsBase;

NombreDeVariablesHorsBase = 0;

for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) IndexDeLaVariableDansLesVariablesHorsBase[Var] = -1;

Spx->PresenceDeVariablesDeBornesIdentiques = NON_SPX;

# ifdef ENLEVER_LES_VARIABLES_XMIN_EGAL_XMAX_DE_LA_LISTE_HORS_BASE
  Xmin = Spx->Xmin;
  Xmax = Spx->Xmax;
  InDualFramework = Spx->InDualFramework;
	LeSteepestEdgeEstInitilise = Spx->LeSteepestEdgeEstInitilise;	
  if ( LeSteepestEdgeEstInitilise == OUI_SPX ) {
    for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
		  if ( Spx->PresenceDeVariablesDeBornesIdentiques == NON_SPX ) {
			  if ( Xmin[Var] == Xmax[Var] ) Spx->PresenceDeVariablesDeBornesIdentiques = OUI_SPX;        
			}
      if ( PositionDeLaVariable[Var] != EN_BASE_LIBRE ) {		
        /* Si la variable a Xmin = Xmax (c'est a dire 0) on ne la met pas dans la liste.
           Et alors, soit il y a une solution avec le probleme reduit et alors elle
	         est optimale, soit il n'y en a pas et il n'y en aurait de toutes facons pas
	         meme si la variable n'avait pas ete enlevee de la liste */
			  /* Noter que si la variable etait dans le framework du steepest edge, on ne sait pas
			     la faire sortir proprement. Cela entrainera donc une reinitialisation du
				   steepest edge a un moment donne. Mais les essais numeriques montrent que c'est
				   malgre tout une facon de faire qui accelere les temps de calcul */				 	 
        if ( InDualFramework[Var] == OUI_SPX ) {
          NumerosDesVariablesHorsBase[NombreDeVariablesHorsBase] = Var;
	 	      IndexDeLaVariableDansLesVariablesHorsBase[Var] = NombreDeVariablesHorsBase;
          NombreDeVariablesHorsBase++;				  
				}
        else if ( Xmin[Var] != Xmax[Var] ) {
          NumerosDesVariablesHorsBase[NombreDeVariablesHorsBase] = Var;
	 	      IndexDeLaVariableDansLesVariablesHorsBase[Var] = NombreDeVariablesHorsBase;
          NombreDeVariablesHorsBase++;
		    }
			}
    }
	}
	else {
    for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
		  if ( Spx->PresenceDeVariablesDeBornesIdentiques == NON_SPX ) {
			  if ( Xmin[Var] == Xmax[Var] ) Spx->PresenceDeVariablesDeBornesIdentiques = OUI_SPX;        
			}		
      if ( PositionDeLaVariable[Var] != EN_BASE_LIBRE ) {		
      /* Si la variable a Xmin = Xmax (c'est a dire 0) on ne la met pas dans la liste.
         Et alors, soit il y a une solution avec le probleme reduit et alors elle
	       est optimale, soit il n'y en a pas et il n'y en aurait de toutes facons pas
	       meme si la variable n'avait pas ete enlevee de la liste */
			/* Noter que si la variable etait dans le framework du steepest edge, on ne sait pas
			   la faire sortir proprement. Cela entrainera donc une reinitialisation du
				 steepest edge a un moment donne. Mais les essais numeriques montrent que c'est
				 malgre tout une facon de faire qui accelere les temps de calcul */				 	 
        if ( Xmin[Var] != Xmax[Var] ) {
          NumerosDesVariablesHorsBase[NombreDeVariablesHorsBase] = Var;
	 	      IndexDeLaVariableDansLesVariablesHorsBase[Var] = NombreDeVariablesHorsBase;
          NombreDeVariablesHorsBase++;
		    }
		  }  	
	  }
	}
# else
  for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
    if ( PositionDeLaVariable[Var] != EN_BASE_LIBRE ) {		
      NumerosDesVariablesHorsBase[NombreDeVariablesHorsBase] = Var;
		  IndexDeLaVariableDansLesVariablesHorsBase[Var] = NombreDeVariablesHorsBase;
      NombreDeVariablesHorsBase++;
		}
  }		
# endif
Spx->NombreDeVariablesHorsBase = NombreDeVariablesHorsBase;
return;
}

/*----------------------------------------------------------------------------*/

void SPX_MajDuTableauDesVariablesHorsBase( PROBLEME_SPX * Spx ) 
{
int i; int j; int Var; int VariableEntrante; int VariableSortante;
int * IndexDeLaVariableDansLesVariablesHorsBase; int * NumerosDesVariablesHorsBase;
double * Xmin; double * Xmax;

VariableEntrante = Spx->VariableEntrante;
VariableSortante = Spx->VariableSortante;
IndexDeLaVariableDansLesVariablesHorsBase = Spx->IndexDeLaVariableDansLesVariablesHorsBase;
NumerosDesVariablesHorsBase               = Spx->NumerosDesVariablesHorsBase;
Xmin = Spx->Xmin;
Xmax = Spx->Xmax;

i = IndexDeLaVariableDansLesVariablesHorsBase[VariableEntrante];
NumerosDesVariablesHorsBase[i] = VariableSortante;

#ifdef ENLEVER_LES_VARIABLES_XMIN_EGAL_XMAX_DE_LA_LISTE_HORS_BASE
  /* Si la variable sortante a Xmin = Xmax (c'est a dire 0) on la sort de la liste.
     Et alors, soit il y a une solution avec le probleme reduit et alors elle
	   est optimale, soit il n'y en a pas et il n'y en aurait de toutes facons pas
	   meme si la variable n'avait pas ete enlevee de la liste */
  if ( Xmin[VariableSortante] != Xmax[VariableSortante] ) {
    IndexDeLaVariableDansLesVariablesHorsBase[VariableSortante] = i;
    IndexDeLaVariableDansLesVariablesHorsBase[VariableEntrante] = -1;				
  }
  else {		
    IndexDeLaVariableDansLesVariablesHorsBase[VariableSortante] = -1;		
    IndexDeLaVariableDansLesVariablesHorsBase[VariableEntrante] = -1;		
		j = Spx->NombreDeVariablesHorsBase - 1;		
		if ( j != i ) {
	    Var = NumerosDesVariablesHorsBase[j];
		  IndexDeLaVariableDansLesVariablesHorsBase[Var] = i;		
      NumerosDesVariablesHorsBase              [i]   = Var;			
		}
    Spx->NombreDeVariablesHorsBase--;		
  }
#else
  IndexDeLaVariableDansLesVariablesHorsBase[VariableSortante] = i;
  IndexDeLaVariableDansLesVariablesHorsBase[VariableEntrante] = -1;
#endif

return;
}

/*----------------------------------------------------------------------------*/

void SPX_InitialiserLesVariablesEnBaseAControler( PROBLEME_SPX * Spx ) 
{
int Var; int Cnt; int NbSurv; double X;
double * BorneInfInfaisabilitePrimale  ; double * BorneSupInfaisabilitePrimale;
int * VariableEnBaseDeLaContrainte    ; char * TypeDeVariable; double * Xmax ;
double * SeuilDeViolationDeBorne       ; double * ValeurDeViolationDeBorne    ;
double * DualPoids                     ; int * IndexDansContrainteASurveiller;
int * NumerosDesContraintesASurveiller; double * BBarre; double ValBBarre    ;

BorneInfInfaisabilitePrimale = Spx->BorneInfInfaisabilitePrimale;
BorneSupInfaisabilitePrimale = Spx->BorneSupInfaisabilitePrimale;
VariableEnBaseDeLaContrainte = Spx->VariableEnBaseDeLaContrainte;
TypeDeVariable               = Spx->TypeDeVariable;
SeuilDeViolationDeBorne      = Spx->SeuilDeViolationDeBorne;
Xmax                         = Spx->Xmax;

BBarre                           = Spx->BBarre;
DualPoids                        = Spx->DualPoids;
ValeurDeViolationDeBorne         = Spx->ValeurDeViolationDeBorne;
IndexDansContrainteASurveiller   = Spx->IndexDansContrainteASurveiller;
NumerosDesContraintesASurveiller = Spx->NumerosDesContraintesASurveiller;
NbSurv = 0;

for ( Cnt = 0 ; Cnt < Spx->NombreDeContraintes ; Cnt++ ) IndexDansContrainteASurveiller[Cnt] = -1;

for ( Cnt = 0 ; Cnt < Spx->NombreDeContraintes ; Cnt++ ) {
  Var = VariableEnBaseDeLaContrainte[Cnt];	
	/* Initialisation des bornes et constitution de la liste */
  if ( TypeDeVariable[Var] == NON_BORNEE ) {
	  /* Variable non bornee */
    BorneInfInfaisabilitePrimale[Cnt] = -LINFINI_SPX;
	  BorneSupInfaisabilitePrimale[Cnt] = LINFINI_SPX;
	}
  else {
	  ValBBarre = BBarre[Cnt];
	  if ( TypeDeVariable[Var] == BORNEE_INFERIEUREMENT ) {
      BorneInfInfaisabilitePrimale[Cnt] = -SeuilDeViolationDeBorne[Var];
	    BorneSupInfaisabilitePrimale[Cnt] = LINFINI_SPX;
      if ( ValBBarre < BorneInfInfaisabilitePrimale[Cnt] ) {
		    /* On ajoute la contrainte dans la liste */		
		    NumerosDesContraintesASurveiller[NbSurv] = Cnt;
		    ValeurDeViolationDeBorne        [NbSurv] = ( ValBBarre * ValBBarre ) / DualPoids[Cnt];		
		    IndexDansContrainteASurveiller[Cnt] = NbSurv;
		    NbSurv++;		 
	    }		
	  }
    else {
		  /* La variable est bornee */
      BorneInfInfaisabilitePrimale[Cnt] = -SeuilDeViolationDeBorne[Var];
	    BorneSupInfaisabilitePrimale[Cnt] = Xmax[Var] + SeuilDeViolationDeBorne[Var];
      if ( ValBBarre < BorneInfInfaisabilitePrimale[Cnt] ) {
		    /* On ajoute la contrainte dans la liste */		
		    NumerosDesContraintesASurveiller[NbSurv] = Cnt;
		    ValeurDeViolationDeBorne        [NbSurv] = ( ValBBarre * ValBBarre ) / DualPoids[Cnt];		
		    IndexDansContrainteASurveiller[Cnt] = NbSurv;
		    NbSurv++;		 
	    }				
	    else if ( ValBBarre > BorneSupInfaisabilitePrimale[Cnt] ) {
		    /* On ajoute la contrainte dans la liste */
		    NumerosDesContraintesASurveiller[NbSurv] = Cnt;
	      Var = VariableEnBaseDeLaContrainte[Cnt];
	      X = ValBBarre - Xmax[Var];
	      ValeurDeViolationDeBorne[NbSurv] = ( X * X ) / DualPoids[Cnt];		
		    IndexDansContrainteASurveiller[Cnt] = NbSurv;
		    NbSurv++;		
	    }
		}		
	}
}

Spx->NombreDeContraintesASurveiller = NbSurv;

return;
}

/*----------------------------------------------------------------------------*/

void SPX_MajDesVariablesEnBaseAControler( PROBLEME_SPX * Spx , int NbBBarreModifies,
                                          int * IndexDeBBarreModifies, char * T ) 
{
int Var; int Cnt; char Viole; int NbSurv; int * IndexDansContrainteASurveiller;
int * NumerosDesContraintesASurveiller; double * BorneInfInfaisabilitePrimale;
double * BorneSupInfaisabilitePrimale; int Cnt1; int CntBase; int k; int i;
int * VariableEnBaseDeLaContrainte; char * TypeDeVariable; double * SeuilDeViolationDeBorne;
double X; double * DualPoids; double * ValeurDeViolationDeBorne; int VariableSortante;
int VariableEntrante; double ValeurDeBBarre; double * BBarre; double * Xmax;

VariableSortante = Spx->VariableSortante;
VariableEntrante = Spx->VariableEntrante;

# ifdef UTILISER_BORNES_AUXILIAIRES
  if ( Spx->ChangementDeBase == OUI_SPX ) {
    /* Si la variable entrante avait sur une borne sup auxiliaire on libere la borne */
		if ( Spx->NombreDeBornesAuxiliairesUtilisees > 0 ) SPX_SupprimerUneBorneAuxiliaire( Spx, VariableEntrante );
  }
# endif  

BorneInfInfaisabilitePrimale = Spx->BorneInfInfaisabilitePrimale;
BorneSupInfaisabilitePrimale = Spx->BorneSupInfaisabilitePrimale;
SeuilDeViolationDeBorne      = Spx->SeuilDeViolationDeBorne;
Xmax                         = Spx->Xmax;
TypeDeVariable               = Spx->TypeDeVariable;

if ( Spx->ChangementDeBase == OUI_SPX ) {
  CntBase = Spx->ContrainteDeLaVariableEnBase[VariableSortante];
  if ( TypeDeVariable[VariableEntrante] == BORNEE_INFERIEUREMENT ) {
    BorneInfInfaisabilitePrimale[CntBase] = -SeuilDeViolationDeBorne[VariableEntrante];
	  BorneSupInfaisabilitePrimale[CntBase] = LINFINI_SPX;	
  }
  else if ( TypeDeVariable[VariableEntrante] == BORNEE ) {
    BorneInfInfaisabilitePrimale[CntBase] = -SeuilDeViolationDeBorne[VariableEntrante];
	  BorneSupInfaisabilitePrimale[CntBase] = Xmax[VariableEntrante] + SeuilDeViolationDeBorne[VariableEntrante];	
  }
  else {
	  /* Variable non bornee */
    BorneInfInfaisabilitePrimale[CntBase] = -LINFINI_SPX;
	  BorneSupInfaisabilitePrimale[CntBase] = LINFINI_SPX;
  }
} 

BBarre    = Spx->BBarre;
DualPoids = Spx->DualPoids;
VariableEnBaseDeLaContrainte = Spx->VariableEnBaseDeLaContrainte;

NbSurv = Spx->NombreDeContraintesASurveiller;
IndexDansContrainteASurveiller   = Spx->IndexDansContrainteASurveiller;
NumerosDesContraintesASurveiller = Spx->NumerosDesContraintesASurveiller;
ValeurDeViolationDeBorne         = Spx->ValeurDeViolationDeBorne;

for ( k = 0 ; k < NbBBarreModifies ; k++ ) {
  Cnt = IndexDeBBarreModifies[k];
	T[Cnt] = 0;
	Viole  = NON_SPX;
  ValeurDeBBarre = BBarre[Cnt];
	
	Var = VariableEnBaseDeLaContrainte[Cnt];
	if ( Var == VariableSortante ) Var = VariableEntrante;
	
	if ( ValeurDeBBarre < BorneInfInfaisabilitePrimale[Cnt] ) {
	  if ( TypeDeVariable[Var] != NON_BORNEE ) {	
	    Viole = OUI_SPX;
	    X = ( ValeurDeBBarre * ValeurDeBBarre ) / DualPoids[Cnt];
		}		
	}
	else if ( ValeurDeBBarre > BorneSupInfaisabilitePrimale[Cnt] ) {
	  if ( TypeDeVariable[Var] == BORNEE ) {		
	    X = ValeurDeBBarre - Xmax[Var];
	    X = ( X * X ) / DualPoids[Cnt];
		  Viole = OUI_SPX;
		}		
  }
  	 					
 	i = IndexDansContrainteASurveiller[Cnt];
	if ( Viole == NON_SPX ) {
		/* On enleve la contrainte de la liste */
	  if ( i >= 0 ) {
		  /* On enleve la contrainte de la liste */
			IndexDansContrainteASurveiller[Cnt] = -1;
			Cnt1 = NumerosDesContraintesASurveiller[NbSurv-1];
			if ( Cnt != Cnt1 ) {
			  NumerosDesContraintesASurveiller[i] = Cnt1;
			  ValeurDeViolationDeBorne        [i] = ValeurDeViolationDeBorne[NbSurv-1];							
			  IndexDansContrainteASurveiller[Cnt1] = i;
			}				
			NbSurv--;
		}    
	}
	else {
	  if ( i < 0 ) {
      /* La Contrainte est nouvellement violee */
		  /* On ajoute la contrainte dans la liste */
			NumerosDesContraintesASurveiller[NbSurv] = Cnt;			
			ValeurDeViolationDeBorne        [NbSurv] = X;			
			IndexDansContrainteASurveiller[Cnt] = NbSurv;
		  NbSurv++;
    }
		else {
      /* La variable est deja dans la liste: on actualise la valeur */
			ValeurDeViolationDeBorne[i] = X;
		}
	}
}

Spx->NombreDeContraintesASurveiller = NbSurv;

return;
}


