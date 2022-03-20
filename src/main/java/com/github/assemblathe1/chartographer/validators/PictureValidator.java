package com.github.assemblathe1.chartographer.validators;


import com.github.assemblathe1.chartographer.exceptions.ValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

//TODO Validation correct input image type

@Component
public class PictureValidator {
    public void validate(Integer x,
                         Integer y,
                         Integer width,
                         Integer height,
                         Integer maxValidWidth,
                         Integer maxValidHeight,
                         Integer originalPictureWidth,
                         Integer originalPictureHeight
    ) {
        List<String> errors = new ArrayList<>();
        checkIfValueLessThanZero(width, errors, "Width");
        checkIfValueLessThanZero(height, errors, "Height");
        checkIfValueMoreThanMaxValue(width, maxValidWidth, errors, "Width");
        checkIfValueMoreThanMaxValue(height, maxValidHeight, errors, "Height");
        checkIfFragmentNotCrossPicture(x, width, originalPictureWidth, errors, "x");
        checkIfFragmentNotCrossPicture(y, height, originalPictureHeight, errors, "y");

        if (!errors.isEmpty()) throw new ValidationException(errors);
    }

    public void validate(Integer width,
                         Integer height,
                         Integer maxValidWidth,
                         Integer maxValidHeight
    ) {
        List<String> errors = new ArrayList<>();
        checkIfValueLessThanZero(width, errors, "Width");
        checkIfValueLessThanZero(height, errors, "Height");
        checkIfValueMoreThanMaxValue(width, maxValidWidth, errors, "Width");
        checkIfValueMoreThanMaxValue(height, maxValidHeight, errors, "Height");

        if (!errors.isEmpty()) throw new ValidationException(errors);
    }

    private void checkIfValueLessThanZero(Integer value, List<String> errors, String parameter) {
        if (value <= 0) errors.add(parameter + " can not be less than 0");
    }

    private void checkIfValueMoreThanMaxValue(Integer value, Integer maxValue, List<String> errors, String parameter) {
        if (value > maxValue) errors.add(parameter + " can not be more than " + maxValue + " .If you want to handle so BIG picture, increase limit in PictureService");
    }

    private void checkIfFragmentNotCrossPicture(Integer value, Integer fragmentSideSize, Integer pictureSideSize,  List<String> errors, String parameter) {
        if (value + fragmentSideSize <= 0 || value >= pictureSideSize) errors.add("Fragment and papyrus do not cross by " + parameter);
    }
}
