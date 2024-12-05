package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    private static final int MIN = 0;
    private static final int MAX = 100;
    private static final int ATTEMPTS = 10;

    private final DrawNumber model;
    private final List<DrawNumberView> views;

    /**
     * @param views
     *            the views to attach
     */
    public DrawNumberApp(final String configFile, final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }
        final Configuration.Builder configurationBuilder = new Configuration.Builder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(configFile)));) {
            for(var configLine = reader.readLine(); Objects.nonNull(configLine); configLine = reader.readLine()) {
                final StringTokenizer tokenizer = new StringTokenizer(configFile, ";");
                while (tokenizer.countTokens() != 0) {
                    switch (tokenizer.nextToken()) {
                        case ("minimum"): configurationBuilder.setMin(Integer.parseInt(tokenizer.nextToken())); 
                            break;
                        case ("maximum"): configurationBuilder.setMax(Integer.parseInt(tokenizer.nextToken()));
                            break;
                        case ("attempts"): configurationBuilder.setAttempts(Integer.parseInt(tokenizer.nextToken()));
                            break;
                        default:
                            break;
                    }
                }
            }            
        } catch (NumberFormatException | IOException e) {
            final String msg = "Exeption relevated";
            displayError(msg);
        }
        final Configuration configuration = configurationBuilder.build();
        if(configuration.isConsistent()) {
            this.model = new DrawNumberImpl(configuration);
        } else {
            displayError("Inconsistent configuration: "
                + "min: " + configuration.getMin() + ", "
                + "max: " + configuration.getMax() + ", "
                + "attempts: " + configuration.getAttempts() + ". Using defaults instead.");
            this.model = new DrawNumberImpl(new Configuration.Builder().build());
        }
    }

    private void displayError(final String message) {
        for(final DrawNumberView view : views ) {
            view.displayError(message);
        }
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view: views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view: views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * @param args
     *            ignored
     * @throws FileNotFoundException 
     */
    public static void main(final String... args) throws FileNotFoundException {
        new DrawNumberApp("config.yml", // res is part of the classpath!
        new DrawNumberViewImpl(),
        new DrawNumberViewImpl(),
        new PrintStreamView(System.out),
        new PrintStreamView("output.log"));
    }

}
