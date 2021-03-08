#pragma once

#include <string>

/**
 * @brief Namespace containing c format fonctions
 */
namespace cte
{
/**
 * @brief Format a string in "printf" format into a std::string
 *
 * The formatted string must be at most 1000 characters.
 */
std::string c_fmt(const char* format, ...);
} // namespace cte