/**
 *  Copyright (C) 2002-2019   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.io.sza;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * An animation made from images stored in a zip-file.
 */
public final class SimpleZippedAnimation implements Iterable<AnimationEvent> {

    private static final class ImageAnimationEventImpl
        implements ImageAnimationEvent {

        private static final Component _c = new Component() {};
        
        private final BufferedImage image;
        private final int durationInMs;


        /**
         * Create a new wrapped image animation.
         *
         * @param image The {@code BufferedImage} to wrap.
         * @param durationInMs The animation duration in ms.
         */
        private ImageAnimationEventImpl(final BufferedImage image,
                                        final int durationInMs) {
            this.image = image;
            this.durationInMs = durationInMs;
        }


        /**
         * Create a scaled version of this image animation.
         *
         * @param scale The scale factor to apply.
         * @return The scaled {@code AnimationEvent}.
         */
        private AnimationEvent createScaledVersion(float scale) {
            final int width = (int)(getWidth() * scale);
            final int height = (int)(getHeight() * scale);
            BufferedImage scaled = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(this.image, 0, 0, width, height, null);
            g.dispose();

            return new ImageAnimationEventImpl(scaled, this.durationInMs);
        }

        public int getWidth() {
            return this.image.getWidth(null);
        }

        public int getHeight() {
            return this.image.getHeight(null);
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public Image getImage() {
            return this.image;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public int getDurationInMs() {
            return this.durationInMs;
        }
    }

    private static final Predicate<AnimationEvent> isIAEI = ae ->
        ae instanceof ImageAnimationEvent;
    private static final ToIntFunction<AnimationEvent> ifIAEIWidth = ae ->
        ((ImageAnimationEventImpl)ae).getWidth();
    private static final ToIntFunction<AnimationEvent> ifIAEIHeight = ae ->
        ((ImageAnimationEventImpl)ae).getHeight();
    
    /** The descriptor file to find in the zip files. */
    private static final String ANIMATION_DESCRIPTOR_FILE = "animation.txt";

    /** The animation events. */
    private final List<AnimationEvent> events;

    /** The maximum width of the individual animations. */
    private final int width;
    /** The maximum height of the individual animations. */
    private final int height;


    /**
     * Creates a new animation from a stream generated by the provided URL.
     * 
     * @param url The URL to read a zip-file from. 
     * @exception IOException if the file cannot be opened, or
     *      is invalid.
     */
    public SimpleZippedAnimation(final URL url) throws IOException {
        this(url.openStream());
    }
    
    /**
     * Creates a new animation from an input stream.
     * 
     * @param stream An {@code InputStream} to a zip-file.
     * @exception IOException if the file cannot be opened, or is invalid.
     */
    public SimpleZippedAnimation(final InputStream stream) throws IOException {
        this(new ZipInputStream(stream));
    }

    /**
     * Create an animation from a zip input stream.
     *
     * @param zipStream An {@code ZipInputStream} to a zip-file.
     * @exception IOException if the file cannot be opened, or is invalid.
     */
    private SimpleZippedAnimation(final ZipInputStream zipStream)
        throws IOException {
        this(loadEvents(zipStream));
    }

    /**
     * Create an animation from a list of animation events.
     *
     * @param evl The list of {@code AnimationEvent}s.
     */
    private SimpleZippedAnimation(final List<AnimationEvent> evl) {
        this(evl, max(evl, isIAEI, ifIAEIWidth), max(evl, isIAEI, ifIAEIHeight));
    }

    /**
     * Create an animation from a given list of events and dimensions.
     *
     * @param events The list of {@code AnimationEvent}s.
     * @param width The width of the animation.
     * @param height The height of the animation.
     */
    private SimpleZippedAnimation(final List<AnimationEvent> events,
                                  final int width, final int height) {
        this.events = events;
        this.width = width;
        this.height = height;
    }


    /**
     * Load animation events from a zip stream.
     *
     * @param zipStream An {@code ZipInputStream} to a zip-file.
     * @return A list of {@code AnimationEvent}s.
     * @exception IOException if there is an error with the stream.
     */
    private static List<AnimationEvent> loadEvents(ZipInputStream zipStream)
        throws IOException {
        // Preload all files from the archive since we cannot use a
        // ZipFile for reading (as we should support an arbitrary stream).
        final Map<String, BufferedImage> loadingImages = new HashMap<>();
        final List<String> loadingDescriptor = new ArrayList<>();
        try {
            BufferedReader in;
            ZipEntry ze;
            while ((ze = zipStream.getNextEntry()) != null) {
                if (ANIMATION_DESCRIPTOR_FILE.equals(ze.getName())) {
                    in = new BufferedReader(new InputStreamReader(zipStream,
                            StandardCharsets.UTF_8));
                    String line;
                    while ((line = in.readLine()) != null) {
                        loadingDescriptor.add(line);
                    }
                } else {
                    loadingImages.put(ze.getName(), ImageIO.read(zipStream));
                }
                zipStream.closeEntry();
            }
        } finally {
            try { zipStream.close(); } catch (IOException e) {}
        }
        
        if (loadingDescriptor.isEmpty()) {
            throw new IOException(ANIMATION_DESCRIPTOR_FILE
                + " is missing from the SZA: " + zipStream);
        }
        
        List<AnimationEvent> events = new ArrayList<>(loadingDescriptor.size());
        for (String line : loadingDescriptor) {
            final int idx = line.indexOf('(');
            final int idx2 = line.indexOf("ms)");
            if (idx < 0 || idx2 <= idx) {
                throw new IOException(ANIMATION_DESCRIPTOR_FILE
                    + " should use the format: FILNAME (TIMEms) in: " + line);
            }
            final String imageName = line.substring(0, idx).trim();
            final int ms = Integer.parseInt(line.substring(idx+1, idx2));
            final BufferedImage image = loadingImages.get(imageName);
            if (image == null) {
                throw new IOException("Could not find referenced image: "
                    + imageName);
            }
            events.add(new ImageAnimationEventImpl(image, ms));
        }
        return events;
    }

    /**
     * Creates a scaled animation based on this object.
     * 
     * @param scale The scaling factor (with 1 being normal size,
     *     2 twice the size, 0.5 half the size etc).
     * @return The scaled animation.
     */
    public SimpleZippedAnimation createScaledVersion(final float scale) {
        final Function<AnimationEvent, AnimationEvent> scaleEvent = ae ->
            (ae instanceof ImageAnimationEventImpl)
                ? ((ImageAnimationEventImpl)ae).createScaledVersion(scale)
                : ae;
        return new SimpleZippedAnimation(transform(this.events, alwaysTrue(),
                                                   scaleEvent),
            (int)(this.width * scale), (int)(this.height * scale));
    }

    /**
     * Gets the width of the animation.
     *
     * @return The largest width of all the frames in this animation.
     */
    public int getWidth() {
        return this.width;
    }
    
    /**
     * Gets the height of the animation.
     *
     * @return The largest height of all the frames in this animation.
     */
    public int getHeight() {
        return this.height;
    }
    

    // Implement Iterable<AnimationEvent>

    /**
     * Make this animation iterable.
     *
     * @return An {@code Iterator} with all the images and other
     *     resources (support for sound may be added later).
     */
    @Override
    public Iterator<AnimationEvent> iterator() {
        return Collections.unmodifiableList(this.events).iterator();
    }
}