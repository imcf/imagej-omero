/*
 * #%L
 * Server- and client-side communication between ImageJ and OMERO.
 * %%
 * Copyright (C) 2013 Board of Regents of the University of
 * Wisconsin-Madison.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package imagej.omero;

import imagej.ImageJ;
import imagej.module.Module;
import imagej.module.ModuleInfo;

import java.io.IOException;
import java.util.Date;

import org.scijava.AbstractContextual;
import org.scijava.Context;

/**
 * Executes ImageJ {@link Module}s as OMERO scripts.
 * 
 * @author Curtis Rueden
 * @see "https://www.openmicroscopy.org/site/support/omero4/developers/Modules/Scripts.html"
 */
public class ScriptRunner extends AbstractContextual {

	// -- Instance fields --

	/** The ImageJ application gateway. */
	private final ImageJ ij;

	// -- Constructors --

	public ScriptRunner() {
		this(new ImageJ());
	}

	public ScriptRunner(final Context context) {
		this(new ImageJ(context));
	}

	public ScriptRunner(final ImageJ ij) {
		this.ij = ij;
		setContext(ij.getContext());
	}

	// -- ScriptRunner methods --

	/** Gets the ImageJ application gateway used by this script runner. */
	public ImageJ ij() {
		return ij;
	}

	/** Invokes the given ImageJ module identifier as an OMERO script. */
	public void invoke(final String id) throws omero.ServerError,
		Glacier2.CannotCreateSessionException, Glacier2.PermissionDeniedException,
		IOException
	{
		// look for a module matching the given identifier
		final ModuleInfo info = ModuleUtils.findModule(ij.module(), id);
		invoke(info);
	}

	/** Invokes the given ImageJ module as an OMERO script. */
	public void invoke(final ModuleInfo info) throws omero.ServerError,
		Glacier2.CannotCreateSessionException, Glacier2.PermissionDeniedException,
		IOException
	{
		// initialize OMERO client session
		final omero.client c = new omero.client();

		// initialize module converter
		final ModuleAdapter adaptedModule =
			new ModuleAdapter(ij.getContext(), info, c);

		// perform appropriate action (either parse or launch)
		try {
			c.createSession().detachOnDestroy();
			final String parse = c.getProperty("omero.scripts.parse");
			if (!parse.isEmpty()) adaptedModule.params();
			else adaptedModule.launch();
		}
		finally {
			c.__del__();
		}
	}

	// -- Main method --

	/** Simple entry point for executing ImageJ modules as scripts. */
	public static void main(final String... args) throws Exception {
		System.err.println(new Date() + ": initializing script runner");

		// NB: Make ImageJ startup less verbose.
		System.setProperty("scijava.log.level", "warn");

		// HACK: Something on OMERO's classpath sets the log level to DEBUG.
		java.util.logging.Logger.getLogger(
			java.util.logging.Logger.GLOBAL_LOGGER_NAME).setLevel(
			java.util.logging.Level.WARNING);

		// initialize script runner
		final ScriptRunner scriptRunner = new ScriptRunner();

		// execute modules
		for (final String id : args) {
			System.err.println(new Date() + ": executing: " + id);
			scriptRunner.invoke(id);
		}

		// clean up resources
		scriptRunner.getContext().dispose();

		System.err.println(new Date() + ": executions completed");

		// shut down the JVM
		System.exit(0);
	}

}
