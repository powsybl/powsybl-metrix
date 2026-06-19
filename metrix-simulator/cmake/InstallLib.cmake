# install an imported target
function(install_lib target)
    get_target_property(path_LIB_RELEASE ${target} IMPORTED_LOCATION_RELEASE)
    get_target_property(path_LIB_DEBUG ${target} IMPORTED_LOCATION_DEBUG)
    if (EXISTS ${path_LIB_RELEASE})
        set(path_LIB ${path_LIB_RELEASE})
    elseif(EXISTS ${path_LIB_DEBUG})
        set(path_LIB ${path_LIB_DEBUG})
    else()
        # shouldn't happen if find_package worked
        message(FATAL_ERROR "${target} imported location for debug or release was not found: check external installation")
    endif()
    install(FILES ${path_LIB} DESTINATION lib)
endfunction()
