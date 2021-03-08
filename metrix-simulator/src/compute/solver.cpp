#include "solver.h"

#include <metrix/log.h>

namespace compute
{
void Solver::solve(PROBLEME_A_RESOUDRE* pne_problem) { PNE_Solveur(pne_problem); }

void Solver::solve(PROBLEME_SIMPLEXE* spx_problem)
{
    if (problem_ != nullptr) {
        LOG(warning) << "Solving a SIMPLEX problem without releasing the previous one: possible memory leak";
    }

    problem_ = SPX_Simplexe(spx_problem, nullptr);
}

void Solver::free()
{
    if (problem_ != nullptr) {
        SPX_LibererProbleme(problem_);
        problem_ = nullptr;
    }
}

} // namespace compute