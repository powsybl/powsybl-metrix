# ifndef PNE_MACROS_POUR_FONCTION_EXTERNES_DE_GESTION_MEMOIRE
# include "mem_fonctions.h"
/*****************************************************************

 
  Macros pour redefinir les primitives de gestion memoire lorsqu'on
  ne veut pas utiliser celles de lib de l'OS

	
*****************************************************************/
	
# define malloc(Taille)           MEM_Malloc(Pne->Tas,Taille)
# define free(Pointeur)           MEM_Free(Pointeur)
# define realloc(Pointeur,Taille) MEM_Realloc(Pne->Tas,Pointeur,Taille)

/*****************************************************************/
# define PNE_MACROS_POUR_FONCTION_EXTERNES_DE_GESTION_MEMOIRE
# endif
