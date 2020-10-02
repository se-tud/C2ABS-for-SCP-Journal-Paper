package de.tud.se.c2abs;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.eclipse.core.runtime.Plugin;

public class C2ABSPlugin extends Plugin {

	private static C2ABSPlugin fgDefault;

	public C2ABSPlugin() {
		super();
		fgDefault = this;

		ConsoleAppender console = new ConsoleAppender(); // create appender
		// configure the appender
		String PATTERN = "%d [%p|%c|%C{1}] %m%n";
		console.setLayout(new PatternLayout(PATTERN));
		console.setThreshold(Level.INFO);
		console.activateOptions();
		// add appender to any Logger (here is root)
		Logger.getRootLogger().addAppender(console);
	}

	public static C2ABSPlugin getDefault() {
		return fgDefault;
	}

}
