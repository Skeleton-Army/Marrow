package com.skeletonarmy.marrow.telemetry;

import static com.skeletonarmy.marrow.telemetry.HtmlArgType.ALL_ARGS;
import static com.skeletonarmy.marrow.telemetry.HtmlArgType.BUILDER;
import static com.skeletonarmy.marrow.telemetry.HtmlArgType.LIST;
import static com.skeletonarmy.marrow.telemetry.HtmlArgType.NONE;

import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MarrowTelemetry implements Telemetry {
    Telemetry telemetry;
    DecimalFormat decimalFormat;

    //NOTE: I don't think any other implementation except the official one implements HTML, so be careful with MultipleTelemetry
    public MarrowTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
        setDisplayFormat(DisplayFormat.HTML);

        decimalFormat = new DecimalFormat("0.####");
    }

    /**
     * Adds a piece of formatted telemetry data.
     * <p>
     *
     * Passing a non-empty {@code caption} adds a caption/value pair, same as the
     * standard {@code Telemetry.addData}. Passing {@code null} or an empty
     * string for {@code caption} instead, adds {@code message} as a standalone
     * telemetry line (see {@link #addLine(String)})  useful for formatted
     * text, or data that shouldn't be paired with a caption.
     *
     * @param caption     the caption to display, or {@code null}/empty to add {@code message} as a standalone line
     * @param message     the message or value to format and display
     * @param args        the modifiers and arguments to format {@code message} with
     *
     * @return            the created telemetry item, or {@code null} when added as a standalone line
     */
    @Override
    @SuppressWarnings("unchecked")
    public Item addData(String caption, String message, Object... args) {
        HtmlArgType argType = isHtml(args);

        List<TelemetryModifier> modifiers = new ArrayList<>();
        Object[] formatArgs = new Object[args.length -1];

        // format double like normal telemetry
        if (isDouble(message)) {
            message = decimalFormat.format(Double.parseDouble(message)); // this sucks
        }

        // setup modifiers and format args
        switch (argType) {
            case LIST: {
                modifiers = (List<TelemetryModifier>) args[0];
                System.arraycopy(args, 1, formatArgs, 0, args.length - 1);
                break;
            }

            case ALL_ARGS: {
                for (Object obj : args) {
                    modifiers.add((TelemetryModifier) obj);
                }
                break;
            }

            case BUILDER: {
                modifiers = ((FormatBuilder) args[0]).setBase(message).getModifiers();
                System.arraycopy(args, 1, formatArgs, 0, args.length - 1);
                break;
            }

            case NONE: {
                return telemetry.addData(caption, message, args);
            }
        }

        // format the message
        String msg = new TelemetryFormatter(message, modifiers).format();

        // pick display method
        if (caption == null || caption.isEmpty()) {
            addLine(msg);
            return null; //TODO: return an actual value, though I don't think it's actually needed
        }

        // when using the List or builder method, the rest of the args should be used for formatting
        if (formatArgs.length > 0) {
            return telemetry.addData(caption, msg, formatArgs);
        }

        return telemetry.addData(caption, msg);
    }

    /**
     * validate and check the addData valist
     *
     * @param args addData args
     * @return the type of args given
     */

    private HtmlArgType isHtml(Object[] args) {
        if (args.length > 0 && args[0] instanceof FormatBuilder) {
            return BUILDER;
        }

        if (args.length > 0 && args[0] instanceof List) {
            List<?> arg0 = (List<?>) args[0];
            if (!arg0.isEmpty() && arg0.get(0) instanceof TelemetryModifier) {
               return LIST;
            }
        }

        for (Object o : args) {
            if (!(o instanceof TelemetryModifier)) {
                return NONE;
            }
        }

        return ALL_ARGS;
    }

    private boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
           return false;
        }
    }

    /*
    |-------------------|
    | Telemetry Methods |
    |-------------------|
     */

    @Override
    public Item addData(String caption, Object value) {
        return telemetry.addData(caption, value);
    }

    @Override
    public <T> Item addData(String caption, Func<T> valueProducer) {
        return telemetry.addData(caption, valueProducer);
    }

    @Override
    public <T> Item addData(String caption, String format, Func<T> valueProducer) {
        return telemetry.addData(caption, format, valueProducer);
    }

    @Override
    public boolean removeItem(Item item) {
        return telemetry.removeItem(item);
    }

    @Override
    public void clear() {
        telemetry.clear();
    }

    @Override
    public void clearAll() {
        telemetry.clearAll();
    }

    @Override
    public Object addAction(Runnable action) {
        return telemetry.addAction(action);
    }

    @Override
    public boolean removeAction(Object token) {
        return telemetry.removeAction(token);
    }

    @Override
    public void speak(String text) {
        telemetry.speak(text);
    }

    @Override
    public void speak(String text, String languageCode, String countryCode) {
        telemetry.speak(text, languageCode, countryCode);
    }

    @Override
    public boolean update() {
        return telemetry.update();
    }

    @Override
    public Line addLine() {
        return telemetry.addLine();
    }

    @Override
    public Line addLine(String lineCaption) {
        return telemetry.addLine(lineCaption);
    }

    @Override
    public boolean removeLine(Line line) {
        return telemetry.removeLine(line);
    }

    @Override
    public boolean isAutoClear() {
        return telemetry.isAutoClear();
    }

    @Override
    public void setAutoClear(boolean autoClear) {
        telemetry.setAutoClear(autoClear);
    }

    @Override
    public int getMsTransmissionInterval() {
        return telemetry.getMsTransmissionInterval();
    }

    @Override
    public void setMsTransmissionInterval(int msTransmissionInterval) {
        telemetry.setMsTransmissionInterval(msTransmissionInterval);
    }

    @Override
    public String getItemSeparator() {
        return telemetry.getItemSeparator();
    }

    @Override
    public void setItemSeparator(String itemSeparator) {
        telemetry.setItemSeparator(itemSeparator);
    }

    @Override
    public String getCaptionValueSeparator() {
        return telemetry.getCaptionValueSeparator();
    }

    @Override
    public void setCaptionValueSeparator(String captionValueSeparator) {
        telemetry.setCaptionValueSeparator(captionValueSeparator);
    }

    @Override
    public void setDisplayFormat(DisplayFormat displayFormat) {
        telemetry.setDisplayFormat(displayFormat);
    }

    @Override
    public Log log() {
        return telemetry.log();
    }
}
