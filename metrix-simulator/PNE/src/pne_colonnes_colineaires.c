/***********************************************************************

   FONCTION: Recherche des colonnes colineaires.					 
                
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

# define TRACES 1

# define ZERO_COLINEAIRE  1.e-12
# define SEUIL_POUR_DELTAC_NON_NUL 1.e-7 /* Pour affirmer que DeltaC est non nul */
# define SEUIL_POUR_DELTAC_NUL 1.e-9 /* Pour affirmer que DeltaC est nul */
# define MARGE_EGALITE_BORNE_MIN_ET_MAX  1.e-8
# define MAX_NBTMX 1000000000 /*10000*/
  
# define COLONNE_A_ETUDIER 0
# define COLONNE_A_EVITER  1

# if PRISE_EN_COMPTE_DES_GROUPES_DE_VARIABLES_EQUIVALENTS == OUI_PNE
												
/*----------------------------------------------------------------------------*/

void PNE_ColonnesColineaires(  PROBLEME_PNE * Pne )     
{
int NombreDeVariables; int NombreDeContraintes; int * Mdeb; int * NbTerm; int Nb;
int * Nuvar; double * A; int Cnt; int il1; char InitV; int Var1; int ic; double * L;
int * Cdeb; int * Csui; int * NumContrainte; int ilMax; char * SensContrainte;
double * B; int HashVar; int NbVarDeCnt; int il; char * Buffer; int * TypeDeVariable;
int * TypeDeBorne; int NbT; int Var; int * NumVarDeCnt; char * Flag; int NbVarDispo;
double * V; int LallocTas; char * T; char * pt; double Cvar; int * HashCode;
int CntDeVar; int * ParLignePremiereContrainte; int * ParLigneContrainteSuivante;
int * NbTermesUtilesDeVar; int NbTermesUtiles; int icPrec; double Nu; int ic1;
char * ContrainteActivable; int MxTrm; int NbMaxTermesDesLignes; int Cnt1;
GROUPE * G; int * NumeroDeGroupeDeVariablesEquivalentes; double * Xmin; double * Xmax;

/*return;*/ 

if ( Pne->ChainageTransposeeExploitable == NON_PNE ) PNE_ConstruireLeChainageDeLaTransposee( Pne );

NombreDeVariables = Pne->NombreDeVariablesTrav;
NombreDeContraintes = Pne->NombreDeContraintesTrav;

TypeDeVariable = Pne->TypeDeVariableTrav;
TypeDeBorne = Pne->TypeDeBorneTrav;
Xmin = Pne->UminTrav;
Xmax = Pne->UmaxTrav;
L = Pne->LTrav;

NumeroDeGroupeDeVariablesEquivalentes = Pne->NumeroDeGroupeDeVariablesEquivalentes;
Pne->LesGroupesDeVariablesEquivalentesSontValides = NON_PNE;		

B = Pne->BTrav;
SensContrainte = Pne->SensContrainteTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
Nuvar = Pne->NuvarTrav;

A = Pne->ATrav;
NumContrainte = Pne->NumContrainteTrav;

Cdeb = Pne->CdebTrav;   
Csui = Pne->CsuiTrav;

if ( Pne->ContrainteActivable == NULL ) {
  Pne->ContrainteActivable = (char *) malloc( Pne->NombreDeContraintesTrav * sizeof( char ) );
  if ( Pne->ContrainteActivable == NULL ) {
    printf(" Solveur PNE , memoire insuffisante. Sous-programme: PNE_InitPne \n");
    Pne->AnomalieDetectee = OUI_PNE;
    longjmp( Pne->Env , Pne->AnomalieDetectee ); /* rq: le 2eme argument ne sera pas utilise */
	}
  for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) Pne->ContrainteActivable[Cnt] = OUI_PNE;
}
ContrainteActivable = Pne->ContrainteActivable;

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
      if ( TypeDeBorne[Nuvar[il]] != VARIABLE_FIXE ) NbT++;
		}
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
  if ( TypeDeBorne[Var] == VARIABLE_FIXE ) continue; 
  if ( TypeDeVariable[Var] != ENTIER )	continue;
	if ( Xmin[Var] == Xmax[Var] ) continue; 	
	
  /* On n'etudie que les variables entieres */
	
	Flag[Var] = COLONNE_A_ETUDIER;
	NbVarDispo++;
	
	/* Calcul d'un hashcode */
	/* On en profite pour dechainer les lignes qui correspondent a des contraintes inactives */
  NbT = 0;
	il = 0;
	ic = Cdeb[Var];
	icPrec = -1;
  while ( ic >= 0 ) {
	  Cnt = NumContrainte[ic];
    if ( ContrainteActivable[Cnt] == OUI_PNE && A[ic] != 0.0 ) {
		  NbT++;
		  il += Cnt;
		  icPrec = ic;			
		  ic = Csui[ic];					
		}
		else {
	    /* On en profite pour dechainer les lignes qui correspondent a des contraintes inactives */
      ic = Csui[ic];
			if ( icPrec >= 0 ) {
			  Csui[icPrec] = ic;
			}
			else {
			  Cdeb[Var] = ic;
				if ( ic < 0 ) break; /* Attention s'il ne reste plus rien dans la ligne il faut sortir */
			}
		}		
  }
	HashCode[Var] = ( il + NbT ) % NombreDeContraintes;
	NbTermesUtilesDeVar[Var] = NbT;
	if ( NbT <= 0 ) Flag[Var] = COLONNE_A_EVITER;

  NumeroDeGroupeDeVariablesEquivalentes[Var] = -1;
	
}

/* Raz des groupes */
if ( Pne->Groupe != NULL ) {
  for ( Nb = 0 ; Nb < Pne->NombreDeGroupesDeVariablesEquivalentes ; Nb++ ) {
	  free( Pne->Groupe[Nb]->VariablesDuGroupe );
	  free( Pne->Groupe[Nb] );
	}
  free( Pne->Groupe );		
  Pne->Groupe = NULL;	
}
Pne->NombreDeGroupesDeVariablesEquivalentes = 0;

/* On balaye les lignes dans l'ordre croissant du nombre de termes et on ne compare que les
   les colonnes qui ont un terme dans cette ligne */

for ( NbT = 2 ; NbT <= NbMaxTermesDesLignes ; NbT++ ) {
  if ( NbVarDispo <= 0 ) goto FinComparaisons;
  Cnt = ParLignePremiereContrainte[NbT];
	while ( Cnt >= 0 ) {
	  /*********************************************/
	  if ( ContrainteActivable[Cnt] == NON_PNE ) goto NextCnt;
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
			Flag[Var] = COLONNE_A_EVITER;
			NbTermesUtiles = NbTermesUtilesDeVar[Var];			
		  HashVar = HashCode[Var];
			Cvar = L[Var];
		  InitV = NON_PNE;
			
      /* On compare a Var toutes les variables entieres suivantes de la contrainte */
	    for ( il1 = il + 1 ; il1 < NbVarDeCnt ; il1++ ) {
        if ( NbVarDispo <= 0 ) goto FinComparaisons;
        Var1 = NumVarDeCnt[il1];								
		    if ( Flag[Var1] == COLONNE_A_EVITER ) continue;
			  if ( HashCode[Var1] != HashVar ) continue;
				if ( fabs( L[Var1] - Cvar ) > ZERO_COLINEAIRE ) continue;

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
        Cnt1 = NumContrainte[ic1];
        if ( T[Cnt1] == 0 ) continue; /* Pas de terme correspondant dans la ligne */		
        Nu = A[ic1] / V[Cnt1];
		    if (  fabs( Nu - 1. ) > ZERO_COLINEAIRE ) continue;
        Nb--;
        ic1 = Csui[ic1];

        /* On poursuit l'analyse de la variable Var1 */		
        while ( ic1 >= 0 ) {
          Cnt1 = NumContrainte[ic1];
          if ( T[Cnt1] == 0 ) { Nb = 1; break; } /* Pas de terme correspondant dans la ligne */		
          if ( fabs( A[ic1] - (Nu * V[Cnt1] ) ) > ZERO_COLINEAIRE ) { Nb = 1; break; }
	        Nb--;			
          ic1 = Csui[ic1];
        }
		
        if ( Nb == 0 ) {
			
	        Flag[Var1] = COLONNE_A_EVITER;

          if ( Pne->Groupe == NULL ) {
				    Pne->NombreDeGroupesAlloues = INCREMENT_TAILLE_GROUPES;
			      Pne->Groupe = (GROUPE **) malloc( Pne->NombreDeGroupesAlloues * sizeof( void * ) );
            if ( Pne->Groupe == NULL ) goto FinColonnesColineaires;
			    }
			
          if ( Pne->NumeroDeGroupeDeVariablesEquivalentes[Var] < 0 ) {
            /* On cree un groupe pour la variable */
	          G = (GROUPE *) malloc( sizeof( GROUPE ) );
				    if ( G == NULL ) goto FinColonnesColineaires;
	          G->NombreDeVariablesDuGroupe = 0;
            G->NombreDeVariablesAllouees = INCREMENT_TAILLE_NOMBRE_DE_VARIABLES_DUN_GROUPE;
	          G->VariablesDuGroupe = (int *) malloc( G->NombreDeVariablesAllouees * sizeof( int ) );
	
            G->VariablesDuGroupe[G->NombreDeVariablesDuGroupe] = Var;
	          G->NombreDeVariablesDuGroupe++;
	
            G->VariablesDuGroupe[G->NombreDeVariablesDuGroupe] = Var1;
	          G->NombreDeVariablesDuGroupe++;
	
	          Pne->NumeroDeGroupeDeVariablesEquivalentes[Var] =  Pne->NombreDeGroupesDeVariablesEquivalentes;
	          Pne->NumeroDeGroupeDeVariablesEquivalentes[Var1] = Pne->NombreDeGroupesDeVariablesEquivalentes;

				    /* Si besoin on augmente la taille de Groupe */
				    if ( Pne->NombreDeGroupesDeVariablesEquivalentes >= Pne->NombreDeGroupesAlloues ) {
				      Pne->NombreDeGroupesAlloues += INCREMENT_TAILLE_GROUPES;
			        Pne->Groupe = (GROUPE **) realloc( Pne->Groupe, Pne->NombreDeGroupesAlloues * sizeof( void * ) );
              if ( Pne->Groupe == NULL ) goto FinColonnesColineaires;
				    }
				
				    Pne->Groupe[Pne->NombreDeGroupesDeVariablesEquivalentes] = G;
            Pne->NombreDeGroupesDeVariablesEquivalentes++;
				
			    }
			    else {
            /* On a deja un groupe pour la variable */
            G = Pne->Groupe[Pne->NumeroDeGroupeDeVariablesEquivalentes[Var]];
				    /* Si besoin on augmente la taille de Groupe */
	          if ( G->NombreDeVariablesDuGroupe >= G->NombreDeVariablesAllouees ) {
              G->NombreDeVariablesAllouees += INCREMENT_TAILLE_NOMBRE_DE_VARIABLES_DUN_GROUPE;	
	            G->VariablesDuGroupe = (int *) realloc( G->VariablesDuGroupe, G->NombreDeVariablesAllouees * sizeof( int ) );
					    if ( G->VariablesDuGroupe == NULL ) goto FinColonnesColineaires;
	          }
				
            G->VariablesDuGroupe[G->NombreDeVariablesDuGroupe] = Var1;
	          G->NombreDeVariablesDuGroupe++;
	
	          Pne->NumeroDeGroupeDeVariablesEquivalentes[Var1] = Pne->NumeroDeGroupeDeVariablesEquivalentes[Var];

			    }			
		
        }
															 
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

Pne->LesGroupesDeVariablesEquivalentesSontValides = OUI_PNE;		

FinColonnesColineaires:

free( Buffer );
free( ParLignePremiereContrainte );
free( ParLigneContrainteSuivante );

# if TRACES == 1
  for ( Nb = 0 ; Nb < Pne->NombreDeGroupesDeVariablesEquivalentes ; Nb++ ) {
	  G = Pne->Groupe[Nb];
		printf("Groupe de variables equivalentes:\n");
		for ( ic = 0 ; ic < G->NombreDeVariablesDuGroupe ; ic ++ ) {
      printf(" %d ", G->VariablesDuGroupe[ic]);
		}
		printf("\n");
	}
# endif

return;
}

/*----------------------------------------------------------------------------*/

# endif
