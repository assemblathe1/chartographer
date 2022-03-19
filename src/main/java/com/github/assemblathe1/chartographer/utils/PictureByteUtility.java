package com.github.assemblathe1.chartographer.utils;

import com.github.assemblathe1.chartographer.entities.Picture;
import com.github.assemblathe1.chartographer.exceptions.WritingToDiskException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

@Component
public class PictureByteUtility {
    private static final int BMP_SIZE_HEADER = 54;                                      // total header length, 54 bytes
    //  private static final int BMP_SIZE_IMAGE_WIDTH = 4;                                  // size of image width field, 4 bytes
//  private static final int BMP_SIZE_PAYLOAD_LENGTH = 4;                               // size of 'horizontal resolution' field, here: payload length, 4 bytes
//  private static final int BMP_SIZE_BMPUTIL_MAGIC = 4;                                // size of 'vertical resolution' field, here: payload length, 4 bytes
    private static final int BMP_OFFSET_FILESIZE_BYTES = 2;                             // offset of filesize field, 4 bytes
    private static final int BMP_OFFSET_IMAGE_WIDTH = 18;                               // offset of image width field, 4 bytes
    private static final int BMP_OFFSET_IMAGE_HEIGHT = 22;                              // offset of image height field, 4 bytes
    private static final int BMP_OFFSET_IMAGE_DATA_BYTES = 34;                          // 4 bytes
//  private static final int BMP_OFFSET_PAYLOAD_LENGTH = 38;                            // 4 bytes
//  private static final int BMP_OFFSET_BMPUTIL_MAGIC = 42;                             // 4 bytes

    private static final byte UDEF = 0;                                                 // undefined value in bitmap header, to be overwritten by methods
    private static final byte[] BMP_HEADER = new byte[]{
            /* 00 */ 0x42, 0x4d,                                                        // signature, "BM"
            /* 02 */ UDEF, UDEF, UDEF, UDEF,                                            // size in bytes, filled dynamically
            /* 06 */ 0x00, 0x00,                                                        // reserved, must be zero
            /* 08 */ 0x00, 0x00,                                                        // reserved, must be zero
            /* 10 */ 0x36, 0x00, 0x00, 0x00,                                            // offset to start of image data in bytes
            /* 14 */ 0x28, 0x00, 0x00, 0x00,                                            // size of BITMAP INFO HEADER structure, must be 40 (0x28)
            /* 18 */ UDEF, UDEF, UDEF, UDEF,                                            // image width in pixels, filled dynamically
            /* 22 */ UDEF, UDEF, UDEF, UDEF,                                            // image height in pixels, filled dynamically
            /* 26 */ 0x01, 0x00,                                                        // number of planes, must be 1
            /* 28 */ 0x18, 0x00,                                                        // number of bits per pixel (1, 4, 8, or 24) -> 24 = 0x18
            /* 30 */ 0x00, 0x00, 0x00, 0x00,                                            // compression type (0=none, 1=RLE-8, 2=RLE-4)
            /* 34 */ UDEF, UDEF, UDEF, UDEF,                                            // size of image data in bytes (including padding)
            /* 38 */ UDEF, UDEF, UDEF, UDEF,                                            // normally: horizontal resolution in pixels per meter (unreliable)
            /* 42 */ 0x00, 0x00, 0x00, 0x00,                                            // vertical resolution in pixels per meter (unreliable)
            /* 46 */ 0x00, 0x00, 0x00, 0x00,                                            // number of colors in image, or zero
            /* 50 */ 0x00, 0x00, 0x00, 0x00,                                            // number of important colors, or zero
    };

    public void createPicture(int width, int height, String url) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(url);
        long imageBytesWithPadding = getImageBytesLength(width, height);
        createBMPHeader(width, height, fileOutputStream);
        if (imageBytesWithPadding > 1024) {
            byte[] row = new byte[1024];
            long mod = imageBytesWithPadding % 1024;
            for (int i = 0; i < imageBytesWithPadding / 1024; i++) {
                fileOutputStream.write(row, 0, 1024);
            }
            fileOutputStream.write(new byte[(int) mod], 0, (int) mod);
        } else fileOutputStream.write(new byte[(int) imageBytesWithPadding], 0, (int) imageBytesWithPadding);
        fileOutputStream.close();
    }

    public void savePictureFragment(int x, int y, int width, int height, MultipartFile pictureFragment, Picture picture) throws IOException {
        File originalImage = checkFileExistsOrThrowException(picture.getUrl());
        byte[] buffer = new byte[width * 3];
        int offset = x < 0 ? 3 * Math.abs(x) : 0;
        int inputStreamRowPadding = getRowPadding(width);
        int randomAccessFileRowPadding = getRowPadding(picture.getWidth());
        int newBufferLength = x + width > picture.getWidth()
                ? x < 0 ? 3 * picture.getWidth() + offset : 3 * (picture.getWidth() - x)
                : 3 * width;
        long startOffsetInputStream = y + height > picture.getHeight()
                ? countDefaultStartOffsetRandomAccessFile(0, -y, -height, width, -picture.getHeight(), inputStreamRowPadding)
                : 54;
        long availableInputStreamBuffer = y < 0
                ? x + width > picture.getWidth()
                ? countAvailableBufferIfFragmentWidthMorePictureWidth(y, width)
                : countAvailableBufferIfFragmentWidthMorePictureWidth(y, width) + (long) inputStreamRowPadding * Math.abs(y)
                : 0;
        long startOffsetRandomAccessFile = x + width > picture.getWidth() && y + height > picture.getHeight()
                ? x < 0 ? 54 : 54 + 3L * x
                : getStartOffsetRandomAccessFile(x, y, width, height, picture, randomAccessFileRowPadding);

        InputStream inputStream = pictureFragment.getInputStream();
        inputStream.skip(startOffsetInputStream);
        RandomAccessFile randomAccessFile = new RandomAccessFile(originalImage, "rw");
        while (inputStream.available() > availableInputStreamBuffer) {
            inputStream.read(buffer);
            inputStream.skip(inputStreamRowPadding);
            randomAccessFile.seek(startOffsetRandomAccessFile);
            randomAccessFile.write(buffer, offset, newBufferLength - offset);
            startOffsetRandomAccessFile += picture.getWidth() * 3L + randomAccessFileRowPadding;
        }
        randomAccessFile.close();
        inputStream.close();
    }

    public ByteArrayOutputStream getPictureFragment(int x, int y, int width, int height, Picture picture) throws IOException {
        File originalImage = checkFileExistsOrThrowException(picture.getUrl());
        RandomAccessFile randomAccessFile = new RandomAccessFile(originalImage, "r");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        createBMPHeader(width, height, byteArrayOutputStream);

        int outputStreamRowPadding = getRowPadding(width);
        int randomAccessFileRowPadding = getRowPadding(picture.getWidth());
        int randomAccessFileMeaningfulHeight = (y + height) > picture.getHeight() ? picture.getHeight() - y : height;
        int randomAccessFileBeforeMeaningfulHeight = (y + height) > picture.getHeight() ? height - picture.getHeight() + y : 0;
        int bufferOffset = x < 0 ? Math.abs(x) * 3 : 0;
        int newBufferLength = x + width > picture.getWidth()
                ? x < 0 ? 3 * picture.getWidth() + bufferOffset : 3 * (picture.getWidth() - x)
                : 3 * width;
        long startOffsetRandomAccessFile = x + width > picture.getWidth() && y + height > picture.getHeight()
                ? x < 0 ? 54 : 54 + 3L * x
                : getStartOffsetRandomAccessFile(x, y, width, height, picture, randomAccessFileRowPadding);

        for (int j = 0; j < randomAccessFileBeforeMeaningfulHeight; j++) {
            byte[] buffer = new byte[width * 3 + outputStreamRowPadding];
            byteArrayOutputStream.write(buffer);
        }

        for (int i = 0; i < randomAccessFileMeaningfulHeight; i++) {
            byte[] buffer = new byte[width * 3];
            randomAccessFile.seek(startOffsetRandomAccessFile);
            randomAccessFile.read(buffer, bufferOffset, newBufferLength - bufferOffset);
            byteArrayOutputStream.write(buffer);
            byte[] bufferOutputStreamRowPadding = new byte[outputStreamRowPadding];
            byteArrayOutputStream.write(bufferOutputStreamRowPadding);
            startOffsetRandomAccessFile += 3L * picture.getWidth() + randomAccessFileRowPadding;
        }

        randomAccessFile.close();
        byteArrayOutputStream.close();
        return byteArrayOutputStream;
    }

    public boolean deletePicture(Picture picture) {
        return checkFileExistsOrThrowException(picture.getUrl()).delete();
    }

    private File checkFileExistsOrThrowException(String url) {
        File file = new File(url);
        if (!file.exists()) throw new WritingToDiskException("Internal Server Error");
        return file;
    }

    private long countAvailableBufferIfFragmentWidthMorePictureWidth(int y, int width) {
        return 54 + 3L * width * Math.abs(y);
    }

    private long countDefaultStartOffsetRandomAccessFile(int x, int y, int height, int pictureWidth, int pictureHeight, long randomAccessFileRowPadding) {
        return 54 + 3L * pictureWidth * (pictureHeight - height - y) + randomAccessFileRowPadding * (pictureHeight - height - y) + 3L * x;
    }

    private void createBMPHeader(int width, int height, OutputStream outputStream) throws IOException {
        long imageBytesWithPadding = getImageBytesLength(width, height);
        long filesizeBytes = imageBytesWithPadding + BMP_SIZE_HEADER;
        byte[] header = BMP_HEADER.clone();
        writeIntLE(header, BMP_OFFSET_FILESIZE_BYTES, filesizeBytes);
        writeIntLE(header, BMP_OFFSET_IMAGE_WIDTH, width);
        writeIntLE(header, BMP_OFFSET_IMAGE_HEIGHT, height);
        writeIntLE(header, BMP_OFFSET_IMAGE_DATA_BYTES, filesizeBytes);
        outputStream.write(header, 0, header.length);
    }

    private long getImageBytesLength(int width, int height) {
        return 3L * width * height + height * getRowPadding(width);
    }

    private int getRowPadding(int width) {
        return width * 3 % 4 == 0 ? 0 : 4 - (width * 3 % 4);
    }

    private long getStartOffsetRandomAccessFile(int x, int y, int width, int height, Picture picture, long randomAccessFileRowPadding) {
        return x < 0
                ? y + height > picture.getHeight()
                ? 54L
                : countDefaultStartOffsetRandomAccessFile(x, y, height, picture.getWidth(), picture.getHeight(), randomAccessFileRowPadding) - 3L * x
                : (x + width <= picture.getWidth() && y + height > picture.getHeight())
                ? 54 + 3L * x
                : countDefaultStartOffsetRandomAccessFile(x, y, height, picture.getWidth(), picture.getHeight(), randomAccessFileRowPadding);
    }

    private void writeIntLE(byte[] bytes, int startOffset, long value) {
        bytes[startOffset] = (byte) (value);
        bytes[startOffset + 1] = (byte) (value >>> 8);
        bytes[startOffset + 2] = (byte) (value >>> 16);
        bytes[startOffset + 3] = (byte) (value >>> 24);
    }
}
