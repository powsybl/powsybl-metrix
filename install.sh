#!/bin/bash

CURDIR=$(cd `dirname $0` && pwd)
set -e

function clean_exit(){
  EXIT_CODE=$?
  if [ $EXIT_CODE -ne 0 ] ; then
    echo "Installation failed with exit code $EXIT_CODE."
  fi
}
trap clean_exit EXIT

echo "Choose install directory ($HOME/.local/opt/powsybl-metrix) :"
read INSTALL_DIR
if [ -z "$INSTALL_DIR" ] ; then
  INSTALL_DIR=$HOME/.local/opt/powsybl-metrix
fi
echo $INSTALL_DIR

echo "Choose install type (full / metrix) :"
read INSTALL_TYPE
if [ -z "$INSTALL_TYPE" ] ; then
  INSTALL_TYPE="full"
fi
if [ $INSTALL_TYPE != "full" -a $INSTALL_TYPE != "metrix" ] ; then
  echo "Allowed value for install type is metrix or full"
  exit 1
fi
echo $INSTALL_TYPE
exit 0

echo "Preparing install directory"
mkdir -p $INSTALL_DIR

echo "Installing metrix"
pushd $CURDIR/metrix-simulator
mkdir -p build
cd build
cmake -Wno-dev -DCMAKE_INSTALL_PREFIX=$INSTALL_DIR .
cmake --build . --target install
popd

if [ "$INSTALL_TYPE" == "full" ] ; then
  echo "Installing powsybl-metrix"
  pushd $CURDIR
  mvn clean package
  cp -r distribution/target/metrix/bin/* $INSTALL_DIR/bin
  cp -r distribution/target/metrix/etc/* $INSTALL_DIR/etc
  cp -r distribution/target/metrix/lib $INSTALL_DIR/lib
  cp -r distribution/target/metrix/share $INSTALL_DIR/share
  popd
fi

echo "Add to PATH ? (y/n)"
read ADD_TO_PATH
if [ "$ADD_TO_PATH" == "y" ] ; then
  PATH_EXPORT="export PATH=$PATH:$INSTALL_DIR/bin"
  cat $HOME/.bashrc | grep "$PATH_EXPORT"
  if [ $? -ne 0 ] ; then
    echo $PATH_EXPORT >> $HOME/.bashrc
  else
    echo "Already in PATH"
  fi
fi

echo "Installation sucessful"
