#include "version_def.h"

#include <sstream>

namespace config
{
std::ostream& operator<<(std::ostream& os, const VersionDef& def)
{
    os << " v" << def.major << "." << def.minor << "." << def.minor_bis << " (build " << def.date << " at " << def.time
       << ")";
    return os;
}

std::string VersionDef::toString() const
{
    std::ostringstream ss;
    ss << *this;
    return ss.str();
}
} // namespace config