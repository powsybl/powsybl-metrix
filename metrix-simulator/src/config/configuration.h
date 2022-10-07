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

#include <map>
#include <memory>
#include <string>
#include <tuple>
#include <vector>

namespace config
{
/**
 * @brief Configuration
 *
 * This class carries all information present in the configuration file. It is implementing a singleton in order
 * to be accessed in the whole code.
 */
class Configuration
{
public:
    enum class ComputationType { OPF = 0, LOAD_FLOW, OPF_WITHOUT_REDISPATCH, OPF_WITH_OVERLOAD };
    // list got from ortools list: only solvers that have linear AND mixed have been extracted
    enum class SolverChoice {
        GLPK = 0,
        CBC,
        SCIP_GLOP,

        // Commercial software (need license).
        GUROBI,
        CPLEX,
        SIRIUS,
        XPRESS, // Must always be the last of the list
    };

public:
    /**
     * @brief Retrieve the configuration instance
     *
     * The configuration is built at the first call of this function.
     */
    static Configuration& instance();

    /**
     * @brief Configure the configuration filename
     *
     * it MUST be called, if necessary, before the first call of @a instance
     *
     * @param pathname the pathname of the configuration file
     */
    static void configure(const std::string& pathname);

public:
    boost::optional<metrix::log::severity::level> logLevel() const { return logLevel_; }
    bool useCurative() const { return nb_max_action_curative_ > 0; }
    bool usePenalisationTD() const { return cost_td_ > 0.; }
    bool usePenalisationHVDC() const { return cost_hvdc_ > 0.; }
    float costENE() const { return cost_valo_end_ * proba_inc_; }
    float costEND() const { return cost_valo_ene_ * proba_inc_; }

    // DIE
    const std::vector<std::string>& cgnomregDIE() const { return cgnomregDIE_; }
    const std::vector<int>& cpposregDIE() const { return cpposregDIE_; }
    const std::vector<std::string>& tnnomnoeDIE() const { return tnnomnoeDIE_; }
    const std::vector<int>& tnneucelDIE() const { return tnneucelDIE_; }
    const std::vector<float>& esafiactDIE() const { return esafiactDIE_; }
    const std::vector<std::string>& trnomtypDIE() const { return trnomtypDIE_; }
    const std::vector<std::string>& trnomgthDIE() const { return trnomgthDIE_; }
    const std::vector<int>& tnneurgtDIE() const { return tnneurgtDIE_; }
    const std::vector<int>& trtypgrpDIE() const { return trtypgrpDIE_; }
    const std::vector<float>& sppactgtDIE() const { return sppactgtDIE_; }
    const std::vector<float>& trpuiminDIE() const { return trpuiminDIE_; }
    const std::vector<float>& trvalpmdDIE() const { return trvalpmdDIE_; }
    const std::vector<float>& trdembanDIE() const { return trdembanDIE_; }
    const std::vector<std::string>& cqnomquaDIE() const { return cqnomquaDIE_; }
    const std::vector<int>& tnnorquaDIE() const { return tnnorquaDIE_; }
    const std::vector<int>& tnnexquaDIE() const { return tnnexquaDIE_; }
    const std::vector<int>& qasurvdiDIE() const { return qasurvdiDIE_; }
    const std::vector<int>& qasurnmkDIE() const { return qasurnmkDIE_; }
    const std::vector<float>& cqadmitaDIE() const { return cqadmitaDIE_; }
    const std::vector<int>& dttrdequDIE() const { return dttrdequDIE_; }
    const std::vector<float>& dtvalsupDIE() const { return dtvalsupDIE_; }
    const std::vector<float>& dtvalinfDIE() const { return dtvalinfDIE_; }
    const std::vector<int>& dtlowtapDIE() const { return dtlowtapDIE_; }
    const std::vector<int>& dtnbtapsDIE() const { return dtnbtapsDIE_; }
    const std::vector<float>& dttapdepDIE() const { return dttapdepDIE_; }
    const std::vector<int>& dtlowranDIE() const { return dtlowranDIE_; }
    const std::vector<int>& dtuppranDIE() const { return dtuppranDIE_; }
    const std::vector<int>& dmptdefkDIE() const { return dmptdefkDIE_; }
    const std::vector<std::string>& dmnomdekDIE() const { return dmnomdekDIE_; }
    const std::vector<int>& dmdescrkDIE() const { return dmdescrkDIE_; }
    const std::vector<int>& ptdefresDIE() const { return ptdefresDIE_; }
    const std::vector<int>& ptdefspeDIE() const { return ptdefspeDIE_; }
    const std::vector<int>& ptvarmarDIE() const { return ptvarmarDIE_; }
    const std::vector<int>& tnvapalDIE() const { return tnvapalDIE_; }
    const std::vector<float>& tnvacouDIE() const { return tnvacouDIE_; }
    const std::vector<int>& ldnbdefkDIE() const { return ldnbdefkDIE_; }
    const std::vector<int>& ldptdefkDIE() const { return ldptdefkDIE_; }
    const std::vector<int>& ldcurperDIE() const { return ldcurperDIE_; }
    const std::vector<int>& grnbdefkDIE() const { return grnbdefkDIE_; }
    const std::vector<int>& grptdefkDIE() const { return grptdefkDIE_; }
    const std::vector<int>& dtmodregDIE() const { return dtmodregDIE_; }
    const std::vector<float>& dtvaldepDIE() const { return dtvaldepDIE_; }
    const std::vector<int>& dtnbdefkDIE() const { return dtnbdefkDIE_; }
    const std::vector<int>& dtptdefkDIE() const { return dtptdefkDIE_; }
    const std::vector<std::string>& dcnomquaDIE() const { return dcnomquaDIE_; }
    const std::vector<int>& dcnorquaDIE() const { return dcnorquaDIE_; }
    const std::vector<int>& dcnexquaDIE() const { return dcnexquaDIE_; };
    const std::vector<float>& dcminpuiDIE() const { return dcminpuiDIE_; }
    const std::vector<float>& dcmaxpuiDIE() const { return dcmaxpuiDIE_; }
    const std::vector<float>& dcimppuiDIE() const { return dcimppuiDIE_; }
    const std::vector<int>& dcregpuiDIE() const { return dcregpuiDIE_; }
    const std::vector<float>& dcdroopkDIE() const { return dcdroopkDIE_; }
    const std::vector<int>& dcnbdefkDIE() const { return dcnbdefkDIE_; }
    const std::vector<int>& dcptdefkDIE() const { return dcptdefkDIE_; }
    const std::vector<float>& dcperst1DIE() const { return dcperst1DIE_; }
    const std::vector<float>& dcperst2DIE() const { return dcperst2DIE_; }
    const std::vector<float>& dcresistDIE() const { return dcresistDIE_; }
    const std::vector<float>& dctensdcDIE() const { return dctensdcDIE_; }
    const std::vector<float>& cqresistDIE() const { return cqresistDIE_; }
    const std::vector<int>& spimpmodDIE() const { return spimpmodDIE_; }
    const std::vector<std::string>& sectnomsDIE() const { return sectnomsDIE_; }
    const std::vector<float>& sectmaxnDIE() const { return sectmaxnDIE_; }
    const std::vector<int>& sectnbqdDIE() const { return sectnbqdDIE_; }
    const std::vector<int>& secttypeDIE() const { return secttypeDIE_; }
    const std::vector<int>& sectnumqDIE() const { return sectnumqDIE_; }
    const std::vector<float>& sectcoefDIE() const { return sectcoefDIE_; }
    const std::vector<int>& openbranDIE() const { return openbranDIE_; }
    const std::vector<std::string>& gbindnomDIE() const { return gbindnomDIE_; }
    const std::vector<int>& gbindrefDIE() const { return gbindrefDIE_; }
    const std::vector<int>& gbinddefDIE() const { return gbinddefDIE_; }
    const std::vector<std::string>& lbindnomDIE() const { return lbindnomDIE_; }
    const std::vector<int>& lbinddefDIE() const { return lbinddefDIE_; }

    unsigned int nbIncidents() const { return nb_incidents_; }
    unsigned int nbDefres() const { return nb_defres_; }
    unsigned int nbDefspe() const { return nb_defspe_; }
    unsigned int nbVarmar() const { return nb_varmar_; }
    unsigned int nbGroupsCouplees() const { return nb_groups_couplees_; }
    unsigned int nbConsoCouplees() const { return nb_conso_couplees_; }

    // general
    unsigned int nbRegions() const { return nb_regions_; }
    unsigned int nbNodes() const { return nb_nodes_; }
    unsigned int nbAcQuads() const { return nb_ac_quads_; }
    unsigned int nbConsos() const { return nb_consos_; }
    unsigned int nbGroups() const { return nb_groups_; }
    unsigned int nbTds() const { return nb_tds_; }
    unsigned int nbDcLinks() const { return nb_dc_links_; }
    unsigned int nbWatchedSections() const { return nb_watched_sections_; }
    unsigned int nbAcEmulatedDcLinks() const { return nb_ac_emulated_dc_links_; }
    unsigned int nbCurativeGroups() const { return nb_curative_groups_; }
    unsigned int nbCurativeCharges() const { return nb_curative_charges_; }
    unsigned int nbOpenedBranchs() const { return nb_opened_branchs_; }
    unsigned int nbGroupsTypes() const { return nb_groups_types_; }

    // computation options
    float coeffPertes() const { return coeff_pertes_; }
    unsigned int maxRelancePertes() const { return max_relance_pertes_; }
    unsigned int thresholdRelancePertes() const { return threshold_relance_pertes_; }
    bool useItam() const { return test_seuil_itam_; }
    bool useIncRompantConnexite() const { return inc_rompant_connexite_; }
    bool useParRompantConnexite() const { return par_rompant_connexite_; }
    bool useParadesEquivalentes() const { return parades_equivalentes_; }
    bool displayResultatsEquilibrage() const { return resultats_equilibrage_; }
    bool displayResultatsRedispatch() const { return resultats_redispatch_; }
    bool useResVarMarLigne() const { return res_var_mar_ligne_; }
    bool useResVarMarHvdc() const { return res_var_mar_hvdc_; }
    bool useResPertesDetail() const { return res_pertes_detail_; }
    bool displayResultatsSurcharges() const { return resultats_surcharges_; }
    bool showAllAngleTDTransitHVDC() const { return showAllAngleTDTransitHVDC_; }
    bool showLostLoadDetailed() const { return show_lost_load_detailed_; }

    float costTd() const { return cost_td_; }
    float costHvdc() const { return cost_hvdc_; }
    float costFailure() const { return cost_failure_; }
    ComputationType computationType() const { return computation_; }
    unsigned int uRef() const { return u_ref_; }
    unsigned int nbMaxNumberMicroIterations() const { return nb_max_number_micro_iterations_; }
    unsigned int nbMaxActionCurative() const { return nb_max_action_curative_; }
    unsigned int nbThreats() const { return nb_threats_; }
    unsigned int timeMaxPne() const { return time_max_pne_; }
    float probaInc() const { return proba_inc_; }
    float costValoEnd() const { return cost_valo_end_; }
    float costValoEne() const { return cost_valo_ene_; }
    int limitCurativeGrp() const { return limit_curative_grp_; }
    int adequacyCostOffset() const { return adequacy_cost_offset_; }
    int redispatchCostOffset() const { return redispatch_cost_offset_; }
    int costEcart() const { return cost_ecart_; }
    SolverChoice solverChoice() const { return solver_choice_; }
    SolverChoice pcSolverChoice() const { return pc_solver_choice_; }
    const std::string& specificSolverParams() const { return specific_solver_params_; }
    double noiseCost() const { return noise_cost_; }

    unsigned int lostLoadDetailedMax() const { return lost_load_detailed_max_; }

    // setters to force their value
    void useItam(bool value) { test_seuil_itam_ = value; }

    // complex getter
    double thresholdMaxITAM(double thresholdMaxInc, double thresholdMaxBeforeCur) const;

private:
    static std::string pathname_;

private:
    /**
     * @brief Constructor
     *
     * @param[in] pathname the pathname of the configuration file
     *
     * raw configuration from file is built then converted into an internal-friendly configuration
     */
    explicit Configuration(const std::string& pathname);

private:
    template<class T>
    using raw_map = std::map<std::string, std::vector<T>>;
    using raw_configuration
        = std::tuple<raw_map<int>, raw_map<float>, raw_map<double>, raw_map<std::string>, raw_map<bool>>;
    enum raw_configuration_index { INTEGER = 0, FLOAT, DOUBLE, STRING, BOOLEAN };

private:
    static raw_configuration readRawConfiguration(const std::string& pathname);
    static void checkConfiguration(const raw_configuration& raw_config);

private:
    void initWithRawConfig(const raw_configuration& raw_config);

private:
    // DIE configuration
    std::vector<std::string> cgnomregDIE_;
    std::vector<int> cpposregDIE_;
    std::vector<std::string> tnnomnoeDIE_;
    std::vector<int> tnneucelDIE_;
    std::vector<float> esafiactDIE_;
    std::vector<std::string> trnomtypDIE_;
    std::vector<std::string> trnomgthDIE_;
    std::vector<int> tnneurgtDIE_;
    std::vector<int> trtypgrpDIE_;
    std::vector<float> sppactgtDIE_;
    std::vector<float> trpuiminDIE_;
    std::vector<float> trvalpmdDIE_;
    std::vector<float> trdembanDIE_;
    std::vector<std::string> cqnomquaDIE_;
    std::vector<int> tnnorquaDIE_;
    std::vector<int> tnnexquaDIE_;
    std::vector<int> qasurvdiDIE_;
    std::vector<int> qasurnmkDIE_;
    std::vector<float> cqadmitaDIE_;
    std::vector<int> dttrdequDIE_;
    std::vector<float> dtvalsupDIE_;
    std::vector<float> dtvalinfDIE_;
    std::vector<int> dtlowtapDIE_;
    std::vector<int> dtnbtapsDIE_;
    std::vector<float> dttapdepDIE_;
    std::vector<int> dtlowranDIE_;
    std::vector<int> dtuppranDIE_;
    std::vector<int> dmptdefkDIE_;
    std::vector<std::string> dmnomdekDIE_;
    std::vector<int> dmdescrkDIE_;
    std::vector<int> ptdefresDIE_;
    std::vector<int> ptdefspeDIE_;
    std::vector<int> ptvarmarDIE_;
    std::vector<int> tnvapalDIE_;
    std::vector<float> tnvacouDIE_;
    std::vector<int> ldnbdefkDIE_;
    std::vector<int> ldptdefkDIE_;
    std::vector<int> ldcurperDIE_;
    std::vector<int> grnbdefkDIE_;
    std::vector<int> grptdefkDIE_;
    std::vector<int> dtmodregDIE_;
    std::vector<float> dtvaldepDIE_;
    std::vector<int> dtnbdefkDIE_;
    std::vector<int> dtptdefkDIE_;
    std::vector<std::string> dcnomquaDIE_;
    std::vector<int> dcnorquaDIE_;
    std::vector<int> dcnexquaDIE_;
    std::vector<float> dcminpuiDIE_;
    std::vector<float> dcmaxpuiDIE_;
    std::vector<float> dcimppuiDIE_;
    std::vector<int> dcregpuiDIE_;
    std::vector<float> dcdroopkDIE_;
    std::vector<int> dcnbdefkDIE_;
    std::vector<int> dcptdefkDIE_;
    std::vector<float> dcperst1DIE_;
    std::vector<float> dcperst2DIE_;
    std::vector<float> dcresistDIE_;
    std::vector<float> dctensdcDIE_;
    std::vector<float> cqresistDIE_;
    std::vector<int> spimpmodDIE_;
    std::vector<std::string> sectnomsDIE_;
    std::vector<float> sectmaxnDIE_;
    std::vector<int> sectnbqdDIE_;
    std::vector<int> secttypeDIE_;
    std::vector<int> sectnumqDIE_;
    std::vector<float> sectcoefDIE_;
    std::vector<int> openbranDIE_;
    std::vector<std::string> gbindnomDIE_;
    std::vector<int> gbindrefDIE_;
    std::vector<int> gbinddefDIE_;
    std::vector<std::string> lbindnomDIE_;
    std::vector<int> lbinddefDIE_;

    unsigned int nb_incidents_;
    unsigned int nb_defres_;
    unsigned int nb_defspe_;
    unsigned int nb_varmar_;
    unsigned int nb_groups_couplees_;
    unsigned int nb_conso_couplees_;

    unsigned int nb_regions_;
    unsigned int nb_nodes_;
    unsigned int nb_ac_quads_;
    unsigned int nb_consos_;
    unsigned int nb_groups_;
    unsigned int nb_tds_;
    unsigned int nb_dc_links_;
    unsigned int nb_watched_sections_;
    unsigned int nb_ac_emulated_dc_links_;
    unsigned int nb_curative_groups_;
    unsigned int nb_curative_charges_;
    unsigned int nb_opened_branchs_;
    unsigned int nb_groups_types_;

    // Options de calcul
    float coeff_pertes_;                    // coeff de pertes utilise par ASSESS : conso = conso (1+coeff_pertes_/100)
    unsigned int max_relance_pertes_;       // nb max de relance sur écart de pertes
    unsigned int threshold_relance_pertes_; // seuil de relance sur écart de pertes
    bool test_seuil_itam_;                  // vérification des contraintes sur seuil itam (avant curatif)
    bool inc_rompant_connexite_;            // prise en compte des incidents rompant la connexité
    bool par_rompant_connexite_;            // prise en compte des parades rompant la connexité sur un incident normal
    bool parades_equivalentes_;             // détection des variables équivalentes
    bool resultats_equilibrage_;            // sortie des résultats détaillés de la phase d'équilibrage initial
    bool resultats_redispatch_;             // sortie des résultats détaillés de la phase de redispatching
    bool res_var_mar_ligne_; // sortie des résultats sur les variations marginales sur les lignes (tableaux r4 et r4b)
    bool res_var_mar_hvdc_;  // sortie des résultats sur les variations marginales sur les lignes (tableau r6)
    bool res_pertes_detail_; // sortie des résultats sur les pertes par regions (tableau r9b)
    bool resultats_surcharges_;      // sortie uniquement des résultats sur les flux correspondants à une surcharge
    bool showAllAngleTDTransitHVDC_; // Display in all cases the angle for TDs and HVDCs even if they were not modified
    bool show_lost_load_detailed_;

    float cost_td_;                               // cost of TD actions
    float cost_hvdc_;                             // cost of HVDC action
    float cost_failure_;                          // cost of preventive action (END)
    ComputationType computation_;                 // computation type
    unsigned int u_ref_;                          // in kV
    unsigned int nb_max_number_micro_iterations_; // Nb max de micro-itération par variante (historiquement 17)
    unsigned int nb_max_action_curative_;         // can be negative to mean that no action is taken into account
    unsigned int nb_threats_;
    unsigned int time_max_pne_;
    float proba_inc_;
    float cost_valo_end_;
    float cost_valo_ene_;
    int limit_curative_grp_;
    int adequacy_cost_offset_;
    int redispatch_cost_offset_;
    int cost_ecart_;
    SolverChoice solver_choice_;
    SolverChoice pc_solver_choice_;
    std::string specific_solver_params_;
    double noise_cost_;

    unsigned int lost_load_detailed_max_;

    boost::optional<metrix::log::severity::level> logLevel_; ///< logger level
};

/**
 * @brief inline function to retrieve the configuration
 */
inline Configuration& configuration() { return Configuration::instance(); }

} // namespace config