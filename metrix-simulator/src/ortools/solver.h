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

#include <map>
#include <memory>
#include <string>
#include <string_view>
#include <vector>

namespace ortools
{
class Solver : public compute::ISolver
{
public:
    explicit Solver(config::SolverChoice solver_choice, std::string_view specific_params);

    void solve(PROBLEME_A_RESOUDRE* pne_problem) final;
    void solve(PROBLEME_SIMPLEXE* spx_problem) final;
    void free() final { solver_.reset(); }

private:
    // First = linear solver choice ; second = mixed solver choice
    using SolverChoice = std::pair<operations_research::MPSolver::OptimizationProblemType,
                                   operations_research::MPSolver::OptimizationProblemType>;

    static const std::string solverName_;
    static const std::map<config::SolverChoice, SolverChoice> solver_choices_;

    template<class PROBLEM>
    static std::shared_ptr<operations_research::MPSolverParameters> makeParams(const PROBLEM& problem);

    template<class PROBLEM>
    static void updateProblem(PROBLEM& problem, const std::shared_ptr<operations_research::MPSolver>& solver);

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

    static void transferVariables(const std::shared_ptr<operations_research::MPSolver>& solver, const Columns& columns);

    static void transferRows(const std::shared_ptr<operations_research::MPSolver>& solver,
                             double const* rhs,
                             char const* sens,
                             int nbRow);

    static void transferMatrix(const std::shared_ptr<operations_research::MPSolver>& solver,
                               int const* indexRows,
                               int const* terms,
                               int const* indexCols,
                               double const* coeffs,
                               int nbRow);

    template<class PROBLEM>
    operations_research::MPSolver::OptimizationProblemType type() const;

    template<class PROBLEM>
    std::shared_ptr<operations_research::MPSolver> makeMPSolver()
    {
        auto problemType = type<PROBLEM>();
        checkSolverAvailability(problemType);
        auto solver = std::make_shared<operations_research::MPSolver>(solverName_, problemType);
        applySpecificParams(*solver);
        return solver;
    }

    void checkSolverAvailability(operations_research::MPSolver::OptimizationProblemType problemType) const;

    void applySpecificParams(operations_research::MPSolver& solver) const;

    std::shared_ptr<operations_research::MPSolver> toMPSolver(const PROBLEME_A_RESOUDRE& problem);
    std::shared_ptr<operations_research::MPSolver> toMPSolver(const PROBLEME_SIMPLEXE& problem);

    std::shared_ptr<operations_research::MPSolver> solver_;
    config::SolverChoice solver_choice_;
    std::string specific_params_;
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
                                                const std::shared_ptr<operations_research::MPSolver>& solver);
template<>
void Solver::updateProblem<PROBLEME_SIMPLEXE>(PROBLEME_SIMPLEXE& problem,
                                              const std::shared_ptr<operations_research::MPSolver>& solver);

template<>
operations_research::MPSolver::OptimizationProblemType Solver::type<PROBLEME_A_RESOUDRE>() const;
template<>
operations_research::MPSolver::OptimizationProblemType Solver::type<PROBLEME_SIMPLEXE>() const;

} // namespace ortools
