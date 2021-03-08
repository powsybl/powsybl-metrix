#pragma once

#include <exception>
#include <string>

/**
 * @brief Namespace managing the errors, exceptions and messages sent to the user of the programm
 */
namespace err
{
/**
 * @brief Main exception of metrix
 *
 * This class carries the filepath and line number in which it was thrown through the use of the @a ErrorI macro
 */
class Error : public std::exception
{
public:
    explicit Error(const std::string& msg, const std::string& file, int line) noexcept;

    const char* what() const noexcept final { return msg_.c_str(); }

private:
    void formatMsg(const std::string& msg, const std::string& file, int line);

private:
    std::string msg_;
};

} // namespace err

#define ErrorI(msg) err::Error(msg, __FILE__, __LINE__)
