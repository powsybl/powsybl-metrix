#pragma once

#include <pne.h>

namespace compute
{
class ISolver
{
public:
    virtual ~ISolver() = default;

    virtual void solve(PROBLEME_A_RESOUDRE* pne_problem) = 0;
    virtual void solve(PROBLEME_SIMPLEXE* spx_problem) = 0;

    virtual void free() = 0;
};
} // namespace compute