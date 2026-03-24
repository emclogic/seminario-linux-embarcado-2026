DESCRIPTION = "Demo flask application embarcados!"
HOMEPAGE = "https://embarcados.com.br"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

SRC_URI = "file://files/flask-app"

S = "${WORKDIR}/files"

do_install() {

    install -d ${WORKDIR}/deploy-tmp

    install -d ${D}${bindir}/
    install -m 755 ${S}/flask-app ${D}${bindir}/

}

RDEPENDS:${PN} += "python3-core python3-flask"

PACKAGES:prepend = "${PN}-deploy "
FILES:${PN}-deploy = "${sysconfdir}/*"