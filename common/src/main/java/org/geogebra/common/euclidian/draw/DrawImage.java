/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

/*
 * DrawPoint.java
 *
 * Created on 11. Oktober 2001, 23:59
 */

package org.geogebra.common.euclidian.draw;

import org.geogebra.common.awt.GAffineTransform;
import org.geogebra.common.awt.GAlphaComposite;
import org.geogebra.common.awt.GColor;
import org.geogebra.common.awt.GComposite;
import org.geogebra.common.awt.GGeneralPath;
import org.geogebra.common.awt.GGraphics2D;
import org.geogebra.common.awt.GPoint2D;
import org.geogebra.common.awt.GRectangle;
import org.geogebra.common.awt.GRectangle2D;
import org.geogebra.common.awt.GShape;
import org.geogebra.common.awt.MyImage;
import org.geogebra.common.euclidian.BoundingBox;
import org.geogebra.common.euclidian.Drawable;
import org.geogebra.common.euclidian.EuclidianBoundingBoxHandler;
import org.geogebra.common.euclidian.EuclidianView;
import org.geogebra.common.euclidian.event.AbstractEvent;
import org.geogebra.common.factories.AwtFactory;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoImage;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.main.Feature;
import org.geogebra.common.util.DoubleUtil;
import org.geogebra.common.util.debug.Log;

/**
 * 
 * @author Markus
 */
public final class DrawImage extends Drawable {

	private GeoImage geoImage;
	private boolean isVisible;
	private MyImage image;

	private boolean absoluteLocation;
	private GAlphaComposite alphaComp;
	private double alpha = -1;
	private boolean isInBackground = false;
	private GAffineTransform at, atInverse, tempAT;
	private boolean needsInterpolationRenderingHint;
	private int screenX, screenY;
	private GRectangle classicBoundingBox;
	private GGeneralPath highlighting;
	private double[] hitCoords = new double[2];
	private BoundingBox boundingBox;
	private GRectangle2D cropBox;
	private double originalRatio = Double.NaN;
	private boolean wasCroped = false;
	final private static boolean DEBUG = false;
	/**
	 * ratio of the whole image and the crop box width
	 */
	private double imagecropRatioX;
	/**
	 * ratio of the whole image and the crop box width
	 */
	private double imagecropRatioY;
	/**
	 * the image should have at least 100px width
	 */
	public final static int IMG_WIDTH_THRESHOLD = 100;
	/**
	 * the croped image should have at least 50px width
	 */
	public final static int IMG_CROP_THRESHOLD = 50;

	/**
	 * Creates new drawable image
	 * 
	 * @param view
	 *            view
	 * @param geoImage
	 *            image
	 */
	public DrawImage(EuclidianView view, GeoImage geoImage) {
		this.view = view;
		this.geoImage = geoImage;
		geo = geoImage;

		// temp
		at = AwtFactory.getPrototype().newAffineTransform();
		tempAT = AwtFactory.getPrototype().newAffineTransform();
		classicBoundingBox = AwtFactory.getPrototype().newRectangle();

		selStroke = AwtFactory.getPrototype().newMyBasicStroke(1.5f);

		update();
	}

	private void debug(String d) {
		if (DEBUG) {
			Log.debug(d);
		}
	}

	private void debugPoints(GeoPoint A, GeoPoint B, GeoPoint D){
		if (!DEBUG) {
			return;
		}
		if (A != null) {
			debug("A: " + A.getInhomX() + ", " + A.getInhomY());
		} else {
			debug("A is null");
		}
		if (B != null) {
			debug("B: " + B.getInhomX() + ", " + B.getInhomY());
		} else {
			debug("B is null");
		}
		if (D != null) {
			debug("D: " + D.getInhomX() + ", " + D.getInhomY());
		} else {
			debug("D is null");
		}
	}
	
	@Override
	public void update() {
		isVisible = geo.isEuclidianVisible();

		if (!isVisible) {
			return;
		}

		if (geo.getAlphaValue() != alpha) {
			alpha = geo.getAlphaValue();
			alphaComp = AwtFactory.getPrototype().newAlphaComposite(alpha);
		}

		image = geoImage.getFillImage();
		int width = image.getWidth();
		int height = image.getHeight();
		absoluteLocation = geoImage.isAbsoluteScreenLocActive();

		// ABSOLUTE SCREEN POSITION
		if (absoluteLocation){
			// scaleX and scaleY should be 1 if there is no MOW_PIN_IMAGE
			// feature flag, so in that case there is no any effect of these
			double scaleX = geoImage.getScaleX();
			double scaleY = geoImage.getScaleY();
			screenX = geoImage.getAbsoluteScreenLocX();
			screenY = (int) (geoImage.getAbsoluteScreenLocY() - height * scaleY);
			if (geo.getKernel().getApplication().has(Feature.MOW_PIN_IMAGE)) {
				classicBoundingBox.setBounds(screenX, screenY, (int) (width * scaleX), (int) (height * scaleY));
			}
			labelRectangle.setBounds(screenX, screenY, (int) (width * scaleX), (int) (height * scaleY));
		}

		// RELATIVE SCREEN POSITION
		else {
			boolean center = geoImage.isCentered();
			GeoPoint A = geoImage.getCorner(center ? 3 : 0);
			GeoPoint B = center ? null : geoImage.getCorner(1);
			GeoPoint D = center ? null : geoImage.getCorner(2);
			
			debug("points in update: ");
			debugPoints(A, B, D);    

			double ax = 0;
			double ay = 0;
			if (A != null) {
				if (!A.isDefined() || A.isInfinite()) {
					isVisible = false;
					return;
				}
				ax = A.inhomX;
				ay = A.inhomY;

				debug("A: " + ax + ", " + ay);
			}

			// set transform according to corners
			at.setTransform(view.getCoordTransform()); // last transform: real
														// world
														// -> screen

			at.translate(ax, ay); // translate to first corner A

			if (B == null) {
				// we only have corner A
				if (D == null) {
					// use original pixel width and height of image
					at.scale(view.getInvXscale(),
							// make sure old files work
							// https://dev.geogebra.org/trac/changeset/57611
							geo.getKernel().getApplication().fileVersionBefore(
									new int[] { 5, 0, 397, 0 })
											? -view.getInvXscale()
											: -view.getInvYscale());
				}
				// we have corners A and D
				else {
					if (!D.isDefined() || D.isInfinite()) {
						isVisible = false;
						return;
					}
					// rotate to coord system (-ADn, AD)
					double ADx = D.inhomX - ax;
					double ADy = D.inhomY - ay;
					tempAT.setTransform(ADy, -ADx, ADx, ADy, 0, 0);
					at.concatenate(tempAT);

					// scale height of image to 1
					double yscale = 1.0 / height;
					at.scale(yscale, -yscale);
				}
			} else {
				if (!B.isDefined() || B.isInfinite()) {
					isVisible = false;
					return;
				}

				// we have corners A and B
				if (D == null) {
					// rotate to coord system (AB, ABn)
					double ABx = B.inhomX - ax;
					double ABy = B.inhomY - ay;
					tempAT.setTransform(ABx, ABy, -ABy, ABx, 0, 0);
					at.concatenate(tempAT);

					// scale width of image to 1
					double xscale = 1.0 / width;
					at.scale(xscale, -xscale);
				} else { // we have corners A, B and D
					if (!D.isDefined() || D.isInfinite()) {
						isVisible = false;
						return;
					}

					// shear to coord system (AB, AD)
					double ABx = B.inhomX - ax;
					double ABy = B.inhomY - ay;
					double ADx = D.inhomX - ax;
					double ADy = D.inhomY - ay;
					tempAT.setTransform(ABx, ABy, ADx, ADy, 0, 0);
					at.concatenate(tempAT);

					// scale width and height of image to 1
					at.scale(1.0 / width, -1.0 / height);
				}
			}

			if (geoImage.isCentered()) {
				// move image to the center
				at.translate(-width / 2.0, -height / 2.0);
			} else {
				// move image up so that A becomes lower left corner
				at.translate(0, -height);
			}
			labelRectangle.setBounds(0, 0, width, height);

			// calculate bounding box for isInside
			classicBoundingBox.setBounds(0, 0, width, height);
			GShape shape = at.createTransformedShape(classicBoundingBox);
			classicBoundingBox = shape.getBounds();

			try {
				// for hit testing
				atInverse = at.createInverse();
			} catch (Exception e) {
				isVisible = false;
				return;
			}

			// improve rendering for sheared and scaled images (translations
			// don't need this)
			// turns false if the image doen't want interpolation
			needsInterpolationRenderingHint = (geoImage.isInterpolate())
					&& (!isTranslation(at) || view.getPixelRatio() != 1);
		}

		if (isInBackground != geoImage.isInBackground()) {
			isInBackground = !isInBackground;
			if (isInBackground) {
				view.addBackgroundImage(this);
			} else {
				view.removeBackgroundImage(this);
				view.updateBackgroundImage();
			}
		}

		if (!view.isBackgroundUpdating() && isInBackground) {
			view.updateBackgroundImage();
		}
		if (geo.getKernel().getApplication().has(
				Feature.MOW_PIN_IMAGE) && getBounds() != null) {
				getBoundingBox().setRectangle(getBounds());
		}

		if (geo.getKernel().getApplication().has(Feature.MOW_PIN_IMAGE)) {
			if (this.wasCroped && getCropBox() != null) {
				getBoundingBox().setRectangle(getCropBox().getBounds());
			} else if (getBounds() != null) {
				getBoundingBox().setRectangle(getBounds());
			}
		}
	}

	private static boolean isTranslation(GAffineTransform at2) {
		return DoubleUtil.isEqual(at2.getScaleX(), 1.0, Kernel.MAX_PRECISION)
				&& DoubleUtil.isEqual(at2.getScaleY(), 1.0, Kernel.MAX_PRECISION)
				&& DoubleUtil.isEqual(at2.getShearX(), 0.0, Kernel.MAX_PRECISION)
				&& DoubleUtil.isEqual(at2.getShearY(), 0.0, Kernel.MAX_PRECISION);
	}

	/**
	 * If background flag changed, do immediate update. Otherwise mark for
	 * update after next repaint.
	 * 
	 * @return whether it was in background for the whole time
	 */
	public boolean checkInBackground() {
		if (isInBackground != geoImage.isInBackground()) {
			update();
		} else {
			setNeedsUpdate(true);
		}
		return isInBackground && geoImage.isInBackground();
	}

	@Override
	public void draw(GGraphics2D g3) {
		if (isVisible) {
			GComposite oldComp = g3.getComposite();
			if (alpha >= 0f && alpha < 1f) {
				if (alphaComp == null) {
					alphaComp = AwtFactory.getPrototype()
							.newAlphaComposite(alpha);
				}
				g3.setComposite(alphaComp);
			}

			if (absoluteLocation) {
				g3.saveTransform();
				g3.translate(screenX, screenY);
				g3.scale(geoImage.getScaleX(), geoImage.getScaleY());
				g3.translate(-screenX, -screenY);
				g3.drawImage(image, screenX, screenY);
				g3.restoreTransform();
				if (!isInBackground && geo.doHighlighting()) {
					// draw rectangle around image
					g3.setStroke(selStroke);
					g3.setPaint(GColor.LIGHT_GRAY);
					g3.draw(labelRectangle);
				}
			} else {
				g3.saveTransform();
				g3.transform(at);

				// improve rendering quality for transformed images
				Object oldInterpolationHint = g3
						.setInterpolationHint(needsInterpolationRenderingHint);
				if (getBoundingBox().isCropBox()) {
					g3.setComposite(
							AwtFactory.getPrototype().newAlphaComposite(0.5f));
				}
				if (!wasCroped || getBoundingBox().isCropBox()) {
					g3.drawImage(image, 0, 0);
				}
				if (getBoundingBox().isCropBox() || wasCroped) {
					GRectangle2D drawRectangle = wasCroped ? cropBox : getBoundingBox().getRectangle();
					g3.setComposite(
							AwtFactory.getPrototype().newAlphaComposite(1.0f));
					GPoint2D ptScr = AwtFactory.getPrototype().newPoint2D(
							drawRectangle.getX(), drawRectangle.getY());
					GPoint2D ptDst = AwtFactory.getPrototype().newPoint2D();
					atInverse.transform(ptScr, ptDst);
					GShape shape = atInverse.createTransformedShape(drawRectangle);
					g3.drawImage(image,
							(int) ptDst.getX(), (int) ptDst.getY(),
							(int) shape.getBounds().getWidth(),
							(int) shape.getBounds().getHeight(),
							(int) ptDst.getX(), (int) ptDst.getY());
				}

				g3.restoreTransform();
				if (!isInBackground && geo.doHighlighting()) {
					// draw rectangle around image
					g3.setStroke(selStroke);
					g3.setPaint(GColor.LIGHT_GRAY);

					// changed to code below so that the line thicknesses aren't
					// transformed
					// g2.draw(labelRectangle);

					// draw parallelogram around edge
					GPoint2D corner1 = AwtFactory.getPrototype().newPoint2D(
							labelRectangle.getMinX(), labelRectangle.getMinY());
					GPoint2D corner2 = AwtFactory.getPrototype().newPoint2D(
							labelRectangle.getMinX(), labelRectangle.getMaxY());
					GPoint2D corner3 = AwtFactory.getPrototype().newPoint2D(
							labelRectangle.getMaxX(), labelRectangle.getMaxY());
					GPoint2D corner4 = AwtFactory.getPrototype().newPoint2D(
							labelRectangle.getMaxX(), labelRectangle.getMinY());
					at.transform(corner1, corner1);
					at.transform(corner2, corner2);
					at.transform(corner3, corner3);
					at.transform(corner4, corner4);
					if (highlighting == null) {
						highlighting = AwtFactory.getPrototype()
								.newGeneralPath();
					} else {
						highlighting.reset();
					}
					highlighting.moveTo(corner1.getX(), corner1.getY());
					highlighting.lineTo(corner2.getX(), corner2.getY());
					highlighting.lineTo(corner3.getX(), corner3.getY());
					highlighting.lineTo(corner4.getX(), corner4.getY());
					highlighting.lineTo(corner1.getX(), corner1.getY());
					if (!geoImage.getKernel().getApplication()
							.isWhiteboardActive()) {
						// no highlight if we have bounding box for mow
						g3.draw(highlighting);
					}

				}

				// reset previous values
				g3.resetInterpolationHint(oldInterpolationHint);
			}

			g3.setComposite(oldComp);
		}
	}

	/**
	 * Returns whether this is background image
	 * 
	 * @return true for background images
	 */
	boolean isInBackground() {
		return geoImage.isInBackground();
	}

	/**
	 * was this object clicked at? (mouse pointer location (x,y) in screen
	 * coords)
	 */
	@Override
	public boolean hit(int x, int y, int hitThreshold) {
		if (!isVisible || geoImage.isInBackground()) {
			return false;
		}

		hitCoords[0] = x;
		hitCoords[1] = y;

		// convert screen to image coordinate system
		if (!geoImage.isAbsoluteScreenLocActive()) {
			atInverse.transform(hitCoords, 0, hitCoords, 0, 1);
		}
		return labelRectangle.contains(hitCoords[0], hitCoords[1]);
	}

	@Override
	public boolean intersectsRectangle(GRectangle rect) {
		if (!isVisible || geoImage.isInBackground()) {
			return false;
		}

		return rect.intersects(classicBoundingBox);
	}

	@Override
	public boolean isInside(GRectangle rect) {
		if (!isVisible || geoImage.isInBackground()) {
			return false;
		}
		return rect.contains(classicBoundingBox);
	}

	/**
	 * Returns the bounding box of this DrawPoint in screen coordinates.
	 */
	@Override
	public GRectangle getBounds() {
		if (!geo.isDefined() || !geo.isEuclidianVisible()) {
			return null;
		}
		return classicBoundingBox;
	}

	/**
	 * Returns false
	 */
	@Override
	public boolean hitLabel(int x, int y) {
		return false;
	}

	@Override
	public GeoElement getGeoElement() {
		return geo;
	}

	@Override
	public void setGeoElement(GeoElement geo) {
		this.geo = geo;
	}

	@Override
	public BoundingBox getBoundingBox() {
		if (boundingBox == null) {
			boundingBox = new BoundingBox(
					view.getApplication().has(Feature.MOW_CROP_IMAGE) ? true
							: false);
		}
		return boundingBox;
	}

	/**
	 * @return crop box
	 */
	public GRectangle getCropBox() {
		return cropBox.getBounds();
	}

	@Override
	public void updateByBoundingBoxResize(AbstractEvent e,
			EuclidianBoundingBoxHandler handler) {
		if (!(geo.getKernel().getApplication()
				.has(Feature.MOW_IMAGE_BOUNDING_BOX) && geo.getKernel().getApplication()
						.has(Feature.MOW_CROP_IMAGE))
				|| (absoluteLocation && !geo.getKernel().getApplication().has(Feature.MOW_PIN_IMAGE))) {
			return;
		}
		if (boundingBox.isCropBox()) {
			if (!geo.getKernel().getApplication().has(Feature.MOW_CROP_IMAGE)) {
				return;
			}
			wasCroped = true;
			if (Double.isNaN(originalRatio)) {
				double rectWidth = getBoundingBox().getRectangle().getWidth();
				double rectHeight = getBoundingBox().getRectangle().getHeight();
				originalRatio = rectHeight / rectWidth;
			}
			updateImageCrop(e, handler);
		} else {
			if (!geo.getKernel().getApplication()
					.has(Feature.MOW_IMAGE_BOUNDING_BOX)) {
				return;
			}
			if (Double.isNaN(originalRatio)) {
				double width = geoImage.getImageScreenWidth();
				double height = geoImage.getImageScreenHeight();
				originalRatio = height / width;
			}
			if (absoluteLocation && geo.getKernel().getApplication().has(Feature.MOW_PIN_IMAGE)) {
				// updates the current coordinates of corner points
				geoImage.screenToReal();
			}
			geoImage.updateScaleAndLocation();
			updateImageResize(e, handler);

			if (!geo.getKernel().getApplication().has(Feature.MOW_PIN_IMAGE)) {
				return;
			}			
			if (absoluteLocation && geo.getKernel().getApplication().has(Feature.MOW_PIN_IMAGE)) {
				geoImage.updateScaleAndLocation();
			}
		}
	}

	private void updateImageCrop(AbstractEvent event,
			EuclidianBoundingBoxHandler handler) {
		int eventX = event.getX();
		int eventY = event.getY();
		double newWidth = 1;
		double newHeight = 1;
		GRectangle2D rect = AwtFactory.getPrototype().newRectangle2D();
		switch (handler) {
		case BOTTOM:
			if (eventY - getBoundingBox().getRectangle().getY() <= Math
					.min(IMG_CROP_THRESHOLD, image.getHeight())
					|| eventY > getBounds().getMaxY()) {
				return;
			}
			rect.setRect(getBoundingBox().getRectangle().getX(),
					getBoundingBox().getRectangle().getY(),
					getBoundingBox().getRectangle().getWidth(),
					eventY - getBoundingBox().getRectangle().getY());
			originalRatio = Double.NaN;
			break;
		case TOP:
			if (getBoundingBox().getRectangle().getMaxY() - eventY <= Math
					.min(IMG_CROP_THRESHOLD, image.getHeight())
					|| eventY < getBounds().getMinY()) {
				return;
			}
			rect.setRect(getBoundingBox().getRectangle().getX(), eventY,
					getBoundingBox().getRectangle().getWidth(),
					getBoundingBox().getRectangle().getMaxY() - eventY);
			originalRatio = Double.NaN;
			break;
		case LEFT:
			if (getBoundingBox().getRectangle().getMaxX() - eventX <= Math
					.min(IMG_CROP_THRESHOLD, image.getWidth())
					|| eventX < getBounds().getMinX()) {
				return;
			}
			rect.setRect(eventX, getBoundingBox().getRectangle().getY(),
					getBoundingBox().getRectangle().getMaxX() - eventX,
					getBoundingBox().getRectangle().getHeight());
			originalRatio = Double.NaN;
			break;
		case RIGHT:
			if (eventX - getBoundingBox().getRectangle().getX() <= Math
					.min(IMG_CROP_THRESHOLD, image.getWidth())
					|| eventX > getBounds().getMaxX()) {
				return;
			}
			rect.setRect(getBoundingBox().getRectangle().getX(),
					getBoundingBox().getRectangle().getY(),
					eventX - getBoundingBox().getRectangle().getX(),
					getBoundingBox().getRectangle().getHeight());
			originalRatio = Double.NaN;
			break;
		case BOTTOM_RIGHT:
			newWidth = eventX - getBoundingBox().getRectangle().getMinX();
			newHeight = originalRatio * newWidth;
			if (newWidth <= Math.min(IMG_CROP_THRESHOLD, image.getWidth())
					|| eventX > getBounds().getMaxX()
					|| getBoundingBox().getRectangle().getY()
							+ newHeight > getBounds().getMaxY()) {
				return;
			}
			rect.setRect(getBoundingBox().getRectangle().getX(),
					getBoundingBox().getRectangle().getY(), newWidth,
					newHeight);
			break;
		case BOTTOM_LEFT:
			newWidth = getBoundingBox().getRectangle().getMaxX() - eventX;
			newHeight = originalRatio * newWidth;
			if (newWidth <= Math.min(IMG_CROP_THRESHOLD, image.getWidth())
					|| eventX < getBounds().getMinX()
					|| getBoundingBox().getRectangle().getY()
							+ newHeight > getBounds().getMaxY()) {
				return;
			}
			rect.setRect(getBoundingBox().getRectangle().getMaxX() - newWidth,
					getBoundingBox().getRectangle().getY(), newWidth,
					newHeight);
			break;
		case TOP_RIGHT:
			newWidth = eventX - getBoundingBox().getRectangle().getMinX();
			newHeight = originalRatio * newWidth;
			if (newWidth <= Math.min(IMG_CROP_THRESHOLD, image.getWidth())
					|| eventX > getBounds().getMaxX()
					|| getBoundingBox().getRectangle().getMaxY()
							- newHeight < getBounds().getMinY()) {
				return;
			}
			rect.setRect(getBoundingBox().getRectangle().getX(),
					getBoundingBox().getRectangle().getMaxY() - newHeight,
					newWidth,
					newHeight);
			if (rect.getBounds().getMaxY() > getBounds().getMaxY()) {
				return;
			}
			break;
		case TOP_LEFT:
			newWidth = getBoundingBox().getRectangle().getMaxX() - eventX;
			newHeight = originalRatio * newWidth;
			if (newWidth <= Math.min(IMG_CROP_THRESHOLD, image.getWidth())
					|| eventX < getBounds().getMinX()
					|| getBoundingBox().getRectangle().getMaxY()
							- newHeight < getBounds().getMinY()) {
				return;
			}
			rect.setRect(getBoundingBox().getRectangle().getMaxX() - newWidth,
					getBoundingBox().getRectangle().getMaxY() - newHeight,
					newWidth, newHeight);
			if (rect.getBounds().getMaxY() > getBounds().getMaxY()) {
				return;
			}
			break;
		default:
			break;
		}
		boundingBox.setRectangle(rect);
		// remember last crop box position
		cropBox = rect;
		imagecropRatioX = view.getXscale() * geoImage.getImageScreenWidth() / cropBox.getWidth();
		imagecropRatioY = view.getYscale() * geoImage.getImageScreenHeight() / cropBox.getHeight();
	}

	private void updateImageResize(AbstractEvent event,
			EuclidianBoundingBoxHandler handler) {
		int eventX = event.getX();
		int eventY = event.getY();
		GeoPoint A, B, D;
		double cropMinX, cropMaxX, cropMinY, cropMaxY;
		if (this.wasCroped) {
			cropMinX = view.toRealWorldCoordX(getCropBox().getMinX());
			cropMaxX = view.toRealWorldCoordX(getCropBox().getMaxX());
			cropMinY = view.toRealWorldCoordY(getCropBox().getMaxY());
			cropMaxY = view.toRealWorldCoordY(getCropBox().getMinY());

			A = new GeoPoint(geo.getConstruction(), cropMinX, cropMinY, 1.0);
			A.remove();
			B = new GeoPoint(geo.getConstruction(), cropMaxX, cropMinY, 1.0);
			B.remove();
			D = new GeoPoint(geo.getConstruction(), cropMinX, cropMaxY, 1.0);
			D.remove();
		} else {
			A = geoImage.getCorner(0);
			B = geoImage.getCorner(1);
			D = geoImage.getCorner(2);
		}

		double newWidth = 1;
		double newHeight = 1;
		double rwEventX = view.toRealWorldCoordX(eventX);
		if (A == null) {
			A = new GeoPoint(geoImage.cons);
			geoImage.calculateCornerPoint(A, 1);
		}
		if (B == null) {
			B = new GeoPoint(geoImage.cons);
			geoImage.calculateCornerPoint(B, 2);
		}
		if (D == null) {
			D = new GeoPoint(geoImage.cons);
			geoImage.calculateCornerPoint(D, 3);
		}

		debug("points in updateImageResize: ");
		debugPoints(A, B, D);

		switch (handler) {
		case TOP_RIGHT:
			if (eventX - view.toScreenCoordXd(A.getInhomX()) <= Math
					.min(IMG_WIDTH_THRESHOLD, image.getWidth())) {
				return;
			}
			B.setX(rwEventX);
			newWidth = rwEventX - D.getInhomX();
			newHeight = -originalRatio * newWidth;
			B.updateCoords();
			B.updateRepaint();
			D.setX(A.getInhomX());
			D.setY(A.getInhomY() - newHeight);
			D.updateCoords();
			D.updateRepaint();
			setCorner(D, 2);
			break;
		case TOP_LEFT:
			if (view.toScreenCoordXd(B.getInhomX()) - eventX <= Math
					.min(IMG_WIDTH_THRESHOLD, image.getWidth())) {
				return;
			}
			A.setX(rwEventX);
			A.updateCoords();
			A.updateRepaint();
			D.setX(rwEventX);
			newWidth = B.getInhomX() - rwEventX;
			newHeight = -originalRatio * newWidth;
			D.setY(B.getInhomY() - newHeight);
			D.updateCoords();
			D.updateRepaint();
			setCorner(D, 2);
			break;

		case BOTTOM_RIGHT:
			if (eventX - view.toScreenCoordXd(A.getInhomX()) <= Math
					.min(IMG_WIDTH_THRESHOLD, image.getWidth())) {
				return;
			}
			D.setX(A.getInhomX());
			D.updateCoords();
			D.updateRepaint();
			setCorner(D, 2);
			B.setX(rwEventX);
			newWidth = rwEventX - D.getInhomX();
			newHeight = -originalRatio * newWidth;
			B.setY(D.getInhomY() + newHeight);
			B.updateCoords();
			B.updateRepaint();
			A.setY(B.getInhomY());
			A.updateCoords();
			A.updateRepaint();
			break;
		case BOTTOM_LEFT:
			if (view.toScreenCoordXd(B.getInhomX()) - eventX <= Math
					.min(IMG_WIDTH_THRESHOLD, image.getWidth())) {
				return;
			}
			A.setX(rwEventX);
			newWidth = B.getInhomX() - rwEventX;
			newHeight = -originalRatio * newWidth;
			A.setY(D.getInhomY() + newHeight);
			A.updateCoords();
			A.updateRepaint();
			B.setY(A.getInhomY());
			B.updateCoords();
			B.updateRepaint();
			D.setX(A.getInhomX());
			D.updateCoords();
			D.updateRepaint();
			setCorner(D, 2);
			break;
		case RIGHT:
			if (eventX - view.toScreenCoordXd(A.getInhomX()) <= Math
					.min(IMG_WIDTH_THRESHOLD, image.getWidth())) {
				return;
			}
			B.setX(rwEventX);
			B.updateCoords();
			B.updateRepaint();
			D.setX(A.getInhomX());
			D.updateCoords();
			D.updateRepaint();
			setCorner(D, 2);
			originalRatio = Double.NaN;
			break;
		case LEFT:
			if (view.toScreenCoordXd(B.getInhomX()) - eventX <= Math
					.min(IMG_WIDTH_THRESHOLD, image.getWidth())) {
				return;
			}
			A.setX(rwEventX);
			A.updateCoords();
			A.updateRepaint();
			D.setX(A.getInhomX());
			D.updateCoords();
			D.updateRepaint();
			setCorner(D, 2);
			originalRatio = Double.NaN;
			break;
		case TOP:
			if (view.toScreenCoordYd(A.getInhomY()) - eventY <= Math
					.min(IMG_WIDTH_THRESHOLD, image.getWidth())) {
				return;
			}
			D.setY(view.toRealWorldCoordY(eventY));
			D.setX(A.getInhomX());
			D.updateCoords();
			D.updateRepaint();
			setCorner(D, 2);
			originalRatio = Double.NaN;
			break;
		case BOTTOM:
			if (eventY - view.toScreenCoordYd(D.getInhomY()) <= Math
					.min(IMG_WIDTH_THRESHOLD, image.getWidth())) {
				return;
			}
			D.setX(A.getInhomX());
			D.updateCoords();
			D.updateRepaint();
			setCorner(D, 2);
			A.setY(view.toRealWorldCoordY(eventY));
			A.updateCoords();
			A.updateRepaint();
			B.setY(view.toRealWorldCoordY(eventY));
			B.updateCoords();
			B.updateRepaint();
			originalRatio = Double.NaN;
			break;
		default:
			break;
		}

		if (wasCroped) {
			// the new screen positions of crop box and the new width/height
			double screenAX = view.toScreenCoordXd(A.getInhomX());
			double screenAY = view.toScreenCoordYd(A.getInhomY());
			double screenBX = view.toScreenCoordXd(B.getInhomX());
			double screenDY = view.toScreenCoordYd(D.getInhomY());
			double screenCropWidth = screenBX - screenAX;
			double screenCropHeight = screenAY - screenDY;

			// change x coordinates of image corners
			switch (handler) {
			case TOP_RIGHT:
			case RIGHT:
			case BOTTOM_RIGHT:
			case TOP_LEFT:
			case LEFT:
			case BOTTOM_LEFT:
				double curScaleX = screenCropWidth / cropBox.getWidth();
				double imageScreenAx = view.toScreenCoordXd(geoImage.getCorner(0).getX());
				double newImageWidth = screenCropWidth * this.imagecropRatioX;
				double oldDistLeftSide = getCropBox().getMinX() - imageScreenAx;
				double newDistLeftSide = oldDistLeftSide * curScaleX;
				double newLeftSideImgScr = screenAX - newDistLeftSide;
				double newLeftSideImg = view.toRealWorldCoordX(newLeftSideImgScr);
				double newRightSideImg = view.toRealWorldCoordX(newLeftSideImgScr + newImageWidth);
				geoImage.getCorner(1).setX(newRightSideImg);
				geoImage.getCorner(0).setX(newLeftSideImg);
				if (geoImage.getCorner(2) != null) {
					geoImage.getCorner(2).setX(newLeftSideImg);
				}
				break;
			default:
				// do nothing
				break;
			}

			// change y coordinates of image corners
			switch (handler) {
			case TOP_LEFT:
			case TOP:
			case TOP_RIGHT:
			case BOTTOM_LEFT:
			case BOTTOM:
			case BOTTOM_RIGHT:
				double curScaleY = screenCropHeight / cropBox.getHeight();
				double imageScreenAy = view.toScreenCoordYd(geoImage.getCorner(0).getY());
				double newImageHeight = screenCropHeight * this.imagecropRatioY;
				double oldDistBottomSide = imageScreenAy - getCropBox().getMaxY();
				double newDistBottomSide = oldDistBottomSide * curScaleY;
				double newBottomSideImgScr = screenAY + newDistBottomSide;
				double newBottomSideImg = view.toRealWorldCoordY(newBottomSideImgScr);
				geoImage.getCorner(0).setY(newBottomSideImg);
				geoImage.getCorner(1).setY(newBottomSideImg);
				if (geoImage.getCorner(2) != null) {
					double newTopSideImg = view.toRealWorldCoordY(newBottomSideImgScr - newImageHeight);
					geoImage.getCorner(2).setY(newTopSideImg);
				}
				break;
			default:
				// do nothing
				break;
			}

			// update geoImage cornerpoints
			geoImage.getCorner(0).updateCoords();
			geoImage.getCorner(0).updateRepaint();
			geoImage.getCorner(1).updateCoords();
			geoImage.getCorner(1).updateRepaint();
			if (geoImage.getCorner(2) != null) {
				geoImage.getCorner(2).updateCoords();
				geoImage.getCorner(2).updateRepaint();
			}

			// update crop box
			cropBox.setRect(screenAX, screenDY, screenCropWidth, screenCropHeight);
		}

	}

	private void setCorner(GeoPoint point, int corner) {
		if (!wasCroped) {
			geoImage.setCorner(point, corner);
		}
	}
}
