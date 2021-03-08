/***********************************************************************

   FONCTION: Calcul des coupes 
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pne_sys.h"

# include "pne_fonctions.h"
# include "pne_define.h"

# include "spx_define.h"
# include "spx_fonctions.h"

# include "bb_define.h"
# include "bb_fonctions.h"

# ifdef PNE_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pne_memoire.h"
# endif

# define PROPORTION_DE_COUPES_INSEREES 1.0  /* Coefficient qui s'applique au nombre de coupes calculees pour determiner
                                               le nombre max. de coupes conservees apres un tri en fonction de la
						                                   distance du point courant */ 

# define NOMBRE_MIN_DE_COUPES_INSEREES  25    /* Si la proportion du nombre de coupes inserees conduit a un nombre trop
                                                 faible de coupes, on le remplace par ce nombre */

# define NOMBRE_MAX_DE_COUPES_INSEREES_FIXE_A_LAVANCE NON_PNE /* Si OUI_PNE on prend  NOMBRE_MAX_DE_COUPES_INSEREES
                                                                 si NON_PNE on prend PROPORTION_MAX_DE_COUPES_INSEREES * nb de contraintes */
# define NOMBRE_MAX_DE_COUPES_INSEREES  200
# define PROPORTION_MAX_DE_COUPES_INSEREES 0.2 

# define SEUIL_INHIB_GOMORY 1.e-5 /*1.e-4*/

int PNE_PartitionCoupeTriRapide( COUPE_CALCULEE ** , int , int );
void PNE_CoupeTriRapide( COUPE_CALCULEE ** , int , int );

/*----------------------------------------------------------------------------*/

int PNE_PartitionCoupeTriRapide( COUPE_CALCULEE ** CoupesCalculees, int Deb, int Fin )
{
int Compt; double Pivot; int i; COUPE_CALCULEE * Coupe;
Compt = Deb;
Pivot = CoupesCalculees[Deb]->Distance;
/* Ordre decroissant */
for ( i = Deb + 1 ; i <= Fin ; i++) {		
  if ( CoupesCalculees[i]->Distance > Pivot) {
    Compt++;
    Coupe = CoupesCalculees[Compt];
    CoupesCalculees[Compt] = CoupesCalculees[i];
    CoupesCalculees[i] = Coupe; 			
  }
}	
Coupe = CoupesCalculees[Compt];
CoupesCalculees[Compt] = CoupesCalculees[Deb];
CoupesCalculees[Deb] = Coupe;
return(Compt);
}

/*----------------------------------------------------------------------------*/

void PNE_CoupeTriRapide( COUPE_CALCULEE ** CoupesCalculees, int Debut, int Fin )
{
int Pivot;
if ( Debut < Fin ) {
  Pivot = PNE_PartitionCoupeTriRapide( CoupesCalculees, Debut, Fin );  
  PNE_CoupeTriRapide( CoupesCalculees, Debut  , Pivot-1 );
  PNE_CoupeTriRapide( CoupesCalculees, Pivot+1, Fin );
}
return;
}

/*----------------------------------------------------------------------------*/

void PNE_CalculerLesCoupes( PROBLEME_PNE * Pne )
{
int i; COUPE_CALCULEE * Coupe; int NbMxCoupes; int NbI; char OnInverse  ; int iMax;
int NbCoupesInserees; char ContrainteSaturee; int NbTermesMx ; int DernierIndex;
int FinDeBoucle; int NombreDeCoupesDejaCalculees; BB * Bb; int Plim; char InhiberGomory;
double SeuilAdm; double S; int Cnt; int il; int ilMax; int * MdebTrav; int * NbTermTrav;
int * NuvarTrav; double * ATrav; double * UTrav; char * SensContrainteTrav; double * BTrav;
int NombreDeVariables; int NombreDeContraintes;

PNE_MiseAJourDesSeuilDeSelectionDesCoupes( Pne );

Bb = Pne->ProblemeBbDuSolveur;
NombreDeVariables = Pne->NombreDeVariablesTrav;
NombreDeContraintes = Pne->NombreDeContraintesTrav;

/* Au premier passage on alloue les vecteurs pour les coupes */
if ( Pne->Coefficient_CG == NULL ) {
  Pne->Coefficient_CG         = (double *) malloc( NombreDeVariables * sizeof( double ) );
  Pne->IndiceDeLaVariable_CG  = (int *)    malloc( NombreDeVariables * sizeof( int   ) );
  if ( Pne->Coefficient_CG == NULL || Pne->IndiceDeLaVariable_CG == NULL ) {
    printf(" Solveur PNE , memoire insuffisante. Sous-programme: PNE_CalculerLesCoupes \n");
    Pne->AnomalieDetectee = OUI_PNE;
    longjmp( Pne->Env , Pne->AnomalieDetectee ); /* rq: le 2eme argument ne sera pas utilise */
  }
  memset( (double *) Pne->Coefficient_CG, 0, NombreDeVariables * sizeof( double ) );
  memset( (char *) Pne->IndiceDeLaVariable_CG, 0, NombreDeVariables * sizeof( int ) );		
}

if ( Pne->ValeurLocale == NULL ) {
  Pne->ValeurLocale = (double *) malloc( NombreDeVariables * sizeof( double ) );
  Pne->IndiceLocal  = (int *)    malloc( NombreDeVariables * sizeof( int   ) );
  if ( Pne->ValeurLocale == NULL || Pne->IndiceLocal == NULL ) {
    printf(" Solveur PNE , memoire insuffisante. Sous-programme: PNE_CalculerLesCoupes \n");
    Pne->AnomalieDetectee = OUI_PNE;
    longjmp( Pne->Env , Pne->AnomalieDetectee ); /* rq: le 2eme argument ne sera pas utilise */
  }
  memset( (double *) Pne->ValeurLocale, 0, NombreDeVariables * sizeof( double ) );
  memset( (char *) Pne->IndiceLocal, 0, NombreDeVariables * sizeof( int ) );	
}

if ( Pne->FlottantBanaliseEnNombreDeVariables_1 == NULL ) {
  Pne->FlottantBanaliseEnNombreDeVariables_1 = (double *) malloc( NombreDeVariables * sizeof( double ) );
  Pne->FlottantBanaliseEnNombreDeVariables_2 = (double *) malloc( NombreDeVariables * sizeof( double ) );
  Pne->FlottantBanaliseEnNombreDeVariables_3 = (double *) malloc( NombreDeVariables * sizeof( double ) );
	
  Pne->EntierBanaliseEnNombreDeVariables_1 = (int *) malloc( NombreDeVariables * sizeof( int ) );
  Pne->EntierBanaliseEnNombreDeVariables_2 = (int *) malloc( NombreDeVariables * sizeof( int ) );
  Pne->EntierBanaliseEnNombreDeVariables_3 = (int *) malloc( NombreDeVariables * sizeof( int ) );

  Pne->CharBanaliseEnNombreDeVariables_1 = (char *) malloc( NombreDeVariables * sizeof( char ) );
  Pne->CharBanaliseEnNombreDeVariables_2 = (char *) malloc( NombreDeVariables * sizeof( char ) );
  Pne->CharBanaliseEnNombreDeVariables_3 = (char *) malloc( NombreDeVariables * sizeof( char ) );

  Pne->EntierBanaliseEnNombreDeContraintes_1 = (int *) malloc( NombreDeContraintes * sizeof( int ) );
  Pne->CharBanaliseEnNombreDeContraintes_1 = (char *) malloc( NombreDeContraintes * sizeof( char ) );
  if ( Pne->FlottantBanaliseEnNombreDeVariables_1 == NULL || Pne->FlottantBanaliseEnNombreDeVariables_2 == NULL || Pne->FlottantBanaliseEnNombreDeVariables_3 == NULL ||
	     Pne->EntierBanaliseEnNombreDeVariables_1 == NULL || Pne->EntierBanaliseEnNombreDeVariables_2 == NULL || Pne->EntierBanaliseEnNombreDeVariables_3 == NULL ||
			 Pne->CharBanaliseEnNombreDeVariables_1 == NULL || Pne->CharBanaliseEnNombreDeVariables_2  == NULL || Pne->CharBanaliseEnNombreDeVariables_3 == NULL ||
			 Pne->EntierBanaliseEnNombreDeContraintes_1 == NULL || Pne->CharBanaliseEnNombreDeContraintes_1 == NULL ) {
    printf(" Solveur PNE , memoire insuffisante. Sous-programme: PNE_CalculerLesCoupes \n");
    Pne->AnomalieDetectee = OUI_PNE;
    longjmp( Pne->Env , Pne->AnomalieDetectee ); /* rq: le 2eme argument ne sera pas utilise */
  }
}

Pne->NombreDeCoupesCalculeesNonEvaluees = 0;

Pne->NombreDeCoupesAjoute = 0; 
 
NombreDeCoupesDejaCalculees = Pne->NombreDeCoupesCalculees;

# if UTILISER_LE_GRAPHE_DE_CONFLITS == OUI_PNE
  PNE_DetectionDesCliquesViolees( Pne ); 
  # if UTILISER_LES_ODD_HOLES == OUI_PNE
    PNE_DetectionDesOddCyclesVioles( Pne );
  # endif	
  # if UTILISER_LES_COUPES_DE_PROBING == OUI_PNE 
    PNE_DetectionDesCoupesDeProbingViolees( Pne );
	# endif
  # if CONSTRUIRE_BORNES_VARIABLES == OUI_PNE
    PNE_DetectionDesContraintesDeBorneVariableViolees( Pne );		
	# endif
  # if RELATION_DORDRE_DANS_LE_PROBING == OUI_PNE
    PNE_DetectionDesContraintesDOrdreViolees( Pne );		
	# endif	
# endif

# if UTILISER_LES_COUPES_DE_COUTS_REDUITS == OUI_PNE
  PNE_DetectionDesCoupesDeCoutsReduitsViolees( Pne );
# endif

PNE_DetectionKnapsackNegligeesViolees( Pne );
PNE_DetectionGomoryNegligeesViolees( Pne );

/* Test: pas concluant pour l'instant*/
/*
PNE_TwoStepMirSurContrainte( Pne ); 
*/
/* Fin Test */

/* Calcul des coupes de Gomory */
/* Lorsque le Gap est tres petit, la partie Branch and Bound redemande les calculs de coupe. Plim a
   pour objectif d'eviter le calcul des Gomory si la profondeur est trop grande. Par contre on
	 calculera quand-meme les autres types de coupes */
 
Plim = 1000 /*30*/; /* Il ne faut pas trop s'opposer au calcul cyclique des coupes */
 
if ( Bb->CalculerDesCoupesDeGomory == OUI_PNE && Bb->NoeudEnExamen->ProfondeurDuNoeud <= Plim ) {

  InhiberGomory = NON_PNE;
  /* On verifie au moins l'admissibilite primale sur les contraintes (hors coupes) avant de lancer
     un calcul de coupes de Gomory */
  MdebTrav = Pne->MdebTrav;
  NbTermTrav = Pne->NbTermTrav;
  ATrav = Pne->ATrav;
  UTrav = Pne->UTrav;
  NuvarTrav = Pne->NuvarTrav;
  SensContrainteTrav = Pne->SensContrainteTrav;
  BTrav = Pne->BTrav;
  SeuilAdm = SEUIL_INHIB_GOMORY;
  for ( Cnt = 0 ; Cnt < Pne->NombreDeContraintesTrav ; Cnt++ ) {
    il = MdebTrav[Cnt];
    ilMax = il + NbTermTrav[Cnt];
    S = 0.;  
    while ( il < ilMax ) {
		  S += ATrav[il] * UTrav[NuvarTrav[il]];
			il++;
		}
    if ( SensContrainteTrav[Cnt] == '=' ) {
		  if ( fabs( S - BTrav[Cnt] ) > SeuilAdm ) {
			  InhiberGomory = OUI_PNE;
			  break;
		  }
	  }
	  else if ( SensContrainteTrav[Cnt] == '<' ) {
	    if ( S > BTrav[Cnt] + SeuilAdm ) {
			  InhiberGomory = OUI_PNE;
			  break;
		  }
	  }
	  else if ( SensContrainteTrav[Cnt] == '>' ) {
		  if ( S < BTrav[Cnt] - SeuilAdm ) { 
			  InhiberGomory = OUI_PNE;
			  break;
		  }		
    }		
  }
	# if CALCULER_COUPES_DE_GOMORY == OUI_PNE 	
    if ( InhiberGomory == NON_PNE ) {  
      PNE_Gomory( Pne );
      /* Attention, pour les desactiver il faut aussi intervenir dans le simplexe */
      PNE_CalculerLesCoupesDIntersection( Pne );
	  }
	# endif
}
 
/* Knapsack */        
/* Pas concluant */
/*
if ( Pne->PremFrac >= 0 && 0 ) {
  PNE_KnapsackSurCombinaisonsComplexesDeContraintes( Pne ); 
}
*/

# if CALCULER_COUPES_KNAPSACK_SIMPLE == OUI_PNE
  if ( Pne->PremFrac >= 0 ) {
    /*PNE_CoverKnapsackSimple( Pne );*/
		/* En test: on remplace par les agregations de contraintes */
		
    PNE_KnapsackAvecCombinaisonsDeContraintes( Pne );
		
  }
# endif
  
/* Marchand Wolsey */
# if CALCULER_MIR_MARCHAND_WOLSEY == OUI_PNE
  if ( Pne->PremFrac >= 0 ) {
    PNE_MIRMarchandWolsey( Pne );
  }
# endif

# if FAIRE_DES_COUPES_AVEC_LES_BORNES_INF_MODIFIEES == OUI_PNE
  PNE_DetectionDesContraintesDeBorneInfViolees( Pne );
# endif

#if VERBOSE_PNE
  printf("-> Pne->NombreDeCoupesCalculees %d\n",Pne->NombreDeCoupesCalculees); 
#endif

/* Maintenant, on nettoie la structure temporaire de stockage des coupes en fonction de l'ampleur
   des violations de contraintes de la solution courante */	 
goto AB;
OnInverse = OUI_PNE;
iMax      = Pne->NombreDeCoupesCalculees - 1;
while ( OnInverse == OUI_PNE ) {
   OnInverse = NON_PNE;
   for ( i = NombreDeCoupesDejaCalculees ; i < iMax ; i++ ) {    
     if ( Pne->CoupesCalculees[i]->Distance < Pne->CoupesCalculees[i+1]->Distance ) {
       OnInverse = OUI_PNE;
       /* On Inverse les pointeurs */
       Coupe = Pne->CoupesCalculees[i + 1];
       Pne->CoupesCalculees[i+1] = Pne->CoupesCalculees[i];
       Pne->CoupesCalculees[i]   = Coupe; 
     }
   }
}
goto AC;
AB:
i = NombreDeCoupesDejaCalculees;
iMax = Pne->NombreDeCoupesCalculees - 1;
if ( Pne->CoupesCalculees != NULL ) PNE_CoupeTriRapide( Pne->CoupesCalculees, i, iMax );
AC:
/*   */  

NbMxCoupes = (int) (PROPORTION_DE_COUPES_INSEREES * ( Pne->NombreDeCoupesCalculees - NombreDeCoupesDejaCalculees ) );
if ( NbMxCoupes < NOMBRE_MIN_DE_COUPES_INSEREES ) NbMxCoupes = NOMBRE_MIN_DE_COUPES_INSEREES;

/* Init du nombre max de termes pour conserver un creux "honorable" */
NbTermesMx = Pne->NombreDeVariablesTrav + Pne->NombreDeContraintesDInegalite - Pne->NombreDeContraintesTrav;
NbTermesMx = (int) ceil( 0.5 * NbTermesMx );

if ( NbTermesMx < 100 ) NbTermesMx = 100;

/* On verifie qu'on peut mettre au moins qq coupes */
NbCoupesInserees = 0;
NbI = 0;
for ( i = NombreDeCoupesDejaCalculees ; NbCoupesInserees < NbMxCoupes && i < Pne->NombreDeCoupesCalculees ; i++ ) {
  if ( Pne->CoupesCalculees[i]->NombreDeTermes > NbTermesMx && NbI >= NOMBRE_MIN_DE_COUPES_INSEREES ) continue;
  NbI++;
  NbCoupesInserees++;
}

/* Si on ne peut pas en inserer assez, on force au nombre min */
if ( NbCoupesInserees < 1 ) {
  NbMxCoupes = 1;
  NbTermesMx = Pne->NombreDeVariablesTrav;
}

NbCoupesInserees = 0;

DernierIndex = Pne->NombreDeCoupesCalculees - 1;
FinDeBoucle  = Pne->NombreDeCoupesCalculees; 

/* Limitation du nombre de coupes ajoutees */
# if NOMBRE_MAX_DE_COUPES_INSEREES_FIXE_A_LAVANCE == OUI_PNE 
  if ( NbMxCoupes > NOMBRE_MAX_DE_COUPES_INSEREES ) NbMxCoupes = NOMBRE_MAX_DE_COUPES_INSEREES;
# else
  if ( NbMxCoupes > PROPORTION_MAX_DE_COUPES_INSEREES * Pne->NombreDeContraintesTrav ) {	
	  NbMxCoupes = (int) ceil ( PROPORTION_MAX_DE_COUPES_INSEREES * Pne->NombreDeContraintesTrav );
		if ( NbMxCoupes < NOMBRE_MIN_DE_COUPES_INSEREES ) NbMxCoupes = NOMBRE_MIN_DE_COUPES_INSEREES;
	}
# endif

/*printf("Debut Insertion\n");*/

NbI = 0;
for ( i = NombreDeCoupesDejaCalculees ; NbCoupesInserees < NbMxCoupes && i < FinDeBoucle ; i++ ) {	
  if ( Pne->CoupesCalculees[i]->NombreDeTermes < NbTermesMx || NbI < NOMBRE_MIN_DE_COUPES_INSEREES ) {

    /*printf("Distance %e\n",Pne->CoupesCalculees[i]->Distance);*/
	
    /* On ajoute la contrainte dans le probleme relaxe courant en vue d'une nouvelle resolution */
    Pne->CoupesCalculees[i]->NumeroDeLaContrainte = Pne->Coupes.NombreDeContraintes;
    ContrainteSaturee = NON_PNE; /* C'est une nouvelle contrainte, ainsi on mettra EN_BASE la variable d'ecart */		
    PNE_InsererUneContrainte( Pne,
                              Pne->CoupesCalculees[i]->NombreDeTermes ,
                              Pne->CoupesCalculees[i]->Coefficient,
			                        Pne->CoupesCalculees[i]->IndiceDeLaVariable,
			                        Pne->CoupesCalculees[i]->SecondMembre,
			                        ContrainteSaturee,
                              Pne->CoupesCalculees[i]->Type
															);

    NbI++;
															
    /* Informer la partie Bb qu'il faut relancer et incrementer un compteur de la partie branch and bound */
    BB_DemanderUneNouvelleResolutionDuProblemeRelaxe( Bb );
    NbCoupesInserees++;
  }
  else {
    /* On inverse avec la derniere */
    Coupe = Pne->CoupesCalculees[DernierIndex];
    Pne->CoupesCalculees[DernierIndex] = Pne->CoupesCalculees[i];
    Pne->CoupesCalculees[i] = Coupe;
    DernierIndex--;
    FinDeBoucle--; 
    i--; 
  }
}

/*printf("Nombre de coupes inserees parmi les coupes nouvellement calculees %d  (on en a calcule %d)\n",NbCoupesInserees,FinDeBoucle-NombreDeCoupesDejaCalculees);*/

/* On libere le reste des structures */
for ( ; i < Pne->NombreDeCoupesCalculees ; i++ ) {
  free( Pne->CoupesCalculees[i]->Coefficient ); 
  free( Pne->CoupesCalculees[i]->IndiceDeLaVariable );
  free( Pne->CoupesCalculees[i] );	
}

Pne->NombreDeCoupesCalculees = NombreDeCoupesDejaCalculees + NbCoupesInserees;

Pne->NombreDeCoupesCalculeesNonEvaluees = NbCoupesInserees;
Pne->NombreDeCoupesAjouteesAuRoundPrecedent = NbCoupesInserees;

return;
}



