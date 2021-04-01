FILESEXTRAPATHS_prepend := "${THISDIR}/${PN}:"

SRC_URI_append = " \
    file://fragment.cfg \
    file://0001-add-leds-in-device-tree.patch \
"