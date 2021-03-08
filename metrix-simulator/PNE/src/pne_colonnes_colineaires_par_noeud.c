/***********************************************************************

   FONCTION: Recherche des colonnes colineaires. Apres la resolution d'un
	           probleme relaxe et avant le strong branching.
                
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

# define COLONNE_A_ETUDIER 0
# define COLONNE_A_EVITER  1

# if PRISE_EN_COMPTE_DES_GROUPES_DE_VARIABLES_EQUIVALENTS == OUI_PNE

/*----------------------------------------------------------------------------*/

void PNE_ColonnesColineairesParNoeud( PROBLEME_PNE * Pne )     
{
double * Bmin; double * Bmax; double * B; double * A; int ic1; int Cnt1; double * V;
char * BminValide; char * BmaxValide; char * ContrainteActivable; char * SensContrainte;
int * SuivFrac; double Nu; int Nb; int Var1; int CntDeVar; int ilMax; char InitV;
int NombreDeVariables; int NombreDeContraintes; int Cnt; int HashVar; int NbTermesUtiles;
int * TypeDeVariable; int * TypeDeBorne; int NbT; int Var; char * Flag; double Cvar;
int * Mdeb; int * NbTerm; int * Nuvar; int * Cdeb; int * Csui; int * NumContrainte;
PROBING_OU_NODE_PRESOLVE * ProbingOuNodePresolve; char * pt; int * NbTermesUtilesDeVar;
int * HashCode; int il; int ic; int icPrec; char * T; double * L; char * Buffer; int LallocTas;
double * Xmin; double * Xmax; int * NumeroDeGroupeDeVariablesEquivalentes; GROUPE * G;

return;

ProbingOuNodePresolve = Pne->ProbingOuNodePresolve;
if ( ProbingOuNodePresolve == NULL ) return;

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

/* Les valeurs de Bmin/Bmax du node presolve n'ont pas change */

Bmin = ProbingOuNodePresolve->Bmin;
Bmax = ProbingOuNodePresolve->Bmax;
BminValide = ProbingOuNodePresolve->BminValide;
BmaxValide = ProbingOuNodePresolve->BmaxValide;

LallocTas = 0;
LallocTas += NombreDeContraintes * sizeof( double ); /* V */
LallocTas += NombreDeContraintes * sizeof( char ); /* T */
LallocTas += NombreDeVariables * sizeof( char ); /* Flag */
LallocTas += NombreDeVariables * sizeof( int ); /* HashCode */
LallocTas += NombreDeVariables * sizeof( int ); /* NbTermesUtilesDeVar */

ContrainteActivable = (char *) malloc( NombreDeContraintes * sizeof( char ) );
Cdeb = (int *) malloc( NombreDeVariables * sizeof( int ) );
Csui = (int *) malloc( Pne->TailleAlloueePourLaMatriceDesContraintes * sizeof( int ) );
Buffer = (char *) malloc( LallocTas );
if ( ContrainteActivable == NULL || Cdeb == NULL || Csui == NULL || Buffer == NULL ) {
  free( ContrainteActivable ); free( Cdeb ); free( Csui ); free( Buffer ); 
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
NbTermesUtilesDeVar = (int *) pt;
pt += NombreDeVariables * sizeof( int );

memcpy( (char *) ContrainteActivable, (char *) Pne->ContrainteActivable, NombreDeContraintes * sizeof( char ) );
memcpy( (char *) Cdeb, (char *) Pne->CdebTrav, NombreDeVariables * sizeof( int ) );
memcpy( (char *) Csui, (char *) Pne->CsuiTrav, Pne->TailleAlloueePourLaMatriceDesContraintes * sizeof( int ) );
memset( (char *) T, 0, NombreDeContraintes * sizeof( char ) );

memset( (char *) NbTermesUtilesDeVar, 0, NombreDeVariables * sizeof( int ) );
memset( (char *) V, 0, NombreDeVariables * sizeof( double ) );
memset( (char *) HashCode, 0, NombreDeVariables * sizeof( int ) );
memset( (char *) Flag, COLONNE_A_EVITER, NombreDeContraintes * sizeof( char ) );

for ( Cnt = 0 ; Cnt < NombreDeContraintes ; Cnt++ ) {
	if ( SensContrainte[Cnt] == '<' ) {
		if ( BmaxValide[Cnt] == OUI_PNE ) {
      if ( Bmax[Cnt] <= B[Cnt] ) {
				ContrainteActivable[Cnt] = NON_PNE;				
			}
		}   
	}
}

/* Hascode et variables a etudier */

for ( Var = 0 ; Var < NombreDeVariables ; Var++ ) {
  /* On ne prend  en compte ni les variables entieres ni les variables fixes */
  if ( TypeDeBorne[Var] == VARIABLE_FIXE ) continue; 
  if ( TypeDeVariable[Var] != ENTIER ) continue; 
	if ( Xmin[Var] == Xmax[Var] ) continue; 
	
  /* On n'etudie que les variables entieres non fixees */	
	Flag[Var] = COLONNE_A_ETUDIER;
	
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
		}
		else {
	    /* On en profite pour dechainer les lignes qui correspondent a des contraintes inactives */
      ic = Csui[ic];
			if ( icPrec >= 0 ) {
			  Csui[icPrec] = ic;
			  continue;
			}
			else Cdeb[Var] = ic;
		}
		
		icPrec = ic;
		ic = Csui[ic];
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

/* Pour chaque variable a valeur fractionnaire on recherche les colonnes colineaires parmi celles
   qui ne sont pas instanciees */
SuivFrac = Pne->SuivFrac;	 
Var = Pne->PremFrac;
while ( Var >= 0 ) {

  /* Recherche des colonnes colineaires a la colonne de Var */
  Flag[Var] = COLONNE_A_EVITER; /* Pour ne plus passer dessus */
	if ( NbTermesUtilesDeVar[Var] <= 0 ) goto NextVar;
	NbTermesUtiles = NbTermesUtilesDeVar[Var];				
  Cvar = L[Var];
	HashVar = HashCode[Var];
	InitV = NON_PNE;

	Cnt = NumContrainte[Cdeb[Var]]; 
  il = Mdeb[Cnt];
	ilMax = il + NbTerm[Cnt];		
  while ( il < ilMax ) {
    Var1 = Nuvar[il];
    if ( Flag[Var1] == COLONNE_A_EVITER ) goto NextIl;  
	  if ( fabs( L[Var1] - Cvar ) > ZERO_COLINEAIRE ) goto NextIl;
    if ( NbTermesUtilesDeVar[Var1] != NbTermesUtiles ) goto NextIl;						
		if ( HashCode[Var1] != HashVar ) goto NextIl;

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
    if ( T[Cnt1] == 0 ) goto NextIl; /* Pas de terme correspondant dans la ligne */		
    Nu = A[ic1] / V[Cnt1];
		if (  fabs( Nu - 1. ) > ZERO_COLINEAIRE ) goto NextIl;
    Nb--;
    ic1 = Csui[ic1];

    /* On poursuit l'analyse de la variable Var1 */		
    while ( ic1 >= 0 ) {
      Cnt1 = NumContrainte[ic1];
      if ( T[Cnt1] == 0 ) goto NextIl; /* Pas de terme correspondant dans la ligne */		
      if ( fabs( A[ic1] - (Nu * V[Cnt1] ) ) > ZERO_COLINEAIRE )  goto NextIl;
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
		
    NextIl:
		il++;
	}
	
  /* RAZ de V et T avant de passer a la variable suivante */
	if ( InitV == OUI_PNE ) {
	  ic = Cdeb[Var];
 	  while ( ic >= 0 ) {
		  V[NumContrainte[ic]] = 0.0;
		  T[NumContrainte[ic]] = 0;
		  ic = Csui[ic];
	  }
	}	

  NextVar:
  Var = SuivFrac[Var];
}

Pne->LesGroupesDeVariablesEquivalentesSontValides = OUI_PNE;		

FinColonnesColineaires:

free( ContrainteActivable );
free( Cdeb );
free( Csui );
free( Buffer );

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

printf("NombreDeGroupesDeVariablesEquivalentes %d\n",Pne->NombreDeGroupesDeVariablesEquivalentes);

return;
}

/*----------------------------------------------------------------------------*/

# endif
