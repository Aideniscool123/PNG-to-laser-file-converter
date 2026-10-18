package fileConverter.core;

import fileConverter.util.Fmt;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static fileConverter.model.CutMode.OUTLINE;


public final class SvgWriter {

    public static final double CUT_LINE_MM = 0.1;
    public static final String CUT_COLOR = "#FF0000";

    private SvgWriter() {
    }

    public static void write(fileConverter.core.LaserJob job, File file, String sourceName) throws IOException {
        Files.writeString(file.toPath(), toSvg(job, sourceName), StandardCharsets.UTF_8);
    }

    public static String toSvg(fileConverter.core.LaserJob job, String sourceName) {
        if (job.isEmpty()) {
            throw new IllegalStateException("Nothing to export: every pixel is switched off.");
        }
        double px = job.pixelMm();
        double off = job.offsetMm();
        String width = Fmt.num(job.widthMm());
        String height = Fmt.num(job.heightMm());

        StringBuilder sb = new StringBuilder(8192);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!-- FileConverter | source: ").append(safeForComment(sourceName))
                .append(" | ").append(Fmt.num(px)).append(" mm per pixel | cut: ")
                .append(job.cutMode().shortName()).append(" -->\n");
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width).append("mm\" height=\"")
                .append(height).append("mm\" viewBox=\"0 0 ").append(width).append(' ').append(height).append("\">\n");

        for (fileConverter.core.LaserJob.Layer layer : job.layers()) {
            String hex = Fmt.hex(layer.rgb());
            sb.append("  <g id=\"engrave-").append(hex.substring(1)).append("\" fill=\"").append(hex)
                    .append("\" stroke=\"none\">\n");
            for (fileConverter.core.LaserJob.Run run : layer.runs()) {
                sb.append("    <rect x=\"").append(Fmt.num(off + run.x() * px))
                        .append("\" y=\"").append(Fmt.num(off + run.y() * px))
                        .append("\" width=\"").append(Fmt.num(run.len() * px))
                        .append("\" height=\"").append(Fmt.num(px)).append("\"/>\n");
            }
            sb.append("  </g>\n");
        }

        switch (job.cutMode()) {
            case BOX -> {
                sb.append("  <g id=\"cut\" fill=\"none\" stroke=\"").append(CUT_COLOR)
                        .append("\" stroke-width=\"").append(Fmt.num(CUT_LINE_MM)).append("\">\n");
                sb.append("    <rect x=\"0\" y=\"0\" width=\"").append(width).append("\" height=\"")
                        .append(height).append("\"/>\n");
                sb.append("  </g>\n");
            }
            case OUTLINE -> {
                sb.append("  <g id=\"cut\" fill=\"none\" stroke=\"").append(CUT_COLOR)
                        .append("\" stroke-width=\"").append(Fmt.num(CUT_LINE_MM)).append("\">\n");
                for (fileConverter.core.LaserJob.Loop loop : job.loops()) { // holes come first
                    sb.append("    <path d=\"");
                    for (int i = 0; i < loop.xs().length; i++) {
                        sb.append(i == 0 ? 'M' : 'L')
                                .append(Fmt.num(off + loop.xs()[i] * px)).append(' ')
                                .append(Fmt.num(off + loop.ys()[i] * px));
                        sb.append(i == loop.xs().length - 1 ? " Z" : " ");
                    }
                    sb.append("\"/>\n");
                }
                sb.append("  </g>\n");
            }
            case NONE -> {

            }
        }

        sb.append("</svg>\n");
        return sb.toString();
    }

    private static String safeForComment(String name) {
        return name == null ? "unknown" : name.replaceAll("-{2,}", "-");
    }
}
