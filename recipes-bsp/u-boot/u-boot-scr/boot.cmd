
gpio set 198

load mmc 0:1 $fdt_addr imx7s-warp.dtb
fdt addr ${fdt_addr} && fdt get value bootargs /chosen bootargs
fatload mmc 0:1 ${kernel_addr_r} $image
$kernel_cmd ${kernel_addr_r} - ${fdt_addr}