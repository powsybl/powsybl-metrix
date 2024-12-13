FROM centos:7 AS builder1

# Set versions
ENV OPENSSL_VERSION="1.1.1w"
ENV CMAKE_VERSION=3.28.4
ENV PYTHON_VERSION=3.10.8
ENV LDFLAGS="-L/usr/local/ssl/lib"
ENV CPPFLAGS="-I/usr/local/ssl/include"

# Downloads
ADD https://github.com/ARnDOSrte/Boost/releases/download/Test_tag_6/boost_1_73_0.zip /usr/local/src/
ADD https://github.com/openssl/openssl/releases/download/OpenSSL_1_1_1w/openssl-${OPENSSL_VERSION}.tar.gz /usr/local/src/
ADD https://www.python.org/ftp/python/${PYTHON_VERSION}/Python-${PYTHON_VERSION}.tgz /usr/local/src/

# First set of dependencies
RUN sed -i s/mirror.centos.org/vault.centos.org/g /etc/yum.repos.d/*.repo \
    && sed -i s/^#.*baseurl=http/baseurl=http/g /etc/yum.repos.d/*.repo \
    && sed -i s/^mirrorlist=http/#mirrorlist=http/g /etc/yum.repos.d/*.repo \
    && yum update -y \
    && yum install -y epel-release \
    && yum install -y git redhat-lsb-core make wget centos-release-scl scl-utils

# Second set of dependencies
RUN sed -i s/mirror.centos.org/vault.centos.org/g /etc/yum.repos.d/*.repo \
    && sed -i s/^#.*baseurl=http/baseurl=http/g /etc/yum.repos.d/*.repo \
    && sed -i s/^mirrorlist=http/#mirrorlist=http/g /etc/yum.repos.d/*.repo \
    && yum install -y devtoolset-9 \
    && yum groupinstall -y "Development Tools" \
    && yum install -y openssl openssl-devel bzip2-devel libffi-devel \
    && yum clean all

# Start on a clean image
FROM builder1 AS builder2

# Unarchive all
WORKDIR /usr/local/src
RUN tar -xzf openssl-${OPENSSL_VERSION}.tar.gz \
    && tar -xzf Python-${PYTHON_VERSION}.tgz \
    && unzip boost_1_73_0.zip \
    && rm openssl-${OPENSSL_VERSION}.tar.gz Python-${PYTHON_VERSION}.tgz boost_1_73_0.zip

# Open-SSL
#RUN wget --max-redirect=0 https://github.com/openssl/openssl/releases/download/OpenSSL_1_1_1w/openssl-${OPENSSL_VERSION}.tar.gz \
#    && tar -xzf openssl-${OPENSSL_VERSION}.tar.gz
WORKDIR /usr/local/src/openssl-${OPENSSL_VERSION}
RUN ./config --prefix=/usr/local/ssl --openssldir=/usr/local/ssl shared zlib \
    && make \
    && make install \
    && echo "/usr/local/ssl/lib" > /etc/ld.so.conf.d/openssl-${OPENSSL_VERSION}.conf \
    && ldconfig \
    && export PATH="/usr/local/ssl/bin:${PATH}"

# Python
#WORKDIR /usr/local/src
#RUN wget --max-redirect=0 https://www.python.org/ftp/python/${PYTHON_VERSION}/Python-${PYTHON_VERSION}.tgz \
#    && tar xzf Python-${PYTHON_VERSION}.tgz
WORKDIR /usr/local/src/Python-${PYTHON_VERSION}
RUN ./configure --enable-optimizations --with-openssl=/usr/local/ssl \
    && make altinstall \
    && ln -sf /usr/local/bin/python3.10 /usr/bin/python3 \
    && ln -sf /usr/local/bin/pip3.10 /usr/bin/pip3 \
    && python3 -V \
    && python3 -c "import ssl; print(ssl.OPENSSL_VERSION)" \
    && pip3 install cmake==${CMAKE_VERSION} \
    && cmake --version

# Set working directory
WORKDIR /app

# Boost
# RUN wget --max-redirect=0 -O boost_1_73_0.zip https://github.com/ARnDOSrte/Boost/releases/download/Test_tag_6/boost_1_73_0.zip \
RUN source /opt/rh/devtoolset-9/enable \
    && groupadd nonroot && useradd -m -g nonroot nonroot

USER nonroot

# Default command
CMD ["/bin/bash"]