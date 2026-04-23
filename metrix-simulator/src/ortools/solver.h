#pragma once

#include "compute/isolver.h"
#include "config/configuration.h"
#include "config/constants.h"
#include "pne.h"
#include <ortools/linear_solver/linear_solver.h>

#include <map>
#include <memory>
#include <string>
#include <vector>

namespace ortools
{
namespace helper
{
template<class T>
inline std::vector<T> makeVectorFromPointer(T* array, size_t nb_elements)
{ return std::vector<T>(array, array + nb_elements); }
} // namespace helper

class Solver : public compute::ISolver
{
public:
    explicit Solver(config::Configuration::SolverChoice solver_choice, const std::string& specific_params);

    void solve(PROBLEME_A_RESOUDRE* pne_problem) final;
    void solve(PROBLEME_SIMPLEXE* spx_problem) final;
    void free() final {}

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
                                  double* costs,
                                  int nbVar,
                                  double* xValues,
                                  int const* typeDeBorneDeLaVariable,
                                  int const* typeDeVariable = nullptr);

    static void transferRows(const std::shared_ptr<operations_research::MPSolver>& solver,
                             double const* rhs,
                             char const* sens,
                             int nbRow);

    static void transferMatrix(const std::shared_ptr<operations_research::MPSolver>& solver,
                               int const* indexRows,
                               int const* terms,
                               int* indexCols,
                               double* coeffs,
                               int nbRow);

    static bool solve(const std::shared_ptr<operations_research::MPSolver>& solver,
                      const operations_research::MPSolverParameters& params);

private:
    template<class PROBLEM>
    operations_research::MPSolver::OptimizationProblemType type() const;

    template<class PROBLEM>
    std::shared_ptr<operations_research::MPSolver> makeMPSolver()
    {
        auto solver = std::make_shared<operations_research::MPSolver>(solverName_, type<PROBLEM>());

        if (solver_choice_ == config::Configuration::SolverChoice::XPRESS) {
            std::string xpress_params = "THREADS 1";
            if (!specific_params_.empty()) {
                xpress_params += " " + specific_params_;
            }
            solver->SetSolverSpecificParametersAsString(xpress_params);
        } else {
            (void)solver->SetNumThreads(1);
            if (!specific_params_.empty()) {
                solver->SetSolverSpecificParametersAsString(specific_params_);
            }
        }

        return solver;
    }

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
