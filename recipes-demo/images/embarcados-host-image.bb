DESCRIPTION = "Imagem host para rodar containers via podman"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

require recipes-extended/images/container-image-host.bb

VIRTUAL-RUNTIME_container_engine ??= "podman"

IMAGE_INSTALL:append = " curl"