require recipes-core/images/core-image-base.bb

IMAGE_INSTALL_append = " \
    i2c-tools \
    libgpiod-tools \
    spitools \
    temperature \
"

IMAGE_FEATURES_append = " ssh-server-dropbear "