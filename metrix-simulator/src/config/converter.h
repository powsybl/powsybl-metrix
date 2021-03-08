#pragma once

/**
 * @file Header declaring convert functions
 *
 * These functions are used to convert raw data read from files into native types
 */

#include <string>

namespace config
{
/**
 * @brief Namespace containing some helper functions for configuration files reading
 */
namespace convert
{
/**
 * @brief Convert string ot integer
 *
 * @param str the string to convert
 *
 * @throw ERRFormatNombre message if string is invalid
 * @returns converted value
 */
int toInt(const std::string& str);

/**
 * @brief Convert string ot double
 *
 * @param str the string to convert
 *
 * @throw ERRFormatNombre message if string is invalid
 * @returns converted value
 */
double toDouble(const std::string& str);
} // namespace convert
} // namespace config