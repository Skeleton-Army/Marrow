package com.skeletonarmy.marrow.telemetry;

import static com.skeletonarmy.marrow.telemetry.HtmlArgType.ALL_ARGS;
import static com.skeletonarmy.marrow.telemetry.HtmlArgType.LIST;
import static com.skeletonarmy.marrow.telemetry.HtmlArgType.NONE;

import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.internal.opmode.TelemetryImpl;

import java.util.ArrayList;
import java.util.List;

public class MarrowTelemetry implements Telemetry {
    Telemetry telemetry;

    //NOTE: I don't think any other implementation except the official one implements HTML, so be careful with MultipleTelemetry
    public MarrowTelemetry(Telemetry telemetry) {
        this.telemetry = telemetry;
        setDisplayFormat(DisplayFormat.HTML);
    }

    /**
     * add updating key-value pair data to telemetry stream
     *
     * @param caption            the caption to use or {@code null} for no caption
     * @param message            the string by which the arguments are to be formatted
     * @param args               the arguments to format or {@link TelemetryModifier}s to apply to the message
     *                           if the first element is a {@link List<TelemetryModifier>} it will be used,
     *                           and the rest of the args will be used
     *
     * @return the {@link Item} which is printed to the telemetry stream, or {@code null} when {@code caption} is {@code null}
     */

    @Override
    @SuppressWarnings("unchecked")
    public Item addData(String caption, String message, Object... args) {
        HtmlArgType argType = isHtml(args);

        List<TelemetryModifier<?>> modifiers = new ArrayList<>();
        Object[] formatArgs = new Object[args.length -1];

        switch (argType) {
            case LIST: {
                modifiers = (List<TelemetryModifier<?>>) args[0];
                System.arraycopy(args, 1, formatArgs, 0, args.length - 1);
                break;
            }

            case ALL_ARGS: {
                for (Object obj : args) {
                    modifiers.add((TelemetryModifier<?>) obj);
                }
                break;
            }

            case NONE: {
                return telemetry.addData(caption, message, args);
            }
        }

        String msg = new TelemetryFormatter(message, modifiers).format();

        if (caption == null) {
            addLine(msg);
            return null; //TODO: return an actual value, though I don't think it's actually needed
        }

        // when using the List method, the rest of the args should be used for formatting
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
        if (args.length > 0 && args[0] instanceof List) {
            List<?> arg0 = (List<?>) args[0];
            if (!arg0.isEmpty() && arg0.get(0) instanceof TelemetryModifier) {
               return LIST;
            }
        }

        HtmlArgType result = ALL_ARGS;
        for (Object o : args) {
            if (!(o instanceof TelemetryModifier)) {
                result = NONE;
                break;
            }
        }

        return result;
    }

    //------------------|
    // Telemetry Methods|
    //------------------|

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
