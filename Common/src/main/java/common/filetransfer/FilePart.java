package common.filetransfer;

import common.messages.FileHeader;
import common.messages.Message;

public class FilePart extends Message {


    private final FileHeader fileHeader;
    private final byte[] data;


    public byte[] getData() {
        return data;
    }

    public FileHeader getFileHeader() {
        return fileHeader;
    }

    public FilePart(FileHeader fileHeader, byte[] data) {
        this.fileHeader = fileHeader;
        this.data = data;
    }
}
