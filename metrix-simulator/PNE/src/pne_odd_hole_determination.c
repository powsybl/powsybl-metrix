/***********************************************************************

   FONCTION: Determination odd holes cliques a partir du graphe de conflits.
	           On ne prend que les cycles sans corde de longueur superieure a 3.
                 
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

# define TRACES 0

# define NOMBRE_MAX_DE_NOEUDS_DANS_UN_CYCLE 20
# define NOMBRE_MAX_DE_NOEUDS_VOISINS 10
# define NOMBRE_MAX_DE_NOEUDS_PARCOURUS_DANS_LA_RECHERCHE_DES_CYCLES_DUN_NOEUD 1000

# define INCREMENT_ALLOC_ODD_CYCLES 10000 /*1000*/
# define INCREMENT_ALLOC_TAILLE_ODD_CYCLES (5*INCREMENT_ALLOC_ODD_CYCLES)
# define NOMBRE_MAX_DE_ODD_CYCLES 100000 

void PNE_AllocOddCycles( PROBLEME_PNE * );
void PNE_FreeOddCycles( PROBLEME_PNE * );
void PNE_ReallocNbOddCycles( PROBLEME_PNE * );
void PNE_ReallocTailleOddCycles( PROBLEME_PNE * );
void PNE_CycleVisiterLesVoisins( PROBLEME_PNE * , int * , int * , int * , char * , int , int , int , int * , int * , char * , int * );
void PNE_ArchiverUnOddCycle( PROBLEME_PNE * , int * , int * , char * );

/*----------------------------------------------------------------------------*/

void PNE_FreeOddCycles( PROBLEME_PNE * Pne )
{
if ( Pne->OddCycles != NULL ) {
  free( Pne->OddCycles->First ); Pne->OddCycles->First = NULL;
  free( Pne->OddCycles->NbElements ); Pne->OddCycles->NbElements = NULL;
  free( Pne->OddCycles->Noeud ); Pne->OddCycles->Noeud = NULL;
  free( Pne->OddCycles->SecondMembre ); Pne->OddCycles->SecondMembre = NULL;
  free( Pne->OddCycles->LeOddCycleEstDansLePool );
  free( Pne->OddCycles ); Pne->OddCycles = NULL;
}
return;
}

/*----------------------------------------------------------------------------*/

void PNE_AllocOddCycles( PROBLEME_PNE * Pne )
{
ODD_CYCLES * OddCycles; 
Pne->OddCycles = (ODD_CYCLES *) malloc( sizeof( ODD_CYCLES ) );
if ( Pne->OddCycles == NULL ) { PNE_FreeOddCycles( Pne ); return; }
OddCycles = Pne->OddCycles;

OddCycles->First = NULL;
OddCycles->NbElements = NULL;
OddCycles->Noeud = NULL;
OddCycles->SecondMembre = NULL;
OddCycles->LeOddCycleEstDansLePool = NULL;

OddCycles->NbOddCyclesAllouees = INCREMENT_ALLOC_ODD_CYCLES;
OddCycles->TailleOddCyclesAllouee = INCREMENT_ALLOC_TAILLE_ODD_CYCLES;  

OddCycles->First = (int *) malloc( OddCycles->NbOddCyclesAllouees * sizeof( int ) );
OddCycles->NbElements = (int *) malloc( OddCycles->NbOddCyclesAllouees * sizeof( int ) );
OddCycles->SecondMembre = (int *) malloc( OddCycles->NbOddCyclesAllouees * sizeof( int ) );
OddCycles->LeOddCycleEstDansLePool = (char *) malloc( OddCycles->NbOddCyclesAllouees * sizeof( char ) );
OddCycles->Noeud = (int *) malloc( OddCycles->TailleOddCyclesAllouee * sizeof( int ) );

if ( OddCycles->First == NULL || OddCycles->NbElements == NULL || OddCycles->SecondMembre == NULL ||
     OddCycles->LeOddCycleEstDansLePool == NULL || OddCycles->Noeud == NULL ) {
  PNE_FreeOddCycles( Pne );
	return;
}

OddCycles->NombreDeOddCycles = 0;
OddCycles->Full = NON_PNE;

return;
}

/*----------------------------------------------------------------------------*/
void PNE_ReallocNbOddCycles( PROBLEME_PNE * Pne )
{
int i; int * pt; char * ptc; ODD_CYCLES * OddCycles;
OddCycles = Pne->OddCycles;
i = OddCycles->NbOddCyclesAllouees + INCREMENT_ALLOC_ODD_CYCLES;
pt = (int *) realloc( OddCycles->First, i * sizeof( int ) );
if ( pt == NULL ) { OddCycles->Full = OUI_PNE; return; }
OddCycles->First = pt;
pt = (int *) realloc( OddCycles->NbElements, i * sizeof( int ) );
if ( pt == NULL ) { OddCycles->Full = OUI_PNE; return; }
OddCycles->NbElements = pt;
pt = (int *) realloc( OddCycles->SecondMembre, i * sizeof( int ) );
if ( pt == NULL ) { OddCycles->Full = OUI_PNE; return; }
OddCycles->SecondMembre = pt;
ptc = (char *) realloc( OddCycles->LeOddCycleEstDansLePool, i * sizeof( char ) );
if ( ptc == NULL ) { OddCycles->Full = OUI_PNE; return; }
OddCycles->LeOddCycleEstDansLePool = ptc;
OddCycles->NbOddCyclesAllouees = i;
return;
}
/*----------------------------------------------------------------------------*/
void PNE_ReallocTailleOddCycles( PROBLEME_PNE * Pne )
{
int i; int * pt; ODD_CYCLES * OddCycles;
OddCycles = Pne->OddCycles;
i = OddCycles->TailleOddCyclesAllouee + INCREMENT_ALLOC_TAILLE_ODD_CYCLES;
pt = (int *) realloc( OddCycles->Noeud, i * sizeof( int ) );
if ( pt == NULL ) { OddCycles->Full = OUI_PNE; return; }
OddCycles->Noeud = pt;
OddCycles->TailleOddCyclesAllouee = i;
return;
}

/*----------------------------------------------------------------------------*/

void PNE_ArchiverUnOddCycle( PROBLEME_PNE * Pne, int * NbNoeudsDansLeCycle, int * NumeroDesNoeudsDansLeCycle, char * NoeudDansLeCycle )
{
ODD_CYCLES * OddCycles; int Pivot; int Complement; int * Noeud; int S; int NbElem;
int c; int Index; int IndexDebut; int ic; int i; int N; char ComplementTrouve;
int SecondMembre; int Edge; int Nv; int EdgeNv; int NombreDeNoeudsAuDepart;
int NombreDeNoeuds; char Lifter;

/* Si c'est pair on n'archive pas */
NombreDeNoeudsAuDepart = *NbNoeudsDansLeCycle;
if ( NombreDeNoeudsAuDepart % 2 == 0 ) return;
if ( NombreDeNoeudsAuDepart <= 3 ) return;

Lifter = OUI_PNE;
NombreDeNoeuds = NombreDeNoeudsAuDepart;

SecondMembre = (int) floor( (double) NombreDeNoeudsAuDepart / 2. );

/************************/
if ( Lifter != OUI_PNE ) goto FinLifting;
/* On essaie de lifter : pour les tests on lifte avec le coeff 1 pour ne pas avoir a le stocker */

for ( i = 0 ; i < NombreDeNoeuds ; i++ ) {
	N = NumeroDesNoeudsDansLeCycle[i];
  /* On examine les noeuds voisins de N qui ne sont pas dans le cycle */
  Edge = Pne->ConflictGraph->First[N];
  while ( Edge >= 0 ) {
	  Nv = Pne->ConflictGraph->Adjacent[Edge];
	  if ( NoeudDansLeCycle[Nv] == NON_PNE ) {
		  /* On essaie d'ajouter le noeud */
			S = NombreDeNoeuds;
      EdgeNv = Pne->ConflictGraph->First[Nv];
      while ( EdgeNv >= 0 ) {
			  if ( NoeudDansLeCycle[Pne->ConflictGraph->Adjacent[EdgeNv]] == OUI_PNE ) S--;			 			
	      EdgeNv = Pne->ConflictGraph->Next[EdgeNv];
			}
			if ( S < SecondMembre ) {
        /* On peut lifter en ajoutant le noeud Nv */
				if ( Pne->OddCycles != NULL ) {
				  printf("On lifte le odd cycle %d avec la variable %d S = %d SecondMembre = %d Coefficient possible %d\n",
					        Pne->OddCycles->NombreDeOddCycles,Nv,S,SecondMembre,SecondMembre-S);
				}
        NoeudDansLeCycle[Nv] = OUI_PNE;
        NumeroDesNoeudsDansLeCycle[NombreDeNoeuds] = Nv;
        NombreDeNoeuds++;
			} 
		}		
	  Edge = Pne->ConflictGraph->Next[Edge];
  }  	
}
FinLifting:
/************************/

Pivot = Pne->ConflictGraph->Pivot;

if ( Pne->OddCycles == NULL ) {
  PNE_AllocOddCycles( Pne );
}
if ( Pne->OddCycles == NULL ) {
  /* On depile les noeuds qu'on a eventuellement ajoutes au lifting */
  for ( i = NombreDeNoeudsAuDepart ; i < NombreDeNoeuds ; i++ ) NoeudDansLeCycle[NumeroDesNoeudsDansLeCycle[i]] = NON_PNE;	
  return;
}

OddCycles = Pne->OddCycles;
Noeud = OddCycles->Noeud;

c = OddCycles->NombreDeOddCycles - 1;
if ( c >= 0 ) Index = OddCycles->First[c] + OddCycles->NbElements[c];
else Index = 0;
c++;

if ( c == OddCycles->NbOddCyclesAllouees ) {
  PNE_ReallocNbOddCycles( Pne );
	if ( OddCycles->Full == OUI_PNE ) {
    /* On depile les noeuds qu'on a eventuellement ajoutes au lifting */
    for ( i = NombreDeNoeudsAuDepart ; i < NombreDeNoeuds ; i++ ) NoeudDansLeCycle[NumeroDesNoeudsDansLeCycle[i]] = NON_PNE;	
	  return;
	}
}

OddCycles->First[c] = Index;
IndexDebut = Index;
NbElem = 0;
for ( i = 0 ; i < NombreDeNoeuds ; i++ ) {
  if ( Index == OddCycles->TailleOddCyclesAllouee ) {
    PNE_ReallocTailleOddCycles( Pne );
	  if ( OddCycles->Full == OUI_PNE ) {
      /* On depile les noeuds qu'on a eventuellement ajoutes au lifting */
      for ( i = NombreDeNoeudsAuDepart ; i < NombreDeNoeuds ; i++ ) NoeudDansLeCycle[NumeroDesNoeudsDansLeCycle[i]] = NON_PNE;			
		  return;
		}
    Noeud = OddCycles->Noeud;	
  }
	N = NumeroDesNoeudsDansLeCycle[i];
  /* On regarde s'il n'y a pas deja son complement */
	if ( N < Pivot ) Complement = N + Pivot;
	else Complement = Pivot - N;	

  ComplementTrouve = NON_PNE;
	for ( ic = IndexDebut; ic < Index ; ic++ ) {
    if ( Noeud[ic] == Complement ) {
      SecondMembre--;
			/* Et on enleve le noeud de l'index ic */
			Noeud[ic] = Noeud[Index-1];
			NbElem--;
			Index--;
      ComplementTrouve = OUI_PNE;
			break;
		}
	}
	
	if ( ComplementTrouve == NON_PNE ) {
    Noeud[Index] = N;
		NbElem++;
		Index++;
	}
}

if ( NbElem > 0 ) {
  OddCycles->NbElements[c] = NbElem;
	OddCycles->SecondMembre[c] = SecondMembre;	
  OddCycles->LeOddCycleEstDansLePool[c] = NON_PNE;

  /* Il faut maintenant lifter le odd hole sur le graphe */
	/* On prend le noeud du graphe qui a le plus grand nombre de voisins dans le odd hole et on fixe les valeurs a 0.
	   On calcule la somme des variables restantes du odd hole.
		 Si la contrainte n'est pas violee on ajoute le noeud dans le odd hole en lui calculant un coefficient. */
	
  OddCycles->NombreDeOddCycles++;
  if ( Pne->OddCycles->NombreDeOddCycles > NOMBRE_MAX_DE_ODD_CYCLES ) {
    OddCycles->Full = OUI_PNE;
  }
}

/* On depile les noeuds qu'on a eventuellement ajoutes au lifting */
for ( i = NombreDeNoeudsAuDepart ; i < NombreDeNoeuds ; i++ ) NoeudDansLeCycle[NumeroDesNoeudsDansLeCycle[i]] = NON_PNE;

/*
printf("Archivage du cycle sans corde %d\n",c);
for ( ic = OddCycles->First[c] ; ic < OddCycles->First[c] + OddCycles->NbElements[c] ; ic++ ) {
  printf(" %d ",Noeud[ic] );
}
printf(" < %d \n",OddCycles->SecondMembre[c]);
*/

return;
}


/*----------------------------------------------------------------------------*/
/* Prevoir une longueur max de cycle pour que ce ne soit pas trop long */

void PNE_CycleVisiterLesVoisins( PROBLEME_PNE * Pne, int * First, int * Adjacent, int * Next, char * Ouvert,
                                 int Noeud, int NoeudPere, int NoeudDeDepart,
																 int * NbNoeudsDansLeCycle, int * NumeroDesNoeudsDansLeCycle, char * NoeudDansLeCycle,
																 int * NbVisitesDeNoeuds )
{
int Edge; int NoeudVoisin; char CycleTrouve; int NbNv; int Nv;
# if TRACES == 1
  int i;
# endif

# if TRACES == 1
  printf(" Noeud = %d NoeudPere = %d NbNoeudsDansLeCycle %d \n",Noeud,NoeudPere,*NbNoeudsDansLeCycle);
# endif

CycleTrouve = NON_PNE;

Edge = First[Noeud];
while ( Edge >= 0 ) {
  /* Il ne doit pas y avoir de corde */
	NoeudVoisin = Adjacent[Edge];	
	if ( NoeudVoisin == NoeudPere ) goto NextEdge0;

  if ( Ouvert[Edge] == NON_PNE ) {
	  if ( NoeudVoisin == NoeudDeDepart ) {
	    /* On a trouve un cycle */
		  CycleTrouve = OUI_PNE;			
		  goto NextEdge0;
		}
	}
	if ( NoeudDansLeCycle[NoeudVoisin] == OUI_PNE ) {
    # if TRACES == 1
      printf(" le noeud %d a de ses voisins se trouve dans le cycle et forme une corde \n",Noeud);
  	  printf(" 1- On enleve le noeud %d \n",Noeud);		
    # endif
    NoeudDansLeCycle[Noeud] = NON_PNE;
    *NbNoeudsDansLeCycle = *NbNoeudsDansLeCycle - 1;
		return;
	}
	NextEdge0:
	Edge = Next[Edge];
}

if ( CycleTrouve == OUI_PNE ) {

  PNE_ArchiverUnOddCycle( Pne, NbNoeudsDansLeCycle, NumeroDesNoeudsDansLeCycle, NoeudDansLeCycle );
		
	/* On enleve la 1ere arete du cycle qui permet de rejoindre le noeud de depart */
  Edge = First[NumeroDesNoeudsDansLeCycle[1]];
  while ( Edge >= 0 ) {
    if ( Adjacent[Edge] == NoeudDeDepart ) {
		  /*printf("Ouverture de l'arete %d qui va de %d a %d\n",Edge,NumeroDesNoeudsDansLeCycle[1],NoeudDeDepart);*/
      Ouvert[Edge] = OUI_PNE;
      break;
		}	 
	  Edge = Next[Edge];
  }
	
  # if TRACES == 1
    printf(" Cycle trouve. On enleve le noeud %d \n",Noeud);
	# endif	
  NoeudDansLeCycle[Noeud] = NON_PNE;
  *NbNoeudsDansLeCycle = *NbNoeudsDansLeCycle - 1;	
	return;
	
}


/* Aucune corde: on ajoute un noeud voisin de Noeud dans le cycle */
NbNv = 0;
Edge = First[Noeud];
while ( Edge >= 0 ) {

  if ( *NbVisitesDeNoeuds > NOMBRE_MAX_DE_NOEUDS_PARCOURUS_DANS_LA_RECHERCHE_DES_CYCLES_DUN_NOEUD ) {
    break;
	}

  if ( NbNv > NOMBRE_MAX_DE_NOEUDS_VOISINS ) {
	  /*printf(" NbNv = %d > NOMBRE_MAX_DE_NOEUDS_VOISINS \n",NbNv);*/
	  break;
	}
  if ( Ouvert[Edge] == OUI_PNE ) goto NextEdge1;
	NoeudVoisin = Adjacent[Edge];
	if ( NoeudDansLeCycle[NoeudVoisin] == OUI_PNE ) {
	  /*printf(" goto NextEdge1 car %d est dans le cycle \n",NoeudVoisin);*/
	  goto NextEdge1;
	}

	if ( *NbNoeudsDansLeCycle >= NOMBRE_MAX_DE_NOEUDS_DANS_UN_CYCLE ) {
	  break;
	}
  NbNv++;
	
  /* On ajoute le noeud dans le cycle */

  # if TRACES == 1
    printf(" on ajoute le noeud %d dans le cycle (noeud pere %d)\n",NoeudVoisin,Noeud);
  # endif
	
  NoeudDansLeCycle[NoeudVoisin] = OUI_PNE;
  NumeroDesNoeudsDansLeCycle[*NbNoeudsDansLeCycle] = NoeudVoisin;
  *NbNoeudsDansLeCycle = *NbNoeudsDansLeCycle + 1;
	
  # if TRACES == 1
	printf("Cycle potentiel:\n");
	for ( i = 0 ; i < *NbNoeudsDansLeCycle ; i++ ) printf(" %d ",NumeroDesNoeudsDansLeCycle[i]);
	printf("\n");
  # endif
	
  *NbVisitesDeNoeuds = *NbVisitesDeNoeuds + 1;
	
  PNE_CycleVisiterLesVoisins( Pne, First, Adjacent, Next, Ouvert, NoeudVoisin, Noeud, NoeudDeDepart,
	                            NbNoeudsDansLeCycle, NumeroDesNoeudsDansLeCycle, NoeudDansLeCycle,
															NbVisitesDeNoeuds );
	
	NextEdge1:
	Edge = Next[Edge];
}
	
/* Mais il faut depiler avant pour le cas ou on serait sorti sur un break */
# if TRACES == 1
  printf(" Depilage jusqu'au noeud %d inclus\n",Noeud);
# endif
while( 1 ) {
  Nv = NumeroDesNoeudsDansLeCycle[*NbNoeudsDansLeCycle - 1];

  # if TRACES == 1
	  printf(" 2 On enleve le noeud %d \n",Nv);
	# endif
				
  NoeudDansLeCycle[Nv] = NON_PNE;
  *NbNoeudsDansLeCycle = *NbNoeudsDansLeCycle - 1;
		
  if ( Nv == Noeud ) break;
}
																													
return;
}


/*----------------------------------------------------------------------------*/

void PNE_RechercherTousLesCyclesSansCorde( PROBLEME_PNE * Pne )
{
char * Ouvert; int * Adjacent; int * Next; int * First; int Noeud; CONFLICT_GRAPH * ConflictGraph;
int NbNoeudsDuGraphe; int Edge; int * NumeroDesNoeudsDansLeCycle; char * NoeudDansLeCycle;
int * NumeroDesNoeudsVisites; char * NoeudVisite; int NoeudPere; int NoeudDeDepart;
int NbNoeudsDansLeCycle; int NbNoeudsVisites; int NbVisitesDeNoeuds; int Nv; int EdgeNv;
int il;

if ( Pne->Controls != NULL ) {
  if ( Pne->Controls->RechercherLesCliques == NON_PNE ) {
	  return;
	}
}

ConflictGraph = Pne->ConflictGraph;
if ( ConflictGraph == NULL ) return;

NbNoeudsDuGraphe = ConflictGraph->NbNoeudsDuGraphe;
Adjacent = ConflictGraph->Adjacent;
Next = ConflictGraph->Next;
First = ConflictGraph->First;

Ouvert = NULL;
NumeroDesNoeudsDansLeCycle = NULL;
NoeudDansLeCycle = NULL;
NumeroDesNoeudsVisites = NULL;
NoeudVisite = NULL;

goto AAA;

il = 0;
First[0] = il;
Adjacent[il] = 1;
Next[il] = il+1;
il++;
Adjacent[il] = 5;
Next[il] = il+1;
il++;
Adjacent[il] = 3;
Next[il] = il+1;
il++;
Adjacent[il] = 2;
Next[il] = il+1;
il++;
Adjacent[il] = 6;
Next[il] = il+1;
il++;
Adjacent[il] = 4;
Next[il] = -1;
il++;

First[1] = il;
Adjacent[il] = 0;
Next[il] = il+1;
il++;
Adjacent[il] = 3;
Next[il] = il+1;
il++;
Adjacent[il] = 2;
Next[il] = -1;
il++;

First[2] = il;
Adjacent[il] = 1;
Next[il] = il+1;
il++;
Adjacent[il] = 0;
Next[il] = il+1;
il++;
Adjacent[il] = 3;
Next[il] = -1;
il++;

First[3] = il;
Adjacent[il] = 2;
Next[il] = il+1;
il++;
Adjacent[il] = 0;
Next[il] = il+1;
il++;
Adjacent[il] = 1;
Next[il] = il+1;
il++;
Adjacent[il] = 5;
Next[il] = il+1;
il++;
Adjacent[il] = 4;
Next[il] = -1;
il++;

First[4] = il;
Adjacent[il] = 3;
Next[il] = il+1;
il++;
Adjacent[il] = 0;
Next[il] = -1;
il++;

First[5] = il;
Adjacent[il] = 0;
Next[il] = il+1;
il++;
Adjacent[il] = 9;
Next[il] = il+1;
il++;
Adjacent[il] = 3;
Next[il] = -1;
il++;

First[6] = il;
Adjacent[il] = 0;
Next[il] = il+1;
il++;
Adjacent[il] = 7;
Next[il] = -1;
il++;

First[7] = il;
Adjacent[il] = 6;
Next[il] = il+1;
il++;
Adjacent[il] = 8;
Next[il] = -1;
il++;

First[8] = il;
Adjacent[il] = 7;
Next[il] = il+1;
il++;
Adjacent[il] = 9;
Next[il] = -1;
il++;

First[9] = il;
Adjacent[il] = 8;
Next[il] = il+1;
il++;
Adjacent[il] = 5;
Next[il] = -1;
il++;

ConflictGraph->NbNoeudsDuGraphe = 10;
NbNoeudsDuGraphe = ConflictGraph->NbNoeudsDuGraphe;
ConflictGraph->NbEdges = il;
ConflictGraph->Pivot = ConflictGraph->NbNoeudsDuGraphe;

AAA:

Ouvert = (char *) malloc( ConflictGraph->NbEdges * sizeof( char ) );
if ( Ouvert == NULL) goto FinCycles;

NumeroDesNoeudsDansLeCycle = (int *) malloc( NOMBRE_MAX_DE_NOEUDS_DANS_UN_CYCLE * sizeof( int ) );
if ( NumeroDesNoeudsDansLeCycle == NULL) goto FinCycles;

NoeudDansLeCycle = (char *) malloc( NbNoeudsDuGraphe * sizeof( char ) );
if ( NoeudDansLeCycle == NULL) goto FinCycles;

NumeroDesNoeudsVisites = (int *) malloc( NbNoeudsDuGraphe * sizeof( int ) );
if ( NumeroDesNoeudsVisites == NULL) goto FinCycles;

NoeudVisite = (char *) malloc( NbNoeudsDuGraphe * sizeof( char ) );
if ( NoeudVisite == NULL) goto FinCycles;

for ( Edge = 0 ; Edge < ConflictGraph->NbEdges ; Edge++ ) Ouvert[Edge] = NON_PNE;

memset( (char *) NoeudDansLeCycle, NON_PNE, NbNoeudsDuGraphe * sizeof( char ) );
memset( (char *) NoeudVisite, NON_PNE, NbNoeudsDuGraphe * sizeof( char ) );

NbVisitesDeNoeuds = 0;

for ( Noeud = 0 ; Noeud < NbNoeudsDuGraphe ; Noeud++ ) {
  NoeudPere = -1;	
  NoeudDeDepart = Noeud;

	# if TRACES == 1
    printf("Recherche des cycles partant du noeud %d\n",Noeud);
	# endif
			
	NumeroDesNoeudsDansLeCycle[0] = Noeud;
	NbNoeudsDansLeCycle = 1;
	NoeudDansLeCycle[Noeud] = OUI_PNE;

  NbNoeudsVisites = 0;
	/*
	NumeroDesNoeudsVisites[0] = Noeud;
  NbNoeudsVisites = 1;
  NoeudVisite[Noeud] = OUI_PNE;
	*/
	
	NbVisitesDeNoeuds = 0;
	
  PNE_CycleVisiterLesVoisins( Pne, First, Adjacent, Next, Ouvert,Noeud, NoeudPere, NoeudDeDepart,
	                            &NbNoeudsDansLeCycle, NumeroDesNoeudsDansLeCycle, NoeudDansLeCycle,
                              &NbVisitesDeNoeuds );															
															
  /* Ouvrir toutes les aretes qui partent de Noeud */
  Edge = First[Noeud]; 
  while ( Edge >= 0 ) {
	  Ouvert[Edge] = OUI_PNE;
		Nv = Adjacent[Edge];
    EdgeNv = First[Nv]; 
    while ( EdgeNv >= 0 ) {
      if ( Adjacent[EdgeNv] == Noeud ) {
	      Ouvert[EdgeNv] = OUI_PNE;
        break;
			}
	    EdgeNv = Next[EdgeNv];
		}		
	  Edge = Next[Edge];
	}
 
}

FinCycles:

if ( Pne->OddCycles != NULL ) {
  if ( Pne->OddCycles->NombreDeOddCycles > 0 ) {
    printf("Found %d odd cycles\n",Pne->OddCycles->NombreDeOddCycles);
	}
}

free( Ouvert );
free( NumeroDesNoeudsDansLeCycle );
free( NoeudDansLeCycle );
free( NumeroDesNoeudsVisites );
free( NoeudVisite );

return;
}
