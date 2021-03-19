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

#include <fstream>
#include <map>

namespace err
{
/**
 * @brief Main dictionnary manager for display
 *
 * Each known error/warning/info string representation that is meant to be display to a user is contained in an exterior
 * file, allowing to be modified without recompiling the code.
 *
 * This file is then converted into a map. The value can have at most 5 arguments which are filled by the caller of the
 * error, depending of the error.
 *
 * This class implements the singleton pattern in order to be used anywhere in the code.
 */
class IoDico
{
public:
    /**
     * @brief Set the dirname of where to find the dictionnary files
     *
     * This method MUST be called, if necessary, before calling the first call to @a instance
     */
    static void configure(const std::string& path) { path_ = path; }

    /**
     * @brief Retrieve the single instance
     *
     * This will build the instance at the first call
     */
    static IoDico& instance();

public:
    /**
     * @brief Add a dictionnary name
     *
     * A file named @a filename will be searched in the dirname configured by the function @a configure
     */
    void add(const std::string& filename);

    /**
     * @brief Formats the message
     *
     * Search in the inner mapping the @a msgId key to retrieve the string representation of the error/warning/info.
     * Replace the corresponding arguments in the string by their value px
     */
    std::string msg(const char* msgId,
                    const std::string& p1 = "",
                    const std::string& p2 = "",
                    const std::string& p3 = "",
                    const std::string& p4 = "",
                    const std::string& p5 = "");

    const std::string& filename() const { return filename_; }

private:
    /**
     * @brief Private constructor, required for singleton pattern
     */
    IoDico();

    static std::string findFile(const std::string& file);
    void readFile(const char* filename);
    std::string findAndReadFile(const std::string& file);

private:
    static std::string path_;

    static bool processLine(std::ifstream& in, std::string& key, std::string& value);

private:
    std::map<std::string, std::string> map_;
    std::string filename_;
};

inline IoDico& ioDico() { return IoDico::instance(); }


} // namespace err