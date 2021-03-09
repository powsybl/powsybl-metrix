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

#include <metrix/log.h>

#include <boost/optional.hpp>

#include <memory>
#include <string>

namespace config
{
/**
 * @brief Configuration coming from input arguments of the programm
 *
 * The singleton instance is contained in the source file. The singleton design pattern is not the same as the other
 * because the constructor has a lot of arguments and the other pattern is not adapted to it
 */
class InputConfiguration
{
public:
    enum class CheckConstraintLevel {
        NONE = 0,
        LOAD_FLOW,     ///< When adding a constraint, perform a load flow to check transit
        EVERY_INCIDENT ///< When adding a constraint, run every incident to check that we didn't forget a constraint
    };

    InputConfiguration() = default;
    InputConfiguration(const std::string& filepath_error,
                       const std::string& filepath_variant,
                       const std::string& filepath_results,
                       const std::string& filepath_parades,
                       unsigned int first_variant,
                       unsigned int nb_variant,
                       bool print_log,
                       const std::vector<metrix::log::Verbose>& enabled_verboses,
                       boost::optional<metrix::log::severity::level> level,
                       bool write_constraint_file,
                       bool print_constraints_matrix,
                       bool write_sensivity_file,
                       bool write_matrix_report,
                       CheckConstraintLevel check_constraint_level,
                       bool compare_load_flow_report,
                       bool ignore_incident_group_absent,
                       bool use_all_outputs,
                       bool export_mps_file);

    const std::string& filepathError() const { return filepathError_; }
    const std::string& filepathVariant() const { return filepathVariant_; }
    const std::string& filepathResults() const { return filepathResults_; }
    const std::string& filepathParades() const { return filepathParades_; }
    unsigned int firstVariant() const { return firstVariant_; }
    unsigned int nbVariant() const { return nbVariant_; }
    bool printLog() const { return printLog_; }
    const std::vector<metrix::log::Verbose>& verboses() const { return verboses_; };
    const boost::optional<metrix::log::severity::level>& logLevel() const { return logLevel_; }
    bool writeConstraintsFile() const { return writeConstraintsFile_; }
    bool printConstraintsMatrix() const { return printConstraintsMatrix_; }
    bool writeSensivityFile() const { return writeSensivityFile_; }
    bool writeMatrixReport() const { return writeMatrixReport_; }
    const CheckConstraintLevel& checkConstraintLevel() const { return checkConstraintLevel_; }
    bool compareLoadFlowReport() const { return compareLoadFlowReport_; }
    bool ignoreIncidentGroupAbsent() const { return ignoreIncidentGroupAbsent_; }
    bool useAllOutputs() const { return useAllOutputs_; }
    bool exportMPSFile() const { return exportMPSFile_; }

private:
    std::string filepathError_;
    std::string filepathVariant_;
    std::string filepathResults_;
    std::string filepathParades_;
    unsigned int firstVariant_ = 0;
    unsigned int nbVariant_ = 0;
    bool printLog_ = false;
    std::vector<metrix::log::Verbose> verboses_;
    boost::optional<metrix::log::severity::level> logLevel_;
    bool writeConstraintsFile_ = false;
    bool printConstraintsMatrix_ = false;
    bool writeSensivityFile_ = false;
    bool writeMatrixReport_ = false;
    CheckConstraintLevel checkConstraintLevel_ = CheckConstraintLevel::NONE;
    bool compareLoadFlowReport_ = false;
    bool ignoreIncidentGroupAbsent_ = false;
    bool useAllOutputs_ = false;
    bool exportMPSFile_ = false;
};

void configureInputConfiguration(InputConfiguration&& config);

/**
 * @brief Retrieves the instance of input configuration
 */
const InputConfiguration& inputConfiguration();

} // namespace config