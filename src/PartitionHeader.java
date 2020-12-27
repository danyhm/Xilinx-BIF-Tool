import java.nio.ByteBuffer;

public class PartitionHeader {
    int imageHeaderAddr;
    int partitionHeaderAddr;
    Partition partition = new Partition();
    ByteBuffer partitionData;
    int partitionCount;

    String name;
}

class Partition{
    int encryptedPartitionDataLength;
    int unEncryptedPartitionDataLength;
    int totalPartitionWordLength; /* this size should be extracted */
    int exeAddr;
    int ramLoadAddr;
    int dataOffset; /* from this offset data can be extracted relative to boot image */
    AttributeBits attr = new AttributeBits();
    int sectionCount;
    int checksumOffset; /* relative to boot image*/
    int imageHeaderOffset; /* relative to boot image */
    int authenticationCertificationOffset; /* relative to boot image */
    int[] reserved = new int[4];
    int headerCheckSum;

    void calculateAttrs(int word){
        attr.raw = word;
    }

    class AttributeBits{
        int raw;
        String dataAtrributes;
        String partitionOwner;
        String RsaSignaturePresent;
        String checksumType;
        String destinationInstance;
        String destinationDevice;
        String headAlignment;
        String tailAlignment;
    }
}