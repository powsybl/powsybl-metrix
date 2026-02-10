## Configuration et build de la chaîne Sirius - OR-Tools - Metrix

```bash
### clone du dépôt git de Sirius dans un dossier sirius-solver
git clone https://github.com/rte-france/sirius-solver.git --branch=metrix sirius-solver
### configuration cmake + build + install en Release
cmake -S sirius-solver/ -B sirius-solver/builds/ -D CMAKE_BUILD_TYPE=Release -D CMAKE_INSTALL_PREFIX=install/sirius-solver/
cmake --build sirius-solver/builds --config Release --target install

### clone du dépôt git d'OR-Tools version RTE
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
git clone https://github.com/powsybl/powsybl-metrix.git --branch=ortools-updated powsybl-metrix
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
# NB : un fichier de log de l'exécution des tests est généré : metrix-simulator/build-ortools/Testing/Temporary/LastTest.log
```

## Metrix : comment utiliser un autre solveur que Sirius ?
Dans le fichier d'entrée de metrix-simulator 'fort.json' ajouter/modifier le champ "SOLVERCH" dans la liste "attributes" de la partie "IntegerFile" comme suit :
```json
        {
          "name": "SOLVERCH",
          "type": "INTEGER",
          "valueCount": 1,
          "firstIndexMaxValue": 1,
          "secondIndexMaxValue": 1,
          "firstValueIndex": 1,
          "lastValueIndex": 1,
          "values": [6]  <- valeur de l'enum qui correspond au solveur choisi (enum config::Configuration::SolverChoice dans metrix-simulator/src/config/configuration.h:36) ; ici 6 <=> Xpress
        }
```
