package org.geogebra.common.kernel.geos;

import org.geogebra.common.euclidian.EuclidianViewInterfaceCommon;
import org.geogebra.common.kernel.CircularDefinitionException;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.Locateable;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.arithmetic.ValueType;
import org.geogebra.common.kernel.kernelND.GeoElementND;
import org.geogebra.common.kernel.kernelND.GeoPointND;
import org.geogebra.common.plugin.GeoClass;

/**
 * Geo for embedded apps
 */
public class GeoEmbed extends GeoElement implements GeoFrame, Locateable, Furniture {

	private boolean defined = true;
	private GeoPoint[] corner;
	private int embedID;
	private double contentWidth = 800;
	private double contentHeight = 600;
	private boolean background = true;
	private String appName = "graphing";

	/**
	 * @param c
	 *            construction
	 */
	public GeoEmbed(Construction c) {
		super(c);
		corner = new GeoPoint[3];
	}

	/**
	 * Center this in a view
	 * 
	 * @param ev
	 *            view
	 */
	public void initPosition(EuclidianViewInterfaceCommon ev) {
		double x = ev.toRealWorldCoordX(ev.getViewWidth() / 2.0)
				- contentWidth / ev.getXscale() / 2;
		double y = ev.toRealWorldCoordY(ev.getViewHeight() / 2.0)
				- contentHeight / ev.getYscale() / 2;
		corner[0] = new GeoPoint(cons);
		corner[0].setCoords(x, y, 1);
		corner[1] = new GeoPoint(cons);
		corner[1].setCoords(x + contentWidth / ev.getXscale(), y, 1);
		corner[2] = new GeoPoint(cons);
		corner[2].setCoords(x, y + contentHeight / ev.getXscale(), 1);
	}

	@Override
	public GeoClass getGeoClassType() {
		return GeoClass.EMBED;
	}

	@Override
	public ValueType getValueType() {
		return ValueType.UNKNOWN;
	}

	@Override
	public GeoElement copy() {
		GeoEmbed ret = new GeoEmbed(cons);
		ret.set(this);
		return ret;
	}

	@Override
	public void set(GeoElementND geo) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean isDefined() {
		return defined;
	}

	@Override
	public void setUndefined() {
		defined = false;
	}

	@Override
	public String toValueString(StringTemplate tpl) {
		return "";
	}

	@Override
	public boolean showInAlgebraView() {
		return false;
	}

	@Override
	protected boolean showInEuclidianView() {
		return true;
	}

	@Override
	public boolean isEqual(GeoElementND geo) {
		return false;
	}

	@Override
	public HitType getLastHitType() {
		return HitType.ON_BOUNDARY;
	}

	/**
	 * Get corner, same as GeoImage
	 * 
	 * @param i
	 *            index
	 * @return corner
	 */
	public GeoPoint getCorner(int i) {
		if (corner[i] == null) {
			GeoPoint ret = new GeoPoint(cons);
			ret.setCoords(0, 0, 1);
			return ret;
		}
		return corner[i];
	}

	@Override
	public void getXMLtags(StringBuilder sb) {
		super.getXMLtags(sb);
		sb.append("\t<embed id=\"");
		sb.append(embedID);
		sb.append("\" app=\"");
		sb.append(appName);
		sb.append("\"/>\n");
		XMLBuilder.dimension(sb, Double.toString(contentWidth), Double.toString(contentHeight));
		for (int i = 0; i < corner.length; i++) {
			XMLBuilder.getCornerPointXML(sb, i, corner);
		}
	}

	/**
	 * @return appName param of the applet
	 */
	public String getAppName() {
		return appName;
	}

	/**
	 * @return embed ID: needs to be unique in construction
	 */
	public int getEmbedID() {
		return embedID;
	}

	/**
	 * @param embedID
	 *            embed ID: needs to be unique in construction
	 */
	public void setEmbedId(int embedID) {
		this.embedID = embedID;
	}

	@Override
	public void setStartPoint(GeoPointND p) throws CircularDefinitionException {
		corner[0] = (GeoPoint) p;
	}

	@Override
	public void removeStartPoint(GeoPointND p) {
		for (int i = 0; i < corner.length; i++) {
			if (corner[i] == p) {
				corner[i] = (GeoPoint) p.copy();
			}
		}
	}

	@Override
	public GeoPointND getStartPoint() {
		return corner[0];
	}

	@Override
	public void setStartPoint(GeoPointND p, int number) throws CircularDefinitionException {
		corner[number] = (GeoPoint) p;
	}

	@Override
	public GeoPointND[] getStartPoints() {
		return corner;
	}

	@Override
	public void initStartPoint(GeoPointND p, int number) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean hasAbsoluteLocation() {
		return false;
	}

	@Override
	public boolean isAlwaysFixed() {
		return false;
	}

	@Override
	public void setWaitForStartPoint() {
		// TODO Auto-generated method stub
	}

	@Override
	public void updateLocation() {
		// TODO Auto-generated method stub
	}

	/**
	 * @return whether the applet is currently inactive (can be moved)
	 */
	public boolean isBackground() {
		return background;
	}

	/**
	 * @param background
	 *            whether the applet is currently inactive (can be moved)
	 */
	public void setBackground(boolean background) {
		this.background = background;
	}

	@Override
	public void setReady() {
		background = false;
	}

	@Override
	public boolean isReady() {
		return !background;
	}

	@Override
	public int getAbsoluteScreenLocX() {
		EuclidianViewInterfaceCommon view = kernel.getApplication().getActiveEuclidianView();
		return view.toScreenCoordX(getCorner(0).getInhomX());
	}

	@Override
	public int getAbsoluteScreenLocY() {
		EuclidianViewInterfaceCommon view = kernel.getApplication().getActiveEuclidianView();
		return view.toScreenCoordY(getCorner(2).getInhomY());
	}

	@Override
	public void setAbsoluteScreenLoc(int x, int y) {
		EuclidianViewInterfaceCommon view = kernel.getApplication().getActiveEuclidianView();
		double oldWidth = getCorner(1).getInhomX() - getCorner(0).getInhomX();
		double oldHeight = getCorner(2).getInhomY() - getCorner(0).getInhomY();
		getCorner(2).setCoords(view.toRealWorldCoordX(x), view.toRealWorldCoordY(y), 1);
		getCorner(0).setCoords(view.toRealWorldCoordX(x), view.toRealWorldCoordY(y) - oldHeight, 1);
		getCorner(1).setCoords(view.toRealWorldCoordX(x) + oldWidth,
				view.toRealWorldCoordY(y) - oldHeight, 1);
	}

	@Override
	public boolean isFurniture() {
		return true;
	}

	/**
	 * @return width parameter of the embedded applet
	 */
	public double getContentWidth() {
		return contentWidth;
	}

	/**
	 * @return height parameter of the embedded applet
	 */
	public double getContentHeight() {
		return contentHeight;
	}

	/**
	 * @param newWidth
	 *            width parameter of the embedded applet
	 */
	public void setContentWidth(double newWidth) {
		this.contentWidth = newWidth;
	}

	/**
	 * @param newHeight
	 *            height parameter of the embedded applet
	 */
	public void setContentHeight(double newHeight) {
		this.contentHeight = newHeight;
	}

	/**
	 * @param appName
	 *            app name of the embedded applet
	 */
	public void setAppName(String appName) {
		this.appName = appName;
	}

}
