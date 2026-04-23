#include "solver.h"

#include "err/error.h"

#include <iostream>

using namespace operations_research;

namespace ortools
{
const std::string Solver::solverName_ = "simple_lp_program";

const std::map<config::Configuration::SolverChoice, Solver::SolverChoice> Solver::solver_choices_ = {
    std::make_pair(config::Configuration::SolverChoice::GLPK,
                   std::make_pair(operations_research::MPSolver::GLPK_LINEAR_PROGRAMMING,
                                  operations_research::MPSolver::GLPK_MIXED_INTEGER_PROGRAMMING)),
    std::make_pair(config::Configuration::SolverChoice::CBC,
                   std::make_pair(operations_research::MPSolver::CLP_LINEAR_PROGRAMMING,
                                  operations_research::MPSolver::CBC_MIXED_INTEGER_PROGRAMMING)),
    std::make_pair(config::Configuration::SolverChoice::SCIP_GLOP,
                   std::make_pair(operations_research::MPSolver::GLOP_LINEAR_PROGRAMMING,
                                  operations_research::MPSolver::SCIP_MIXED_INTEGER_PROGRAMMING)),
    std::make_pair(config::Configuration::SolverChoice::GUROBI,
                   std::make_pair(operations_research::MPSolver::GUROBI_LINEAR_PROGRAMMING,
                                  operations_research::MPSolver::GUROBI_MIXED_INTEGER_PROGRAMMING)),
    std::make_pair(config::Configuration::SolverChoice::CPLEX,
                   std::make_pair(operations_research::MPSolver::CPLEX_LINEAR_PROGRAMMING,
                                  operations_research::MPSolver::CPLEX_MIXED_INTEGER_PROGRAMMING)),
    std::make_pair(config::Configuration::SolverChoice::SIRIUS,
                   std::make_pair(operations_research::MPSolver::SIRIUS_LINEAR_PROGRAMMING,
                                  operations_research::MPSolver::SIRIUS_MIXED_INTEGER_PROGRAMMING)),
    std::make_pair(config::Configuration::SolverChoice::XPRESS,
                   std::make_pair(operations_research::MPSolver::XPRESS_LINEAR_PROGRAMMING,
                                  operations_research::MPSolver::XPRESS_MIXED_INTEGER_PROGRAMMING)),
};

Solver::Solver(config::Configuration::SolverChoice solver_choice, const std::string& specific_params) :
    solver_choice_(solver_choice),
    specific_params_(specific_params)
{
}

void Solver::solve(PROBLEME_A_RESOUDRE* problem)
{
    solver_ = toMPSolver(*problem);

    if (problem->AffichageDesTraces) {
        solver_->EnableOutput();
    }

    auto params = makeParams<PROBLEME_A_RESOUDRE>(*problem);
    auto status = solver_->Solve(*params);

    if (status == operations_research::MPSolver::ResultStatus::OPTIMAL) {
        problem->ExistenceDUneSolution = SOLUTION_OPTIMALE_TROUVEE;
        updateProblem(*problem, solver_);
    } else if (status == operations_research::MPSolver::ResultStatus::FEASIBLE) {
        problem->ExistenceDUneSolution = ARRET_PAR_LIMITE_DE_TEMPS_AVEC_SOLUTION_ADMISSIBLE_DISPONIBLE;
        updateProblem(*problem, solver_);
    } else if (status == operations_research::MPSolver::ResultStatus::INFEASIBLE) {
        problem->ExistenceDUneSolution = PROBLEME_INFAISABLE;
    } else {
        problem->ExistenceDUneSolution = PAS_DE_SOLUTION_TROUVEE;
    }
}

void Solver::solve(PROBLEME_SIMPLEXE* problem)
{
    solver_ = toMPSolver(*problem);

    if (problem->AffichageDesTraces) {
        solver_->EnableOutput();
    }

    auto params = makeParams<PROBLEME_SIMPLEXE>(*problem);
    auto status = solver_->Solve(*params);

    if (status == operations_research::MPSolver::ResultStatus::OPTIMAL
        || status == operations_research::MPSolver::ResultStatus::FEASIBLE) {
        problem->ExistenceDUneSolution = OUI_SPX;
        updateProblem(*problem, solver_);
    } else {
        problem->ExistenceDUneSolution = NON_SPX;
    }
}

static std::string convertTypeDeBorneDeLaVariableToParameterAsString(int nbVar, int* typeDeBorneDeLaVariable)
{
    std::stringstream ss;
    ss << "VAR_BOUNDS_TYPE";
    for (int idxVar = 0; idxVar < nbVar; ++idxVar) {
        ss << " " << typeDeBorneDeLaVariable[idxVar];
    }
    return ss.str();
}

std::shared_ptr<operations_research::MPSolver> Solver::toMPSolver(const PROBLEME_A_RESOUDRE& problem)
{
    auto solver = makeMPSolver<PROBLEME_A_RESOUDRE>();

    // Create the variables and set objective cost.
    transferVariables(solver,
                      problem.Xmin,
                      problem.Xmax,
                      problem.CoutLineaire,
                      problem.NombreDeVariables,
                      problem.X,
                      problem.TypeDeBorneDeLaVariable,
                      problem.TypeDeVariable);

    // Create constraints and set coefs
    transferRows(solver, problem.SecondMembre, problem.Sens, problem.NombreDeContraintes);
    transferMatrix(solver,
                   problem.IndicesDebutDeLigne,
                   problem.NombreDeTermesDesLignes,
                   problem.IndicesColonnes,
                   problem.CoefficientsDeLaMatriceDesContraintes,
                   problem.NombreDeContraintes);

    // set time limit
    // TempsDExecutionMaximum est en secondes, set_time_limit attend des millisecondes
    if (problem.TempsDExecutionMaximum > 0) {
        solver->set_time_limit(static_cast<int64_t>(problem.TempsDExecutionMaximum) * 1000);
    }

    if (solver_choice_ == config::Configuration::SolverChoice::SIRIUS) {
        // transfer bounds type
        solver->SetSolverSpecificParametersAsString(convertTypeDeBorneDeLaVariableToParameterAsString(
            problem.NombreDeVariables, problem.TypeDeBorneDeLaVariable));
    }

    return solver;
}

std::shared_ptr<operations_research::MPSolver> Solver::toMPSolver(const PROBLEME_SIMPLEXE& problem)
{
    auto solver = makeMPSolver<PROBLEME_SIMPLEXE>();

    // Create the variables and set objective cost.
    // transferVariables(solver, problem.Xmin, problem.Xmax, problem.CoutLineaire, problem.NombreDeVariables);
    transferVariables(solver,
                      problem.Xmin,
                      problem.Xmax,
                      problem.CoutLineaire,
                      problem.NombreDeVariables,
                      problem.X,
                      problem.TypeDeVariable);

    // Create constraints and set coefs
    transferRows(solver, problem.SecondMembre, problem.Sens, problem.NombreDeContraintes);
    transferMatrix(solver,
                   problem.IndicesDebutDeLigne,
                   problem.NombreDeTermesDesLignes,
                   problem.IndicesColonnes,
                   problem.CoefficientsDeLaMatriceDesContraintes,
                   problem.NombreDeContraintes);

    if (solver_choice_ == config::Configuration::SolverChoice::SIRIUS) {
        // transfer bounds type
        solver->SetSolverSpecificParametersAsString(
            convertTypeDeBorneDeLaVariableToParameterAsString(problem.NombreDeVariables, problem.TypeDeVariable));
    }

    return solver;
}

void Solver::transferVariables(const std::shared_ptr<operations_research::MPSolver>& solver,
                               double const* bMin,
                               double const* bMax,
                               double* costs,
                               int nbVar,
                               double* xValues,
                               int const* typeDeBorneDeLaVariable,
                               int const* typeDeVariable)
{
    // Store current X values to set solution hint
    std::vector<std::pair<const operations_research::MPVariable*, double>> hint(nbVar);

    MPObjective* const objective = solver->MutableObjective();
    for (int idxVar = 0; idxVar < nbVar; ++idxVar) {
        std::ostringstream oss;
        oss << "x" << idxVar;

        double min_l = 0., max_l = 0.;
        switch (typeDeBorneDeLaVariable[idxVar]) {
            case VARIABLE_FIXE:
                if (typeDeVariable == nullptr && nullptr != xValues) {
                    min_l = max_l = xValues[idxVar];
                } else {
                    min_l = bMin[idxVar];
                    max_l = bMin[idxVar];
                }
                break;
            case VARIABLE_BORNEE_DES_DEUX_COTES:
                min_l = bMin[idxVar];
                max_l = bMax[idxVar];
                break;
            case VARIABLE_BORNEE_INFERIEUREMENT:
                min_l = bMin[idxVar];
                max_l = operations_research::MPSolver::infinity();
                break;
            case VARIABLE_BORNEE_SUPERIEUREMENT:
                min_l = -operations_research::MPSolver::infinity();
                max_l = bMax[idxVar];
                break;
            case VARIABLE_NON_BORNEE:
                min_l = -operations_research::MPSolver::infinity();
                max_l = operations_research::MPSolver::infinity();
                break;
            default: {
                std::ostringstream ss;
                ss << "Unknown typeDeBorneDeLaVariable: " << typeDeBorneDeLaVariable[idxVar];
                ErrorI(ss.str());
            }
        }

        operations_research::MPVariable* x(nullptr);
        if (typeDeVariable != nullptr && typeDeVariable[idxVar] == ENTIER) {
            x = solver->MakeIntVar(min_l, max_l, oss.str());
            objective->SetCoefficient(x, costs[idxVar]);
        } else {
            x = solver->MakeNumVar(min_l, max_l, oss.str());
            objective->SetCoefficient(x, costs[idxVar]);
        }

        if (nullptr != xValues) {
            hint[idxVar] = std::make_pair(x, xValues[idxVar]);
        }
    }

    if (nullptr != xValues) {
        solver->SetHint(hint);
    }
}

void Solver::transferRows(const std::shared_ptr<operations_research::MPSolver>& solver,
                          double const* rhs,
                          char const* sens,
                          int nbRow)
{
    for (int idxRow = 0; idxRow < nbRow; ++idxRow) {
        double bMin = -MPSolver::infinity(), bMax = MPSolver::infinity();
        if (sens[idxRow] == '=') {
            bMin = bMax = rhs[idxRow];
        } else if (sens[idxRow] == '<') {
            bMax = rhs[idxRow];
        } else if (sens[idxRow] == '>') {
            bMin = rhs[idxRow];
        }
        std::ostringstream oss;
        oss << "c" << idxRow;
        solver->MakeRowConstraint(bMin, bMax, oss.str());
    }
}

void Solver::transferMatrix(const std::shared_ptr<operations_research::MPSolver>& solver,
                            int const* indexRows,
                            int const* terms,
                            int* indexCols,
                            double* coeffs,
                            int nbRow)
{
    auto variables = solver->variables();
    auto constraints = solver->constraints();

    for (int idxRow = 0; idxRow < nbRow; ++idxRow) {
        MPConstraint* const ct = constraints[idxRow];
        int debutLigne = indexRows[idxRow];
        for (int idxCoef = 0; idxCoef < terms[idxRow]; ++idxCoef) {
            int pos = debutLigne + idxCoef;
            ct->SetCoefficient(variables[indexCols[pos]], coeffs[pos]);
        }
    }
}

bool Solver::solve(const std::shared_ptr<operations_research::MPSolver>& solver, const MPSolverParameters& params)
{
    auto status = solver->Solve(params);

    return (status == MPSolver::OPTIMAL || status == MPSolver::FEASIBLE);
}

template<>
std::shared_ptr<operations_research::MPSolverParameters>
Solver::makeParams<PROBLEME_SIMPLEXE>(const PROBLEME_SIMPLEXE& problem)
{
    static_cast<void>(problem);
    auto params = std::make_shared<MPSolverParameters>();

    params->SetIntegerParam(MPSolverParameters::SCALING, MPSolverParameters::SCALING_ON);
    params->SetIntegerParam(MPSolverParameters::LP_ALGORITHM, MPSolverParameters::DUAL);
    params->SetIntegerParam(MPSolverParameters::PRESOLVE, MPSolverParameters::PRESOLVE_OFF);

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
    params->SetIntegerParam(MPSolverParameters::LP_ALGORITHM, MPSolverParameters::DUAL);

    return params;
}

template<>
void Solver::updateProblem<PROBLEME_A_RESOUDRE>(PROBLEME_A_RESOUDRE& problem,
                                                const std::shared_ptr<operations_research::MPSolver>& solver)
{
    auto& variables = solver->variables();
    int nbVar = problem.NombreDeVariables;

    // Extracting variable values and reduced costs
    for (int idxVar = 0; idxVar < nbVar; ++idxVar) {
        auto& var = variables[idxVar];
        problem.X[idxVar] = var->solution_value();
    }
}

static int extractBasisStatus(operations_research::MPVariable& var)
{
    // get the variable value
    double solutionValue = var.solution_value();
    // extract and return correct basis status based on bounds comparison
    MPSolver::BasisStatus ortoolsBasisStatus = var.basis_status();
    switch (ortoolsBasisStatus) {
        case MPSolver::FREE: {
            if (fabs(var.lb() - solutionValue) < config::constants::epsilon) {
                return HORS_BASE_SUR_BORNE_INF;
            } else if (fabs(var.ub() - solutionValue) < config::constants::epsilon) {
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

static bool isSlackInBase(operations_research::MPConstraint& cnt)
{
    // return true if the slack variable is in the base, false otherwise
    return cnt.basis_status() == MPSolver::BASIC;
}


template<>
void Solver::updateProblem<PROBLEME_SIMPLEXE>(PROBLEME_SIMPLEXE& problem,
                                              const std::shared_ptr<operations_research::MPSolver>& solver)
{
    auto& variables = solver->variables();
    int nbVar = problem.NombreDeVariables;

    // Extracting variable values and reduced costs
    for (int idxVar = 0; idxVar < nbVar; ++idxVar) {
        auto& var = variables[idxVar];
        problem.X[idxVar] = var->solution_value();
        problem.CoutsReduits[idxVar] = var->reduced_cost();
        problem.PositionDeLaVariable[idxVar] = extractBasisStatus(*var);
    }

    auto& constraints = solver->constraints();
    int nbRow = problem.NombreDeContraintes;
    problem.NbVarDeBaseComplementaires = 0;
    int idxCmpVar = 0;
    for (int idxRow = 0; idxRow < nbRow; ++idxRow) {
        auto& row = constraints[idxRow];
        problem.CoutsMarginauxDesContraintes[idxRow] = row->dual_value();
        if (isSlackInBase(*row)) {
            problem.NbVarDeBaseComplementaires++;
            problem.ComplementDeLaBase[idxCmpVar] = idxRow;
            idxCmpVar++;
        }
    }
}

template<>
operations_research::MPSolver::OptimizationProblemType Solver::type<PROBLEME_A_RESOUDRE>() const
{ return solver_choices_.at(solver_choice_).second; }

template<>
operations_research::MPSolver::OptimizationProblemType Solver::type<PROBLEME_SIMPLEXE>() const
{ return solver_choices_.at(solver_choice_).first; }

} // namespace ortools
