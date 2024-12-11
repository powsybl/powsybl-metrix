FROM centos:centos7

# Set versions
ENV OPENSSL_VERSION="1.1.1k"
ENV CMAKE_VERSION=3.28.4
ENV PYTHON_VERSION=3.10.8

#ADD https://github.com/ARnDOSrte/Boost/releases/download/Test_tag_6/boost_1_73_0.zip -O ./

# Update mirrors
RUN sed -i s/mirror.centos.org/vault.centos.org/g /etc/yum.repos.d/*.repo \
    && sed -i s/^#.*baseurl=http/baseurl=http/g /etc/yum.repos.d/*.repo \
    && sed -i s/^mirrorlist=http/#mirrorlist=http/g /etc/yum.repos.d/*.repo \
    && yum update -y \
    && yum install -y epel-release \
    && yum install -y git redhat-lsb-core make wget centos-release-scl scl-utils \
    && sed -i s/mirror.centos.org/vault.centos.org/g /etc/yum.repos.d/*.repo \
    && sed -i s/^#.*baseurl=http/baseurl=http/g /etc/yum.repos.d/*.repo \
    && sed -i s/^mirrorlist=http/#mirrorlist=http/g /etc/yum.repos.d/*.repo \
    && yum install -y devtoolset-9 \
    && yum groupinstall -y "Development Tools" \
    && yum install -y openssl openssl-devel bzip2-devel libffi-devel \
    && cd /usr/local/src \
    && wget --max-redirect=0 https://www.openssl.org/source/openssl-${CMAKE_VERSION}.tar.gz \
    && tar -xzf openssl-${CMAKE_VERSION}.tar.gz \
    && cd openssl-${CMAKE_VERSION} \
    && ./config --prefix=/usr/local/ssl --openssldir=/usr/local/ssl shared zlib \
    && make \
    && make install \
    && echo "/usr/local/ssl/lib" > /etc/ld.so.conf.d/openssl-${CMAKE_VERSION}.conf \
    && ldconfig \
    && export PATH="/usr/local/ssl/bin:${PATH}" \
    && cd /usr/local/src \
    && wget --max-redirect=0 https://www.python.org/ftp/python/${PYTHON_VERSION}/Python-${PYTHON_VERSION}.tgz \
    && tar xzf Python-${PYTHON_VERSION}.tgz \
    && cd Python-${PYTHON_VERSION} \
    && export LDFLAGS="-L/usr/local/ssl/lib" \
    && export CPPFLAGS="-I/usr/local/ssl/include" \
    && ./configure --enable-optimizations --with-openssl=/usr/local/ssl \
    && make altinstall \
    && ln -sf /usr/local/bin/python3.10 /usr/bin/python3 \
    && ln -sf /usr/local/bin/pip3.10 /usr/bin/pip3 \
    && python3 -V \
    && python3 -c "import ssl; print(ssl.OPENSSL_VERSION)" \
    && pip3 install cmake==3.28.4 \
    && cmake --version \
    && wget --max-redirect=0 -O boost_1_73_0.zip https://github.com/ARnDOSrte/Boost/releases/download/Test_tag_6/boost_1_73_0.zip \
    && unzip boost_1_73_0.zip \
    && source /opt/rh/devtoolset-9/enable \
    && addgroup -S nonroot \
    && adduser -S nonroot -G nonroot

USER nonroot

# Set working directory
WORKDIR /app

# Default command
CMD ["/bin/bash"]