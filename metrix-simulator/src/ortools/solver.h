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

// OR-Tools tire glog transitivement, qui définit un macro `LOG` en collision
// avec celui de metrix::log. On inclut OR-Tools et on undef le macro ici afin
// que les consommateurs n'aient pas à le gérer eux-mêmes.
// IMPORTANT : ce header doit être inclus AVANT <metrix/log.h> dans tout .cpp.
#include <ortools/linear_solver/linear_solver.h>
#ifdef LOG
#  undef LOG
#endif

#include <metrix/log.h>

#include "compute/isolver.h"
#include "config/configuration.h"
#include "config/constants.h"
#include "pne.h"

#include <map>
#include <memory>
#include <string>
#include <vector>

namespace ortools
{
class Solver : public compute::ISolver
{
public:
    explicit Solver(config::Configuration::SolverChoice solver_choice, const std::string& specific_params);

    void solve(PROBLEME_A_RESOUDRE* pne_problem) final;
    void solve(PROBLEME_SIMPLEXE* spx_problem) final;
    void free() final { solver_.reset(); }

private:
    // First = linear solver choice ; second = mixed solver choice
    using SolverChoice = std::pair<operations_research::MPSolver::OptimizationProblemType,
                                   operations_research::MPSolver::OptimizationProblemType>;

private:
    static const std::string solverName_;
    static const std::map<config::Configuration::SolverChoice, SolverChoice> solver_choices_;

private:
    template<class PROBLEM>
    static std::shared_ptr<operations_research::MPSolverParameters> makeParams(const PROBLEM& problem);

    template<class PROBLEM>
    static void updateProblem(PROBLEM& problem, const std::shared_ptr<operations_research::MPSolver>& solver);

    static void transferVariables(const std::shared_ptr<operations_research::MPSolver>& solver,
                                  double const* bMin,
                                  double const* bMax,
                                  double const* costs,
                                  int nbVar,
                                  double const* xValues,
                                  int const* typeDeBorneDeLaVariable,
                                  int const* typeDeVariable = nullptr,
                                  bool useHint = false);

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

private:

    template<class PROBLEM>
    operations_research::MPSolver::OptimizationProblemType type() const;

    template<class PROBLEM>
    std::shared_ptr<operations_research::MPSolver> makeMPSolver()
    {
        auto problemType = type<PROBLEM>();
        checkSolverAvailability(problemType);
        auto solver = std::make_shared<operations_research::MPSolver>(solverName_, problemType);
        if (specific_params_.size() > 0 && !solver->SetSolverSpecificParametersAsString(specific_params_)) {
            static bool warned = false; // avoid repeating the warning on every (micro-iteration) solve
            if (!warned) {
                LOG_ALL(warning) << "SPECIFICSOLVERPARAMS rejected by the solver backend: '" << specific_params_
                                 << "'";
                warned = true;
            }
        }
        return solver;
    }

    void checkSolverAvailability(operations_research::MPSolver::OptimizationProblemType problemType) const;

    std::shared_ptr<operations_research::MPSolver> toMPSolver(const PROBLEME_A_RESOUDRE& problem);
    std::shared_ptr<operations_research::MPSolver> toMPSolver(const PROBLEME_SIMPLEXE& problem);

private:
    std::shared_ptr<operations_research::MPSolver> solver_;
    config::Configuration::SolverChoice solver_choice_;
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
