package com.skeletonarmy.marrow.cv;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.RealMatrix;

public class MathUtils {
    public static Vector3D rayPlaneIntersection(
            Vector3D rayOrigin,
            Vector3D rayDirection,
            Vector3D planeNormal,
            double planeOffset) {

        // Calculate the denominator of the ray-plane intersection equation
        double denom = planeNormal.dotProduct(rayDirection);

        // Check if the ray is parallel to the plane
        if (Math.abs(denom) < 1e-6) {
            return null; // No intersection, ray is parallel to the plane
        }

        // Calculate the distance along the ray to the intersection point
        double t = -(planeNormal.dotProduct(rayOrigin) + planeOffset) / denom;

        // Check if the intersection is behind the ray origin
        if (t < 0) {
            return null; // Intersection is behind the ray
        }

        // Compute the intersection point
        Vector3D intersectionPoint = rayOrigin.add(rayDirection.scalarMultiply(t));
        return intersectionPoint;
    }
}
