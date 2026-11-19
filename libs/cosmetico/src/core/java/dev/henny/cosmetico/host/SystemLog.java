package dev.henny.cosmetico.host;

final class SystemLog implements Log {
    private final System.Logger logger = System.getLogger("cosmetico");

    @Override
    public void info(String message) {
        logger.log(System.Logger.Level.INFO, message);
    }

    @Override
    public void warn(String message) {
        logger.log(System.Logger.Level.WARNING, message);
    }

    @Override
    public void error(String message, Throwable error) {
        logger.log(System.Logger.Level.ERROR, message, error);
    }

    @Override
    public void debug(String message) {
        logger.log(System.Logger.Level.DEBUG, message);
    }
}
