//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#include "input_configuration.h"

namespace config
{
/// @brief Single instance of the input configuration
static InputConfiguration config_;

void configureInputConfiguration(InputConfiguration&& config) { config_ = std::move(config); }

InputConfiguration::InputConfiguration(const std::string& filepath_error,
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
                                       bool write_PTDF_file,
                                       bool write_LODF_file,
                                       CheckConstraintLevel check_constraint_level,
                                       bool compare_load_flow_report,
                                       bool ignore_incident_group_absent,
                                       bool use_all_outputs,
                                       bool export_mps_file) :
    filepathError_{filepath_error},
    filepathVariant_{filepath_variant},
    filepathResults_{filepath_results},
    filepathParades_{filepath_parades},
    firstVariant_{first_variant},
    nbVariant_{nb_variant},
    printLog_{print_log},
    verboses_{enabled_verboses},
    logLevel_{level},
    writeConstraintsFile_{write_constraint_file},
    printConstraintsMatrix_{print_constraints_matrix},
    writePTDFfile_{write_PTDF_file},
    writeLODFfile_{write_LODF_file},
    checkConstraintLevel_{check_constraint_level},
    compareLoadFlowReport_{compare_load_flow_report},
    ignoreIncidentGroupAbsent_{ignore_incident_group_absent},
    useAllOutputs_{use_all_outputs},
    exportMPSFile_{export_mps_file}
{
}

const InputConfiguration& inputConfiguration() { return config_; }

} // namespace config