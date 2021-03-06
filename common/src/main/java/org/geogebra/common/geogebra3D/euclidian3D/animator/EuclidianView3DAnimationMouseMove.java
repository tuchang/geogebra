package org.geogebra.common.geogebra3D.euclidian3D.animator;

import org.geogebra.common.euclidian.EuclidianController;
import org.geogebra.common.geogebra3D.euclidian3D.EuclidianView3D;
import org.geogebra.common.geogebra3D.euclidian3D.animator.EuclidianView3DAnimator.AnimationType;
import org.geogebra.common.kernel.Matrix.CoordMatrix4x4;
import org.geogebra.common.kernel.Matrix.Coords;
import org.geogebra.common.kernel.kernelND.GeoPointND;

/**
 * animation for mouse move
 *
 */
public class EuclidianView3DAnimationMouseMove extends EuclidianView3DAnimation {

	private int mouseMoveDX;
	private int mouseMoveDY;
	private int mouseMoveMode;
	private double aOld;
	private double bOld;
	private double xZeroOld;
	private double yZeroOld;
	private double zZeroOld;
	private Coords tmpCoords1 = new Coords(4);

	/**
	 * 
	 * @param view3D 3D view
	 * @param animator animator
	 */
	EuclidianView3DAnimationMouseMove(EuclidianView3D view3D, EuclidianView3DAnimator animator) {
		super(view3D, animator);
	}

	/**
	 * remembers original values
	 */
	public void rememberOrigins() {
		aOld = view3D.getAngleA();
		bOld = view3D.getAngleB();
		xZeroOld = view3D.getXZero();
		yZeroOld = view3D.getYZero();
		zZeroOld = view3D.getZZero();
	}

	/**
	 * 
	 * 
	 * @param dx
	 *            mouse delta x
	 * @param dy
	 *            mouse delta y
	 * @param mode
	 *            mouse mode
	 */
	public void set(int dx, int dy, int mode) {
		mouseMoveDX = dx;
		mouseMoveDY = dy;
		mouseMoveMode = mode;
	}

	@Override
	public void setupForStart() {
		// nothing to do
	}

	@Override
	public AnimationType getType() {
		return AnimationType.MOUSE_MOVE;
	}

	@Override
	public void animate() {
		switch (mouseMoveMode) {
			case EuclidianController.MOVE_ROTATE_VIEW:
				view3D.setRotXYinDegrees(aOld - mouseMoveDX, bOld + mouseMoveDY);
				view3D.updateMatrix();
				view3D.setViewChangedByRotate();
				break;
			case EuclidianController.MOVE_VIEW:
				Coords v = new Coords(mouseMoveDX, -mouseMoveDY, 0, 0);
				view3D.toSceneCoords3D(v);

				if (view3D.getCursorOnXOYPlane().getRealMoveMode() == GeoPointND.MOVE_MODE_XY) {
				v.projectPlaneThruVIfPossible(CoordMatrix4x4.IDENTITY, view3D.getViewDirection(),
						tmpCoords1);
					view3D.setXZero(xZeroOld + tmpCoords1.getX());
					view3D.setYZero(yZeroOld + tmpCoords1.getY());
				} else {
					v.projectPlaneInPlaneCoords(CoordMatrix4x4.IDENTITY, tmpCoords1);
					view3D.setZZero(zZeroOld + tmpCoords1.getZ());
				}
			view3D.getSettings().updateOriginFromView(view3D.getXZero(), view3D.getYZero(),
					view3D.getZZero());
				view3D.updateMatrix();
				view3D.setViewChangedByTranslate();
				break;
			default:
				// do nothing
				break;
		}
		end();
	}

}
