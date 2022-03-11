package com.github.assemblathe1.chartographer.controllers;

import com.github.assemblathe1.chartographer.services.PicturesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/chartas")
@RequiredArgsConstructor
public class PicturesController {

    private final PicturesService picturesService;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public String createPicture(
            @RequestParam(name = "width") Integer width,
            @RequestParam(name = "height") Integer height) {
        return picturesService.createPicture(width, height).toString();
    }

    @PostMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void savePictureFragment(
            @PathVariable String id,
            @RequestParam(name = "x") Integer x,
            @RequestParam(name = "y") Integer y,
            @RequestParam(name = "width") Integer width,
            @RequestParam(name = "height") Integer height,
            @RequestParam("file") MultipartFile file) {
        picturesService.savePictureFragment(id, x, y, width, height, file);
    }

    @GetMapping(value = "/{id}")
    public  ResponseEntity<byte[]> getPictureFragment(
            @PathVariable String id,
            @RequestParam(name = "x") Integer x,
            @RequestParam(name = "y") Integer y,
            @RequestParam(name = "width") Integer width,
            @RequestParam(name = "height") Integer height) {
        return ResponseEntity.ok().contentType(MediaType.valueOf("image/bmp")).body(picturesService.getPictureFragment(id, x, y, width, height).toByteArray());
    }

    //TODO при удалении файла которого нет, ошибка не появляется

    @DeleteMapping(value = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deletePicture(@PathVariable String id) {
        picturesService.deletePicture(id);
    }
}
