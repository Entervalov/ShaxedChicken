package org.entervalov.shaxedchicken.client.gui.changelog;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChangelogParser {

    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");

    private static final int COLOR_PURPLE = 0x9B4FDE;
    private static final int COLOR_GREEN = 0x7BCC8F;
    private static final int COLOR_RED = 0xE85D5D;
    private static final int COLOR_ORANGE = 0xDD9944;
    private static final int COLOR_BLUE = 0x6EAADD;
    private static final int COLOR_YELLOW = 0xDDCC55;
    private static final int COLOR_PINK = 0xCC6699;
    private static final int COLOR_CYAN = 0x77BBCC;
    private static final int COLOR_GRAY = 0x777777;
    private static final int COLOR_WHITE = 0xDDDDDD;
    private static final int COLOR_GOLD = 0xCCAA44;
    private static final int COLOR_BLACK = 0x999999;

    private static final int CAT_NEW = 0x9B4FDE;
    private static final int CAT_FIX = 0x6EAADD;
    private static final int CAT_SYS = 0xDD9944;
    private static final int CAT_DEF = 0xCC6699;
    private static final int CAT_SOON = 0x777777;
    private static final int CAT_DEFAULT = 0xBBBBBB;

    public static List<ITextComponent> loadChangelog(int maxWidth) {
        List<ITextComponent> lines = new ArrayList<>();
        ResourceLocation loc = new ResourceLocation("shaxedchicken", "changelog.hk");
        Minecraft mc = Minecraft.getInstance();

        try {
            IResource resource = mc.getResourceManager().getResource(loc);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int lineCount = 0;

                while ((line = reader.readLine()) != null) {
                    lineCount++;
                    if (lineCount <= 2 && (line.startsWith("#") || line.contains("@date"))) continue;
                    if (line.trim().equals("===")) break;

                    ITextComponent parsedLine = parseLine(line);

                    if (mc.font.width(parsedLine) > maxWidth) {
                        mc.font.getSplitter().splitLines(parsedLine, maxWidth, Style.EMPTY)
                                .forEach(prop -> {
                                    IFormattableTextComponent component = new StringTextComponent("");
                                    prop.visit((style, text) -> {
                                        component.append(new StringTextComponent(text).setStyle(style));
                                        return java.util.Optional.empty();
                                    }, Style.EMPTY);
                                    lines.add(component);
                                });
                    } else {
                        lines.add(parsedLine);
                    }
                }
            }
        } catch (Exception e) {
            lines.add(new StringTextComponent("Error loading changelog").withStyle(TextFormatting.RED));
        }
        return lines;
    }

    private static ITextComponent parseLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return new StringTextComponent("");

        if (trimmed.equals("---")) return new StringTextComponent("\u0000DIVIDER\u0000");

        if (trimmed.startsWith("## ")) {
            String text = trimmed.substring(3);
            String prefix = "// ";
            int color = CAT_DEFAULT;

            if (text.contains("[SYS]")) { text = text.replace("[SYS]", "").trim(); prefix = "[SYS] "; color = CAT_SYS; }
            else if (text.contains("[NEW]")) { text = text.replace("[NEW]", "").trim(); prefix = "[NEW] "; color = CAT_NEW; }
            else if (text.contains("[FIX]")) { text = text.replace("[FIX]", "").trim(); prefix = "[FIX] "; color = CAT_FIX; }
            else if (text.contains("[DEF]")) { text = text.replace("[DEF]", "").trim(); prefix = "[DEF] "; color = CAT_DEF; }
            else if (text.contains("[SOON]")) { text = text.replace("[SOON]", "").trim(); prefix = "[SOON] "; color = CAT_SOON; }

            return new StringTextComponent("\u0000CATEGORY\u0000" + prefix + text.toUpperCase())
                    .setStyle(Style.EMPTY.withColor(Color.fromRgb(color)).withBold(true));
        }

        IFormattableTextComponent component;
        if (trimmed.startsWith("+ ")) {
            component = new StringTextComponent("  + ").setStyle(Style.EMPTY.withColor(Color.fromRgb(COLOR_GREEN)))
                    .append(parseRichText(trimmed.substring(2), Style.EMPTY.withColor(Color.fromRgb(0xBBCCBB))));
        } else if (trimmed.startsWith("- ")) {
            component = new StringTextComponent("  - ").setStyle(Style.EMPTY.withColor(Color.fromRgb(COLOR_RED)))
                    .append(parseRichText(trimmed.substring(2), Style.EMPTY.withColor(Color.fromRgb(0xDDBBBB))));
        } else if (trimmed.startsWith("* ")) {
            component = new StringTextComponent("  * ").setStyle(Style.EMPTY.withColor(Color.fromRgb(COLOR_YELLOW)))
                    .append(parseRichText(trimmed.substring(2), Style.EMPTY.withColor(Color.fromRgb(0xDDDDBB))));
        } else if (trimmed.startsWith("! ")) {
            component = new StringTextComponent("  ! ").setStyle(Style.EMPTY.withColor(Color.fromRgb(COLOR_PURPLE)).withBold(true))
                    .append(parseRichText(trimmed.substring(2), Style.EMPTY.withColor(Color.fromRgb(0xCCBBDD))));
        } else if (trimmed.startsWith("> ")) {
            component = new StringTextComponent("    | ").setStyle(Style.EMPTY.withColor(Color.fromRgb(0x444448)))
                    .append(parseRichText(trimmed.substring(2), Style.EMPTY.withColor(Color.fromRgb(COLOR_GRAY)).withItalic(true)));
        } else if (trimmed.startsWith("~ ")) {
            component = new StringTextComponent("  ~ ").setStyle(Style.EMPTY.withColor(Color.fromRgb(COLOR_BLUE)))
                    .append(parseRichText(trimmed.substring(2), Style.EMPTY.withColor(Color.fromRgb(0xAABBCC))));
        } else if (trimmed.startsWith("WIP:")) {
            component = new StringTextComponent("  >> WIP: ").setStyle(Style.EMPTY.withColor(Color.fromRgb(COLOR_ORANGE)).withBold(true))
                    .append(parseRichText(trimmed.substring(4).trim(), Style.EMPTY.withColor(Color.fromRgb(0xBBAA88))));
        } else {
            component = (IFormattableTextComponent) parseRichText(line, Style.EMPTY.withColor(Color.fromRgb(0xAAAAAA)));
        }
        return component;
    }

    private static ITextComponent parseRichText(String text, Style baseStyle) {
        IFormattableTextComponent root = new StringTextComponent("").setStyle(baseStyle);
        Pattern pattern = Pattern.compile("\\{([a-z_]+)}(.*?)\\{/\\1}|\\*\\*(.*?)\\*\\*", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                root.append(new StringTextComponent(text.substring(lastEnd, matcher.start())).setStyle(baseStyle));
            }
            if (matcher.group(1) != null) {
                String colorName = matcher.group(1).toLowerCase();
                String content = matcher.group(2);
                root.append(parseBold(content, getColorStyle(colorName)));
            } else if (matcher.group(3) != null) {
                root.append(new StringTextComponent(matcher.group(3)).setStyle(baseStyle.withBold(true)));
            }
            lastEnd = matcher.end();
        }
        if (lastEnd < text.length()) {
            root.append(new StringTextComponent(text.substring(lastEnd)).setStyle(baseStyle));
        }
        return root;
    }

    private static Style getColorStyle(String name) {
        int color = COLOR_WHITE;
        switch (name) {
            case "red": color = COLOR_RED; break;
            case "green": color = COLOR_GREEN; break;
            case "blue": color = COLOR_BLUE; break;
            case "yellow": color = COLOR_YELLOW; break;
            case "orange": color = COLOR_ORANGE; break;
            case "pink": color = COLOR_PINK; break;
            case "purple": color = COLOR_PURPLE; break;
            case "cyan": case "aqua": color = COLOR_CYAN; break;
            case "gold": color = COLOR_GOLD; break;
            case "gray": color = COLOR_GRAY; break;
            case "black": color = COLOR_BLACK; break;
        }
        return Style.EMPTY.withColor(Color.fromRgb(color));
    }

    private static ITextComponent parseBold(String text, Style style) {
        IFormattableTextComponent root = new StringTextComponent("").setStyle(style);
        Matcher matcher = BOLD_PATTERN.matcher(text);
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) root.append(new StringTextComponent(text.substring(lastEnd, matcher.start())));
            root.append(new StringTextComponent(matcher.group(1)).withStyle(TextFormatting.BOLD));
            lastEnd = matcher.end();
        }
        if (lastEnd < text.length()) root.append(new StringTextComponent(text.substring(lastEnd)));
        return root;
    }
}
