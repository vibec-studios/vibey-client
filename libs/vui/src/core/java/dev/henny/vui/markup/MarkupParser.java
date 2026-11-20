package dev.henny.vui.markup;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.SourceCursor;
import dev.henny.vui.diag.SourcePos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

public final class MarkupParser {
    private static final Set<String> RAW_TEXT_TAGS = Whitespace.RAW_TEXT_TAGS;

    private final SourceCursor in;
    private final Diagnostics diagnostics;
    private final Deque<String> open = new ArrayDeque<>();
    private String pendingClose;

    private MarkupParser(SourceCursor in, Diagnostics diagnostics) {
        this.in = in;
        this.diagnostics = diagnostics;
    }

    public static List<TemplateNode> parse(String text, String file, Diagnostics diagnostics) {
        return parse(text, new SourcePos(file, 1, 1), diagnostics);
    }

    public static List<TemplateNode> parse(String text, SourcePos start, Diagnostics diagnostics) {
        MarkupParser p = new MarkupParser(new SourceCursor(text, start), diagnostics);
        List<TemplateNode> nodes = p.children(null);
        return Whitespace.condense(nodes);
    }

    private List<TemplateNode> children(String parent) {
        List<TemplateNode> out = new ArrayList<>();
        while (!in.atEnd()) {
            if (in.startsWith("<!--")) {
                comment();
            } else if (in.startsWith("</")) {
                if (closing(parent)) return out;
            } else if (in.peek() == '<' && Character.isLetter(in.peek(1))) {
                out.add(element());
                if (pendingClose != null) {
                    if (pendingClose.equals(parent)) pendingClose = null;
                    return out;
                }
            } else {
                text(out);
            }
        }
        if (parent != null) diagnostics.error(in.pos(), "missing </" + parent + ">", "close the element that is still open");
        return out;
    }

    private boolean closing(String parent) {
        SourcePos pos = in.pos();
        in.skip(2);
        String name = readName();
        in.skipWhitespace();
        if (in.peek() == '>') in.next();
        else diagnostics.error(in.pos(), "expected '>' after </" + name, null);
        if (name.equals(parent)) return true;
        if (open.contains(name)) {
            diagnostics.error(pos, "missing </" + parent + "> before </" + name + ">", "every open tag needs its closing tag");
            pendingClose = name;
            return true;
        }
        diagnostics.error(pos, "unexpected </" + name + ">", parent == null ? "nothing is open here" : "the open element is <" + parent + ">");
        return false;
    }

    private void comment() {
        SourcePos pos = in.pos();
        int end = in.indexOf("-->");
        if (end < 0) {
            diagnostics.error(pos, "unclosed comment", "add -->");
            in.readUntil(in.text().length());
            return;
        }
        in.readUntil(end + 3);
    }

    private ElementNode element() {
        SourcePos pos = in.pos();
        in.next();
        String tag = readName();
        List<Attribute> attributes = new ArrayList<>();
        boolean selfClosing = attributes(tag, attributes);
        if (selfClosing) return new ElementNode(tag, attributes, List.of(), pos);
        if (RAW_TEXT_TAGS.contains(tag)) return rawText(tag, attributes, pos);
        open.push(tag);
        List<TemplateNode> kids = children(tag);
        open.pop();
        return new ElementNode(tag, attributes, kids, pos);
    }

    private ElementNode rawText(String tag, List<Attribute> attributes, SourcePos pos) {
        SourcePos contentPos = in.pos();
        String close = "</" + tag;
        int end = in.indexOf(close);
        if (end < 0) {
            diagnostics.error(pos, "missing </" + tag + ">", null);
            end = in.text().length();
        }
        String content = in.readUntil(end);
        if (!in.atEnd()) {
            in.skip(close.length());
            in.skipWhitespace();
            if (in.peek() == '>') in.next();
        }
        TextNode text = new TextNode(List.of(new TextPart(false, content, contentPos)), contentPos);
        return new ElementNode(tag, attributes, List.of(text), pos);
    }

    private boolean attributes(String tag, List<Attribute> out) {
        while (true) {
            in.skipWhitespace();
            if (in.atEnd()) {
                diagnostics.error(in.pos(), "unclosed start tag <" + tag, "add > or />");
                return true;
            }
            if (in.startsWith("/>")) {
                in.skip(2);
                return true;
            }
            if (in.peek() == '>') {
                in.next();
                return false;
            }
            attribute(tag, out);
        }
    }

    private void attribute(String tag, List<Attribute> out) {
        SourcePos pos = in.pos();
        StringBuilder name = new StringBuilder();
        while (!in.atEnd() && !Character.isWhitespace(in.peek()) && "=>".indexOf(in.peek()) < 0 && !in.startsWith("/>")) {
            name.append(in.next());
        }
        if (name.length() == 0) {
            diagnostics.error(pos, "unexpected '" + in.peek() + "' in <" + tag + ">", null);
            in.next();
            return;
        }
        in.skipWhitespace();
        String value = null;
        SourcePos valuePos = in.pos();
        if (in.peek() == '=') {
            in.next();
            in.skipWhitespace();
            boolean quoted = in.peek() == '"' || in.peek() == '\'';
            valuePos = quoted ? in.pos().shift(1) : in.pos();
            value = attributeValue(name.toString());
        }
        out.add(AttributeNames.classify(name.toString(), value, pos, valuePos, diagnostics));
    }

    private String attributeValue(String name) {
        char quote = in.peek();
        if (quote == '"' || quote == '\'') {
            SourcePos start = in.pos();
            in.next();
            int end = in.indexOf(String.valueOf(quote));
            if (end < 0) {
                diagnostics.error(start, "unclosed value of '" + name + "'", "add the closing " + quote);
                return in.readUntil(in.text().length());
            }
            String v = in.readUntil(end);
            in.next();
            return v;
        }
        StringBuilder sb = new StringBuilder();
        while (!in.atEnd() && !Character.isWhitespace(in.peek()) && in.peek() != '>' && !in.startsWith("/>")) sb.append(in.next());
        return sb.toString();
    }

    private void text(List<TemplateNode> out) {
        SourcePos pos = in.pos();
        StringBuilder sb = new StringBuilder();
        while (!in.atEnd()) {
            if (in.startsWith("{{")) {
                int end = in.indexOf("}}");
                if (end < 0) {
                    sb.append(in.readUntil(in.text().length()));
                    break;
                }
                sb.append(in.readUntil(end + 2));
                continue;
            }
            if (in.peek() == '<' && (Character.isLetter(in.peek(1)) || in.peek(1) == '/' || in.peek(1) == '!')) break;
            sb.append(in.next());
        }
        out.add(new TextNode(Interpolation.split(sb.toString(), pos, diagnostics), pos));
    }

    private String readName() {
        StringBuilder sb = new StringBuilder();
        while (!in.atEnd()) {
            char c = in.peek();
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == ':' || c == '.') sb.append(in.next());
            else break;
        }
        return sb.toString();
    }
}
