/***********************************************************************

   FONCTION: Factorisation LU.
                
 
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "lu_constantes_externes.h"
# include "lu_definition_arguments.h"
# include "lu_define.h"
# include "lu_fonctions.h"

# ifdef LU_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "lu_memoire.h"
# endif

/*--------------------------------------------------------------------------------------------------*/

MATRICE * LU_Factorisation( MATRICE_A_FACTORISER * Mat )  	    
{
int FaireScalingDeLaMatrice; MATRICE * Matrice; void * Tas            ;
double SeuilDePivotage      ; int NombreDeChangementsDeSeuilDePivotage;

# ifdef LU_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  Tas = MEM_Init(); 
  Matrice = (MATRICE *) MEM_Malloc ( Tas, sizeof( MATRICE ) );
  if ( Matrice == NULL ) {
    printf("Saturation memoire, impossible d'allouer un objet MATRICE\n");  
    Mat->ProblemeDeFactorisation = SATURATION_MEMOIRE; 
    return( NULL );
  }
  Matrice->Tas = Tas;
# else
  Matrice = (MATRICE *) malloc ( sizeof( MATRICE ) );
  if ( Matrice == NULL ) {
    printf("Saturation memoire, impossible d'allouer un objet MATRICE\n");  
    Mat->ProblemeDeFactorisation = SATURATION_MEMOIRE; 
    return( NULL );
  }
  Matrice->Tas = NULL;
# endif

LU_InitTableauxANull( Matrice );

Matrice->MatriceEntree = Mat;
			
Matrice->AnomalieDetectee = NON_LU;
Matrice->NombreDeChangementsDeSeuilDePivotage = 0;

if ( (Matrice->MatriceEntree)->SeuilPivotMarkowitzParDefaut == OUI_LU ) Matrice->SeuilDePivotage = PREMIER_SEUIL_DE_PIVOTAGE;
else Matrice->SeuilDePivotage = (Matrice->MatriceEntree)->ValeurDuPivotMarkowitz;

/* Precaution pour les etourdis */
if ( Matrice->SeuilDePivotage > 1.0 ) Matrice->SeuilDePivotage = 1.0;  
else if ( Matrice->SeuilDePivotage == 0.0 ) Matrice->SeuilDePivotage = PREMIER_SEUIL_DE_PIVOTAGE;

FaireScalingDeLaMatrice = (Matrice->MatriceEntree)->FaireScalingDeLaMatrice;
if ( FaireScalingDeLaMatrice != OUI_LU && FaireScalingDeLaMatrice != NON_LU ) {
  printf("Vous avez mal initialise l'indicateur FaireScalingDeLaMatrice. Vous devez choisir entre OUI_LU et NON_LU\n");
  (Matrice->MatriceEntree)->ProblemeDeFactorisation = OUI_LU;   
  return( Matrice );  
}

Factorisation:
setjmp( Matrice->Env );  
if ( Matrice->AnomalieDetectee != NON_LU ) {
     
   #if VERBOSE_LU
     printf(" Probleme dans la factorisation LU\n");fflush(stdout);
   #endif

   /*LU_LibererLesTableauxTemporaires( Matrice );*/ /* Fait ci-dessous */

   if ( Matrice->AnomalieDetectee == MATRICE_SINGULIERE ) {

     LU_LibererLesTableauxTemporaires( Matrice );
	 
     if ( Matrice->NombreDeChangementsDeSeuilDePivotage < Matrice->NombreMaxDeChangementsDeSeuilDePivotage ) {
       if ( Matrice->SeuilDePivotage + INCREMENT_DE_SEUIL_DE_PIVOTAGE < 1.0 ) {

         #if VERBOSE_LU
           printf(" modification de SeuilDePivotage avant %lf apres %lf\n",
                    Matrice->SeuilDePivotage,Matrice->SeuilDePivotage+INCREMENT_DE_SEUIL_DE_PIVOTAGE );
           fflush(stdout);
         #endif
	 	   
	       SeuilDePivotage                      = Matrice->SeuilDePivotage;
         NombreDeChangementsDeSeuilDePivotage = Matrice->NombreDeChangementsDeSeuilDePivotage;

	       Mat = Matrice->MatriceEntree;
	 
         LU_LibererMemoireLU( Matrice );

         # ifdef LU_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
           Tas = MEM_Init(); 
           Matrice = (MATRICE *) MEM_Malloc ( Tas, sizeof( MATRICE ) );
           if ( Matrice == NULL ) {
             printf("Saturation memoire, impossible d'allouer un objet MATRICE\n");  
             Mat->ProblemeDeFactorisation = SATURATION_MEMOIRE; 
             return( NULL );
           }
           Matrice->Tas = Tas;
         # else
           Matrice = (MATRICE *) malloc ( sizeof( MATRICE ) );
           if ( Matrice == NULL ) {
             printf("Saturation memoire, impossible d'allouer un objet MATRICE\n");  
             Mat->ProblemeDeFactorisation = SATURATION_MEMOIRE; 
             return( NULL );
           }
           Matrice->Tas = NULL;
         # endif				 

         LU_InitTableauxANull( Matrice );
				 
				 Matrice->MatriceEntree = Mat;
				 
         Matrice->AnomalieDetectee = NON_LU;
         Matrice->NombreDeChangementsDeSeuilDePivotage = 0;
				   
         Matrice->SeuilDePivotage                      = SeuilDePivotage + INCREMENT_DE_SEUIL_DE_PIVOTAGE; 
         Matrice->NombreDeChangementsDeSeuilDePivotage = NombreDeChangementsDeSeuilDePivotage + 1;
				 
				 goto Factorisation;
       }
     }
 
   }

  (Matrice->MatriceEntree)->ProblemeDeFactorisation = Matrice->AnomalieDetectee;
  #if VERBOSE_LU
    printf(" Matrice non inversible malgre les changements de seuils\n");fflush(stdout);
  #endif
   
  return( Matrice );
} 
else {
  LU_FactorisationCalculs( Matrice , Matrice->MatriceEntree );
}

return( Matrice );
}























































