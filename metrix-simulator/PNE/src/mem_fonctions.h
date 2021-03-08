# ifdef __CPLUSPLUS
  extern "C"
	{
# endif
# ifndef FONCTION_EXTERNES_MEMOIRE_DEJA_DEFINIES
/*****************************************************************/

void * MEM_Init( void );
void   MEM_Quit( void * );
char * MEM_Malloc( void * , size_t );
void   MEM_Free( void * ); 
char * MEM_Realloc( void * , void * , size_t );
long   MEM_QuantiteLibre( BLOCS_LIBRES * );

/*****************************************************************/
# define FONCTION_EXTERNES_MEMOIRE_DEJA_DEFINIES
# endif
# ifdef __CPLUSPLUS
  }
# endif 
