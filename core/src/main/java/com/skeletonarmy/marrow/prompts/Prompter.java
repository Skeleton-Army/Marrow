package com.skeletonarmy.marrow.prompts;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.skeletonarmy.marrow.internal.Button;
import com.skeletonarmy.marrow.internal.GamepadInput;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Prompter {
    private final OpMode opMode;
    private final List<PromptEntry<?>> entries = new ArrayList<>();
    private final Map<String, Object> results = new HashMap<>();

    private Runnable completeFunc = null;
    private int currentIndex = 0;
    private boolean isCompleted = false;
    private boolean showSummary = false;
    private boolean inSummary = false;

    public Prompter(OpMode opMode) {
        this.opMode = opMode;
    }

    // ---- API ----

    /**
     * Adds a prompt to the queue.
     * Returns a {@link PromptHandle} for chaining conditions and callbacks.
     */
    public <T> PromptHandle prompt(String key, Prompt<T> prompt) {
        requireValidKey(key);
        PromptEntry<T> entry = new PromptEntry<>(key, prompt);
        entries.add(entry);
        return new PromptHandle(entry);
    }

    /**
     * @deprecated Use {@link #prompt(String, Prompt)} with {@link PromptHandle#showIf} instead.
     */
    @Deprecated
    public <T> Prompter prompt(String key, Supplier<Prompt<T>> promptSupplier) {
        requireValidKey(key);
        entries.add(new PromptEntry<>(key, promptSupplier));
        return this;
    }

    /**
     * Gets the chosen value of a prompt from its key.
     *
     * @param key The prompt's key
     * @return The value of the prompt result
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        requireValidKey(key);
        if (!results.containsKey(key)) throw new NoSuchElementException("No result found for key '" + key + "'. Ensure prompts have been executed, or use getOrDefault() if the result may be absent.");
        return (T) results.get(key);
    }

    /**
     * Gets the chosen value of a prompt from its key.
     *
     * @param key The prompt's key
     * @param defaultValue The value to return if no result exists for the key
     * @return The value of the prompt result
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T defaultValue) {
        requireValidKey(key);
        return (T) results.getOrDefault(key, defaultValue);
    }

    /**
     * Sets a function to run once all prompts are complete.
     */
    public Prompter onComplete(Runnable func) {
        completeFunc = func;
        return this;
    }

    /**
     * Shows a summary screen after all prompts are complete.
     * The driver must confirm before {@link #onComplete} fires.
     */
    public Prompter withSummary() {
        showSummary = true;
        return this;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    // ---- CONDITION HELPERS ----

    /**
     * Returns a {@link BooleanSupplier} that is true if ALL of the given keys have a truthy result.
     */
    public BooleanSupplier all(String... keys) {
        return () -> {
            for (String key : keys)
                if (!Boolean.TRUE.equals(getOrDefault(key, false))) return false;
            return true;
        };
    }

    /**
     * Returns a {@link BooleanSupplier} that is true if ANY of the given keys have a truthy result.
     */
    public BooleanSupplier any(String... keys) {
        return () -> {
            for (String key : keys)
                if (Boolean.TRUE.equals(getOrDefault(key, false))) return true;
            return false;
        };
    }

    // ---- LIFECYCLE ----

    /**
     * Runs the prompt queue. Should be called in a loop.
     */
    public void run() {
        if (isCompleted) return;

        GamepadInput.update(opMode.gamepad1, opMode.gamepad2);

        if (inSummary) {
            runSummary();
        } else if (processPrompts()) {
            if (showSummary) inSummary = true;
            else complete();
        }

        opMode.telemetry.update();
    }

    // ---- INTERNALS ----

    private void complete() {
        isCompleted = true;
        opMode.telemetry.clear();
        if (completeFunc != null) completeFunc.run();
    }

    private void runSummary() {
        opMode.telemetry.addLine("=== CONFIRM SETTINGS ===");
        opMode.telemetry.addLine("");

        for (PromptEntry<?> entry : entries) {
            if (!results.containsKey(entry.key)) continue;
            opMode.telemetry.addData(entry.label != null ? entry.label : entry.key, results.get(entry.key));
        }

        opMode.telemetry.addLine("");
        opMode.telemetry.addLine("Press CROSS/A to confirm");
        opMode.telemetry.addLine("Press CIRCLE/B to go back");

        if (GamepadInput.justPressed(Button.A)) {
            inSummary = false;
            complete();
        } else if (GamepadInput.justPressed(Button.B)) {
            inSummary = false;
            goBackTo(entries.size() - 1);
        }
    }

    /**
     * @return true if all prompts are finished
     */
    private boolean processPrompts() {
        if (GamepadInput.justPressed(Button.B) && currentIndex > 0)
            goBackTo(currentIndex - 1);

        if (currentIndex >= entries.size()) return true;

        PromptEntry<?> entry = entries.get(currentIndex);

        if (shouldSkip(entry)) {
            currentIndex++;
            return false;
        }

        Prompt<?> prompt = entry.getPrompt(opMode.telemetry);
        if (prompt == null) {
            currentIndex++;
            return false;
        }

        Object result = prompt.process();
        if (result != null) {
            results.put(entry.key, result);
            if (entry.onAnswer != null) entry.onAnswer.accept(result);
            currentIndex++;
        }

        return false;
    }

    private void goBackTo(int index) {
        // Clear forward from current
        while (currentIndex > index) {
            PromptEntry<?> e = entries.get(currentIndex);
            e.reset();
            results.remove(e.key);
            currentIndex--;
        }

        // Skip over invisible entries going backwards
        while (currentIndex > 0 && shouldSkip(entries.get(currentIndex))) {
            currentIndex--;
        }

        // Clear the entry we land on so it can be re-answered
        PromptEntry<?> landed = entries.get(currentIndex);
        landed.reset();
        results.remove(landed.key);
    }

    private boolean shouldSkip(PromptEntry<?> entry) {
        return !entry.isVisible() || results.containsKey(entry.key);
    }

    private static void requireValidKey(String key) {
        if (key == null) throw new IllegalArgumentException("Key cannot be null.");
        if (key.isEmpty()) throw new IllegalArgumentException("Key cannot be empty.");
    }

    public class PromptHandle {
        private final PromptEntry<?> entry;

        PromptHandle(PromptEntry<?> entry) {
            this.entry = entry;
        }

        /**
         * Show this prompt only if the given key's result equals the given value.
         * Multiple calls are AND-ed together.
         */
        public PromptHandle showIf(String key, Object value) {
            entry.addCondition(() -> value.equals(getOrDefault(key, null)));
            return this;
        }

        /**
         * Show this prompt only if the given supplier returns true.
         * Multiple calls are AND-ed together.
         * Useful with {@link Prompter#any(String...)} and {@link Prompter#all(String...)}.
         */
        public PromptHandle showIf(BooleanSupplier condition) {
            entry.addCondition(condition);
            return this;
        }

        /**
         * OR the previous condition with a new one based on key equality.
         */
        public PromptHandle or(String key, Object value) {
            BooleanSupplier last = entry.removeLastCondition();
            entry.addCondition(() -> last.getAsBoolean() || value.equals(getOrDefault(key, null)));
            return this;
        }

        /**
         * OR the previous condition with a raw supplier.
         */
        public PromptHandle or(BooleanSupplier condition) {
            BooleanSupplier last = entry.removeLastCondition();
            entry.addCondition(() -> last.getAsBoolean() || condition.getAsBoolean());
            return this;
        }

        /** Sets a display label for this entry in the summary screen. */
        public PromptHandle label(String label) {
            entry.label = label;
            return this;
        }

        /** Fires a callback when this prompt is answered. */
        @SuppressWarnings("unchecked")
        public <T> PromptHandle then(Consumer<T> callback) {
            entry.onAnswer = o -> callback.accept((T) o);
            return this;
        }

        // Delegate to Prompter for continued chaining

        public <T> PromptHandle prompt(String key, Prompt<T> prompt) {
            return Prompter.this.prompt(key, prompt);
        }

        public Prompter onComplete(Runnable func) {
            return Prompter.this.onComplete(func);
        }

        public Prompter withSummary() {
            return Prompter.this.withSummary();
        }
    }

    private static class PromptEntry<T> {
        final String key;
        String label = null;
        Consumer<Object> onAnswer = null;

        private final List<BooleanSupplier> conditions = new ArrayList<>();
        private final Prompt<T> promptInstance;

        // Deprecated path
        private final Supplier<Prompt<T>> promptSupplier;
        private Prompt<T> supplierResult;

        @SuppressWarnings("unchecked")
        PromptEntry(String key, Prompt<?> prompt) {
            this.key = key;
            this.promptInstance = (Prompt<T>) prompt;
            this.promptSupplier = null;
        }

        PromptEntry(String key, Supplier<Prompt<T>> supplier) {
            this.key = key;
            this.promptInstance = null;
            this.promptSupplier = supplier;
        }

        void addCondition(BooleanSupplier condition) {
            conditions.add(condition);
        }

        BooleanSupplier removeLastCondition() {
            if (conditions.isEmpty()) throw new IllegalStateException("No conditions to remove for key '" + key + "'. Call showIf() before or().");
            return conditions.remove(conditions.size() - 1);
        }

        boolean isVisible() {
            for (BooleanSupplier c : conditions)
                if (!c.getAsBoolean()) return false;
            return true;
        }

        Prompt<T> getPrompt(Telemetry telemetry) {
            if (promptInstance != null) {
                promptInstance.configure(telemetry);
                return promptInstance;
            }

            // Deprecated
            if (supplierResult == null && promptSupplier != null) {
                supplierResult = promptSupplier.get();
                if (supplierResult != null) supplierResult.configure(telemetry);
            }
            return supplierResult;
        }

        void reset() {
            supplierResult = null;
        }
    }
}
