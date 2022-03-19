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

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PicturesControllerStatusBadTests {

    @Autowired
    private MockMvc mvc;
    Picture picture = new Picture();
    int fragmentWidth;
    int fragmentHeight;
    int maxValidPictureWidth;
    int maxValidPictureHeight;

    @PostConstruct
    public void postConstruct() {
        picture.setId(1L);
        maxValidPictureWidth = 20000;
        maxValidPictureHeight = 50000;
        picture.setWidth(maxValidPictureWidth);
        picture.setHeight(maxValidPictureHeight);
        fragmentWidth = 31;
        fragmentHeight = 26;
    }

    @MockBean
    private PicturesRepository picturesRepository;

    private String getTestFile(String fileName) {
        return getClass().getClassLoader().getResource("pictures/" + fileName).getPath();
    }

    @Test
    public void givenPicture_whenCreateNewPicture_thenStatus400() throws Exception {
        sendCreatePictureRequest(maxValidPictureWidth + 1, maxValidPictureHeight);
        sendCreatePictureRequest(maxValidPictureWidth, maxValidPictureHeight + 1);
        sendCreatePictureRequest(-1, maxValidPictureHeight);
        sendCreatePictureRequest(maxValidPictureWidth, -1);
        sendCreatePictureRequest(-1, -1);
    }

    private void sendCreatePictureRequest(int width, int height) throws Exception {
        mvc
                .perform(post("/chartas/")
                        .param("width", String.valueOf(width))
                        .param("height", String.valueOf(height))
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void givenId_whenSaveMultipartPictureFragment_thenStatus400() throws Exception {
        FileInputStream fileInputStream = new FileInputStream(getTestFile("whenSaveMultipartPictureFragment.bmp"));
        MockMultipartFile pictureFragment = new MockMultipartFile(
                "file",
                "whenSaveMultipartPictureFragment.bmp",
                String.valueOf(MediaType.valueOf("image/bmp")),
                fileInputStream
        );
        given(picturesRepository.findById(Mockito.anyLong())).willReturn(Optional.of(picture));

        sendSavePictureFragmentRequest(pictureFragment, -fragmentWidth - 1, 0, fragmentWidth, fragmentHeight);
        sendSavePictureFragmentRequest(pictureFragment, 0, -fragmentHeight - 1, fragmentWidth, fragmentHeight);
        sendSavePictureFragmentRequest(pictureFragment, picture.getWidth() + 1, 0, fragmentWidth, fragmentHeight);
        sendSavePictureFragmentRequest(pictureFragment, 0, picture.getHeight() + 1, fragmentWidth, fragmentHeight);
        sendSavePictureFragmentRequest(pictureFragment, 0, 0, maxValidPictureWidth + 1, fragmentHeight);
        sendSavePictureFragmentRequest(pictureFragment, 0, 0, fragmentWidth, maxValidPictureHeight + 1);
    }

    private void sendSavePictureFragmentRequest(MockMultipartFile pictureFragment, int x, int y, int fragmentWidth, int fragmentHeight) throws Exception {
        mvc
                .perform(multipart("/chartas/{id}/", picture.getId())
                        .file(pictureFragment)
                        .param("x", String.valueOf(x))
                        .param("y", String.valueOf(y))
                        .param("width", String.valueOf(fragmentWidth))
                        .param("height", String.valueOf(fragmentHeight)
                        )
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void givenId_whenGetMultipartPictureFragment_thenStatus400() throws Exception {
        given(picturesRepository.findById(Mockito.anyLong())).willReturn(Optional.of(picture));
        sendGetPictureFragmentRequest(-fragmentWidth - 1, 0, fragmentWidth, fragmentHeight);
        sendGetPictureFragmentRequest(0, -fragmentHeight - 1, fragmentWidth, fragmentHeight);
        sendGetPictureFragmentRequest(picture.getWidth() + 1, 0, fragmentWidth, fragmentHeight);
        sendGetPictureFragmentRequest(0, picture.getHeight() + 1, fragmentWidth, fragmentHeight);
        sendGetPictureFragmentRequest(0, 0, maxValidPictureWidth + 1, fragmentHeight);
        sendGetPictureFragmentRequest(0, 0, fragmentWidth, maxValidPictureHeight + 1);
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
