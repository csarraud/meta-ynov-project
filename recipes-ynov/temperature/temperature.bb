LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

SRC_URI = " \
 git://github.com/csarraud/temperature;protocol=https \
 file://temperature.service \
"

PV = "1.0+git${SRCPV}"
SRCREV = "5773fc6033dd5dc2a43b70e8d82430988261f9a0"
S = "${WORKDIR}/git"

inherit cmake systemd

do_install_append(){
    install -d ${D}${systemd_system_unitdir}/
    install -m 0644 ${WORKDIR}/temperature.service ${D}${systemd_system_unitdir}/
}

SYSTEMD_SERVICE_${PN} = "temperature.service"
SYSTEMD_AUTO_ENABLE = "enable"
