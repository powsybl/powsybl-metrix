/***********************************************************************

   FONCTION: On positionne correctement les variables hors base compte 
             tenu de la crash base que l'on a determinee ou bien des 
             valeurs de couts reduits a la fin d'une iteration de phase 1.
             Remarque: il est important, pour les variables non bornées
             d'un seul ou des 2 cotés, de faire le test par rapport à  
             SeuilDAmissibiliteDuale2 sachant que la restauration de 
             l'admissibilité est faite par rapport à un seuil plus petit 
             SeuilDAmissibiliteDuale1. 
                
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "spx_sys.h"

# include "spx_fonctions.h"
# include "spx_define.h"

/*# undef UTILISER_BORNES_AUXILIAIRES*/

# define SEUIL_ADMISSIBILITE_DUALE_TOLERE 1.e-10 /* S'il n'y a qu'un petit nombre de seuils violes */

/*----------------------------------------------------------------------------*/

void SPX_VerifierAdmissibiliteDuale( PROBLEME_SPX * Spx , int * AdmissibiliteRestauree )
{
int Var; double SommeDesInfaisabilites; char RestaurerAdmissibiliteDuale; int Iteration;
double PlusGrandeViolation; int VariableDePlusGrandeViolation; double VolumeDeChangementDeBornes; 
int NombreDeChangementDeBorne; int NombreMaxDIterations; int * NumerosDesVariablesHorsBase;
int i; char * TypeDeVariable; double * CBarre; char * PositionDeLaVariable; double * Xmax;
double * C; char * OrigineDeLaVariable; char * StatutBorneSupCourante; double * Xmin;
double * SeuilDAmissibiliteDuale; int NbInfDual; int SeuilNbInfDual;

VariableDePlusGrandeViolation = -1;
PlusGrandeViolation           = 0.;

Spx->NbInfaisabilitesDuales = 0;			       
SommeDesInfaisabilites     = 0.0;

VolumeDeChangementDeBornes = 0.0;
NombreDeChangementDeBorne  = 0;

RestaurerAdmissibiliteDuale = NON_SPX;
*AdmissibiliteRestauree     = NON_SPX;

NumerosDesVariablesHorsBase = Spx->NumerosDesVariablesHorsBase;
TypeDeVariable              = Spx->TypeDeVariable;
CBarre                      = Spx->CBarre;
PositionDeLaVariable        = Spx->PositionDeLaVariable;
Xmax                        = Spx->Xmax;
C                           = Spx->C;
OrigineDeLaVariable         = Spx->OrigineDeLaVariable;
StatutBorneSupCourante      = Spx->StatutBorneSupCourante;
Xmin                        = Spx->Xmin;

SeuilDAmissibiliteDuale = Spx->SeuilDAmissibiliteDuale2;

/* Boucle sur les variables hors base */
/* Examen des couts reduits pour chaque type de variable. On compte le nombre de violations */
NbInfDual = 0;
SeuilNbInfDual = 5;
for ( i = 0 ; i < Spx->NombreDeVariablesHorsBase ; i++ ) {
  Var = NumerosDesVariablesHorsBase[i];
  if ( TypeDeVariable[Var] == BORNEE ) {
    if ( CBarre[Var] > SeuilDAmissibiliteDuale[Var] ) {
      if ( PositionDeLaVariable[Var] == HORS_BASE_SUR_BORNE_SUP ) NbInfDual++;             
    }
    else if ( CBarre[Var] < -SeuilDAmissibiliteDuale[Var] ) {
      if ( PositionDeLaVariable[Var] == HORS_BASE_SUR_BORNE_INF ) NbInfDual++;			      
    }   
    continue;
  }
  if ( TypeDeVariable[Var] == BORNEE_INFERIEUREMENT ) {
    /* La variable est duale realisable si son cout reduit est positif */
    if ( CBarre[Var] < -SeuilDAmissibiliteDuale[Var] ) NbInfDual++;	     		
  }
  if ( TypeDeVariable[Var] == NON_BORNEE ) {
    /* La variable est duale realisable si son cout reduit est nul */ 
    if ( fabs( CBarre[Var] ) > SeuilDAmissibiliteDuale[Var] ) NbInfDual++;	      			
  }
}

/* Examen des couts reduits pour chaque type de variable */
for ( i = 0 ; i < Spx->NombreDeVariablesHorsBase ; i++ ) {
  Var = NumerosDesVariablesHorsBase[i];
  if ( TypeDeVariable[Var] == BORNEE ) {

    if ( CBarre[Var] > SeuilDAmissibiliteDuale[Var] ) {
      if ( PositionDeLaVariable[Var] == HORS_BASE_SUR_BORNE_SUP ) {
			  if ( NbInfDual <= SeuilNbInfDual && CBarre[Var] < SEUIL_ADMISSIBILITE_DUALE_TOLERE ) {
			    /* On considere que c'est quand-meme bon */
					continue;
			  }			
        /*
        printf("Iteration %d erreur variable bornee des 2 cotes %d cbarre %e Xmax %e HORS_BASE_SUR_BORNE_SUP\n",
				        Spx->Iteration,Var,Spx->CBarre[Var],Spx->Xmax[Var]); 
        */							
        VolumeDeChangementDeBornes+= Xmax[Var];								 
        # ifdef UTILISER_BORNES_AUXILIAIRES
		      if ( Spx->NombreDeBornesAuxiliairesUtilisees > 0 ) {						 				
            if ( StatutBorneSupCourante[Var] == BORNE_AUXILIAIRE_DE_VARIABLE_BORNEE_INFERIEUREMENT ) {
					    /* La variable est devenue duale admissible avec ses bornes natives */
              SPX_SupprimerUneBorneAuxiliaire( Spx, Var );		
            }					
					}
 			  #endif
        NombreDeChangementDeBorne++;
        PositionDeLaVariable[Var] = HORS_BASE_SUR_BORNE_INF; 
        *AdmissibiliteRestauree   = OUI_SPX;
				continue;
      }
    }
    else if ( CBarre[Var] < -SeuilDAmissibiliteDuale[Var] ) {
      if ( PositionDeLaVariable[Var] == HORS_BASE_SUR_BORNE_INF ) {
			  if ( NbInfDual <= SeuilNbInfDual && CBarre[Var] > -SEUIL_ADMISSIBILITE_DUALE_TOLERE ) {
			    /* On considere que c'est quand-meme bon */
					continue;
			  }					
        /*
        printf("Iteration %d erreur variable bornee des 2 cotes %d cbarre %e Xmax %e HORS_BASE_SUR_BORNE_INF\n",
				        Spx->Iteration,Var,Spx->CBarre[Var],Spx->Xmax[Var]); 
        */
        NombreDeChangementDeBorne++;
        PositionDeLaVariable[Var]  = HORS_BASE_SUR_BORNE_SUP;
        VolumeDeChangementDeBornes+= Xmax[Var];       
        *AdmissibiliteRestauree    = OUI_SPX;
				continue;
      }
    }   
    /* Remarque importante: lorsque la base de part n'est pas fournie, on met initialement toutes les variables hors base 
       a HORS_BASE_SUR_BORNE_INF donc si les 2 tests ci-dessus ne positionne pas la variable c'est qu'elle est bien 
       positionnee */		
    continue;
  }

  if ( TypeDeVariable[Var] == BORNEE_INFERIEUREMENT ) {
    if ( PositionDeLaVariable[Var] != HORS_BASE_SUR_BORNE_INF ) {
		  printf("SPX_VerifierAdmissibiliteDuale:\n");
      printf("Bug iteration %d var %d cbarre %e BORNEE_INFERIEUREMENT pas HORS_BASE_SUR_BORNE_INF\n",Spx->Iteration,Var,Spx->CBarre[Var]);
      if ( Spx->PositionDeLaVariable[Var] == EN_BASE_LIBRE ) printf("Position: EN_BASE_LIBRE\n");
      if ( Spx->PositionDeLaVariable[Var] == HORS_BASE_SUR_BORNE_INF ) printf("Position: HORS_BASE_SUR_BORNE_INF\n");
      if ( Spx->PositionDeLaVariable[Var] == HORS_BASE_SUR_BORNE_SUP ) printf("Position: HORS_BASE_SUR_BORNE_SUP\n");
      if ( Spx->PositionDeLaVariable[Var] == HORS_BASE_A_ZERO ) printf("Position: HORS_BASE_A_ZERO\n");			
      printf("Xmin %e  Xmax %e\n",Spx->Xmin[Var],Spx->Xmax[Var]);			
      if ( Spx->OrigineDeLaVariable[Var] == NATIVE ) printf("Origine de la variable: NATIVE\n");			
      if ( Spx->OrigineDeLaVariable[Var] == ECART ) printf("Origine de la variable: ECART\n");			
      if ( Spx->OrigineDeLaVariable[Var] == BASIQUE_ARTIFICIELLE ) printf("Origine de la variable: BASIQUE_ARTIFICIELLE\n");									
      printf("IndexDeLaVariableDansLesVariablesHorsBase %d\n",Spx->IndexDeLaVariableDansLesVariablesHorsBase[Var]);			
      /*exit(0);*/
    }
    /* La variable est duale realisable si son cout reduit est positif */
    if ( CBarre[Var] < -SeuilDAmissibiliteDuale[Var] ) {
			if ( NbInfDual <= SeuilNbInfDual && CBarre[Var] > -SEUIL_ADMISSIBILITE_DUALE_TOLERE ) {
			  /* On considere que c'est quand-meme bon */
				continue;
			}		
      #if VERBOSE_SPX
        printf("SPX_VerifierAdmissibiliteDuale Iteration %d erreur iteration %d var %d cbarre %e seuil %e BORNEE_INFERIEUREMENT\n",
				        Spx->Iteration,Spx->Iteration,Var,Spx->CBarre[Var],-SeuilDAmissibiliteDuale[Var]); 
      #endif
			
      # ifndef UTILISER_BORNES_AUXILIAIRES
        SommeDesInfaisabilites+= CBarre[Var];
        Spx->NbInfaisabilitesDuales++;
        RestaurerAdmissibiliteDuale = OUI_SPX;
			#else
        /* On cree une borne sup et on change la position de la variable */        							
        PositionDeLaVariable  [Var] = HORS_BASE_SUR_BORNE_SUP; 
		    TypeDeVariable        [Var] = BORNEE;
        StatutBorneSupCourante[Var] = BORNE_AUXILIAIRE_DE_VARIABLE_BORNEE_INFERIEUREMENT;				
			  Xmax                  [Var] = SPX_CalculerUneBorneAuxiliaire( Spx, Var );
        /*
        printf("SPX_VerifierAdmissibiliteDuale Iteration %d erreur iteration %d var %d cbarre %e BORNEE_INFERIEUREMENT => On cree une borne auxiliaire valeur %e\n",
				        Spx->Iteration,Spx->Iteration,Var,Spx->CBarre[Var],Xmax[Var]); 		  		
				*/
        Spx->NombreDeBornesAuxiliairesUtilisees++;
			  *AdmissibiliteRestauree = OUI_SPX;
        NombreDeChangementDeBorne++;
        VolumeDeChangementDeBornes+= Xmax[Var];
			#endif
			continue;
    }		
    continue;
  }

  if ( TypeDeVariable[Var] == NON_BORNEE ) {
    if ( PositionDeLaVariable[Var] != HORS_BASE_A_ZERO ) {
		  printf("SPX_VerifierAdmissibiliteDuale:\n");
      printf("Bug iteration %d var %d cbarre %e NON_BORNEE hors base mais pas HORS_BASE_A_ZERO \n",Spx->Iteration,Var,Spx->CBarre[Var]);
      if ( Spx->PositionDeLaVariable[Var] == EN_BASE_LIBRE ) printf("Position: EN_BASE_LIBRE\n");
      if ( Spx->PositionDeLaVariable[Var] == HORS_BASE_SUR_BORNE_INF ) printf("Position: HORS_BASE_SUR_BORNE_INF\n");
      if ( Spx->PositionDeLaVariable[Var] == HORS_BASE_SUR_BORNE_SUP ) printf("Position: HORS_BASE_SUR_BORNE_SUP\n");
      if ( Spx->PositionDeLaVariable[Var] == HORS_BASE_A_ZERO ) printf("Position: HORS_BASE_A_ZERO\n");						
      printf("Xmin %e  Xmax %e\n",Spx->Xmin[Var],Spx->Xmax[Var]);
      if ( Spx->OrigineDeLaVariable[Var] == NATIVE ) printf("Origine de la variable: NATIVE\n");			
      if ( Spx->OrigineDeLaVariable[Var] == ECART ) printf("Origine de la variable: ECART\n");			
      if ( Spx->OrigineDeLaVariable[Var] == BASIQUE_ARTIFICIELLE ) printf("Origine de la variable: BASIQUE_ARTIFICIELLE\n");							
      printf("IndexDeLaVariableDansLesVariablesHorsBase %d\n",Spx->IndexDeLaVariableDansLesVariablesHorsBase[Var]);			
      /*exit(0);*/
    }
    /* La variable est duale realisable si son cout reduit est nul */ 
    if ( fabs( CBarre[Var] ) > SeuilDAmissibiliteDuale[Var] ) {
			if ( NbInfDual <= SeuilNbInfDual && CBarre[Var] < SEUIL_ADMISSIBILITE_DUALE_TOLERE ) {
			  /* On considere que c'est quand-meme bon */
				continue;
			}			
      #if VERBOSE_SPX
        printf("SPX_VerifierAdmissibiliteDuale Iteration %d erreur var %d cbarre %e hors base NON_BORNEE\n",Spx->Iteration,Var,Spx->CBarre[Var]); 
      #endif
						
      # ifndef UTILISER_BORNES_AUXILIAIRES
        SommeDesInfaisabilites+= CBarre[Var];
        Spx->NbInfaisabilitesDuales++;
        RestaurerAdmissibiliteDuale = OUI_SPX;
        /* On veut absolument faire des iterations de phase 1 plutot que modifier le cout */
			#else
        /* On cree une borne sup et on change la position de la variable */
        /*
        printf("Iteration %d erreur var %d cbarre %e hors base NON_BORNEE => On cree une borne auxiliaire\n",Spx->Iteration,Var,Spx->CBarre[Var]); 
				*/				
			  TypeDeVariable        [Var] = BORNEE;
        StatutBorneSupCourante[Var] = BORNE_AUXILIAIRE_DE_VARIABLE_NON_BORNEE;
			  Xmax[Var] = SPX_CalculerUneBorneAuxiliaire( Spx, Var );							
		 	  Xmin[Var] = 0.;
        Spx->NombreDeBornesAuxiliairesUtilisees++;     							
        if ( CBarre[Var] < -SeuilDAmissibiliteDuale[Var] ) PositionDeLaVariable[Var] = HORS_BASE_SUR_BORNE_SUP;			
        else PositionDeLaVariable[Var] = HORS_BASE_SUR_BORNE_INF;			
        *AdmissibiliteRestauree = OUI_SPX;
        NombreDeChangementDeBorne++;
			  VolumeDeChangementDeBornes+= Xmax[Var];
			#endif
      continue;
    }				
  }  

}

#if VERBOSE_SPX
  if ( NombreDeChangementDeBorne > 0 ) {
	  printf("Controle d'admissibilite duale, nombre de variables bornees repositionnes %d VolumeDeChangementDeBornes %e\n",
		        NombreDeChangementDeBorne,VolumeDeChangementDeBornes);
	}
#endif
		
/* Il faut le laisser car on peut repositionner des bornes sur des variables avec Xmin = Xmax = 0 */
if ( VolumeDeChangementDeBornes < 1.e-8 ) *AdmissibiliteRestauree = NON_SPX;

Spx->SommeDesInfaisabilitesDuales = -SommeDesInfaisabilites;

if ( RestaurerAdmissibiliteDuale == OUI_SPX ) {
  Iteration            = Spx->Iteration;
  NombreMaxDIterations = Spx->NombreMaxDIterations;
  /* On limite a 1000 */
  # ifndef UTILISER_BORNES_AUXILIAIRES
    Spx->NombreMaxDIterations = 1000;
	# endif
  
  #if VERBOSE_SPX
    printf("Restauration de l'admissibilite duale necessaire a l'iteration %d : ",Spx->Iteration); 
    printf("Nombre d'infaisabilites duales %5d Somme des infaisabilites duales %e \n",Spx->NbInfaisabilitesDuales,-SommeDesInfaisabilites); 
  #endif
    
  SPX_FactoriserLaBase( Spx );
  SPX_DualPhase1Simplexe( Spx );

	Spx->PhaseEnCours = PHASE_2;
  
  Spx->Iteration            = Iteration;
  Spx->NombreMaxDIterations = NombreMaxDIterations;
  Spx->CalculerBBarre       = OUI_SPX; 
  Spx->CalculerCBarre       = OUI_SPX;
  
  if ( Spx->LaBaseEstDualeAdmissible == NON_SPX ) {
    /* Echec */
    Spx->YaUneSolution = NON_SPX;
    Spx->Iteration     = 10 * Spx->NombreMaxDIterations;
  }
  
  *AdmissibiliteRestauree = OUI_SPX;
  
}  

return;
}


