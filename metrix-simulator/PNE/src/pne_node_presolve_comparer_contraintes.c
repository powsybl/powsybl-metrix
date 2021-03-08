/***********************************************************************

   FONCTION: Comparaison des contraintes et etudes des supports pour
	           tenter d'ameliorer les bornes, de fixer des variables ou
						 de modifier des seconds membres.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"  
  
# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_define.h" 
  
# include "bb_define.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define TRACES 1
# define TRACES_MODIFS_BORNES 1

# define ZERO_COLINEAIRE  1.e-12
# define SEUIL_FORCING_CONSTRAINT  1.e-7
# define FORCING_AU_MAX 1
# define FORCING_AU_MIN 2

# define CONTRAINTE_DE_BORNE_INF 1
# define CONTRAINTE_DE_BORNE_SUP 2
# define CONTRAINTE_EGAL         3

void PNE_ComparaisonDesContraintesEtActions( PROBLEME_PNE * , int , int , double * , double * , int * , int , char * , char , int * );
void PNE_CalculMinMaxDeCnt1PourTEgal0 ( PROBLEME_PNE * , int , double * , char * , double * , char * , double * , int * );
void PNE_ComparaisonDesContraintesFaireLeForcing( PROBLEME_PNE * , char , int , int * , int * , int * , double * , char * , double * , double * , int * );
void PNE_AjusterBornesDesVariablesPourTEgal0 ( PROBLEME_PNE * , int , char , double , char , double , char , double , int * , int * );

/*----------------------------------------------------------------------------*/

void PNE_ComparerLesContraintes( PROBLEME_PNE * Pne, char * ContrainteActivable, char Mode, char * RefaireUnCycle ) 
{
int il; int ic; int ilDebCnt; int ilMaxCnt; int ic1;  int NombreDeContraintes; int NbContraintesModifees;
int NombreDeVariables; int Cnt; int Cnt1; double * V; int * T; char BrnInfConnue; double BCnt; double * B;
int * Mdeb; int * NbTerm; int * Nuvar; double * A; int * NumContrainte; int NbTermesUtilesDeCnt;
int * Cdeb; int * Csui; int Var; int NbT; int MxTrm; int * ContrainteSource; char * SensContrainte;
char InitV; int NbCntDeVar; int * NumCntDeVar; int VarCnt; double BFixeDeCnt; int * ParColonnePremiereVariable;
int * ParColonneVariableSuivante; int NbMaxTermesDesColonnes; double * ValeurDeBorneSup; char * BorneSupConnue;
double * ValeurDeBorneInf; char * BorneInfConnue;

return;

NombreDeVariables = Pne->NombreDeVariablesTrav;
NombreDeContraintes = Pne->NombreDeContraintesTrav;
   
B = Pne->BTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
SensContrainte = Pne->SensContrainteTrav;   
Nuvar = Pne->NuvarTrav;
A = Pne->ATrav;
Cdeb = Pne->CdebTrav;
Csui = Pne->CsuiTrav;
NumContrainte = Pne->NumContrainteTrav;

BorneSupConnue = Pne->ProbingOuNodePresolve->BorneSupConnue;
ValeurDeBorneSup = Pne->ProbingOuNodePresolve->ValeurDeBorneSup;
BorneInfConnue = Pne->ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = Pne->ProbingOuNodePresolve->ValeurDeBorneInf;

V = (double *) malloc( NombreDeVariables * sizeof( double ) );
T = (int *) malloc( NombreDeVariables * sizeof( int ) );
NumCntDeVar = (int *) malloc( NombreDeContraintes * sizeof( int ) );
ContrainteSource = (int *) malloc( NombreDeContraintes * sizeof( int ) );
ParColonnePremiereVariable = (int *) malloc( (NombreDeContraintes+1) * sizeof( int ) );
ParColonneVariableSuivante = (int *) malloc( NombreDeVariables * sizeof( int ) );

if ( V == NULL || T == NULL || NumCntDeVar == NULL || ContrainteSource  == NULL ||
     ParColonnePremiereVariable == NULL || ParColonneVariableSuivante == NULL ) {
  free( V ); free( T );free( NumCntDeVar ); free( ContrainteSource );
	free( ParColonnePremiereVariable );free( ParColonneVariableSuivante );
  return;
}

memset( (int *) T, 0, NombreDeVariables * sizeof( int ) );
memset( (int *) ContrainteSource, OUI_PNE, NombreDeContraintes * sizeof( int ) );

for ( NbT = 0 ; NbT <= NombreDeContraintes ; NbT++ ) ParColonnePremiereVariable[NbT] = -1;
MxTrm = -1;
for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  BrnInfConnue = BorneInfConnue[Var];
  if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	     BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) continue;
  NbT = 0;
	ic = Cdeb[Var];
  while ( ic >= 0 ) {
		if ( A[ic] != 0 && ContrainteActivable[NumContrainte[ic]] == OUI_PNE ) NbT++;					
		ic = Csui[ic];
  }
	if ( NbT > MxTrm ) MxTrm = NbT;	
	ic = ParColonnePremiereVariable[NbT];	
	ParColonnePremiereVariable[NbT] = Var;	  
	ParColonneVariableSuivante[Var] = ic;	
}
NbMaxTermesDesColonnes = MxTrm;

/* On balaye les colonnes dans l'ordre croissant du nombre de termes et on ne compare que les
   les lignes qui ont un terme dans cette colonne */
	 
NbContraintesModifees = 0;

for ( NbT = 2 ; NbT <= NbMaxTermesDesColonnes ; NbT++ ) {
  Var = ParColonnePremiereVariable[NbT];
	while ( Var >= 0 ) {
	  /*********************************************/
    BrnInfConnue = BorneInfConnue[Var];
    if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	       BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) goto NextVar;		
	  /* On prepare la table des contraintes a explorer */
 	  NbCntDeVar = 0;
	  ic = Cdeb[Var];
    while ( ic >= 0 ) {
		  if ( A[ic] != 0 ) {
		    Cnt = NumContrainte[ic];
				if ( ContrainteActivable[Cnt] == OUI_PNE ) {
			    NumCntDeVar[NbCntDeVar] = Cnt;
				  NbCntDeVar++;
				}
      }
	    ic = Csui[ic];
	  }

	  for ( ic = 0 ; ic < NbCntDeVar ; ic++ ) {
	    Cnt = NumCntDeVar[ic];
			if ( ContrainteActivable[Cnt] == NON_PNE ) continue;			
      if ( ContrainteSource[Cnt] == NON_PNE ) continue;				
      ilDebCnt = 0;
		  InitV = NON_PNE;
			ilMaxCnt = -1;
			BCnt = LINFINI_PNE;
			NbTermesUtilesDeCnt = 0;
      /* On compare a Cnt toutes les contraintes d'inegalite de la colonne */
	    for ( ic1 = 0 ; ic1 < NbCntDeVar ; ic1++ ) {
	      Cnt1 = NumCntDeVar[ic1];
				if ( ContrainteActivable[Cnt1] == NON_PNE ) continue;
				if ( Cnt1 == Cnt ) continue;
			  /* Comparaison de Cnt a Cnt1 */
			  if ( InitV == NON_PNE ) {
		      /* Preparation des tables pour la contrainte Cnt */
					BFixeDeCnt = 0.0;
          ilDebCnt = Mdeb[Cnt];
	        ilMaxCnt = ilDebCnt + NbTerm[Cnt];
	        il = ilDebCnt;
	        while ( il < ilMaxCnt ) {
			      if ( A[il] != 0.0 ) {
					    VarCnt = Nuvar[il];
              BrnInfConnue = BorneInfConnue[VarCnt];
              if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	            BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) BFixeDeCnt += A[il] * ValeurDeBorneInf[VarCnt];							
				      else {
		            V[VarCnt] = A[il];
		            T[VarCnt] = 1;
						    NbTermesUtilesDeCnt++;
					    }							
				    }  
		        il++;
	        }
		      BCnt = B[Cnt] - BFixeDeCnt;
          InitV = OUI_PNE;
					if ( NbTermesUtilesDeCnt <= 1 ) break;	 				
					if ( NbTermesUtilesDeCnt > 10 ) break;										
			  }

        if ( NbTerm[Cnt1] >= NbTermesUtilesDeCnt && NbTerm[Cnt1] < 100 ) {				
          PNE_ComparaisonDesContraintesEtActions( Pne, Cnt, NbTermesUtilesDeCnt, &BCnt, V, T,
				                                          Cnt1, ContrainteActivable, Mode, &NbContraintesModifees );																									 
        }
			  if ( ContrainteActivable[Cnt] == NON_PNE ) break;
			  if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) goto FinComparaisons;																								 
	    }
      /* RAZ de V et T avant de passer a la contrainte suivante */
		  if ( InitV == OUI_PNE ) {
        il = ilDebCnt;
 	      while ( il < ilMaxCnt ) {
		      V[Nuvar[il]] = 0.0;
		      T[Nuvar[il]] = 0;
		      il++;
	      }
		  }
			ContrainteSource[Cnt] = NON_PNE;
  	}				
	  /*********************************************/
		NextVar:
	  Var = ParColonneVariableSuivante[Var];
	}
}

if ( NbContraintesModifees > 0 ) *RefaireUnCycle = OUI_PNE;

FinComparaisons:
free( V ); free( T );free( NumCntDeVar ); free( ContrainteSource );
free( ParColonnePremiereVariable );free( ParColonneVariableSuivante );

return;
}

/*----------------------------------------------------------------------------*/

void PNE_ComparaisonDesContraintesEtActions( PROBLEME_PNE * Pne, int Cnt, int NbTermesUtilesDeCnt,
                                             double * ValeurNetteDeBCnt_E_S,
                                             double * V, int * T, int Cnt1, char * ContrainteActivable, char Mode, int * NbContraintesModifees )
{
int Nb; int il1; int il1Max; int * Mdeb; int * NbTerm; double * A; int * Nuvar; char * SensContrainte; int Var1; double * B;
double Rapport; char TypeDeContrainteDeBorne; double X; double ValeurDeLaContraintedeBorne; char BrnInfConnue;
double Smax; char SmaxValide; double Smin; char SminValide; double * ValeurDeBorneSup;
char * BorneSupConnue; double * ValeurDeBorneInf; char * BorneInfConnue; PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve;
char SensCnt; char SensCnt1; double ValeurNetteDeBCnt; double ValeurNetteDeBCnt_1; double Delta;

ValeurNetteDeBCnt = *ValeurNetteDeBCnt_E_S;

SensContrainte = Pne->SensContrainteTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
B = Pne->BTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;

ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;
BorneSupConnue = ProbingOuNodePresolve->BorneSupConnue;
ValeurDeBorneSup = ProbingOuNodePresolve->ValeurDeBorneSup;
BorneInfConnue = ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = ProbingOuNodePresolve->ValeurDeBorneInf;

Nb = NbTermesUtilesDeCnt;
SensCnt = SensContrainte[Cnt];
SensCnt1 = SensContrainte[Cnt1];

il1 = Mdeb[Cnt1];
il1Max = il1 + NbTerm[Cnt1];
/* Determination du rapport */
Rapport = 1.0;
while ( il1 < il1Max ) {
  if ( A[il1] != 0.0 ) {
    Var1 = Nuvar[il1];
    BrnInfConnue = BorneInfConnue[Var1];
    if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	       BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) goto NextIl1_0;		
    if ( T[Var1] != 0 ) {				
			/* Terme correspondant dans la colonne */				
		  Rapport = V[Var1] / A[il1];
      Nb--;
			il1++;
			break;		 			
	  }
	}
	NextIl1_0:
  il1++;
}

/* On poursuit l'analyse de la contrainte Cnt1 */
while ( il1 < il1Max ) {
  if ( A[il1] != 0.0 ) {
    Var1 = Nuvar[il1];
    BrnInfConnue = BorneInfConnue[Var1];
    if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	       BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) goto NextIl1_1;				
		if ( T[Var1] != 0 ) {			 
			/* Terme correspondant dans la colonne */				
			X = fabs( V[Var1] - (Rapport * A[il1]) );
      if ( X > ZERO_COLINEAIRE ) return;				
	    Nb--;			
		}
	}
	NextIl1_1:
	il1++;
}

if ( Nb != 0 ) return;
  
PNE_CalculMinMaxDeCnt1PourTEgal0 ( Pne, Cnt1, &Smin, &SminValide, &Smax, &SmaxValide, &ValeurNetteDeBCnt_1, T );

/* Si Rapport est positif, alors Cnt peut etre vue comme une contrainte de borne sup sur un groupe de variables */
/* Si Rapport est negatif, alors Cnt peut etre vue comme une contrainte de borne inf sur un groupe de variables */

ValeurDeLaContraintedeBorne = ValeurNetteDeBCnt / Rapport;

if ( Rapport > 0 ) TypeDeContrainteDeBorne = CONTRAINTE_DE_BORNE_SUP;		
else TypeDeContrainteDeBorne = CONTRAINTE_DE_BORNE_INF;
if ( SensCnt == '=' ) TypeDeContrainteDeBorne = CONTRAINTE_EGAL;	

if ( SensCnt1 == '<' ) {
  if ( TypeDeContrainteDeBorne == CONTRAINTE_DE_BORNE_SUP || TypeDeContrainteDeBorne == CONTRAINTE_EGAL ) {
    if ( SmaxValide == OUI_PNE ) {
      if ( ValeurDeLaContraintedeBorne + Smax < ValeurNetteDeBCnt_1 + (0.05 * SEUIL_DADMISSIBILITE) ) {        
        # if TRACES == 1
				  printf("Contrainte %d de type < redondante grace a la contrainte %d  Max %e  ValeurNetteDeBCnt_1 %e\n",
					        Cnt1,Cnt,ValeurDeLaContraintedeBorne+Smax,ValeurNetteDeBCnt_1);        
			  # endif
			  ContrainteActivable[Cnt1] = NON_PNE;						       
        *NbContraintesModifees = *NbContraintesModifees + 1;
	    }
		}
  }
  if ( TypeDeContrainteDeBorne == CONTRAINTE_DE_BORNE_INF || TypeDeContrainteDeBorne == CONTRAINTE_EGAL ) {
    if ( SminValide == OUI_PNE ) {
      if ( ValeurDeLaContraintedeBorne + Smin > ValeurNetteDeBCnt_1 + SEUIL_DADMISSIBILITE ) {
        # if TRACES == 1
				  printf("Contrainte %d infaisable: %e > %e\n",Cnt1,ValeurDeLaContraintedeBorne+Smin,ValeurNetteDeBCnt_1);
			  # endif
			  Pne->ProbingOuNodePresolve->Faisabilite = NON_PNE;			
			  return;
	    }	
		  if ( fabs( (ValeurDeLaContraintedeBorne+Smin) - ValeurNetteDeBCnt_1 ) < SEUIL_FORCING_CONSTRAINT ) {        
        # if TRACES == 1
			    printf("Forcing contrainte %d grace a la contrainte %d\n",Cnt1,Cnt);
			  # endif
        /* On fixe les variables pour obtenir Smin */
        PNE_ComparaisonDesContraintesFaireLeForcing( Pne, FORCING_AU_MIN, Cnt1, Mdeb,  NbTerm, Nuvar, A,
																							 	     BorneInfConnue, ValeurDeBorneInf, ValeurDeBorneSup, T );
				if ( SensCnt == '=' ) ContrainteActivable[Cnt1] = NON_PNE;				
        *NbContraintesModifees = *NbContraintesModifees + 1;				
		  }
		  else {
		    /* Tentative d'amelioration de bornes des variables de Cnt1 pour les T = 0 */
		    /* On dispose d'une nouvelle borne sup */
				if ( ContrainteActivable[Cnt1] == OUI_PNE ) {				
          PNE_AjusterBornesDesVariablesPourTEgal0( Pne, Cnt1, '<', Smin, SminValide, Smax, SmaxValide,
				                                           ValeurNetteDeBCnt_1 - ValeurDeLaContraintedeBorne, T, NbContraintesModifees);																												

        }
      }
		}
	}
}
else {
  /* Sens de Cnt1 est = */
  if ( TypeDeContrainteDeBorne == CONTRAINTE_DE_BORNE_SUP || TypeDeContrainteDeBorne == CONTRAINTE_EGAL ) {
    if ( SmaxValide == OUI_PNE ) {
	    if (  ValeurDeLaContraintedeBorne + Smax < ValeurNetteDeBCnt_1 - SEUIL_DADMISSIBILITE ) {
        # if TRACES == 1
				  printf("Contrainte %d infaisable: %e < %e\n",Cnt1,ValeurDeLaContraintedeBorne+Smax,ValeurDeLaContraintedeBorne);
			  # endif
			  Pne->ProbingOuNodePresolve->Faisabilite = NON_PNE;			
			  return;
		  }
		  if ( fabs( (ValeurDeLaContraintedeBorne+Smax) - ValeurNetteDeBCnt_1 ) < SEUIL_FORCING_CONSTRAINT ) {        
        # if TRACES == 1
			    printf("Forcing contrainte %d de type = grace a la contrainte %d\n",Cnt1,Cnt);
			  # endif
        /* On fixe les variables pour obtenir Smax */			 
        PNE_ComparaisonDesContraintesFaireLeForcing( Pne, FORCING_AU_MAX, Cnt1, Mdeb,  NbTerm, Nuvar, A,
																							 	     BorneInfConnue, ValeurDeBorneInf, ValeurDeBorneSup, T );																												
        *NbContraintesModifees = *NbContraintesModifees + 1;
				if ( SensCnt == '=' ) ContrainteActivable[Cnt1] = NON_PNE;
		  }
		  /* Tentative d'amelioration de bornes des variables de Cnt1 pour les T = 0 */
		  /* On dispose d'une nouvelle borne inf */
			if ( ContrainteActivable[Cnt1] == OUI_PNE ) {					
        PNE_AjusterBornesDesVariablesPourTEgal0( Pne, Cnt1, '>', Smin, SminValide, Smax, SmaxValide,
				                                         ValeurNetteDeBCnt_1 - ValeurDeLaContraintedeBorne, T, NbContraintesModifees);																										
      }
    }		
  }
	/* Ci dessous: il est important de ne pas mettre else if */
  if ( TypeDeContrainteDeBorne == CONTRAINTE_DE_BORNE_INF || TypeDeContrainteDeBorne == CONTRAINTE_EGAL ) {
    if ( SminValide == OUI_PNE ) {
      if ( ValeurDeLaContraintedeBorne + Smin > ValeurNetteDeBCnt_1 + SEUIL_DADMISSIBILITE ) {
        # if TRACES == 1
				  printf("Contrainte %d infaisable: %e > %e\n",Cnt1,ValeurDeLaContraintedeBorne+Smin,ValeurNetteDeBCnt_1);
			  # endif
			  Pne->ProbingOuNodePresolve->Faisabilite = NON_PNE;			
			  return;
		  }  
      if ( fabs( (ValeurNetteDeBCnt_1-ValeurDeLaContraintedeBorne) - Smin ) < SEUIL_FORCING_CONSTRAINT ) {					
        # if TRACES == 1
			    printf("Forcing contrainte %d gde type = race a la contrainte %d\n",Cnt1,Cnt);
			  # endif
        /* On fixe les variables pour obtenir Smin */			 
        PNE_ComparaisonDesContraintesFaireLeForcing( Pne, FORCING_AU_MIN, Cnt1, Mdeb,  NbTerm, Nuvar, A,
																							 	     BorneInfConnue, ValeurDeBorneInf, ValeurDeBorneSup, T );
				if ( SensCnt == '=' ) ContrainteActivable[Cnt1] = NON_PNE;																															 
        *NbContraintesModifees = *NbContraintesModifees + 1;						  
		  }		
		  /* Tentative d'amelioration de bornes des variables de Cnt1 pour les T = 0 */
		  /* On dispose d'une nouvelle borne sup */
			if ( ContrainteActivable[Cnt1] == OUI_PNE ) {								
        PNE_AjusterBornesDesVariablesPourTEgal0( Pne, Cnt1, '<', Smin, SminValide, Smax, SmaxValide,
				                                         ValeurNetteDeBCnt_1 - ValeurDeLaContraintedeBorne, T, NbContraintesModifees);
			}																				 
    }		
  }
}

/* Repercussions sur Cnt */
if ( SensCnt == '<' ) {
  if ( SminValide == OUI_PNE ) {
	  if ( Rapport > 0.0 ) {
		  if ( ValeurNetteDeBCnt > Rapport * (ValeurNetteDeBCnt_1 - Smin) && Mode == PRESOLVE_SIMPLIFIE_POUR_REDUCED_COST_FIXING_AU_NOEUD_RACINE ) {
        # if TRACES == 1        				
			    printf("Rapport = %e on peut faire passer le second membre de la contrainte %d de %e a %e\n",Rapport,Cnt,B[Cnt],Rapport * (ValeurNetteDeBCnt_1 - Smin));
			  # endif
				Delta = ValeurNetteDeBCnt - ( Rapport * (ValeurNetteDeBCnt_1 - Smin) );
				ValeurNetteDeBCnt -= Delta;
				*ValeurNetteDeBCnt_E_S = ValeurNetteDeBCnt;
        B[Cnt] -= Delta;				
        *NbContraintesModifees = *NbContraintesModifees + 1;
		  }
	  }
    else {
	    /* Rapport negatif */
      if ( fabs( ValeurNetteDeBCnt - ( Rapport * ( ValeurNetteDeBCnt_1 - Smin ) ) ) < SEUIL_FORCING_CONSTRAINT && 0 ) {			
        # if TRACES == 1        				
		      printf("Rapport = %e on peut transformer la contrainte d'inegalite %d en contrainte d'egalite\n",Rapport,Cnt);
			  # endif
        /* On ne peut pas le faire pendant le branch and bound sauf a changer le type de la variable d'ecart */				
				SensContrainte[Cnt] = '=';
        *NbContraintesModifees = *NbContraintesModifees + 1;				
			}
		}					
	}  
	if ( SensCnt1 == '=' ) {
    if ( SmaxValide == OUI_PNE ) {
	    if ( Rapport > 0.0 ) {
        if ( fabs( ValeurNetteDeBCnt - ( Rapport * ( ValeurNetteDeBCnt_1 - Smax ) ) ) < SEUIL_FORCING_CONSTRAINT && 0 ) {			
          # if TRACES == 1        				
		        printf("Rapport = %e on peut transformer la contrainte d'inegalite %d en contrainte d'egalite\n",Rapport,Cnt);
			    # endif
          /* On ne peut pas le faire pendant le branch and bound sauf a changer le type de la variable d'ecart */          
					SensContrainte[Cnt] = '=';
          *NbContraintesModifees = *NbContraintesModifees + 1;					
			  }
			}
			else {
	      /* Rapport negatif */
		    if ( ValeurNetteDeBCnt > Rapport * (ValeurNetteDeBCnt_1 - Smax) && Mode == PRESOLVE_SIMPLIFIE_POUR_REDUCED_COST_FIXING_AU_NOEUD_RACINE ) {
          # if TRACES == 1        				
			      printf("Rapport = %e on peut faire passer le second membre de la contrainte %d de %e a %e\n",Rapport,Cnt,B[Cnt],Rapport * (ValeurNetteDeBCnt_1 - Smax));
			    # endif					
				  Delta = ValeurNetteDeBCnt - ( Rapport * (ValeurNetteDeBCnt_1 - Smax) );
				  ValeurNetteDeBCnt -= Delta;
				  *ValeurNetteDeBCnt_E_S = ValeurNetteDeBCnt;
          B[Cnt] -= Delta;	
					
          *NbContraintesModifees = *NbContraintesModifees + 1;
				}
		  }	  			
		}
	}
}
else {
  /* Sens de Cnt est = */
  if ( SminValide == OUI_PNE ) {
		if ( ValeurNetteDeBCnt / Rapport > (ValeurNetteDeBCnt_1 - Smin) + SEUIL_DADMISSIBILITE ) {
      # if TRACES == 1        				
			  printf("Pas de solution pour la contrainte %d\n",Cnt);
			# endif
			Pne->ProbingOuNodePresolve->Faisabilite = NON_PNE;
      return;
	  }
	}
	if ( SensCnt1 == '=' ) {
	  if ( SmaxValide == OUI_PNE ) {
	 	  if ( ValeurNetteDeBCnt / Rapport < (ValeurNetteDeBCnt_1 - Smax) - SEUIL_DADMISSIBILITE ) {
        # if TRACES == 1        				
			    printf("Pas de solution pour la contrainte %d\n",Cnt);
			  # endif
				Pne->ProbingOuNodePresolve->Faisabilite = NON_PNE;
        return;
	    }
		}
	}
}

return;
}

/*----------------------------------------------------------------------------*/

void PNE_ComparaisonDesContraintesFaireLeForcing( PROBLEME_PNE * Pne, char TypeDeForcing, int Cnt1,
                                                  int * Mdeb, int * NbTerm, int * Nuvar, double * A,
																									char * BorneInfConnue, double * ValeurDeBorneInf,
																									double * ValeurDeBorneSup, int * T )																							 
{
int il1; int il1Max; int Var1; char UneVariableAEteFixee; char BorneMiseAJour; char BrnInfConnue;
double NouvelleValeur;
il1 = Mdeb[Cnt1];
il1Max = il1 + NbTerm[Cnt1];
if ( TypeDeForcing == FORCING_AU_MIN ) {
  while ( il1 < il1Max ) {
    Var1 = Nuvar[il1];
    if ( T[Var1] == 0 ) {
      BrnInfConnue = BorneInfConnue[Var1];			
      if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	         BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) goto NextIl1_0;
      UneVariableAEteFixee = NON_PNE;
			BorneMiseAJour = NON_PNE;										
      if ( A[il1] > 0 ) { NouvelleValeur = ValeurDeBorneInf[Var1]; UneVariableAEteFixee = FIXATION_SUR_BORNE_INF; }
			else if ( A[il1] < 0 ) { NouvelleValeur = ValeurDeBorneSup[Var1]; UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP; }
	    if ( UneVariableAEteFixee != NON_PNE ) {
			  # if TRACES == 1
				  if ( UneVariableAEteFixee == FIXATION_SUR_BORNE_INF ) printf("Forcing au min. Var %d Borne sup %e -> %e\n",Var1,ValeurDeBorneSup[Var1],NouvelleValeur); 
				  if ( UneVariableAEteFixee == FIXATION_SUR_BORNE_SUP ) printf("Forcing au min. Var %d Borne inf %e -> %e\n",Var1,ValeurDeBorneInf[Var1],NouvelleValeur); 
				# endif
        PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var1, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
        if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;
	    }							  
		}
		NextIl1_0:				  	    
    il1++;
  }
}
else {
  while ( il1 < il1Max ) {
    Var1 = Nuvar[il1];
    if ( T[Var1] == 0 ) {
      BrnInfConnue = BorneInfConnue[Var1];			
      if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	    BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) goto NextIl1_1;
      UneVariableAEteFixee = NON_PNE;
			BorneMiseAJour = NON_PNE;										
      if ( A[il1] > 0 ) { NouvelleValeur = ValeurDeBorneSup[Var1]; UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP; }			   
			else if ( A[il1] < 0 ) { NouvelleValeur = ValeurDeBorneInf[Var1]; UneVariableAEteFixee = FIXATION_SUR_BORNE_INF; }			
	    if ( UneVariableAEteFixee != NON_PNE ) {
			  # if TRACES == 1
				  if ( UneVariableAEteFixee == FIXATION_SUR_BORNE_INF ) printf("Forcing au min. Var %d Borne sup %e -> %e\n",Var1,ValeurDeBorneSup[Var1],NouvelleValeur); 
				  if ( UneVariableAEteFixee == FIXATION_SUR_BORNE_SUP ) printf("Forcing au min. Var %d Borne inf %e -> %e\n",Var1,ValeurDeBorneInf[Var1],NouvelleValeur); 
				# endif			
        PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var1, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
        if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;
	    }
		}			
		NextIl1_1:				 
    il1++;
  }				
}
return;
}

/*----------------------------------------------------------------------------*/

void PNE_CalculMinMaxDeCnt1PourTEgal0 ( PROBLEME_PNE * Pne, int Cnt1,
                                        double * SminS, char * SminValideS,
																				double * SmaxS, char * SmaxValideS,
                                        double * ValeurNetteDeBCnt1,
																				int * T )
{
double SFixe; int il1; int il1Max; int * Mdeb; int * NbTerm; double * A; int * Nuvar;
int Var1; double * B; double Smax; char SmaxValide; double Smin; char SminValide;
double a1; char BrnInfConnue; double * ValeurDeBorneSup; char * BorneSupConnue;
double * ValeurDeBorneInf; char * BorneInfConnue;

Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
B = Pne->BTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;

BorneSupConnue = Pne->ProbingOuNodePresolve->BorneSupConnue;
ValeurDeBorneSup = Pne->ProbingOuNodePresolve->ValeurDeBorneSup;
BorneInfConnue = Pne->ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = Pne->ProbingOuNodePresolve->ValeurDeBorneInf;

SminValide = OUI_PNE;
SmaxValide = OUI_PNE;
Smax = 0;
Smin = 0;
SFixe = 0.0;
 
il1 = Mdeb[Cnt1];
il1Max = il1 + NbTerm[Cnt1];
while ( il1 < il1Max ) {
  if ( SminValide == NON_PNE && SmaxValide == NON_PNE ) break;
  if ( A[il1] != 0.0 ) {
    Var1 = Nuvar[il1];
		if ( T[Var1] == 0 ) {
	    a1 = A[il1];		  
      BrnInfConnue = BorneInfConnue[Var1];
      if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	         BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) SFixe += a1 * ValeurDeBorneInf[Var1];				
			else {
        if ( a1 > 0 ) {
          if ( BorneSupConnue[Var1] == OUI_PNE ) Smax += a1 * ValeurDeBorneSup[Var1];				  
				  else SmaxValide = NON_PNE;
          if ( BorneInfConnue[Var1] == OUI_PNE ) Smin += a1 * ValeurDeBorneInf[Var1];				  
			    else SminValide = NON_PNE;					
			  }
			  else {
          if ( BorneInfConnue[Var1] == OUI_PNE ) Smax += a1 * ValeurDeBorneInf[Var1];			    
			    else SmaxValide = NON_PNE;
          if ( BorneSupConnue[Var1] == OUI_PNE ) Smin += a1 * ValeurDeBorneSup[Var1];				  
				  else SminValide = NON_PNE;
				}
			}
	  }
	}
  il1++;
}

if ( SminValide == NON_PNE ) Smin = -LINFINI_PNE;
if ( SmaxValide == NON_PNE ) Smax = LINFINI_PNE;

*SminS = Smin;
*SminValideS = SminValide;
*SmaxS = Smax;
*SmaxValideS = SmaxValide;

*ValeurNetteDeBCnt1 = B[Cnt1] - SFixe;

return;
}

/*----------------------------------------------------------------------------*/

void PNE_AjusterBornesDesVariablesPourTEgal0 ( PROBLEME_PNE * Pne, int Cnt1,
                                               char SensCnt1, double Smin, char SminValide,
																				       double Smax, char SmaxValide, double b,
																				       int * T, int * NbContraintesModifees )
{
int il1; int il1Max; int * Mdeb; int * NbTerm; double * A; int * Nuvar; int Var1;
char * BorneSupConnue; char * BorneInfConnue; int Faisabilite; char UneVariableAEteFixee;
double * ValeurDeBorneInf; double * ValeurDeBorneSup; double a1; char BorneMiseAJour;
double S; double NouvelleValeur; char BrnInfConnue; char XsValide; char XiValide;
double Xs; double Xi; double Xs0; double Xi0; int NbModifs;
# if TRACES_MODIFS_BORNES == 1
  double OldBorne;
# endif

NbModifs = 0;

Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;

BorneSupConnue = Pne->ProbingOuNodePresolve->BorneSupConnue;
ValeurDeBorneSup = Pne->ProbingOuNodePresolve->ValeurDeBorneSup;
BorneInfConnue = Pne->ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = Pne->ProbingOuNodePresolve->ValeurDeBorneInf;

if ( SensCnt1 == '<' ) {
  if ( SminValide == NON_PNE ) return;
  il1 = Mdeb[Cnt1];
  il1Max = il1 + NbTerm[Cnt1];
  while ( il1 < il1Max ) {
    if ( A[il1] != 0.0 ) {		
      Var1 = Nuvar[il1];
		  if ( T[Var1] == 0 ) {
	      a1 = A[il1];
        BrnInfConnue = BorneInfConnue[Var1];
        if ( BrnInfConnue != FIXE_AU_DEPART && BrnInfConnue != FIXATION_SUR_BORNE_INF &&
	           BrnInfConnue != FIXATION_SUR_BORNE_SUP && BrnInfConnue != FIXATION_A_UNE_VALEUR ) {				
          if ( a1 > 0 ) {
            if ( BorneInfConnue[Var1] != NON_PNE ) {
						
	  	        XsValide = NON_PNE;
		          XiValide = NON_PNE;		
		          Xs = ValeurDeBorneSup[Var1];
		          Xi = ValeurDeBorneInf[Var1];
		          Xs0 = Xs;
		          Xi0 = Xi;
							
							XsValide = OUI_PNE;
              S = Smin - ( a1 * ValeurDeBorneInf[Var1] );									
						 	Xs = ( b - S ) / a1;

              # if TRACES_MODIFS_BORNES == 1
                OldBorne = ValeurDeBorneSup[Var1];
              # endif							
	
		          if ( XiValide == OUI_PNE || XsValide == OUI_PNE ) {
		            if ( Xi > Xi0 || Xs < Xs0 ) {
                  PNE_ModifierLaBorneDUneVariable( Pne, Var1, SensCnt1, XsValide, Xs, XiValide, Xi, &NouvelleValeur,
		                                               &BorneMiseAJour, &UneVariableAEteFixee, &Faisabilite );
                  if ( Faisabilite == NON_PNE ) {
		                # if TRACES == 1
			                printf("Pas de solution dans le presolve simplifie\n");
			              # endif			
		                return;
		              }		
	                /* Si la variable a ete fixee ou une borne mise a jour on modifie les bornes des contraintes */																	 
	                if ( UneVariableAEteFixee != NON_PNE || BorneMiseAJour != NON_PNE  ) {		  
		                # if TRACES == 1
			                printf("Variable %d  Borne sup %e -> %e\n",Var1,OldBorne,NouvelleValeur);
			              # endif
										NbModifs++;
                    PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var1, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
                    if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;
									}
								}
							}							                          																																						
				    }
			    }
			    else {
            if (  BorneSupConnue[Var1] != NON_PNE ) {
						
	  	        XsValide = NON_PNE;
		          XiValide = NON_PNE;		
		          Xs = ValeurDeBorneSup[Var1];
		          Xi = ValeurDeBorneInf[Var1];
		          Xs0 = Xs;
		          Xi0 = Xi;
													
							XiValide = OUI_PNE;
              S = Smin - ( a1 * ValeurDeBorneSup[Var1] );
							Xi = ( b - S ) / a1;

              # if TRACES_MODIFS_BORNES == 1
                OldBorne = ValeurDeBorneInf[Var1];
              # endif							
			     								
		          if ( XiValide == OUI_PNE || XsValide == OUI_PNE ) {
		            if ( Xi > Xi0 || Xs < Xs0 ) {
                  PNE_ModifierLaBorneDUneVariable( Pne, Var1, SensCnt1, XsValide, Xs, XiValide, Xi, &NouvelleValeur,
		                                               &BorneMiseAJour, &UneVariableAEteFixee, &Faisabilite );
                  if ( Faisabilite == NON_PNE ) {
		                # if TRACES == 1
			                printf("Pas de solution dans le presolve simplifie\n");
			              # endif			
		                return;
		              }		
	                /* Si la variable a ete fixee ou une borne mise a jour on modifie les bornes des contraintes */																	 
	                if ( UneVariableAEteFixee != NON_PNE || BorneMiseAJour != NON_PNE  ) {
										NbModifs++;									
		                # if TRACES == 1
			                printf("Variable %d  Borne inf %e -> %e\n",Var1,OldBorne,NouvelleValeur);
			              # endif							
                    PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var1, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
                    if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;
									}
								}
							}							                          																																										    
							
			      }
				  }
			  }				
	    }
	  }
    il1++;
  }
}
else if ( SensCnt1 == '>' ) {
  if ( SmaxValide == NON_PNE ) return;	
  il1 = Mdeb[Cnt1];
  il1Max = il1 + NbTerm[Cnt1];
  while ( il1 < il1Max ) {
    if ( A[il1] != 0.0 ) {
      Var1 = Nuvar[il1];
		  if ( T[Var1] == 0 ) {			
	      a1 = A[il1];
        BrnInfConnue = BorneInfConnue[Var1];
        if ( BrnInfConnue != FIXE_AU_DEPART && BrnInfConnue != FIXATION_SUR_BORNE_INF &&
	           BrnInfConnue != FIXATION_SUR_BORNE_SUP && BrnInfConnue != FIXATION_A_UNE_VALEUR ) {					
          if ( a1 > 0 ) {
            if ( BorneSupConnue[Var1] != NON_PNE ) {
						
	  	        XsValide = NON_PNE;
		          XiValide = NON_PNE;		
		          Xs = ValeurDeBorneSup[Var1];
		          Xi = ValeurDeBorneInf[Var1];
		          Xs0 = Xs;
		          Xi0 = Xi;
																			
		          XiValide = OUI_PNE;		
              S = Smax - ( a1 * ValeurDeBorneSup[Var1] );							
							Xi = ( b - S ) / a1;

              # if TRACES_MODIFS_BORNES == 1
                OldBorne = ValeurDeBorneInf[Var1];
              # endif							
										
		          if ( XiValide == OUI_PNE || XsValide == OUI_PNE ) {
		            if ( Xi > Xi0 || Xs < Xs0 ) {
                  PNE_ModifierLaBorneDUneVariable( Pne, Var1, SensCnt1, XsValide, Xs, XiValide, Xi, &NouvelleValeur,
		                                               &BorneMiseAJour, &UneVariableAEteFixee, &Faisabilite );
                  if ( Faisabilite == NON_PNE ) {
		                # if TRACES == 1
			                printf("Pas de solution dans le presolve simplifie\n");
			              # endif			
		                return;
		              }		
	                /* Si la variable a ete fixee ou une borne mise a jour on modifie les bornes des contraintes */																	 
	                if ( UneVariableAEteFixee != NON_PNE || BorneMiseAJour != NON_PNE  ) {		  
										NbModifs++;									
		                # if TRACES == 1
			                printf("Variable %d  Borne inf %e -> %e\n",Var1,OldBorne,NouvelleValeur);
			              # endif							
                    PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var1, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
                    if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;
									}
								}
							}							                          																																										    							 							
				    }
			    }
			    else {
            if ( BorneInfConnue[Var1] != NON_PNE ) {

	  	        XsValide = NON_PNE;
		          XiValide = NON_PNE;		
		          Xs = ValeurDeBorneSup[Var1];
		          Xi = ValeurDeBorneInf[Var1];
		          Xs0 = Xs;
		          Xi0 = Xi;
													
							XsValide = OUI_PNE;
              S = Smax - ( a1 * ValeurDeBorneInf[Var1] );
							Xs = ( b - S ) / a1;
	
              # if TRACES_MODIFS_BORNES == 1
                OldBorne = ValeurDeBorneSup[Var1];
              # endif															

		          if ( XiValide == OUI_PNE || XsValide == OUI_PNE ) {
		            if ( Xi > Xi0 || Xs < Xs0 ) {
                  PNE_ModifierLaBorneDUneVariable( Pne, Var1, SensCnt1, XsValide, Xs, XiValide, Xi, &NouvelleValeur,
		                                               &BorneMiseAJour, &UneVariableAEteFixee, &Faisabilite );
                  if ( Faisabilite == NON_PNE ) {
		                # if TRACES == 1
			                printf("Pas de solution dans le presolve simplifie\n");
			              # endif			
		                return;
		              }		
	                /* Si la variable a ete fixee ou une borne mise a jour on modifie les bornes des contraintes */																	 
	                if ( UneVariableAEteFixee != NON_PNE || BorneMiseAJour != NON_PNE  ) {		  
										NbModifs++;									
		                # if TRACES == 1
			                printf("Variable %d  Borne sup %e -> %e\n",Var1,OldBorne,NouvelleValeur);
			              # endif							
                    PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var1, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );
                    if ( Pne->ProbingOuNodePresolve->Faisabilite == NON_PNE ) return;
									}
								}
							}																																																						
			      }
				  }
			  }
	    }
	  }
    il1++;
  }
}

*NbContraintesModifees = *NbContraintesModifees + NbModifs;

return;
}

/*----------------------------------------------------------------------------*/
