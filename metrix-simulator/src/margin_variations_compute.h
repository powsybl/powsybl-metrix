#pragma once

#include "pne.h"

#include <exception>
#include <sstream>
#include <vector>

/**
 * @brief Embed all elements required to build optimization problem for margin variations
 */
class MarginVariationMatrix
{
public:
    MarginVariationMatrix(int nbConstraints,
                          int nbComplementaryVar,
                          int nbVars,
                          const std::vector<int>& lineIndexes,
                          const std::vector<int>& columnIndexes,
                          const std::vector<int>& varPosition,
                          const std::vector<int>& nbTermesLine,
                          const std::vector<int>& numVarEnBaseDansB,
                          const std::vector<double>& constraintsMatrixCoeffs,
                          const std::vector<int>& baseComplement,
                          const std::vector<char>& sens);
    ~MarginVariationMatrix();

    MATRICE* matrix() const { return pmatrix_; }
    MATRICE_A_FACTORISER* matrixToFactor() { return &B_; }

    /**
     * @brief Special exceptions thrown in constructor
     */
    struct Exception : public std::exception {
        enum class Location {
            BASE_SIZE = 0, ///< Base size incompatible with number of constraints
            FACTORIZATION  ///< factorizaton failed
        };
        Exception(Location loc, int info) : location{loc}, info{info} {}

        const char* what() const noexcept final
        {
            std::stringstream ss;
            switch (location) {
                case Location::BASE_SIZE: ss << "Number of constraints is different from base size " << info; break;
                case Location::FACTORIZATION: ss << "Problem during base factorization: " << info; break;
                default:
                    // impossible case by definition of the enum
                    break;
            }
            return ss.str().c_str();
        }

        Location location;
        int info;
    };

private:
    void init(int nbConstraints,
              int nbComplementaryVar,
              int nbVars,
              const std::vector<int>& lineIndexes,
              const std::vector<int>& columnIndexes,
              const std::vector<int>& varPosition,
              const std::vector<int>& nbTermesLine,
              const std::vector<int>& numVarEnBaseDansB,
              const std::vector<double>& constraintsMatrixCoeffs,
              const std::vector<int>& baseComplement,
              const std::vector<char>& sens);

private:
    MATRICE_A_FACTORISER B_;
    MATRICE* pmatrix_;
    std::vector<int> BIndexDebutDesColonnes_;
    std::vector<int> BNbTermesDesColonnes_;
    std::vector<int> BIndicesDeLigne_;
    std::vector<double> BValeurDesTermesDeLaMatrice_;
};
