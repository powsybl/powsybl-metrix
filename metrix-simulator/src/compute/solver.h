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