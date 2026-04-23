## Building metrix-simulator

### Prerequisites

- **CMake** ≥ 3.14
- **C/C++ compiler** with C++11 support
- **Boost** ≥ 1.66 — must be installed on the system
- **Git** — for downloading external dependencies
- **Xpress** (optional): pre-installed commercial solver, with the `XPRESS_ROOT` environment variable pointing to the installation directory

### Build

```bash
# Clone the repository
git clone https://github.com/powsybl/powsybl-metrix.git powsybl-metrix

# Build external dependencies (SuiteSparse, Sirius, OR-Tools)
mkdir -p powsybl-metrix/metrix-simulator/build/external
cd powsybl-metrix/metrix-simulator/build/external

cmake ../../external \
      -D CMAKE_BUILD_TYPE=Release \
      -D USE_ORTOOLS=ON \
      -D USE_XPRESS=ON \
      -D XPRESS_ROOT="$XPRESS_ROOT"

cmake --build . -j$(nproc)
cd ../../../..

# Build metrix-simulator
cmake -S powsybl-metrix/metrix-simulator \
      -B powsybl-metrix/metrix-simulator/build \
      -D CMAKE_BUILD_TYPE=Release \
      -D USE_ORTOOLS=ON \
      -D USE_XPRESS=ON \
      -D XPRESS_ROOT="$XPRESS_ROOT"

cmake --build powsybl-metrix/metrix-simulator/build -j$(nproc)

# Run tests
ctest --test-dir powsybl-metrix/metrix-simulator/build
```

> **Note**: building external dependencies includes downloading and compiling OR-Tools and its own dependencies (abseil, protobuf, etc.). The first run may take several minutes. Subsequent builds are incremental.

### CMake options

| Option | Default | Description |
|--------|---------|-------------|
| `USE_ORTOOLS` | `OFF` | Enable multi-solver support via OR-Tools |
| `USE_XPRESS` | `OFF` | Enable the Xpress backend in OR-Tools (requires `USE_ORTOOLS=ON`) |
| `XPRESS_ROOT` | `$XPRESS_ROOT` | Xpress installation path (cmake variable or environment variable) |

Without `USE_ORTOOLS`, the build produces a Sirius-only binary, identical to the legacy production version.

---

## Runtime solver selection

A single build supports both Sirius and Xpress. The solver is selected at runtime through the `SOLVERCH` and/or `PCSOLVERCH` fields in the `attributes` list of the `IntegerFile` section of the input file `fort.json`.

### Solver parameters

| Parameter | Used for | Default value |
|-----------|----------|---------------|
| `SOLVERCH` | Main network optimization (MIP/LP) | SIRIUS (5) |
| `PCSOLVERCH` | Economic stacking of generators (initial phase, without network) | SIRIUS (5) |

### Available values

| Value | Solver | Execution path |
|-------|--------|----------------|
| 5 | SIRIUS | Direct call to `PNE_Solveur` / `SPX_Simplexe` (no OR-Tools involved) |
| 6 | XPRESS | Via OR-Tools `MPSolver` |
| 0 | GLPK | Via OR-Tools `MPSolver` |
| 1 | CBC | Via OR-Tools `MPSolver` |
| 2 | SCIP_GLOP | Via OR-Tools `MPSolver` |
| 3 | GUROBI | Via OR-Tools `MPSolver` |
| 4 | CPLEX | Via OR-Tools `MPSolver` |

> **Important**: `SOLVERCH=5` (SIRIUS) uses the direct call path, identical to the legacy production behavior. All other values go through the OR-Tools abstraction layer. Commercial solvers (GUROBI, CPLEX, XPRESS) require a license installed on the machine.

### Configuration examples

**Sirius direct for both phases** (default behavior, identical to production):

No configuration needed, or explicitly:
```json
{
  "name": "SOLVERCH",
  "type": "INTEGER",
  "valueCount": 1,
  "firstIndexMaxValue": 1,
  "secondIndexMaxValue": 1,
  "firstValueIndex": 1,
  "lastValueIndex": 1,
  "values": [5]
},
{
  "name": "PCSOLVERCH",
  "type": "INTEGER",
  "valueCount": 1,
  "firstIndexMaxValue": 1,
  "secondIndexMaxValue": 1,
  "firstValueIndex": 1,
  "lastValueIndex": 1,
  "values": [5]
}
```

**Xpress for both phases**:
```json
{
  "name": "SOLVERCH",
  "type": "INTEGER",
  "valueCount": 1,
  "firstIndexMaxValue": 1,
  "secondIndexMaxValue": 1,
  "firstValueIndex": 1,
  "lastValueIndex": 1,
  "values": [6]
},
{
  "name": "PCSOLVERCH",
  "type": "INTEGER",
  "valueCount": 1,
  "firstIndexMaxValue": 1,
  "secondIndexMaxValue": 1,
  "firstValueIndex": 1,
  "lastValueIndex": 1,
  "values": [6]
}
```

**Mixed configuration** — economic stacking with Sirius direct, main optimization with Xpress:
```json
{
  "name": "PCSOLVERCH",
  "type": "INTEGER",
  "valueCount": 1,
  "firstIndexMaxValue": 1,
  "secondIndexMaxValue": 1,
  "firstValueIndex": 1,
  "lastValueIndex": 1,
  "values": [5]
},
{
  "name": "SOLVERCH",
  "type": "INTEGER",
  "valueCount": 1,
  "firstIndexMaxValue": 1,
  "secondIndexMaxValue": 1,
  "firstValueIndex": 1,
  "lastValueIndex": 1,
  "values": [6]
}
```

> **Note**: if only `SOLVERCH` is configured, `PCSOLVERCH` defaults to SIRIUS (5). This may be intentional when using Sirius for the economic stacking phase and a higher-performance solver for the main optimization.

### Advanced parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `SPECIFICSOLVERPARAMS` | STRING | Solver-specific parameters passed through to the solver (e.g. Xpress options) |
