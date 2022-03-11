package com.github.assemblathe1.chartographer.services;

import com.github.assemblathe1.chartographer.entities.Picture;
import com.github.assemblathe1.chartographer.exceptions.ResourceNotFoundException;
import com.github.assemblathe1.chartographer.exceptions.WritingToDiskException;
import com.github.assemblathe1.chartographer.repositories.PicturesRepository;
import com.github.assemblathe1.chartographer.utils.PictureByteHandler;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PicturesService {
    @Value("${upload.path}")
    private String picturesDirectory;
    private final PicturesRepository picturesRepository;
    private final PictureValidator pictureValidator;
    private final StartupArgumentsRunner startupArgumentsRunner;
    private final PictureByteHandler pictureByteHandler;

    @Transactional
    public Long createPicture(Integer width, Integer height) {
        String picturesFolder = startupArgumentsRunner.getFolder();
        String url = picturesFolder.length() > 2 ? picturesFolder.substring(2, picturesFolder.length()-1) + "/" + UUID.randomUUID() + ".bmp" : picturesDirectory + UUID.randomUUID() + ".bmp";
        pictureValidator.validate(0, 0, width, height, 20000, 50000, width, height);
        Picture savedPicture = picturesRepository.save(new Picture(url, width, height));
        try {
            if (savedPicture.getId() != null) pictureByteHandler.createPicture(savedPicture.getWidth(), savedPicture.getHeight(), savedPicture.getUrl());
        } catch (IOException e) {
            picturesRepository.deleteById(savedPicture.getId());
            throw new WritingToDiskException("Internal Server Error");
        }
        return savedPicture.getId();
    }

    public void savePictureFragment(String id, Integer x, Integer y, Integer width, Integer height, MultipartFile pictureFragment) {
        Picture picture = findPictureById(id);
        pictureValidator.validate(x, y, width, height, 20000, 50000, picture.getWidth(), picture.getHeight());
        try {
            pictureByteHandler.restorePictureFragment(x, y, width, height, pictureFragment, picture);
        } catch (IOException e) {
            throw new WritingToDiskException("Internal Server Error");
        }
    }

    public ByteArrayOutputStream getPictureFragment(String id, Integer x, Integer y, Integer width, Integer height) {
        Picture picture = findPictureById(id);
        pictureValidator.validate(x, y, width, height, 5000, 5000, picture.getWidth(), picture.getHeight());
        ByteArrayOutputStream byteArrayOutputStream;
        try {
            byteArrayOutputStream = pictureByteHandler.getPictureFragment(x, y, width, height, picture);
        } catch (IOException e) {
            throw new WritingToDiskException("Internal Server Error");
        }
        return byteArrayOutputStream;
    }

    //TODO нет проверки на то что удаляемый файл уже удален - мб и не надо, если нет ошибок
    public void deletePicture(String id) {
        Picture picture = findPictureById(id);
        String pictureUrl = picture.getUrl();
        if (pictureUrl != null) {
            picturesRepository.deleteById(Long.valueOf(id));
            new File(pictureUrl).delete();
        }
    }

    public Picture findPictureById(String id) {
        return picturesRepository
                .findById(Long.valueOf(id))
                .orElseThrow(() -> new ResourceNotFoundException("Picture with id " + id + " was not found"));
    }
}
