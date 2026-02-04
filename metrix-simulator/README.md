## Configuration et build de la chaîne Sirius - OR-Tools - Metrix

```bash
### clone du dépôt git de Sirius dans un dossier sirius-solver
git clone https://github.com/rte-france/sirius-solver.git --branch=metrix sirius-solver
### configuration cmake + build + install en Release
cmake -S sirius-solver/ -B sirius-solver/builds/ -D CMAKE_BUILD_TYPE=Release -D CMAKE_INSTALL_PREFIX=install/sirius-solver/
cmake --build sirius-solver/builds --config Release --target install

### clone du dépôt git du fork d'OR-Tools dans un dossier or-tools
git clone https://github.com/rte-france/or-tools-rte.git --branch=release/v9.11-rte1.1 or-tools
### configuration cmake + build + install en Release
cmake -S or-tools/ -B or-tools/builds/ \
      -D CMAKE_BUILD_TYPE=Release \
      -D CMAKE_INSTALL_PREFIX=install/or-tools/ \
      -D ortools_REPO=https://github.com/google/or-tools \
      -D ortools_REF=v9.11 \
      -D USE_SIRIUS=ON \
      -D CMAKE_PREFIX_PATH="$SIRIUS_ROOT" \
      -D USE_XPRESS=ON \
      -D XPRESS_ROOT="$XPRESS_ROOT"
# NB : Afin de diminuer le temps de compilations, certaines options peuvent être désactivées : BUILD_CXX_SAMPLES / BUILD_CXX_EXAMPLES
cmake --build or-tools/builds --config Release --target install

### clone du dépôt git de Metrix dans un dossier metrix
git clone https://github.com/powsybl/powsybl-metrix.git --branch=temporary_ortools powsybl-metrix
### configuration cmake + build en Release
cmake -S powsybl-metrix/metrix-simulator -B powsybl-metrix/metrix-simulator/build-ortools \
      -D CMAKE_BUILD_TYPE=Release \
      -D sirius_solver_ROOT="$SIRIUS_ROOT" \
      -D CMAKE_PREFIX_PATH="$HOME/software/install/or-tools" \
      -D XPRESS_ROOT="$XPRESS_ROOT" \
      -D USE_ORTOOLS=ON \
      -D KLU_INCLUDE_DIR=/usr/include/suitesparse \
      -D INSTALL_CMAKE_DIR=lib/cmake/metrix-simulator
# NB : la valeur de l'option USE_ORTOOLS permet d'activer ou non l'appel de Sirius via OR-Tools. Par défaut cette option vaut OFF.
cmake --build powsybl-metrix/metrix-simulator/build-ortools --config Release
### execution des tests
ctest --test-dir powsybl-metrix/metrix-simulator/build-ortools --build-config Release
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
