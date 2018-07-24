package models;

import android.util.Log;

public class Vector3 {
    public double x,y,z;

    public Vector3() {
        x=y=z=0;
    }

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3(double x, double y) {
        this.x = x;
        this.y = y;
        this.z = 0;
    }

    public double magnitude() {
        return Math.sqrt((x*x) + (y*y) + (z*z));
    }

    //AKA unit vector AKA normalized vector
    public Vector3 direction() {
        double mag = this.magnitude();
        return new Vector3(x/mag, y/mag, z/mag);
    }

    public Vector3 pathTo(Vector3 destination) {
        return new Vector3(destination.x - x, destination.y - y, destination.z - z);
    }

    public Vector3 scalarMultiply(double scale) {
        return new Vector3(x*scale, y*scale, z*scale);
    }

    public Vector3 subtract(Vector3 b) {
        return new Vector3(x-b.x, y-b.y, z-b.z);
    }

    public String toString() {
        return "(" + Double.toString(x) + ", " + Double.toString(y) + ", " + Double.toString(z) + ")";
    }

    public float[] toQuaternion() {
        float[] quat = new float[4];
        float cosx = (float)Math.cos(x/2);
        float sinx = (float)Math.sin(x/2);
        float cosy = (float)Math.cos(y/2);
        float siny = (float)Math.sin(y/2);
        float cosz = (float)Math.cos(z/2);
        float sinz = (float)Math.sin(z/2);

        //swap y and z

        quat[0] = sinx*cosy*cosz - cosx*siny*sinz;
        quat[1] = cosx*siny*cosz + sinx*cosy*sinz;
        quat[2] = cosx*cosy*sinz - sinx*siny*cosz;
        quat[3] = cosx*cosy*cosz + sinx*siny*sinz;

        Log.i("VECTOR3", "(" + quat[0] + ", "+ quat[1]+ ", " + quat[2] + ", " + quat[3] + ")");
        return quat;
    }
}
