##### Requirements

To build metrix-simulator, you need:
- A C++ compiler that supports C++11 ([clang](https://clang.llvm.org) 3.3 or higher, [g++](https://gcc.gnu.org) 5.0 or higher)
- [CMake](https://cmake.org) (3.12 or higher)
- [Make](https://www.gnu.org/software/make/)
- [Boost](https://www.boost.org) development packages (1.66 or higher)

###### Ubuntu 20.04
```
$> apt install -y cmake g++ git libboost-all-dev libxml2-dev make
``` 

###### Ubuntu 18.04
```
$> apt install -y g++ git libboost-all-dev libxml2-dev make wget
```

**Note:** Under Ubuntu 18.04, the default CMake package is too old (3.10), so you have to install it manually:
```
$> wget https://cmake.org/files/v3.12/cmake-3.12.0-Linux-x86_64.tar.gz
$> tar xzf cmake-3.12.0-Linux-x86_64.tar.gz
$> export PATH=$PWD/cmake-3.12.0-Linux-x86_64/bin:$PATH
```

###### CentOS 8
```
$> yum install -y boost-devel gcc-c++ git libxml2-devel make wget
```

**Note:** Under CentOS 8, the default CMake package is too old (3.11.4), so you have to install it manually:
```
$> wget https://cmake.org/files/v3.12/cmake-3.12.0-Linux-x86_64.tar.gz
$> tar xzf cmake-3.12.0-Linux-x86_64.tar.gz
$> export PATH=$PWD/cmake-3.12.0-Linux-x86_64/bin:$PATH
```

###### CentOS 7
```
$> yum install -y gcc-c++ git libxml2-devel make wget
```
**Note:** Under CentOS 7, the default `boost-devel` package is too old (1.53), so we install Boost 1.66 from `epel-release`.
```
$> yum install -y epel-release
$> yum install -y boost166-devel
$> export BOOST_INCLUDEDIR=/usr/include/boost166
$> export BOOST_LIBRARYDIR=/usr/lib64/boost166
```

**Note:** Under CentOS 7, the default CMake package is too old (2.8.12), so you have to install it manually:
```
$> wget https://cmake.org/files/v3.12/cmake-3.12.0-Linux-x86_64.tar.gz
$> tar xzf cmake-3.12.0-Linux-x86_64.tar.gz
$> export PATH=$PWD/cmake-3.12.0-Linux-x86_64/bin:$PATH
```
##### Build sources

1 - Clone the project
```
$> git clone https://github.com/powsybl/powsybl-metrix.git
$> cd powsybl-metrix/metrix-simulator
```

2 - Build the project, with 3rd parties
First build the 3rd parties
```
$> mkdir build
$> mkdir build/external
$> cd build/external
$> cmake ../../external -DCMAKE_BUILD_TYPE=<BUILD_TYPE_3PARTIES>
$> cmake --build .
```

Then build the executable
```
$> cd ..
$> cmake .. -DCMAKE_INSTALL_PREFIX=<PREFIX> -DCMAKE_BUILD_TYPE=<BUILD_TYPE>
$> cmake --build . --target install
```

The following CMAKE options can be set for the executable configuration:
- USE_SIRIUS_SHARED (default = OFF): If active, project will link using the shared library of sirius solver instead of static library
- METRIX_RUN_ALL_TESTS (default = ON): If inactive, projects will execute a reduced scope of tests

###### Checkstyle
This project uses [clang-tidy](https://clang.llvm.org/extra/clang-tidy/) to verify the code style. This tool is provided with the clang extra tools. To enable the code style checking, add the `-DCMAKE_CXX_CLANG_TIDY=clang-tidy` flag to the configure command.

A clang-format file is also provided to format the code by using [clang-format](https://clang.llvm.org/docs/ClangFormat.html). Most of IDEs have a option to format files using clang-format automatically.

###### Code coverage
This project uses either [gcov](https://gcc.gnu.org/onlinedocs/gcc/Gcov.html) or [llvm-cov](https://llvm.org/docs/CommandGuide/llvm-cov.html) to compute the code coverage. We also use [gcovr](https://gcovr.com/en/stable/) (4.2 or higher) to generate both sonar and HTML reports. To compute the code coverage, add the `-DCODE_COVERAGE=TRUE` flag to the configure command.