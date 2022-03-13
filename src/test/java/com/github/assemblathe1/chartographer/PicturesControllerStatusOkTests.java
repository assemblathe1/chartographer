package com.github.assemblathe1.chartographer;

import com.github.assemblathe1.chartographer.entities.Picture;
import com.github.assemblathe1.chartographer.repositories.PicturesRepository;
import com.github.assemblathe1.chartographer.utils.PictureByteHandler;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PicturesControllerStatusOkTests {
    @Autowired
    private MockMvc mvc;
    Picture picture = new Picture();
    long pictureByteSize;

    @PostConstruct
    public void postConstruct() {
        picture.setId(1L);
        picture.setWidth(51);
        picture.setHeight(102);
        pictureByteSize = 54 + picture.getWidth() * picture.getHeight() * 3L + picture.getHeight() * (picture.getWidth() * 3 % 4 == 0 ? 0 : 4 - (picture.getWidth() * 3 % 4));
    }

    @MockBean
    private PicturesRepository picturesRepository;

    @MockBean
    private PictureByteHandler pictureByteHandler;

    private final String tmpdir = System.getProperty("java.io.tmpdir");
    private final int restoringPictureFragmentWidth = 31;
    private final int restoringPictureFragmentHeight = 26;

    private String getTestFile(String fileName) {
        return getClass().getClassLoader().getResource("pictures/" + fileName).getPath();
    }

    @Test
    public void givenPicture_whenSaveNewPicture_thenStatus201andIDReturns() throws Exception {
        given(picturesRepository.save(Mockito.any())).willReturn(picture);
        Mockito.doNothing().when(pictureByteHandler).createPicture(Mockito.any(), Mockito.any(), Mockito.any());
        mvc
                .perform(post("/chartas/")
                        .param("width", picture.getWidth().toString())
                        .param("height", picture.getHeight().toString())
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().string(picture.getId().toString()));
    }

    @Test
    public void givenId_whenSaveMultipartPictureFragment_thenStatus200() throws Exception {
        FileInputStream fileInputStream = new FileInputStream(getTestFile("whenSaveMultipartPictureFragment.bmp"));
        MockMultipartFile pictureFragment = new MockMultipartFile("file", "whenSaveMultipartPictureFragment.bmp",
                String.valueOf(MediaType.valueOf("image/bmp")), fileInputStream);
        fileInputStream.close();

        given(picturesRepository.findById(Mockito.anyLong())).willReturn(Optional.of(picture));
        Mockito.doNothing().when(pictureByteHandler).savePictureFragment(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),Mockito.any());
        mvc
                .perform(multipart("/chartas/{id}/", picture.getId())
                        .file(pictureFragment)
                        .param("x", String.valueOf(0))
                        .param("y", String.valueOf(0))
                        .param("width", String.valueOf(restoringPictureFragmentWidth))
                        .param("height", String.valueOf(restoringPictureFragmentHeight)
                        )
                ).andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void givenId_whenGetMultipartPictureFragment_thenStatus200andMultipartPictureFragmentReturns() throws Exception {
        // Подготавливаем файл для тела ответа
        String sourcePictureFile = getTestFile("whenSaveMultipartPictureFragment.bmp");
        assertThat(new File(sourcePictureFile)).exists();
        FileInputStream fileInputStream = new FileInputStream(getTestFile("whenSaveMultipartPictureFragment.bmp"));
        int sourceFileByteLength = fileInputStream.available();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(fileInputStream.readAllBytes());
        fileInputStream.close();
        byteArrayOutputStream.close();

        given(picturesRepository.findById(Mockito.anyLong())).willReturn(Optional.of(picture));
        Mockito.doReturn(byteArrayOutputStream).when(pictureByteHandler).getPictureFragment(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        mvc
                .perform(get("/chartas/{id}/", picture.getId())
                        .param("x", String.valueOf(0))
                        .param("y", String.valueOf(0))
                        .param("width", String.valueOf(restoringPictureFragmentWidth))
                        .param("height", String.valueOf(restoringPictureFragmentHeight)
                        )
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.valueOf("image/bmp")))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, String.valueOf(sourceFileByteLength)));

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
