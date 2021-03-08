#include "converter.h"

#include <err/IoDico.h>
#include <err/error.h>
#include <metrix/log.h>

namespace config
{
namespace convert
{
int toInt(const std::string& str)
{
    try {
        return std::stoi(str);
    } catch (const std::invalid_argument& e) {
        LOG(error) << e.what();
        throw ErrorI(err::ioDico().msg("ERRFormatNombre", str));
    }
}

double toDouble(const std::string& str)
{
    try {
        return std::stod(str);
    } catch (const std::invalid_argument& e) {
        LOG(error) << e.what();
        throw ErrorI(err::ioDico().msg("ERRFormatNombre", str));
    }
}
} // namespace convert
} // namespace config