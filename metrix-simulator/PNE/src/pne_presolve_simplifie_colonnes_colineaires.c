/***********************************************************************

   FONCTION: Recherche des colonnes colineaires dans le node presolve.
	           Le but n'est pas de remplcaer des variables mais d'en fixer
						 lorsque les 2 colonnes colineaires on des couts differents.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# include "prs_fonctions.h"
# include "prs_define.h"
				
# ifdef PRS_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define ZERO_COLINEAIRE  1.e-12

# define COLONNE_A_ETUDIER 0
# define COLONNE_A_EVITER  1

# define TEST_UNIQUEMENT_COLONNES_A_DEUX_TERMES_POUR_RESOLUTION OUI_PNE

/*----------------------------------------------------------------------------*/

void PNE_PresolveSimplifieColonnesColineaires( PROBLEME_PNE * Pne, char * ContrainteActivable, char Mode )	   
{
double * A; int ic1; int Cnt1; double * V; int NbVarDispo; int NbVarDeCnt;
int * NumVarDeCnt; int il1; double DeltaC;
double Nu; int Nb; int Var1; int CntDeVar; int ilMax; char InitV;
int NombreDeVariables; int NombreDeContraintes; int Cnt; int HashVar; int NbTermesUtiles;
int * TypeDeVariable; int NbT; int Var; char * Flag; double Cvar;
int * Mdeb; int * NbTerm; int * Nuvar; int * Cdeb; int * Csui; int * NumContrainte;
PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve; char * pt; int * NbTermesUtilesDeVar;
int * HashCode; int il; int ic; char * T; double * L; char * Buffer; int LallocTas;
double * ValeurDeBorneSup; char * BorneSupConnue; int MxTrm; char BrnInfConnue;
double * ValeurDeBorneInf; char * BorneInfConnue; int NbMaxTermesDesLignes;
int * ParLignePremiereContrainte; int * ParLigneContrainteSuivante; double NouvelleValeur; char BorneMiseAJour;
char UneVariableAEteFixee; char * SensContrainte;

return;

if ( Mode == PRESOLVE_SIMPLIFIE_POUR_NODE_PRESOLVE ) return;

printf("PNE_PresolveSimplifieColonnesColineaires \n");

ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;
if ( ProbingOuNodePresolve == NULL ) return;

NombreDeVariables = Pne->NombreDeVariablesTrav;
NombreDeContraintes = Pne->NombreDeContraintesTrav;

TypeDeVariable = Pne->TypeDeVariableTrav;

BorneSupConnue = ProbingOuNodePresolve->BorneSupConnue;
ValeurDeBorneSup = ProbingOuNodePresolve->ValeurDeBorneSup;
BorneInfConnue = ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneInf = ProbingOuNodePresolve->ValeurDeBorneInf;

L = Pne->LTrav;

Mdeb = Pne->MdebTrav;
SensContrainte = Pne->SensContrainteTrav;
NbTerm = Pne->NbTermTrav;
Nuvar = Pne->NuvarTrav;

A = Pne->ATrav;
NumContrainte = Pne->NumContrainteTrav;

Cdeb = Pne->CdebTrav;   
Csui = Pne->CsuiTrav;

/* Classement des lignes en fonction du nombre de termes */

ParLignePremiereContrainte = (int *) malloc( (NombreDeVariables+1) * sizeof( int ) );
ParLigneContrainteSuivante = (int *) malloc( NombreDeContraintes * sizeof( int ) );

for ( NbT = 0 ; NbT <= NombreDeVariables ; NbT++ ) ParLignePremiereContrainte[NbT] = -1;
MxTrm = -1;
for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  if ( ContrainteActivable[Cnt] == NON_PNE ) continue;
  NbT = 0;
	il = Mdeb[Cnt];
	ilMax = il + NbTerm[Cnt];
  while ( il < ilMax ) {
		if ( A[il] != 0 ) {
		  Var = Nuvar[il];
      BrnInfConnue = BorneInfConnue[Var];
      if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	         BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) goto NextIl;
      NbT++;			
		}
		NextIl:
	  il++;
	}
	if ( NbT > MxTrm ) MxTrm = NbT;	
	il = ParLignePremiereContrainte[NbT];
 
	ParLignePremiereContrainte[NbT] = Cnt;	
	ParLigneContrainteSuivante[Cnt] = il;		
	
}
NbMaxTermesDesLignes = MxTrm;

LallocTas = 0;
LallocTas += NombreDeContraintes * sizeof( double ); /* V */
LallocTas += NombreDeContraintes * sizeof( char ); /* T */
LallocTas += NombreDeVariables * sizeof( char ); /* Flag */
LallocTas += NombreDeVariables * sizeof( int ); /* HashCode */
LallocTas += NombreDeVariables * sizeof( int ); /* NumVarDeCnt */
LallocTas += NombreDeVariables * sizeof( int ); /* NbTermesUtilesDeVar */

Buffer = (char *) malloc( LallocTas );
if ( Buffer == NULL ) {
  printf(" Solveur PNE , memoire insuffisante dans le presolve. Sous-programme: PRS_ColonnesColineaires \n");
	return;
}

pt = Buffer;
V = (double *) pt;
pt += NombreDeContraintes * sizeof( double );
T = (char *) pt;
pt +=  NombreDeContraintes * sizeof( char );
Flag = (char *) pt;
pt += NombreDeVariables * sizeof( char );
HashCode = (int *) pt;
pt += NombreDeVariables * sizeof( int );
NumVarDeCnt = (int *) pt;
pt += NombreDeVariables * sizeof( int ); 
NbTermesUtilesDeVar = (int *) pt;
pt += NombreDeVariables * sizeof( int );

memset( (char *) T, 0, NombreDeContraintes * sizeof( char ) );
memset( (char *) Flag, COLONNE_A_EVITER, NombreDeContraintes * sizeof( char ) );

NbVarDispo = 0;
for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  BrnInfConnue = BorneInfConnue[Var];
  if ( BrnInfConnue == FIXE_AU_DEPART || BrnInfConnue == FIXATION_SUR_BORNE_INF ||
	     BrnInfConnue == FIXATION_SUR_BORNE_SUP || BrnInfConnue == FIXATION_A_UNE_VALEUR ) continue;			
	Flag[Var] = COLONNE_A_ETUDIER;
	NbVarDispo++;
	
	/* Calcul d'un hashcode */
	/* On en profite pour dechainer les lignes qui correspondent a des contraintes inactives */
  NbT = 0;
	il = 0;
	ic = Cdeb[Var];
  while ( ic >= 0 ) {
	  Cnt = NumContrainte[ic];
    if ( ContrainteActivable[Cnt] == OUI_PNE && A[ic] != 0.0 ) {
		  NbT++;
		  il += Cnt;
		}		
		ic = Csui[ic];
  }
	HashCode[Var] = ( il + NbT ) % NombreDeContraintes;
	NbTermesUtilesDeVar[Var] = NbT;
	if ( NbT <= 0 ) Flag[Var] = COLONNE_A_EVITER;	
}

/* On balaye les lignes dans l'ordre croissant du nombre de termes et on ne compare que les
   les colonnes qui ont un terme dans cette ligne */

# if TEST_UNIQUEMENT_COLONNES_A_DEUX_TERMES_POUR_RESOLUTION == NON_PNE
  for ( NbT = 2 ; NbT <= NbMaxTermesDesLignes ; NbT++ ) {
# else
  for ( NbT = 2 ; NbT <= 2 ; NbT++ ) {
# endif
  if ( NbVarDispo <= 0 ) goto FinComparaisons;
  Cnt = ParLignePremiereContrainte[NbT];
	while ( Cnt >= 0 ) {
	  /*********************************************/
	  if ( ContrainteActivable[Cnt] == NON_PNE ) goto NextCnt;
    # if TEST_UNIQUEMENT_COLONNES_A_DEUX_TERMES_POUR_RESOLUTION == OUI_PNE
      if ( SensContrainte[Cnt] != '=' ) goto NextCnt;
    # endif		
	  /* On prepare la table des colonnes a explorer */
		il = Mdeb[Cnt];
		ilMax = il + NbTerm[Cnt];
		NbVarDeCnt = 0;
		while ( il < ilMax ) {
		  if ( A[il] != 0 ) {
        if ( Flag[Nuvar[il]] == COLONNE_A_ETUDIER ) {
				  NumVarDeCnt[NbVarDeCnt] = Nuvar[il];
					NbVarDeCnt++;
				}
		  }
	    il++;
	  }
	  for ( il = 0 ; il < NbVarDeCnt ; il++ ) {
      if ( NbVarDispo <= 0 ) goto FinComparaisons;
      Var = NumVarDeCnt[il];
		  if ( Flag[Var] == COLONNE_A_EVITER ) continue;
			NbTermesUtiles = NbTermesUtilesDeVar[Var];			
		  HashVar = HashCode[Var];
			Cvar = L[Var];
		  InitV = NON_PNE;
			
      /* On compare a Var toutes les variables suivantes de la contrainte */
	    for ( il1 = il + 1 ; il1 < NbVarDeCnt ; il1++ ) {
        if ( NbVarDispo <= 0 ) goto FinComparaisons;
        Var1 = NumVarDeCnt[il1];								
		    if ( Flag[Var1] == COLONNE_A_EVITER ) continue;
			  if ( HashCode[Var1] != HashVar ) continue;
        if ( NbTermesUtilesDeVar[Var1] != NbTermesUtiles ) continue;				
			  /* Comparaison de Var a Var1 et suppression eventuelle de Var1 */
			  if ( InitV == NON_PNE ) {
		      /* Preparation des tables pour la variable Var */
	        ic = Cdeb[Var];
	        while ( ic >= 0 ) {
					  CntDeVar = NumContrainte[ic];
		        V[CntDeVar] = A[ic];
		        T[CntDeVar] = 1;
		        ic = Csui[ic];
	        }
          InitV = OUI_PNE;			
			  }

        Nu = 1.; /* Juste pour eviter les warning de compilation */
        Nb = NbTermesUtiles;
        /* Determination du rapport */
        ic1 = Cdeb[Var1];
				while ( ContrainteActivable[NumContrainte[ic1]] == NON_PNE && ic1 >= 0 ) {
          # if TEST_UNIQUEMENT_COLONNES_A_DEUX_TERMES_POUR_RESOLUTION == OUI_PNE
            if ( SensContrainte[NumContrainte[ic1]] != '=' ) {
						  ic1 = -1;
							break;
						}
          # endif				
          ic1 = Csui[ic1];
				}
				if ( ic1 < 0 ) continue;
        Cnt1 = NumContrainte[ic1];
        if ( T[Cnt1] == 0 ) continue; /* Pas de terme correspondant dans la ligne */
        # if TEST_UNIQUEMENT_COLONNES_A_DEUX_TERMES_POUR_RESOLUTION == OUI_PNE
          if ( SensContrainte[Cnt1] != '=' ) continue;
        # endif								
        Nu = A[ic1] / V[Cnt1];
        Nb--;
        ic1 = Csui[ic1];

        /* On poursuit l'analyse de la variable Var1 */		
        while ( ic1 >= 0 ) {
          Cnt1 = NumContrainte[ic1];
					if ( ContrainteActivable[Cnt1] == NON_PNE ) goto NextIc1;
          if ( T[Cnt1] == 0 ) { Nb = 1; break; } /* Pas de terme correspondant dans la ligne */		
          if ( fabs( A[ic1] - (Nu * V[Cnt1] ) ) > ZERO_COLINEAIRE ) { Nb = 1; break; }
          # if TEST_UNIQUEMENT_COLONNES_A_DEUX_TERMES_POUR_RESOLUTION == OUI_PNE
            if ( SensContrainte[Cnt1] != '=' ) { Nb = 1; break; }
          # endif									
	        Nb--;
					NextIc1:
          ic1 = Csui[ic1];
        }
		
        if ( Nb == 0 ) {
          # if TEST_UNIQUEMENT_COLONNES_A_DEUX_TERMES_POUR_RESOLUTION == OUI_PNE
            printf("Systeme 2x2 qu'on peut resoudre \n");
						continue;
          # endif													
				  DeltaC = L[Var1] - ( Nu * Cvar );
	        if ( fabs( DeltaC ) > ZERO_COLINEAIRE ) {			
            if ( DeltaC > ZERO_COLINEAIRE ) {					
	            Flag[Var1] = COLONNE_A_EVITER;
              NouvelleValeur = ValeurDeBorneInf[Var1];
					    BorneMiseAJour = NON_PNE;
				      UneVariableAEteFixee = FIXATION_SUR_BORNE_INF;								
					    printf("Colonnes %d et %d colineaires avec couts differents Cout %e Cout1 %e on met %d a %e L[Var1] - ( Nu * Cvar ) %e Nu %e\n",
						          Var,Var1,L[Var],L[Var1],Var1,NouvelleValeur,L[Var1] - ( Nu * Cvar ),Nu);
              /*PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var1, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );*/
						}					
            else if ( DeltaC < -ZERO_COLINEAIRE ) {
	            Flag[Var1] = COLONNE_A_EVITER;
              NouvelleValeur = ValeurDeBorneSup[Var1];
					    BorneMiseAJour = NON_PNE;
				      UneVariableAEteFixee = FIXATION_SUR_BORNE_SUP;								
					    printf("Colonnes %d et %d colineaires avec couts differents Cout %e Cout1 %e on met %d a %e L[Var1] - ( Nu * Cvar ) %e Nu %e\n",
						          Var,Var1,L[Var],L[Var1],Var1,NouvelleValeur,L[Var1] - ( Nu * Cvar ),Nu);
              /*PNE_VariableProbingAppliquerLeConflictGraph( Pne, Var1, NouvelleValeur, BorneMiseAJour, UneVariableAEteFixee );*/
						}												
					}
	        /* A completer */
					
        }
				if ( Flag[Var] == COLONNE_A_EVITER ) break;															 
	    }
      /* RAZ de V et T avant de passer a la variable suivante */
		  Flag[Var] = COLONNE_A_EVITER;
			NbVarDispo--;
		  if ( InitV == OUI_PNE ) {
	      ic = Cdeb[Var];
 	      while ( ic >= 0 ) {
		      V[NumContrainte[ic]] = 0.0;
		      T[NumContrainte[ic]] = 0;
		      ic = Csui[ic];
	      }
		  }
  	}
	  /*********************************************/
    NextCnt:
		Cnt = ParLigneContrainteSuivante[Cnt];
	}
}

FinComparaisons:


printf("\n FinComparaisons \n\n");

free( Buffer );
free( ParLignePremiereContrainte );
free( ParLigneContrainteSuivante );

return;
}


