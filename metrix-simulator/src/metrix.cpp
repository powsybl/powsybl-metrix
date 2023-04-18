//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#include "calcul.h"
#include "config/configuration.h"
#include "config/input_configuration.h"
#include "config/parades_configuration.h"
#include "config/variant_configuration.h"
#include "config/version.h"
#include "config/version_def.h"
#include "cte.h"
#include "err/IoDico.h"
#include "err/error.h"
#include "options/options.h"
#include "parametres.h"
#include "reseau.h"
#include "status.h"
#include <metrix/log.h>

#include <algorithm>
#include <cstdio>
#include <cstdlib>
#include <ctime>
#include <iostream>
#include <string>
#include <vector>

using std::cerr;


using std::vector;

static constexpr config::VersionDef version_def(
    __DATE__, __TIME__, config::version::major_version, config::version::minor_version, config::version::patch_version);

static void print_help(const char* program_name, const options::Options& options, bool log)
{
    std::cerr << program_name << version_def.toString() << '\n' << options.desc() << std::endl;
    if (log) {
        LOG_ALL(critical) << program_name << version_def.toString() << '\n' << options.desc() << metrix::log::sync;
    }
}

int main(int argc, char* argv[])
{
    try {
        options::Options options;

        auto parsing_result = options.parse(argc, argv);
        if (!std::get<0>(parsing_result)) {
            print_help(argv[0], options, true);
            return EXIT_FAILURE;
        }
        if (std::get<0>(parsing_result) && std::get<1>(parsing_result) == options::Options::Request::HELP) {
            print_help(argv[0], options, false);
            return EXIT_SUCCESS;
        }
        // other cases is status OK and RUN requested

        // UTF-8 support
        setlocale(LC_ALL, "en_US.UTF-8");

        const char* dicoPath = ".";
        if (getenv("METRIX_ETC") != nullptr) {
            dicoPath = getenv("METRIX_ETC");
        }
        err::IoDico::configure(dicoPath);
        err::ioDico().add("METRIX");

        const auto& config = config::configuration();

        // input configuration must be updated after dico is initialized
        options.update_configuration();

        const auto& input_config = config::inputConfiguration();

        // configure metrix log
        // other than critical level logs are forbidden before this point, because the logger is not configured yet

        metrix::log::Logger::config.resultFilepath = input_config.filepathError();
        metrix::log::Logger::config.printLog = input_config.printLog();
        metrix::log::Logger::config.verboses = input_config.verboses();

        if (config.logLevel()) {
            metrix::log::Logger::config.loggerLevel = *config.logLevel();
        }
        // Configuration from input arguments has priority
        if (input_config.logLevel()) {
            metrix::log::Logger::config.loggerLevel = *input_config.logLevel();
        }

        LOG(info) << "Run \"" << argv[0] << "\" " << version_def.toString();

        LOG(info) << "Arguments metrix: "
                  << err::ioDico().msg("INFOCommande",
                                       input_config.filepathError(),
                                       input_config.filepathVariant(),
                                       cte::c_fmt("%u", input_config.firstVariant()),
                                       cte::c_fmt("%u", input_config.nbVariant()));

        LOG_ALL(info) << "Use dictionnary file " << err::ioDico().filename();

        // Lecture des parametres
        //----------------------
        time_t start;
        time_t end;
        time_t lect;
        double dif;
        time(&start);

        int status = -1;
        Reseau res;

        try {
            // Lecture des donnees
            res.lireDonnees();
            time(&lect);

            config::VariantConfiguration variant_config(input_config.filepathVariant());
            
            // check first variante
            if (!variant_config.variante(input_config.firstVariant())) {
                throw ErrorI(err::ioDico().msg("ERRPremiereVarIntrouvable", input_config.filepathVariant()));
            }

            auto base = variant_config.variante(config::VariantConfiguration::variant_base);
            if (base) {
                // base variant is applied to network base without possibility to go back
                res.updateBase(base->get());
            } else {
                LOG(debug) << "No base change in variants";
            }

            // Double-check connexity in case the base variant broke connexite
            if (!res.connexite()) {
                LOG(warning) << "Base variant broke the connexity";
            }

            // Construction des variantes
            // To gain computation time, we gather variantes that generates the same topology, i.e. modifiy the same set
            // of quadripoles
            MapQuadinVar variantesOrdonnees;
            res.updateVariants(variantesOrdonnees, variant_config);
            time(&end);
            dif = difftime(end, lect);

            LOG(debug) << "Reading time for variantes: " << dif;

            if (config::configuration().computationType() != config::Configuration::ComputationType::LOAD_FLOW) {
                config::ParadesConfiguration parades_config(input_config.filepathParades());
                res.updateParades(parades_config);

                time(&lect);
                dif = difftime(lect, end);

                LOG(debug) << "Reading time for parades: " << dif;
            }

            if (config::configuration().useItam()) {
                // Ajout d'une parade "ne rien faire" sur les incidents avec curatif mais sans parade
                for (auto icdtIt = res.incidents_.cbegin(); icdtIt != res.incidents_.end(); ++icdtIt) {
                    auto& inc = icdtIt->second;

                    if (!inc->listeElemCur_.empty() && inc->parades_.empty()) {
                        auto paradeNRF = res.ajouteParadeNeRienFaire(inc);
                        // On echange les listes
                        inc->listeElemCur_.swap(paradeNRF->listeElemCur_);
                        inc->lccElemCur_.swap(paradeNRF->lccElemCur_);
                        inc->tdFictifsElemCur_.swap(paradeNRF->tdFictifsElemCur_);

                        if (!paradeNRF->tdFictifsElemCur_.empty()) {
                            // duplicate fictive curative corresponding to AC emulating on father incident
                            for (auto it = paradeNRF->tdFictifsElemCur_.cbegin();
                                 it != paradeNRF->tdFictifsElemCur_.end();
                                 ++it) {
                                auto elemCur = std::make_shared<ElementCuratifTD>(it->second->td_);
                                inc->listeElemCur_.push_back(elemCur);
                                inc->tdFictifsElemCur_.insert(
                                    std::pair<std::shared_ptr<Quadripole>, std::shared_ptr<ElementCuratifTD>>(it->first,
                                                                                                              elemCur));
                            }
                        }
                    }
                }
            }

            // Resolution du probleme
            Calculer comput(res, variantesOrdonnees);


            // Lancement du calcul
            status = comput.resolutionProbleme();

            if (status != METRIX_PAS_PROBLEME) {
                throw ErrorI(err::ioDico().msg("ERRResolProbleme"));
            }
        } catch (const err::Error& e) {
            LOG_ALL(error) << "Not recoverable error: " << e.what();
            // exceptions during computation are considered normal behaviour for the code (TNR)
        }

        time(&end);
        dif = difftime(end, start);

        LOG(info) << "Execution time: " << dif << metrix::log::sync;
    } catch (const std::exception& e) {
        std::cerr << e.what(); // add to cerr also because print on stdout is disabled by default and exception can
                               // occurs before logger initialization
        LOG_ALL(critical) << e.what() << metrix::log::sync;
        return METRIX_PAS_SOLUTION;
    } catch (...) {
        std::cerr << "Unknown error"; // add to cerr also because print on stdout is disabled by default and error can
                                      // occurs before logger initialization
        LOG_ALL(critical) << "Unknown error" << metrix::log::sync;
        return METRIX_PAS_SOLUTION;
    }

    return METRIX_PAS_PROBLEME;
}
