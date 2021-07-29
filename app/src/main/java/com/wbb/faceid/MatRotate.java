package com.wbb.faceid;

import org.opencv.core.Core;
import org.opencv.core.Mat;

public class MatRotate {

    public static void matRotateClockWise90(Mat old, Mat newOne) {
        if (!old.empty()) {
            Core.transpose(old, newOne);
            Core.flip(old, newOne, 1);
        }
    }

    public static void matRotateClockWise180(Mat old, Mat newOne) {
        if (!old.empty()) {
            Core.flip(old, newOne, 0);
            Core.flip(old, newOne, 1);
        }
    }

    public static void matRotateClockWise270(Mat old, Mat newOne) {
        if (!old.empty()) {
            Core.transpose(old, newOne);
            Core.flip(old, newOne, 0);
        }
    }

    public static void myRotateAntiClockWise90(Mat old, Mat newOne) {
        if (!old.empty()) {
            Core.transpose(old, newOne);
            Core.flip(old, newOne, 0);
        }
    }
}
