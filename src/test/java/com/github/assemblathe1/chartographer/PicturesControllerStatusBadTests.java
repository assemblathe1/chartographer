package com.github.assemblathe1.chartographer;

import com.github.assemblathe1.chartographer.entities.Picture;
import com.github.assemblathe1.chartographer.repositories.PicturesRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PicturesControllerStatusBadTests {
    @Autowired
    private MockMvc mvc;
    Picture picture = new Picture();
    int restoringOrReturningPicturePartWidth;
    int restoringOrReturningPicturePartHeight;
    int maxValidWidth;
    int maxValidHeight;

    @PostConstruct
    public void postConstruct() {
        picture.setId(1L);
        maxValidWidth = 20000;
        maxValidHeight = 50000;
        picture.setWidth(maxValidWidth);
        picture.setHeight(maxValidHeight);
        restoringOrReturningPicturePartWidth = 31;
        restoringOrReturningPicturePartHeight = 26;
    }

    @MockBean
    private PicturesRepository picturesRepository;

    private String getTestFile(String fileName) {
        return getClass().getClassLoader().getResource("pictures/" + fileName).getPath();
    }

    @Test
    public void givenPicture_whenSaveNewPicture_thenStatus400() throws Exception {
        sendCreatePictureRequest(maxValidWidth + 1, maxValidHeight);
        sendCreatePictureRequest(maxValidWidth, maxValidHeight + 1);
        sendCreatePictureRequest(-1, maxValidHeight);
        sendCreatePictureRequest(maxValidWidth, - 1);
    }

    private void sendCreatePictureRequest(int width, int height) throws Exception {
        mvc
                .perform(post("/chartas/{width}&{height}", width, height))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void givenId_whenSaveMultipartPictureFragment_thenStatus400() throws Exception {
        FileInputStream fileInputStream = new FileInputStream(getTestFile("whenSaveMultipartPictureFragment.bmp"));
        MockMultipartFile pictureFragment = new MockMultipartFile("file", "whenSaveMultipartPictureFragment.bmp",
                String.valueOf(MediaType.valueOf("image/bmp")), fileInputStream);
        given(picturesRepository.findById(Mockito.anyLong())).willReturn(Optional.of(picture));

        sendSavePictureFragmentRequest(pictureFragment, -restoringOrReturningPicturePartWidth - 1, 0, restoringOrReturningPicturePartWidth, restoringOrReturningPicturePartHeight);
        sendSavePictureFragmentRequest(pictureFragment, 0, -restoringOrReturningPicturePartHeight - 1, restoringOrReturningPicturePartWidth, restoringOrReturningPicturePartHeight);
        sendSavePictureFragmentRequest(pictureFragment, picture.getWidth() + 1, 0, restoringOrReturningPicturePartWidth, restoringOrReturningPicturePartHeight);
        sendSavePictureFragmentRequest(pictureFragment, 0, picture.getHeight() + 1, restoringOrReturningPicturePartWidth, restoringOrReturningPicturePartHeight);
        sendSavePictureFragmentRequest(pictureFragment, 0, 0, maxValidWidth + 1, restoringOrReturningPicturePartHeight);
        sendSavePictureFragmentRequest(pictureFragment, 0, 0, restoringOrReturningPicturePartWidth, maxValidHeight + 1);
    }

    private void sendSavePictureFragmentRequest(MockMultipartFile pictureFragment, int x, int y, int restoringPicturePartWidth, int restoringPicturePartHeight) throws Exception {
        mvc
                .perform(multipart("/chartas/{id}/", picture.getId())
                        .file(pictureFragment)
                        .param("x", String.valueOf(x))
                        .param("y", String.valueOf(y))
                        .param("width", String.valueOf(restoringPicturePartWidth))
                        .param("height", String.valueOf(restoringPicturePartHeight)
                        )
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void givenId_whenGetMultipartPictureFragment_thenStatus400() throws Exception {
        given(picturesRepository.findById(Mockito.anyLong())).willReturn(Optional.of(picture));
        sendGetPictureFragmentRequest(-restoringOrReturningPicturePartWidth - 1, 0, restoringOrReturningPicturePartWidth, restoringOrReturningPicturePartHeight);
        sendGetPictureFragmentRequest(0, -restoringOrReturningPicturePartHeight - 1, restoringOrReturningPicturePartWidth, restoringOrReturningPicturePartHeight);
        sendGetPictureFragmentRequest(picture.getWidth() + 1, 0, restoringOrReturningPicturePartWidth, restoringOrReturningPicturePartHeight);
        sendGetPictureFragmentRequest(0, picture.getHeight() + 1, restoringOrReturningPicturePartWidth, restoringOrReturningPicturePartHeight);
        sendGetPictureFragmentRequest(0, 0, maxValidWidth + 1, restoringOrReturningPicturePartHeight);
        sendGetPictureFragmentRequest(0, 0, restoringOrReturningPicturePartWidth, maxValidHeight + 1);
    }

    private void sendGetPictureFragmentRequest(int x, int y, int returningPicturePartWidth, int returningPicturePartHeight) throws Exception {
        mvc
                .perform(get("/chartas/{id}/", picture.getId())
                        .param("x", String.valueOf(x))
                        .param("y", String.valueOf(y))
                        .param("width", String.valueOf(returningPicturePartWidth))
                        .param("height", String.valueOf(returningPicturePartHeight)
                        )
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void givenId_whenDeleteNotExistingPicture_thenStatus404() throws Exception {
        mvc
                .perform(delete("/chartas/{id}/", 99999999L))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}
