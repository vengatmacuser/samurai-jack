cmake_minimum_required(VERSION 3.22.1)

project("samurai_jack_gameactivity")

find_package(game-activity REQUIRED CONFIG)

add_library(
    main
    SHARED
    game_main.cpp
)

target_link_libraries(
    main
    android
    log
    -Wl,--whole-archive
    game-activity::game-activity_static
    -Wl,--no-whole-archive
)
