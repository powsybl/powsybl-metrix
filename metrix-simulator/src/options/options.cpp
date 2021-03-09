//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#include "options.h"

#include "config/input_configuration.h"
#include "err/IoDico.h"
#include "err/error.h"
#include <metrix/log.h>

#include <boost/filesystem.hpp>

#include <cctype>
#include <cstdlib>

namespace po = boost::program_options;

namespace options
{
/**
 * @brief Structure encapsulate a log level type
 *
 * Encapsulate that way is required as boost program option overloaded validate function doesn't support directly enum
 * or enum classes. It is therefore required that we use a wrapper
 */
struct ParsedLogLevel {
    explicit ParsedLogLevel(metrix::log::severity::level lvl) : level{lvl} {}

    metrix::log::severity::level level;
};

/**
 * @brief Wrapper for check constraints level
 *
 * For the same reason as for @a ParsedLogLevel , we need a wrapper class for the overloade validate function
 */
struct ParsedCheckConstraintLevel {
    explicit ParsedCheckConstraintLevel(config::InputConfiguration::CheckConstraintLevel lvl) : level{lvl} {}

    config::InputConfiguration::CheckConstraintLevel level;
};

/**
 * @brief overload validate function
 *
 * Function to validate the case for log level
 *
 * see boost documentation for program options, "how to" chapter, "custom validator" section
 */
static void validate(boost::any& v, const std::vector<std::string>& values, ParsedLogLevel* unused_obj, int unused)
{
    static_cast<void>(unused_obj);
    static_cast<void>(unused);

    // Make sure no previous assignment to 'a' was made.
    po::validators::check_first_occurrence(v);

    // Extract the first string from 'values'. If there is more than
    // one string, it's an error, and exception will be thrown.
    const std::string& value = po::validators::get_single_string(values);

    // Check that the log level name is correct
    if (Options::severities_.count(value) == 0) {
        throw po::validation_error(po::validation_error::invalid_option_value);
    }

    v = boost::any(ParsedLogLevel(Options::severities_.at(value)));
}

/**
 * @brief Validate option for check level constraint
 *
 * Function to validate the case for check constraint value
 *
 * see boost documentation for program options, "how to" chapter, "custom validator" section
 */
static void
validate(boost::any& v, const std::vector<std::string>& values, ParsedCheckConstraintLevel* unused_obj, int unused)
{
    static_cast<void>(unused_obj);
    static_cast<void>(unused);

    // Make sure no previous assignment to 'a' was made.
    po::validators::check_first_occurrence(v);

    // Extract the first string from 'values'. If there is more than
    // one string, it's an error, and exception will be thrown.
    const std::string& value = po::validators::get_single_string(values);

    // Check the type of value
    if (value.length() != 1 || std::isdigit(value.front()) == 0) {
        throw po::validation_error(po::validation_error::invalid_option_value);
    }

    int val = std::atoi(value.c_str());

    switch (val) {
        case 0:
            v = boost::any(ParsedCheckConstraintLevel(config::InputConfiguration::CheckConstraintLevel::NONE));
            break;
        case 1:
            v = boost::any(ParsedCheckConstraintLevel(config::InputConfiguration::CheckConstraintLevel::LOAD_FLOW));
            break;
        case 2:
            v = boost::any(
                ParsedCheckConstraintLevel(config::InputConfiguration::CheckConstraintLevel::EVERY_INCIDENT));
            break;
        default: throw po::validation_error(po::validation_error::invalid_option_value);
    }
}

const std::map<std::string, metrix::log::severity::level> Options::severities_ = {
    {"trace", metrix::log::severity::trace},
    {"debug", metrix::log::severity::debug},
    {"info", metrix::log::severity::info},
    {"warning", metrix::log::severity::warning},
    {"error", metrix::log::severity::error},
    {"critical", metrix::log::severity::critical},
};

Options::Options()
{
    po::options_description hidden;
    desc_.add_options()("help,h", "Display help message")(
        "log-level",
        po::value<ParsedLogLevel>(), // cannot bind directly to config because of structure ParsedLogLevel that cannot
                                     // be defined inside a class
        "Logger level (allowed values are critical, error, warning, info, debug, trace): default is info")(
        "print-log,p",
        po::bool_switch(&config_.printLog)->default_value(false),
        "Print developer log in standard output")("verbose-config",
                                                  po::bool_switch(&config_.verboseConfig)->default_value(false),
                                                  "Activate debug/trace logs relative to configuration")(
        "verbose-constraints",
        po::bool_switch(&config_.verboseConstraints)->default_value(false),
        "Activate debug/trace logs relative to constraint detection")(
        "write-constraints",
        po::bool_switch(&config_.writeConstraintsFile)->default_value(false),
        "Write the constraints in a dedicated file")(
        "print-constraints",
        po::bool_switch(&config_.printConstraintsMatrix)->default_value(false),
        "Trace in logs the constraints matrix (time consuming even if trace logs are not active), log level at trace "
        "is required")("write-sensitivity",
                       po::bool_switch(&config_.writeSensivityFile)->default_value(false),
                       "Write the sensivity matrix in a dedicated file")(
        "write-report",
        po::bool_switch(&config_.writeMatrixReport)->default_value(false),
        "Write the rate matrix report in a dedicated file")(
        "check-constraints-level",
        po::value<ParsedCheckConstraintLevel>(),
        "Check adding constraints:\n"
        "0: no check (default)\n"
        "1: When adding a constraint, perform a load flow to check transit (more time consuming)\n"
        "2: When adding a constraint, run every incident to check that we didn't forget a "
        "constraint (even more time consuming")(
        "compare-reports",
        po::bool_switch(&config_.compareLoadFlowReport)->default_value(false),
        "Compare load flow reports after application of report factors to check trigger of coupling")(
        "no-incident-group",
        po::bool_switch(&config_.ignoreIncidentGroupAbsent)->default_value(false),
        "Ignore incident if a group of N-K is not available")(
        "all-outputs",
        po::bool_switch(&config_.allOutputs)->default_value(false),
        "Display all values in results files")(
        "mps-file", po::bool_switch(&config_.exportMPSFile)->default_value(false), "Export MPS file");

    // These options will not be displayed as program options but as arguments of the program (see display description)
    hidden.add_options()(
        "errorFilepath", po::value<std::string>(&config_.filepathError)->required(), "Error file pathname")(
        "variantFilepath", po::value<std::string>(&config_.filepathVariant)->required(), "Variant file pathname")(
        "resultsFilepath", po::value<std::string>(&config_.filepathResults)->required(), "Output file pathname")(
        "firstVariantIndex", po::value<int>(&config_.firstVariant)->required(), "Index first variant")(
        "numberVariants", po::value<int>(&config_.nbVariant)->required(), "Number of variants to process")(
        "paradesFilepath",
        po::value<std::string>(&config_.filepathParades)->default_value("parades.csv"),
        "Parades file pathname");

    auto test = hidden.find("paradesFilepath", true).description();

    allOptions_.add(desc_);
    allOptions_.add(hidden);

    // Order of the program's arguments is the order of declaration
    positional_.add("errorFilepath", 1);
    positional_.add("variantFilepath", 1);
    positional_.add("resultsFilepath", 1);
    positional_.add("firstVariantIndex", 1);
    positional_.add("numberVariants", 1);
    positional_.add("paradesFilepath", 1);
}

void Options::update_configuration() const
{
    if (config_.firstVariant < 0) {
        throw ErrorI(err::ioDico().msg("ERRFirstVariant"));
    }
    if (config_.nbVariant <= 0) {
        throw ErrorI(err::ioDico().msg("ERRNbVariants"));
    }

    std::vector<metrix::log::Verbose> verboses;
    if (config_.verboseConfig) {
        verboses.push_back(metrix::log::Verbose::CONFIG);
    }
    if (config_.verboseConstraints) {
        verboses.push_back(metrix::log::Verbose::CONSTRAINTS);
    }

    config::configureInputConfiguration(config::InputConfiguration(config_.filepathError,
                                                                   config_.filepathVariant,
                                                                   config_.filepathResults,
                                                                   config_.filepathParades,
                                                                   static_cast<unsigned int>(config_.firstVariant),
                                                                   static_cast<unsigned int>(config_.nbVariant),
                                                                   config_.printLog,
                                                                   verboses,
                                                                   config_.logLevel,
                                                                   config_.writeConstraintsFile,
                                                                   config_.printConstraintsMatrix,
                                                                   config_.writeSensivityFile,
                                                                   config_.writeMatrixReport,
                                                                   config_.checkConstraintLevel,
                                                                   config_.compareLoadFlowReport,
                                                                   config_.ignoreIncidentGroupAbsent,
                                                                   config_.allOutputs,
                                                                   config_.exportMPSFile));
}

auto Options::parse(int argc, char** argv) -> std::tuple<bool, Request>
{
    try {
        po::variables_map vm;
        po::store(po::command_line_parser(argc, argv).options(allOptions_).positional(positional_).run(), vm);

        config_.programName = std::string(argv[0]);
        // The rest of the fields of the temporary configuration are binded throught the mechanism of boost program
        // options when notifying

        if (vm.count("help") > 0) {
            return std::make_tuple(true, Request::HELP);
        }

        po::notify(vm);

        // These are not binded automatically
        if (vm.count("log-level") > 0) {
            config_.logLevel = vm["log-level"].as<ParsedLogLevel>().level;
        }
        if (vm.count("check-constraints-level") > 0) {
            config_.checkConstraintLevel = vm["check-constraints-level"].as<ParsedCheckConstraintLevel>().level;
        }

        return std::make_tuple(true, Request::RUN);
    } catch (const std::exception& e) {
        std::cerr << e.what() << std::endl; // Also dislay on error out because logger is not initialized
        LOG_ALL(error) << e.what();
        return std::make_tuple(false, Request::RUN); // Run is irrelevant here
    }
}

std::string Options::desc() const { return make_usage_string(basename(config_.programName), desc_, positional_); }

std::string Options::basename(const std::string& filepath)
{
    // we use boost filesystem here because filesystem in STl is available only in c++17
    boost::filesystem::path path(filepath);
    return path.filename().generic_string();
}

std::string Options::make_usage_string(const std::string& program_name,
                                       const po::options_description& desc,
                                       const po::positional_options_description& p)
{
    std::vector<std::string> parts;
    parts.emplace_back("Usage:\n");
    parts.push_back(program_name);
    for (unsigned int i = 0; i < p.max_total_count(); ++i) {
        parts.emplace_back("<" + p.name_for_position(i) + ">");
    }
    // special case for last input argument
    parts.push_back("\n<" + p.name_for_position(p.max_total_count() - 1) + "> = \"parades.csv\" by default");

    if (!desc.options().empty()) {
        parts.emplace_back("\n[options]");
    }
    std::ostringstream oss;
    std::copy(parts.begin(), parts.end(), std::ostream_iterator<std::string>(oss, " "));
    oss << std::endl << desc;
    return oss.str();
}

std::ostream& operator<<(std::ostream& os, const Options& opt)
{
    os << opt.desc();
    return os;
}

} // namespace options