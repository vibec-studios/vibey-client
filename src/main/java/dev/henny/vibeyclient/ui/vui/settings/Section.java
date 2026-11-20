package dev.henny.vibeyclient.ui.vui.settings;

import java.util.List;

public record Section(String id, String title, List<SettingRow> rows) {}
