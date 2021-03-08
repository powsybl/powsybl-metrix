/***********************************************************************

   FONCTION: Routine principale du point interieur. Cette implementation
             du point interieur est thread safe c'est a dire qu'on
             peut lancer plusieurs point interieur en parallele.
 
   Optimisation d'un critere quadratique ou lineaire sous contraintes
   lineaires.
             
   AUTEUR: R. GONZALEZ

************************************************************************/

# include "pi_sys.h"

# include "pi_fonctions.h"
# include "pi_define.h"

# ifdef PI_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
  # include "pi_memoire.h"
# endif

/*----------------------------------------------------------------------------*/
/*                   Routine d'entree du point interieur                      */

void PI_Quamin( PROBLEME_POINT_INTERIEUR * Probleme )
{
PROBLEME_PI * Pi; void * Tas;

Pi = NULL;

if ( Pi == NULL ) {
  # ifdef PI_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
    Tas = MEM_Init();
    Pi = (PROBLEME_PI *) MEM_Malloc( Tas, sizeof( PROBLEME_PI ) );
    if ( Pi == NULL ) {
      printf("Saturation memoire, impossible d'allouer un objet PROBLEME_PI\n");
      Probleme->ExistenceDUneSolution = PI_ERREUR_INTERNE;
      return;
    }				
	  Pi->Tas = Tas;
  # else 		
    Pi = (PROBLEME_PI *) malloc( sizeof( PROBLEME_PI ) );
    if ( Pi == NULL ) {
      printf("Saturation memoire, impossible d'allouer un objet PROBLEME_PI\n");
      Probleme->ExistenceDUneSolution = PI_ERREUR_INTERNE;
      return;
    }		
	  Pi->Tas = NULL;
	# endif
}

Pi->AnomalieDetectee = NON_PI; /* codes d'erreur a revoir */
setjmp( Pi->Env );  
if ( Pi->AnomalieDetectee != NON_PI ) {
  /* Liberation du probleme */ 
  PI_LibereProbleme( Pi );
  PI_LibereMatrice( Pi );

  # ifdef PI_UTILISER_LES_OUTILS_DE_GESTION_MEMOIRE_PROPRIETAIRE	
    MEM_Quit( Pi->Tas );
  # else
    free( Pi );
  # endif
	
  return;
} 
else {
  /* Optimisation */
  PI_QuaminCalculs( Probleme , Pi );
  /* Pi est toujours libere */
}

return;
}


