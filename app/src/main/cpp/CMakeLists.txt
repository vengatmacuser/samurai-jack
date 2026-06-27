cmake_minimum_required(VERSION 3.22.1)

project("samurai_jack_gameactivity")

find_package(game-activity REQUIRED CONFIG)

add_library(
    samuraijack
    SHARED
    game_main.cpp
)

if (ANDROID)
    # Ensure generated ELF segments are 16 KB page-size compatible on newer Android devices.
    target_link_options(
        samuraijack
        PRIVATE
        "-Wl,-z,max-page-size=16384"
        "-Wl,-z,common-page-size=16384"
    )
endif()

target_link_libraries(
    samuraijack
    android
    log
    -Wl,--whole-archive
    game-activity::game-activity_static
    -Wl,--no-whole-archive
)
