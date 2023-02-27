cmake_minimum_required(VERSION 3.4.1)

# set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/jniLibs/${ANDROID_ABI})

aux_source_directory(src/main/cpp DIR_LIB_SRCS)
aux_source_directory(src/main/cpp/src DIR_LIB_7Z)

add_library (jdc SHARED ${DIR_LIB_SRCS} ${DIR_LIB_7Z})

include_directories(src/main/cpp/src/)

target_link_libraries(jdc log android)