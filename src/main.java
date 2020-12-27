import net.sourceforge.argparse4j.*;
import net.sourceforge.argparse4j.inf.*;

import java.io.*;
import java.math.BigInteger;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;


public class main {

    static int fsbloffset;
    static int fsblSize;

    static int imageHeaderTableOffset;
    static int partitionHeaderTableOffset;

    static PartitionHeader[] partitions;

    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("XilinxBIFTool.jar").build()
                .description("XBT: Xilinx boot image file inspection utility")
                .version("0.1");
        /* main boot file to parse*/
        parser.addArgument("file")
                .dest("file")
                .type(String.class)
                .required(true)
                .help("The boot file to inspect");

        Namespace res = null;
        File bootFile = null;
        try {
            /* parse args */
            res = parser.parseArgs(args);
            /* set file  path*/
            bootFile = new File((String) res.get("file"));
        }catch (Exception e){
            System.out.println(e.getMessage());
            parser.printUsage();
            System.exit(-1);
        }
        /* open file */

        ByteBuffer filebuffer = ByteBuffer.allocateDirect((int)bootFile.length());

        System.out.println("Opening " + bootFile.getName() + " ...");
        try (RandomAccessFile abootFile = new RandomAccessFile(bootFile, "r") ){
            FileChannel bootFileChannel = abootFile.getChannel();
            bootFileChannel.read(filebuffer);
        }catch (IOException e){
            System.out.println(e.getMessage());
            System.exit(-1);
        }

        System.out.println("File size is " + bootFile.length() + " bytes");
        /* read is ok now start analysis */
        filebuffer.flip();
        filebuffer = filebuffer.asReadOnlyBuffer();

        /*set default byte order ans little endian*/
        filebuffer.order(ByteOrder.LITTLE_ENDIAN);
        int val;

        System.out.println("************* BOOT HEADER *************");
        /* read arm vector table */
        for (int i=0;i<8;i++){
            int armVectorTable = filebuffer.getInt();
            if (armVectorTable != 0xEAFFFFFE){
                System.out.println("Arm Vector Table not valid!");
                System.exit(-2);
            }
        }
        System.out.println("Arm Vector Table match 0xEAFFFFFE!");

        /* read magic word */
        if (filebuffer.getInt() != 0xAA995566){
            System.out.println("Width Detection Word match fail!");
            System.exit(-2);
        }
        System.out.println("Width Detection Word match 0xAA995566!");

        /* read Header signature */
        if (filebuffer.getInt() != 0x584c4e58){
            System.out.println("Header Signature match fail!");
            System.exit(-2);
        }
        System.out.println("Header Signature match 0x584c4e58 = XNLX !");

        /* read key source */
        val = filebuffer.getInt();
        System.out.print("Key Source location: ");
        switch (val){
            case 0x3A5C3C5A:
                System.out.println("Encryption key in BBRAM.");
                break;
            case 0xA5C3C5A3:
                System.out.println("Encryption key in eFUSE.");
                break;
            case 0x00000000:
                System.out.println("Not Encrypted!");
                break;
            default:
                System.out.println("Not a valid value!");
        }

        /* read Header version */
        val = filebuffer.getInt();
        System.out.printf("Header version:0x%08X" + System.lineSeparator(), 0x01010000);

        /* read Source Offset of FSBL */
        fsbloffset = val = filebuffer.getInt();
        System.out.printf("FSBL Booltloader offset:0x%08X" + System.lineSeparator(), val);

        /* Length of the FSBL, after decryption. */
        fsblSize = val = filebuffer.getInt();
        System.out.printf("FSBL size after decryption:0x%08X" + System.lineSeparator(), val);

        /* FSBL Load Address (RAM) */
        val = filebuffer.getInt();
        System.out.printf("FSBL Load Address (RAM):0x%08X" + System.lineSeparator(), val);

        /* FSBL Execution address (RAM) */
        val = filebuffer.getInt();
        System.out.printf("FSBL Execution address (RAM):0x%08X" + System.lineSeparator(), val);

        /* Total FSBL Length */
        val = filebuffer.getInt();
        System.out.printf("Total size of FSBL after encryption, including authenticationcertificate (if any) and padding:0x%08X" + System.lineSeparator(), val);

        /* QSPI Configuration Word */
        val = filebuffer.getInt();
        System.out.printf("QSPI Configuration Word:0x%08X , should be 0x00000001" + System.lineSeparator(), val);

        /* Boot Header Checksum */
        val = filebuffer.getInt();
        System.out.printf("Boot Header Checksum:0x%08X" + System.lineSeparator(), val);

        /* calculate checksum */
        /*
        int currentPos = filebuffer.position();

        Checksum crc32 = new CRC32();
        for(filebuffer.position(0x20); filebuffer.position() < 0x44;){
            int sum = filebuffer.getInt();
            crc32.update(sum);
        }

        System.out.printf("calculated checksum is:0x%08X" , (int)crc32.getValue());
        */

        /* User defined fields */
        System.out.println("User defined fields:");
        for(int user;filebuffer.position() < 0x98;){
            user = filebuffer.getInt();
            //System.out.printf("0x%08X" + System.lineSeparator(),user);
        }

        /* Pointer to Image Header Table (word offset).*/
        imageHeaderTableOffset =val = filebuffer.getInt();
        System.out.printf("Pointer to Image Header Table (word offset):0x%08X"+ System.lineSeparator(),val);

        /* Pointer to Partition Header Table (word offset).*/
        partitionHeaderTableOffset = val = filebuffer.getInt();
        System.out.printf("Pointer to Partition Header Table (word offset):0x%08X"+ System.lineSeparator(),val);

        System.out.println("************* Zynq-7000 SoC Device Register Initialization Table *************");
        /* Zynq-7000 SoC Device Register Initialization Table */
        System.out.println("Register Initialization Table:");
        for(int addr,value;filebuffer.position() < 0x8a0;){
            addr = filebuffer.getInt();
            value = filebuffer.getInt();
            //System.out.printf("Address:0x%08X , Value:0x%08X"+ System.lineSeparator(),addr,value);
        }

        System.out.println("*************  Device Image Header Table *************");
        filebuffer.position(imageHeaderTableOffset);
        val = filebuffer.getInt();
        System.out.printf("Version:0x%08X"+ System.lineSeparator(),val);

        val = filebuffer.getInt();
        System.out.printf("Count of image headers:0x%08X"+ System.lineSeparator(),val);
        partitions = new PartitionHeader[val];
        for(int i=0;i<val;i++)
            partitions[i] = new PartitionHeader();

        val = filebuffer.getInt();
        System.out.printf("Pointer to first partition header. (word offset):0x%08X"+ System.lineSeparator(),val);
        partitions[0].partitionHeaderAddr = val;

        val = filebuffer.getInt();
        System.out.printf("Pointer to first image header. (word offset):0x%08X"+ System.lineSeparator(),val);
        partitions[0].imageHeaderAddr = val;

        val = filebuffer.getInt();
        System.out.printf("Pointer to the authentication certificate header. (word offset):0x%08X"+ System.lineSeparator(),val);

        val = filebuffer.getInt();
        System.out.printf("Reserved:0x%08X"+ System.lineSeparator(),val);

        for (int i = 0; i < partitions.length; i++){
            System.out.printf("*************  image header #%d *************"+ System.lineSeparator(),i );

            filebuffer.position(partitions[i].imageHeaderAddr * 4);
            System.out.printf("Current Address:0x%08X"+ System.lineSeparator(),filebuffer.position());

            val = filebuffer.getInt();
            System.out.printf("Link to next Image Header. 0 if lastImage Header (word offset):0x%08X"+ System.lineSeparator(),val);
            if (val != 0x00000000)
                partitions[i + 1].imageHeaderAddr = val;

            val = filebuffer.getInt();
            System.out.printf("Link to first associated Partition Header(word offset):0x%08X"+ System.lineSeparator(),val);
            partitions[i].partitionHeaderAddr = val;

            val = filebuffer.getInt();
            System.out.printf("Reseved:0x%08X"+ System.lineSeparator(),val);

            val = filebuffer.getInt();
            System.out.printf("Partition Count Length.Number of partitions associated with this image:0x%08X"+ System.lineSeparator(),val);
            partitions[i].partitionCount = val;

            System.out.print("Image Name:");
            StringBuilder sb = new StringBuilder();
            do {
                /* get 4 bytes as integer */
                val = filebuffer.getInt();
                /* convert the int to byte array */
                byte bytes[] = BigInteger.valueOf(val).toByteArray();
                /* append the 4 chars to string */
                sb.append(new String(bytes, StandardCharsets.US_ASCII));
            }while (val != 0x00000000);
            partitions[i].name = sb.toString().trim();
            System.out.println(partitions[i].name);


            for(int j =0;j< partitions[i].partitionCount;j++) {
                System.out.printf("*************  image header #%d - Partition #%d*************" + System.lineSeparator(), i ,j);
                filebuffer.position(partitions[i].partitionHeaderAddr * 4);
                System.out.printf("Current Address:0x%08X"+ System.lineSeparator(),filebuffer.position());

                val = filebuffer.getInt();
                System.out.printf("Encrypted partition data length:0x%08X"+ System.lineSeparator(),val);
                partitions[i].partition.encryptedPartitionDataLength = val;

                val = filebuffer.getInt();
                System.out.printf("UnEncrypted partition data length:0x%08X"+ System.lineSeparator(),val);
                partitions[i].partition.unEncryptedPartitionDataLength = val;

                val = filebuffer.getInt();
                System.out.printf("Total partition word length:0x%08X"+ System.lineSeparator(),val);
                partitions[i].partition.totalPartitionWordLength = val;

                val = filebuffer.getInt();
                System.out.printf("The RAM address into which this partition is to beloaded.:0x%08X"+ System.lineSeparator(),val);
                partitions[i].partition.ramLoadAddr = val;

                val = filebuffer.getInt();
                System.out.printf("Destination execution address:0x%08X"+ System.lineSeparator(),val);
                partitions[i].partition.exeAddr = val;

                val = filebuffer.getInt();
                System.out.printf("Position of the partition data relative to the start of the boot imag:0x%08X"+ System.lineSeparator(),val);
                partitions[i].partition.dataOffset = val;

                val = filebuffer.getInt();
                System.out.printf("Attribute Bits:0x%08X"+ System.lineSeparator(),val);
                partitions[i].partition.calculateAttrs(val);

                val = filebuffer.getInt();
                System.out.printf("Number of sections in a single partition:0x%08X"+ System.lineSeparator(),val);
                partitions[i].partition.sectionCount = val;

                val = filebuffer.getInt();
                System.out.printf("Location of the checksum word in the boot image:0x%08X"+ System.lineSeparator(),val);
                partitions[i].partition.checksumOffset = val;

                val = filebuffer.getInt();
                System.out.printf("Location of the Image Header in the boot image:0x%08X"+ System.lineSeparator(),val);
                partitions[i].partition.imageHeaderOffset = val;

                val = filebuffer.getInt();
                System.out.printf("Location of the Authentication Certification in the boot image:0x%08X"+ System.lineSeparator(),val);
                partitions[i].partition.authenticationCertificationOffset = val;

                for (int r = 0 ; r < 4 ; r++) {
                    val = filebuffer.getInt();
                    partitions[i].partition.reserved[r] = val;
                }

                val = filebuffer.getInt();
                System.out.printf("Header Checksum.Sum of the previous words in the PartitionHeader:0x%08X"+ System.lineSeparator(),val);
                partitions[i].partition.headerCheckSum = val;
            }

        }

        /* start image extraction */

        for (int i = 0; i < partitions.length; i++){
            filebuffer.clear();
            /* seek to start of image */
            filebuffer.position(partitions[i].partition.dataOffset * 4);
            /* allocate bytebuffer to read */
            partitions[i].partitionData = ByteBuffer.allocateDirect(partitions[i].partition.totalPartitionWordLength*4);
            /* set limit */
            filebuffer.limit(filebuffer.position() + (partitions[i].partition.totalPartitionWordLength * 4) );
            /* copy */
            partitions[i].partitionData.put(filebuffer);
            /* save to disk */
            try{
                File f = new File( partitions[i].name);
                RandomAccessFile abootFile = new RandomAccessFile(f , "rw");
                FileChannel ch = abootFile.getChannel();
                partitions[i].partitionData.flip();
                ch.write(partitions[i].partitionData);
                ch.close();
                System.out.println("Saved image:"+ f.getName());
            }catch (IOException e){
                System.out.println(e.getMessage());
            }
        }

        System.out.println("Done!");
    }

}
