package com.skeletonarmy.marrow.apriltags;

import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;

import java.util.Arrays;

//The FTC SDK doesn't include `equals` method for a lot of their classes. So this class...
public class FtcEquals {
    public static boolean equals(VectorF a, VectorF b) {
        return Arrays.equals(a.getData(), b.getData());
    }

    public static boolean equals(Quaternion a, Quaternion b) {
        if (a == b) return true;
        if (a == null || b == null) return false;

        return
                a.w == b.w &&
                a.x == b.x &&
                a.y == b.y &&
                a.z == b.z &&
                a.acquisitionTime == b.acquisitionTime;
    }
}
