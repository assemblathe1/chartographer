package com.github.assemblathe1.chartographer;

import com.github.assemblathe1.chartographer.entities.Picture;
import com.github.assemblathe1.chartographer.repositories.PicturesRepository;
import com.github.assemblathe1.chartographer.services.PicturesService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    @PostConstruct
    public void postConstruct() {
        picture.setId(1L);
        picture.setWidth(51);
        picture.setHeight(102);
        pictureByteSize = 54 + picture.getWidth() * picture.getHeight() * 3L + picture.getHeight() * (picture.getWidth() * 3 % 4 == 0 ? 0 : 4 - (picture.getWidth() * 3 % 4));
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
    public void givenId_whenSaveMultipartPictureFragment_thenStatus200() throws Exception {
        File source = new File(getTestFile("whenSaveMultipartPicture.bmp"));
        File copied = new File(tmpdir + "whenSaveMultipartPicture.bmp");
        picture.setUrl(copied.getAbsolutePath());
        Files.deleteIfExists(copied.toPath());
        assertThat(copied).doesNotExist();
        assertThat(source).exists().hasSize(pictureByteSize);
        FileUtils.copyFile(source, copied);
        assertThat(copied).exists();
        assertThat(Files.readAllLines(source.toPath()).equals(Files.readAllLines(copied.toPath())));
        FileInputStream fileInputStream = new FileInputStream(getTestFile("whenSaveMultipartPictureFragment.bmp"));
        MockMultipartFile pictureFragment = new MockMultipartFile("file", "whenSaveMultipartPictureFragment.bmp",
                String.valueOf(MediaType.valueOf("image/bmp")), fileInputStream);

        int restoringPicturePartWidth = 31;
        int restoringPicturePartHeight = 26;

        given(picturesRepository.findById(Mockito.anyLong())).willReturn(Optional.of(picture));
        mvc
                .perform(multipart("/chartas/{id}/", picture.getId())
                        .file(pictureFragment)
                        .param("x", String.valueOf(0))
                        .param("y", String.valueOf(0))
                        .param("width", String.valueOf(restoringPicturePartWidth))
                        .param("height", String.valueOf(restoringPicturePartHeight)
                        )
                ).andDo(print())
                .andExpect(status().isOk());
        assertThat(copied).exists();
    }

    @Test
    public void givenId_whenGetMultipartPictureFragment_thenStatus200andMultipartPictureFragmentReturns() throws Exception {
        String sourcePicture = getTestFile("whenGetMultipartPictureFragment.bmp");
        picture.setUrl(sourcePicture);
        assertThat(new File(sourcePicture)).exists().hasSize(pictureByteSize);

        given(picturesRepository.findById(Mockito.anyLong())).willReturn(Optional.of(picture));
        int returningPictureWidth = 31;
        int returningPictureHeight = 26;
        long returningPictureByteSize = 54 + returningPictureWidth * returningPictureHeight * 3L + returningPictureHeight * (returningPictureWidth * 3 % 4 == 0 ? 0 : 4 - (returningPictureWidth * 3 % 4));

        mvc
                .perform(get("/chartas/{id}/", picture.getId())
                        .param("x", String.valueOf(0))
                        .param("y", String.valueOf(0))
                        .param("width", String.valueOf(returningPictureWidth))
                        .param("height", String.valueOf(returningPictureHeight)
                        )
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.valueOf("image/bmp")))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, String.valueOf(returningPictureByteSize)));
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
