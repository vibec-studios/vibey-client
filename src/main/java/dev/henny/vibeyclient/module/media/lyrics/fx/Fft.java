package dev.henny.vibeyclient.module.media.lyrics.fx;

/** In-place radix-2 complex FFT of one fixed size with precomputed twiddles and Hann window. No allocation per call. */
final class Fft {
    final int n;
    final float[] re;
    final float[] im;
    final float[] window;
    private final float[] cos;
    private final float[] sin;
    private final int[] rev;

    Fft(int n) {
        if (Integer.bitCount(n) != 1) throw new IllegalArgumentException("n must be a power of two");
        this.n = n;
        re = new float[n];
        im = new float[n];
        window = new float[n];
        cos = new float[n / 2];
        sin = new float[n / 2];
        rev = new int[n];
        int bits = Integer.numberOfTrailingZeros(n);
        for (int i = 0; i < n; i++) {
            rev[i] = Integer.reverse(i) >>> (32 - bits);
            window[i] = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * i / (n - 1)));
        }
        for (int i = 0; i < n / 2; i++) {
            cos[i] = (float) Math.cos(-2 * Math.PI * i / n);
            sin[i] = (float) Math.sin(-2 * Math.PI * i / n);
        }
    }

    /** Loads {@code samples} (length n) windowed into re, zeroes im. */
    void load(float[] samples) {
        for (int i = 0; i < n; i++) {
            re[i] = samples[i] * window[i];
            im[i] = 0f;
        }
    }

    void transform() {
        for (int i = 0; i < n; i++) {
            int j = rev[i];
            if (j > i) {
                float t = re[i];
                re[i] = re[j];
                re[j] = t;
                t = im[i];
                im[i] = im[j];
                im[j] = t;
            }
        }
        for (int size = 2; size <= n; size <<= 1) {
            int half = size >> 1;
            int step = n / size;
            for (int i = 0; i < n; i += size) {
                for (int k = 0; k < half; k++) {
                    float wr = cos[k * step];
                    float wi = sin[k * step];
                    int a = i + k;
                    int b = a + half;
                    float xr = re[b] * wr - im[b] * wi;
                    float xi = re[b] * wi + im[b] * wr;
                    re[b] = re[a] - xr;
                    im[b] = im[a] - xi;
                    re[a] += xr;
                    im[a] += xi;
                }
            }
        }
    }

    /** |X[k]|² of bin k after {@link #transform()}. */
    float power(int k) {
        return re[k] * re[k] + im[k] * im[k];
    }
}
