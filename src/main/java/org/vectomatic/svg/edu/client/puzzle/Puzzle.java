/**********************************************
 * Copyright (C) 2010 Lukas Laag
 * This file is part of lib-gwt-svg-edu.
 * 
 * libgwtsvg-edu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * libgwtsvg-edu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with libgwtsvg-edu.  If not, see http://www.gnu.org/licenses/
 **********************************************/
package org.vectomatic.svg.edu.client.puzzle;

import java.util.ArrayList;
import java.util.List;

import org.vectomatic.dom.svg.OMNode;
import org.vectomatic.dom.svg.OMSVGClipPathElement;
import org.vectomatic.dom.svg.OMSVGDefsElement;
import org.vectomatic.dom.svg.OMSVGGElement;
import org.vectomatic.dom.svg.OMSVGMatrix;
import org.vectomatic.dom.svg.OMSVGPathElement;
import org.vectomatic.dom.svg.OMSVGPathSegList;
import org.vectomatic.dom.svg.OMSVGPoint;
import org.vectomatic.dom.svg.OMSVGRect;
import org.vectomatic.dom.svg.OMSVGRectElement;
import org.vectomatic.dom.svg.OMSVGSVGElement;
import org.vectomatic.dom.svg.OMSVGTransform;
import org.vectomatic.dom.svg.OMSVGUseElement;
import org.vectomatic.dom.svg.utils.SVGConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Window;

/**
 * Main class of the puzzle game
 * @author laaglu
 */
public class Puzzle implements MouseDownHandler, MouseMoveHandler, MouseUpHandler {
	/**
	 * Class to represent a drag and drop source/target
	 */
	private class Target {
		/**
		 * The piece contained by this target (if any)
		 */
		private Piece piece;
		/**
		 * The shadow to display when the drag source enters this
		 * target (only for the assembly zone)
		 */
		private OMSVGUseElement shadow;
		/**
		 * The target matrix to which this target belongs
		 */
		private TargetMatrix matrix;
		/**
		 * Coordinates of the target in the target matrix
		 */
		private int u, v;
		public Target(TargetMatrix targetMatrix, int u, int v) {
			this.matrix = targetMatrix;
			this.u = u;
			this.v = v;
		}
		public Piece getPiece() {
			return piece;
		}
		void setPiece(Piece piece) {
			this.piece = piece;
			if (piece != null) {
				OMSVGUseElement geometry = piece.geometry;
				if (geometry != null) {
					setPosition(getPosition());
				}
			}
		}
		void setShadow(OMSVGUseElement shadow) {
			this.shadow = shadow;
			if (shadow != null) {
				setSelected(false, shadow);
			}
		}
		void setSelected(boolean selected, OMSVGUseElement shadow) {
			OMSVGPoint p = getPosition();
			shadow.getX().getBaseVal().setValue(p.getX());
			shadow.getY().getBaseVal().setValue(p.getY());
			shadow.setClassNameBaseVal(selected ? matrix.selectedShadowClass : matrix.shadowClass);
		}
		public OMSVGPoint getPosition() {
			return rootSvg.createSVGPoint(matrix.x + u * matrix.w, matrix.y + v * matrix.h);
		}
		public void setPosition(OMSVGPoint p) {
			OMSVGUseElement geometry = piece.geometry;
			if (geometry != null) {
				geometry.getX().getBaseVal().setValue(p.getX());
				geometry.getY().getBaseVal().setValue(p.getY());
			}
		}
		public void doLayout() {
			OMSVGPoint p = getPosition();
			if (piece != null) {
				setPosition(p);
			}
			if (shadow != null) {
				shadow.getX().getBaseVal().setValue(p.getX());
				shadow.getY().getBaseVal().setValue(p.getY());
			}
		}
		
		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder();
			buffer.append(matrix.id);
			buffer.append(".T<");
			buffer.append(u);
			buffer.append(", ");
			buffer.append(v);
			buffer.append(">[");
			buffer.append(piece != null ? piece.toString() : "null");
			buffer.append(",");
			buffer.append(shadow != null ? (shadow.getClassName().getBaseVal().equals(matrix.selectedShadowClass)) : "?");
			buffer.append("]");
			return buffer.toString();
		}
	}
	
	/**
	 * Class to group together drag and drop targets
	 */
	class TargetMatrix {
		/**
		 * An id (to print in the debugger)
		 */
		private String id;
		/**
		 * The matrix of targets
		 */
		private Target[][] targets;
		/**
		 * A rectangle used to compute the layout of the targets
		 */
		private float x, y, w, h;
		/**
		 * The CSS class of a target 
		 * (when not selected in the drag and drop operation)
		 */
		private String shadowClass;
		/**
		 * The CSS class of a target 
		 * (when selected in the drag and drop operation)
		 */
		private String selectedShadowClass;
		
		TargetMatrix(String id, String shadowClass, String selectedShadowClass) {
			targets = new Target[colCount][];
			this.id = id;
			this.shadowClass = shadowClass;
			this.selectedShadowClass = selectedShadowClass;
			for (int i = 0; i < colCount; i++) {
				targets[i] = new Target[rowCount];
				for (int j = 0; j < rowCount; j++) {
					targets[i][j] = new Target(this, i, j);
				}
			}
		}
		public void setPiece(Piece piece, int col, int row) {
			targets[col][row].setPiece(piece); 
		}
		public Piece getPiece(int col, int row) {
			return getTarget(col, row).getPiece();
		}
		public Target getTarget(int col, int row) {
			return targets[col][row];
		}
		public Target getTarget(MouseEvent<? extends EventHandler> e) {
			OMSVGPoint p = getCoordinates(e).substract(rootSvg.createSVGPoint(x, y)).product(rootSvg.createSVGPoint(1f / w, 1f / h)).floor();
			return p.getX() >= 0 && p.getX() < colCount && p.getY() >= 0 && p.getY() < rowCount ? targets[(int)p.getX()][(int)p.getY()] : null;
		}
		public void doLayout(float x, float y, float w, float h) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
			for (int i = 0; i < colCount; i++) {
				for (int j = 0; j < rowCount; j++) {
					targets[i][j].doLayout();
				}
			}
		}
	}
	
	/**
	 * Class to represent a puzzle piece
	 */
	static class Piece {
		/**
		 * Piece coordinates in the assembled puzzle
		 */
		int x, y;
		/**
		 * Piece connector to other pieces
		 */
		Connector north, south, east, west;
		/**
		 * The piece geometry
		 */
		OMSVGUseElement geometry;
		/**
		 * The piece shadow used during drag and drop operations
		 */
		OMSVGUseElement shadow;
		Piece(int x, int y) {
			this.x = x;
			this.y = y;
		}
		public String getId() {
			return x + "-" + y;
		}
		@Override
		public String toString() {
			return getId();
		}
	}
	
	/**
	 * Class to represent a directed connector
	 * between two pieces
	 */
	static class Connector {
		/**
		 * The source piece
		 */
		Piece src;
		/**
		 * The destination piece
		 */
		Piece dest;
		Connector(Piece src, Piece dest) {
			if (Random.nextInt() % 2 == 0) {
				this.src = src;
				this.dest = dest;
			} else {
				this.src = dest;
				this.dest = src;
			}
		}
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(src);
			builder.append(">>");
			builder.append(dest);
			return builder.toString();
		}
	}

	static enum ConnectorShape {
		SQUARE {
			@Override
			void makeVConnector(
					float connectorWidth,
					float connectorHeight,
					float pieceWidth,
					int direction,
					int connectorDirection,
					OMSVGPathElement piecePath,
					OMSVGPathSegList segs) {	
				segs.appendItem(piecePath.createSVGPathSegLinetoHorizontalAbs(0.5f * (pieceWidth - direction * connectorWidth)));
				segs.appendItem(piecePath.createSVGPathSegLinetoVerticalRel(connectorDirection * direction * connectorHeight));	
				segs.appendItem(piecePath.createSVGPathSegLinetoHorizontalRel(direction * connectorWidth));	
				segs.appendItem(piecePath.createSVGPathSegLinetoVerticalRel(-connectorDirection * direction * connectorHeight));
			}
			@Override
			void makeHConnector(
					float connectorWidth,
					float connectorHeight,
					float pieceHeight,
					int direction,
					int connectorDirection,
					OMSVGPathElement piecePath,
					OMSVGPathSegList segs) {
				segs.appendItem(piecePath.createSVGPathSegLinetoVerticalAbs(0.5f * (pieceHeight - direction * connectorHeight)));
				segs.appendItem(piecePath.createSVGPathSegLinetoHorizontalRel(connectorDirection * direction * connectorWidth));	
				segs.appendItem(piecePath.createSVGPathSegLinetoVerticalRel(direction * connectorHeight));	
				segs.appendItem(piecePath.createSVGPathSegLinetoHorizontalRel(-connectorDirection * direction * connectorWidth));
			}
		},
		SPLINE {
			@Override
			void makeVConnector(
					float connectorWidth,
					float connectorHeight,
					float pieceWidth,
					int direction,
					int connectorDirection,
					OMSVGPathElement piecePath,
					OMSVGPathSegList segs) {
				float x1 = 0.5f * (pieceWidth - 0.5f * direction * connectorWidth);
				float kx = 0.5f * K * direction * connectorWidth;
				float ky = 0.5f * K * connectorDirection * direction * connectorHeight;
				float w2 = 0.5f * direction * connectorWidth;
				float w4 = 0.25f * direction * connectorWidth;
				float h2 = 0.5f * connectorDirection * direction * connectorHeight;
				segs.appendItem(piecePath.createSVGPathSegLinetoHorizontalAbs(x1));
				segs.appendItem(piecePath.createSVGPathSegCurvetoCubicRel(-w4, -h2,  0, -ky,     -w4, ky - h2));
				segs.appendItem(piecePath.createSVGPathSegCurvetoCubicRel( w2, -h2,  0, -ky, w2 - kx,     -h2));
				segs.appendItem(piecePath.createSVGPathSegCurvetoCubicRel( w2,  h2, kx,  0 ,      w2, h2 - ky));
				segs.appendItem(piecePath.createSVGPathSegCurvetoCubicRel(-w4,  h2,  0,  ky,     -w4, h2 - ky));				
			}
			@Override
			void makeHConnector(
					float connectorWidth,
					float connectorHeight,
					float pieceHeight,
					int direction,
					int connectorDirection,
					OMSVGPathElement piecePath,
					OMSVGPathSegList segs) {
				float y1 = 0.5f * (pieceHeight - 0.5f * direction * connectorHeight);
				float kx = 0.5f * K * connectorDirection * direction * connectorWidth;
				float ky = 0.5f * K * direction * connectorHeight;
				float h2 = 0.5f * direction * connectorHeight;
				float h4 = 0.25f * direction * connectorHeight;
				float w2 = 0.5f * connectorDirection * direction * connectorWidth;
				segs.appendItem(piecePath.createSVGPathSegLinetoVerticalAbs(y1));
				segs.appendItem(piecePath.createSVGPathSegCurvetoCubicRel(-w2, -h4, -kx,  0, kx - w2,    -h4));
				segs.appendItem(piecePath.createSVGPathSegCurvetoCubicRel(-w2,  h2, -kx,  0,     -w2, h2 -ky));
				segs.appendItem(piecePath.createSVGPathSegCurvetoCubicRel( w2,  h2,  0 , ky, w2 - kx,     h2));
				segs.appendItem(piecePath.createSVGPathSegCurvetoCubicRel( w2, -h4,  kx,  0, w2 - kx,    -h4));
			}
		},
		NONE;
		void makeVConnector(
				float connectorWidth,
				float connectorHeight,
				float pieceWidth,
				int direction,
				int connectorDirection,
				OMSVGPathElement piecePath,
				OMSVGPathSegList segs) {
			
		}
		void makeHConnector(
				float connectorWidth,
				float connectorHeight,
				float pieceHeight,
				int direction,
				int connectorDirection,
				OMSVGPathElement piecePath,
				OMSVGPathSegList segs) {
		}
	}

	static PuzzleCss style = PuzzleBundle.INSTANCE.getCss();
	private static final String ID_PIECE = "piece";
	private static final String ID_PIECE_CLIP = "piecec";
	private static final String ID_PIECE_PATH = "piecep";
	private static final String ID_IMAGE = "puzzle";
	/**
	 * Best tangent size to emulate circle with spline
	 */
	private static final float K = ((float)Math.sqrt(2) - 1) * 4 / 3;
	/**
	 * Size of the connector as a percentage of the piece size
	 */
	private static final float CONNECTOR_PCT = 0.15f;
	/**
	 * Size of the border as a percentage of the puzzle size
	 */
	private static final float ASSEMBLY_BORDER_SIZE_PCT = 0.075f;
	/**
	 * Size of the border corner radius as a percentage of the puzzle size
	 */
	private static final float ASSEMBLY_BORDER_CORNER_PCT = 0.025f;
	/**
	 * Size of the margin separating the assemblyZone from the
	 * pieceZone as a percentage of the puzzle size
	 */
	private static final float MARGIN_PCT = 0.04f;

	/**
	 * The list of all puzzle pieces
	 */
	private List<Piece> pieceList;
	/**
	 * The original puzzle SVG Image
	 */
	OMSVGSVGElement srcSvg;
	/**
	 * The root element of the SVG DOM hierarchy
	 */
	OMSVGSVGElement rootSvg;
	/**
	 * The tile zone
	 */
	private TargetMatrix tileZone;
	/**
	 * The assembly zone
	 */
	private TargetMatrix assemblyZone;
	/**
	 * The border around the assembly zone
	 */
	private OMSVGRectElement assemblyBorder;
	/**
	 * The content rectangle #1 in the assembly zone
	 */
	private OMSVGRectElement assemblyContent1;
	/**
	 * The content rectangle #2 in the assembly zone
	 */
	private OMSVGRectElement assemblyContent2;
	/**
	 * The number of pieces per column
	 */
	private int colCount;
	/**
	 * The number of pieces per row
	 */
	private int rowCount;
	/**
	 * The dimension of a tile in the tile zone
	 */
	float tileWidth, tileHeight;
	/**
	 * The coordinates of the upper left corner of
	 * the tile zone
	 */
	float tileZoneX, tileZoneY;
	/**
	 * The coordinates of the upper left corner of the
	 * puzzle in the assembly zone
	 */
	float puzzleX, puzzleY;
	/**
	 * Size of puzzle svg in svg coordinates
	 */
	float srcWidth, srcHeight;
	/**
	 * Size of a puzzle piece in puzzle svg coordinates
	 */
	float pieceWidth, pieceHeight;
	/**
	 * Size of a puzzle connector in puzzle svg coordinates
	 */
	float connectorWidth, connectorHeight;
	/**
	 * True if a drag and drop session is taking place
	 */
	boolean dragging;
	/**
	 * Distance from the drag point to the piece upper
	 * left corner
	 */
	OMSVGPoint d;
	/**
	 * Drag and drop source
	 */
	Target srcTarget;
	/**
	 * Drag and drop target
	 */
	Target destTarget;
	/**
	 * True if the game is displayed in landscape mode
	 */
	private boolean landscape;

	public Puzzle(OMSVGSVGElement srcSvg, int colCount, int rowCount) {
		this.srcSvg = srcSvg;
		this.colCount = colCount;
		this.rowCount = rowCount;

		// Compute basic metrics
		OMSVGRect viewBox = srcSvg.getViewBox().getBaseVal();
		srcWidth = viewBox.getWidth();
		srcHeight = viewBox.getHeight();
		pieceWidth = srcWidth / colCount; // The dimension of a piece
		pieceHeight = srcHeight / rowCount;
		connectorWidth = CONNECTOR_PCT * pieceWidth; // The dimension of a piece connector
		connectorHeight = CONNECTOR_PCT * pieceHeight;
		tileWidth = (CONNECTOR_PCT * 2 + 1.01f) * pieceWidth;
		tileHeight = (CONNECTOR_PCT * 2 + 1.01f) * pieceHeight;

		// Create the puzzle geometry
		rootSvg = new OMSVGSVGElement();
		rootSvg.addMouseDownHandler(this);
		rootSvg.addMouseMoveHandler(this);
		rootSvg.addMouseUpHandler(this);
		OMSVGDefsElement defs = new OMSVGDefsElement();
		rootSvg.appendChild(defs);

		// Create main tile zones
		tileZone = new TargetMatrix("tiles", style.tileShadow(), style.tileShadowSelected());
		assemblyZone = new TargetMatrix("assembly", style.assemblyShadow(), style.assemblyShadowSelected());

		// Create the puzzle pieces
		pieceList = new ArrayList<Piece>();
		for (int i = 0; i < colCount; i++) {
			for (int j = 0; j < rowCount; j++) {
				Piece piece = new Piece(i, j);
				tileZone.setPiece(piece, i, j);
				pieceList.add(piece);
			}
		}
		
		// Create the connectors between the pieces
		for (int i = 0; i < colCount - 1; i++) {
			for (int j = 0; j < rowCount; j++) {
				Connector c = new Connector(tileZone.getPiece(i, j), tileZone.getPiece(i + 1, j));
				tileZone.getPiece(i, j).east = c;
				tileZone.getPiece(i + 1, j).west = c;
			}
		}
		for (int i = 0; i < colCount; i++) {
			for (int j = 0; j < rowCount - 1; j++) {
				Connector c = new Connector(tileZone.getPiece(i, j), tileZone.getPiece(i, j + 1));
				tileZone.getPiece(i, j).south = c;
				tileZone.getPiece(i, j + 1).north = c;
			}
		}
		//
		// Create the puzzle game layout:
		// + at the left or top, the assemblyGroup (where the player assembles the pieces).
		//   The assemblyGroup itself consists in a assemblyBorder, and an
		//   assemblyZone (which contains the assembled pieces). Optionally and
		//   assemblyHint can be displayed (show the contour of the pieces)
		// + at the right or bottom, the tileZone (where the pieces are randomly positioned
		//   on a grid of tiles)
		// <g id="assemblyGroup">
		//  <rect id="assemblyBorder" rx="" ry="">
		//  <rect id="assemblyContent1"/>
		//  <g id="assemblyHint">
		//   <use xlink:href="#piecepX-Y"/>
		//  </g>
		//  <rect id="assemblyContent2"/>
		// <g>
		// </g>
		OMSVGGElement assemblyGroup = new OMSVGGElement();


		assemblyBorder = new OMSVGRectElement();
		assemblyBorder.setClassNameBaseVal(style.assemblyBorder());
		assemblyContent1 = new OMSVGRectElement();
		assemblyContent1.setClassNameBaseVal(style.assemblyContent1());
		OMSVGGElement assemblyShadows = new OMSVGGElement();
		assemblyContent2 = new OMSVGRectElement();
		assemblyContent2.setClassNameBaseVal(style.assemblyContent2());
		assemblyGroup.appendChild(assemblyBorder);
		assemblyGroup.appendChild(assemblyContent1);
		assemblyGroup.appendChild(assemblyShadows);
		assemblyGroup.appendChild(assemblyContent2);
		rootSvg.appendChild(assemblyGroup);
		OMSVGGElement tileShadows = new OMSVGGElement();
		rootSvg.appendChild(tileShadows);
		
		// Copy the source SVG in a dedicated group inside
		// the defs
		OMSVGGElement imgGroup = new OMSVGGElement();
		imgGroup.setId(ID_IMAGE);
		for (OMNode node : srcSvg.getChildNodes()) {
			imgGroup.appendChild(node.cloneNode(true));
		}
		defs.appendChild(imgGroup);

		ConnectorShape connectorShape = ConnectorShape.SPLINE;
		String connectorParam = Window.Location.getParameter("connector");
		if (connectorParam != null) {
			try {
				connectorShape = ConnectorShape.valueOf(ConnectorShape.class, connectorParam.toUpperCase());
			} catch(Throwable e) {
				GWT.log("Cannot parse connector=" + connectorParam, e);
			}
		}

		for (int i = 0; i < colCount; i++) {
			for (int j = 0; j < rowCount; j++) {
				
				// Create the piece definition geometry
				// Each piece definition has the following structure
				// <g id="pieceX-Y">
				//  <clipPath id="piececX-Y">
				//   <path id="piecepX-Y" d="..."/> 
				//  </clipPath>
				//  <use x="0" y="0" xlink:href="#piecepX-Y"/>
				//  <g style="clip-path:url(#piececX-Y)">
				//   <g transform="translate(-X,-Y)">
				//    <use x="0" y="0" xlink:href="#puzzle"/>
				//   </g>
				//  </g>
				//  <use x="0" y="0" xlink:href="#piecepX-Y"/>
				// </g>
				Piece piece = tileZone.getPiece(i, j);

				OMSVGGElement pieceDef = new OMSVGGElement();
				String idPiece = ID_PIECE + piece.getId();
				pieceDef.setId(idPiece);
				
				String idPieceClip = ID_PIECE_CLIP + piece.getId();
				OMSVGClipPathElement pieceClipDef = new OMSVGClipPathElement();
				pieceClipDef.setId(idPieceClip);
				
				String idPiecePath= ID_PIECE_PATH + piece.getId();
				OMSVGPathElement piecePath = new OMSVGPathElement();
				piecePath.setId(idPiecePath);
				OMSVGPathSegList segs = piecePath.getPathSegList();
				segs.appendItem(piecePath.createSVGPathSegMovetoAbs(0f, 0f));
				if (piece.north != null) {
					connectorShape.makeVConnector(connectorWidth, connectorHeight, pieceWidth, 1, (piece.north.src == piece) ? 1 : -1, piecePath, segs);
				}
				segs.appendItem(piecePath.createSVGPathSegLinetoHorizontalAbs(pieceWidth));
				if (piece.east != null) {
					connectorShape.makeHConnector(connectorWidth, connectorHeight, pieceHeight, 1, (piece.east.src == piece) ? 1 : -1, piecePath, segs);
				}
				segs.appendItem(piecePath.createSVGPathSegLinetoVerticalAbs(pieceHeight));
				if (piece.south != null) {
					connectorShape.makeVConnector(connectorWidth, connectorHeight, pieceWidth, -1, (piece.south.src == piece) ? 1 : -1, piecePath, segs);
				}
				segs.appendItem(piecePath.createSVGPathSegLinetoHorizontalAbs(0));
				if (piece.west != null) {
					connectorShape.makeHConnector(connectorWidth, connectorHeight, pieceHeight, -1, (piece.west.src == piece) ? 1 : -1, piecePath, segs);
				}
				segs.appendItem(piecePath.createSVGPathSegClosePath());

				OMSVGGElement pieceClipPath = new OMSVGGElement();
				pieceClipPath.getStyle().setSVGProperty(SVGConstants.CSS_CLIP_PATH_PROPERTY, "url(#" + idPieceClip + ")");
				
				OMSVGGElement pieceTransform = new OMSVGGElement();
				OMSVGTransform xform = rootSvg.createSVGTransform();
				xform.setTranslate(
						viewBox.getX() - i * pieceWidth, 
						viewBox.getY() - j * pieceHeight);
				pieceTransform.getTransform().getBaseVal().appendItem(xform);

				OMSVGUseElement pieceContent = new OMSVGUseElement();
				pieceContent.getX().getBaseVal().setValue(viewBox.getX());
				pieceContent.getY().getBaseVal().setValue(viewBox.getY());
				pieceContent.getHref().setBaseVal("#" + idPiecePath);
				pieceContent.setClassNameBaseVal(style.pieceContent());

				OMSVGUseElement imgUse = new OMSVGUseElement();
				imgUse.getX().getBaseVal().setValue(viewBox.getX());
				imgUse.getY().getBaseVal().setValue(viewBox.getY());
				imgUse.getHref().setBaseVal("#" + ID_IMAGE);
				
				OMSVGUseElement pieceBorder = new OMSVGUseElement();
				pieceBorder.getX().getBaseVal().setValue(viewBox.getX());
				pieceBorder.getY().getBaseVal().setValue(viewBox.getY());
				pieceBorder.getHref().setBaseVal("#" + idPiecePath);
				pieceBorder.setClassNameBaseVal(style.pieceBorder());
				
				pieceDef.appendChild(pieceClipDef);
				pieceClipDef.appendChild(piecePath);
				pieceDef.appendChild(pieceContent);
				pieceDef.appendChild(pieceClipPath);
				pieceClipPath.appendChild(pieceTransform);
				pieceTransform.appendChild(imgUse);
				pieceDef.appendChild(pieceBorder);
				defs.appendChild(pieceDef);
				
				// Create the hints
				OMSVGUseElement tileShadow = new OMSVGUseElement();
				tileShadow.getHref().setBaseVal("#" + idPiecePath);
				tileShadow.setClassNameBaseVal(style.tileShadow());
				piece.shadow = tileShadow;
				tileShadows.appendChild(tileShadow);
				OMSVGUseElement assemblyShadow = new OMSVGUseElement();
				Target assemblyTarget = assemblyZone.getTarget(i,j);
				assemblyShadow.getHref().setBaseVal("#" + idPiecePath);
				assemblyShadows.appendChild(assemblyShadow);
				assemblyTarget.setShadow(assemblyShadow);
				assemblyShadows.appendChild(assemblyShadow);

				// Create the piece
				// <use x="130" y="260" xlink:href="#pieceX-Y"/>
				OMSVGUseElement geometry = new OMSVGUseElement();
				geometry.setClassNameBaseVal(style.piece());
				geometry.getHref().setBaseVal("#" + idPiece);
				rootSvg.appendChild(geometry);
				piece.geometry = geometry;
			}
		}
		doLayout();
	}
	
	public void doLayout() {
		int windowWidth = Window.getClientWidth();
		int windowHeight = Window.getClientHeight();
		landscape = windowWidth >= windowHeight;

		// Compute basic metrics
		float borderWidth = ASSEMBLY_BORDER_SIZE_PCT * srcWidth;
		float borderHeight = ASSEMBLY_BORDER_SIZE_PCT * srcHeight;
		float borderCornerWidth = ASSEMBLY_BORDER_CORNER_PCT * srcWidth;
		float borderCornerHeight = ASSEMBLY_BORDER_CORNER_PCT * srcHeight;
		float tileZoneWidth = colCount * tileWidth;
		float tileZoneHeight = rowCount * tileHeight;
		float assemblyZoneWidth = borderWidth * 2 + srcWidth;
		float assemblyZoneHeight = borderHeight * 2 + srcHeight;
		float assemblyZoneX = landscape ? 0f : 0.5f * (tileZoneWidth - assemblyZoneWidth);
		float assemblyZoneY = landscape ? 0.5f * (tileZoneHeight - assemblyZoneHeight) : 0;
		tileZoneX = landscape ? assemblyZoneWidth + MARGIN_PCT * srcWidth : 0f;
		tileZoneY = landscape ? 0f : assemblyZoneHeight + MARGIN_PCT * srcHeight;
		puzzleX = assemblyZoneX + borderWidth;
		puzzleY = assemblyZoneY + borderHeight;
		float totalWidth = landscape ? tileZoneX + tileZoneWidth : tileZoneWidth;
		float totalHeight = landscape ? tileZoneHeight : tileZoneY + tileZoneHeight;
		rootSvg.setViewBox(0, 0, totalWidth, totalHeight);
		rootSvg.getWidth().getBaseVal().newValueSpecifiedUnits(Unit.PCT, 100);
		rootSvg.getHeight().getBaseVal().newValueSpecifiedUnits(Unit.PCT, 100);
		
		assemblyBorder.getX().getBaseVal().setValue(assemblyZoneX);
		assemblyBorder.getY().getBaseVal().setValue(assemblyZoneY);
		assemblyBorder.getWidth().getBaseVal().setValue(assemblyZoneWidth);
		assemblyBorder.getHeight().getBaseVal().setValue(assemblyZoneHeight);
		assemblyBorder.getRx().getBaseVal().setValue(borderCornerWidth);
		assemblyBorder.getRy().getBaseVal().setValue(borderCornerHeight);
		

		assemblyContent1.getX().getBaseVal().setValue(puzzleX);
		assemblyContent1.getY().getBaseVal().setValue(puzzleY);
		assemblyContent1.getWidth().getBaseVal().setValue(srcWidth);
		assemblyContent1.getHeight().getBaseVal().setValue(srcHeight);

		assemblyContent2.getX().getBaseVal().setValue(puzzleX);
		assemblyContent2.getY().getBaseVal().setValue(puzzleY);
		assemblyContent2.getWidth().getBaseVal().setValue(srcWidth);
		assemblyContent2.getHeight().getBaseVal().setValue(srcHeight);
		tileZone.doLayout(tileZoneX + connectorWidth, tileZoneY + connectorHeight, tileWidth, tileHeight);
		assemblyZone.doLayout(puzzleX, puzzleY, pieceWidth, pieceHeight);
	}
	
	public boolean isLandscape() {
		return landscape;
	}

	public OMSVGSVGElement getSvgElement() {
		return rootSvg;
	}
	
	public void shuffle() {
		List<Piece> pieceList = new ArrayList<Piece>(this.pieceList);
		// Shuffle the pieces
		int pieceCount = colCount * rowCount;
		for (int i = 0; i < colCount; i++) {
			for (int j = 0; j < rowCount; j++) {
				assemblyZone.setPiece(null, i, j);
				tileZone.setPiece(pieceList.remove(Random.nextInt(pieceCount--)), i, j);
			}
		}
	}
	
	@Override
	public void onMouseDown(MouseDownEvent event) {
		if (!dragging) {
			srcTarget = getTarget(event);
			if (srcTarget != null) {
				Piece piece = srcTarget.getPiece();
				if (piece != null) {
					dragging = true;
					d = getCoordinates(event).substract(srcTarget.getPosition());
					// Move the DOM node to the end of the tree so that it is drawn after
					// all other nodes
					rootSvg.removeChild(piece.geometry);
					rootSvg.appendChild(piece.geometry);
				}
				event.preventDefault();
				event.stopPropagation();
			}
		} else {
			onMouseUp_(event);
		}
	}
	
	@Override
	public void onMouseMove(MouseMoveEvent event) {
		if (dragging) {
			if (destTarget != null) {
				destTarget.setSelected(false, destTarget.shadow != null ? destTarget.shadow : srcTarget.piece.shadow);
			}
			Target target = getTarget(event);
//			GWT.log("target = " + target);
			if (target != null && (target.piece == null || target == srcTarget)) {
				destTarget = target;
				destTarget.setSelected(true, destTarget.shadow != null ? destTarget.shadow : srcTarget.piece.shadow);
			} else {
				destTarget = null;
			}
			srcTarget.setPosition(getCoordinates(event).substract(d));
			event.preventDefault();
			event.stopPropagation();
		}
	}

	@Override
	public void onMouseUp(MouseUpEvent event) {
		onMouseUp_(event);
	}

	private void onMouseUp_(MouseEvent<? extends EventHandler> event) {
		if (dragging) {
			if (destTarget == null) {
				destTarget = srcTarget;
			} else {
				if (destTarget != null) {
					destTarget.setSelected(false, destTarget.shadow != null ? destTarget.shadow : srcTarget.piece.shadow);
					destTarget.setPiece(srcTarget.getPiece());
					if (srcTarget != destTarget) {
						srcTarget.setPiece(null);
					}
					if (isGameOver()) {
						Window.alert(PuzzleConstants.INSTANCE.congratulations());
					}
				} else {
					srcTarget.setPosition(srcTarget.getPosition());
				}
				destTarget = null;
				dragging = false;
			}
		}
		event.preventDefault();
		event.stopPropagation();
	}

	public boolean isGameOver() {
		for (int i = 0; i < colCount; i++) {
			for (int j = 0; j < rowCount; j++) {
				Piece piece = assemblyZone.getPiece(i, j);
				if (piece == null || piece.x != i || piece.y != j) {
					return false;
				}
			}
		}
		return true;
	}
	
	public OMSVGPoint getCoordinates(MouseEvent<? extends EventHandler> e) {
		OMSVGPoint p = rootSvg.createSVGPoint(e.getClientX(), e.getClientY());
		OMSVGMatrix m = rootSvg.getScreenCTM().inverse();
		return p.matrixTransform(m);
	}
	
	public Target getTarget(MouseEvent<? extends EventHandler> e) {
		Target target = tileZone.getTarget(e);
		if (target == null) {
			target = assemblyZone.getTarget(e);
		}
		return target;
	}
}
