## Configuration et build de la chaîne Sirius - OR-Tools - Metrix

```bash
### clone du dépôt git de Sirius dans un dossier sirius-solver
git clone https://github.com/rte-france/temp-pne.git --branch=metrix sirius-solver
### configuration cmake + build + install en Release
cmake -S sirius-solver/ -B sirius-solver/builds/ -D CMAKE_BUILD_TYPE=Release -D CMAKE_INSTALL_PREFIX=install/sirius-solver/
cmake --build sirius-solver/builds --config Release --target install

### clone du dépôt git du fork d'OR-Tools dans un dossier or-tools
git clone https://github.com/rte-france/or-tools.git --branch=rte_dev or-tools
### configuration cmake + build + install en Release
cmake -S or-tools/ -B or-tools/builds/ -D CMAKE_BUILD_TYPE=Release -D CMAKE_INSTALL_PREFIX=install/or-tools/ \
      -D BUILD_DEPS=ON -D USE_SIRIUS=ON -D sirius_solver_ROOT=install/sirius-solver -D USE_XPRESS=ON -D XPRESS_ROOT="path/to/xpress/install"
# NB : Afin de diminuer le temps de compilations, certaines options peuvent être désactivées : BUILD_CXX_SAMPLES / BUILD_CXX_EXAMPLES
cmake --build or-tools/builds --config Release --target install

### clone du dépôt git de Metrix dans un dossier metrix
git clone https://devin-source.rte-france.com/imagrid/metrix.git --branch=dev_ortools metrix
### configuration cmake + build en Release
cmake -S metrix/ -B metrix/builds/ -D CMAKE_BUILD_TYPE=Release \
      -D BOOST_ROOT="path/to/boost-1.66.0/install" -D sirius_solver_ROOT=install/sirius-solver/ -D ortools_ROOT=install/or-tools -D XPRESS_ROOT:="path/to/xpress/install" \
      -D USE_ORTOOLS=ON
# NB : la valeur de l'option USE_ORTOOLS permet d'activer ou non l'appel de Sirius via OR-Tools. Par défaut cette option vaut OFF.
cmake --build metrix/builds --config Release
### execution des tests
cd metrix/builds; ctest --build-config Release; cd -
# NB : un fichier de log de l'exécution des tests est généré : metrix/builds/Testing/Temporary/LastTest.log
```

## Metrix : comment utiliser un autre solveur que Sirius ?
Pour utiliser un autre solveur en passant par OR-Tools, il faut changer le type renvoyé par les fonctions `Solver::type<PROBLEME_A_RESOUDRE>()` et `Solver::type<PROBLEME_SIMPLEXE>()` du fichier src/ortools/solver.cpp (l. 245)  
Exemple Sirius : 
```cpp
template<>
operations_research::MPSolver::OptimizationProblemType Solver::type<PROBLEME_A_RESOUDRE>()
{
    return operations_research::MPSolver::SIRIUS_MIXED_INTEGER_PROGRAMMING;
}

template<>
operations_research::MPSolver::OptimizationProblemType Solver::type<PROBLEME_SIMPLEXE>()
{
    return operations_research::MPSolver::SIRIUS_LINEAR_PROGRAMMING;
}
```
Exemple Xpress : 
```cpp
template<>
operations_research::MPSolver::OptimizationProblemType Solver::type<PROBLEME_A_RESOUDRE>()
{
    return operations_research::MPSolver::XPRESS_MIXED_INTEGER_PROGRAMMING;
}

template<>
operations_research::MPSolver::OptimizationProblemType Solver::type<PROBLEME_SIMPLEXE>()
{
    return operations_research::MPSolver::XPRESS_LINEAR_PROGRAMMING;
}
```
NB : Les différentes valeurs possibles sont celles de l'enum operations_research::MPSolver::OptimizationProblemType du projet OR-Tools (ortools/linear_solver/linear_solver.h:187)
