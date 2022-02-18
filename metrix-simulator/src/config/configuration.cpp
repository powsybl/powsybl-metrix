//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#include "configuration.h"

#include "constants.h"
#include "err/IoDico.h"
#include "err/error.h"

#include <boost/property_tree/json_parser.hpp>
#include <boost/property_tree/ptree.hpp>

#include <iostream>
#include <sstream>
#include <type_traits>

namespace config
{
/**
 * @brief Private namespace for template helper functions to read configuration file
 */
namespace helper
{
template<typename T>
static std::vector<T> asVector(const boost::property_tree::ptree& pt, const boost::property_tree::ptree::key_type& key)
{
    std::vector<T> r;
    for (auto& item : pt.get_child(key)) {
        r.push_back(item.second.get_value<T>());
    }
    return r;
}

template<class T>
static void updateRaw(std::vector<T>& raw_values, const boost::property_tree::ptree& pt)
{
    auto values = helper::asVector<T>(pt, "values");
    raw_values.insert(raw_values.end(), values.begin(), values.end());
}

template<class T>
static inline std::vector<T> updateValue(const std::map<std::string, std::vector<T>>& raw_map, const std::string& key)
{
    // key not present is equivalent to empty vector of data
    return raw_map.count(key) > 0 ? raw_map.at(key) : std::vector<T>{};
}

template<class U, class T>
static inline T
updateValueNumber(const std::map<std::string, std::vector<U>>& raw_map, const std::string& key, const T& default_value)
{
    // default value is used only if key not present in set or present with no value
    return (raw_map.count(key) > 0 && !raw_map.at(key).empty()) ? static_cast<T>(raw_map.at(key).front())
                                                                : default_value;
}

template<class T>
static void checkRequiredKeyOnce(const std::map<std::string, std::vector<T>>& raw_map, const std::string& key)
{
    // A required key SHALL:
    // - be present
    // - have only one value
    if (!(raw_map.count(key) > 0 && raw_map.at(key).size() == 1)) {
        LOG_ALL(error) << err::ioDico().msg("ERRPbLectureParam", key);
        throw ErrorI(err::ioDico().msg("ERRPbLectureParametres"));
    }
}

template<class T>
static bool checkAtMostKeyOnce(const std::map<std::string, std::vector<T>>& raw_map, const std::string& key)
{
    if (raw_map.count(key) == 0) {
        // key is optional
        return false;
    }
    if (raw_map.at(key).size() != 1) {
        LOG_ALL(error) << err::ioDico().msg("ERRPbLectureParam", key);
        throw ErrorI(err::ioDico().msg("ERRPbLectureParametres"));
    }

    return true;
}

} // namespace helper

std::string Configuration::pathname_("fort.json");

void Configuration::configure(const std::string& pathname) { pathname_ = pathname; }

Configuration& Configuration::instance()
{
    static Configuration static_instance(pathname_);
    return static_instance;
}

void Configuration::checkConfiguration(const raw_configuration& raw_config)
{
    helper::checkRequiredKeyOnce(std::get<FLOAT>(raw_config), "CGCPERTE");   // coeff pertes
    helper::checkRequiredKeyOnce(std::get<INTEGER>(raw_config), "CGNBREGI"); // nunber of regions
    helper::checkRequiredKeyOnce(std::get<INTEGER>(raw_config), "TNNBNTOT"); // number of nodes
    helper::checkRequiredKeyOnce(std::get<INTEGER>(raw_config), "CQNBQUAD"); // number of quadripoles
    helper::checkRequiredKeyOnce(std::get<INTEGER>(raw_config), "ECNBCONS"); // number of consumptions
    helper::checkRequiredKeyOnce(std::get<INTEGER>(raw_config), "TRNBGROU"); // number of linked groups
    helper::checkRequiredKeyOnce(std::get<INTEGER>(raw_config), "TRNBTYPE"); // number of types of groups
    helper::checkRequiredKeyOnce(std::get<INTEGER>(raw_config), "DTNBTRDE"); // number of TDs
    helper::checkRequiredKeyOnce(std::get<INTEGER>(raw_config), "DMNBDEFK"); // number of N-k defaults
    helper::checkRequiredKeyOnce(std::get<INTEGER>(raw_config), "DCNBLIES"); // number of LCC
    // number of HVDC: check required is disabled for now because of use case where key is present without a value in
    // TNR

    // helper::checkRequiredKeyOnce(std::get<INTEGER>(raw_config), "DCNDROOP");

    //
    helper::checkRequiredKeyOnce(std::get<INTEGER>(raw_config), "SECTNBSE"); // number of watched sections
    helper::checkRequiredKeyOnce(std::get<INTEGER>(raw_config), "UNOMINAL"); // uref

    // Check validity log level, if present
    if (helper::checkAtMostKeyOnce(std::get<INTEGER>(raw_config), "LOGLEVEL")) {
        // Log level is present once
        // check log level value
        auto log_level = static_cast<unsigned int>(std::get<INTEGER>(raw_config).at("LOGLEVEL").front());
        if (log_level > metrix::log::severity::critical) {
            LOG_ALL(critical) << err::ioDico().msg("ERRPbLectureParam", "LOGLEVEL");
            throw ErrorI(err::ioDico().msg("ERRPbLectureParametres"));
        }
    }
}

auto Configuration::readRawConfiguration(const std::string& pathname) -> raw_configuration
{
    boost::property_tree::ptree tree;
    boost::property_tree::read_json(pathname, tree);

    raw_map<int> map_int;
    raw_map<float> map_float;
    raw_map<double> map_double;
    raw_map<std::string> map_string;
    raw_map<bool> map_bool;

    // assumed hierarchy for JSON files:
    //  "files": [
    //     "attributes": [
    //         {
    //             "name": ...
    //             "type": "INTEGER" or "FLOAT" or "DOUBLE" or "STRING" or "BOOLEAN"
    //             "values": [
    //                 ...
    //             ]
    //         }
    //     ]
    // ]

    for (auto& file : tree.get_child("files")) {
        for (auto& attribute : file.second.get_child("attributes")) {
            auto key = attribute.second.get_child("name").get_value<std::string>();
            auto type_str = attribute.second.get_child("type").get_value<std::string>();
            if (type_str == "INTEGER") {
                helper::updateRaw(map_int[key], attribute.second);
            } else if (type_str == "FLOAT") {
                helper::updateRaw(map_float[key], attribute.second);
            } else if (type_str == "DOUBLE") {
                helper::updateRaw(map_double[key], attribute.second);
            } else if (type_str == "STRING") {
                helper::updateRaw(map_string[key], attribute.second);
            } else if (type_str == "BOOLEAN") {
                helper::updateRaw(map_bool[key], attribute.second);
            } else {
                // unsupported
                LOG_ALL(critical) << "Type of configuration parameter " << type_str << " unsupported"
                                  << metrix::log::sync;
                std::exit(EXIT_FAILURE);
            }
        }
    }

    return std::make_tuple(map_int, map_float, map_double, map_string, map_bool);
}

void Configuration::initWithRawConfig(const raw_configuration& raw_config)
{
    cgnomregDIE_ = helper::updateValue(std::get<STRING>(raw_config), "CGNOMREG");
    cpposregDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "CPPOSREG");
    tnnomnoeDIE_ = helper::updateValue(std::get<STRING>(raw_config), "TNNOMNOE");
    tnneucelDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "TNNEUCEL");
    esafiactDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "ESAFIACT");
    trnomtypDIE_ = helper::updateValue(std::get<STRING>(raw_config), "TRNOMTYP");
    trnomgthDIE_ = helper::updateValue(std::get<STRING>(raw_config), "TRNOMGTH");
    tnneurgtDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "TNNEURGT");
    trtypgrpDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "TRTYPGRP");
    sppactgtDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "SPPACTGT");
    trpuiminDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "TRPUIMIN");
    trvalpmdDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "TRVALPMD");
    trdembanDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "TRDEMBAN");
    cqnomquaDIE_ = helper::updateValue(std::get<STRING>(raw_config), "CQNOMQUA");
    tnnorquaDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "TNNORQUA");
    tnnexquaDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "TNNEXQUA");
    qasurvdiDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "QASURVDI");
    qasurnmkDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "QASURNMK");
    cqadmitaDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "CQADMITA");
    dttrdequDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "DTTRDEQU");
    dtvalsupDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "DTVALSUP");
    dtvalinfDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "DTVALINF");
    dtlowtapDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "DTLOWTAP");
    dtnbtapsDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "DTNBTAPS");
    dttapdepDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "DTTAPDEP");
    dtlowranDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "DTLOWRAN");
    dtuppranDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "DTUPPRAN");
    dmptdefkDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "DMPTDEFK");
    dmnomdekDIE_ = helper::updateValue(std::get<STRING>(raw_config), "DMNOMDEK");
    dmdescrkDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "DMDESCRK");
    ptdefspeDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "PTDEFSPE");
    ptdefresDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "PTDEFRES");
    ptvarmarDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "PTVARMAR");
    tnvapalDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "TNVAPAL1");
    tnvacouDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "TNVACOU1");
    ldnbdefkDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "LDNBDEFK");
    ldptdefkDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "LDPTDEFK");
    ldcurperDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "LDCURPER");
    grnbdefkDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "GRNBDEFK");
    grptdefkDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "GRPTDEFK");
    dtmodregDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "DTMODREG");
    dtvaldepDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "DTVALDEP");
    dtnbdefkDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "DTNBDEFK");
    dtptdefkDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "DTPTDEFK");
    dcnomquaDIE_ = helper::updateValue(std::get<STRING>(raw_config), "DCNOMQUA");
    dcnorquaDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "DCNORQUA");
    dcnexquaDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "DCNEXQUA");
    dcminpuiDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "DCMINPUI");
    dcmaxpuiDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "DCMAXPUI");
    dcimppuiDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "DCIMPPUI");
    dcregpuiDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "DCREGPUI");
    dcdroopkDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "DCDROOPK");
    dcnbdefkDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "DCNBDEFK");
    dcptdefkDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "DCPTDEFK");
    dcperst1DIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "DCPERST1");
    dcperst2DIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "DCPERST2");
    dcresistDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "DCRESIST");
    dctensdcDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "DCTENSDC");
    cqresistDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "CQRESIST");
    spimpmodDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "SPIMPMOD");
    sectnomsDIE_ = helper::updateValue(std::get<STRING>(raw_config), "SECTNOMS");
    sectmaxnDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "SECTMAXN");
    sectnbqdDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "SECTNBQD");
    secttypeDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "SECTTYPE");
    sectnumqDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "SECTNUMQ");
    sectcoefDIE_ = helper::updateValue(std::get<FLOAT>(raw_config), "SECTCOEF");
    openbranDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "OPENBRAN");
    gbindnomDIE_ = helper::updateValue(std::get<STRING>(raw_config), "GBINDNOM");
    gbindrefDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "GBINDREF");
    gbinddefDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "GBINDDEF");
    lbindnomDIE_ = helper::updateValue(std::get<STRING>(raw_config), "LBINDNOM");
    lbinddefDIE_ = helper::updateValue(std::get<INTEGER>(raw_config), "LBINDDEF");
    nb_incidents_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "DMNBDEFK", 0U);
    nb_defres_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "NBDEFRES", 0U);
    nb_defspe_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "NBDEFSPE", 0U);
    nb_varmar_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "NBVARMAR", 0U);
    nb_groups_couplees_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "NBGBINDS", 0U);
    nb_conso_couplees_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "NBLBINDS", 0U);

    // general options
    nb_regions_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "CGNBREGI", 0U);
    nb_nodes_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "TNNBNTOT", 0U);
    nb_ac_quads_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "CQNBQUAD", 0U);
    nb_consos_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "ECNBCONS", 0U);
    nb_groups_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "TRNBGROU", 0U);
    nb_tds_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "DTNBTRDE", 0U);
    nb_dc_links_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "DCNBLIES", 0U);
    nb_watched_sections_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "SECTNBSE", 0U);
    nb_ac_emulated_dc_links_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "DCNDROOP", 0U);
    nb_curative_groups_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "GRNBCURA", 0U);
    nb_curative_charges_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "NBLDCURA", 0U);
    nb_opened_branchs_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "NBOPEBRA", 0U);
    nb_groups_types_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "TRNBTYPE", 0U);

    // computation options
    coeff_pertes_ = helper::updateValueNumber(std::get<FLOAT>(raw_config), "CGCPERTE", 0.0F);
    max_relance_pertes_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "RELPERTE", 0U);
    threshold_relance_pertes_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "SEUILPER", 500U);
    test_seuil_itam_ = helper::updateValueNumber(std::get<BOOLEAN>(raw_config), "TESTITAM", false);
    inc_rompant_connexite_ = helper::updateValueNumber(std::get<BOOLEAN>(raw_config), "INCNOCON", false);
    par_rompant_connexite_ = helper::updateValueNumber(std::get<BOOLEAN>(raw_config), "PARNOCON", false);
    parades_equivalentes_ = helper::updateValueNumber(std::get<BOOLEAN>(raw_config), "PAREQUIV", false);
    resultats_equilibrage_ = helper::updateValueNumber(std::get<BOOLEAN>(raw_config), "EQUILRES", false);
    resultats_redispatch_ = helper::updateValueNumber(std::get<BOOLEAN>(raw_config), "REDISRES", false);
    res_var_mar_ligne_ = helper::updateValueNumber(std::get<BOOLEAN>(raw_config), "VARMARES", false);
    res_var_mar_hvdc_ = helper::updateValueNumber(std::get<BOOLEAN>(raw_config), "LCCVMRES", false);
    res_pertes_detail_ = helper::updateValueNumber(std::get<BOOLEAN>(raw_config), "LOSSDETA", false);
    resultats_surcharges_ = helper::updateValueNumber(std::get<BOOLEAN>(raw_config), "OVRLDRES", false);
    showAllAngleTDTransitHVDC_ = helper::updateValueNumber(std::get<BOOLEAN>(raw_config), "SHTDHVDC", false);
    show_lost_load_detailed_ = helper::updateValueNumber(std::get<BOOLEAN>(raw_config), "LOSTLOAD", false);

    cost_td_ = helper::updateValueNumber(std::get<FLOAT>(raw_config), "TDPENALI", 1.e-2F);
    cost_hvdc_ = helper::updateValueNumber(std::get<FLOAT>(raw_config), "HVDCPENA", 0.1F);
    cost_failure_ = helper::updateValueNumber(std::get<FLOAT>(raw_config), "COUTDEFA", 13000.F);
    computation_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "MODECALC", ComputationType::OPF);
    u_ref_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "UNOMINAL", 0U);
    nb_max_number_micro_iterations_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "NBMAXMIT", 30U);
    nb_max_action_curative_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "NBMAXCUR", 0);
    nb_threats_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "NBTHREAT", 1U);
    time_max_pne_ = helper::updateValueNumber(
        std::get<INTEGER>(raw_config), "MAXSOLVE", 0U); // no maximum time by default
    proba_inc_ = helper::updateValueNumber(std::get<FLOAT>(raw_config), "PROBAINC", 1.e-3F);
    cost_valo_end_ = helper::updateValueNumber(std::get<FLOAT>(raw_config), "COUENDCU", 26000.F);
    cost_valo_ene_ = helper::updateValueNumber(std::get<FLOAT>(raw_config), "COUENECU", 100.F);
    limit_curative_grp_ = helper::updateValueNumber(
        std::get<INTEGER>(raw_config), "LIMCURGR", -1); // not used by default
    adequacy_cost_offset_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "ADEQUAOF", 0);
    redispatch_cost_offset_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "REDISPOF", 0);
    cost_ecart_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "COUTECAR", 10);
    noise_cost_ = helper::updateValueNumber(std::get<FLOAT>(raw_config), "NULLCOST", 0.5);

    lost_load_detailed_max_ = helper::updateValueNumber(std::get<INTEGER>(raw_config), "LOSTCMAX", 100U);

    // log level
    auto& map = std::get<INTEGER>(raw_config);
    if (map.count("LOGLEVEL") > 0 && !map.at("LOGLEVEL").empty()) {
        logLevel_ = boost::make_optional<metrix::log::severity::level>(
            static_cast<metrix::log::severity::level>(map.at("LOGLEVEL").front()));
    }
}

Configuration::Configuration(const std::string& pathname)
{
    try {
        auto raw_config = readRawConfiguration(pathname);
        checkConfiguration(raw_config);
        initWithRawConfig(raw_config);
    } catch (const err::Error&) {
        // propagate Error without doing anything else
        throw;
    } catch (const std::exception& e) {
        // other than iodico error (probably reading exception)
        LOG_ALL(error) << "Cannot read json configuration \"" << pathname << "\": " << e.what();
        throw ErrorI(err::ioDico().msg("ERRPbLectureParametres"));
    }
}

double Configuration::thresholdMaxITAM(double thresholdMaxInc, double thresholdMaxBeforeCur) const
{
    // In case threshold max before curative is defined but not threshold max inc,
    // we'll use threshold max before curative instead for threshold max inc

    if (!test_seuil_itam_ || thresholdMaxInc != constants::valdef) {
        return thresholdMaxInc;
    }

    // This implies that in case both equal valdef, we return valdef
    return thresholdMaxBeforeCur;
}

} // namespace config