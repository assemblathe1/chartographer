package com.github.assemblathe1.chartographer.validators;


import com.github.assemblathe1.chartographer.exceptions.ValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

//TODO Validation correct input image type

@Component
public class PictureValidator {
    public void validate(Integer x, Integer y, Integer width, Integer height, Integer maxValidWidth, Integer maxValidHeight, Integer originalPictureWidth, Integer originalPictureHeight) {
        List<String> errors = new ArrayList<>();
        if (width <= 0) {
            errors.add("Width can not be less than 0");
        }
        if (height <= 0) {
            errors.add("Height can not be less than 0");
        }
        if (width > maxValidWidth) {
            errors.add("Width can not be more than " + maxValidWidth + " .If you want to handle so BIG picture, increase limit in PictureService");
        }
        if (height > maxValidHeight) {
            errors.add("Height can not be less than " + maxValidHeight + " .If you want to handle so BIG picture, increase limit in PictureService");
        }
        if (x + width <= 0 || x >= originalPictureWidth) {
            errors.add("Fragment and papyrus do not cross by x ");
        }
        if (y + height <= 0 || y >= originalPictureHeight) {
            errors.add("Fragment and papyrus do not cross by y ");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
