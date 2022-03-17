package com.github.assemblathe1.chartographer;

import com.github.assemblathe1.chartographer.entities.Picture;
import com.github.assemblathe1.chartographer.repositories.PicturesRepository;
import com.github.assemblathe1.chartographer.services.PicturesService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@AutoConfigureMockMvc
public class PicturesServiceTest {

    @Autowired
    private PicturesService picturesService;

    @MockBean
    private PicturesRepository picturesRepository;

    Picture picture = new Picture();
    long pictureByteSize;
    int restoringFragmentWidth = 31;
    int restoringFragmentHeight = 26;

    @PostConstruct
    public void postConstruct() {
        picture.setId(1L);
        picture.setWidth(51);
        picture.setHeight(102);
        pictureByteSize = getPictureByteSize(picture.getWidth(), picture.getHeight());
    }

    private final String tmpdir = System.getProperty("java.io.tmpdir");

    @Test
    public void createPictureTest() throws Exception {
        String createdPicture = tmpdir + "whenSaveNewPicture.bmp";
        File createdBMPFile = new File(createdPicture);
        picture.setUrl(createdPicture);

        Files.deleteIfExists(createdBMPFile.toPath());
        assertThat(createdBMPFile).doesNotExist();
        given(picturesRepository.save(Mockito.any(Picture.class))).willReturn(picture);

        picturesService.createPicture(picture.getWidth(), picture.getHeight());
        assertThat(createdBMPFile).exists().hasSize(pictureByteSize);
        assertEquals(URLConnection.guessContentTypeFromName(createdBMPFile.getName()), "image/bmp");

        BufferedImage bufferedImage = ImageIO.read(createdBMPFile);
        assertEquals(bufferedImage.getWidth(), picture.getWidth());
        assertEquals(bufferedImage.getHeight(), picture.getHeight());
        for (int i = 0; i < 10; i++) {
            assertEquals(bufferedImage.getRGB(new Random()
                            .ints(1, picture.getWidth() - 1)
                            .findFirst()
                            .getAsInt(),
                    new Random().ints(1, picture.getHeight() - 1)
                            .findFirst()
                            .getAsInt()), new Color(0, 0, 0)
                            .getRGB());
        }
    }

    @Test
    public void savePictureFragmentTest() throws Exception {
        // Приверяем и копируем папирус в папку temp
        File sourcePicture = new File(getTestFile("whenSaveMultipartPicture.bmp"));
        File copiedPicture = new File(tmpdir + "whenSaveMultipartPicture.bmp");
        picture.setUrl(copiedPicture.getAbsolutePath());
        Files.deleteIfExists(copiedPicture.toPath());
        assertThat(copiedPicture).doesNotExist();
        assertThat(sourcePicture).exists().hasSize(pictureByteSize);
        FileUtils.copyFile(sourcePicture, copiedPicture);
        assertThat(copiedPicture).exists();
        assertThat(Files.readAllLines(sourcePicture.toPath()).equals(Files.readAllLines(copiedPicture.toPath())));
        given(picturesRepository.findById(Mockito.anyLong())).willReturn(Optional.of(picture));

        // Проверка корректности отпавляемого фрагмента изображения
        File restoringFragment = new File(getTestFile("whenSaveMultipartPictureFragment.bmp"));
        FileInputStream fileInputStream = new FileInputStream(restoringFragment);
        MockMultipartFile pictureFragment = new MockMultipartFile("file", "whenSaveMultipartPictureFragment.bmp",
                String.valueOf(MediaType.valueOf("image/bmp")), fileInputStream);
        BufferedImage bufferedFragment = ImageIO.read(restoringFragment);
        assertEquals(bufferedFragment.getWidth(), restoringFragmentWidth);
        assertEquals(bufferedFragment.getHeight(), restoringFragmentHeight);
        fileInputStream.close();

        String pictureId = picture.getId().toString();
        // Сохранение фрагмента с нужными координатами х и у
        // y < 0
        runSavePictureFragment(pictureId, -26, -10, pictureFragment);
        runSavePictureFragment(pictureId, 10, -10, pictureFragment);
        runSavePictureFragment(pictureId, 46, -10, pictureFragment);
        // y >=0 && y >= width of papyrus
        runSavePictureFragment(pictureId, -26, 38, pictureFragment);
        runSavePictureFragment(pictureId, 10, 38, pictureFragment);
        runSavePictureFragment(pictureId, 46, 38, pictureFragment);
        // y >= width of papyrus
        runSavePictureFragment(pictureId, -26, 86, pictureFragment);
        runSavePictureFragment(pictureId, 10, 86, pictureFragment);
        runSavePictureFragment(pictureId, 46, 86, pictureFragment);

        // Проверка корректности папируса после восстановления фрагмента
        assertThat(copiedPicture).exists().hasSize(pictureByteSize);
        BufferedImage bufferedPicture = ImageIO.read(copiedPicture);
        assertEquals(bufferedPicture.getWidth(), picture.getWidth());
        assertEquals(bufferedPicture.getHeight(), picture.getHeight());

        //проверка корректности сохранения фрагмента
        // y < 0
        checkSavedFragmentsInPapyrus(-26, -10, 27, 11, bufferedPicture, bufferedFragment);
        checkSavedFragmentsInPapyrus(10, -10, 1, 11, bufferedPicture, bufferedFragment);
        checkSavedFragmentsInPapyrus(46, -10, 1, 11, bufferedPicture, bufferedFragment);
        // y >=0 && y >= width of papyrus
        checkSavedFragmentsInPapyrus(-26, 38, 27, 1, bufferedPicture, bufferedFragment);
        checkSavedFragmentsInPapyrus(10, 38, 1, 1, bufferedPicture, bufferedFragment);
        checkSavedFragmentsInPapyrus(46, 38, 1, 1, bufferedPicture, bufferedFragment);
        // y >= width of papyrus
        checkSavedFragmentsInPapyrus(-26, 86, 27, 1, bufferedPicture, bufferedFragment);
        checkSavedFragmentsInPapyrus(10, 86, 1, 1, bufferedPicture, bufferedFragment);
        checkSavedFragmentsInPapyrus(46, 86, 1, 1, bufferedPicture, bufferedFragment);
    }

    private void checkSavedFragmentsInPapyrus(int x, int y, int testColorX, int testColorY, BufferedImage bufferedPicture, BufferedImage bufferedFragment) {
        assertEquals(
                bufferedPicture.getRGB(
                        testColorX + x,
                        testColorY + y
                ),
                bufferedFragment.getRGB(
                        testColorX,
                        testColorY
                )
        );
    }

    @Test
    public void getPictureFragmentTest() throws Exception {
        // Подготавливаем и проверяем тестовый папирус
        String sourcePicture = getTestFile("whenGetMultipartPictureFragment.bmp");
        BufferedImage bufferedPicture = ImageIO.read(new File(sourcePicture));
        picture.setUrl(sourcePicture);
        assertThat(new File(sourcePicture)).exists().hasSize(pictureByteSize);

        given(picturesRepository.findById(Mockito.anyLong())).willReturn(Optional.of(picture));
        int currentFragmentWidth;
        // Проверяем соответствие цветов пикселей исходного изображения и полученного фрагмента
        // y < 0
        checkReturnedFragmentsOfPapyrus(-26, -10, 27, 11, bufferedPicture);
        checkReturnedFragmentsOfPapyrus(10, -10, 1, 11, bufferedPicture);
        checkReturnedFragmentsOfPapyrus(46, -10, 1, 11, bufferedPicture);
        // y >=0 && y >= width of papyrus
        checkReturnedFragmentsOfPapyrus(-26, 38, 27, 1, bufferedPicture);
        checkReturnedFragmentsOfPapyrus(10, 38, 1, 1, bufferedPicture);
        checkReturnedFragmentsOfPapyrus(46, 38, 1, 1, bufferedPicture);
        // y >= width of papyrus
        checkReturnedFragmentsOfPapyrus(-26, 86, 27, 1, bufferedPicture);
        checkReturnedFragmentsOfPapyrus(10, 86, 1, 1, bufferedPicture);
        checkReturnedFragmentsOfPapyrus(46, 86, 1, 1, bufferedPicture);
    }

    private void checkReturnedFragmentsOfPapyrus(int x, int y, int testColorX, int testColorY, BufferedImage bufferedPicture) throws IOException {
        // Получаем и проверяем полученный фрагмент
        byte[] byteArray = picturesService.getPictureFragment(picture.getId().toString(), x, y, restoringFragmentWidth, restoringFragmentHeight).toByteArray();
        assertEquals(byteArray.length, getPictureByteSize(restoringFragmentWidth, restoringFragmentHeight));
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        BufferedImage bufferedFragment = ImageIO.read(byteArrayInputStream);
        byteArrayInputStream.close();
        assertEquals(bufferedFragment.getHeight(), restoringFragmentHeight);
        assertEquals(bufferedFragment.getWidth(), restoringFragmentWidth);
        assertEquals(
                bufferedPicture.getRGB(
                        testColorX + x,
                        testColorY + y
                ),
                bufferedFragment.getRGB(
                        testColorX,
                        testColorY
                )
        );
    }

    @Test
    public void deletePictureTest() throws Exception {
        //Копируем тестовый файл в temp
        File source = new File(getTestFile("whenDeletePicture.bmp"));
        File copied = new File(tmpdir + "whenDeletePicture.bmp");
        picture.setUrl(copied.getAbsolutePath());
        Files.deleteIfExists(copied.toPath());
        assertThat(copied).doesNotExist();
        assertThat(source).exists();
        FileUtils.copyFile(source, copied);
        assertThat(copied).exists();
        assertThat(Files.readAllLines(source.toPath()).equals(Files.readAllLines(copied.toPath())));

        given(picturesRepository.findById(Mockito.anyLong())).willReturn(Optional.of(picture));
        doNothing().when(picturesRepository).deleteById(Mockito.anyLong());
        picturesService.deletePicture(picture.getId().toString());
        assertThat(copied).doesNotExist();
    }

    private String getTestFile(String fileName) {
        return getClass().getClassLoader().getResource("pictures/" + fileName).getPath();
    }

    private long getPictureByteSize(int width, int height) {
        return 54 + width * height * 3L + height * (width * 3 % 4 == 0 ? 0 : 4 - (width * 3 % 4));
    }

    private void runSavePictureFragment(String pictureId, int x, int y, MockMultipartFile pictureFragment) {
        picturesService.savePictureFragment(pictureId, x, y, restoringFragmentWidth, restoringFragmentHeight, pictureFragment);
    }
}
