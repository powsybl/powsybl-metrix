//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//


/***************************************************************************************

Modele      : OPF en Actif Seul con�u pour �tre int�gr� dans la logique statistique d'ASSESS
Auteur      : Yacine HASSAINE
Description :
COPYRIGHT RTE 2008

*****************************************************************************************/

#include "err/error.h"

#include <boost/filesystem.hpp>

#include <sstream>

namespace err
{
Error::Error(const std::string& msg, const std::string& file, int line) noexcept { formatMsg(msg, file, line); }

void Error::formatMsg(const std::string& msg, const std::string& file, int line)
{
    std::ostringstream oss;

    oss << msg;

    if (!file.empty() && line != -1) {
        boost::filesystem::path path(file);
        oss << " (file=" << path.filename().generic_string() << " ,line=" << line << ")";
    }

    msg_ = oss.str();
}

} // namespace err