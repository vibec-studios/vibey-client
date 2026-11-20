package dev.henny.cosmetico.host;

public interface Log {
    void info(String message);

    void warn(String message);

    void error(String message, Throwable error);

    void debug(String message);

    Log SYSTEM = new SystemLog();

    Log NONE = new Log() {
        @Override
        public void info(String message) {}

        @Override
        public void warn(String message) {}

        @Override
        public void error(String message, Throwable error) {}

        @Override
        public void debug(String message) {}
    };
}
