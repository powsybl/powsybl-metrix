//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#include "solver.h"

#include "err/IoDico.h"
#include "err/error.h"
#include "factory.h"
#include <ortools/init/init.h>

#include <cmath>
#include <map>
#include <mutex>
#include <sstream>

using namespace operations_research;

namespace ortools
{
namespace
{
const std::string modelName = "simple_lp_program";

// Nom du fichier MPS ecrit par Sirius (PNE_EcrireJeuDeDonneesMPS, SPX_EcrireProblemeAuFormatMPS)
const std::string mpsFileName = "Donnees_Probleme_Solveur.mps";
} // namespace

Solver::Solver(config::SolverChoice solver_choice, std::string_view specific_params) :
    backend_(backend(solver_choice)),
    specific_params_(specific_params)
{
}

void Solver::initEnvironment()
{
    // Initialisation de la journalisation d'OR-Tools, requise une fois avant tout autre usage :
    // sans elle, absl ecrit tous ses messages sur stderr, INFO et WARNING compris (dont la
    // banniere Xpress, emise a chaque initialisation de l'environnement) ; une fois initialisee,
    // seuls ERROR et FATAL y restent.
    static std::once_flag loggingInitialized;
    std::call_once(loggingInitialized, [] { CppBridge::InitLogging("metrix-simulator"); });

    // Une instance OR-Tools Xpress interrompt le processus (CHECK) si l'environnement Xpress
    // ne s'initialise pas : la disponibilite est controlee avant d'en creer une.
    if (!MPSolver::SupportsProblemType(backend_.lp)) {
        throw ErrorI(err::ioDico().msg("ERRSolveurIndisponible", backend_.name));
    }
    environment_ = std::make_unique<MPSolver>(modelName, backend_.lp);
}

const Solver::Backend& Solver::backend(config::SolverChoice solver_choice)
{
    static const std::map<config::SolverChoice, Backend> backends = {
        {config::SolverChoice::XPRESS,
         {"XPRESS", MPSolver::XPRESS_LINEAR_PROGRAMMING, MPSolver::XPRESS_MIXED_INTEGER_PROGRAMMING}},
    };
    return backends.at(solver_choice);
}

std::unique_ptr<MPSolver> Solver::makeMPSolver(MPSolver::OptimizationProblemType type)
{
    if (!environment_) {
        initEnvironment();
    }
    auto solver = std::make_unique<MPSolver>(modelName, type);
    // OR-Tools applique les paires une par une et s'arrete a la premiere cle inconnue :
    // un rejet laisse donc le backend partiellement configure, sans moyen de savoir
    // jusqu'ou. On echoue plutot que de poursuivre sur une configuration incertaine.
    if (!specific_params_.empty() && !solver->SetSolverSpecificParametersAsString(specific_params_)) {
        throw ErrorI(err::ioDico().msg("ERRParamSolveurRefuse", specific_params_));
    }
    return solver;
}

void Solver::solve(PROBLEME_A_RESOUDRE* problem)
{
    const auto solver = makeMPSolver(backend_.mip);
    load(*solver,
         *problem,
         Columns{problem->NombreDeVariables,
                 problem->Xmin,
                 problem->Xmax,
                 problem->CoutLineaire,
                 problem->X,
                 problem->TypeDeBorneDeLaVariable,
                 problem->TypeDeVariable});
    if (problem->TempsDExecutionMaximum > 0) { // 0 : illimite
        solver->SetTimeLimit(absl::Seconds(problem->TempsDExecutionMaximum));
    }
    if (problem->SortirLesDonneesDuProbleme == OUI_PNE) {
        solver->Write(mpsFileName);
    }

    switch (solver->Solve(*makeParams(*problem))) {
        case MPSolver::OPTIMAL:
            problem->ExistenceDUneSolution = SOLUTION_OPTIMALE_TROUVEE;
            updateProblem(*problem, *solver);
            break;
        case MPSolver::FEASIBLE:
            problem->ExistenceDUneSolution = ARRET_PAR_LIMITE_DE_TEMPS_AVEC_SOLUTION_ADMISSIBLE_DISPONIBLE;
            updateProblem(*problem, *solver);
            break;
        case MPSolver::INFEASIBLE: problem->ExistenceDUneSolution = PROBLEME_INFAISABLE; break;
        case MPSolver::UNBOUNDED: problem->ExistenceDUneSolution = PROBLEME_NON_BORNE; break;
        default: {
            // ABNORMAL couvre a la fois une erreur du solveur et l'atteinte de la limite de temps
            // sans solution entiere, que Sirius restitue en PAS_DE_SOLUTION_TROUVEE
            const bool timeLimitReached = solver->DurationSinceConstruction() >= solver->TimeLimit();
            problem->ExistenceDUneSolution = timeLimitReached ? PAS_DE_SOLUTION_TROUVEE : ARRET_CAR_ERREUR_INTERNE;
        }
    }
}

void Solver::solve(PROBLEME_SIMPLEXE* problem)
{
    const auto solver = makeMPSolver(backend_.lp);
    // NB : pour un PROBLEME_SIMPLEXE, le champ TypeDeVariable contient en realite
    // les types de borne (cf. le renseignement de pb_ dans calculmacrofonctions.cpp).
    load(*solver,
         *problem,
         Columns{problem->NombreDeVariables,
                 problem->Xmin,
                 problem->Xmax,
                 problem->CoutLineaire,
                 problem->X,
                 problem->TypeDeVariable,
                 nullptr});

    switch (solver->Solve(*makeParams(*problem))) {
        case MPSolver::OPTIMAL:
            problem->ExistenceDUneSolution = OUI_SPX;
            updateProblem(*problem, *solver);
            break;
        case MPSolver::INFEASIBLE:
        case MPSolver::UNBOUNDED: problem->ExistenceDUneSolution = NON_SPX; break;
        default: problem->ExistenceDUneSolution = SPX_ERREUR_INTERNE; // ABNORMAL : erreur du solveur
    }
}

template<class PROBLEM>
void Solver::load(MPSolver& solver, const PROBLEM& problem, const Columns& columns)
{
    if (problem.AffichageDesTraces) {
        solver.EnableOutput();
    }
    transferVariables(solver, columns);
    transferRows(solver, problem.SecondMembre, problem.Sens, problem.NombreDeContraintes);
    transferMatrix(solver,
                   problem.IndicesDebutDeLigne,
                   problem.NombreDeTermesDesLignes,
                   problem.IndicesColonnes,
                   problem.CoefficientsDeLaMatriceDesContraintes,
                   problem.NombreDeContraintes);
}

void Solver::transferVariables(MPSolver& solver, const Columns& columns)
{
    MPObjective* const objective = solver.MutableObjective();
    for (int idxVar = 0; idxVar < columns.count; ++idxVar) {
        const std::string name = "x" + std::to_string(idxVar);

        double min_l = 0.;
        double max_l = 0.;
        switch (columns.boundType[idxVar]) {
            case VARIABLE_FIXE:
                if (columns.x != nullptr) {
                    min_l = columns.x[idxVar];
                    max_l = min_l;
                } else {
                    min_l = columns.min[idxVar];
                    max_l = min_l;
                }
                break;
            case VARIABLE_BORNEE_DES_DEUX_COTES:
                min_l = columns.min[idxVar];
                max_l = columns.max[idxVar];
                break;
            case VARIABLE_BORNEE_INFERIEUREMENT:
                min_l = columns.min[idxVar];
                max_l = operations_research::MPSolver::infinity();
                break;
            case VARIABLE_BORNEE_SUPERIEUREMENT:
                min_l = -operations_research::MPSolver::infinity();
                max_l = columns.max[idxVar];
                break;
            case VARIABLE_NON_BORNEE:
                min_l = -operations_research::MPSolver::infinity();
                max_l = operations_research::MPSolver::infinity();
                break;
            default: {
                std::ostringstream ss;
                ss << "Unknown TypeDeBorneDeLaVariable: " << columns.boundType[idxVar];
                throw ErrorI(ss.str());
            }
        }

        const operations_research::MPVariable* x = nullptr;
        if (columns.variableType != nullptr && columns.variableType[idxVar] == ENTIER) {
            x = solver.MakeIntVar(min_l, max_l, name);
        } else {
            x = solver.MakeNumVar(min_l, max_l, name);
        }
        if (columns.cost[idxVar] != 0.) {
            objective->SetCoefficient(x, columns.cost[idxVar]);
        }
    }
}

void Solver::transferRows(MPSolver& solver, double const* rhs, char const* sens, int nbRow)
{
    for (int idxRow = 0; idxRow < nbRow; ++idxRow) {
        double bMin = -MPSolver::infinity();
        double bMax = MPSolver::infinity();
        if (sens[idxRow] == '=') {
            bMin = rhs[idxRow];
            bMax = bMin;
        } else if (sens[idxRow] == '<') {
            bMax = rhs[idxRow];
        } else if (sens[idxRow] == '>') {
            bMin = rhs[idxRow];
        } else {
            // Un sens inconnu produirait silencieusement une contrainte libre
            // (-inf, +inf) : mieux vaut echouer explicitement.
            std::ostringstream ss;
            ss << "Unknown constraint sense '" << sens[idxRow] << "' at row " << idxRow;
            throw ErrorI(ss.str());
        }
        solver.MakeRowConstraint(bMin, bMax, "c" + std::to_string(idxRow));
    }
}

void Solver::transferMatrix(
    MPSolver& solver, int const* indexRows, int const* terms, int const* indexCols, double const* coeffs, int nbRow)
{
    const auto& variables = solver.variables();
    const auto& constraints = solver.constraints();

    for (int idxRow = 0; idxRow < nbRow; ++idxRow) {
        MPConstraint* const ct = constraints[idxRow];
        int debutLigne = indexRows[idxRow];
        for (int idxCoef = 0; idxCoef < terms[idxRow]; ++idxCoef) {
            int pos = debutLigne + idxCoef;
            ct->SetCoefficient(variables[indexCols[pos]], coeffs[pos]);
        }
    }
}

template<>
std::shared_ptr<operations_research::MPSolverParameters>
Solver::makeParams<PROBLEME_SIMPLEXE>(const PROBLEME_SIMPLEXE& problem)
{
    static_cast<void>(problem);
    auto params = std::make_shared<MPSolverParameters>();

    params->SetIntegerParam(MPSolverParameters::SCALING, MPSolverParameters::SCALING_ON);

    return params;
}

template<>
std::shared_ptr<operations_research::MPSolverParameters>
Solver::makeParams<PROBLEME_A_RESOUDRE>(const PROBLEME_A_RESOUDRE& problem)
{
    auto params = std::make_shared<MPSolverParameters>();
    auto presolve = (problem.FaireDuPresolve == NON_PNE) ? MPSolverParameters::PRESOLVE_OFF
                                                         : MPSolverParameters::PRESOLVE_ON;
    params->SetIntegerParam(MPSolverParameters::PRESOLVE, presolve);
    params->SetDoubleParam(MPSolverParameters::RELATIVE_MIP_GAP, problem.ToleranceDOptimalite / 100.0);
    params->SetIntegerParam(MPSolverParameters::SCALING, MPSolverParameters::SCALING_ON);

    return params;
}

template<>
void Solver::updateProblem<PROBLEME_A_RESOUDRE>(PROBLEME_A_RESOUDRE& problem, const MPSolver& solver)
{
    const auto& variables = solver.variables();
    int nbVar = problem.NombreDeVariables;

    // NOTE: constraint duals (VariablesDualesDesContraintes) are deliberately not
    // extracted here: MPSolver does not expose meaningful duals for MIP solves.
    // This is safe because metrix always re-solves a simplex with fixed integer
    // variables before reading any dual value (see the UTILISATION_PNE_SOLVEUR
    // handling at the beginning of metrix2assess). Keep that invariant in mind
    // if the result flow ever changes.

    // Extracting variable values
    for (int idxVar = 0; idxVar < nbVar; ++idxVar) {
        const auto& var = variables[idxVar];
        problem.X[idxVar] = var->solution_value();
    }
}

static int extractBasisStatus(const operations_research::MPVariable& var)
{
    // get the variable value
    double solutionValue = var.solution_value();
    // extract and return correct basis status based on bounds comparison
    MPSolver::BasisStatus ortoolsBasisStatus = var.basis_status();
    switch (ortoolsBasisStatus) {
        case MPSolver::FREE: {
            if (std::fabs(var.lb() - solutionValue) < config::constants::epsilon) {
                return HORS_BASE_SUR_BORNE_INF;
            } else if (std::fabs(var.ub() - solutionValue) < config::constants::epsilon) {
                return HORS_BASE_SUR_BORNE_SUP;
            }
            return HORS_BASE_A_ZERO;
        }
        case MPSolver::AT_LOWER_BOUND: return HORS_BASE_SUR_BORNE_INF;
        case MPSolver::AT_UPPER_BOUND: return HORS_BASE_SUR_BORNE_SUP;
        case MPSolver::FIXED_VALUE: return HORS_BASE_SUR_BORNE_INF;
        case MPSolver::BASIC: return EN_BASE;
        default: {
            std::ostringstream ss;
            ss << "Unknown ortoolsBasisStatus: " << ortoolsBasisStatus;
            throw ErrorI(ss.str());
        }
    }
}

static bool isSlackInBase(const operations_research::MPConstraint& cnt)
{
    // return true if the slack variable is in the base, false otherwise
    return cnt.basis_status() == MPSolver::BASIC;
}


template<>
void Solver::updateProblem<PROBLEME_SIMPLEXE>(PROBLEME_SIMPLEXE& problem, const MPSolver& solver)
{
    const auto& variables = solver.variables();
    int nbVar = problem.NombreDeVariables;

    // Extracting variable values and reduced costs
    for (int idxVar = 0; idxVar < nbVar; ++idxVar) {
        const auto& var = variables[idxVar];
        problem.X[idxVar] = var->solution_value();
        problem.CoutsReduits[idxVar] = var->reduced_cost();
        problem.PositionDeLaVariable[idxVar] = extractBasisStatus(*var);
    }

    const auto& constraints = solver.constraints();
    int nbRow = problem.NombreDeContraintes;
    problem.NbVarDeBaseComplementaires = 0;
    int idxCmpVar = 0;
    for (int idxRow = 0; idxRow < nbRow; ++idxRow) {
        const auto& row = constraints[idxRow];
        problem.CoutsMarginauxDesContraintes[idxRow] = row->dual_value();
        if (isSlackInBase(*row)) {
            problem.NbVarDeBaseComplementaires++;
            problem.ComplementDeLaBase[idxCmpVar] = idxRow;
            idxCmpVar++;
        }
    }
}

std::shared_ptr<compute::ISolver> makeSolver(config::SolverChoice solver_choice, std::string_view specific_params)
{
    return std::make_shared<Solver>(solver_choice, specific_params);
}
} // namespace ortools
