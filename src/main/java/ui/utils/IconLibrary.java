package ui.utils;

import javafx.scene.shape.SVGPath;
import javafx.scene.paint.Color;

public class IconLibrary {

    // Lucide-style stroke icons (stroke-linecap="round", stroke-width="2")
    public static final String SEARCH    = "M21 21l-4.35-4.35M19 11a8 8 0 11-16 0 8 8 0 0116 0z";
    public static final String CART      = "M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4H6z M3 6h18 M16 10a4 4 0 01-8 0";
    public static final String USER      = "M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2 M12 11a4 4 0 100-8 4 4 0 000 8z";
    public static final String HOME      = "M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z M9 22V12h6v10";
    public static final String PHONE     = "M5 2h14a2 2 0 012 2v16a2 2 0 01-2 2H5a2 2 0 01-2-2V4a2 2 0 012-2z M12 18h.01";
    public static final String HEADPHONE = "M3 18v-6a9 9 0 0118 0v6 M21 19a2 2 0 01-2 2h-1a2 2 0 01-2-2v-3a2 2 0 012-2h3 M3 19a2 2 0 002 2h1a2 2 0 002-2v-3a2 2 0 00-2-2H3";
    public static final String WATCH     = "M12 22a8 8 0 100-16 8 8 0 000 16z M12 6V2 M12 22v-4 M16 2h-8 M16 22h-8 M12 12v-4 M12 12l3 3";
    public static final String LAPTOP    = "M2 16h20 M20 16a2 2 0 002-2V5a2 2 0 00-2-2H4a2 2 0 00-2 2v9a2 2 0 002 2M2 16l-2 4h24l-2-4";
    public static final String STAR      = "M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z";
    public static final String STAR_EMPTY= "M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z";
    public static final String TRASH     = "M3 6h18 M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2";
    public static final String PLUS      = "M12 5v14M5 12h14";
    public static final String MINUS     = "M5 12h14";
    public static final String ARROW_R   = "M5 12h14 M12 5l7 7-7 7";
    public static final String ARROW_L   = "M19 12H5 M12 19l-7-7 7-7";
    public static final String LOCK      = "M19 11H5a2 2 0 00-2 2v7a2 2 0 002 2h14a2 2 0 002-2v-7a2 2 0 00-2-2z M7 11V7a5 5 0 0110 0v4";
    public static final String HEART     = "M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z";
    public static final String CHECK     = "M20 6L9 17l-5-5";
    public static final String CLOSE     = "M18 6L6 18 M6 6l12 12";
    public static final String SETTINGS  = "M12 15a3 3 0 100-6 3 3 0 000 6z M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09a1.65 1.65 0 00-1-1.51 1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06a1.65 1.65 0 00.33-1.82 1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09a1.65 1.65 0 001.51-1 1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06a1.65 1.65 0 001.82.33 1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001-1.51 1.65 1.65 0 001.82-.33l.06.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82 1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z";
    public static final String CATEGORY  = "M3 3h7v7H3z M14 3h7v7h-7z M14 14h7v7h-7z M3 14h7v7H3z";
    public static final String TAG       = "M20.59 13.41l-7.17 7.17a2 2 0 01-2.83 0L2 12V2h10l8.59 8.59a2 2 0 010 2.82z M7 7h.01";
    public static final String TRUCK     = "M1 3h15v13H1z M16 8h4l3 3v5h-7V8z M5.5 21a2.5 2.5 0 100-5 2.5 2.5 0 000 5z M18.5 21a2.5 2.5 0 100-5 2.5 2.5 0 000 5z";
    public static final String PACKAGE   = "M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z M3.27 6.96L12 12.01l8.73-5.05 M12 22.08V12";

    public static SVGPath getIcon(String pathData, double size, String hexColor) {
        SVGPath svg = new SVGPath();
        svg.setContent(pathData);
        svg.setFill(null); // Default to stroke only
        svg.setStroke(Color.web(hexColor));
        svg.setStrokeWidth(2.0);
        // Scaling
        double width = svg.getBoundsInLocal().getWidth();
        double height = svg.getBoundsInLocal().getHeight();
        double scale = size / Math.max(width, height);
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        return svg;
    }
    
    public static SVGPath getFilledIcon(String pathData, double size, String hexColor) {
        SVGPath svg = new SVGPath();
        svg.setContent(pathData);
        svg.setFill(Color.web(hexColor));
        svg.setStroke(null);
        double width = svg.getBoundsInLocal().getWidth();
        double height = svg.getBoundsInLocal().getHeight();
        double scale = size / Math.max(width, height);
        svg.setScaleX(scale);
        svg.setScaleY(scale);
        return svg;
    }
}
