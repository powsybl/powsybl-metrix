## Building metrix-simulator

### Prerequisites

- **CMake** ≥ 3.14
- **C/C++ compiler** with C++11 support
- **Boost** ≥ 1.66 — must be installed on the system
- **Git** — for downloading external dependencies
- **Xpress** (optional, **runtime only**): a licensed FICO Xpress installation on the machine that runs `SOLVERCH=6`, located through the `XPRESSDIR` environment variable. Nothing from Xpress is needed to build: OR-Tools does not link `libxprs`, it loads `$XPRESSDIR/lib/libxprs.so` with `dlopen` at the first Xpress solve (falling back to `/opt/xpressmp`). `XPRESSDIR` is normally exported by the `xpvars.sh` script shipped with Xpress

### Build

```bash
# Clone the repository
git clone https://github.com/powsybl/powsybl-metrix.git powsybl-metrix

# Build external dependencies (SuiteSparse, Sirius, and optionally OR-Tools).
# OR-Tools is built only when USE_ORTOOLS=ON (default OFF, mirroring the
# metrix-simulator flag). Its Xpress backend is always compiled in and needs
# no Xpress installation at build time.
mkdir -p powsybl-metrix/metrix-simulator/build/external
cd powsybl-metrix/metrix-simulator/build/external

cmake ../../external \
      -D CMAKE_BUILD_TYPE=Release \
      -D USE_ORTOOLS=ON

cmake --build . -j$(nproc)
cd ../../../..

# Build metrix-simulator.
# USE_ORTOOLS enables the OR-Tools Xpress backend at runtime.
# USE_XPRESS authorizes SOLVERCH=6 at runtime. It is a pure authorization
# switch: whether Xpress actually works is decided on the running machine
# by XPRESSDIR and the Xpress license.
cmake -S powsybl-metrix/metrix-simulator \
      -B powsybl-metrix/metrix-simulator/build \
      -D CMAKE_BUILD_TYPE=Release \
      -D USE_ORTOOLS=ON \
      -D USE_XPRESS=ON

cmake --build powsybl-metrix/metrix-simulator/build -j$(nproc)

# Run tests (the Xpress suite, if enabled, receives XPRESSDIR from the harness)
ctest --test-dir powsybl-metrix/metrix-simulator/build
```

> **Note**: the Xpress TNR suite (`tests/xpress`) is disabled by default
> because FICO Xpress is a licensed product: it must never run in
> automated environments. Developers with a licensed Xpress installation
> can enable it by configuring with `-D METRIX_RUN_XPRESS_TESTS=ON`
> (in addition to `USE_XPRESS=ON`); the harness hands the Xpress location
> to every test as `XPRESSDIR`, taken from `XPRESS_ROOT` (CMake variable or
> environment) or, failing that, from the `XPRESSDIR` already exported in
> the environment. The per-category `CMakeLists.txt`
> files under `tests/xpress/` are symbolic links to their `tests/sirius/`
> counterparts (single source of truth for test registrations): on
> Windows, the repository must be cloned with `git config core.symlinks
> true` (and the *Developer Mode* or admin rights required by Windows for
> symlink creation), otherwise CMake will fail on these files when the
> Xpress suite is enabled.

> **Note**: building external dependencies includes downloading and
> compiling OR-Tools along with its own dependencies (abseil, protobuf,
> etc.).

#### Skipping `external/` (system-installed dependencies)

If SuiteSparse, Sirius, and OR-Tools are already installed system-wide
(or in custom locations), `external/` can be bypassed. Point the root
configure at the installs directly:

```bash
cmake -S powsybl-metrix/metrix-simulator \
      -B powsybl-metrix/metrix-simulator/build \
      -D CMAKE_BUILD_TYPE=Release \
      -D USE_ORTOOLS=ON \
      -D USE_XPRESS=ON \
      -D USE_SIRIUS_SHARED=ON \
      -D sirius_solver_ROOT=/path/to/sirius \
      -D SUITESPARSE_HOME=/path/to/suitesparse \
      -D ortools_ROOT=/path/to/ortools
```

The variant of Sirius to point at depends on `USE_SIRIUS_SHARED`:
`sirius_solver_ROOT` for the shared library, `sirius_solver_static_ROOT`
for the static one.

### CMake options

#### `external/` options

| Variable | Form | Description |
|----------|------|-------------|
| `USE_ORTOOLS` | CMake option | Default `OFF`. Build the OR-Tools third party. Must be `ON` when `metrix-simulator` is configured with `USE_ORTOOLS=ON` (OR-Tools requires Python3 with development headers and a recent C++ compiler, hence the opt-in). |
| `NNI`, `NNI_PASSWORD` | env var only | Internal RTE credentials for Git proxy. Optional. |

#### Root options

| Option | Default | Description |
|--------|---------|-------------|
| `USE_ORTOOLS` | `OFF` | Enable the OR-Tools Xpress backend. When `OFF`, the binary is Sirius-only, identical to the legacy production version. |
| `USE_XPRESS` | `OFF` | Authorize `SOLVERCH=6` at runtime. Requires `USE_ORTOOLS=ON` (enforced by `cmake_dependent_option`). Pure authorization switch: the Xpress backend is always compiled into OR-Tools 9.13, and whether it works on a given machine depends on `XPRESSDIR` and the Xpress license; a missing library or license is reported at runtime as a metrix error (`ERRSolveurIndisponible`) when `SOLVERCH=6` is requested. |
| `XPRESS_ROOT` | `-D` flag or env var | Test harness only: Xpress installation handed to the test suite as `XPRESSDIR`. Defaults to the `XPRESSDIR` of the environment. Not needed to build. |
| `USE_SIRIUS_SHARED` | `OFF` | Link Sirius as a shared library instead of static. When `ON`, deploys `libsirius_solver.so` alongside the binary. |
| `CODE_COVERAGE` | `OFF` | Instrument the binary for coverage analysis (forces `Debug` build type). |
| `METRIX_RUN_ALL_TESTS` | `ON` | Run the full TNR test suite. Set to `OFF` for a reduced suite. |

---

## Runtime solver selection

A single build supports both Sirius and Xpress. The solver is selected at runtime through the `SOLVERCH` and/or `PCSOLVER` fields in the `attributes` list of the `IntegerFile` section of the input file `fort.json`.

### Solver parameters

| Parameter | Used for | Default value |
|-----------|----------|---------------|
| `SOLVERCH` | Main network optimization (MIP/LP) | SIRIUS (5) |
| `PCSOLVER` | Economic stacking of generators (initial phase, without network) | SIRIUS (5) |

### Available values

| Value | Solver | Execution path |
|-------|--------|----------------|
| 5 | SIRIUS | Direct call to `PNE_Solveur` / `SPX_Simplexe` (no OR-Tools involved) |
| 6 | XPRESS | Via OR-Tools `MPSolver` |

> **Important**: `SOLVERCH=5` (SIRIUS) uses the direct call path, identical to the legacy production behavior. `SOLVERCH=6` (XPRESS) goes through the OR-Tools abstraction layer and requires a binary built with `USE_ORTOOLS` and `USE_XPRESS`, plus a licensed Xpress installation on the machine, located through the `XPRESSDIR` environment variable (OR-Tools loads `$XPRESSDIR/lib/libxprs.so` with `dlopen` at the first solve, falling back to `/opt/xpressmp`; `LD_LIBRARY_PATH` is not consulted). Any of these missing raises `ERRSolveurIndisponible`. The same binary serves machines with and without Xpress.

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
  "name": "PCSOLVER",
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
  "name": "PCSOLVER",
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
  "name": "PCSOLVER",
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

> **Note**: if only `SOLVERCH` is configured, `PCSOLVER` defaults to SIRIUS (5). This may be intentional when using Sirius for the economic stacking phase and a higher-performance solver for the main optimization.

### Advanced parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `SOLVPARA` | STRING | Solver-specific parameters passed through to the solver (e.g. Xpress options) |
| `PRTBCOST` | FLOAT | Deterministic perturbation of generator and load costs, lifting the degeneracy that makes alternate optima solver-dependent. `0` (default) disables it. Applies to the off-network phase only; see `docs/simulator/io_doc.md` for the formula and its limits. |
