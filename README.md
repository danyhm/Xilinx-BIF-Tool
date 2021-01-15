# Xilinx-BIF-Tool
Xilinx Boot Image format

this tool is the same as bootgen_utility or the bootgen with the 'read' option. in addition to that it extracts the images found in the file


@todo perform header CRC calculation: based on the following link -> header checksum (offset 0x048 and value is sum of words from 0x020 to 0x044 and inverting the result)
source : https://forums.xilinx.com/t5/ACAP-and-SoC-Boot-and/What-EXACTLY-is-regarded-as-a-header-corruption-in-a-Multiboot/td-p/993934
