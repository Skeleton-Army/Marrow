package com.skeletonarmy.marrow.cv;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

public class CameraParameters {
    private final Vector3D rayOrigin;
    private final Vector3D rayDirection;

    private CameraParameters(Vector3D rayOrigin, Vector3D rayDirection) {
        this.rayOrigin = rayOrigin;
        this.rayDirection = rayDirection;
    }

    public static CameraParameters fromPixel(
            RealMatrix intrinsicMatrix, // 3x3 camera intrinsic matrix
            RealMatrix extrinsicMatrix, // 4x4 camera extrinsic matrix
            double u, double v) {       // Pixel coordinates (u, v)

        // Step 1: Convert pixel (u, v) to normalized camera coordinates
        double fx = intrinsicMatrix.getEntry(0, 0); // Focal length in x
        double fy = intrinsicMatrix.getEntry(1, 1); // Focal length in y
        double cx = intrinsicMatrix.getEntry(0, 2); // Principal point x
        double cy = intrinsicMatrix.getEntry(1, 2); // Principal point y

        double xNormalized = (u - cx) / fx;
        double yNormalized = (v - cy) / fy;

        // Step 2: Define the ray in the camera's local coordinate system
        Vector3D rayCameraSpace = new Vector3D(xNormalized, yNormalized, 1);

        // Step 3: Extract rotation (R) and translation (t) from the extrinsic matrix
        Vector3D rayOrigin = new Vector3D(
                extrinsicMatrix.getEntry(0, 3),
                extrinsicMatrix.getEntry(1, 3),
                extrinsicMatrix.getEntry(2, 3)
        );

        RealMatrix rotationMatrix = extrinsicMatrix.getSubMatrix(0, 2, 0, 2);

        // Convert rotation matrix to a direction vector
        double[] rayDirectionWorldArray = rotationMatrix.operate(new double[]{
                rayCameraSpace.getX(),
                rayCameraSpace.getY(),
                rayCameraSpace.getZ()
        });

        Vector3D rayDirection = new Vector3D(rayDirectionWorldArray[0], rayDirectionWorldArray[1], rayDirectionWorldArray[2]);
        rayDirection = rayDirection.normalize();

        return new CameraParameters(rayOrigin, rayDirection);
    }

    public Vector3D getRayOrigin() {
        return rayOrigin;
    }

    public Vector3D getRayDirection() {
        return rayDirection;
    }

    @Override
    public String toString() {
        return "Ray Origin: " + rayOrigin + "\nRay Direction: " + rayDirection;
    }
}


