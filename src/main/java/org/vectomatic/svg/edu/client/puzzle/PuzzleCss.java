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

import com.google.gwt.resources.client.CssResource;

/**
 * CSS declarations for the puzzle game
 */
public interface PuzzleCss extends CssResource {
	@ClassName("piece-content")
	public String pieceContent();
	@ClassName("piece-border")
	public String pieceBorder();
	@ClassName("piece")
	public String piece();
	@ClassName("assembly-border")
	public String assemblyBorder();
	@ClassName("assembly-content-1")
	public String assemblyContent1();
	@ClassName("assembly-content-2")
	public String assemblyContent2();
	@ClassName("assembly-shadow")
	public String assemblyShadow();
	@ClassName("assembly-shadow-selected")
	public String assemblyShadowSelected();
	@ClassName("tile-shadow")
	public String tileShadow();
	@ClassName("tile-shadow-selected")
	public String tileShadowSelected();

}