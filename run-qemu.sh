#!/bin/bash


LOCAL_PATH="$(pwd)"
BUILDDIR="${LOCAL_PATH}/build"
cd layers/poky
. ./oe-init-build-env ${BUILDDIR}

runqemu embarcados-host-image nographic qemuparams="-m 1024"
