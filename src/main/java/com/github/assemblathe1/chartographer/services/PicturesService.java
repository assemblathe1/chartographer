package com.github.assemblathe1.chartographer.services;

import com.github.assemblathe1.chartographer.entities.Picture;
import com.github.assemblathe1.chartographer.exceptions.ResourceNotFoundException;
import com.github.assemblathe1.chartographer.exceptions.WritingToDiskException;
import com.github.assemblathe1.chartographer.repositories.PicturesRepository;
import com.github.assemblathe1.chartographer.utils.PictureByteUtility;
import com.github.assemblathe1.chartographer.utils.StartupArgumentsRunner;
import com.github.assemblathe1.chartographer.validators.PictureValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PicturesService {
    @Value("${upload.maxPictureWidth}")
    private int maxPictureWidth;
    @Value("${upload.maxPictureHeight}")
    private int maxPictureHeight;
    @Value("${upload.maxFragmentWidth}")
    private int maxFragmentWidth;
    @Value("${upload.maxFragmentHeight}")
    private int maxFragmentHeight;

    private final PicturesRepository picturesRepository;
    private final PictureValidator pictureValidator;
    private final StartupArgumentsRunner startupArgumentsRunner;
    private final PictureByteUtility pictureByteUtility;

    @Transactional
    public Long createPicture(int width, int height) {
        String picturesFolder = startupArgumentsRunner.getFolder();
        String url = picturesFolder.length() > 2
                ? createBMPFilePath(picturesFolder.substring(1, picturesFolder.length() - 1))
                : createBMPFilePath(System.getProperty("java.io.tmpdir") + "/pictures");

        System.out.println(System.getProperty("user.dir"));
        System.out.println(PicturesService.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        System.out.println(url);
        System.out.println("!!!   " + Path.of(url).toFile().getAbsolutePath());

        pictureValidator.validate(null, null, width, height, maxPictureWidth, maxPictureHeight, null, null);
        Picture savedPicture = picturesRepository.save(new Picture(url, width, height));
        try {
            pictureByteUtility.createPicture(savedPicture.getWidth(), savedPicture.getHeight(), savedPicture.getUrl());
        } catch (IOException e) {
            picturesRepository.deleteById(savedPicture.getId());
            e.getMessage();
            throw new WritingToDiskException("Internal Server Error");
        }
        return savedPicture.getId();
    }

    public void savePictureFragment(String id, int x, int y, int width, int height, MultipartFile pictureFragment) {
        Picture picture = findPictureById(id);
        pictureValidator.validate(x, y, width, height, maxPictureWidth, maxPictureHeight, picture.getWidth(), picture.getHeight());
        try {
            pictureByteUtility.savePictureFragment(x, y, width, height, pictureFragment, picture);
        } catch (IOException e) {
            throw new WritingToDiskException("Internal Server Error");
        }
    }

    public ByteArrayOutputStream getPictureFragment(String id, int x, int y, int width, int height) {
        Picture picture = findPictureById(id);
        pictureValidator.validate(x, y, width, height, maxFragmentWidth, maxFragmentHeight, picture.getWidth(), picture.getHeight());
        ByteArrayOutputStream byteArrayOutputStream;
        try {
            byteArrayOutputStream = pictureByteUtility.getPictureFragment(x, y, width, height, picture);
        } catch (IOException e) {
            throw new WritingToDiskException("Internal Server Error");
        }
        return byteArrayOutputStream;
    }

    //TODO нет проверки на то что удаляемый файл уже удален - мб и не надо, если нет ошибок
    public void deletePicture(String id) {
        Picture picture = findPictureById(id);
        if (pictureByteUtility.deletePicture(picture)) {
            picturesRepository.deleteById(Long.valueOf(id));
        }
    }

    public Picture findPictureById(String id) {
        return picturesRepository
                .findById(Long.valueOf(id))
                .orElseThrow(() -> new ResourceNotFoundException("Picture with id " + id + " was not found"));
    }

    private String createBMPFilePath(String savingFolder) {
        File defaultPicturesDirectory = new File(savingFolder);
        if (!defaultPicturesDirectory.exists()) defaultPicturesDirectory.mkdir();
        System.out.println("createBMPFileDefaultPath " + defaultPicturesDirectory.getPath());
        return Path.of(defaultPicturesDirectory.getPath() + "/" + UUID.randomUUID() + ".bmp").toString();
    }
}
