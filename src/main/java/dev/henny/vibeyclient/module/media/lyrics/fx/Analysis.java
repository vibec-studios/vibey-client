package dev.henny.vibeyclient.module.media.lyrics.fx;

/**
 * The music features of one stream, hop by hop (pure Java, no game classes, preallocated). Feed it the window that is
 * audible now with {@link #process}; or {@link #silence} when there is no sound. Results, all 0 … 1 unless noted:
 * <ul>
 *   <li>{@link #bass}, {@link #mid}, {@link #high}, {@link #energy}: band and overall level, normalised to the
 *       loudest part of the last ~10 s (so quiet and loud songs both use the whole range)</li>
 *   <li>{@link #onset}: true in the hop where a beat/hit starts (spectral flux over an adaptive threshold, bass
 *       weighted, at most every 180 ms), {@link #onsetStrength} how strong it was</li>
 *   <li>{@link #bpm}: tempo estimate from the onset intervals (0 = unknown)</li>
 *   <li>{@link #drop}: true in the hop where the music "drops" (short-term level far above the long-term one)</li>
 * </ul>
 */
public final class Analysis {
    private static final int BANDS = 4;
    /** Band edges in Hz: bass, mid, high (overall = all). */
    private static final float[] EDGES = {35f, 160f, 2000f, 6000f};
    private static final int FLUX_HISTORY = 48;
    private static final int ONSET_HISTORY = 24;
    private static final double[] MULTS = {1, 2, 0.5};

    private final Fft fft;
    private final int rate;
    private final int[] lo = new int[BANDS];
    private final int[] hi = new int[BANDS];
    private final float[] peakDb = new float[BANDS];
    private final float[] level = new float[BANDS];
    private final float[] prevLog;
    private final float[] weight;
    private final float[] flux = new float[FLUX_HISTORY];
    private int fluxCount;
    private float prevFlux;
    private double lastOnset = -10;
    private final double[] onsetTimes = new double[ONSET_HISTORY];
    private int onsetCount;
    private final float[] tempoVotes = new float[71];
    private double lastTime = -1;
    private double firstTime = -1;
    /** Bass level in dB, short-term and long-term average (drops are a jump of the first over the second). */
    private float shortDb = -75f;
    private float longDb = -75f;
    private float bassDb = -75f;
    private double lastDrop = -100;

    public float bass;
    public float mid;
    public float high;
    public float energy;
    public boolean onset;
    public float onsetStrength;
    public float bpm;
    public boolean drop;

    public Analysis(int rate, int n) {
        this.rate = rate;
        this.fft = new Fft(n);
        prevLog = new float[n / 2];
        weight = new float[n / 2];
        float binHz = rate / (float) n;
        // bands 0 = bass (35–160), 1 = mid (160–2000), 2 = high (2000–6000), 3 = everything (35–6000)
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {0, 3}};
        for (int b = 0; b < BANDS; b++) {
            lo[b] = Math.max(1, Math.round(EDGES[edges[b][0]] / binHz));
            hi[b] = Math.max(lo[b] + 1, Math.min(n / 2 - 1, Math.round(EDGES[edges[b][1]] / binHz)));
            peakDb[b] = -45f;
        }
        for (int k = 0; k < n / 2; k++) {
            float hz = k * binHz;
            // kicks and bass hits drive the beat; hats and noise count a little
            weight[k] = hz < 35 ? 0f : hz < 180 ? 3f : hz < 2000 ? 0.25f : hz < 6000 ? 0.1f : 0f;
        }
    }

    public int size() {
        return fft.n;
    }

    public int rate() {
        return rate;
    }

    /** One hop: {@code samples} (length {@link #size()}) end at the moment {@code t} (seconds, monotonic). */
    public void process(float[] samples, double t) {
        float dt = lastTime < 0 ? 0.0116f : (float) Math.max(0.001, Math.min(0.25, t - lastTime));
        lastTime = t;
        fft.load(samples);
        fft.transform();
        int half = fft.n / 2;
        float norm = 4f / (fft.n * fft.n);
        float fx = 0f;
        for (int k = 1; k < half; k++) {
            float p = fft.power(k) * norm;
            float lg = (float) Math.log1p(1000f * Math.sqrt(p));
            float d = lg - prevLog[k];
            if (d > 0) fx += d * weight[k];
            prevLog[k] = lg;
        }
        for (int b = 0; b < BANDS; b++) {
            double e = 0;
            for (int k = lo[b]; k < hi[b]; k++) e += fft.power(k) * norm;
            float db = (float) (10 * Math.log10(e + 1e-12));
            if (b == 0) bassDb = Math.max(-75f, db);
            // the loudest part of the last seconds is "full"; the peak sinks by 0.8 dB per second
            peakDb[b] = Math.max(db, Math.max(-45f, peakDb[b] - 0.8f * dt));
            float l = db < -66f ? 0f : clamp01(1f + (db - peakDb[b]) / 24f);
            // fast attack, softer release
            float k = l > level[b] ? 1f - (float) Math.exp(-dt / 0.03f) : 1f - (float) Math.exp(-dt / 0.18f);
            level[b] += (l - level[b]) * k;
        }
        publish();
        onsetDetect(fx, t);
        dropDetect(dt, t);
    }

    /** No sound right now (paused, nothing playing, no data): everything fades out. */
    public void silence(double t) {
        float dt = lastTime < 0 ? 0.0116f : (float) Math.max(0.001, Math.min(0.25, t - lastTime));
        lastTime = t;
        float k = 1f - (float) Math.exp(-dt / 0.25f);
        for (int b = 0; b < BANDS; b++) level[b] -= level[b] * k;
        java.util.Arrays.fill(prevLog, 0f);
        bassDb = -75f;
        publish();
        onset = false;
        drop = false;
        dropDetect(dt, t);
    }

    /** An onset from outside (the lyrics timing when there is no audio). */
    public void external(double t, float strength, float bassLevel, float energyLevel) {
        float dt = lastTime < 0 ? 0.0116f : (float) Math.max(0.001, Math.min(0.25, t - lastTime));
        lastTime = t;
        float k = 1f - (float) Math.exp(-dt / 0.2f);
        level[0] += (bassLevel - level[0]) * k;
        level[1] += (energyLevel * 0.8f - level[1]) * k;
        level[2] += (energyLevel * 0.5f - level[2]) * k;
        level[3] += (energyLevel - level[3]) * k;
        publish();
        onset = strength > 0 && t - lastOnset > 0.18;
        if (onset) {
            onsetStrength = strength;
            noteOnset(t);
        }
        drop = false;
        dropDetect(dt, t);
    }

    private void publish() {
        bass = level[0];
        mid = level[1];
        high = level[2];
        energy = level[3];
    }

    private void onsetDetect(float fx, double t) {
        flux[fluxCount % FLUX_HISTORY] = fx;
        fluxCount++;
        int n = Math.min(fluxCount, FLUX_HISTORY);
        double mean = 0;
        for (int i = 0; i < n; i++) mean += flux[i];
        mean /= n;
        double var = 0;
        for (int i = 0; i < n; i++) var += (flux[i] - mean) * (flux[i] - mean);
        double std = Math.sqrt(var / n);
        double threshold = mean + 1.5 * std + 0.05;
        // rising over the threshold, not right after the last one
        onset = n > 8 && fx > threshold && fx >= prevFlux && t - lastOnset > 0.18 && energy > 0.08f;
        prevFlux = fx;
        if (onset) {
            onsetStrength = clamp01((float) ((fx - mean) / (4 * std + 1e-6)));
            noteOnset(t);
        }
    }

    private void noteOnset(double t) {
        lastOnset = t;
        onsetTimes[onsetCount % ONSET_HISTORY] = t;
        onsetCount++;
        // tempo: every interval between recent onsets (and its halves/doubles) votes for 60–200 bpm (2 bpm bins)
        for (int i = 0; i < tempoVotes.length; i++) tempoVotes[i] *= 0.97f;
        // the gap to the previous onset votes for its tempo, its double and half count a little (a mild prior
        // around 120 bpm decides between them)
        if (onsetCount > 1) {
            double iv = t - onsetTimes[(onsetCount - 2) % ONSET_HISTORY];
            if (iv < 2.0) for (double m : MULTS) vote(60.0 / (iv / m), m == 1 ? 1f : 0.35f);
        }
        int best = -1;
        float bestV = 0f;
        for (int i = 0; i < tempoVotes.length; i++) {
            if (tempoVotes[i] > bestV) {
                bestV = tempoVotes[i];
                best = i;
            }
        }
        bpm = best < 0 || bestV < 1.5f ? 0f : 60f + best * 2f;
    }

    private void vote(double bpmValue, float w) {
        if (bpmValue < 60 || bpmValue > 200) return;
        w *= (float) Math.exp(-Math.pow(Math.log(bpmValue / 120.0), 2) / (2 * 0.5 * 0.5));
        double pos = (bpmValue - 60) / 2.0;
        int i = (int) Math.floor(pos);
        float f = (float) (pos - i);
        if (i >= 0 && i < tempoVotes.length) tempoVotes[i] += w * (1f - f);
        if (i + 1 >= 0 && i + 1 < tempoVotes.length) tempoVotes[i + 1] += w * f;
    }

    private void dropDetect(float dt, double t) {
        if (firstTime < 0) firstTime = t;
        // the bass comes back much louder than it was over the last seconds (after a build-up or a break)
        shortDb += (bassDb - shortDb) * (1f - (float) Math.exp(-dt / 0.25f));
        longDb += (bassDb - longDb) * (1f - (float) Math.exp(-dt / 4f));
        drop = shortDb > -40f && shortDb - longDb > 9f && t - lastDrop > 6.0 && t - firstTime > 4.0;
        if (drop) {
            lastDrop = t;
            longDb = shortDb;
        }
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(1f, v);
    }
}
