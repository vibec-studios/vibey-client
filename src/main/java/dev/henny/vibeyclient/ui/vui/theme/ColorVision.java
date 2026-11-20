package dev.henny.vibeyclient.ui.vui.theme;

public enum ColorVision {
    PROTAN(new double[][] {{0.152286, 1.052583, -0.204868}, {0.114503, 0.786281, 0.099216}, {-0.003882, -0.048116, 1.051998}}),
    DEUTAN(new double[][] {{0.367322, 0.860646, -0.227968}, {0.280085, 0.672501, 0.047413}, {-0.011820, 0.042940, 0.968881}}),
    TRITAN(new double[][] {{1.255528, -0.076749, -0.178779}, {-0.078411, 0.930809, 0.147602}, {0.004733, 0.691367, 0.303900}});

    private final double[][] m;

    ColorVision(double[][] m) {
        this.m = m;
    }

    public int simulate(int argb) {
        double[] l = {Contrast.linear((argb >> 16) & 0xFF), Contrast.linear((argb >> 8) & 0xFF), Contrast.linear(argb & 0xFF)};
        int[] out = new int[3];
        for (int i = 0; i < 3; i++) out[i] = Contrast.gamma(m[i][0] * l[0] + m[i][1] * l[1] + m[i][2] * l[2]);
        return 0xFF000000 | (out[0] << 16) | (out[1] << 8) | out[2];
    }

    public double distance(int a, int b) {
        double[] p = lab(simulate(a)), q = lab(simulate(b));
        return Math.sqrt(sq(p[0] - q[0]) + sq(p[1] - q[1]) + sq(p[2] - q[2]));
    }

    static double[] lab(int argb) {
        double r = Contrast.linear((argb >> 16) & 0xFF), g = Contrast.linear((argb >> 8) & 0xFF), b = Contrast.linear(argb & 0xFF);
        double x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047;
        double y = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        double z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883;
        return new double[] {116 * f(y) - 16, 500 * (f(x) - f(y)), 200 * (f(y) - f(z))};
    }

    private static double f(double t) {
        return t > 0.008856 ? Math.cbrt(t) : 7.787 * t + 16 / 116.0;
    }

    private static double sq(double v) {
        return v * v;
    }
}
