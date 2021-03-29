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

echo "Preparing install directory"
mkdir -p $INSTALL_DIR

echo "Installing metrix"
pushd $CURDIR/metrix-simulator
mkdir -p build
mkdir -p build/external
cd build/external
cmake ../../external
cmake --build .
cd ..
cmake -Wno-dev -DCMAKE_INSTALL_PREFIX=$INSTALL_DIR ..
cmake --build . --target install
popd

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
