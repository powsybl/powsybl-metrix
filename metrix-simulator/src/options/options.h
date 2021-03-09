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

#include "config/input_configuration.h"
#include <metrix/log.h>

#include <boost/optional.hpp>
#include <boost/program_options.hpp>

#include <map>
#include <ostream>
#include <string>
#include <tuple>

/**
 * @brief Namespace for input program options
 */
namespace options
{
/**
 * @brief This class manages the input programm options
 *
 * It relies on boost program options to describe all options allowed for the program
 */
class Options
{
public:
    /**
     * @brief Parsing request, in case of no error
     */
    enum class Request {
        RUN = 0, ///< Simulation run requested
        HELP,    ///< display help is requested
    };

public:
    Options();

    /**
     * @brief Parse input arguments
     *
     * Parse inputs and put it in temporary configuration
     *
     * @returns (true, type of request) if parsing was done, (false, irrelevant) if not
     */
    std::tuple<bool, Request> parse(int argc, char** argv);

    /**
     * @brief Update global input configuration with temporary
     *
     * Checks validity of configuration
     *
     * @throw IoDico exceptions if input configuration is invalid
     */
    void update_configuration() const;

    /**
     * @brief Format usage string displayed when "help" is requested on display
     */
    std::string desc() const;

public:
    static const std::map<std::string, metrix::log::severity::level> severities_;

private:
    /**
     * @brief Input configuration without check
     *
     * Without the compatibility checks performed after, this structure correspond to the parsed options of the
     * programs. It will be used to update the true input configuration
     */
    struct TemporaryInputConfiguration {
        std::string programName;
        std::string filepathError;
        std::string filepathVariant;
        std::string filepathResults;
        std::string filepathParades;
        int firstVariant = -1;
        int nbVariant = -1;
        bool printLog = false;
        bool verboseConfig = false;
        bool verboseConstraints = false;
        boost::optional<metrix::log::severity::level> logLevel;
        bool writeConstraintsFile = false;
        bool printConstraintsMatrix = false;
        bool writeSensivityFile = false;
        bool writeMatrixReport = false;
        config::InputConfiguration::CheckConstraintLevel checkConstraintLevel
            = config::InputConfiguration::CheckConstraintLevel::NONE;
        bool compareLoadFlowReport = false;
        bool ignoreIncidentGroupAbsent = false;
        bool allOutputs = false;
        bool exportMPSFile = false;
    };

private:
    static std::string make_usage_string(const std::string& program_name,
                                         const boost::program_options::options_description& desc,
                                         const boost::program_options::positional_options_description& p);
    static std::string basename(const std::string& filepath);

private:
    friend std::ostream& operator<<(std::ostream& os, const Options& opt);

private:
    boost::program_options::options_description allOptions_;
    boost::program_options::options_description desc_ = boost::program_options::options_description("Metrix options");
    boost::program_options::positional_options_description positional_;

    TemporaryInputConfiguration config_;
};

std::ostream& operator<<(std::ostream& os, const Options& opt);

} // namespace options