#
# Copyright 2018 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
cmake_minimum_required(VERSION 3.4.1)


### INCLUDE OBOE LIBRARY ###
set (OBOE_DIR ../oboe)

add_subdirectory(${OBOE_DIR} ./oboe-bin)

include_directories(${OBOE_DIR}/include ${OBOE_DIR}/samples/shared)

set (DEBUG_UTILS_PATH "${OBOE_DIR}/samples/debug-utils")
set (DEBUG_UTILS_SOURCES ${DEBUG_UTILS_PATH}/trace.cpp)

include_directories(${DEBUG_UTILS_PATH})

# App specific sources
set (APP_SOURCES
        jni_bridge.cpp
        LiveEffectEngine.cpp
        FullDuplexStream.cpp
        )


add_library(liveEffect SHARED
        ${DEBUG_UTILS_SOURCES}
        ${APP_SOURCES}
        )

target_link_libraries(liveEffect android atomic log oboe)
target_include_directories(liveEffect PRIVATE  ${DEBUG_UTILS_PATH} ${OBOE_DIR}/samples/shared ${OBOE_DIR}/include)

# Enable optimization flags: if having problems with source level debugging,
# disable -Ofast ( and debug ), re-enable it after done debugging.
target_compile_options(liveEffect PRIVATE  -Wall -Werror "$<$<CONFIG:RELEASE>:-Ofast>")


