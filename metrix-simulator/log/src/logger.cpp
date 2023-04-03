//
// Copyright (c) 2021, RTE (http://www.rte-france.com)
// See AUTHORS.txt
// All rights reserved.
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, you can obtain one at http://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
//

#include <metrix/log/logger.h>

#include <boost/core/null_deleter.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/filesystem.hpp>

#include <algorithm>
#include <cstring>

using namespace boost::log;

namespace metrix
{
namespace log
{
/**
 * @brief Private namespace for helper functions for logging
 */
namespace helper
{
static inline std::string get_filename(const std::string& filepath)
{
    // we use boost filesystem here because filesystem in STl is available only in c++17
    boost::filesystem::path path(filepath);
    return path.filename().generic_string();
}
} // namespace helper

Logger::LoggerConfiguration Logger::config("metrixOut.txt", "metrix.log", default_level, false, {});

const std::map<severity::level, std::string> Logger::severities_ = {{severity::trace, "TRACE"},
                                                                    {severity::debug, "DEBUG"},
                                                                    {severity::info, "INFO"},
                                                                    {severity::warning, "WARNING"},
                                                                    {severity::error, "ERROR"},
                                                                    {severity::critical, "CRITICAL"}};

const std::chrono::milliseconds Logger::synk_timeout = std::chrono::milliseconds{100};

template<>
Logger& operator<<(Logger& logger, const Sync& element)
{
    static_cast<void>(element);
    std::unique_lock<std::mutex> lock(logger.mutex_);
    logger.logImpl();
    return logger;
}

template<>
Logger& operator<<(Logger& logger, const Verbose& element)
{
    if (std::find(Logger::config.verboses.begin(), Logger::config.verboses.end(), element)
        == Logger::config.verboses.end()) {
        std::unique_lock<std::mutex> lock(logger.mutex_);
        logger.logInfo_.reset();
    }

    return logger;
}

Logger::~Logger()
{
    if (!context_.stopped()) {
        context_.stop();
    }
    if (thread_ && !(thread_->get_id() == std::this_thread::get_id())) {
        thread_->join();
    }

    resultFileStream_.flush();
    core::get()->flush();
    core::get()->remove_all_sinks();
}

std::string Logger::computeDevfilePattern(const std::string& filepath)
{
    boost::filesystem::path path(filepath);
    const std::string rotating("%3N");

    if (path.has_extension()) {
        auto filename = path.filename().generic_string();
        size_t index = filename.find_last_of('.');
        if (index != std::string::npos) {
            filename = filename.substr(0, index);
        }
        return path.remove_filename().generic_string() + filename + rotating + path.extension().generic_string();
    }

    return std::string(path.generic_string()) + rotating;
}

Logger::Logger() : timer_(context_)
{
    // file synk
    auto file_pattern = computeDevfilePattern(config.devFilepath);
    auto backend = boost::make_shared<sinks::text_file_backend>(keywords::file_name = file_pattern,
                                                                keywords::auto_flush = true,
                                                                keywords::rotation_size = 5 * 1024 * 1024 // 5Mo
    );

    auto sink_file = boost::make_shared<async_sink_file>(backend);

    sink_file->set_filter(std::bind(&Logger::check, this, std::placeholders::_1));
    sink_file->set_formatter(std::bind(&Logger::formatter, this, std::placeholders::_1, std::placeholders::_2));

    core::get()->add_sink(sink_file);

    // stream synk
    if (config.printLog) {
        auto sink = boost::make_shared<async_sink_stream>();
        sink->locked_backend()->add_stream(boost::shared_ptr<std::ostream>(&std::clog, boost::null_deleter()));
        sink->locked_backend()->auto_flush(true);
        sink->set_filter(std::bind(&Logger::check, this, std::placeholders::_1));
        sink->set_formatter(std::bind(&Logger::formatter, this, std::placeholders::_1, std::placeholders::_2));
        core::get()->add_sink(sink);
    }

    logger_ = std::unique_ptr<logger_src>(new logger_src);

    thread_ = std::unique_ptr<std::thread>(new std::thread([this]() {
        timer_.expires_from_now(Logger::synk_timeout);
        timer_.async_wait(std::bind(&Logger::onTimerExpired, this, std::placeholders::_1));

        context_.run();
    }));
}

Logger& Logger::instance()
{
    static Logger logger_instance;
    return logger_instance;
}

bool Logger::check(const boost::log::attribute_value_set& set) const
{
    return checkLevel(set["Severity"].extract<severity::level>().get());
}

Logger& Logger::log(severity::level lvl, const std::string& filepath, int line, TargetOutput target)
{
    std::unique_lock<std::mutex> lock(mutex_);

    // flush previous log
    logImpl();

    if (!checkLevel(lvl)) {
        return *this;
    }

    logInfo_ = std::unique_ptr<LogInfo>(new LogInfo(lvl, helper::get_filename(filepath), line, target));

    timer_.expires_from_now(synk_timeout);

    return *this;
}

void Logger::logImplDev() const
{
    BOOST_LOG_SCOPED_LOGGER_ATTR(
        (*logger_),
        "Timestamp",
        boost::log::attributes::constant<std::chrono::system_clock::time_point>(std::chrono::system_clock::now()))
    BOOST_LOG_SCOPED_LOGGER_ATTR((*logger_), "File", boost::log::attributes::constant<std::string>(logInfo_->filename))
    BOOST_LOG_SCOPED_LOGGER_ATTR((*logger_), "Line", boost::log::attributes::constant<int>(logInfo_->line))
    BOOST_LOG_SEV((*logger_), logInfo_->level) << logInfo_->stream.str();
}

void Logger::logImplResult()
{
    resultFileStream_ << logInfo_->stream.str() << std::endl;
    resultFileStream_.flush();
}

void Logger::logImpl()
{
    if (!logInfo_) {
        return;
    }
    if (logInfo_->stream.str().empty()) {
        return;
    }

    switch (logInfo_->target) {
        case TargetOutput::DEV: logImplDev(); break;
        case TargetOutput::RESULT: logImplResult(); break;
        default: // ALL
            logImplDev();
            logImplResult();
            break;
    }

    logInfo_.reset();
}

void Logger::formatter(const record_view& view, formatting_ostream& os) const
{
    static constexpr size_t nb_char_time_formatted = 25; // format Www Mmm dd hh:mm:ss yyyy + EOL
    auto lvl = view.attribute_values()["Severity"].extract<severity::level>().get();

    auto time = std::chrono::system_clock::to_time_t(
        view.attribute_values()["Timestamp"].extract<std::chrono::system_clock::time_point>().get());
    std::string time_formatted;
    time_formatted.assign(nb_char_time_formatted, '\0');
    std::tm l_tm;
    localtime_r(&time, &l_tm);
    std::strftime(&time_formatted[0], nb_char_time_formatted, "%a %b %d %H:%M:%S %Y", &l_tm);

    os << "[" << time_formatted << "] [" << severities_.at(lvl) << "] "
       << view.attribute_values()["File"].extract<std::string>() << ",l"
       << view.attribute_values()["Line"].extract<int>() << " : "
       << view.attribute_values()["Message"].extract<std::string>();
}

void Logger::onTimerExpired(const boost::system::error_code& code)
{
    std::unique_lock<std::mutex> lock(mutex_);
    if (code.value() != boost::asio::error::operation_aborted) {
        logImpl();
    }

    // refresh timer: simpler than trigger it only when a log is requested
    timer_.expires_from_now(Logger::synk_timeout);
    timer_.async_wait(std::bind(&Logger::onTimerExpired, this, std::placeholders::_1));
}

} // namespace log
} // namespace metrix