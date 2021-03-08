#pragma once

#include <boost/log/attributes.hpp>
#include <boost/log/common.hpp>
#include <boost/log/expressions.hpp>
#include <boost/log/sinks.hpp>
#include <boost/log/sources/logger.hpp>

#include <boost/asio.hpp>

#include <boost/shared_ptr.hpp>

#include <chrono>
#include <fstream>
#include <iostream>
#include <map>
#include <memory>
#include <mutex>
#include <sstream>
#include <string>
#include <vector>

namespace metrix
{
/**
 * @brief Namespace for logging library
 */
namespace log
{
/**
 * @brief Namespace for definitions of log level
 */
namespace severity
{
/**
 * @brief definition of log level
 */
enum level {
    trace = 0, ///< level for traces, i.e. big logs to understand internal state (typically communications frames)
    debug,     ///< level for debug, i.e. logs to understand the sequence of the programm
    info,      ///< level for information, typically status
    warning, ///< level for warning, i.e. abnormal state / behaviour that does not threaten the normal behaviour of the
             ///< programm
    error, ///< level for errors, i.e. states / behaviour that should't happen. The programm may not work properly after
           ///< such a log
    critical ///< level for critical, i.e. non recoverable errors. The program stops with an error after this log.
             ///< SHALL always be the last value of the enum as it is used for log level validity
};

} // namespace severity

constexpr severity::level default_level = severity::info; ///< default log level

// Manipulators
struct Sync {
};

/**
 * constexpr variable allowing to use sync with the same interface as std::hex for the STL ostream. Put at the end of a
 * LOG statement, it forces the flushing of all sinks without waiting for the timer. Data sent to the log flow in the
 * same line after using sync is ignored.
 * @code{.cpp}
 * LOG(info) << "This is a log" << metrix::log::sync << "This part is ignored";
 * @endcode
 */
constexpr Sync sync;

/**
 * @brief Verbose definitions
 *
 * This enum defines a complementary set to configure a log. In case the log level is compliant with the logger level, a
 * log can have a verbose attribute which will add a filter according to the logger configuration
 * @code
 * LOG(info) << metrix::log::verbose_config << "This log will be displayed only if Verbose config is active in log
 * configuration";
 * @endcode
 */
enum class Verbose {
    CONFIG = 0,  ///< Log configuration
    CONSTRAINTS, ///< Detected constraints
};

// alias variables in order to comply with sync using style
constexpr Verbose verbose_config = Verbose::CONFIG;
constexpr Verbose verbose_constraints = Verbose::CONSTRAINTS;

/**
 * @brief Logger class
 *
 * This class implements a singleton in order to be used by everyone, relying on boost log singleton core to do so.
 *
 * It embedds a configuration definining the output filepath
 *
 * When a log is performed, it is stored in an internal state until a timer expired (hard-coded timeout) or until a new
 * log is performed. Then the whole stream is put in the corresponding logger.
 *
 * The logger supports:
 * - a simple file logger with no formatting (results)
 * - a logger with formatting (cout and dev file). Since the dev file can be very big in debug mode and below, a
 * rotating file, based on the name given in configuration is used. printing on stdout is optional and based on program
 * options. It relies on boost log library.
 *
 * The timer uses the pattern defined in Boost asio to run.
 *
 * The log macros are used the following way:
 * @code{.cpp}
 * LOG(info) << "This is a info log on dev file only: " << " you can use multiple flow in a single line";
 * LOG_ALL(info) << "This is a info log on result and log file";
 * LOG_RES() << "This is a log on the result file only: it is a info log";
 * @endcode
 *
 * @image html "log static diagram.png" "Static diagram"
 *
 * And this is the sequence of a log
 *
 * @image html "log sequence.png" "Sequence diagram of a log"
 */
class Logger
{
public:
    /**
     * @brief Logger configuration
     */
    struct LoggerConfiguration {
        LoggerConfiguration(const std::string& result,
                            const std::string& dev,
                            severity::level level,
                            bool print_log,
                            const std::vector<Verbose>& enabled_verbose) :
            resultFilepath{result},
            devFilepath{dev},
            loggerLevel{level},
            printLog{print_log},
            verboses{enabled_verbose}
        {
        }

        std::string resultFilepath;    ///< filepath for simple file logger
        std::string devFilepath;       ///< filepath for logger with formatter
        severity::level loggerLevel;   ///< Level of the logger
        bool printLog;                 ///< Print dev log on standard output
        std::vector<Verbose> verboses; ///< list of activated verboses
    };

    /**
     * @brief Target output definition
     */
    enum class TargetOutput {
        ALL = 0, ///< result file AND dev file
        RESULT,  ///< result file only
        DEV      ///< dev file only
    };

public:
    static LoggerConfiguration config; ///< configuration, MUST be set before the first call of @a instance

public:
    /**
     * @brief Retrieve the logger instance
     *
     * @returns the logger instance
     */
    static Logger& instance();

    /**
     * @brief Destructor
     *
     * Stops thread management
     */
    ~Logger();

public:
    /**
     * @brief Start a log input
     *
     * This function is meant to be used through the macros LOG, LOG_RES and LOG_ALL. Calling this function
     * initialize the internal state to start a log then the user can use the "<<" operator to stream its information.
     *
     * @param[in] lvl the log level of the log
     * @param[in] filepath the file path in which the log is performed
     * @param[in] line the line number of the log being performed
     * @param[in] target the target of the log
     *
     * @returns itself, in order to continue the log in one line
     */
    Logger& log(severity::level lvl, const std::string& filepath, int line, TargetOutput target);

    template<class T>
    friend Logger& operator<<(Logger& logger, const T& element);

private:
    using logger_src = boost::log::sources::severity_logger<severity::level>;
    using async_sink_stream = boost::log::sinks::asynchronous_sink<boost::log::sinks::text_ostream_backend>;
    using async_sink_file = boost::log::sinks::asynchronous_sink<boost::log::sinks::text_file_backend>;

    /**
     * @brief Structure handling the information of the current processing log
     */
    struct LogInfo {
        LogInfo(severity::level lvl, const std::string& filepath, int line_number, TargetOutput target_output) :
            level(lvl),
            filename(filepath),
            line(line_number),
            target{target_output}
        {
        }

        severity::level level;
        std::string filename;
        int line;
        TargetOutput target;
        std::stringstream stream;
    };

private:
    static const std::map<severity::level, std::string>
        severities_; ///< dictionnary to convert level into its string representation
    static const std::chrono::milliseconds synk_timeout; ///< timeout for synchronizer timer

private:
    static std::string computeDevfilePattern(const std::string& filepath);
    static bool checkLevel(severity::level level) { return level >= config.loggerLevel; }

private:
    bool check(const boost::log::attribute_value_set& set);

    /**
     * @brief Formatter of logger
     *
     * This function is given to boost logger to format the string message as followed:
     * [date] [level] file,line : message
     * where:
     * - date the ctime-format date
     * - level is the string representation of the log level
     * - file is the filename of the source file where the log is performed
     * - line is the line number in the source file where the log is performed
     * - message is the log message
     *
     * @param[in] view the boost log record view containing the attributes to format the log
     * @param[out] os the stream where to put the formatted message
     */
    void formatter(const boost::log::record_view& view, boost::log::formatting_ostream& os);
    void logImpl();
    void onTimerExpired(const boost::system::error_code& code);

    void logImplDev();
    void logImplResult();

    /**
     * @brief Constructor
     *
     * Private constructor to implement the singleton
     */
    Logger();

private:
    std::unique_ptr<LogInfo> logInfo_;

    // underlying loggers
    std::unique_ptr<logger_src> logger_;
    std::ofstream resultFileStream_;

    // thread management
    std::unique_ptr<std::thread> thread_;
    boost::asio::io_service context_;
    boost::asio::steady_timer timer_;
    mutable std::mutex mutex_;
};

template<class T>
Logger& operator<<(Logger& logger, const T& element)
{
    std::unique_lock<std::mutex> lock(logger.mutex_);
    if (logger.logInfo_) {
        logger.logInfo_->stream << element;
    }
    return logger;
}

template<>
Logger& operator<<(Logger& logger, const Sync& element);

template<>
Logger& operator<<(Logger& logger, const Verbose& element);

} // namespace log
} // namespace metrix

/**
 * @brief Performs a log in dev file
 *
 * @param[in] lvl log level name (without namespace)
 */
#define LOG(lvl)                         \
    metrix::log::Logger::instance().log( \
        metrix::log::severity::lvl, __FILE__, __LINE__, metrix::log::Logger::TargetOutput::DEV)

/**
 * @brief Performs a log in result file
 *
 * Performs a default level log in the result file only
 */
#define LOG_RES()                        \
    metrix::log::Logger::instance().log( \
        metrix::log::default_level, __FILE__, __LINE__, metrix::log::Logger::TargetOutput::RESULT)

/**
 * @brief Performs a log in dev file
 *
 * @param[in] lvl log level name (without namespace)
 */
#define LOG_ALL(lvl)                     \
    metrix::log::Logger::instance().log( \
        metrix::log::severity::lvl, __FILE__, __LINE__, metrix::log::Logger::TargetOutput::ALL)
