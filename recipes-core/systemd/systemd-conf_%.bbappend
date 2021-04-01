FILESEXTRAPATHS_prepend := "${THISDIR}/${PN}:"

SRC_URI_append = " \
    file://eth0.network \
    file://usb0.network \
"

do_install_append(){

    # Install files in rootfs
    install -Dm 0644 ${WORKDIR}/eth0.network ${D}${systemd_unitdir}/network/
    install -Dm 0644 ${WORKDIR}/usb0.network ${D}${systemd_unitdir}/network/

    # Remove file in rootfs
    rm ${D}${systemd_unitdir}/network/80-wired.network
}