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

#include <boost/optional.hpp>

#include <functional>
#include <map>
#include <sstream>
#include <string>
#include <tuple>
#include <vector>

namespace config
{
class VariantConfiguration
{
public:
    /// @brief Alias for indexes in int_value and double_value tuples
    enum { NAME = 0, VALUE };

    /**
     * @brief Definition of a variant according to the configuration file
     */
    struct VariantConfig {
        using double_value = std::tuple<std::string, double>;
        using int_value = std::tuple<std::string, int>;
        enum class CostType { UP_HR = 0, DOWN_HR, UP_AR, DOWN_AR };

        enum class Threshold {
            MAX_N = 0,
            MAX_INC,
            MAX_INC_COMPLEX,
            MAX_BEFORE_CUR,
            MAX_BEFORE_CUR_COMPLEX,
            MAX_NEXOR,
            MAX_INC_EXOR,
            MAX_INC_COMPLEX_EXOR,
            MAX_BEFORE_CUR_EXOR,
            MAX_BEFORE_CUR_COMPLEX_EXOR,
        };

        int num = variant_base;
        std::vector<double_value> consos;
        std::vector<double_value> deleteConsosCosts;

        std::vector<std::string> unavailableGroups;
        std::vector<double_value> groups;
        std::vector<double_value> pmaxGroups;
        std::vector<double_value> pminGroups;
        std::map<CostType, std::vector<double_value>> costs;

        std::vector<std::string> unavailableLines;
        std::vector<double_value> pmaxHvdc;
        std::vector<double_value> pminHvdc;
        std::vector<double_value> powerHvdc;
        std::vector<int_value> tdPhasing;
        std::map<Threshold, std::vector<double_value>> tresholds;

        std::vector<double_value> balancesConso;
        std::vector<double_value> balancesProd;

        std::vector<double_value> probas;

        //Stores the randomized list of groups returned by the G++9 compiler
        //Used for the testsof the CI
        std::vector<std::string> randomGroups;
    };
    using VariantMap = std::map<int, VariantConfig>;

public:
    static constexpr int variant_base = -1; ///< variant number to apply to all variant of the computation

public:
    /**
     * @brief Constructor
     *
     * @param pathname the pathname of the variant configuration file
     */
    explicit VariantConfiguration(const std::string& pathname);

    /**
     * @brief Retrieve variant information
     *
     * @param num variant number
     * @returns variant configuration, if variant number exists
     */
    boost::optional<std::reference_wrapper<const VariantConfig>> variante(int num);

    /**
     * @brief Retrieve all variants
     *
     * @returns map of variants
     */
    const VariantMap& variants() const { return config_; }

private:
    using Processor = std::function<void(VariantConfig&, std::istringstream&)>;


private:
    static std::tuple<std::string, double> extractDouble(std::istringstream& iss);
    static std::tuple<std::string, int> extractInt(std::istringstream& iss);

private:
    void processGroup(VariantConfig& variant, std::istringstream& iss) const;
    void processConso(VariantConfig& variant, std::istringstream& iss) const;
    void processCostConso(VariantConfig& variant, std::istringstream& iss) const;
    void processImposedGroup(VariantConfig& variant, std::istringstream& iss) const;
    void processGroupPmax(VariantConfig& variant, std::istringstream& iss) const;
    void processGroupPmin(VariantConfig& variant, std::istringstream& iss) const;

    void processCost(VariantConfig::CostType cost_type, VariantConfig& variant, std::istringstream& iss) const;

    void processLine(VariantConfig& variant, std::istringstream& iss) const;
    void processHVDCPmax(VariantConfig& variant, std::istringstream& iss) const;
    void processHVDCPmin(VariantConfig& variant, std::istringstream& iss) const;
    void processHVDCPower(VariantConfig& variant, std::istringstream& iss) const;
    void processTDPhasing(VariantConfig& variant, std::istringstream& iss) const;

    void processThreshold(VariantConfig::Threshold threshold, VariantConfig& variant, std::istringstream& iss) const;

    void processBalancesConsumption(VariantConfig& variant, std::istringstream& iss) const;
    void processBalancesProduction(VariantConfig& variant, std::istringstream& iss) const;
    void processProbaInc(VariantConfig& variant, std::istringstream& iss) const;

    void processRandomGroups(VariantConfig& variant, std::istringstream& iss) const;

private:
    const std::map<std::string, Processor>
        line_processors_; ///< map containing all functions to process each type of data read in the file
    VariantMap config_;   ///< map of variants
};
} // namespace config