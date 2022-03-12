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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PicturesServiceTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private PicturesService picturesService;

    @MockBean
    private PicturesRepository picturesRepository;

    Picture picture = new Picture();
    long pictureByteSize;
    int restoringFragmentWidth = 31;
    int restoringFragmentHeight = 26;
    int x = 0;
    int y = 0;

    @PostConstruct
    public void postConstruct() {
        picture.setId(1L);
        picture.setWidth(51);
        picture.setHeight(102);
        pictureByteSize = getPictureByteSize(picture.getWidth(), picture.getHeight());
    }

    private long getPictureByteSize(int width, int height) {
        return 54 + width * height * 3L + height * (width * 3 % 4 == 0 ? 0 : 4 - (width * 3 % 4));
    }

    private final String tmpdir = System.getProperty("java.io.tmpdir");

    private String getTestFile(String fileName) {
        return getClass().getClassLoader().getResource("pictures/" + fileName).getPath();
    }

    @Test
    public void createPictureTest() throws Exception {
        String createdPicture = tmpdir + "whenSaveNewPicture.bmp";
        File createdBMPFile = new File(createdPicture);
        picture.setUrl(createdPicture);

        Files.deleteIfExists(createdBMPFile.toPath());
        assertThat(createdBMPFile).doesNotExist();
        given(picturesRepository.save(Mockito.any())).willReturn(picture);

        picturesService.createPicture(picture.getWidth(), picture.getHeight());
        assertThat(createdBMPFile).exists().hasSize(pictureByteSize);
        assertEquals(URLConnection.guessContentTypeFromName(createdBMPFile.getName()), "image/bmp");

        BufferedImage bufferedImage = ImageIO.read(createdBMPFile);
        assertEquals(bufferedImage.getWidth(), picture.getWidth());
        assertEquals(bufferedImage.getHeight(), picture.getHeight());
        for (int i = 0; i < 10; i++) {
            assertEquals(bufferedImage.getRGB(new Random().ints(1, picture.getWidth() - 1).findFirst().getAsInt(), new Random().ints(1, picture.getHeight() - 1).findFirst().getAsInt()), new Color(0, 0, 0).getRGB());
        }
        Files.delete(createdBMPFile.toPath());
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
        given(picturesRepository.findById(Mockito.any())).willReturn(Optional.of(picture));

        // Проверка корректности воссстанавливаемого фрагмента изображения
        File restoringFragment = new File(getTestFile("whenSaveMultipartPictureFragment.bmp"));
        FileInputStream fileInputStream = new FileInputStream(restoringFragment);
        MockMultipartFile pictureFragment = new MockMultipartFile("file", "whenSaveMultipartPictureFragment.bmp",
                String.valueOf(MediaType.valueOf("image/bmp")), fileInputStream);
        BufferedImage bufferedFragment = ImageIO.read(restoringFragment);
        assertEquals(bufferedFragment.getWidth(), restoringFragmentWidth);
        assertEquals(bufferedFragment.getHeight(), restoringFragmentHeight);
        fileInputStream.close();

        // Сохранение фрагмента с нужными координатами х и у
        picturesService.savePictureFragment(picture.getId().toString(), x, y, restoringFragmentWidth, restoringFragmentHeight, pictureFragment);

        // Проверка корректности папируса после восстановления фрагмента
        assertThat(copiedPicture).exists().hasSize(pictureByteSize);
        BufferedImage bufferedPicture = ImageIO.read(copiedPicture);
        assertEquals(bufferedPicture.getWidth(), picture.getWidth());
        assertEquals(bufferedPicture.getHeight(), picture.getHeight());
        for (int i = 0; i < 10; i++) {
            int testColorX = new Random().ints(1, restoringFragmentWidth - 1).findFirst().getAsInt();
            int testColorY = new Random().ints(1, restoringFragmentHeight - 1).findFirst().getAsInt();
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
    }

    @Test
    public void getPictureFragmentTest() throws Exception {
        // Подготавливаем и проверяем тестовый папирус
        String sourcePicture = getTestFile("whenGetMultipartPictureFragment.bmp");
        picture.setUrl(sourcePicture);
        assertThat(new File(sourcePicture)).exists().hasSize(pictureByteSize);
        FileInputStream fileInputStream = new FileInputStream(sourcePicture);
        BufferedImage bufferedImage = ImageIO.read(fileInputStream);
        fileInputStream.close();


        long returningPictureByteSize = getPictureByteSize(restoringFragmentWidth, restoringFragmentHeight);
        given(picturesRepository.findById(Mockito.anyLong())).willReturn(Optional.of(picture));

        // Получаем и проверяем полученный фрагмент
        byte[] byteArray = picturesService.getPictureFragment(picture.getId().toString(), x, y, restoringFragmentWidth, restoringFragmentHeight).toByteArray();
        assertEquals(byteArray.length, returningPictureByteSize);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        BufferedImage bufferedFragment = ImageIO.read(byteArrayInputStream);
        byteArrayInputStream.close();

        assertEquals(bufferedFragment.getHeight(), restoringFragmentHeight);
        assertEquals(bufferedFragment.getWidth(), restoringFragmentWidth);

        for (int i = 0; i < 10; i++) {
            int testColorX = new Random().ints(1, restoringFragmentWidth - 1).findFirst().getAsInt();
            int testColorY = new Random().ints(1, restoringFragmentHeight - 1).findFirst().getAsInt();
            assertEquals(
                    bufferedImage.getRGB(
                            testColorX + x,
                            testColorY + y
                    ),
                    bufferedFragment.getRGB(
                            testColorX,
                            testColorY
                    )
            );
        }
    }

    @Test
    public void givenId_whenDeletePicture_thenStatus200() throws Exception {
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
        mvc
                .perform(delete("/chartas/{id}/", picture.getId()))
                .andDo(print())
                .andExpect(status().isOk());

        assertThat(copied).doesNotExist();
    }
}
