/***********************************************************************

   FONCTION: Recherche de contraintes colineaires.
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# define ZERO_COLINEAIRE  1.e-12 /*1.e-10*/
# define MARGE_POUR_INFAISABILITE  1.e-6 /*1.e-6*/
# define MAX_NBTMX  1000

# define CONTRAINTE_A_SUPPRIMER  1
# define CONTRAINTE_A_ETUDIER    2
# define CONTRAINTE_ETUDIEE      3

# define TRACES 0

void PNE_ComparerContraintes( PROBLEME_PNE * , int , int , double , double * , char * , int , char * , int * );
															 
/*----------------------------------------------------------------------------*/

void PNE_ContraintesColineaires( PROBLEME_PNE * Pne, int * NbCntElim, char * Reboucler )
{
int il; int ic; int ilDebCnt; int ilMaxCnt; int ic1;  int NombreDeContraintes; int NombreDeVariables;
int Cnt; int Cnt1; double * V; char * T; int LallocTas; double BCnt; double * B; int * Mdeb;
int * NbTerm; int * Nuvar; double * A; int * NumContrainte; int NbTermesUtilesDeCnt; double S;
char * Flag; char * pt; int * Cdeb; int * Csui; int NbContraintesSupprimees; int Var; int NbT;
int * ParColonnePremiereVariable; int * ParColonneVariableSuivante; char * Buffer; char * SensContrainte;
int HashCnt;  int * HashCode; char InitV; int NbCntDeVar; int * NumCntDeVar; int VarCnt; double * X;
int * TypeDeBorne; int NbMaxTermesDesColonnes; int * NumeroDesContraintesInactives;
int * CorrespondanceCntPneCntEntree; int DerniereContrainte;

if ( Pne->ChainageTransposeeExploitable == NON_PNE ) PNE_ConstruireLeChainageDeLaTransposee( Pne );

NbContraintesSupprimees = 0;

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

X = Pne->UTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;

LallocTas = 0;
LallocTas += NombreDeVariables * sizeof( double ); /* V */
LallocTas += NombreDeVariables * sizeof( char ); /* T */
LallocTas += NombreDeContraintes * sizeof( char ); /* Flag */
LallocTas += NombreDeContraintes * sizeof( int ); /* HashCode */
LallocTas += NombreDeContraintes * sizeof( int ); /* NumCntDeVar */

LallocTas += (NombreDeContraintes+1) * sizeof( int ); /* ParColonnePremiereVariable */
LallocTas += NombreDeVariables * sizeof( int ); /* ParColonneVariableSuivante */

Buffer = (char *) malloc( LallocTas );
if ( Buffer == NULL ) {
  printf(" Solveur PNE , memoire insuffisante dans le presolve. Sous-programme: PNE_ContraintesColineaires \n");
	return;
}

pt = Buffer;
V = (double *) pt;
pt += NombreDeVariables * sizeof( double );
T = (char *) pt;
pt +=  NombreDeVariables * sizeof( char );
Flag = (char *) pt;
pt += NombreDeContraintes * sizeof( char );
HashCode = (int *) pt;
pt += NombreDeContraintes * sizeof( int );
NumCntDeVar = (int *) pt;
pt += NombreDeContraintes * sizeof( int );

ParColonnePremiereVariable = (int *) pt;
pt += (NombreDeContraintes+1) * sizeof( int );
ParColonneVariableSuivante = (int *) pt;
pt += NombreDeVariables * sizeof( int );

for ( NbT = 0 ; NbT <= NombreDeContraintes ; NbT++ ) ParColonnePremiereVariable[NbT] = -1;
NbMaxTermesDesColonnes = -1;
for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  if ( TypeDeBorne[Var] == VARIABLE_FIXE ) continue;
  NbT = 0;
	ic = Cdeb[Var];
  while ( ic >= 0 ) {
		if ( A[ic] != 0 ) NbT++;					
		ic = Csui[ic];
  }
	if ( NbT > NbMaxTermesDesColonnes ) NbMaxTermesDesColonnes = NbT;	
	ic = ParColonnePremiereVariable[NbT];	
	ParColonnePremiereVariable[NbT] = Var;	  
	ParColonneVariableSuivante[Var] = ic;	
}

memset( (char *) T, 0, NombreDeVariables * sizeof( char ) );

for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  Flag[Cnt] = CONTRAINTE_A_ETUDIER;	
	/* Calcul d'un hashcode */
	NbT = 0;
	ic = 0;
  il = Mdeb[Cnt];
  ilMaxCnt = il + NbTerm[Cnt];
  while ( il < ilMaxCnt ) {
    if ( A[il] != 0.0 ) {
      Var = Nuvar[il];
		  if ( TypeDeBorne[Var] != VARIABLE_FIXE ) {
			  NbT++;
				ic += Var;
			}
		}
	  il++;
  }
	HashCode[Cnt] = ( ic + NbT ) % NombreDeVariables;
}

/* On balaye les colonnes dans l'ordre croissant du nombre de termes et on ne compare que les
   les lignes qui ont un terme dans cette colonne */

for ( NbT = 2 ; NbT <= NbMaxTermesDesColonnes ; NbT++ ) {
  Var = ParColonnePremiereVariable[NbT];
	while ( Var >= 0 ) {
	  /*********************************************/
	  if ( TypeDeBorne[Var] == VARIABLE_FIXE ) goto NextVar;
	  /* On prepare la table des contraintes a explorer */
 	  NbCntDeVar = 0;
	  ic = Cdeb[Var];
    while ( ic >= 0 ) {
		  if ( A[ic] != 0 ) {
		    Cnt = NumContrainte[ic];
        if ( Flag[Cnt] == CONTRAINTE_A_ETUDIER ) {
			    NumCntDeVar[NbCntDeVar] = Cnt;
				  NbCntDeVar++;
			  }
      }
	    ic = Csui[ic];
	  }

    if ( NbCntDeVar > MAX_NBTMX ) NbCntDeVar = MAX_NBTMX;

	  for ( ic = 0 ; ic < NbCntDeVar ; ic++ ) {
	    Cnt = NumCntDeVar[ic];
		  if ( Flag[Cnt] == CONTRAINTE_A_SUPPRIMER || Flag[Cnt] == CONTRAINTE_ETUDIEE ) continue;
			
      ilDebCnt = 0;
		  HashCnt = HashCode[Cnt];
		  InitV = NON_PNE;
			ilMaxCnt = -1;
			BCnt = LINFINI_PNE;
			NbTermesUtilesDeCnt = 0;
      /* On compare a Cnt toutes les contraintes suivantes de la colonne */
	    for ( ic1 = ic + 1 ; ic1 < NbCntDeVar ; ic1++ ) {
	      Cnt1 = NumCntDeVar[ic1];
			  if ( HashCode[Cnt1] != HashCnt ) continue;
		    if ( Flag[Cnt1] != CONTRAINTE_A_ETUDIER || Flag[Cnt1] == CONTRAINTE_A_SUPPRIMER ) continue;	
			  /* Comparaison de Cnt a Cnt1 */
			  if ( InitV == NON_PNE ) {
		      /* Preparation des tables pour la contrainte Cnt */
			    S = 0.0;
          ilDebCnt = Mdeb[Cnt];
	        ilMaxCnt = ilDebCnt + NbTerm[Cnt];
	        il = ilDebCnt;
	        while ( il < ilMaxCnt ) {
			      if ( A[il] != 0.0 ) {
					    VarCnt = Nuvar[il];														
				      if ( TypeDeBorne[VarCnt] != VARIABLE_FIXE ) {
		            V[VarCnt] = A[il];
		            T[VarCnt] = 1;
						    NbTermesUtilesDeCnt++;
					    }
					    else S += A[il] * X[VarCnt];
				    }  
		        il++;
	        }
		      BCnt = B[Cnt] - S;
          InitV = OUI_PNE;
					if ( NbTermesUtilesDeCnt <= 0 ) break;					
			  }
        PNE_ComparerContraintes( Pne, Cnt, NbTermesUtilesDeCnt, BCnt, V, T,
				                         Cnt1, Flag, &NbContraintesSupprimees );

				/* Si le Flag de Cnt a change on passe a la contrainte suivante */
		    if ( Flag[Cnt] == CONTRAINTE_A_SUPPRIMER || Flag[Cnt] == CONTRAINTE_ETUDIEE ) break;			
																 
	    }
      /* RAZ de V et T avant de passer a la contrainte suivante */
      if ( Flag[Cnt] != CONTRAINTE_A_SUPPRIMER ) Flag[Cnt] = CONTRAINTE_ETUDIEE;
			
		  if ( InitV == OUI_PNE ) {
        il = ilDebCnt;
 	      while ( il < ilMaxCnt ) {
		      V[Nuvar[il]] = 0.0;
		      T[Nuvar[il]] = 0;
		      il++;
	      }
		  }
  	}
	  /*********************************************/
		NextVar:
	  Var = ParColonneVariableSuivante[Var];
	}
}

if ( NbContraintesSupprimees > 0 ) {
  Pne->ChainageTransposeeExploitable = NON_PNE;

	NumeroDesContraintesInactives = Pne->NumeroDesContraintesInactives;
  CorrespondanceCntPneCntEntree = Pne->CorrespondanceCntPneCntEntree;
	
  for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
    if ( Flag[Cnt] != CONTRAINTE_A_SUPPRIMER ) continue;
		
		/* On inverse avec la derniere contrainte */		
		
    NumeroDesContraintesInactives[Pne->NombreDeContraintesInactives] = CorrespondanceCntPneCntEntree[Cnt];
    Pne->NombreDeContraintesInactives++;

 	  DerniereContrainte = NombreDeContraintes - 1;				
 	  Mdeb[Cnt]   = Mdeb[DerniereContrainte];
    NbTerm[Cnt] = NbTerm[DerniereContrainte];				
    B[Cnt]      = B[DerniereContrainte];		
    SensContrainte[Cnt] = SensContrainte[DerniereContrainte];
    Flag[Cnt] = Flag[DerniereContrainte];	
    CorrespondanceCntPneCntEntree[Cnt] = CorrespondanceCntPneCntEntree[DerniereContrainte];		
	  Cnt--;
	  NombreDeContraintes--;
		
  }
	Pne->NombreDeContraintesTrav = NombreDeContraintes;	
}

free( Buffer );

# if TRACES == 1
  if ( NbContraintesSupprimees > 0 ) {
    printf("Nombre de contraintes supprimees par colinearite %d\n",NbContraintesSupprimees);
	}
# endif

if ( Pne->AffichageDesTraces == OUI_PNE ) {
  if ( NbContraintesSupprimees > 0 ) printf("Found %d colinear constraint(s)\n",NbContraintesSupprimees);
  else printf("No colinear constraints found\n");
}

*NbCntElim = *NbCntElim + NbContraintesSupprimees;
if ( NbContraintesSupprimees > 0 ) *Reboucler = OUI_PNE;

return;
}

/*----------------------------------------------------------------------------*/

void PNE_ComparerContraintes( PROBLEME_PNE * Pne, int Cnt, int NbTermesUtilesDeCnt, double BCnt,
                              double * V, char * T, int Cnt1, char * Flag, int * NbContraintesSupprimees )
{
int Nb; double S1; int il1; int il1Max; int * Mdeb; int * NbTerm; double * A; int * Nuvar;
int Var1; double * B; double BCnt1; double Rapport; double * X; char * SensContrainte;
int * TypeDeBorne; char SensCnt1; char SensCnt; double x;

SensContrainte = Pne->SensContrainteTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
B = Pne->BTrav;
A = Pne->ATrav;
Nuvar = Pne->NuvarTrav;

X = Pne->UTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;

Nb = NbTermesUtilesDeCnt;
S1 = 0.0;		
il1 = Mdeb[Cnt1];
il1Max = il1 + NbTerm[Cnt1];
/* Determination du rapport */
Rapport = 1.0;
while ( il1 < il1Max ) {
  if ( A[il1] != 0.0 ) {
    Var1 = Nuvar[il1];
	  if ( TypeDeBorne[Var1] == VARIABLE_FIXE ) S1 += A[il1] * X[Var1];  
	  else {
      if ( T[Var1] == 0 ) return; /* Pas de terme correspondant dans la colonne */		
		  Rapport = V[Var1] / A[il1];
      Nb--;
			il1++;
			break;
	  }
	}
  il1++;
}

/* On poursuit l'analyse de la contrainte Cnt1 */
while ( il1 < il1Max ) {
  if ( A[il1] != 0.0 ) {
    Var1 = Nuvar[il1];
	  if ( TypeDeBorne[Var1] == VARIABLE_FIXE ) S1 += A[il1] * X[Var1];  
    else {
		  if ( T[Var1] == 0 ) return;
			x = fabs( V[Var1] - ( Rapport * A[il1]) );
      if ( x > ZERO_COLINEAIRE ) return;			
	    Nb--;
		}
	}
	il1++;
}

if ( Nb != 0 ) return;

SensCnt = SensContrainte[Cnt];

BCnt1 = B[Cnt1] - S1;
SensCnt1 = SensContrainte[Cnt1];

if ( SensCnt == '=' ) {
  if ( SensCnt1 == '=' ) {
	  x = fabs( BCnt - (Rapport * BCnt1) );
    if ( x > ZERO_COLINEAIRE ) return; /* Il est preferable de ne pas se prononcer sur la faisabilite */
	  /* Cnt et Cnt1 sont colineaires on inhibe Cnt1 */		
		Flag[Cnt1] = CONTRAINTE_A_SUPPRIMER;
    *NbContraintesSupprimees = *NbContraintesSupprimees + 1;		
	}
  else if ( SensCnt1 == '<' ) {
	  if ( Rapport > 0.0 ) {
      if ( Rapport * BCnt1 < BCnt - MARGE_POUR_INFAISABILITE ) {
				Pne->YaUneSolution = PROBLEME_INFAISABLE;
        return;
			}
			else if ( Rapport * BCnt1 >= BCnt ) {
	      /* Cnt et Cnt1 sont colineaires on inhibe Cnt1 */				
				Flag[Cnt1] = CONTRAINTE_A_SUPPRIMER;												
        *NbContraintesSupprimees = *NbContraintesSupprimees + 1;        				
			}
		}
		else {
      /* Le rapport est negatif */
      if ( Rapport * BCnt1 > BCnt + MARGE_POUR_INFAISABILITE ) {
				Pne->YaUneSolution = PROBLEME_INFAISABLE;
        return;				
			}
			else if ( Rapport * BCnt1 <= BCnt ) {
	      /* Cnt et Cnt1 sont colineaires on inhibe Cnt1 */				
				Flag[Cnt1] = CONTRAINTE_A_SUPPRIMER;								
        *NbContraintesSupprimees = *NbContraintesSupprimees + 1;								
			}
		}		
	}
}
else {
  /* La contrainte Cnt est de type < */
  if ( SensCnt1 == '=' ) {
    if ( BCnt < (Rapport * BCnt1) - MARGE_POUR_INFAISABILITE ) {
			Pne->YaUneSolution = PROBLEME_INFAISABLE;
      return;				
		}
		else if ( BCnt >= Rapport * BCnt1 ) {
	    /* Cnt et Cnt1 sont colineaires on inhibe Cnt */
			Flag[Cnt] = CONTRAINTE_A_SUPPRIMER;							
      *NbContraintesSupprimees = *NbContraintesSupprimees + 1;		
		}		
	}
	else {	
	  /* La contrainte Cnt1 est aussi de type < */
	  if ( Rapport > 0.0 ) {		
		  if ( BCnt < Rapport * BCnt1 ) {			
			  /* On inhibe Cnt1 */
				Flag[Cnt1] = CONTRAINTE_A_SUPPRIMER;				
        *NbContraintesSupprimees = *NbContraintesSupprimees + 1;		
			}
			else {			
			  /* On inhibe Cnt */
				Flag[Cnt] = CONTRAINTE_A_SUPPRIMER;				
        *NbContraintesSupprimees = *NbContraintesSupprimees + 1;		
			}
		}
		else {		
		  /* Rapport < 0 */
			if ( Rapport * BCnt1 > BCnt + MARGE_POUR_INFAISABILITE ) {
			  Pne->YaUneSolution = PROBLEME_INFAISABLE;
        return;						
			}
			else if ( fabs( BCnt - (Rapport * BCnt1) ) < ZERO_COLINEAIRE ) {
        /* On inhibe Cnt1 */				
				Flag[Cnt1] = CONTRAINTE_A_SUPPRIMER;				
        *NbContraintesSupprimees = *NbContraintesSupprimees + 1;				
				/* On transforme Cnt en = */				
        SensContrainte[Cnt] = '=';			
			}
		}
	}
}

return;
}


  
