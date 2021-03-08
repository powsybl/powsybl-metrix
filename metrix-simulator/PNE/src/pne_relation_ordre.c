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

# define TRACES 0

# define ZERO_COLINEAIRE  1.e-12  

# define EPSILON_CONTRAINTE 1.e-8

# define INCREMENT_NOMBRE_DE_CONTRAINTES 100
# define INCREMENT_TAILLE_CONTRAINTES 200

/*----------------------------------------------------------------------------*/

void PNE_ContraintesDeRelationDordre(  PROBLEME_PNE * Pne )     
{
# if RELATION_DORDRE_DANS_LE_PROBING == NON_PNE
  return;
# else

int NombreDeVariables; int NombreDeContraintes; int * Mdeb; int * NbTerm; int Nb;
int * Nuvar; double * A; int Cnt; int il1; char InitV; int Var1; int ic; double * L;
int * Cdeb; int * Csui; int * NumContrainte; int ilMax; char * SensContrainte;
double * B; int HashVar; int NbVarDeCnt; int il; int * TypeDeVariable; int NbT;
int Var; int * NumVarDeCnt; char * ColonneAEtudier; int NbVarDispo; double * V;
char * T; double Cvar; int * HashCode; int CntDeVar; int * ParLignePremiereContrainte;
int * ParLigneContrainteSuivante; int * NbTermesUtilesDeVar; int NbTermesUtiles;
int icPrec; double Nu; int ic1; char * ContrainteActivable; int MxTrm; int NbMaxTermesDesLignes;
int Cnt1; int NbCntInact; GROUPE * G; int * NumeroDeGroupeDeVariablesEquivalentes; 
int NombreDeGroupesAlloues; int NombreDeGroupesDeVariablesEquivalentes; GROUPE ** Groupe;
char * BorneSupConnue; char * BorneInfConnue; double * ValeurDeBorneSup; double * ValeurDeBorneInf; 
char VariableFixe; char * BmaxValide; double * Bmax; char * BminValide; double * Bmin;

/*printf("PNE_ContraintesDeRelationDordre\n");*/

if ( Pne->ChainageTransposeeExploitable == NON_PNE ) PNE_ConstruireLeChainageDeLaTransposee( Pne );

NombreDeVariables = Pne->NombreDeVariablesTrav;
NombreDeContraintes = Pne->NombreDeContraintesTrav;

TypeDeVariable = Pne->TypeDeVariableTrav;
L = Pne->LTrav;

BorneSupConnue = Pne->ProbingOuNodePresolve->BorneSupConnue;
BorneInfConnue = Pne->ProbingOuNodePresolve->BorneInfConnue;
ValeurDeBorneSup = Pne->ProbingOuNodePresolve->ValeurDeBorneSup;
ValeurDeBorneInf = Pne->ProbingOuNodePresolve->ValeurDeBorneInf;

BmaxValide = Pne->ProbingOuNodePresolve->BmaxValide;
Bmax = Pne->ProbingOuNodePresolve->Bmax;
BminValide = Pne->ProbingOuNodePresolve->BminValide;
Bmin = Pne->ProbingOuNodePresolve->Bmin;

B = Pne->BTrav;
SensContrainte = Pne->SensContrainteTrav;
Mdeb = Pne->MdebTrav;
NbTerm = Pne->NbTermTrav;
Nuvar = Pne->NuvarTrav;

A = Pne->ATrav;
NumContrainte = Pne->NumContrainteTrav;

Cdeb = Pne->CdebTrav;   
Csui = Pne->CsuiTrav;

Groupe = Pne->Groupe;
if ( Groupe == NULL ) {
  NombreDeGroupesAlloues = INCREMENT_TAILLE_GROUPES;
  Groupe = (GROUPE **) malloc( NombreDeGroupesAlloues * sizeof( void * ) );
  if ( Groupe == NULL ) return;
  Pne->Groupe = Groupe;
  Pne->NombreDeGroupesDeVariablesEquivalentes = 0;
  Pne->NombreDeGroupesAlloues = 0;
}

NumeroDeGroupeDeVariablesEquivalentes = Pne->NumeroDeGroupeDeVariablesEquivalentes;
if ( NumeroDeGroupeDeVariablesEquivalentes == NULL ) {
  NumeroDeGroupeDeVariablesEquivalentes = (int *) malloc( NombreDeVariables * sizeof( int ) );
  if ( NumeroDeGroupeDeVariablesEquivalentes == NULL ) {
		return;
	}
  Pne->NumeroDeGroupeDeVariablesEquivalentes = NumeroDeGroupeDeVariablesEquivalentes;	
	for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) NumeroDeGroupeDeVariablesEquivalentes[Var] = -1;
}

if ( Pne->ContrainteActivable == NULL ) {
  Pne->ContrainteActivable = (char *) malloc( NombreDeContraintes * sizeof( char ) );
}
ParLignePremiereContrainte = (int *) malloc( (NombreDeVariables+1) * sizeof( int ) );
ParLigneContrainteSuivante = (int *) malloc( NombreDeContraintes * sizeof( int ) );
V = (double *) malloc( NombreDeContraintes * sizeof( double ) );
T = (char *) malloc( NombreDeContraintes * sizeof( char ) );
ColonneAEtudier = (char *) malloc( NombreDeVariables * sizeof( char ) );
HashCode = (int *) malloc( NombreDeVariables * sizeof( int ) );
NumVarDeCnt = (int *) malloc( NombreDeVariables * sizeof( int ) );
NbTermesUtilesDeVar = (int *) malloc( NombreDeVariables * sizeof( int ) );

if ( Pne->ContrainteActivable == NULL || ParLignePremiereContrainte == NULL || ParLigneContrainteSuivante == NULL ||
     V == NULL || T == NULL || ColonneAEtudier == NULL || HashCode == NULL || NumVarDeCnt == NULL ||
		 NbTermesUtilesDeVar == NULL ) {
	goto FinEtFree;
}

NombreDeGroupesDeVariablesEquivalentes = Pne->NombreDeGroupesDeVariablesEquivalentes;
NombreDeGroupesAlloues = Pne->NombreDeGroupesAlloues ;

for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
	VariableFixe = NON_PNE;
  if ( BorneInfConnue[Var] == FIXE_AU_DEPART ) VariableFixe = OUI_PNE;
	else if ( BorneInfConnue[Var] ==  FIXATION_SUR_BORNE_INF ) VariableFixe = OUI_PNE;  
	else if ( BorneInfConnue[Var] == FIXATION_SUR_BORNE_SUP ) VariableFixe = OUI_PNE;   
	else if ( BorneInfConnue[Var] == FIXATION_A_UNE_VALEUR ) VariableFixe = OUI_PNE;   
  else if ( BorneInfConnue[Var] == OUI_PNE && BorneSupConnue[Var] == OUI_PNE ) {
    if ( ValeurDeBorneInf[Var] == ValeurDeBorneSup[Var] ) VariableFixe = OUI_PNE;
	}		
	ColonneAEtudier[Var] = NON_PNE;
	if ( VariableFixe == NON_PNE && TypeDeVariable[Var] == ENTIER ) ColonneAEtudier[Var] = OUI_PNE;
}

ContrainteActivable = Pne->ContrainteActivable;
NbCntInact = 0;
for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
  T[Cnt] = 0;
  ContrainteActivable[Cnt] = OUI_PNE;
  if ( SensContrainte[Cnt] == '<' ) {
    if ( BmaxValide[Cnt] == OUI_PNE ) {
	    if ( Bmax[Cnt] <= B[Cnt] + EPSILON_CONTRAINTE ) {		   
	      ContrainteActivable[Cnt] = NON_PNE;	
	  	  NbCntInact++;
		    /*printf("Contrainte type %c %d non activable\n",SensContrainte[Cnt],Cnt);*/
	    }	  
    }	  
  }
  else {
    if ( BmaxValide[Cnt] == OUI_PNE ) {
	    if ( fabs ( Bmax[Cnt] - B[Cnt] ) <= EPSILON_CONTRAINTE ) {		  
	      ContrainteActivable[Cnt] = NON_PNE;
         NbCntInact++;
		    /*printf("Contrainte type %c %d non activable\n",SensContrainte[Cnt],Cnt);*/
	    }
    }	
    if ( BminValide[Cnt] == OUI_PNE ) {	
      if ( fabs ( Bmin[Cnt] - B[Cnt] ) <= EPSILON_CONTRAINTE ) {		  
	      ContrainteActivable[Cnt] = NON_PNE;	
        NbCntInact++;
		    /*printf("Contrainte type %c %d non activable\n",SensContrainte[Cnt],Cnt);*/
	    }	  
    }	  
  }	
}
/*printf("NbCntInact %d sur %d\n",NbCntInact,NombreDeContraintes);*/

NbVarDispo = 0;
for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  if ( ColonneAEtudier[Var] == NON_PNE ) continue;
  /* Calcul d'un hashcode */
  NbT = 0;
  il = 0;
  ic = Cdeb[Var];
  icPrec = -1;
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
  if ( NbT <= 0 ) ColonneAEtudier[Var] = NON_PNE;
  if ( ColonneAEtudier[Var] == OUI_PNE ) NbVarDispo++; 	
}

/* Classement des lignes en fonction du nombre de termes */

for ( NbT = 0 ; NbT <= NombreDeVariables ; NbT++ ) ParLignePremiereContrainte[NbT] = -1;
MxTrm = -1;
for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {		
  if ( ContrainteActivable[Cnt] == NON_PNE ) continue;
  NbT = 0;
  il = Mdeb[Cnt];
  ilMax = il + NbTerm[Cnt];
  while ( il < ilMax ) {
    if ( A[il] != 0 ) {
      if ( ColonneAEtudier[Nuvar[il]] == OUI_PNE ) NbT++;
    }
    il++;
  }
  if ( NbT > MxTrm ) MxTrm = NbT;	
  il = ParLignePremiereContrainte[NbT];
 
  ParLignePremiereContrainte[NbT] = Cnt;	
  ParLigneContrainteSuivante[Cnt] = il;		
	
}
NbMaxTermesDesLignes = MxTrm;

/* On balaye les lignes dans l'ordre croissant du nombre de termes et on ne compare que les
   les colonnes qui ont un terme dans cette ligne */
	 
for ( NbT = 2 ; NbT <= NbMaxTermesDesLignes ; NbT++ ) {
  if ( NbVarDispo <= 0 ) goto FinComparaisons;
  Cnt = ParLignePremiereContrainte[NbT];
  while ( Cnt >= 0 ) {
	  /* On prepare la table des colonnes a explorer */
	  il = Mdeb[Cnt];
	  ilMax = il + NbTerm[Cnt];
	  NbVarDeCnt = 0;
	  while ( il < ilMax ) {
	    if ( A[il] != 0 ) {
			  Var = Nuvar[il];
        if ( ColonneAEtudier[Var] == OUI_PNE ) {
		      NumVarDeCnt[NbVarDeCnt] = Var;
		      NbVarDeCnt++;
		    }
	    }
	    il++;
	  }
		
	  /************************************************************/
	  for ( il = 0 ; il < NbVarDeCnt ; il++ ) {			
      if ( NbVarDispo <= 0 ) goto FinComparaisons;
      Var = NumVarDeCnt[il];
	    if ( ColonneAEtudier[Var] == NON_PNE ) continue;
	    NbTermesUtiles = NbTermesUtilesDeVar[Var];			
	    HashVar = HashCode[Var];
	    Cvar = L[Var];
	    InitV = NON_PNE;
		
      /* On compare a Var toutes les variables entieres suivantes de la contrainte */
      for ( il1 = il + 1 ; il1 < NbVarDeCnt ; il1++ ) {			
        if ( NbVarDispo <= 0 ) goto FinComparaisons;
        Var1 = NumVarDeCnt[il1];

        if ( NumeroDeGroupeDeVariablesEquivalentes[Var1] >= 0 ) {
          if ( NumeroDeGroupeDeVariablesEquivalentes[Var] >= 0 ) {
					  if ( NumeroDeGroupeDeVariablesEquivalentes[Var] != NumeroDeGroupeDeVariablesEquivalentes[Var1] ) {
					    printf("NumeroDeGroupeDeVariablesEquivalentes %d %d\n",NumeroDeGroupeDeVariablesEquivalentes[Var],NumeroDeGroupeDeVariablesEquivalentes[Var1]);
					  }
						continue;
					}
				}
				
		    if ( ColonneAEtudier[Var1] == NON_PNE ) continue;				
		    if ( fabs( L[Var1] - Cvar ) > ZERO_COLINEAIRE )  continue;				
		    if ( HashCode[Var1] != HashVar ) continue;
        if ( NbTermesUtilesDeVar[Var1] != NbTermesUtiles ) continue;
				
	      /* Comparaison de Var a Var1 et suppression eventuelle de Var1 */
		    if ( InitV == NON_PNE ) {
		      /* Preparation des tables pour la variable Var */
	        ic = Cdeb[Var];
	        while ( ic >= 0 ) {
		        CntDeVar = NumContrainte[ic];
		        if ( ContrainteActivable[CntDeVar] == OUI_PNE && A[ic] != 0 ) {
		          V[CntDeVar] = A[ic];
		          T[CntDeVar] = 1;
			      }
		        ic = Csui[ic];
	        }  
		    }
        InitV = OUI_PNE;			  
        Nb = NbTermesUtiles;
        /* Comparaison */
        ic1 = Cdeb[Var1];
        while ( ic1 >= 0 ) {		
          Cnt1 = NumContrainte[ic1];
		      if ( ContrainteActivable[Cnt1] == OUI_PNE && A[ic1] != 0  ) {
            if ( T[Cnt1] == 0 ) { Nb = 1; break; } /* Pas de terme correspondant dans la ligne */						
            Nu = A[ic1] / V[Cnt1];
		        if (  fabs( Nu - 1. ) > ZERO_COLINEAIRE ) { Nb = 1 ; break; }						
            Nb--;			
		      }
          ic1 = Csui[ic1];
	      }
				
        if ( Nb == 0 ) {

          ColonneAEtudier[Var1] = NON_PNE;
	        NbVarDispo--;
				          
          if ( NumeroDeGroupeDeVariablesEquivalentes[Var] < 0 ) {
            /* On cree un groupe pour la variable */
	          G = (GROUPE *) malloc( sizeof( GROUPE ) );
			      if ( G == NULL ) goto FinComparaisons;
	          G->NombreDeVariablesDuGroupe = 0;
            G->NombreDeVariablesAllouees = INCREMENT_TAILLE_NOMBRE_DE_VARIABLES_DUN_GROUPE;
	          G->VariablesDuGroupe = (int *) malloc( G->NombreDeVariablesAllouees * sizeof( int ) );
	
            G->VariablesDuGroupe[G->NombreDeVariablesDuGroupe] = Var;
	          G->NombreDeVariablesDuGroupe++;
	
            G->VariablesDuGroupe[G->NombreDeVariablesDuGroupe] = Var1;
	          G->NombreDeVariablesDuGroupe++;
	
	          NumeroDeGroupeDeVariablesEquivalentes[Var] =  NombreDeGroupesDeVariablesEquivalentes;
	          NumeroDeGroupeDeVariablesEquivalentes[Var1] = NombreDeGroupesDeVariablesEquivalentes;

			      /* Si besoin on augmente la taille de Groupe */
			      if ( NombreDeGroupesDeVariablesEquivalentes >= NombreDeGroupesAlloues ) {
			        NombreDeGroupesAlloues += INCREMENT_TAILLE_GROUPES;
			        Groupe = (GROUPE **) realloc( Groupe, NombreDeGroupesAlloues * sizeof( void * ) );
              if ( Groupe == NULL ) goto FinComparaisons;
							Pne->Groupe = Groupe;
			      }
				
		        Groupe[NombreDeGroupesDeVariablesEquivalentes] = G;
            NombreDeGroupesDeVariablesEquivalentes++;				
	        }
		      else {
            /* On a deja un groupe pour la variable */
            G = Groupe[NumeroDeGroupeDeVariablesEquivalentes[Var]];
			      /* Si besoin on augmente la taille de Groupe */
	          if ( G->NombreDeVariablesDuGroupe >= G->NombreDeVariablesAllouees ) {
              G->NombreDeVariablesAllouees += INCREMENT_TAILLE_NOMBRE_DE_VARIABLES_DUN_GROUPE;	
	            G->VariablesDuGroupe = (int *) realloc( G->VariablesDuGroupe, G->NombreDeVariablesAllouees * sizeof( int ) );
			        if ( G->VariablesDuGroupe == NULL ) goto FinComparaisons;
	          }
				
            G->VariablesDuGroupe[G->NombreDeVariablesDuGroupe] = Var1;
	          G->NombreDeVariablesDuGroupe++;
	
	          NumeroDeGroupeDeVariablesEquivalentes[Var1] = NumeroDeGroupeDeVariablesEquivalentes[Var];
		      }					
        }
			}
    
      /* RAZ de V et T avant de passer a la variable suivante */
	    ColonneAEtudier[Var] = NON_PNE;
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
	  /************************************************************/
	  Cnt = ParLigneContrainteSuivante[Cnt];
	}
}

FinComparaisons:

if ( Pne->AffichageDesTraces == OUI_PNE ) {
  if ( NombreDeGroupesDeVariablesEquivalentes != 0 ) printf("Found %d sets of equivalent binaries\n",NombreDeGroupesDeVariablesEquivalentes);
}

# if TRACES == 1
  if ( Groupe != NULL ) {
	  printf("NombreDeGroupesDeVariablesEquivalentes: %d\n",NombreDeGroupesDeVariablesEquivalentes);
	  
    for ( Nb = 0 ; Nb < NombreDeGroupesDeVariablesEquivalentes ; Nb++ ) {
      G = Groupe[Nb];
	    printf("Groupe de variables equivalentes:\n");
	    for ( ic = 0 ; ic < G->NombreDeVariablesDuGroupe ; ic ++ ) {
        printf(" %d ", G->VariablesDuGroupe[ic]);
	    }
	    printf("\n");
    }
		
    fflush( stdout );
	}
# endif

if ( NombreDeGroupesDeVariablesEquivalentes == 0 ) goto FinEtFree;

/* Construction des relations d'ordre */
{ 

  # if METHODE_DE_PRISE_EN_COMPTE_DES_CONTRAINTES_DORDRE == CONTRAINTES_DORDRE_DANS_LE_GRAPHE
    int Var1; int Var2; int VariableInstanciee; double ValeurDeVariableInstanciee;
	
    for ( Nb = 0 ; Nb < NombreDeGroupesDeVariablesEquivalentes ; Nb++ ) {	
      G = Groupe[Nb];
      for ( ic = 0 ; ic < G->NombreDeVariablesDuGroupe - 1 ; ic ++ ) {	
        Var1 = G->VariablesDuGroupe[ic];
	      Var2 = G->VariablesDuGroupe[ic+1];			
	      /* X[Var2] - X[Var1] <= 0 */
	      VariableInstanciee = Var1;
	      ValeurDeVariableInstanciee = 0;
	      Pne->ProbingOuNodePresolve->NombreDeVariablesFixees = 1;
	      Pne->ProbingOuNodePresolve->NumeroDesVariablesFixees[0] = Var2;
	      Pne->ProbingOuNodePresolve->BorneInfConnue[Var2] = FIXATION_SUR_BORNE_INF;	
        PNE_MajConflictGraph( Pne, VariableInstanciee, ValeurDeVariableInstanciee );		
      }			
    }
  # endif
		
	/* Pour revenir a l'etat initial */
  memcpy( (char *) Pne->ProbingOuNodePresolve->BorneInfConnue, (char *) Pne->ProbingOuNodePresolve->BorneInfConnueSv, Pne->NombreDeVariablesTrav * sizeof( char ) );
  Pne->ProbingOuNodePresolve->NombreDeVariablesFixees = 0;
  Pne->ProbingOuNodePresolve->NbVariablesModifiees = 0;
  Pne->ProbingOuNodePresolve->NbContraintesModifiees = 0;
	
}

FinEtFree:

Pne->NombreDeGroupesDeVariablesEquivalentes = NombreDeGroupesDeVariablesEquivalentes;
Pne->NombreDeGroupesAlloues = NombreDeGroupesAlloues;

free( Pne->ContrainteActivable ); Pne->ContrainteActivable = NULL;
free( ParLignePremiereContrainte );
free( ParLigneContrainteSuivante );
free( V );
free( T );
free( ColonneAEtudier );
free( HashCode );
free( NumVarDeCnt );
free( NbTermesUtilesDeVar );

return;

# endif

}

/*----------------------------------------------------------------------------*/

# if RELATION_DORDRE_DANS_LE_PROBING == OUI_PNE

# if METHODE_DE_PRISE_EN_COMPTE_DES_CONTRAINTES_DORDRE == CONTRAINTES_DORDRE_DANS_LES_COUPES  

void PNE_ConstruireLesContraintesDOrdre( PROBLEME_PNE * Pne )
{
int Nb; int ic; GROUPE * G; GROUPE ** Groupe; int Var1; int Var2;

Groupe = Pne->Groupe;

for ( Nb = 0 ; Nb < Pne->NombreDeGroupesDeVariablesEquivalentes ; Nb++ ) {	
  G = Groupe[Nb];
  for ( ic = 0 ; ic < G->NombreDeVariablesDuGroupe - 1 ; ic ++ ) {	
    Var1 = G->VariablesDuGroupe[ic];
	  Var2 = G->VariablesDuGroupe[ic+1];			
	  /* X[Var2] - X[Var1] <= 0 */
    PNE_ConstruireUneContrainteDOrdre( Pne, Var1, -1.0, Var2, 1.0 );
  }			
}

return;
}

/*----------------------------------------------------------------------------*/

void PNE_ConstruireUneContrainteDOrdre( PROBLEME_PNE * Pne, int Var1, double Coeff1, int Var2, double Coeff2 )
{
int c; int Index; CONTRAINTES_DORDRE * ContraintesDOrdre; int i; int j;

/* Attention il faut verifier qu'elle n'existe pas deja ou plutot les generer a la fin du probing */

if ( Pne->ContraintesDOrdre == NULL ) {
  Pne->ContraintesDOrdre = (CONTRAINTES_DORDRE *) malloc( sizeof( CONTRAINTES_DORDRE ) );
	if ( Pne->ContraintesDOrdre == NULL ) return;
	
  ContraintesDOrdre = Pne->ContraintesDOrdre;
	ContraintesDOrdre->NombreDeContraintes = 0;
	
	ContraintesDOrdre->NbContraintesDOrdreAllouees  = INCREMENT_NOMBRE_DE_CONTRAINTES;
	i = ContraintesDOrdre->NbContraintesDOrdreAllouees;
  ContraintesDOrdre->First = (int *) malloc( i * sizeof( int ) );
  ContraintesDOrdre->LaContrainteEstDansLePool = (char *) malloc( i * sizeof( char ) );
  if ( ContraintesDOrdre->First == NULL || ContraintesDOrdre->LaContrainteEstDansLePool == NULL ) {
    free( ContraintesDOrdre->First ); free( ContraintesDOrdre->LaContrainteEstDansLePool );
    free( Pne->ContraintesDOrdre );
		Pne->ContraintesDOrdre = NULL;
		return;
  }
  ContraintesDOrdre->TailleContraintesDOrdreAllouee = INCREMENT_TAILLE_CONTRAINTES;
	i = ContraintesDOrdre->TailleContraintesDOrdreAllouee;
  ContraintesDOrdre->Coefficient = (double *) malloc( i * sizeof( double ) );
  ContraintesDOrdre->IndiceDeLaVariable = (int *) malloc( i * sizeof( int ) );
  if ( ContraintesDOrdre->Coefficient == NULL || ContraintesDOrdre->IndiceDeLaVariable == NULL ) {
    free( ContraintesDOrdre->First ); free( ContraintesDOrdre->LaContrainteEstDansLePool );
	  free( ContraintesDOrdre->Coefficient ); free( ContraintesDOrdre->IndiceDeLaVariable );		
    free( Pne->ContraintesDOrdre );
		Pne->ContraintesDOrdre = NULL;
		return;
  } 		
}

ContraintesDOrdre = Pne->ContraintesDOrdre;

c = ContraintesDOrdre->NombreDeContraintes - 1;
if ( c >= 0 ) Index = ContraintesDOrdre->First[c] + 2;
else Index = 0;
c++;

if ( c == ContraintesDOrdre->NbContraintesDOrdreAllouees ) {
  /* Il faut reallouer */	
	ContraintesDOrdre->NbContraintesDOrdreAllouees += INCREMENT_NOMBRE_DE_CONTRAINTES;
	i = ContraintesDOrdre->NbContraintesDOrdreAllouees;
  ContraintesDOrdre->First = (int *) realloc( ContraintesDOrdre->First, i * sizeof( int ) );
  ContraintesDOrdre->LaContrainteEstDansLePool = (char *) realloc( ContraintesDOrdre->LaContrainteEstDansLePool, i * sizeof( char ) );
  if ( ContraintesDOrdre->First == NULL || ContraintesDOrdre->LaContrainteEstDansLePool == NULL ) {
    free( ContraintesDOrdre->First ); free( ContraintesDOrdre->LaContrainteEstDansLePool );
    free( Pne->ContraintesDOrdre );
		Pne->ContraintesDOrdre = NULL;
		return;
  }		
}

ContraintesDOrdre->First[c] = Index;

j = 0;
if ( Index == ContraintesDOrdre->TailleContraintesDOrdreAllouee ) {	
  /* Il faut reallouer */
  ContraintesDOrdre->TailleContraintesDOrdreAllouee += INCREMENT_TAILLE_CONTRAINTES;
	i = ContraintesDOrdre->TailleContraintesDOrdreAllouee;
  ContraintesDOrdre->Coefficient = (double *) realloc( ContraintesDOrdre->Coefficient, i * sizeof( double ) );
  ContraintesDOrdre->IndiceDeLaVariable = (int *) realloc( ContraintesDOrdre->IndiceDeLaVariable, i * sizeof( int ) );
  if ( ContraintesDOrdre->Coefficient == NULL || ContraintesDOrdre->IndiceDeLaVariable == NULL ) {
    free( ContraintesDOrdre->First ); free( ContraintesDOrdre->LaContrainteEstDansLePool );
	  free( ContraintesDOrdre->Coefficient ); free( ContraintesDOrdre->IndiceDeLaVariable );		
    free( Pne->ContraintesDOrdre );
		Pne->ContraintesDOrdre = NULL;		
		return;
  } 				
}		   
ContraintesDOrdre->Coefficient[Index] = Coeff1;
ContraintesDOrdre->IndiceDeLaVariable[Index] = Var1;	
Index++;		

j++;
if ( Index == ContraintesDOrdre->TailleContraintesDOrdreAllouee ) {	
  /* Il faut reallouer */
  ContraintesDOrdre->TailleContraintesDOrdreAllouee += INCREMENT_TAILLE_CONTRAINTES;
	i = ContraintesDOrdre->TailleContraintesDOrdreAllouee;
  ContraintesDOrdre->Coefficient = (double *) realloc( ContraintesDOrdre->Coefficient, i * sizeof( double ) );
  ContraintesDOrdre->IndiceDeLaVariable = (int *) realloc( ContraintesDOrdre->IndiceDeLaVariable, i * sizeof( int ) );
  if ( ContraintesDOrdre->Coefficient == NULL || ContraintesDOrdre->IndiceDeLaVariable == NULL ) {
    free( ContraintesDOrdre->First ); free( ContraintesDOrdre->LaContrainteEstDansLePool );
	  free( ContraintesDOrdre->Coefficient ); free( ContraintesDOrdre->IndiceDeLaVariable );		
    free( Pne->ContraintesDOrdre );
		Pne->ContraintesDOrdre = NULL;		
		return;
  } 				
}		   
ContraintesDOrdre->Coefficient[Index] = Coeff2;
ContraintesDOrdre->IndiceDeLaVariable[Index] = Var2;	
Index++;		

ContraintesDOrdre->LaContrainteEstDansLePool[c] = NON_PNE;
ContraintesDOrdre->NombreDeContraintes++;

return;
}

# endif
  
# endif
