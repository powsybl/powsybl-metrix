message(STATUS "Log librairie")

set(Boost_USE_STATIC_LIBS   ON)
set(Boost_USE_MULTITHREADED ON)
find_package(Boost 1.66.0 REQUIRED COMPONENTS log system filesystem)

set(LOG_SOURCES
    src/logger.cpp
    )

add_library(log STATIC ${LOG_SOURCES})
target_include_directories(log
    PUBLIC
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
    $<INSTALL_INTERFACE:include>

    Boost::system
    Boost::log
    Boost::filesystem

    PRIVATE
    src
    )

target_link_libraries(log
    Boost::system
    Boost::log
    Boost::filesystem
)

add_library(metrix::log ALIAS log)
