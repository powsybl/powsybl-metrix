#include "cte.h"

#include "err/error.h"

#include <cstdarg>
#include <cstring>

namespace cte
{
std::string c_fmt(const char* format, ...)
{
    const int max = 1000;
    char buf[max];
    va_list ap;
    va_start(ap, format);
    vsnprintf(buf, max, format, ap);
    if (strlen(buf) >= sizeof(buf)) {
        std::string msg = "pb de conversion en string c_fmt";
        throw ErrorI(msg);
    }

    return std::string(buf);
}
} // namespace cte