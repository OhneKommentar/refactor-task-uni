package com.instakill.imageprocessing.application;

public interface ImageProcessingService {

    byte[] process(byte[] image, String format);
}
