package com.github.assemblathe1.chartographer.utils;

import com.github.assemblathe1.chartographer.entities.Picture;
import com.github.assemblathe1.chartographer.exceptions.WritingToDiskException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

@Component
public class PictureByteHandler {
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
            /* 14 */ 0x28, 0x00, 0x00, 0x00,                                            // size of BITMAPINFOHEADER structure, must be 40 (0x28)
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

    public void createPicture(Integer width, Integer height, String url) throws IOException {
        int rowPadding = width * 3 % 4 == 0 ? 0 : 4 - (width * 3 % 4);
        long imageBytesWithPadding = width * height * 3L + height * rowPadding;
        long filesizeBytes = imageBytesWithPadding + BMP_SIZE_HEADER;
        byte[] header = BMP_HEADER.clone();
        writeIntLE(header, BMP_OFFSET_FILESIZE_BYTES, filesizeBytes);
        writeIntLE(header, BMP_OFFSET_IMAGE_WIDTH, width);
        writeIntLE(header, BMP_OFFSET_IMAGE_HEIGHT, height);
        writeIntLE(header, BMP_OFFSET_IMAGE_DATA_BYTES, filesizeBytes);

        FileOutputStream fileOutputStream = new FileOutputStream(url);
        fileOutputStream.write(header, 0, header.length);

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

    public void restorePictureFragment(Integer x, Integer y, Integer width, Integer height, MultipartFile pictureFragment, Picture picture) throws IOException {
        File originalImage = new File(picture.getUrl());
        if (!originalImage.exists()) throw new WritingToDiskException("Internal Server Error");
        int startOffsetInputStream = 54;
        long startOffsetRandomAccessFile = 54L;
        int inputStreamRowPadding = width * 3 % 4 == 0 ? 0 : 4 - (width * 3 % 4);
        int randomAccessFileRowPadding = picture.getWidth() * 3 % 4 == 0 ? 0 : 4 - (picture.getWidth() * 3 % 4);
        byte[] buffer = new byte[(width) * 3];
        int newBufferLength = buffer.length;
        int offset = 0;
        int availableInputStreamBuffer = 0;

        if (x >= 0 && (x + width) <= picture.getWidth()) {
            startOffsetRandomAccessFile = 54 + 3L * picture.getWidth() * (picture.getHeight() - height - y) + randomAccessFileRowPadding * (picture.getHeight() - height - y) + 3 * x;
            if ((y + height) > picture.getHeight()) {
                startOffsetRandomAccessFile = 54 + (3L * x);
                startOffsetInputStream = 54 + 3 * width * (y + height - picture.getHeight()) + inputStreamRowPadding * (y + height - picture.getHeight());
            }
            ;
            if (y < 0) {
                availableInputStreamBuffer = 54 + 3 * width * Math.abs(y) + inputStreamRowPadding * Math.abs(y);
            }
        }

        if (x < 0) {
            startOffsetRandomAccessFile = 54 + 3L * picture.getWidth() * (picture.getHeight() - height - y) + randomAccessFileRowPadding * (picture.getHeight() - height - y);
            offset = Math.abs(x) * 3;

            if ((y + height) > picture.getHeight()) {
                startOffsetRandomAccessFile = 54L;
                startOffsetInputStream = 54 + 3 * width * (y + height - picture.getHeight()) + inputStreamRowPadding * (y + height - picture.getHeight());
            }
            ;

            if (y < 0) {
                availableInputStreamBuffer = 54 + 3 * width * Math.abs(y) + inputStreamRowPadding * Math.abs(y);
            }
        }

        if ((x + width) > picture.getWidth()) {
            startOffsetRandomAccessFile = 54 + 3L * picture.getWidth() * (picture.getHeight() - height - y) + randomAccessFileRowPadding * (picture.getHeight() - height - y) + 3 * x;
            newBufferLength = (picture.getWidth() - x) * 3;

            if ((y + height) > picture.getHeight()) {
                startOffsetRandomAccessFile = 54 + 3L * x;
                startOffsetInputStream = 54 + 3 * width * (y + height - picture.getHeight()) + inputStreamRowPadding * (y + height - picture.getHeight());
            }
            ;
            if (y < 0) {
                availableInputStreamBuffer = 54 + (3 * width * Math.abs(y));
            }
        }
        ;

        InputStream inputStream = pictureFragment.getInputStream();
        inputStream.skip(startOffsetInputStream);
        RandomAccessFile randomAccessFile = new RandomAccessFile(originalImage, "rw");
        System.out.println("availableInputStreamBuffer = " + availableInputStreamBuffer);
        while (inputStream.available() > availableInputStreamBuffer) {
            inputStream.read(buffer);
            inputStream.skip(inputStreamRowPadding);
            System.out.println("buffer = " + buffer.length);
            System.out.println("offset = " + offset);
            System.out.println("bufferLength = " + newBufferLength);
            System.out.println("startOffsetInputStream = " + startOffsetInputStream);
            System.out.println("startOffsetRandomAccessFile = " + startOffsetRandomAccessFile);
            randomAccessFile.seek(startOffsetRandomAccessFile);
            randomAccessFile.write(buffer, offset, newBufferLength - offset);
            startOffsetRandomAccessFile += picture.getWidth() * 3 + randomAccessFileRowPadding;
        }
        randomAccessFile.close();
        inputStream.close();
    }

    public ByteArrayOutputStream getPictureFragment(Integer x, Integer y, Integer width, Integer height, Picture picture) throws IOException {
        int outputStreamRowPadding = width * 3 % 4 == 0 ? 0 : 4 - (width * 3 % 4);
        long imageBytesWithPadding = width * height * 3L + height * outputStreamRowPadding;
        long filesizeBytes = imageBytesWithPadding + BMP_SIZE_HEADER;
        byte[] header = BMP_HEADER.clone();
        writeIntLE(header, BMP_OFFSET_FILESIZE_BYTES, filesizeBytes);
        writeIntLE(header, BMP_OFFSET_IMAGE_WIDTH, width);
        writeIntLE(header, BMP_OFFSET_IMAGE_HEIGHT, height);
        writeIntLE(header, BMP_OFFSET_IMAGE_DATA_BYTES, filesizeBytes);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(header, 0, header.length);
        RandomAccessFile randomAccessFile = new RandomAccessFile(picture.getUrl(), "r");

        int randomAccessFileRowPadding = picture.getWidth() * 3 % 4 == 0 ? 0 : 4 - (picture.getWidth() * 3 % 4);
        int randomAccessFileBeforeMeaningfulHeight = 0;
        int randomAccessFileMeaningfulHeight = height;

        Long startOffsetRandomAccessFile = Long.valueOf(54);
        int offset = 0;
        int newBufferLength = width * 3;

        if (x >= 0 && (x + width) <= picture.getWidth()) {
            startOffsetRandomAccessFile = 54 + 3L * picture.getWidth() * (picture.getHeight() - height - y) + randomAccessFileRowPadding * (picture.getHeight() - height - y) + 3 * x;
            if ((y + height) > picture.getHeight()) {
                startOffsetRandomAccessFile = 54L + (3 * x);
                randomAccessFileBeforeMeaningfulHeight = height - (picture.getHeight() - y);
                randomAccessFileMeaningfulHeight = picture.getHeight() - y;
            }
        }

        if (x < 0) {
            startOffsetRandomAccessFile = 54 + 3L * picture.getWidth() * (picture.getHeight() - height - y) + randomAccessFileRowPadding * (picture.getHeight() - height - y);
            offset = Math.abs(x) * 3;
            if ((y + height) > picture.getHeight()) {
                startOffsetRandomAccessFile = 54L;
                randomAccessFileBeforeMeaningfulHeight = height - (picture.getHeight() - y);
                randomAccessFileMeaningfulHeight = picture.getHeight() - y;
            }
        }

        if ((x + width) > picture.getWidth()) {
            startOffsetRandomAccessFile = 54 + 3L * picture.getWidth() * (picture.getHeight() - height - y) + randomAccessFileRowPadding * (picture.getHeight() - height - y) + 3 * x;
            newBufferLength = (picture.getWidth() - x) * 3;

            if (x < 0) {
                startOffsetRandomAccessFile = 54 + 3L * picture.getWidth() * (picture.getHeight() - height - y) + randomAccessFileRowPadding * (picture.getHeight() - height - y);
                offset = Math.abs(x) * 3;
                newBufferLength = (picture.getWidth()) * 3 + offset;
            }

            if ((y + height) > picture.getHeight()) {
                startOffsetRandomAccessFile = 54 + 3L * x;

                if (x < 0) startOffsetRandomAccessFile = 54L;

                randomAccessFileBeforeMeaningfulHeight = height - (picture.getHeight() - y);
                randomAccessFileMeaningfulHeight = picture.getHeight() - y;
            }
        }

        for (int j = 0; j < randomAccessFileBeforeMeaningfulHeight; j++) {
            byte[] buffer = new byte[width * 3 + outputStreamRowPadding];
            byteArrayOutputStream.write(buffer);
        }

        for (int i = 0; i < randomAccessFileMeaningfulHeight; i++) {
            byte[] buffer = new byte[width * 3];
            randomAccessFile.seek(startOffsetRandomAccessFile);
            randomAccessFile.read(buffer, offset, newBufferLength - offset);
            byteArrayOutputStream.write(buffer);
            byte[] bufferOutputStreamRowPadding = new byte[outputStreamRowPadding];
            byteArrayOutputStream.write(bufferOutputStreamRowPadding);
            startOffsetRandomAccessFile += picture.getWidth() * 3 + randomAccessFileRowPadding;
        }

        randomAccessFile.close();
        byteArrayOutputStream.close();
        return byteArrayOutputStream;
    }

    private void writeIntLE(byte[] bytes, int startoffset, long value) {
        bytes[startoffset] = (byte) (value);
        bytes[startoffset + 1] = (byte) (value >>> 8);
        bytes[startoffset + 2] = (byte) (value >>> 16);
        bytes[startoffset + 3] = (byte) (value >>> 24);
    }
}
