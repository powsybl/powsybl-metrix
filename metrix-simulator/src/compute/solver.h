#pragma once

#include <pne.h>

namespace compute
{
class Solver
{
public:
    void solve(PROBLEME_A_RESOUDRE* pne_problem);
    void solve(PROBLEME_SIMPLEXE* spx_problem);

    void free();

private:
    PROBLEME_SPX* problem_ = nullptr;
};
} // namespace compute