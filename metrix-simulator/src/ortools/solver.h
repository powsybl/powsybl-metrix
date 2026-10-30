//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#pragma once

// OR-Tools tire absl/log transitivement (via ortools/base/logging.h), qui
// définit un macro `LOG` en collision avec celui de <metrix/log.h>. Ce header
// et solver.cpp n'incluent donc aucun en-tête de log metrix (les messages
// passent par log_bridge.h), et le reste de metrix ne doit pas inclure ce
// header : il passe par factory.h.
#include "compute/isolver.h"
#include "config/constants.h"
#include "config/solver_choice.h"
#include "pne.h"
#include <ortools/linear_solver/linear_solver.h>

#include <memory>
#include <string>
#include <string_view>

namespace ortools
{
class Solver : public compute::ISolver
{
public:
    explicit Solver(config::SolverChoice solver_choice, std::string_view specific_params);

    void solve(PROBLEME_A_RESOUDRE* pne_problem) final;
    void solve(PROBLEME_SIMPLEXE* spx_problem) final;
    /// Nothing to free: the model only lives during solve()
    void free() final {}

private:
    /// OR-Tools backend of a metrix solver choice
    struct Backend {
        const char* name;
        operations_research::MPSolver::OptimizationProblemType lp;
        operations_research::MPSolver::OptimizationProblemType mip;
    };

    static const Backend& backend(config::SolverChoice solver_choice);

    template<class PROBLEM>
    static std::shared_ptr<operations_research::MPSolverParameters> makeParams(const PROBLEM& problem);

    template<class PROBLEM>
    static void updateProblem(PROBLEM& problem, const operations_research::MPSolver& solver);

    /// Column data of a Sirius problem, as the parallel arrays of PROBLEME_SIMPLEXE / PROBLEME_A_RESOUDRE
    struct Columns {
        int count;               ///< NombreDeVariables
        double const* min;       ///< Xmin
        double const* max;       ///< Xmax
        double const* cost;      ///< CoutLineaire
        double const* x;         ///< X, input value of the VARIABLE_FIXE columns (may be nullptr)
        int const* boundType;    ///< TypeDeBorneDeLaVariable (VARIABLE_FIXE, VARIABLE_BORNEE_...)
        int const* variableType; ///< TypeDeVariable (ENTIER / REEL), nullptr for a pure LP
    };

    static void transferVariables(operations_research::MPSolver& solver, const Columns& columns);

    static void transferRows(operations_research::MPSolver& solver, double const* rhs, char const* sens, int nbRow);

    static void transferMatrix(operations_research::MPSolver& solver,
                               int const* indexRows,
                               int const* terms,
                               int const* indexCols,
                               double const* coeffs,
                               int nbRow);

    /// Load the columns, rows and matrix of a Sirius problem into an empty MPSolver
    template<class PROBLEM>
    static void load(operations_research::MPSolver& solver, const PROBLEM& problem, const Columns& columns);

    /// Initialize the OR-Tools logging and the backend environment (first solve)
    void initEnvironment();

    /// New MPSolver of the backend, configured with the SOLVPARA parameters
    [[nodiscard]] std::unique_ptr<operations_research::MPSolver>
    makeMPSolver(operations_research::MPSolver::OptimizationProblemType type);

    const Backend& backend_;
    std::string specific_params_;

    /**
     * @brief Idle MPSolver keeping the backend environment initialized, created on the first solve
     *
     * A new MPSolver is built for each solve (MPSolver::Clear() does not reset
     * the coefficients cached by the OR-Tools Xpress interface). Each OR-Tools
     * Xpress instance initializes the Xpress environment and releases it on
     * destruction: without a living instance in between, every solve would
     * check the license again.
     */
    std::unique_ptr<operations_research::MPSolver> environment_;
};

// specializations for ortools
template<>
std::shared_ptr<operations_research::MPSolverParameters>
Solver::makeParams<PROBLEME_A_RESOUDRE>(const PROBLEME_A_RESOUDRE& problem);
template<>
std::shared_ptr<operations_research::MPSolverParameters>
Solver::makeParams<PROBLEME_SIMPLEXE>(const PROBLEME_SIMPLEXE& problem);

template<>
void Solver::updateProblem<PROBLEME_A_RESOUDRE>(PROBLEME_A_RESOUDRE& problem,
                                                const operations_research::MPSolver& solver);
template<>
void Solver::updateProblem<PROBLEME_SIMPLEXE>(PROBLEME_SIMPLEXE& problem, const operations_research::MPSolver& solver);

} // namespace ortools
