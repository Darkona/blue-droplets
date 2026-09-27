package com.darkona.dropletsofthirst.core;

import java.util.Arrays;
import java.util.List;

/**
 * Config lists of {@code "a,b,c"} number rows (altitude bands, curves), parsed once per list instance and flattened.
 * {@link #get} is allocation-free while the list is the same instance (config values keep theirs until reloaded).
 */
public final class NumberRows
{
    private static final class Parsed
    {
        final Object source;
        final double[] values;

        Parsed(Object source, double[] values)
        {
            this.source = source;
            this.values = values;
        }
    }

    private final int columns;
    private volatile Parsed parsed = new Parsed(null, new double[0]);

    public NumberRows(int columns)
    {
        this.columns = columns;
    }

    public double[] get(List<? extends String> rows)
    {
        Parsed current = parsed;
        if (current.source != rows)
            parsed = current = new Parsed(rows, parse(rows, columns));
        return current.values;
    }

    /**
     * Valid rows only, flattened; a row is valid when it has exactly {@code columns} numbers.
     */
    public static double[] parse(List<? extends String> rows, int columns)
    {
        double[] values = new double[rows.size() * columns];
        int length = 0;
        for (String row : rows)
        {
            double[] parsedRow = parseRow(row, columns);
            if (parsedRow == null)
                continue;
            System.arraycopy(parsedRow, 0, values, length, columns);
            length += columns;
        }
        return Arrays.copyOf(values, length);
    }

    public static double[] parseRow(Object row, int columns)
    {
        if (!(row instanceof String))
            return null;
        String[] parts = ((String) row).split(",");
        if (parts.length != columns)
            return null;
        double[] values = new double[columns];
        try
        {
            for (int i = 0; i < columns; i++)
                values[i] = Double.parseDouble(parts[i].trim());
        }
        catch (NumberFormatException e)
        {
            return null;
        }
        return values;
    }

    /**
     * Third column of the first {@code min,max,value} row with {@code min <= y <= max}, or {@code fallback}.
     */
    public static double band(double[] rows, double y, double fallback)
    {
        for (int i = 0; i + 2 < rows.length; i += 3)
            if (y >= rows[i] && y <= rows[i + 1])
                return rows[i + 2];
        return fallback;
    }

    /**
     * Piecewise-linear curve through {@code x,y} points in ascending x, flat beyond the ends; 1 without points.
     */
    public static double curve(double[] points, double x)
    {
        if (points.length < 2)
            return 1.0;
        if (x <= points[0])
            return points[1];
        for (int i = 2; i + 1 < points.length; i += 2)
        {
            if (x <= points[i])
            {
                double x0 = points[i - 2];
                double y0 = points[i - 1];
                double span = points[i] - x0;
                return span <= 0 ? points[i + 1] : y0 + (points[i + 1] - y0) * (x - x0) / span;
            }
        }
        return points[points.length - 1];
    }

    /**
     * Whether the x values of a flattened curve are in ascending order.
     */
    public static boolean ascending(double[] points)
    {
        for (int i = 2; i < points.length; i += 2)
            if (points[i] < points[i - 2])
                return false;
        return true;
    }
}
