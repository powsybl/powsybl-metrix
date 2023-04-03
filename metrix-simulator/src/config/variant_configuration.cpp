//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#include "variant_configuration.h"

#include "configuration.h"
#include "converter.h"
#include "parametres.h"
#include <err/IoDico.h>
#include <err/error.h>
#include <metrix/log.h>

#include <cstring>
#include <fstream>
#include <sstream>

namespace config
{
auto VariantConfiguration::variante(int num) -> boost::optional<std::reference_wrapper<const VariantConfig>>
{
    if (config_.count(num) > 0) {
        return boost::make_optional<std::reference_wrapper<const VariantConfig>>(config_.at(num));
    }

    return {};
}

VariantConfiguration::VariantConfiguration(const std::string& pathname) :
    line_processors_{
        std::make_pair(
            "ECHANG",
            std::bind(
                &VariantConfiguration::processBalancesConsumption, this, std::placeholders::_1, std::placeholders::_2)),
        std::make_pair(
            "ECHANGP",
            std::bind(
                &VariantConfiguration::processBalancesProduction, this, std::placeholders::_1, std::placeholders::_2)),
        std::make_pair(
            "PRODIN",
            std::bind(&VariantConfiguration::processGroup, this, std::placeholders::_1, std::placeholders::_2)),
        std::make_pair(
            "CONELE",
            std::bind(&VariantConfiguration::processConso, this, std::placeholders::_1, std::placeholders::_2)),
        std::make_pair(
            "PRODIM",
            std::bind(&VariantConfiguration::processImposedGroup, this, std::placeholders::_1, std::placeholders::_2)),
        std::make_pair(
            "TRVALPMD",
            std::bind(&VariantConfiguration::processGroupPmax, this, std::placeholders::_1, std::placeholders::_2)),
        std::make_pair(
            "TRPUIMIN",
            std::bind(&VariantConfiguration::processGroupPmin, this, std::placeholders::_1, std::placeholders::_2)),
        std::make_pair("CTORDR",
                       std::bind(&VariantConfiguration::processCost,
                                 this,
                                 VariantConfig::CostType::UP_HR,
                                 std::placeholders::_1,
                                 std::placeholders::_2)),
        std::make_pair("COUBHR",
                       std::bind(&VariantConfiguration::processCost,
                                 this,
                                 VariantConfig::CostType::DOWN_HR,
                                 std::placeholders::_1,
                                 std::placeholders::_2)),
        std::make_pair("COUHAR",
                       std::bind(&VariantConfiguration::processCost,
                                 this,
                                 VariantConfig::CostType::UP_AR,
                                 std::placeholders::_1,
                                 std::placeholders::_2)),
        std::make_pair("COUBAR",
                       std::bind(&VariantConfiguration::processCost,
                                 this,
                                 VariantConfig::CostType::DOWN_AR,
                                 std::placeholders::_1,
                                 std::placeholders::_2)),
        std::make_pair(
            "COUEFF",
            std::bind(&VariantConfiguration::processCostConso, this, std::placeholders::_1, std::placeholders::_2)),
        std::make_pair(
            "QUADIN",
            std::bind(&VariantConfiguration::processLine, this, std::placeholders::_1, std::placeholders::_2)),
        std::make_pair(
            "DCMINPUI",
            std::bind(&VariantConfiguration::processHVDCPmin, this, std::placeholders::_1, std::placeholders::_2)),
        std::make_pair(
            "DCMAXPUI",
            std::bind(&VariantConfiguration::processHVDCPmax, this, std::placeholders::_1, std::placeholders::_2)),
        std::make_pair(
            "DCIMPPUI",
            std::bind(&VariantConfiguration::processHVDCPower, this, std::placeholders::_1, std::placeholders::_2)),
        std::make_pair("QATI00MN",
                       std::bind(&VariantConfiguration::processThreshold,
                                 this,
                                 VariantConfig::Threshold::MAX_N,
                                 std::placeholders::_1,
                                 std::placeholders::_2)), // seuil N
        std::make_pair("QATI5MNS",
                       std::bind(&VariantConfiguration::processThreshold,
                                 this,
                                 VariantConfig::Threshold::MAX_INC,
                                 std::placeholders::_1,
                                 std::placeholders::_2)), // seuil N-1
        std::make_pair("QATI20MN",
                       std::bind(&VariantConfiguration::processThreshold,
                                 this,
                                 VariantConfig::Threshold::MAX_INC_COMPLEX,
                                 std::placeholders::_1,
                                 std::placeholders::_2)), // seuil inc complexe
        std::make_pair("QATITAMN",
                       std::bind(&VariantConfiguration::processThreshold,
                                 this,
                                 VariantConfig::Threshold::MAX_BEFORE_CUR,
                                 std::placeholders::_1,
                                 std::placeholders::_2)), // seuil ITAM
        std::make_pair("QATITAMK",
                       std::bind(&VariantConfiguration::processThreshold,
                                 this,
                                 VariantConfig::Threshold::MAX_BEFORE_CUR_COMPLEX,
                                 std::placeholders::_1,
                                 std::placeholders::_2)), // seuil ITAM inc complexe
        std::make_pair("QATI00MN2",
                       std::bind(&VariantConfiguration::processThreshold,
                                 this,
                                 VariantConfig::Threshold::MAX_NEXOR,
                                 std::placeholders::_1,
                                 std::placeholders::_2)),
        std::make_pair("QATI5MNS2",
                       std::bind(&VariantConfiguration::processThreshold,
                                 this,
                                 VariantConfig::Threshold::MAX_INC_EXOR,
                                 std::placeholders::_1,
                                 std::placeholders::_2)),
        std::make_pair("QATI20MN2",
                       std::bind(&VariantConfiguration::processThreshold,
                                 this,
                                 VariantConfig::Threshold::MAX_INC_COMPLEX_EXOR,
                                 std::placeholders::_1,
                                 std::placeholders::_2)),
        std::make_pair("QATITAMN2",
                       std::bind(&VariantConfiguration::processThreshold,
                                 this,
                                 VariantConfig::Threshold::MAX_BEFORE_CUR_EXOR,
                                 std::placeholders::_1,
                                 std::placeholders::_2)),
        std::make_pair("QATITAMK2",
                       std::bind(&VariantConfiguration::processThreshold,
                                 this,
                                 VariantConfig::Threshold::MAX_BEFORE_CUR_COMPLEX_EXOR,
                                 std::placeholders::_1,
                                 std::placeholders::_2)),
        std::make_pair(
            "DTVALDEP",
            std::bind(&VariantConfiguration::processTDPhasing, this, std::placeholders::_1, std::placeholders::_2)),
        std::make_pair(
            "GROURAND",
            std::bind(&VariantConfiguration::processRandomGroups, this, std::placeholders::_1, std::placeholders::_2)),
        std::make_pair(
            "PROBABINC",
            std::bind(&VariantConfiguration::processProbaInc, this, std::placeholders::_1, std::placeholders::_2))}
{
    std::ifstream fic(pathname);
    if (!fic) {
        throw ErrorI(err::ioDico().msg("ERRPbOuvertureFic", pathname));
    }

    std::string line;
    std::string sub_line;
    std::istringstream iss;
    iss.exceptions(std::ifstream::eofbit | std::ifstream::badbit);

    try {
        // Number of tirages
        if (!getline(fic, line)) {
            throw ErrorI(err::ioDico().msg("ERRLectureFichier", pathname));
        }
        iss.str(line);
        getline(iss, sub_line, ';');
        if (sub_line != "NT") {
            throw ErrorI(err::ioDico().msg("ERRMotCleNbVar", pathname));
        }
        getline(iss, sub_line, ';');
        auto nb_tirages = convert::toInt(sub_line);

        LOG(debug) << metrix::log::verbose_config << "nb max tirages = " << nb_tirages;

        // parse lines
        while (!fic.eof()) {
            if (!getline(fic, line)) {
                if (!fic.eof()) {
                    throw ErrorI(err::ioDico().msg("ERRLectureFichier", pathname));
                }
                return;
            }

            // variant index
            iss.str(line);
            getline(iss, sub_line, ';');
            int numvar = convert::toInt(sub_line);

            auto& variant = config_[numvar]; // create new variant or continue the previous one
            variant.num = numvar;

            // key
            // loi vide de numero différent de -1, on la stocke dans la map
            if (iss.rdbuf()->in_avail() == 0) {
                continue;
            }
            std::string key;
            getline(iss, key, ';');
            if (key.empty()) {
                continue;
            }

            // number of variations by law
            getline(iss, sub_line, ';');
            auto nb_variations = convert::toInt(sub_line);
            if (nb_variations == 0) {
                continue;
            }

            if (line_processors_.count(key) == 0) {
                throw ErrorI(err::ioDico().msg("ERRTypeLoiInconnu", key));
            }
            auto processor = line_processors_.at(key);

            for (int j = 0; j < nb_variations; j++) {
                processor(variant, iss);
            }
        }
    } catch (const std::ios_base::failure& e) {
        LOG(error) << e.what();
        throw ErrorI(err::ioDico().msg("ERRLectFicPointVirgule", pathname));
    } catch (const err::Error&) {
        // do nothing more: propagate error
        throw;
    } catch (const std::exception& e) {
        LOG(error) << e.what();
        throw ErrorI(err::ioDico().msg("ERRLectureFichier", pathname)); // propagate error
    } catch (...) {
        LOG(error) << "Unknown error";
        throw ErrorI(err::ioDico().msg("ERRLectureFichier", pathname)); // propagate error
    }
}

void VariantConfiguration::processGroup(VariantConfig& variant, std::istringstream& iss) const
{
    std::string sub_line;
    getline(iss, sub_line, ';');
    rtrim(sub_line);

    variant.unavailableGroups.push_back(sub_line);

    LOG(debug) << metrix::log::verbose_config << "Variant " << variant.num << " : group " << sub_line << " unavailable";
}

std::tuple<std::string, double> VariantConfiguration::extractDouble(std::istringstream& iss)
{
    std::string sub_line;
    getline(iss, sub_line, ';');
    auto name = sub_line;
    rtrim(name);

    getline(iss, sub_line, ';');
    auto value = convert::toDouble(sub_line);

    return std::make_tuple(name, value);
}

std::tuple<std::string, int> VariantConfiguration::extractInt(std::istringstream& iss)
{
    std::string sub_line;
    getline(iss, sub_line, ';');
    auto name = sub_line;
    rtrim(name);

    getline(iss, sub_line, ';');
    auto value = convert::toInt(sub_line);

    return std::make_tuple(name, value);
}

void VariantConfiguration::processConso(VariantConfig& variant, std::istringstream& iss) const
{
    auto conso = extractDouble(iss);
    variant.consos.push_back(conso);

    LOG(debug) << metrix::log::verbose_config << "Variant " << variant.num << " : conso " << std::get<NAME>(conso)
               << " with value " << std::get<VALUE>(conso);
}

void VariantConfiguration::processRandomGroups(VariantConfig& variant, std::istringstream& iss) const
{
    std::string sub_line;
    getline(iss, sub_line, ';');
    rtrim(sub_line);
    variant.randomGroups.push_back(sub_line);

    LOG(debug) << metrix::log::verbose_config << "Variant " << variant.num << " : group " << sub_line << 
    "is in position " << variant.randomGroups.size()-1;
}

void VariantConfiguration::processImposedGroup(VariantConfig& variant, std::istringstream& iss) const
{
    auto group = extractDouble(iss);
    variant.groups.push_back(group);

    LOG(debug) << metrix::log::verbose_config << "Variant " << variant.num << " : group " << std::get<NAME>(group)
               << " is imposed at value " << std::get<VALUE>(group);
}

void VariantConfiguration::processGroupPmax(VariantConfig& variant, std::istringstream& iss) const
{
    auto group = extractDouble(iss);
    variant.pmaxGroups.push_back(group);

    LOG(debug) << metrix::log::verbose_config << "Variant " << variant.num << " : group " << std::get<NAME>(group)
               << " at Pmax value " << std::get<VALUE>(group);
}

void VariantConfiguration::processGroupPmin(VariantConfig& variant, std::istringstream& iss) const
{
    auto group = extractDouble(iss);
    variant.pminGroups.push_back(group);

    LOG(debug) << metrix::log::verbose_config << "Variant " << variant.num << " : group " << std::get<NAME>(group)
               << " at Pmin value " << std::get<VALUE>(group);
}

void VariantConfiguration::processLine(VariantConfig& variant, std::istringstream& iss) const
{
    std::string sub_line;
    getline(iss, sub_line, ';');
    rtrim(sub_line);

    variant.unavailableLines.push_back(sub_line);

    LOG(debug) << metrix::log::verbose_config << "Variant " << variant.num << " : line " << sub_line << " unavailable";
}

void VariantConfiguration::processHVDCPmax(VariantConfig& variant, std::istringstream& iss) const
{
    auto line = extractDouble(iss);
    variant.pmaxHvdc.push_back(line);

    LOG(debug) << metrix::log::verbose_config << "Variant " << variant.num << " : line " << std::get<NAME>(line)
               << " Pmax at " << std::get<VALUE>(line);
}

void VariantConfiguration::processHVDCPmin(VariantConfig& variant, std::istringstream& iss) const
{
    auto line = extractDouble(iss);
    variant.pminHvdc.push_back(line);

    LOG(debug) << metrix::log::verbose_config << "Variant " << variant.num << " : line " << std::get<NAME>(line)
               << " Pmin at " << std::get<VALUE>(line);
}

void VariantConfiguration::processHVDCPower(VariantConfig& variant, std::istringstream& iss) const
{
    auto line = extractDouble(iss);
    variant.powerHvdc.push_back(line);

    LOG(debug) << metrix::log::verbose_config << "Variant " << variant.num << " : line " << std::get<NAME>(line)
               << " power at " << std::get<VALUE>(line);
}

void VariantConfiguration::processTDPhasing(VariantConfig& variant, std::istringstream& iss) const
{
    auto td = extractInt(iss);
    variant.tdPhasing.push_back(td);

    LOG(debug) << metrix::log::verbose_config << "Variant " << variant.num << " : TD " << std::get<NAME>(td)
               << " phasing at " << std::get<VALUE>(td);
}

void VariantConfiguration::processCostConso(VariantConfig& variant, std::istringstream& iss) const
{
    auto conso = extractDouble(iss);
    variant.deleteConsosCosts.push_back(conso);

    LOG(debug) << metrix::log::verbose_config << "Variant " << variant.num << " : conso " << std::get<NAME>(conso)
               << " delete cost at " << std::get<VALUE>(conso);
}

void VariantConfiguration::processBalancesConsumption(VariantConfig& variant, std::istringstream& iss) const
{
    auto region = extractDouble(iss);
    variant.balancesConso.push_back(region);

    LOG(debug) << metrix::log::verbose_config << "Variant " << variant.num << " : region " << std::get<NAME>(region)
               << ", balance objective by consumption value at" << std::get<VALUE>(region);
}

void VariantConfiguration::processBalancesProduction(VariantConfig& variant, std::istringstream& iss) const
{
    auto region = extractDouble(iss);
    variant.balancesProd.push_back(region);

    LOG(debug) << metrix::log::verbose_config << "Variant " << variant.num << " : region " << std::get<NAME>(region)
               << ", balance objective by production value at" << std::get<VALUE>(region);
}

void VariantConfiguration::processProbaInc(VariantConfig& variant, std::istringstream& iss) const
{
    auto incident = extractDouble(iss);
    variant.probas.push_back(incident);

    LOG(debug) << metrix::log::verbose_config << "Variant " << variant.num << " : incident " << std::get<NAME>(incident)
               << " probability at" << std::get<VALUE>(incident);
}

void VariantConfiguration::processCost(VariantConfig::CostType cost_type,
                                       VariantConfig& variant,
                                       std::istringstream& iss) const
{
    auto group = extractDouble(iss);
    variant.costs[cost_type].push_back(group);

    std::string cost_str;
    switch (cost_type) {
        case VariantConfig::CostType::UP_HR: cost_str = "UP HR"; break;
        case VariantConfig::CostType::DOWN_HR: cost_str = "DOWN HR"; break;
        case VariantConfig::CostType::UP_AR: cost_str = "UP AR"; break;
        case VariantConfig::CostType::DOWN_AR: cost_str = "DOWN AR"; break;
        default:
            // impossible case
            break;
    }

    LOG(debug) << metrix::log::verbose_config << "Variant " << variant.num << " : group " << std::get<NAME>(group)
               << " has cost " << cost_str << " at " << std::get<VALUE>(group);
}

void VariantConfiguration::processThreshold(VariantConfig::Threshold threshold,
                                            VariantConfig& variant,
                                            std::istringstream& iss) const
{
    auto quad = extractDouble(iss);
    variant.tresholds[threshold].push_back(quad);

    std::string str;
    switch (threshold) {
        case VariantConfig::Threshold::MAX_N: str = "MAX N"; break;
        case VariantConfig::Threshold::MAX_INC: str = "MAX INC"; break;
        case VariantConfig::Threshold::MAX_INC_COMPLEX: str = "MAX INC COMPLEX"; break;
        case VariantConfig::Threshold::MAX_BEFORE_CUR:
            str = "MAX BEFORE CUR";
            // If thresholds are defined, we force the check of the ITAM threshold
            config::configuration().useItam(true);
            break;
        case VariantConfig::Threshold::MAX_BEFORE_CUR_COMPLEX:
            str = "MAX BEFORE CUR COMPLEX";
            // If thresholds are defined, we force the check of the ITAM threshold
            config::configuration().useItam(true);
            break;
        case VariantConfig::Threshold::MAX_NEXOR: str = "MAX NExOR"; break;
        case VariantConfig::Threshold::MAX_INC_EXOR: str = "MAX INC NExOR"; break;
        case VariantConfig::Threshold::MAX_INC_COMPLEX_EXOR: str = "MAX INC COMPLEX NExOR"; break;
        case VariantConfig::Threshold::MAX_BEFORE_CUR_EXOR:
            str = "MAX BEFORE CUR ExOR";
            // If thresholds are defined, we force the check of the ITAM threshold
            config::configuration().useItam(true);
            break;
        case VariantConfig::Threshold::MAX_BEFORE_CUR_COMPLEX_EXOR:
            str = "MAX BEFORE CUR COMPLEX NExOR";
            // If thresholds are defined, we force the check of the ITAM threshold
            config::configuration().useItam(true);
            break;
        default:
            // impossible case
            break;
    }

    LOG(debug) << metrix::log::verbose_config << "Variant " << variant.num << " : Quadripol " << std::get<NAME>(quad)
               << " has threshold " << str << " at " << std::get<VALUE>(quad);
}

} // namespace config