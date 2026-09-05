package com.skeletonarmy.marrow.telemetry;

import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

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
     * @param args               the arguments to format or {@link TelemetryModifier}s to apply to s1
     *
     * @return the {@link Item} which is printed to the telemetry stream
     */

    @Override
    // maybe a add a way to also do data AND HTML. maybe have args[0] be an instance of List<TelemetryModifier>
    // do note that instanceof doesn't work with generic types at runtime, so maybe check the first element, but that seems risky.
    public Item addData(String caption, String message, Object... args) {
        boolean isHTML = true;

        for (Object o : args) {
            if (!(o instanceof TelemetryModifier))  {
                isHTML = false;
                break;
            }
        }

        String formattedStr = "";

        if (isHTML) {
            List<TelemetryModifier<?>> origList = new ArrayList<>();
            for (Object o : args) {
                origList.add((TelemetryModifier<?>) o);
            }

            formattedStr = new TelemetryFormatter(message, new ArrayList<>(origList)).format();
        }

        if (isHTML && !formattedStr.isEmpty()) {
            if (caption == null) {
                return (Item) addLine(formattedStr);
            } else {
                return addData(caption, formattedStr);
            }
        }

        return telemetry.addData(caption, message, args);
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
