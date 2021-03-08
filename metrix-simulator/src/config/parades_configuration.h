#pragma once

#include <set>
#include <string>
#include <vector>

namespace config
{
class ParadesConfiguration
{
public:
    struct ParadeDef {
        std::string incident_name;          ///< Incident for which the parade is allowed
        std::set<std::string> constraints;  ///< Quadripol name for constraints
        std::vector<std::string> couplings; ///< Contain the coupling in raw format
    };

public:
    explicit ParadesConfiguration(const std::string& pathname);

    const std::vector<ParadeDef>& parades() const { return parades_; }

private:
    std::vector<ParadeDef> parades_;
};
} // namespace config