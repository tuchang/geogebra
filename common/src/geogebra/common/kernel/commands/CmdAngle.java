package geogebra.common.kernel.commands;

import geogebra.common.kernel.Kernel;
import geogebra.common.kernel.algos.AlgoAngleConic;
import geogebra.common.kernel.algos.AlgoAngleNumeric;
import geogebra.common.kernel.algos.AlgoAngleVector;
import geogebra.common.kernel.arithmetic.Command;
import geogebra.common.kernel.geos.GeoConic;
import geogebra.common.kernel.geos.GeoElement;
import geogebra.common.kernel.geos.GeoLine;
import geogebra.common.kernel.geos.GeoNumberValue;
import geogebra.common.kernel.geos.GeoNumeric;
import geogebra.common.kernel.geos.GeoPoint;
import geogebra.common.kernel.geos.GeoPolygon;
import geogebra.common.kernel.geos.GeoVec3D;
import geogebra.common.kernel.geos.GeoVector;
import geogebra.common.kernel.kernelND.GeoConicND;
import geogebra.common.kernel.kernelND.GeoLineND;
import geogebra.common.kernel.kernelND.GeoPointND;
import geogebra.common.kernel.kernelND.GeoVectorND;
import geogebra.common.main.MyError;


/**
 * Angle[ number ] Angle[ <GeoPolygon> ] Angle[ <GeoConic> ] Angle[ <GeoVector>
 * ] Angle[ <GeoPoint> ] Angle[ <GeoVector>, <GeoVector> ] Angle[ <GeoLine>,
 * <GeoLine> ] Angle[ <GeoPoint>, <GeoPoint>, <GeoPoint> ] Angle[ <GeoPoint>,
 * <GeoPoint>, <Number> ]
 */
public class CmdAngle extends CommandProcessor {

	/**
	 * Create new command processor
	 * 
	 * @param kernel
	 *            kernel
	 */
	public CmdAngle(Kernel kernel) {
		super(kernel);
	}

	@Override
	public GeoElement[] process(Command c) throws MyError {
		int n = c.getArgumentNumber();
		boolean[] ok = new boolean[n];
		
		return process(c,n,ok);
	}

	/**
	 * 
	 * @param c command
	 * @param n arguments length
	 * @param ok ok check
	 * @return result
	 * @throws MyError argument / length error
	 */
	protected GeoElement[] process(Command c, int n, boolean ok[]) throws MyError {

		GeoElement[] arg;

		switch (n) {
		/**
		 * // Anlge[ constant number ] // get number value ExpressionNode en =
		 * null; ExpressionValue eval; double value = 0.0; // check if we got
		 * number: // ExpressionNode && NumberValue || Assignment // build
		 * ExpressionNode from one of these cases ok[0] = false; Object ob =
		 * c.getArgument(0); if (ob instanceof ExpressionNode) { en =
		 * (ExpressionNode) ob; eval = en.evaluate(); if (eval .isNumberValue()
		 * && !(eval .isGeoPolygon())) { value = ((NumberValue)
		 * eval).getDouble(); ok[0] = true; } } else if (ob instanceof
		 * Assignment) { GeoElement geo = cons.lookupLabel(((Assignment)
		 * ob).getVariable()); if (geo .isGeoNumeric()) { // wrap GeoNumeric int
		 * ExpressionNode for // kernel.DependentNumer() en = new
		 * ExpressionNode(kernel, (NumberValue) geo,
		 * ExpressionNode.NO_OPERATION, null); ok[0] = true; } }
		 */
		case 1:
			arg = resArgs(c);

			// wrap angle as angle (needed to avoid ambiguities between numbers
			// and angles in XML)
			if (arg[0].isGeoAngle()) {
				// maybe we have to set a label here
				if (!cons.isSuppressLabelsActive() && !arg[0].isLabelSet()) {
					arg[0].setLabel(c.getLabel());

					// make sure that arg[0] is in construction list
					if (arg[0].isIndependent()) {
						cons.addToConstructionList(arg[0], true);
					} else {
						cons.addToConstructionList(arg[0].getParentAlgorithm(),
								true);
					}
				}
				GeoElement[] ret = { arg[0] };
				return ret;
			}
			// angle from number
			else if (arg[0].isGeoNumeric()) {
				
				AlgoAngleNumeric algo = new AlgoAngleNumeric(cons, c.getLabel(),
						(GeoNumeric) arg[0]);

				GeoElement[] ret = { algo.getAngle() };
				return ret;
			}
			// angle from number
			else if (arg[0].isGeoPoint() || arg[0].isGeoVector()) {
				
				return anglePointOrVector(c.getLabel(), arg[0]);
			}
			// angle of conic or polygon
			else {
				if (arg[0].isGeoConic()) {
					return angle(c.getLabel(), (GeoConicND) arg[0]);
				} else if (arg[0].isGeoPolygon()) {
					return angle(c.getLabels(), (GeoPolygon) arg[0]);
				}
			}

			throw argErr(app, c.getName(), arg[0]);

		case 2:
			arg = resArgs(c);
			
			GeoElement[] ret = process2(c, arg, ok);
			
			if (ret != null){
				return ret;
			}

			// syntax error
			if (ok[0] && !ok[1]) {
				throw argErr(app, c.getName(), arg[1]);
			}
			throw argErr(app, c.getName(), arg[0]);


		case 3:
			arg = resArgs(c);

			// angle between three points
			if ((ok[0] = (arg[0].isGeoPoint()))
					&& (ok[1] = (arg[1].isGeoPoint()))
					&& (ok[2] = (arg[2].isGeoPoint()))) {
				return angle(c.getLabel(),
						(GeoPointND) arg[0], (GeoPointND) arg[1],
						(GeoPointND) arg[2]);
			}
			// fixed angle
			else if ((ok[0] = (arg[0].isGeoPoint()))
					&& (ok[1] = (arg[1].isGeoPoint()))
					&& (ok[2] = (arg[2] instanceof GeoNumberValue))) {
				return getAlgoDispatcher().Angle(c.getLabels(), (GeoPoint) arg[0],
						(GeoPoint) arg[1], (GeoNumberValue) arg[2], true);
			} else {
				throw argErr(app, c.getName(), arg[0]);
			}

		default:
			throw argNumErr(app, c.getName(), n);
		}
	}
	
	/**
	 * process angle when 2 arguments
	 * @param c command
	 * @param arg arguments
	 * @param ok ok array
	 * @return result (if one)
	 */
	protected GeoElement[] process2(Command c, GeoElement[] arg, boolean[] ok){
		
		// angle between vectors
		if ((ok[0] = (arg[0].isGeoVector()))
				&& (ok[1] = (arg[1].isGeoVector()))) {
			return angle(c.getLabel(), (GeoVectorND) arg[0], (GeoVectorND) arg[1]);
		}
		
		// angle between lines
		if ((ok[0] = (arg[0].isGeoLine()))
				&& (ok[1] = (arg[1].isGeoLine()))) {
			return angle(c.getLabel(), (GeoLineND) arg[0], (GeoLineND) arg[1]);
		}

		return null;
	}
	
	
	/**
	 * @param label label
	 * @param p1 first point
	 * @param p2 second point
	 * @param p3 third point
	 * @return angle between 3 points
	 */
	protected GeoElement[] angle(String label, GeoPointND p1, GeoPointND p2, GeoPointND p3){
		GeoElement[] ret = { getAlgoDispatcher().Angle(label, (GeoPoint) p1, (GeoPoint) p2, (GeoPoint) p3) };
		return ret;
	}
	
	
	/**
	 * @param label label
	 * @param g first line
	 * @param h second line
	 * @return angle between lines
	 */
	protected GeoElement[] angle(String label, GeoLineND g, GeoLineND h){
		GeoElement[] ret = { getAlgoDispatcher().Angle(label, (GeoLine) g, (GeoLine) h) };
		return ret;
	}
	
	
	/**
	 * @param label label
	 * @param v first vector
	 * @param w second vector
	 * @return angle between vectors
	 */
	protected GeoElement[] angle(String label, GeoVectorND v, GeoVectorND w){
		GeoElement[] ret = { getAlgoDispatcher().Angle(label, (GeoVector) v, (GeoVector) w) };
		return ret;
	}
	
	/**
	 * @param label label
	 * @param v vector or point
	 * @return angle between Ox and vector/point
	 */
	protected GeoElement[] anglePointOrVector(String label, GeoElement v){
		AlgoAngleVector algo = new AlgoAngleVector(cons, label, (GeoVec3D) v);
		GeoElement[] ret = { algo.getAngle() };
		return ret;
	}
	
	
	/**
	 * @param label label
	 * @param c conic
	 * @return angle between Ox and conic first eigen vector
	 */
	protected GeoElement[] angle(String label, GeoConicND c){
		AlgoAngleConic algo = new AlgoAngleConic(cons, label, (GeoConic) c);
		GeoElement[] ret = { algo.getAngle() };
		return ret;
	}
	
	/**
	 * @param labels label
	 * @param p polygon
	 * @return angles of the polygon
	 */
	protected GeoElement[] angle(String[] labels, GeoPolygon p){
		return getAlgoDispatcher().Angles(labels, p);
	}
	
	
}