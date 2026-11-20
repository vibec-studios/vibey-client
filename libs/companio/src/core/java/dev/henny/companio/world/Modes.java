package dev.henny.companio.world;

import dev.henny.companio.def.MoveMode;

final class Modes {
    private Modes() {}

    static MoveMode resolve(MoveMode wanted, Command command, boolean obeys) {
        MoveMode m = wanted == null ? MoveMode.FOLLOW : wanted;
        if (!obeys) return m;
        if (command == Command.STAY) {
            return switch (m) {
                case FOLLOW, NEAR, HOP, FLY, CIRCLE -> MoveMode.STAY;
                default -> m;
            };
        }
        if (command == Command.SIT) {
            return switch (m) {
                case SHOULDER, HEAD, FLEE, SLEEP -> m;
                default -> MoveMode.SIT;
            };
        }
        return m;
    }

    static MoveMode idle(Command command) {
        return switch (command) {
            case STAY -> MoveMode.STAY;
            case SIT -> MoveMode.SIT;
            default -> MoveMode.FOLLOW;
        };
    }
}
