package org.lwjgl.glfw;

public final class GLFW {
    public static int swaps;
    public static int polls;
    public static int shows;
    public static int hides;
    public static int focus;
    public static int visibleHint = -1;
    public static long lastSwapWindow;

    public static void glfwSwapBuffers(long window) {
        swaps++;
        lastSwapWindow = window;
    }

    public static void glfwPollEvents() {
        polls++;
    }

    public static long glfwCreateWindow(int w, int h, CharSequence title, long monitor, long share) {
        return 4242L;
    }

    public static void glfwWindowHint(int hint, int value) {
        if (hint == 0x00020004) visibleHint = value;
    }

    public static void glfwShowWindow(long window) {
        shows++;
    }

    public static void glfwHideWindow(long window) {
        hides++;
    }

    public static void glfwFocusWindow(long window) {
        focus++;
    }
}
