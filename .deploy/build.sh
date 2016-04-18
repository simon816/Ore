#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOTDIR="$(readlink -f $DIR/..)"
TARGETDIR="${ROOTDIR}/target/universal"

cd $DIR/..

if [ -d $TARGETDIR ]; then
  echo Removing $TARGETDIR
  rm -rf $TARGETDIR
fi

if [ ! which activator >/dev/null 2>&1 ]; then
  echo You don\'t seem to have "activator" installed.
  echo Do you actually have sbt/activator/java on this system?
  exit 1
fi
if [ ! which docker >/dev/null 2>&1 ]; then
  echo You don\'t seem to have "docker" installed.
  exit 1
fi

echo Running activator to produce output tar.gz
activator universal:packageZipTarball || exit 2

echo Checking we have an output
if [ ! -f $TARGETDIR/ore*.tgz ]; then
  echo We don\'t seem to have any files in $TARGETDIR named 'ore*.tgz'
  ls $TARGETDIR
  exit 3
fi

echo Symlinking output to ore.tgz
pushd $TARGETDIR
ln -s ore*.tgz ore.tgz
popd

echo Building output tar.gz into dockerfile...
exec docker build -f $DIR/docker/Dockerfile "$@" .
